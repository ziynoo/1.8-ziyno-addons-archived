package com.example.ziynoaddons;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandAutoParty extends CommandBase {

    @Override
    public String getCommandName() {
        return "autoparty";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/autoparty <ign1> <ign2> <ign3> <ign4> OR /autoparty clear";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
            AutoParty.clearPartyNames();

            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.LIGHT_PURPLE + "AutoParty list cleared"
            ));
            return;
        }

        if (args.length != 4) {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "Usage: /autoparty <ign1> <ign2> <ign3> <ign4> OR /autoparty clear"
            ));
            return;
        }

        AutoParty.setPartyNames(args[0], args[1], args[2], args[3]);

        sender.addChatMessage(new ChatComponentText(
                EnumChatFormatting.LIGHT_PURPLE + "AutoParty set: " +
                        EnumChatFormatting.AQUA +
                        args[0] + ", " + args[1] + ", " + args[2] + ", " + args[3]
        ));
    }
}
