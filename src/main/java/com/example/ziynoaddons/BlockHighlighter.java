package com.example.ziynoaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class BlockHighlighter {

    private final Minecraft mc = Minecraft.getMinecraft();

    // Chat triggers
    private static final String START_MSG = "[BOSS] Storm: Pathetic Maxor, just like expected.";
    private static final String STOP_MSG  = "[BOSS] Storm: I should have known that I stood no chance.";

    // Toggle controlled by chat
    private static boolean enabled = false;

    // Style
    private static final float R = 0.0f, G = 1.0f, B = 0.0f;
    private static final float FILL_A = 0.20f;
    private static final float OUTLINE_A = 0.90f;
    private static final float LINE_WIDTH = 2.0f;

    public BlockHighlighter() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event == null || event.message == null) return;
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

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!enabled) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        RenderManager rm = mc.getRenderManager();
        double viewerX = rm.viewerPosX;
        double viewerY = rm.viewerPosY;
        double viewerZ = rm.viewerPosZ;

        // Render blocks between:
        // (94,165,90) and (94,165,94) inclusive (5 blocks)
        int x = 94;
        int y = 165;
        int z1 = 90;
        int z2 = 94;

        // GL state for translucent overlay
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        // Visible through walls (remove these 2 lines if you DON'T want through-walls)
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);

        for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
            BlockPos pos = new BlockPos(x, y, z);

            AxisAlignedBB bb = new AxisAlignedBB(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
            ).offset(-viewerX, -viewerY, -viewerZ);

            drawFilledBox(bb, R, G, B, FILL_A);
            GL11.glLineWidth(LINE_WIDTH);
            drawOutlinedBox(bb, R, G, B, OUTLINE_A);
        }

        // restore
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void drawFilledBox(AxisAlignedBB bb, float r, float g, float b, float a) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        GlStateManager.color(r, g, b, a);

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        // Bottom
        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();

        // Top
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();

        // North
        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();

        // South
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();

        // West
        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();

        // East
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();

        tess.draw();
    }

    private static void drawOutlinedBox(AxisAlignedBB bb, float r, float g, float b, float a) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        GlStateManager.color(r, g, b, a);

        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);

        // Bottom
        line(wr, bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY, bb.minZ);
        line(wr, bb.maxX, bb.minY, bb.minZ, bb.maxX, bb.minY, bb.maxZ);
        line(wr, bb.maxX, bb.minY, bb.maxZ, bb.minX, bb.minY, bb.maxZ);
        line(wr, bb.minX, bb.minY, bb.maxZ, bb.minX, bb.minY, bb.minZ);

        // Top
        line(wr, bb.minX, bb.maxY, bb.minZ, bb.maxX, bb.maxY, bb.minZ);
        line(wr, bb.maxX, bb.maxY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
        line(wr, bb.maxX, bb.maxY, bb.maxZ, bb.minX, bb.maxY, bb.maxZ);
        line(wr, bb.minX, bb.maxY, bb.maxZ, bb.minX, bb.maxY, bb.minZ);

        // Verticals
        line(wr, bb.minX, bb.minY, bb.minZ, bb.minX, bb.maxY, bb.minZ);
        line(wr, bb.maxX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.minZ);
        line(wr, bb.maxX, bb.minY, bb.maxZ, bb.maxX, bb.maxY, bb.maxZ);
        line(wr, bb.minX, bb.minY, bb.maxZ, bb.minX, bb.maxY, bb.maxZ);

        tess.draw();
    }

    private static void line(WorldRenderer wr, double x1, double y1, double z1, double x2, double y2, double z2) {
        wr.pos(x1, y1, z1).endVertex();
        wr.pos(x2, y2, z2).endVertex();
    }
}
