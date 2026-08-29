package com.example.ziynoaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Locale;

public class RelicNotif extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static final class Config {
        public static boolean ENABLED      = true;
        public static boolean DEBUG_LOG    = false;

        // sound config
        public static String SOUND_ID      = "note.pling";
        public static float  VOL           = 1.0f;
        public static float  PITCH         = 2.0f;

        // how many times to play the ding (one per tick)
        public static int    REPEAT_TICKS  = 4;   // 4 dings by default
    }

    // state
    private boolean playing            = false;
    private int     soundPlays         = 0;
    private int     soundTarget        = 0;

    // gating state
    private boolean coreOpenedThisWorld = false;   // becomes true after "The Core entrance is opening!"
    private boolean triggeredThisWorld   = false;  // only allow one relic ding per world

    // reset on world change
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
        playing = false;
        soundPlays = 0;
        soundTarget = 0;
        coreOpenedThisWorld = false;
        triggeredThisWorld = false;

        if (Config.DEBUG_LOG) {
            System.out.println("[RelicNotif] World load -> reset state");
        }
    }

    // listen to chat (even if cancelled)
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent e) {
        if (!Config.ENABLED) return;
        if (e.type == 2) return; // ignore action bar
        if (mc.thePlayer == null || e.message == null) return;

        String raw = e.message.getUnformattedText();
        if (raw == null || raw.isEmpty()) return;

        String rawLower = raw.toLowerCase(Locale.ROOT);

        // 1) Check for the "Core entrance" gate
        // Once we see this in the world, we are allowed to trigger relic dings
        if (raw.contains("The Core entrance is opening!")) {
            coreOpenedThisWorld = true;
            if (Config.DEBUG_LOG) {
                System.out.println("[RelicNotif] Detected core opening message this world.");
            }
            return;
        }

        // 2) If core hasn't opened yet, do nothing
        if (!coreOpenedThisWorld) {
            return;
        }

        // 3) If we've already triggered once this world, don't trigger again
        if (triggeredThisWorld) {
            return;
        }

        // 4) Check for relic pickup line containing both IGN and "picked the"
        String playerName = mc.thePlayer.getName();
        String ignLower   = playerName.toLowerCase(Locale.ROOT);

        // must contain both the player's IGN and "picked the" (e.g. "Player picked the Red Relic")
        if (!rawLower.contains("picked the")) return;
        if (!rawLower.contains(ignLower))     return;

        if (Config.DEBUG_LOG) {
            System.out.println("[RelicNotif] Trigger from chat: \"" + raw + "\"");
        }

        triggeredThisWorld = true;  // only allow this once per world
        triggerDings();
    }

    private void triggerDings() {
        this.playing     = true;
        this.soundPlays  = 0;
        this.soundTarget = Math.max(0, Config.REPEAT_TICKS);
    }

    // play the sound each tick until we've hit the target count
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!playing) return;
        if (mc.thePlayer == null) return;

        if (soundPlays < soundTarget) {
            mc.thePlayer.playSound(Config.SOUND_ID, Config.VOL, Config.PITCH);
            soundPlays++;
        } else {
            playing = false;
            if (Config.DEBUG_LOG) {
                System.out.println("[RelicNotif] Completed multi-ding (" + soundTarget + " plays)");
            }
        }
    }
}
