package com.example.ziynoaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExplosiveShotReformat {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final Pattern EXPLOSIVE_SHOT = Pattern.compile(
            "^Your Explosive Shot hit (\\d+) (?:enemy|enemies) for ([0-9,]+(?:\\.[0-9]+)?) damage[\\p{Punct}]*$"
    );

    private static final DecimalFormat WHOLE_WITH_COMMAS =
            new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat ONE_DECIMAL_WITH_COMMAS =
            new DecimalFormat("#,##0.0", DecimalFormatSymbols.getInstance(Locale.US));

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type == 2) return; // ignore action bar

        String msg = event.message.getUnformattedText();
        if (msg == null) return;

        Matcher m = EXPLOSIVE_SHOT.matcher(msg.trim());
        if (!m.matches()) return;

        int enemies;
        double totalDamage;

        try {
            enemies = Integer.parseInt(m.group(1));
            totalDamage = Double.parseDouble(m.group(2).replace(",", ""));
        } catch (Exception ignored) {
            return;
        }

        if (enemies <= 0) return;

        double perEnemy = totalDamage / enemies;

        EnumChatFormatting dmgColor;
        if (perEnemy > 1_500_000_000d) {
            dmgColor = EnumChatFormatting.GREEN;
        } else if (perEnemy >= 1_000_000_000d) {
            dmgColor = EnumChatFormatting.YELLOW;
        } else {
            dmgColor = EnumChatFormatting.RED;
        }

        String perEnemyStr = (perEnemy < 1000d)
                ? ONE_DECIMAL_WITH_COMMAS.format(perEnemy)
                : WHOLE_WITH_COMMAS.format(perEnemy);

        String newLine =
                EnumChatFormatting.YELLOW.toString() +
                        EnumChatFormatting.BOLD +
                        "Your Explosive Shot did " +
                        dmgColor + EnumChatFormatting.BOLD + perEnemyStr +
                        EnumChatFormatting.YELLOW + EnumChatFormatting.BOLD +
                        " damage per enemy";

        // if you want to NOT hide the original message, set this to false
        event.setCanceled(true);

        if (mc.ingameGUI != null && mc.ingameGUI.getChatGUI() != null) {
            mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(newLine));
        }

        // Send party chat message right after printing
        if (mc.thePlayer != null && mc.theWorld != null) {
            mc.thePlayer.sendChatMessage("/pc My explosive shot did " + perEnemyStr + " damage!");
        }
    }
}
