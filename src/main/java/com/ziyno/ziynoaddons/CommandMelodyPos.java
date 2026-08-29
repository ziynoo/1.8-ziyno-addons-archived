package com.ziyno.ziynoaddons;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandMelodyPos extends CommandBase {

    @Override
    public String getCommandName() {
        return "melodypos"; 
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/melodypos <x> <y> or /melodypos reset";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; 
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
            ModConfig.melodyX = 0;
            ModConfig.melodyY = 0;
            ModConfig.save();

            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.LIGHT_PURPLE +
                            "Melody HUD position reset to default (center)."
            ));
            return;
        }

        if (args.length != 2) {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "Usage: /melodypos <x> <y> or /melodypos reset"
            ));
            return;
        }

        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);

            ModConfig.melodyX = x;
            ModConfig.melodyY = y;
            ModConfig.save();

            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.LIGHT_PURPLE +
                            "Melody HUD position set to x=" + x + ", y=" + y
            ));
        } catch (NumberFormatException ex) {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "Coordinates must be integers."
            ));
        }
    }
}
