package com.ziyno.ziynoaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SSBlockClicks {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final String TRIGGER = "[BOSS] Goldor: Who dares trespass into my domain?";
    private static final BlockPos BUTTON_POS = new BlockPos(110, 121, 91);

    private static final long WINDOW_MS = 2000L; 
    private static final int ALLOWED_CLICKS = 3;

    private boolean active = false;
    private long startMs = 0L;
    private int clickCount = 0;

    private boolean preWindowLocked = true;

    private static final boolean SHIFT_OVERRIDES = false;

    public SSBlockClicks() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void armWindow() {
        active = true;
        preWindowLocked = false; 
        startMs = System.currentTimeMillis();
        clickCount = 0;
    }

    private void disarmWindow() {
        active = false;
        clickCount = 0;
        preWindowLocked = false; 
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.message == null) return;
        String msg = event.message.getUnformattedText();
        if (msg == null) return;

        if (msg.contains(TRIGGER)) {
            armWindow();
        }
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (event.world != mc.theWorld) return;
        if (event.pos == null) return;

        if (!event.pos.equals(BUTTON_POS)) return;

        if (event.world.getBlockState(event.pos).getBlock() != Blocks.stone_button) return;

        if (SHIFT_OVERRIDES && mc.thePlayer != null && mc.thePlayer.isSneaking()) return;

        long now = System.currentTimeMillis();

        if (preWindowLocked && !active) {
            event.setCanceled(true);
            return;
        }

        if (!active) {
            return;
        }

        if (now - startMs > WINDOW_MS) {
            disarmWindow();
            return;
        }

        clickCount++;

        if (clickCount > ALLOWED_CLICKS) {
            event.setCanceled(true);
        }
    }
}
