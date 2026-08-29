package com.ziyno.ziynoaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CommandToggle extends CommandBase {

    @Override
    public String getCommandName() {
        return "toggle"; 
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/toggle <sa | secrettimer | mageterm | splitee2 | pyping | itemesp | miningfatigue | leapnotif>";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("t");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "sa": {
                if (args.length != 1) { sendUsage(sender); return; }

                DungeonHighlight.shadowAssassinEnabled =
                        !DungeonHighlight.shadowAssassinEnabled;
                boolean state = DungeonHighlight.shadowAssassinEnabled;

                ModConfig.saEnabled = state;
                ModConfig.save();

                String msg = EnumChatFormatting.DARK_PURPLE + "" +
                        EnumChatFormatting.LIGHT_PURPLE +
                        "Shadow Assassin highlight " +
                        (state ? "enabled." : "disabled.");
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
                break;
            }

            case "secrettimer": {
                if (args.length != 1) { sendUsage(sender); return; }

                DeathTickTimer.secretTimerEnabled =
                        !DeathTickTimer.secretTimerEnabled;
                boolean state = DeathTickTimer.secretTimerEnabled;

                ModConfig.secretTimer = state;
                ModConfig.save();

                String msg = EnumChatFormatting.DARK_PURPLE + "" +
                        EnumChatFormatting.LIGHT_PURPLE +
                        "Secret Spawn Timer " +
                        (state ? "enabled." : "disabled.");
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
                break;
            }

            case "mageterm": {
                if (args.length != 1) { sendUsage(sender); return; }

                PartyLocationAlert.ee2ToggleEnabled =
                        !PartyLocationAlert.ee2ToggleEnabled;
                boolean state = PartyLocationAlert.ee2ToggleEnabled;

                ModConfig.ee2Enabled = state;
                ModConfig.save();

                String msg = EnumChatFormatting.DARK_PURPLE + "" +
                        EnumChatFormatting.LIGHT_PURPLE +
                        "Mage Term notification " +
                        (state ? "enabled." : "disabled.");
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
                break;
            }

            case "splitee2": {
                if (args.length != 1) { sendUsage(sender); return; }

                ModConfig.splitee2Enabled = !ModConfig.splitee2Enabled;
                boolean state = ModConfig.splitee2Enabled;
                ModConfig.save();

                String msg = EnumChatFormatting.DARK_PURPLE + "" +
                        EnumChatFormatting.LIGHT_PURPLE +
                        "Split EE2 " +
                        (state ? "enabled." : "disabled.");

                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
                break;
            }

            case "pyping": {
                if (args.length != 1) { sendUsage(sender); return; }

                StormOneTickTimer.pyPingEnabled =
                        !StormOneTickTimer.pyPingEnabled;
                boolean state = StormOneTickTimer.pyPingEnabled;

                ModConfig.pyPingEnabled = state;
                ModConfig.save();

                String msg = EnumChatFormatting.DARK_PURPLE + "" +
                        EnumChatFormatting.LIGHT_PURPLE +
                        "PY Ping notification " +
                        (state ? "enabled." : "disabled.");
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
                break;
            }

            case "itemesp": {
                if (args.length != 1) { sendUsage(sender); return; }

                SecretItemEsp.enabled = !SecretItemEsp.enabled;
                boolean state = SecretItemEsp.enabled;

                ModConfig.itemEspEnabled = state;
                ModConfig.save();

                String msg = EnumChatFormatting.DARK_PURPLE + "" +
                        EnumChatFormatting.LIGHT_PURPLE +
                        "Secret Item ESP " +
                        (state ? "enabled." : "disabled.");
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
                break;
            }

            case "miningfatigue": {
                if (args.length != 1) { sendUsage(sender); return; }

                MiningFatiguePing.ENABLED = !MiningFatiguePing.ENABLED;
                boolean state = MiningFatiguePing.ENABLED;

                ModConfig.miningFatiguePingEnabled = state;
                ModConfig.save();

                String msg = EnumChatFormatting.DARK_PURPLE + "" +
                        EnumChatFormatting.LIGHT_PURPLE +
                        "Mining Fatigue ping " +
                        (state ? "enabled." : "disabled.");
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
                break;
            }

            case "leapnotif": {
                
                if (args.length != 5) {
                    sender.addChatMessage(new ChatComponentText(
                            EnumChatFormatting.RED +
                                    "Usage: /toggle leapnotif <ee2> <ee3> <core> <p5> (each 0 or 1)"
                    ));
                    return;
                }

                Boolean[] bits = new Boolean[4];
                for (int i = 0; i < 4; i++) {
                    String v = args[i + 1];
                    if (!v.equals("0") && !v.equals("1")) {
                        sender.addChatMessage(new ChatComponentText(
                                EnumChatFormatting.RED +
                                        "Usage: /toggle leapnotif <ee2> <ee3> <core> <p5> (each 0 or 1)"
                        ));
                        return;
                    }
                    bits[i] = v.equals("1");
                }

                ModConfig.leapSpot1Enabled = bits[0];
                ModConfig.leapSpot2Enabled = bits[1];
                ModConfig.leapSpot3Enabled = bits[2];
                ModConfig.leapSpot4Enabled = bits[3];

                ModConfig.save();

                String msg = EnumChatFormatting.DARK_PURPLE + "" +
                        EnumChatFormatting.LIGHT_PURPLE +
                        "LeapNotif spots set to: " +
                        EnumChatFormatting.AQUA +
                        (bits[0] ? "1" : "0") + " " +
                        (bits[1] ? "1" : "0") + " " +
                        (bits[2] ? "1" : "0") + " " +
                        (bits[3] ? "1" : "0");

                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
                break;
            }

            default:
                sendUsage(sender);
        }
    }

    private void sendUsage(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText(
                EnumChatFormatting.RED +
                        "Usage: /toggle sa, /toggle secrettimer, /toggle mageterm, /toggle splitee2, /toggle pyping, /toggle itemesp, /toggle miningfatigue, /toggle leapnotif <ee2> <ee3> <core> <p5>"
        ));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
