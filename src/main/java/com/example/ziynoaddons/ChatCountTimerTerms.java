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

public class ChatCountTimerTerms extends Gui {

    private final Minecraft mc = Minecraft.getMinecraft();

    // ---- STATE ----
    private static volatile boolean running = false;

    private static volatile int elapsedTicks = 0;

    private static volatile boolean fading = false;
    private static volatile int fadeTicks = 0;

    private static volatile boolean hudEnabled = true;

    private static volatile boolean s03Active = false;

    private static final String PIPELINE_NAME = "terms_timer_listener";
    private static final AtomicBoolean handlerAttached = new AtomicBoolean(false);

    public ChatCountTimerTerms() {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String msg = event.message.getUnformattedText();
        if (msg == null) return;

        // Start timer when Goldor speaks
        if (msg.contains("[BOSS] Goldor: Who dares trespass into my domain?")) {
            running = true;
            elapsedTicks = 0;
            fading = false;
            fadeTicks = 0;
            return;
        }

        // Stop timer when core opens
        if (msg.contains("The Core entrance is opening!")) {
            running = false;
            // start 2s fade
            fading = true;
            fadeTicks = 40; // 2s @ 20tps
            return;
        }

        // Reset timer on "Starting in 1"
        if (msg.contains("Starting in 1")) {
            running = false;
            elapsedTicks = 0;
            fading = false;
            fadeTicks = 0;
        }
    }

    // ---------------- RENDER ----------------
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if (!hudEnabled) return;
        if (mc.theWorld == null) return;

        // nothing to show
        if (!running && !fading) return;

        // compute seconds from ticks
        double elapsedSeconds = elapsedTicks / 20.0;
        String timerText = String.format("%.2f", elapsedSeconds);

        // your original color logic: cycle every second
        int currentSecond = (int) Math.floor(elapsedSeconds);
        int colorCycle = currentSecond % 3;
        int color;
        switch (colorCycle) {
            case 0: color = 0x00FF00; break; // green
            case 1: color = 0xFFFF00; break; // yellow
            case 2: color = 0xFF0000; break; // red
            default: color = 0xFFFFFF; break;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        float scale = 1.5f;
        int textWidth = mc.fontRendererObj.getStringWidth(timerText);
        float x = (screenWidth / 2f - textWidth * scale / 2f) / scale;
        float y = (screenHeight / 2f - 18) / scale;

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);
        mc.fontRendererObj.drawString(timerText, (int) x, (int) y, color, false);
        GlStateManager.popMatrix();
    }

    // ---------------- WORLD LOAD → RESET ----------------
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        running = false;
        elapsedTicks = 0;
        fading = false;
        fadeTicks = 0;
        s03Active = false;
    }

    // ---------------- CONNECT / DISCONNECT → attach netty ----------------
    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        attachHandler(e.manager);
        running = false;
        elapsedTicks = 0;
        fading = false;
        fadeTicks = 0;
        s03Active = false;
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        detachHandler(e.manager);
        running = false;
        elapsedTicks = 0;
        fading = false;
        fadeTicks = 0;
        s03Active = false;
    }

    private static void attachHandler(NetworkManager nm) {
        if (nm == null || nm.channel() == null) return;
        if (handlerAttached.getAndSet(true)) return;

        try {
            nm.channel().pipeline().addBefore("packet_handler", PIPELINE_NAME, new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

                    // we keep this so we can add S03 alignment later if you want
                    if (msg instanceof S08PacketPlayerPosLook) {
                        s03Active = true;
                    }

                    if (msg instanceof S03PacketTimeUpdate) {
                        if (s03Active) {
                            // no hard align needed for this timer right now
                        }
                    }

                    // real timing: S32 = +1 tick
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

    // ---------------- S32-driven timer ----------------
    private static void handleS32Tick() {
        // normal timing
        if (running) {
            elapsedTicks++;
            return;
        }

        // fade timing
        if (fading) {
            fadeTicks--;
            if (fadeTicks <= 0) {
                fading = false;
            }
        }
    }
}
