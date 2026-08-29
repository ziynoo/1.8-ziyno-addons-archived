package com.example.ziynoaddons;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class AutoParty {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final String INVITE_NEEDLE = "has invited you to join their party!";

    // Leave triggers
    private static final String LEAVE_NEEDLE_1 = "time lost to lag:";
    private static final String LEAVE_NEEDLE_2 = "disconnected and became a ghost.";
    private static final String LEAVE_NEEDLE_3 = "tablecloth"; // + "party"

    // ===== SESSION-ONLY NAMES (RAM ONLY) =====
    private static String[] partyNames = new String[0];

    public static void setPartyNames(String a, String b, String c, String d) {
        partyNames = new String[]{a, b, c, d};
    }

    public static void clearPartyNames() {
        partyNames = new String[0];
    }

    private static boolean isEnabled() {
        return partyNames != null && partyNames.length > 0;
    }

    // ===== QUEUE =====
    private static class QueuedCommand {
        final String cmd;
        final long runAtMs;

        QueuedCommand(String cmd, long runAtMs) {
            this.cmd = cmd;
            this.runAtMs = runAtMs;
        }
    }

    private final Deque<QueuedCommand> queue = new ArrayDeque<>();

    // Optional invite debounce only
    private long lastInviteMs = 0L;
    private String lastInviteIgnLower = null;

    public AutoParty() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void enqueueCommand(String cmd, int minDelayMs, int maxDelayMs) {
        long now = System.currentTimeMillis();

        // chain timing: each command delay is relative to the last queued command
        long base = queue.isEmpty() ? now : Math.max(now, queue.peekLast().runAtMs);

        int delay = ThreadLocalRandom.current().nextInt(minDelayMs, maxDelayMs + 1);
        queue.addLast(new QueuedCommand(cmd, base + delay));
    }

    private String findMatchingIgnInMessageLower(String msgLower) {
        if (partyNames == null) return null;

        for (String name : partyNames) {
            if (name == null) continue;
            String trimmed = name.trim();
            if (trimmed.isEmpty()) continue;

            String ignLower = trimmed.toLowerCase(Locale.ROOT);

            // simple contains check
            if (msgLower.contains(ignLower)) {
                return trimmed; // preserve casing for /p join
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isEnabled()) return;
        if (event.message == null) return;

        String msg = event.message.getUnformattedText();
        if (msg == null) return;

        String msgLower = msg.toLowerCase(Locale.ROOT);

        // ===== Leave triggers =====

        if (msgLower.contains(LEAVE_NEEDLE_3) && msgLower.contains("party")) {
            enqueueCommand("/p leave", 600, 1100);
            enqueueCommand("/joininstance MASTER_CATACOMBS_FLOOR_SEVEN", 1600, 2100); // runs after leave
            return;
        }

        // ===== Join triggers =====
        if (!msgLower.contains(INVITE_NEEDLE)) return;

        String matchedIgn = findMatchingIgnInMessageLower(msgLower);
        if (matchedIgn == null) return;

        // optional: ignore same inviter within 3s
        long now = System.currentTimeMillis();
        String matchedLower = matchedIgn.toLowerCase(Locale.ROOT);
        if (lastInviteIgnLower != null && lastInviteIgnLower.equals(matchedLower) && (now - lastInviteMs) < 3000L) {
            return;
        }
        lastInviteIgnLower = matchedLower;
        lastInviteMs = now;

        enqueueCommand("/p join " + matchedIgn, 600, 1100);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // If cleared (disabled), cancel everything queued
        if (!isEnabled()) {
            queue.clear();
            return;
        }

        if (mc.thePlayer == null) {
            queue.clear();
            return;
        }

        if (queue.isEmpty()) return;

        long now = System.currentTimeMillis();

        // send at most 1 command per tick
        if (now >= queue.peekFirst().runAtMs) {
            mc.thePlayer.sendChatMessage(queue.pollFirst().cmd);
        }
    }
}
