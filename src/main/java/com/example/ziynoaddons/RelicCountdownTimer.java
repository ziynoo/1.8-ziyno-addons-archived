package com.example.ziynoaddons;

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
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;


public class RelicCountdownTimer extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final int TOTAL_TICKS = (int) Math.ceil(2.10 * 20.0); // 42

    // state
    private static volatile boolean running = false;
    private static volatile int remainingTicks = 0;

    // optional S08 → S03 (kept for parity, not required)
    private static volatile boolean s03Active = false;

    // netty
    private static final String PIPELINE_NAME = "relic_timer_listener";
    private static final AtomicBoolean handlerAttached = new AtomicBoolean(false);

    public RelicCountdownTimer() {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        final String msg = event.message.getUnformattedText();
        if (msg == null) return;

        if (msg.contains("[BOSS] Necron: All this, for nothing...")) {
            // start packet-driven countdown
            running = true;
            remainingTicks = TOTAL_TICKS;
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Text event) {
        if (!running) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        // convert to seconds for display
        double remainingSeconds = remainingTicks / 20.0;
        if (remainingSeconds < 0) remainingSeconds = 0;

        String timerText = String.format("%.2f", remainingSeconds);

        // colors (same as your original)
        int color = remainingSeconds >= 1.00 ? 0x00FF00
                : remainingSeconds >= 0.50 ? 0xFFFF00
                : 0xFF0000;

        ScaledResolution sr = new ScaledResolution(mc);
        int screenW = sr.getScaledWidth();
        int screenH = sr.getScaledHeight();

        float scale = 2f;
        int textW = mc.fontRendererObj.getStringWidth(timerText);
        float x = (screenW / 2f - textW * scale / 2f) / scale;
        float y = (screenH / 2f - 32) / scale;

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);
        mc.fontRendererObj.drawString(timerText, (int) x, (int) y, color, false);
        GlStateManager.popMatrix();
    }

    // reset on world change
    @SubscribeEvent
    public void onWorldChange(WorldEvent.Load e) {
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

                    // optional S03
                    if (msg instanceof S03PacketTimeUpdate) {
                        if (s03Active) {
                        }
                    }

                    // real timing: S32 = -1 tick
                    if (msg instanceof S32PacketConfirmTransaction) {
                        S32PacketConfirmTransaction p = (S32PacketConfirmTransaction) msg;

                        int windowId = p.getWindowId();
                        boolean accepted = p.func_148888_e(); // "accepted" flag

                        // Count only Hypixel's heartbeat-style transactions (prevents inventory clicks adding extra ticks)
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
            running = false;
            remainingTicks = 0;
        }
    }
}
