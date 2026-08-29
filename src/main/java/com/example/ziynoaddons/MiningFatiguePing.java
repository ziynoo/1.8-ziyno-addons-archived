package com.example.ziynoaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class MiningFatiguePing {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean ENABLED = true;
    public static float VOLUME = 1.0f;
    public static float PITCH  = 1.0f;
    public static String SOUND_ID = "note.pling";

    private boolean hadMiningFatigueLastTick = false;

    public MiningFatiguePing() {
        // Register this listener
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!ENABLED) return;

        if (mc.theWorld == null || mc.thePlayer == null) {
            hadMiningFatigueLastTick = false;
            return;
        }

        EntityPlayer player = mc.thePlayer;

        boolean hasMiningFatigueNow = player.isPotionActive(Potion.digSlowdown);

        if (hadMiningFatigueLastTick && !hasMiningFatigueNow) {
            player.playSound(SOUND_ID, VOLUME, PITCH);
        }

        // Update for next tick
        hadMiningFatigueLastTick = hasMiningFatigueNow;
    }
}
