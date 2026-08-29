package com.ziyno.ziynoaddons;

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

        public static String SOUND_ID      = "note.pling";
        public static float  VOL           = 1.0f;
        public static float  PITCH         = 2.0f;

        public static int    REPEAT_TICKS  = 4;   
    }

    private boolean playing            = false;
    private int     soundPlays         = 0;
    private int     soundTarget        = 0;

    private boolean coreOpenedThisWorld = false;   
    private boolean triggeredThisWorld   = false;  

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

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent e) {
        if (!Config.ENABLED) return;
        if (e.type == 2) return; 
        if (mc.thePlayer == null || e.message == null) return;

        String raw = e.message.getUnformattedText();
        if (raw == null || raw.isEmpty()) return;

        String rawLower = raw.toLowerCase(Locale.ROOT);

        
        if (raw.contains("The Core entrance is opening!")) {
            coreOpenedThisWorld = true;
            if (Config.DEBUG_LOG) {
                System.out.println("[RelicNotif] Detected core opening message this world.");
            }
            return;
        }

        if (!coreOpenedThisWorld) {
            return;
        }

        if (triggeredThisWorld) {
            return;
        }

        String playerName = mc.thePlayer.getName();
        String ignLower   = playerName.toLowerCase(Locale.ROOT);

        if (!rawLower.contains("picked the")) return;
        if (!rawLower.contains(ignLower))     return;

        if (Config.DEBUG_LOG) {
            System.out.println("[RelicNotif] Trigger from chat: \"" + raw + "\"");
        }

        triggeredThisWorld = true;  
        triggerDings();
    }

    private void triggerDings() {
        this.playing     = true;
        this.soundPlays  = 0;
        this.soundTarget = Math.max(0, Config.REPEAT_TICKS);
    }

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
