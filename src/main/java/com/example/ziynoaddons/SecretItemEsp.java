package com.example.ziynoaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SecretItemEsp {

    private static final Minecraft mc = Minecraft.getMinecraft();
    public static boolean enabled = true;

    // Same secrets list as your ChatTriggers module
    private static final Set<String> SECRETS = new HashSet<>(Arrays.asList(
            "Candycomb",
            "Revive Stone",
            "Trap",
            "Decoy",
            "Inflatable Jerry",
            "Defuse Kit",
            "Dungeon Chest Key",
            "Treasure Talisman",
            "Architect's First Draft",
            "Spirit Leap",
            "Healing VIII Splash Potion",
            "Training Weights"
    ));

    public SecretItemEsp() {
        // ESP render event
        MinecraftForge.EVENT_BUS.register(this);

        // Override the renderer for EntityItem so secrets are invisible
        RenderManager rm = mc.getRenderManager();
        RenderItem renderItem = mc.getRenderItem();
        rm.entityRenderMap.put(EntityItem.class,
                new SecretHidingItemRenderer(rm, renderItem));
    }

    /** Shared check: is this entity one of the secret items? */
    public static boolean isSecretItem(Entity entity) {
        if (!(entity instanceof EntityItem)) return false;

        EntityItem itemEntity = (EntityItem) entity;
        ItemStack stack = itemEntity.getEntityItem();
        if (stack == null) return false;

        String name = stack.getDisplayName();
        if (name == null) return false;

        name = EnumChatFormatting.getTextWithoutFormattingCodes(name);
        return name != null && SECRETS.contains(name);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {

        if (!enabled) return; // <--- Only run ESP when enabled

        if (mc.theWorld == null || mc.thePlayer == null) return;


        EntityPlayerSP player = mc.thePlayer;
        List<Entity> entities = mc.theWorld.loadedEntityList;

        for (Entity entity : entities) {
            if (!isSecretItem(entity)) continue;      // only our secrets

            double d = entity.getDistanceToEntity(player);
            if (d >= 20.0) continue;

            // Color: far = red, close = green
            float r = d > 3.5 ? 1.0f : 0.0f;
            float g = d > 3.5 ? 0.0f : 1.0f;
            float b = 0.0f;

            // Match CT offsets: (x - 0.1, y, z - 0.2, 0.5, 0.5)
            double x = interp(entity.lastTickPosX, entity.posX, event.partialTicks) - 0.1;
            double y = interp(entity.lastTickPosY, entity.posY, event.partialTicks);
            double z = interp(entity.lastTickPosZ, entity.posZ, event.partialTicks) - 0.2;

            // Solid box, through walls
            drawInnerEspBox(x, y, z, 0.5, 0.5, r, g, b, 1.0f);
        }
    }

    private static double interp(double last, double now, float partialTicks) {
        return last + (now - last) * partialTicks;
    }

    private static void drawInnerEspBox(double x, double y, double z,
                                        double w, double h,
                                        float r, float g, float b, float alphaFill) {

        RenderManager rm = mc.getRenderManager();

        double camX = rm.viewerPosX;
        double camY = rm.viewerPosY;
        double camZ = rm.viewerPosZ;

        double rx = x - camX;
        double ry = y - camY;
        double rz = z - camZ;

        AxisAlignedBB box = new AxisAlignedBB(
                rx,        ry,        rz,
                rx + w,    ry + h,    rz + w
        );

        GlStateManager.pushMatrix();

        // DEPTH OFF -> renders through walls
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                1, 0
        );

        // Solid filled box
        GlStateManager.color(r, g, b, alphaFill);
        drawFilledBox(box);

        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.enableDepth(); // restore
        GlStateManager.popMatrix();
    }

    private static void drawFilledBox(AxisAlignedBB box) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        // Bottom
        wr.pos(minX, minY, minZ).endVertex();
        wr.pos(maxX, minY, minZ).endVertex();
        wr.pos(maxX, minY, maxZ).endVertex();
        wr.pos(minX, minY, maxZ).endVertex();

        // Top
        wr.pos(minX, maxY, minZ).endVertex();
        wr.pos(maxX, maxY, minZ).endVertex();
        wr.pos(maxX, maxY, maxZ).endVertex();
        wr.pos(minX, maxY, maxZ).endVertex();

        // North
        wr.pos(minX, minY, minZ).endVertex();
        wr.pos(maxX, minY, minZ).endVertex();
        wr.pos(maxX, maxY, minZ).endVertex();
        wr.pos(minX, maxY, minZ).endVertex();

        // South
        wr.pos(minX, minY, maxZ).endVertex();
        wr.pos(maxX, minY, maxZ).endVertex();
        wr.pos(maxX, maxY, maxZ).endVertex();
        wr.pos(minX, maxY, maxZ).endVertex();

        // West
        wr.pos(minX, minY, minZ).endVertex();
        wr.pos(minX, minY, maxZ).endVertex();
        wr.pos(minX, maxY, maxZ).endVertex();
        wr.pos(minX, maxY, minZ).endVertex();

        // East
        wr.pos(maxX, minY, minZ).endVertex();
        wr.pos(maxX, minY, maxZ).endVertex();
        wr.pos(maxX, maxY, maxZ).endVertex();
        wr.pos(maxX, maxY, minZ).endVertex();

        tess.draw();
    }

    /** Inner renderer that hides secret items */
    private static class SecretHidingItemRenderer extends RenderEntityItem {

        public SecretHidingItemRenderer(RenderManager renderManager, RenderItem renderItem) {
            super(renderManager, renderItem);
        }

        @Override
        public void doRender(EntityItem entity, double x, double y, double z,
                             float entityYaw, float partialTicks) {

            // If it's a secret item, don't render the spinning model at all
            if (SecretItemEsp.enabled && SecretItemEsp.isSecretItem(entity)) {
                return;
            }

            // Otherwise, render normally
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
        }
    }
}
