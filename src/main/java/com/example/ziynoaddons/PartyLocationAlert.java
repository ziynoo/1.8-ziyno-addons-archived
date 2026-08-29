package com.example.ziynoaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PartyLocationAlert extends Gui {

    public static String name2;
    public static boolean ee2ToggleEnabled = true;
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "ee2", "high ee2", "highee2", "early enter 2", "early entry 2",
            "ee3", "early enter 3", "early entry 3",
            "split ee2", "splitee2", "mage term",

            // safespot variants
            "2 safespot", "safespot 2", "s2 safespot", "safespot s2",
            "3 safespot", "safespot 3", "s3 safespot", "safespot s3", "ee2 safespot", "safepsot ee2",
            "ee3 safespot", "safespot ee3", "freaky ee3", "freak ee",

            "core",
            "tunnel"
    ));

    public static final class Config {
        public static boolean ENABLED      = true;
        public static boolean DEBUG_LOG    = false;
        public static String  SOUND_ID     = "note.pling";
        public static int     REPEAT_TICKS = 25;      // 20 ticks ~= 1s
        public static float   VOL          = 1.0f;
        public static float   PITCH        = 2.0f;
    }

    private boolean showBanner = false;
    public boolean windowOpen = false;
    private String bannerText = "";
    private int soundPlays = 0;
    private int soundPlaysTarget = 0;

    private final Set<String> triggeredLabelsThisWorld = new HashSet<>();
    private int gateDestroyedCount = 0;

    // Reset on world change
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
        triggeredLabelsThisWorld.clear();
        showBanner = false;
        windowOpen = false;
        bannerText = "";
        soundPlays = 0;
        soundPlaysTarget = 0;
        gateDestroyedCount = 0;
        if (Config.DEBUG_LOG) System.out.println("[PartyLoc] World load -> cleared per-world triggers/state");
    }

    // Listen last and even if another mod cancels the event
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent e) {
        if (!Config.ENABLED) return;
        if (e.type == 2) return; // ignore action bar


        final String formatted = e.message.getFormattedText();
        String placeholder = e.message.getUnformattedText();

        if (formatted.contains("The gate has been destroyed!")) {
            gateDestroyedCount++;
            if (Config.DEBUG_LOG) {
                System.out.println("[PartyLoc] Gate destroyed count = " + gateDestroyedCount);
            }
        }
        if (placeholder.contains("[BOSS] Goldor: Who dares trespass into my domain?")){
            windowOpen = true;
        }
        if (formatted.contains("The Core entrance is opening!")){
            windowOpen = false;
            return;
        }
        if (!windowOpen){
            return;
        }
        String name1 = Minecraft.getMinecraft().thePlayer.getName();
        if (formatted.contains(name1)){
            if (formatted.contains("ee3 safespot")){
                triggeredLabelsThisWorld.add("s3 safespot");
                return;
            }
            if (formatted.contains("ee3")){
                triggeredLabelsThisWorld.add("ee3");
                return;
            }
            if (formatted.contains("ee2")){
                triggeredLabelsThisWorld.add("ee2");
                return;
            }
            if (formatted.contains("core") || formatted.contains("tunnel") && gateDestroyedCount == 3){
                triggeredLabelsThisWorld.add("core");
                return;
            }
            return;
        }
        final String plain     = stripSection(formatted);
        final String norm      = normalize(plain);

        if (Config.DEBUG_LOG) System.out.println("[PartyLoc] norm=\"" + norm + "\"");

        final int after = findAfterPartyPrefix(norm);
        if (after < 0) return;

        String rest = norm.substring(after).trim();

        while (rest.startsWith("[")) {
            int close = rest.indexOf(']');
            if (close < 0) break;
            rest = rest.substring(close + 1).trim();
        }

        int colon = rest.indexOf(':');
        if (colon <= 0 || colon >= rest.length() - 1) return;

        String preName = rest.substring(0, colon).trim();
        String message = rest.substring(colon + 1).trim();

        int lastBracket = preName.lastIndexOf(']');
        if (lastBracket >= 0 && lastBracket + 1 < preName.length()) {
            preName = preName.substring(lastBracket + 1).trim();
        }
        String name = preName;
        if (name != null && name.trim().equalsIgnoreCase("Ziyno")) return;


        String lower = message.toLowerCase(Locale.ROOT).trim();
        if (lower.isEmpty() || !containsKeyword(lower)) return;

        String label = resolvePlaceLabel(lower);
        if (label == null) return;

        if ("Core".equals(label) && gateDestroyedCount < 3) {
            if (Config.DEBUG_LOG) {
                System.out.println("[PartyLoc] Suppressing Core alert, gates destroyed = " + gateDestroyedCount);
            }
            return;
        }


        String key = label.toLowerCase(Locale.ROOT); // canonical key
        if (triggeredLabelsThisWorld.contains(key)) {
            if (Config.DEBUG_LOG) System.out.println("[PartyLoc] Skipping already-triggered label: " + label);
            return;
        }
        triggeredLabelsThisWorld.add(key);

        name2 = name;

        String cleanBanner = stripSection(name) + " is at " + stripSection(label) + "!";
        trigger(cleanBanner);
    }

    private static boolean containsKeyword(String lower) {
        for (String kw : KEYWORDS) if (lower.contains(kw)) return true;
        return false;
    }

    private static String resolvePlaceLabel(String lower) {
        lower = lower.replaceFirst("^(?:at|inside|in)\\s+", "").trim();

        if (lower.contains("high ee2") || lower.contains("highee2")) return "High EE2";
        if (lower.contains("split ee2") || lower.contains("splitee2") || lower.contains("mage term")) return "Mage Term";
        if (lower.contains("ee3 safespot") || lower.contains("safespot ee3") || lower.contains("freaky ee3") || lower.contains("freak ee") || lower.contains("s3 safespot") || lower.contains("safespot s3") || lower.contains("3 safespot") || lower.contains("safespot 3")) return "S3 Safespot";
        if (lower.contains("ee2 safespot") || lower.contains("safespot ee2") || lower.contains("s2 safespot") || lower.contains("safespot s2") || lower.contains("2 safespot") || lower.contains("safespot 2")) return "S2 Safespot";
        if (lower.contains("ee3") || lower.contains("early enter 3") || lower.contains("early entry 3"))         return "EE3";
        if (lower.contains("ee2") || lower.contains("early enter 2") || lower.contains("early entry 2"))         return "EE2";
        if (lower.contains("core") || lower.contains("tunnel")) return "Core";
        return null;
    }

    private static String stripSection(String s) {
        return (s == null) ? "" : s.replaceAll("§.", "");
    }

    private static String normalize(String s) {
        if (s == null) return "";
        s = s.replace("\u200B","").replace("\u200C","").replace("\u200D","").replace("\uFEFF","");
        s = s.replace('\u00A0',' ').replace('\uFF1A',':');
        s = s.trim().replaceAll("\\s{2,}", " ");
        return s;
    }

    private static int findAfterPartyPrefix(String s) {
        int i = s.indexOf("Party");
        if (i < 0) return -1;
        int j = i + "Party".length();

        while (j < s.length() && Character.isWhitespace(s.charAt(j))) j++;

        if (j < s.length()) {
            char c = s.charAt(j);
            if (c == ':' || c == '>' || c == '\u00BB' || c == '\u203A') j++;
        }

        while (j < s.length() && Character.isWhitespace(s.charAt(j))) j++;

        return (j >= s.length()) ? -1 : j;
    }

    private void trigger(String text) {
        this.bannerText = text;
        this.showBanner = true;
        this.soundPlays = 0;
        this.soundPlaysTarget = Math.max(0, Config.REPEAT_TICKS);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!showBanner || mc.thePlayer == null) return;

        if (soundPlays < soundPlaysTarget) {
            mc.thePlayer.playSound(Config.SOUND_ID, Config.VOL, Config.PITCH);
            soundPlays++;
        } else {
            showBanner = false;
        }
    }

    @SubscribeEvent
    public void onRenderText(RenderGameOverlayEvent.Text e) {
        if (!showBanner || mc.theWorld == null || mc.thePlayer == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRendererObj;

        final int screenW = sr.getScaledWidth();
        final int screenH = sr.getScaledHeight();
        final float scale = 2.0f;
        final int yOffset = -50;
        String render;
        String plain = (bannerText == null) ? "" : bannerText.replaceAll("§.", "");
        if (ee2ToggleEnabled == false && plain.contains("Mage Term")){
            showBanner = false;
            return;
        }
        if (!plain.contains("S3 Safespot") && !plain.contains("S2 Safespot")){
            String ign = cleanIgn(name2);
            String firstColor = "\u00A7e";

            if (DungeonClasses.mages.contains(ign)) {
                firstColor = "\u00A7b";   // aqua
            } else if (DungeonClasses.archers.contains(ign)) {
                firstColor = "\u00A76";   // gold/orange
            } else if (DungeonClasses.berserkers.contains(ign)) {
                firstColor = "\u00A74";   // dark red
            } else if (DungeonClasses.tanks.contains(ign)) {
                firstColor = "\u00A72";   // dark green
            } else if (DungeonClasses.healers.contains(ign)) {
                firstColor = "\u00A7d";   // pink
            }
            String yellowRest = plain.replace(" ", " \u00A7e");
            render = firstColor + yellowRest;
        }
        else{
            String ign = cleanIgn(name2);
            String[] p = plain.trim().split("\\s+");
            if (p.length == 0) return;

            int n = p.length;

            String first = p[0];

            int tailStart = Math.max(1, n - 2);

            String mid = tailStart <= 1 ? "" :
                    String.join(" ", java.util.Arrays.copyOfRange(p, 1, tailStart));

            String tail = String.join(" ", java.util.Arrays.copyOfRange(p, tailStart, n));


            String coloredFirst = "\u00A7e" + first;
            if (DungeonClasses.mages.contains(ign)) {
                coloredFirst = "\u00A7b" + first;   // aqua
            } else if (DungeonClasses.archers.contains(ign)) {
                coloredFirst = "\u00A76" + first;   // gold/orange
            } else if (DungeonClasses.berserkers.contains(ign)) {
                coloredFirst = "\u00A74" + first;   // dark red
            } else if (DungeonClasses.tanks.contains(ign)) {
                coloredFirst = "\u00A72" + first;   // dark green
            } else if (DungeonClasses.healers.contains(ign)) {
                coloredFirst = "\u00A7d" + first;   // pink
            }
            String coloredMid = mid.isEmpty() ? "" : (" \u00A7e" + mid.replace(" ", " \u00A7e")); // yellow
            String coloredTail = tail.isEmpty() ? "" : (" \u00A7a" + tail.replace(" ", " \u00A7a")); // green

            render = (coloredFirst + coloredMid + coloredTail).trim();
        }

        // 3) center using the unformatted width
        int tw = fr.getStringWidth(plain);
        float halfScaled = (tw * scale) / 2.0f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(screenW / 2.0f, screenH / 2.0f + yOffset, 0);
        GlStateManager.scale(scale, scale, 1.0f);

        fr.drawString(render, (int) (-halfScaled / scale), 0, 0xFFFFFF, true);

        GlStateManager.popMatrix();
    }

    private static final Pattern IGN_PATTERN = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private static String cleanIgn(String s) {
        if (s == null) return "";
        Matcher m = IGN_PATTERN.matcher(s);
        if (m.find()) {
            return m.group();          // first IGN-like token
        }
        return s.trim();               // fallback
    }
}
