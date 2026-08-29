package com.ziyno.ziynoaddons;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.event.HoverEvent;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class DungeonLagTimer extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final String START_MSG = "Here, I found this map when I first entered the dungeon.";
    private static final String STOP_MSG  = "Defeated Maxor, Storm, Goldor, and Necron";

    private static volatile boolean running       = false;
    private static volatile int     tickTicksDone = 0;
    private static volatile long    realStartMs   = 0L;
    private static volatile boolean s03Active     = false;

    private static volatile boolean pendingDisplay      = false;
    private static volatile int     pendingDisplayTicks = 0;
    private static volatile ChatComponentText pendingMessage = null;

    private static final String PIPELINE_NAME = "dungeon_lag_timer_bw";
    private static final AtomicBoolean handlerAttached = new AtomicBoolean(false);

    public DungeonLagTimer() {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        String msg = e.message == null ? null : e.message.getUnformattedText();
        if (msg == null) return;

        if (msg.contains(START_MSG)) {
            running = true;
            tickTicksDone = 0;
            realStartMs = System.currentTimeMillis();
            
            pendingDisplay = false;
            pendingMessage = null;
            pendingDisplayTicks = 0;
            return;
        }

        if (msg.contains(STOP_MSG)) {
            if (!running) return;
            running = false;

            long realEndMs = System.currentTimeMillis();
            double realSeconds = (realEndMs - realStartMs) / 1000.0;
            double tickSeconds = tickTicksDone / 20.0;

            double lost = realSeconds - tickSeconds;
            if (lost < 0) lost = 0;

            String lostStr = String.format(Locale.ROOT, "%.2f", lost);
            String realStr = formatSeconds(realSeconds);
            String tickStr = formatSeconds(tickSeconds);

            EnumChatFormatting lagColor;
            if (lost < 4.0) {
                lagColor = EnumChatFormatting.GREEN;
            } else if (lost < 8.0) {
                lagColor = EnumChatFormatting.YELLOW;
            } else {
                lagColor = EnumChatFormatting.RED;
            }

            ChatComponentText base = new ChatComponentText(
                    EnumChatFormatting.AQUA + "Time Lost to Lag: " +
                            lagColor + lostStr + "s"
            );

            ChatComponentText hoverText = new ChatComponentText(
                    EnumChatFormatting.GOLD + "Real Time: " + EnumChatFormatting.YELLOW + realStr + "\n" +
                            EnumChatFormatting.GOLD + "Tick Time: " + EnumChatFormatting.YELLOW + tickStr
            );

            ChatStyle style = new ChatStyle()
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));
            base.setChatStyle(style);

            pendingDisplay = true;
            pendingDisplayTicks = 20; 
            pendingMessage = base;

            tickTicksDone = 0;
            realStartMs = 0L;
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
        running = false;
        tickTicksDone = 0;
        realStartMs = 0L;
        s03Active = false;

        pendingDisplay = false;
        pendingDisplayTicks = 0;
        pendingMessage = null;
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        attachHandler(e.manager);
        running = false;
        tickTicksDone = 0;
        realStartMs = 0L;
        s03Active = false;
        pendingDisplay = false;
        pendingDisplayTicks = 0;
        pendingMessage = null;
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        detachHandler(e.manager);
        running = false;
        tickTicksDone = 0;
        realStartMs = 0L;
        s03Active = false;
        pendingDisplay = false;
        pendingDisplayTicks = 0;
        pendingMessage = null;
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

    private static void handleS32() {
        
        if (running) {
            tickTicksDone++;
        }

        if (pendingDisplay) {
            pendingDisplayTicks--;
            if (pendingDisplayTicks <= 0) {
                if (mc.thePlayer != null && pendingMessage != null) {
                    mc.thePlayer.addChatMessage(pendingMessage);
                }
                pendingDisplay = false;
                pendingMessage = null;
                pendingDisplayTicks = 0;
            }
        }
    }

    private static String formatSeconds(double seconds) {
        int whole = (int) seconds;
        int minutes = whole / 60;
        int sec = whole % 60;
        int hundredths = (int) Math.floor((seconds - whole) * 100.0);
        return String.format(Locale.ROOT, "%02d:%02d.%02d", minutes, sec, hundredths);
    }
}
