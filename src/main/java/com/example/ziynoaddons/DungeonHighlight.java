package com.example.ziynoaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class DungeonHighlight {
    public static boolean shadowAssassinEnabled = false;
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        double partialTicks = event.partialTicks;
        double camX = mc.getRenderManager().viewerPosX;
        double camY = mc.getRenderManager().viewerPosY;
        double camZ = mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull(); // we'll control culling manually later
        GlStateManager.depthMask(false);

        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO
        );

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == null || entity.isDead || entity instanceof EntityArmorStand) continue;

            // Get display/custom name
            String rawName = null;
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase elb = (EntityLivingBase) entity;
                if (elb.hasCustomName()) rawName = elb.getCustomNameTag();
            }
            if (rawName == null && entity.getDisplayName() != null)
                rawName = entity.getDisplayName().getUnformattedText();
            if (rawName == null) continue;

            String cleanName = EnumChatFormatting.getTextWithoutFormattingCodes(rawName).toLowerCase();

            // Detect miniboss types

            boolean isAssassinRaw = cleanName.contains("shadow assassin");
            boolean isAssassin = isAssassinRaw && shadowAssassinEnabled; // only if toggle ON

            boolean isFrozen = cleanName.contains("frozen adventurer");
            boolean isLost = cleanName.contains("lost adventurer");
            boolean isArchaeologist = cleanName.contains("angry archaeologist");
            boolean isMidas = cleanName.contains("king midas");


            if (!(isAssassin || isFrozen || isLost || isArchaeologist || isMidas)) continue;

            if (entity.isInvisible() && !isAssassin) continue;

            float r = 1f, g = 0f, b = 1f; // default magenta (Shadow Assassin)
            if (isFrozen) { r = 0.4f; g = 0.8f; b = 1.0f; }        // Ice-blue
            if (isLost) { r = 0.0f; g = 0.9f; b = 0.6f; }          // Teal
            if (isArchaeologist) { r = 0.0f; g = 0.7f; b = 1.0f; } // Diamond-blue
            if (isMidas) { r = 1.0f; g = 0.84f; b = 0.0f; }        // Gold

            AxisAlignedBB bb = entity.getEntityBoundingBox();
            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - camX;
            double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - camY;
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - camZ;

            AxisAlignedBB shifted = new AxisAlignedBB(
                    bb.minX - entity.posX + x,
                    bb.minY - entity.posY + y,
                    bb.minZ - entity.posZ + z,
                    bb.maxX - entity.posX + x,
                    bb.maxY - entity.posY + y,
                    bb.maxZ - entity.posZ + z
            );

            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(1f, 1f);

            GL11.glCullFace(GL11.GL_FRONT);
            drawFilledBox(shifted, r, g, b, 0.25f);

            GL11.glCullFace(GL11.GL_BACK);
            drawFilledBox(shifted, r, g, b, 0.25f);

            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glDisable(GL11.GL_CULL_FACE);

            GL11.glLineWidth(2.5f);
            drawBoxOutline(shifted, r, g, b, 1f);
            GL11.glLineWidth(1f);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    // === Rendering helpers ===

    private void drawFilledBox(AxisAlignedBB bb, float r, float g, float b, float a) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        // Bottom
        wr.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();

        // Top
        wr.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();

        // North
        wr.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();

        // South
        wr.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();

        // West
        wr.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();

        // East
        wr.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();

        tess.draw();
    }

    private void drawBoxOutline(AxisAlignedBB bb, float r, float g, float b, float a) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        // Bottom edges
        wr.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();

        wr.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();

        wr.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();

        wr.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();

        // Top edges
        wr.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();

        wr.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();

        wr.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();

        wr.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();

        // Verticals
        wr.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();

        wr.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();

        wr.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();

        wr.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();

        tess.draw();
    }
}