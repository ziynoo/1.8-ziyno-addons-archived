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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.ChatComponentText;

import java.util.Locale;

public class MelodyAlert extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static final class Config {
        public static boolean ENABLED   = true;
        public static boolean DEBUG_LOG = true;   // set true while testing
        public static String  SOUND_ID  = "note.harp";
        public static float   VOL       = 1.0f;
        public static float   PITCH     = 2.0f;
    }

    // --- state ---
    public boolean trackedPing = false;
    private boolean showBanner   = false;
    public  boolean windowOpen   = false; // set true when Goldor starts
    private String  bannerText   = "";
    private String  melodyPlayer = null;
    private double  furthestStep = 0.0;   // 1..4 (1 = 1/4, 4 = 4/4)

    // Reset on world change
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
        resetAll();
        if (Config.DEBUG_LOG) System.out.println("[Melody] World load -> cleared state");
    }

    private void resetAll() {
        showBanner   = false;
        windowOpen   = false;
        bannerText   = "";
        melodyPlayer = null;
        furthestStep = 0.0;
    }

    private void resetMelody() {
        showBanner   = false;
        bannerText   = "";
        melodyPlayer = null;
        furthestStep = 0.0;
        if (Config.DEBUG_LOG) System.out.println("[Melody] resetMelody()");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent e) {
        if (!Config.ENABLED) return;
        if (e.type == 2) return; // ignore action bar
        if (mc.thePlayer == null) return;

        // Raw messages
        final String formatted   = e.message.getFormattedText();
        final String placeholder = e.message.getUnformattedText();
        final String plain = stripSection(formatted);
        final String norm  = normalize(plain);
        if (placeholder.contains("[BOSS] Goldor: Who dares trespass into my domain?")) {
            windowOpen = true;
            resetMelody();
            if (Config.DEBUG_LOG) System.out.println("[Melody] Goldor line -> windowOpen = true");
            return;
        }
        if (formatted.contains("The Core entrance is opening!")) {
            windowOpen = false;
            resetMelody();
            if (Config.DEBUG_LOG) System.out.println("[Melody] Core entrance -> windowOpen = false");
            return;
        }
        if (!windowOpen) {
            return; // only care about messages during P3
        }

        if (melodyPlayer != null
                && norm.contains(cleanIgn(melodyPlayer))
                && norm.contains("activated")) {

            if (Config.DEBUG_LOG) {
                System.out.println("[Melody] hide HUD on term line: " + placeholder);
            }

            trackedPing = false;
            resetMelody();
            return;
        }
        if (placeholder.contains("(7/7)") || placeholder.contains("(8/8)"))
        {
            trackedPing = false;
            resetMelody();
            return;
        }
        // Ignore our own chat for Melody detection (like CT)
        String selfName = mc.thePlayer.getName();
        if (formatted.contains(selfName)) {
            return;
        }


        if (Config.DEBUG_LOG) System.out.println("[Melody] norm=\"" + norm + "\"");
        if (Config.DEBUG_LOG) System.out.println("[Melody] melodylPlayer=\"" + melodyPlayer + "\"");
        if (Config.DEBUG_LOG) System.out.println("[Melody] melodylPlayerStripped=\"" + cleanIgn(melodyPlayer) + "\"");



        final int after = findAfterPartyPrefix(norm);
        if (after < 0) return; // not a party message

        String rest = norm.substring(after).trim(); // "[RANK] Name: msg" or "Name: msg"

        // Strip rank tags
        while (rest.startsWith("[")) {
            int close = rest.indexOf(']');
            if (close < 0) break;
            rest = rest.substring(close + 1).trim();
        }

        // Split "IGN: message"
        int colon = rest.indexOf(':');
        if (colon <= 0 || colon >= rest.length() - 1) return;

        String preName = rest.substring(0, colon).trim();
        String message = rest.substring(colon + 1).trim();

        int lastBracket = preName.lastIndexOf(']');
        if (lastBracket >= 0 && lastBracket + 1 < preName.length()) {
            preName = preName.substring(lastBracket + 1).trim();
        }
        String name = preName;
        if (name == null || name.trim().isEmpty()) return;
        if (name.trim().equalsIgnoreCase("Ziyno")) return; // optional ignore
        //test if this works idk
        if (cleanIgn(name).equalsIgnoreCase(Minecraft.getMinecraft().thePlayer.getName())) {
            return;
        }

        String lower = message.toLowerCase(Locale.ROOT);
        if (Config.DEBUG_LOG) {
            System.out.println("[Melody] name=" + name + " msg=\"" + message + "\" lower=\"" + lower + "\"");
        }

        Progress prog = detectProgress(lower);
        if (prog == null) return; // no Melody-ish text found

        if (prog.stepValue >= furthestStep || furthestStep == 0.0) {
            furthestStep = prog.stepValue;
            melodyPlayer = name;
            bannerText   = stripSection(name) + " has Melody! " + prog.display;
            showBanner   = true;

            if (Config.DEBUG_LOG) {
                System.out.println("[Melody] SHOW HUD -> " + bannerText + " (step=" + prog.stepValue + ")");
            }
            if (trackedPing == false){
                trackedPing = true;
                mc.thePlayer.playSound(Config.SOUND_ID, Config.VOL, Config.PITCH);
            }
        }
    }

    private static class Progress {
        final double stepValue;  // 1..4
        final String display;    // "1/4", "2/4", ...

        Progress(double s, String d) {
            this.stepValue = s;
            this.display   = d;
        }
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

    /**
     * Look for 1/4 / 2/4 / 3/4 / 4/4 or 25/50/75/100 in the message.
     * Also treat "completed terminal" as 4/4.
     */
    private static Progress detectProgress(String lower) {
        if (lower.contains("4/4") || lower.contains("100%") || lower.contains("completed terminal")) {
            return new Progress(4.0, "4/4");
        }
        if (lower.contains("3/4") || lower.contains("75%")) {
            return new Progress(3.0, "3/4");
        }
        if (lower.contains("2/4") || lower.contains("50%")) {
            return new Progress(2.0, "2/4");
        }
        if (lower.contains("1/4") || lower.contains("25%")) {
            return new Progress(1.0, "1/4");
        }
        return null;
    }

    // --- Render banner in the center ---

    @SubscribeEvent
    public void onRenderText(RenderGameOverlayEvent.Text e) {
        if (!showBanner || mc.theWorld == null || mc.thePlayer == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRendererObj;

        final int screenW = sr.getScaledWidth();
        final int screenH = sr.getScaledHeight();
        final float scale = ModConfig.melodyScale;     // your chosen HUD scale
        final int defaultYOffset = -50;

        // 1) plain text (for width)
        String plain = (bannerText == null) ? "" : bannerText.replaceAll("§.", "");
        if (plain.isEmpty()) return;

        // 2) figure out who the melody player is (for class color)
        String ign = cleanIgn(melodyPlayer);

        String firstColor = "\u00A7d"; // default = pink if no class found
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

        String progress = null;
        String beforeProgress = plain;

        int lastSpace = plain.lastIndexOf(' ');
        if (lastSpace != -1 && lastSpace + 1 < plain.length()) {
            String lastToken = plain.substring(lastSpace + 1); // e.g. "1/4"
            if (lastToken.equals("1/4") || lastToken.equals("2/4") || lastToken.equals("3/4") || lastToken.equals("4/4")) {
                progress = lastToken;
                beforeProgress = plain.substring(0, lastSpace).trim(); // everything before the number
            }
        }

        // 4) color the body (everything before the progress) all pink
        String pinkBody = beforeProgress.isEmpty()
                ? ""
                : beforeProgress.replace(" ", " \u00A7d"); // first word will get firstColor in a sec

        // 5) color the progress number specially
        String coloredProgress = "";
        if (progress != null && progress.length() >= 3) { // "X/4"
            char digit = progress.charAt(0); // '1', '2', '3', '4'
            String digitColor = "\u00A7d";   // default pink

            if (digit == '1') {
                digitColor = "\u00A74"; // dark red
            } else if (digit == '2') {
                digitColor = "\u00A7e"; // yellow
            } else if (digit == '3') {
                digitColor = "\u00A7a"; // green
            }

            // digit colored, rest of token ("/4") back to pink
            String rest = progress.substring(1); // "/4"
            coloredProgress = " " + digitColor + digit + "\u00A7d" + rest;
        }

        // 6) put the firstColor just before the full string
        String render = firstColor + pinkBody + coloredProgress;

        // Still measure centering based on plain text
        int tw = fr.getStringWidth(plain);

        // 7) Decide screen-space pixel position (top-left of the text)
        int pixelX, pixelY;

        if (ModConfig.melodyX == 0 && ModConfig.melodyY == 0) {
            // --- default: center like before ---
            int centerX = screenW / 2;
            int centerY = screenH / 2 + defaultYOffset;

            int textWidthPx = (int) (tw * scale);
            pixelX = centerX - textWidthPx / 2;
            pixelY = centerY;
        } else {
            // --- use saved pixel coords from /melodypos ---
            pixelX = ModConfig.melodyX;
            pixelY = ModConfig.melodyY;
        }

        // 8) Apply scale and draw
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0f);

        float drawX = pixelX / scale;
        float drawY = pixelY / scale;

        fr.drawString(render, (int) drawX, (int) drawY, 0xFFFFFF, true);

        GlStateManager.popMatrix();
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
}
