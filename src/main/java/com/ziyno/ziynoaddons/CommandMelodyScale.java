package com.ziyno.ziynoaddons;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandMelodyScale extends CommandBase {

    @Override
    public String getCommandName() {
        return "melodyscale"; 
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/melodyscale <scale>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "Usage: /melodyscale <number, e.g. 1.5>"
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

        ModConfig.melodyScale = (float) val;
        ModConfig.save();

        String msg = EnumChatFormatting.LIGHT_PURPLE +
                "Melody HUD scale set to " + EnumChatFormatting.AQUA + String.format("%.2f", val);
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; 
    }
}
