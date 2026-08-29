package com.example.ziynoaddons;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class SheepBoxMod {

    // ====== EDIT THESE ======
    private static final double SCALE = 1.6; //
    private static final double HALF_SIZE = 0.20 * SCALE;
    private static final double HEIGHT    = 0.40 * SCALE;
    private static final double Y_OFFSET  = 0.55;   // % of sheep height to center around (0.55 = mid-body-ish)

    private static final boolean THROUGH_WALLS = false; // set false if you want normal depth testing
    private static final float LINE_WIDTH = 2.0f;
    // =======================

    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre<EntityLivingBase> event) {
        if (!(event.entity instanceof EntitySheep)) return;

        // Stop the normal sheep model from rendering
        event.setCanceled(true);

        // Draw our fixed small cube instead
        drawFixedCube(event.entity, event.x, event.y, event.z);
    }

    private void drawFixedCube(EntityLivingBase entity, double x, double y, double z) {
        // Fixed cube bounds (NOT based on hitbox)
        double minX = -HALF_SIZE;
        double maxX =  HALF_SIZE;

        double centerY = entity.height * Y_OFFSET;
        double minY = centerY - (HEIGHT / 2.0);
        double maxY = centerY + (HEIGHT / 2.0);

        double minZ = -HALF_SIZE;
        double maxZ =  HALF_SIZE;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        if (THROUGH_WALLS) {
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
        }

        GL11.glLineWidth(LINE_WIDTH);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        // White (RGBA)
        float r = 1f, g = 1f, b = 1f, a = 1f;

        // Bottom rectangle
        line(wr, minX, minY, minZ, maxX, minY, minZ, r,g,b,a);
        line(wr, maxX, minY, minZ, maxX, minY, maxZ, r,g,b,a);
        line(wr, maxX, minY, maxZ, minX, minY, maxZ, r,g,b,a);
        line(wr, minX, minY, maxZ, minX, minY, minZ, r,g,b,a);

        // Top rectangle
        line(wr, minX, maxY, minZ, maxX, maxY, minZ, r,g,b,a);
        line(wr, maxX, maxY, minZ, maxX, maxY, maxZ, r,g,b,a);
        line(wr, maxX, maxY, maxZ, minX, maxY, maxZ, r,g,b,a);
        line(wr, minX, maxY, maxZ, minX, maxY, minZ, r,g,b,a);

        // Vertical edges
        line(wr, minX, minY, minZ, minX, maxY, minZ, r,g,b,a);
        line(wr, maxX, minY, minZ, maxX, maxY, minZ, r,g,b,a);
        line(wr, maxX, minY, maxZ, maxX, maxY, maxZ, r,g,b,a);
        line(wr, minX, minY, maxZ, minX, maxY, maxZ, r,g,b,a);

        tess.draw();

        if (THROUGH_WALLS) {
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
        }

        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private void line(WorldRenderer wr,
                      double x1, double y1, double z1,
                      double x2, double y2, double z2,
                      float r, float g, float b, float a) {
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, z2).color(r, g, b, a).endVertex();
    }
}
