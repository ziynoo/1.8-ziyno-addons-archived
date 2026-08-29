package com.example.ziynoaddons;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandLeapPos extends CommandBase {

    @Override
    public String getCommandName() {
        return "leappos"; // /leappos x y
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/leappos <x> <y> or /leappos reset";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
            ModConfig.leapX = 0;
            ModConfig.leapY = 0;
            ModConfig.save();

            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.LIGHT_PURPLE +
                            "LeapNotif position reset to default (center)."
            ));
            return;
        }

        if (args.length != 2) {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "Usage: /leappos <x> <y> or /leappos reset"
            ));
            return;
        }

        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);

            ModConfig.leapX = x;
            ModConfig.leapY = y;
            ModConfig.save();

            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.LIGHT_PURPLE +
                            "LeapNotif position set to x=" + x + ", y=" + y
            ));
        } catch (NumberFormatException ex) {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "Coordinates must be integers."
            ));
        }
    }
}
