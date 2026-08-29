package com.ziyno.ziynoaddons;

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
        public static boolean DEBUG_LOG = true;   
        public static String  SOUND_ID  = "note.harp";
        public static float   VOL       = 1.0f;
        public static float   PITCH     = 2.0f;
    }

    public boolean trackedPing = false;
    private boolean showBanner   = false;
    public  boolean windowOpen   = false; 
    private String  bannerText   = "";
    private String  melodyPlayer = null;
    private double  furthestStep = 0.0;   

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
        if (e.type == 2) return; 
        if (mc.thePlayer == null) return;

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
            return; 
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
        
        String selfName = mc.thePlayer.getName();
        if (formatted.contains(selfName)) {
            return;
        }

        if (Config.DEBUG_LOG) System.out.println("[Melody] norm=\"" + norm + "\"");
        if (Config.DEBUG_LOG) System.out.println("[Melody] melodylPlayer=\"" + melodyPlayer + "\"");
        if (Config.DEBUG_LOG) System.out.println("[Melody] melodylPlayerStripped=\"" + cleanIgn(melodyPlayer) + "\"");

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
        if (name == null || name.trim().isEmpty()) return;
        if (name.trim().equalsIgnoreCase("Ziyno")) return; 
        
        if (cleanIgn(name).equalsIgnoreCase(Minecraft.getMinecraft().thePlayer.getName())) {
            return;
        }

        String lower = message.toLowerCase(Locale.ROOT);
        if (Config.DEBUG_LOG) {
            System.out.println("[Melody] name=" + name + " msg=\"" + message + "\" lower=\"" + lower + "\"");
        }

        Progress prog = detectProgress(lower);
        if (prog == null) return; 

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
        final double stepValue;  
        final String display;    

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
            return m.group();          
        }
        return s.trim();               
    }

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

    @SubscribeEvent
    public void onRenderText(RenderGameOverlayEvent.Text e) {
        if (!showBanner || mc.theWorld == null || mc.thePlayer == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRendererObj;

        final int screenW = sr.getScaledWidth();
        final int screenH = sr.getScaledHeight();
        final float scale = ModConfig.melodyScale;     
        final int defaultYOffset = -50;

        String plain = (bannerText == null) ? "" : bannerText.replaceAll("§.", "");
        if (plain.isEmpty()) return;

        String ign = cleanIgn(melodyPlayer);

        String firstColor = "\u00A7d"; 
        if (DungeonClasses.mages.contains(ign)) {
            firstColor = "\u00A7b";   
        } else if (DungeonClasses.archers.contains(ign)) {
            firstColor = "\u00A76";   
        } else if (DungeonClasses.berserkers.contains(ign)) {
            firstColor = "\u00A74";   
        } else if (DungeonClasses.tanks.contains(ign)) {
            firstColor = "\u00A72";   
        } else if (DungeonClasses.healers.contains(ign)) {
            firstColor = "\u00A7d";   
        }

        String progress = null;
        String beforeProgress = plain;

        int lastSpace = plain.lastIndexOf(' ');
        if (lastSpace != -1 && lastSpace + 1 < plain.length()) {
            String lastToken = plain.substring(lastSpace + 1); 
            if (lastToken.equals("1/4") || lastToken.equals("2/4") || lastToken.equals("3/4") || lastToken.equals("4/4")) {
                progress = lastToken;
                beforeProgress = plain.substring(0, lastSpace).trim(); 
            }
        }

        String pinkBody = beforeProgress.isEmpty()
                ? ""
                : beforeProgress.replace(" ", " \u00A7d"); 

        String coloredProgress = "";
        if (progress != null && progress.length() >= 3) { 
            char digit = progress.charAt(0); 
            String digitColor = "\u00A7d";   

            if (digit == '1') {
                digitColor = "\u00A74"; 
            } else if (digit == '2') {
                digitColor = "\u00A7e"; 
            } else if (digit == '3') {
                digitColor = "\u00A7a"; 
            }

            String rest = progress.substring(1); 
            coloredProgress = " " + digitColor + digit + "\u00A7d" + rest;
        }

        String render = firstColor + pinkBody + coloredProgress;

        int tw = fr.getStringWidth(plain);

        int pixelX, pixelY;

        if (ModConfig.melodyX == 0 && ModConfig.melodyY == 0) {
            
            int centerX = screenW / 2;
            int centerY = screenH / 2 + defaultYOffset;

            int textWidthPx = (int) (tw * scale);
            pixelX = centerX - textWidthPx / 2;
            pixelY = centerY;
        } else {
            
            pixelX = ModConfig.melodyX;
            pixelY = ModConfig.melodyY;
        }

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
