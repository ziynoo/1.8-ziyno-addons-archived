package com.example.ziynoaddons;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public class DeathTickTimer extends Gui {
    public static boolean secretTimerEnabled = true;
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final float NUDGE_PX = 0.045f;

    private static volatile int ticks = -1;
    private static volatile double[] spawnPos = null;

    private static volatile int periodTicks = 40;

    private static volatile boolean hudEnabled = false;

    private static volatile boolean s03Active = false;
    private static volatile boolean s32Active = false;
    private static volatile boolean wantSpawnPos = true;

    // netty
    private static final String PIPELINE_NAME = "bloodwarp_ticktimer";
    private static final AtomicBoolean handlerAttached = new AtomicBoolean(false);

    public static void init() {
        DeathTickTimer inst = new DeathTickTimer();
        MinecraftForge.EVENT_BUS.register(inst);
        FMLCommonHandler.instance().bus().register(inst);
        ClientCommandHandler.instance.registerCommand(new TickTimerCommand());
    }

    private static class TickTimerCommand extends CommandBase {
        @Override public String getCommandName() { return "ticktimer"; }
        @Override public String getCommandUsage(ICommandSender sender) { return "/ticktimer"; }
        @Override public int getRequiredPermissionLevel() { return 0; }
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            hudEnabled = !hudEnabled;
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.YELLOW + "[ticktimer] " + (hudEnabled ? "shown" : "hidden")
            ));
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Text e) {
        if (!hudEnabled || mc.theWorld == null || mc.thePlayer == null) return;

        int displayTicks = Math.max(0, ticks);
        String text = String.valueOf(displayTicks);


        int pt = Math.max(1, periodTicks);
        int redThresh   = Math.max(1, (int)Math.floor(pt * 5.0 / 20.0));   // <= redThresh => red
        int yellowLow   = Math.max(redThresh + 1, (int)Math.floor(pt * 6.0 / 20.0)); // inclusive
        int yellowHigh  = Math.max(yellowLow,     (int)Math.floor(pt * 10.0 / 20.0)); // inclusive
        int greenLow    = Math.min(pt, yellowHigh + 1); // top band start (≈11/20)

        int color;
        if (displayTicks <= redThresh) {
            color = 0xFF0000; // red
        } else if (displayTicks >= yellowLow && displayTicks <= yellowHigh) {
            color = 0xFFFF00; // yellow
        } else if (displayTicks >= greenLow) {
            color = 0x00FF00; // green
        } else {
            // If rounding leaves a tiny gap, treat it as yellow
            color = 0xFFFF00;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float scale = 1.5f;
        int strWidth = mc.fontRendererObj.getStringWidth(text);
        float x = (sr.getScaledWidth() / 2.0f) - (strWidth * scale) / 2.0f;
        float y = (sr.getScaledHeight() / 2.0f) - (mc.fontRendererObj.FONT_HEIGHT * scale) / 2.0f;
        y -= 19.25;

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);
        mc.fontRendererObj.drawStringWithShadow(text, (x / scale) + NUDGE_PX, y / scale, color);
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        String msg = e.message == null ? null : e.message.getUnformattedText();
        if (msg == null) return;

        if (msg.contains("Sending to server")) {
            ticks = -1;
            spawnPos = null;
            wantSpawnPos = true;
            periodTicks = 40; // reset cadence on new connection
        }

        if (msg.contains("is now ready!")) {
            hudEnabled = true;
        }

        if (msg.contains("[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!")){
            hudEnabled = false;
        }

        // Map voiceline: switch cadence to 20 ticks (≈1s cycle) and keep HUD on
        if (msg.contains("Here, I found this map when I first entered the dungeon.")) {
            if (secretTimerEnabled == false){
                hudEnabled = false;
                return;
            }
            periodTicks = 20;
            hudEnabled = true;
            // Give a sane current value now; S03 will align on next tick
            if (ticks <= 0 || ticks > periodTicks) ticks = periodTicks;
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload e) {
        if (mc.theWorld == null || e.world != mc.theWorld) return;
        ticks = -1;
        s32Active = true;
        s03Active = false;
        wantSpawnPos = true;
        hudEnabled = false;
        periodTicks = 40; // restore default cadence on world change
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        attachHandler(e.manager);
        ticks = -1;
        spawnPos = null;
        s03Active = false;
        s32Active = false;
        wantSpawnPos = true;
        periodTicks = 40; // default when (re)connecting
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        detachHandler(e.manager);
        ticks = -1;
        spawnPos = null;
        s03Active = false;
        s32Active = false;
        wantSpawnPos = true;
        hudEnabled = false;
        periodTicks = 40; // default when disconnected
    }

    private static void attachHandler(NetworkManager nm) {
        if (nm == null || nm.channel() == null) return;
        if (handlerAttached.getAndSet(true)) return;

        try {
            nm.channel().pipeline().addBefore("packet_handler", PIPELINE_NAME, new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

                    if (msg instanceof S08PacketPlayerPosLook) {
                        handleS08((S08PacketPlayerPosLook) msg);
                    }

                    if (msg instanceof S03PacketTimeUpdate) {
                        handleS03((S03PacketTimeUpdate) msg);
                    }

                    if (msg instanceof S32PacketConfirmTransaction) {
                        S32PacketConfirmTransaction p = (S32PacketConfirmTransaction) msg;

                        int windowId = p.getWindowId();
                        boolean accepted = p.func_148888_e(); // accepted flag

                        // Only count Hypixel heartbeat S32s (prevents inventory clicks from speeding it up)
                        if (windowId == 0 && !accepted) {
                            handleS32();
                        }
                    }

                    super.channelRead(ctx, msg);
                }
            });
        } catch (Throwable t) {
            handlerAttached.set(false);
        }
    }

    private static void detachHandler(NetworkManager nm) {
        if (nm == null || nm.channel() == null) { handlerAttached.set(false); return; }
        try {
            if (nm.channel().pipeline().get(PIPELINE_NAME) != null) {
                nm.channel().pipeline().remove(PIPELINE_NAME);
            }
        } catch (Throwable ignored) {}
        handlerAttached.set(false);
    }

    private static void handleS08(S08PacketPlayerPosLook p) {
        if (!s03Active) {
            s03Active = true;
        }

        if (wantSpawnPos) {
            double y = p.getY();
            if (y == 75.5 || y == 76.5) {
                spawnPos = new double[] { p.getX(), y, p.getZ() };
                wantSpawnPos = false;
            }
        }
    }

    private static void handleS03(S03PacketTimeUpdate p) {
        if (!s03Active) return;

        long total = p.getTotalWorldTime();
        if (total == 0L) return;

        // Align the countdown to the CURRENT period
        int pt = Math.max(1, periodTicks);
        int aligned = (int)(pt - (total % pt));
        if (aligned <= 0) aligned = pt;
        ticks = aligned;

        s32Active = true;
    }

    private static void handleS32() {
        if (!s32Active) return;
        int pt = Math.max(1, periodTicks);
        ticks--;
        if (ticks <= 0) {
            ticks = pt;
        }
    }
}