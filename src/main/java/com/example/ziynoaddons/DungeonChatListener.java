package com.example.ziynoaddons;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DungeonChatListener {
    private static final String MORT_MAP_LINE = "Here, I found this map when I first entered the dungeon.";

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String msg = event.message.getUnformattedText();
        if (msg.contains("Here, I found this map when I first entered the dungeon.")) {
            DungeonClasses.updateFromScoreboard();
        }

    }
}