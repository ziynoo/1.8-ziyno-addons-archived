package com.ziyno.ziynoaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LeapNotif extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final String PHASE1_START = "[BOSS] Goldor: Who dares trespass into my domain?";
    private static final String CORE_OPENING = "The Core entrance is opening!";
    private static final String PHASE2_END   = "All this, for nothing...";

    private static final double RADIUS = 10;

    private static final int COLOR_DARK_RED = 0xAA0000;
    private static final int COLOR_RED      = 0xFF5555;
    private static final int COLOR_YELLOW   = 0xFFFF55;
    private static final int COLOR_GREEN    = 0x55FF55;

    private static final int COLOR_DARK_BLUE = 0x3A46A2;

    
    private static final Region REGION_1 = new Region(
            "spot1",
            new AxisAlignedBB(59, 132, 138, 62, 135, 141),
            3,
            4
    );

    private static final Region REGION_2 = new Region(
            "spot2",
            new AxisAlignedBB(1, 109, 102, 3, 112, 108),
            3,
            3
    );

    private static final Region REGION_3 = new Region(
            "spot3",
            new AxisAlignedBB(53, 115, 50, 56, 118, 53),
            4,
            4
    );

    
    private static final Region REGION_4 = new Region(
            "spot4",
            new AxisAlignedBB(52, 5, 74, 57, 8, 79),
            4,
            4
    );

    private static final Region[] PHASE1_REGIONS = new Region[]{REGION_1, REGION_2, REGION_3};
    private static final Region[] PHASE2_REGIONS = new Region[]{REGION_4};

    private final Set<Integer> countedEntityIds = new HashSet<>();
    private int leapCount = 0;

    private Region activeRegion = null;
    private int lastTarget = -1;

    private boolean phase1Active = false;
    private boolean phase2Active = false;

    public LeapNotif() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static class Region {
        final String id;
        final AxisAlignedBB box;
        final int targetWhenOn;
        final int targetWhenOff;

        Region(String id, AxisAlignedBB box, int targetWhenOn, int targetWhenOff) {
            this.id = id;
            this.box = box;
            this.targetWhenOn = targetWhenOn;
            this.targetWhenOff = targetWhenOff;
        }

        int getTarget() {
            if ("spot1".equals(id)) {
                
                return ModConfig.splitee2Enabled ? targetWhenOn : targetWhenOff;
            }
            return targetWhenOn;
        }
    }

    private boolean isRegionEnabled(String id) {
        if ("spot1".equals(id)) return ModConfig.leapSpot1Enabled;
        if ("spot2".equals(id)) return ModConfig.leapSpot2Enabled;
        if ("spot3".equals(id)) return ModConfig.leapSpot3Enabled;
        if ("spot4".equals(id)) return ModConfig.leapSpot4Enabled;
        return true;
    }

    private boolean anyEnabledForPhase(boolean phase2) {
        if (phase2) {
            return ModConfig.leapSpot4Enabled;
        }
        return ModConfig.leapSpot1Enabled || ModConfig.leapSpot2Enabled || ModConfig.leapSpot3Enabled;
    }

    private void resetAll() {
        leapCount = 0;
        countedEntityIds.clear();
        activeRegion = null;
        lastTarget = -1;
    }

    private Region getRegionPlayerIsIn(EntityPlayer p, Region[] regions) {
        double x = p.posX, y = p.posY, z = p.posZ;

        for (Region r : regions) {
            if (!isRegionEnabled(r.id)) continue;

            AxisAlignedBB b = r.box;
            if (x >= b.minX && x <= b.maxX &&
                    y >= b.minY && y <= b.maxY &&
                    z >= b.minZ && z <= b.maxZ) {
                return r;
            }
        }
        return null;
    }

    private int getCountColorInt(int count, int target) {
        if (count < 0) count = 0;
        if (count > target) count = target;

        if (target == 3) {
            switch (count) {
                case 0: return COLOR_DARK_RED;
                case 1: return COLOR_RED;
                case 2: return COLOR_YELLOW;
                default: return COLOR_GREEN;
            }
        }

        switch (count) {
            case 0: return COLOR_DARK_RED;
            case 1: return COLOR_DARK_RED;
            case 2: return COLOR_RED;
            case 3: return COLOR_YELLOW;
            default: return COLOR_GREEN;
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        phase1Active = false;
        phase2Active = false;
        resetAll();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.message == null) return;

        String msg = event.message.getUnformattedText();
        if (msg == null) return;

        if (msg.contains(PHASE1_START)) {
            phase1Active = true;
            phase2Active = false;
            resetAll();
            return;
        }

        if (msg.contains(CORE_OPENING)) {
            phase1Active = false;
            phase2Active = true;
            resetAll();
            return;
        }

        if (msg.contains(PHASE2_END)) {
            phase2Active = false;
            resetAll();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!phase1Active && !phase2Active) return;

        if (phase2Active) {
            if (!anyEnabledForPhase(true)) {
                resetAll();
                return;
            }
        } else {
            if (!anyEnabledForPhase(false)) {
                resetAll();
                return;
            }
        }

        if (mc.theWorld == null || mc.thePlayer == null) {
            phase1Active = false;
            phase2Active = false;
            resetAll();
            return;
        }

        if (activeRegion != null && !isRegionEnabled(activeRegion.id)) {
            resetAll();
            return;
        }

        EntityPlayer player = mc.thePlayer;

        Region[] regions = phase2Active ? PHASE2_REGIONS : PHASE1_REGIONS;
        Region now = getRegionPlayerIsIn(player, regions);

        if (now != activeRegion) {
            activeRegion = now;
            leapCount = 0;
            countedEntityIds.clear();
            lastTarget = (activeRegion == null) ? -1 : activeRegion.getTarget();
        }

        if (activeRegion == null) return;

        int target = activeRegion.getTarget();

        if (target != lastTarget) {
            leapCount = 0;
            countedEntityIds.clear();
            lastTarget = target;
        }

        if (leapCount >= target) return;

        AxisAlignedBB search = player.getEntityBoundingBox().expand(RADIUS, RADIUS, RADIUS);
        List<Entity> nearby = mc.theWorld.getEntitiesWithinAABBExcludingEntity(player, search);

        for (Entity e : nearby) {
            if (e == null || e.isDead) continue;
            if (!(e instanceof EntityPlayer)) continue;

            int id = e.getEntityId();
            if (countedEntityIds.contains(id)) continue;

            double dx = e.posX - player.posX;
            double dy = e.posY - player.posY;
            double dz = e.posZ - player.posZ;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq <= (RADIUS * RADIUS)) {
                countedEntityIds.add(id);
                leapCount++;
                if (leapCount >= target) break;
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if (!phase1Active && !phase2Active) return;

        if (phase2Active) {
            if (!anyEnabledForPhase(true)) return;
        } else {
            if (!anyEnabledForPhase(false)) return;
        }

        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (activeRegion == null) return;

        if (!isRegionEnabled(activeRegion.id)) return;

        int target = activeRegion.getTarget();

        String left = String.valueOf(leapCount);
        String right = "/" + target + " Leaped";
        String full = left + right;

        ScaledResolution sr = new ScaledResolution(mc);
        int screenW = sr.getScaledWidth();
        int screenH = sr.getScaledHeight();

        final float scale = ModConfig.leapScale;

        int pixelX, pixelY;

        if (ModConfig.leapX == 0 && ModConfig.leapY == 0) {
            int textWidthPx = (int) (mc.fontRendererObj.getStringWidth(full) * scale);
            pixelX = screenW / 2 - textWidthPx / 2;
            pixelY = screenH / 2 - 32;
        } else {
            pixelX = ModConfig.leapX;
            pixelY = ModConfig.leapY;
        }

        float x = pixelX / scale;
        float y = pixelY / scale;

        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1.0f);

        int countColor = getCountColorInt(leapCount, target);

        mc.fontRendererObj.drawStringWithShadow(left, x, y, countColor);
        mc.fontRendererObj.drawStringWithShadow(
                right,
                x + mc.fontRendererObj.getStringWidth(left),
                y,
                COLOR_DARK_BLUE
        );

        GL11.glPopMatrix();
    }
}
