package com.ziyno.ziynoaddons;

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
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public class ChatCountdownTermsBW extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final int COUNTDOWN_TICKS = 104;

    private static volatile boolean running = false;
    private static volatile int remainingTicks = 0;

    private static volatile boolean s03Active = false;

    private static volatile boolean hudEnabled = true;

    private static final String PIPELINE_NAME = "terms_timer_listener_bw";
    private static final AtomicBoolean handlerAttached = new AtomicBoolean(false);

    public ChatCountdownTermsBW() {
        FMLCommonHandler.instance().bus().register(this);
        ClientCommandHandler.instance.registerCommand(new TermsTimerCommand());
    }

    private static class TermsTimerCommand extends CommandBase {
        @Override public String getCommandName() { return "termstimer"; }
        @Override public String getCommandUsage(ICommandSender sender) { return "/termstimer"; }
        @Override public int getRequiredPermissionLevel() { return 0; }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            hudEnabled = !hudEnabled;
            sender.addChatMessage(new net.minecraft.util.ChatComponentText(
                    (hudEnabled ? "§a[terms] shown" : "§c[terms] hidden")
            ));
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        String msg = e.message == null ? null : e.message.getUnformattedText();
        if (msg == null) return;

        if (msg.contains("[BOSS] Storm: I should have known that I stood no chance.")) {
            running = true;
            remainingTicks = COUNTDOWN_TICKS;
            return;
        }

        if (msg.contains("[BOSS] Goldor: Who dares trespass into my domain?")) {
            running = false;
            remainingTicks = 0;
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Text e) {
        if (!hudEnabled) return;
        if (!running) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        double seconds = remainingTicks / 20.0;
        if (seconds < 0) seconds = 0;

        int color = 0x00FF00;
        if (seconds <= 3.0 && seconds > 1.0) {
            color = 0xFFFF00;
        } else if (seconds <= 1.0) {
            color = 0xFF0000;
        }

        String timerText = String.format("%.2f", seconds);

        ScaledResolution sr = new ScaledResolution(mc);
        float scale = 1.4f;
        int textWidth = mc.fontRendererObj.getStringWidth(timerText);

        float x = (sr.getScaledWidth() / 2.0f) - (textWidth * scale) / 2.0f;
        float y = (sr.getScaledHeight() / 2.0f) - (mc.fontRendererObj.FONT_HEIGHT * scale) / 2.0f;
        y -= 20.0f;

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);
        mc.fontRendererObj.drawStringWithShadow(timerText, x / scale, y / scale, color);
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload e) {
        if (mc.theWorld == null || e.world != mc.theWorld) return;
        running = false;
        remainingTicks = 0;
        s03Active = false;
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        attachHandler(e.manager);
        running = false;
        remainingTicks = 0;
        s03Active = false;
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        detachHandler(e.manager);
        running = false;
        remainingTicks = 0;
        s03Active = false;
    }

    private static void attachHandler(NetworkManager nm) {
        if (nm == null || nm.channel() == null) return;
        if (handlerAttached.getAndSet(true)) return;

        try {
            nm.channel().pipeline().addBefore("packet_handler", PIPELINE_NAME, new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

                    if (msg instanceof S08PacketPlayerPosLook) {
                        s03Active = true;
                    }

                    if (msg instanceof S03PacketTimeUpdate) {
                        if (s03Active) {
                            
                        }
                    }

                    if (msg instanceof S32PacketConfirmTransaction) {
                        S32PacketConfirmTransaction p = (S32PacketConfirmTransaction) msg;

                        int windowId = p.getWindowId();
                        boolean accepted = p.func_148888_e(); 

                        if (windowId == 0 && !accepted) {
                            handleS32Tick();
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

    private static void handleS32Tick() {
        if (!running) return;
        remainingTicks--;
        if (remainingTicks <= 0) {
            remainingTicks = 0;
            running = false;
        }
    }
}
