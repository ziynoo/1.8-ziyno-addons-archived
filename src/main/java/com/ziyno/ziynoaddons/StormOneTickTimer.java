package com.ziyno.ziynoaddons;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class StormOneTickTimer extends Gui {

    public static boolean pyPingEnabled = false;
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final String LEAP_NEEDLE = "leaped to ziyno!"; 

    private static final String LEAP_SOUND = "note.pling";
    private static final float LEAP_VOL   = 10.0f;
    private static final float LEAP_PITCH = 1.0f;

    private static volatile boolean swRunning   = false;
    private static volatile int     swTicksDone = 0;

    private static volatile boolean guiShow        = false;
    private static volatile int     guiTicksFrozen = 0;

    private static final int GUI_DISPLAY_TICKS = 30;
    private static volatile int guiDisplayTicksRemaining = 0;

    private static volatile boolean leapArmed           = false;
    private static volatile boolean leapPingedThisWorld = false;

    private static volatile boolean s03Active = false;

    private static final String PIPELINE_NAME = "storm_one_tick_timer_listener";
    private static final AtomicBoolean handlerAttached = new AtomicBoolean(false);

    public StormOneTickTimer() {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        resetAll();
    }

    private static void resetAll() {
        leapPingedThisWorld = false;
        leapArmed = false;

        swRunning = false;
        swTicksDone = 0;

        guiShow = false;
        guiTicksFrozen = 0;
        guiDisplayTicksRemaining = 0;

        s03Active = false;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        final String msg = event.message.getUnformattedText();
        if (msg == null) return;
        final String msgLc = msg.toLowerCase(Locale.ROOT);

        if (msg.contains("[BOSS] Storm: Pathetic Maxor, just like expected.")) {
            swRunning = true;
            swTicksDone = 0;

            guiShow = false;
            guiTicksFrozen = 0;
            guiDisplayTicksRemaining = 0;

            leapArmed = true;

            return;
        }

        if (pyPingEnabled && leapArmed && !leapPingedThisWorld &&
                (msgLc.contains("leaped to " + Minecraft.getMinecraft().thePlayer.getName() + "!") ||
                        msgLc.contains("teleported to " + Minecraft.getMinecraft().thePlayer.getName() + "!"))) {

            if (mc.thePlayer != null) {
                mc.thePlayer.playSound(LEAP_SOUND, LEAP_VOL, LEAP_PITCH);
            }
            leapArmed = false;
            leapPingedThisWorld = true;
        }

        boolean isStormEnd =
                msgLc.contains("storm is enraged");

        if (swRunning && isStormEnd) {
            swRunning = false;

            guiTicksFrozen = swTicksDone;
            guiShow = true;
            guiDisplayTicksRemaining = GUI_DISPLAY_TICKS; 

            double elapsedSecRaw = swTicksDone / 20.0;
            double offsetSec = elapsedSecRaw - 35.0;
            if (offsetSec < 0.0) offsetSec = 0.0;

            String timeStr = String.format(Locale.ROOT, "%.2f", offsetSec);

            EnumChatFormatting numColor;
            if (offsetSec <= 0.70) {
                numColor = EnumChatFormatting.GREEN;
            } else if (offsetSec <= 1.70) { 
                numColor = EnumChatFormatting.YELLOW;
            } else {
                numColor = EnumChatFormatting.RED;
            }

            String chatLine = EnumChatFormatting.AQUA + "Storm was killed at " + EnumChatFormatting.RESET
                    + numColor + timeStr + "s" + EnumChatFormatting.RESET;

            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(chatLine));
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if (!guiShow) return; 
        if (mc.theWorld == null || mc.thePlayer == null) return;

        double elapsedTimeRaw = guiTicksFrozen / 20.0;
        double elapsedTime = elapsedTimeRaw - 35.0;
        if (elapsedTime < 0.0) elapsedTime = 0.0;

        String timerText = String.format(Locale.ROOT, "%.2f", elapsedTime);

        

        int color;
        if (elapsedTime <= 0.70) {
            color = 0x00FF00; 
        } else if (elapsedTime <= 1.70) {
            color = 0xFFFF00; 
        } else {
            color = 0xFF0000; 
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        float scale = 2f;
        int textWidth = mc.fontRendererObj.getStringWidth(timerText);
        float x = (screenWidth / 2f - textWidth * scale / 2f) / scale;
        float y = (screenHeight / 2f + 20) / scale;

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);
        mc.fontRendererObj.drawString(timerText, (int) x, (int) y, color, false);
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        attachHandler(e.manager);
        resetAll();
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        detachHandler(e.manager);
        resetAll();
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
        if (swRunning) {
            swTicksDone++;
        }

        if (guiShow && guiDisplayTicksRemaining > 0) {
            guiDisplayTicksRemaining--;
            if (guiDisplayTicksRemaining <= 0) {
                guiShow = false; 
            }
        }
    }
}
