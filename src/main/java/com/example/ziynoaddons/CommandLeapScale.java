package com.example.ziynoaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandLeapScale extends CommandBase {

    @Override
    public String getCommandName() {
        return "leapscale"; // /leapscale 2.0
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/leapscale <scale>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "Usage: /leapscale <number, e.g. 2.0>"
            ));
            return;
        }

        double val;
        try {
            val = Double.parseDouble(args[0]);
        } catch (NumberFormatException ex) {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "Invalid number: " + args[0]
            ));
            return;
        }

        if (val < 0.5) val = 0.5;
        if (val > 5.0) val = 5.0;

        ModConfig.leapScale = (float) val;
        ModConfig.save();

        String msg = EnumChatFormatting.LIGHT_PURPLE +
                "LeapNotif scale set to " + EnumChatFormatting.AQUA + String.format("%.2f", val);

        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
    }
}
