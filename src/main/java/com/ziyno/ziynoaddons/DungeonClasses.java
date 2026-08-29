package com.ziyno.ziynoaddons;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DungeonClasses {

    public static final List<String> archers    = new ArrayList<>();
    public static final List<String> berserkers = new ArrayList<>();
    public static final List<String> mages      = new ArrayList<>();
    public static final List<String> healers    = new ArrayList<>();
    public static final List<String> tanks      = new ArrayList<>();

    private static final Pattern CLASS_LINE =
            Pattern.compile("\\[(.)]\\s+([^ ]+)\\s+.*");

    public static void clear() {
        archers.clear();
        berserkers.clear();
        mages.clear();
        healers.clear();
        tanks.clear();
    }

    public static void updateFromScoreboard() {
        clear();

        java.util.List<String> lines = ScoreboardUtils.getSidebarLinesStripped();
        if (lines == null || lines.isEmpty()) return;

        for (String raw : lines) {
            if (raw == null || raw.isEmpty()) continue;

            String line = raw.replaceAll("[^\\x20-\\x7E]", "");

            Matcher m = CLASS_LINE.matcher(line);
            if (!m.matches()) continue;

            char cls = m.group(1).charAt(0); 
            String ign = m.group(2);         

            switch (cls) {
                case 'A':
                    archers.add(ign);
                    break;
                case 'B':
                    berserkers.add(ign);
                    break;
                case 'M':
                    mages.add(ign);
                    break;
                case 'H':
                    healers.add(ign);
                    break;
                case 'T':
                    tanks.add(ign);
                    break;
                default:
                    break;
            }
        }
    }
}
