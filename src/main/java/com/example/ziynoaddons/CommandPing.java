package com.example.ziynoaddons;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandPing extends CommandBase {

    @Override
    public String getCommandName() {
        return "ping";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ping <seconds> <count> (example: /ping 34.5 5)";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // client-side
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.AQUA + "Storm ping: " +
                            EnumChatFormatting.YELLOW + ModConfig.stormPingStartSeconds + "s, " +
                            EnumChatFormatting.YELLOW + ModConfig.stormPingCount + " pings"
            ));
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GRAY + "Usage: " + getCommandUsage(sender)
            ));
            return;
        }

        if (args.length != 2) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: " + getCommandUsage(sender)));
            return;
        }

        double seconds;
        int count;

        try {
            seconds = Double.parseDouble(args[0]);
            count = Integer.parseInt(args[1]);
        } catch (Exception e) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Invalid numbers. Example: /ping 34.5 5"));
            return;
        }

        if (seconds < 0) seconds = 0;
        if (count < 0) count = 0;
        if (count > 50) count = 50;

        // apply live
        ChatCountTimerStorm.setPingSettings(seconds, count);

        // save to config
        ModConfig.stormPingStartSeconds = seconds;
        ModConfig.stormPingCount = count;
        ModConfig.save();

        sender.addChatMessage(new ChatComponentText(
                EnumChatFormatting.GREEN + "Set Storm ping to start at " +
                        EnumChatFormatting.YELLOW + seconds +
                        EnumChatFormatting.GREEN + "s for " +
                        EnumChatFormatting.YELLOW + count +
                        EnumChatFormatting.GREEN + " pings."
        ));
    }
}
