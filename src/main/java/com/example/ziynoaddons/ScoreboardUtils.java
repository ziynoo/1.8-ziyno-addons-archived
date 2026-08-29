package com.example.ziynoaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;

import java.util.*;

public class ScoreboardUtils {

    /**
     * Gets the sidebar scoreboard lines as a top -> bottom List of Strings.
     */
    public static List<String> getSidebarLines() {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.theWorld == null) return Collections.emptyList();

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) return Collections.emptyList();

        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return Collections.emptyList();

        Collection<Score> scores = scoreboard.getSortedScores(objective);

        List<Score> validScores = new ArrayList<>();
        for (Score score : scores) {
            if (score == null) continue;
            String name = score.getPlayerName();
            if (name == null || name.startsWith("#")) continue;
            validScores.add(score);
        }

        if (validScores.size() > 15) {
            validScores = validScores.subList(0, 15);
        }

        Collections.reverse(validScores);

        List<String> lines = new ArrayList<>();
        for (Score score : validScores) {
            String playerName = score.getPlayerName();
            ScorePlayerTeam team = scoreboard.getPlayersTeam(playerName);
            String line = ScorePlayerTeam.formatPlayerName(team, playerName);
            lines.add(line);
        }

        return lines;
    }

    /** Same but with formatting codes removed, similar to ChatLib.removeFormatting / removeUnicode */
    public static List<String> getSidebarLinesStripped() {
        List<String> raw = getSidebarLines();
        List<String> stripped = new ArrayList<>();
        for (String line : raw) {
            stripped.add(EnumChatFormatting.getTextWithoutFormattingCodes(line));
        }
        return stripped;
    }
}
