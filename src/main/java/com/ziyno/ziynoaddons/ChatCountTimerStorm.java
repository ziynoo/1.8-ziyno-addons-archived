package com.ziyno.ziynoaddons;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public class ChatCountTimerStorm extends Gui {

    private final Minecraft mc = Minecraft.getMinecraft();

    private static volatile boolean running = false;
    private static volatile int elapsedTicks = 0;

    private static volatile boolean fading = false;
    private static volatile int fadeTicks = 0;

    private static volatile boolean hudEnabled = true;
    private static volatile boolean s03Active = false;

    private static final String PIPELINE_NAME = "storm_timer_listener";
    private static final AtomicBoolean handlerAttached = new AtomicBoolean(false);

    public static volatile double PING_START_SECONDS = 34.5; 
    public static volatile int PING_COUNT = 3;               

    private static final int PING_INTERVAL_TICKS = 1;

    private static volatile boolean pingArmed = true;   
    private static volatile int pingsLeft = 0;
    private static volatile int nextPingTick = 0;

    private static final String PING_SOUND_ID = "note.pling";
    private static final float PING_VOLUME = 1.0f;
    private static final float PING_PITCH = 2.0f;

    public ChatCountTimerStorm() {
        FMLCommonHandler.instance().bus().register(this);
    }

    public static void setPingSettings(double startSeconds, int count) {
        if (startSeconds < 0) startSeconds = 0;
        if (count < 0) count = 0;
        if (count > 50) count = 50; 

        PING_START_SECONDS = startSeconds;
        PING_COUNT = count;

        pingArmed = true;
        pingsLeft = 0;
        nextPingTick = 0;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String msg = event.message.getUnformattedText();
        if (msg == null) return;

        if (msg.contains("[BOSS] Storm: Pathetic Maxor, just like expected.")) {
            running = true;
            elapsedTicks = 0;
            fading = false;
            fadeTicks = 0;

            pingArmed = true;
            pingsLeft = 0;
            nextPingTick = 0;
            return;
        }

        if (msg.contains("[BOSS] Storm: I should have known that I stood no chance.")) {
            running = false;

            pingArmed = false;
            pingsLeft = 0;
            nextPingTick = 0;

            fading = true;
            fadeTicks = 40;
            return;
        }

        if (msg.contains("Starting in 1")) {
            running = false;
            fading = false;
            elapsedTicks = 0;
            fadeTicks = 0;

            pingArmed = true;
            pingsLeft = 0;
            nextPingTick = 0;
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if (!hudEnabled) return;
        if (mc.theWorld == null) return;
        if (!running && !fading) return;

        double elapsedSeconds = elapsedTicks / 20.0;
        String timerText = String.format("%.2f", elapsedSeconds);

        int color;
        if (elapsedSeconds < 20.0) {
            color = 0x00FF00;
        } else if (elapsedSeconds < 24.0) {
            color = 0xFFFF00;
        } else if (elapsedSeconds < 28.0) {
            color = 0xFF0000;
        } else {
            color = 0x00FF00;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        float scale = 2f;
        int textWidth = mc.fontRendererObj.getStringWidth(timerText);
        float x = (screenWidth / 2f - textWidth * scale / 2f) / scale;
        float y = (screenHeight / 2f - 65) / scale;

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);
        mc.fontRendererObj.drawString(timerText, (int) x, (int) y, color, false);
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        running = false;
        fading = false;
        elapsedTicks = 0;
        fadeTicks = 0;

        pingArmed = true;
        pingsLeft = 0;
        nextPingTick = 0;
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        attachHandler(e.manager);

        running = false;
        fading = false;
        elapsedTicks = 0;
        fadeTicks = 0;
        s03Active = false;

        pingArmed = true;
        pingsLeft = 0;
        nextPingTick = 0;
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        detachHandler(e.manager);

        running = false;
        fading = false;
        elapsedTicks = 0;
        fadeTicks = 0;
        s03Active = false;

        pingArmed = true;
        pingsLeft = 0;
        nextPingTick = 0;
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
        if (running) {
            elapsedTicks++;

            if (PING_COUNT > 0 && pingArmed) {
                double startTickExact = PING_START_SECONDS * 20.0;
                if (elapsedTicks >= startTickExact) {
                    pingArmed = false;
                    pingsLeft = PING_COUNT;
                    nextPingTick = elapsedTicks; 
                }
            }

            if (pingsLeft > 0 && elapsedTicks >= nextPingTick) {
                playPing();
                pingsLeft--;
                nextPingTick = elapsedTicks + PING_INTERVAL_TICKS;
            }

            return;
        }

        if (fading) {
            fadeTicks--;
            if (fadeTicks <= 0) {
                fading = false;
            }
        }
    }

    private static void playPing() {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;

        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (mc.thePlayer != null) {
                    mc.thePlayer.playSound(PING_SOUND_ID, PING_VOLUME, PING_PITCH);
                }
            }
        });
    }
}
