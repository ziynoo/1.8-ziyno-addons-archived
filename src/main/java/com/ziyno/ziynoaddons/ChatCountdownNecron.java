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
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public class ChatCountdownNecron extends Gui {

    private final Minecraft mc = Minecraft.getMinecraft();

    private static final int TOTAL_TICKS = (int) Math.ceil(7.8 * 20.0); 

    private static volatile boolean running = false;
    private static volatile int remainingTicks = 0;

    private static volatile boolean s03Active = false;

    private static final String PIPELINE_NAME = "necron_countdown_listener";
    private static final AtomicBoolean handlerAttached = new AtomicBoolean(false);

    public ChatCountdownNecron() {

        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String msg = event.message.getUnformattedText();
        if (msg == null) return;

        if (msg.contains("You went further than any human before")) {
            running = true;
            remainingTicks = TOTAL_TICKS;
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if (!running) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        double remainingTime = remainingTicks / 20.0;
        if (remainingTime < 0) remainingTime = 0;

        String timerText = String.format("%.2f", remainingTime);

        int color;
        if (remainingTime > 5.0) {
            color = 0x00FF00; 
        } else if (remainingTime > 2.0) {
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
        float y = (screenHeight / 2f - 65) / scale; 

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);
        mc.fontRendererObj.drawString(timerText, (int) x, (int) y, color, false);
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
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
            running = false;
            remainingTicks = 0;
        }
    }
}
