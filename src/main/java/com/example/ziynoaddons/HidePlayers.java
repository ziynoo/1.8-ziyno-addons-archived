package com.example.ziynoaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HidePlayers {

    // Chat triggers
    private static final String START_MSG = "[BOSS] Storm: Pathetic Maxor, just like expected.";
    private static final String STOP_MSG  = "[BOSS] Storm: I should have known that I stood no chance.";

    private static boolean enabled = false;

    // Box corners (inclusive)
    private static final int X_MIN = 33, X_MAX = 41;
    private static final int Y_MIN = 169, Y_MAX = 171;
    private static final int Z_MIN = 63, Z_MAX = 67;

    // If true: hide players in that X/Z rectangle for ALL Y (0..256)
    private static final boolean FULL_HEIGHT = false;

    private static final boolean DONT_HIDE_SELF = true;

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static void reset() {
        enabled = false;
    }

    private static AxisAlignedBB getHideBox() {
        int minX = Math.min(X_MIN, X_MAX);
        int maxX = Math.max(X_MIN, X_MAX);
        int minY = Math.min(Y_MIN, Y_MAX);
        int maxY = Math.max(Y_MIN, Y_MAX);
        int minZ = Math.min(Z_MIN, Z_MAX);
        int maxZ = Math.max(Z_MIN, Z_MAX);

        double y0 = FULL_HEIGHT ? 0 : minY;
        double y1 = FULL_HEIGHT ? 256 : maxY;

        // +1 so it's inclusive in "block coords"
        return new AxisAlignedBB(minX, y0, minZ, maxX + 1, y1 + 1, maxZ + 1);
    }

    private static boolean isPlayerInBox(EntityPlayer p, AxisAlignedBB box) {
        return p.getEntityBoundingBox() != null && p.getEntityBoundingBox().intersectsWith(box);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.message == null) return;

        String msg = event.message.getUnformattedText();
        if (msg == null) return;

        // start when Storm speaks
        if (msg.contains(START_MSG)) {
            enabled = true;
            return;
        }

        // stop when Storm speaks
        if (msg.contains(STOP_MSG)) {
            enabled = false;
        }
    }

    // Reset when changing worlds / leaving dungeon
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        reset();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        reset();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!enabled) return;

        if (mc.theWorld == null || mc.thePlayer == null) return;

        AxisAlignedBB box = getHideBox();

        // Only hide others if YOU are also inside the box
        if (!isPlayerInBox(mc.thePlayer, box)) return;

        EntityPlayer p = event.entityPlayer;
        if (p == null) return;

        if (DONT_HIDE_SELF && p == mc.thePlayer) return;

        if (isPlayerInBox(p, box)) {
            event.setCanceled(true);
        }
    }

}
