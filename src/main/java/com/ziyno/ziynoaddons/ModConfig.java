package com.ziyno.ziynoaddons;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class ModConfig {

    private static Configuration config;

    public static boolean saEnabled       = true;
    public static boolean secretTimer     = true;
    public static boolean ee2Enabled      = true;
    public static boolean pyPingEnabled   = true;
    public static boolean itemEspEnabled  = true;
    public static boolean miningFatiguePingEnabled = true;
    public static boolean splitee2Enabled = true;

    public static boolean leapSpot1Enabled = true;
    public static boolean leapSpot2Enabled = true;
    public static boolean leapSpot3Enabled = true;
    public static boolean leapSpot4Enabled = true;

    public static int melodyX = 0;
    public static int melodyY = 0;
    public static float melodyScale = 2.0f;

    public static int leapX = 0;
    public static int leapY = 0;
    public static float leapScale = 2.0f;

    public static double stormPingStartSeconds = 34.5;
    public static int stormPingCount = 3;

    public static void init(File configFile) {
        config = new Configuration(configFile);
        load();
    }

    public static void load() {
        try {
            config.load();
        } catch (Exception ignored) {}

        saEnabled   = config.get("general", "shadowAssassinEnabled", true).getBoolean();
        secretTimer = config.get("general", "secretTimerEnabled",  true).getBoolean();
        ee2Enabled  = config.get("general", "ee2ToggleEnabled",    true).getBoolean();
        pyPingEnabled  = config.get("general", "pyPingEnabled",    true).getBoolean();
        itemEspEnabled = config.get("general", "itemEspEnabled",      true).getBoolean();
        miningFatiguePingEnabled = config.get("general", "miningFatiguePingEnabled", true).getBoolean();
        splitee2Enabled = config.get("general", "splitee2Enabled", true).getBoolean();

        leapSpot1Enabled = config.get("leapnotif", "spot1Enabled", true).getBoolean();
        leapSpot2Enabled = config.get("leapnotif", "spot2Enabled", true).getBoolean();
        leapSpot3Enabled = config.get("leapnotif", "spot3Enabled", true).getBoolean();
        leapSpot4Enabled = config.get("leapnotif", "spot4Enabled", true).getBoolean();

        melodyX = config.get("melody", "melodyX", 0).getInt();
        melodyY = config.get("melody", "melodyY", 0).getInt();
        melodyScale = (float) config.get("melody", "melodyScale", 2.0).getDouble(2.0);

        leapX = config.get("leapnotif", "leapX", 0).getInt();
        leapY = config.get("leapnotif", "leapY", 0).getInt();
        leapScale = (float) config.get("leapnotif", "leapScale", 2.0).getDouble(2.0);

        stormPingStartSeconds = config.get("stormtimer", "pingStartSeconds", 34.5).getDouble(34.5);
        stormPingCount = config.get("stormtimer", "pingCount", 3).getInt();
        ChatCountTimerStorm.setPingSettings(stormPingStartSeconds, stormPingCount);

        if (config.hasChanged()) {
            config.save();
        }

        DungeonHighlight.shadowAssassinEnabled = saEnabled;
        DeathTickTimer.secretTimerEnabled      = secretTimer;
        PartyLocationAlert.ee2ToggleEnabled    = ee2Enabled;
        StormOneTickTimer.pyPingEnabled        = pyPingEnabled;
        SecretItemEsp.enabled                  = itemEspEnabled;
        MiningFatiguePing.ENABLED              = miningFatiguePingEnabled;
    }

    public static void save() {
        if (config == null) return;

        config.get("general", "shadowAssassinEnabled", saEnabled).set(saEnabled);
        config.get("general", "secretTimerEnabled",  secretTimer).set(secretTimer);
        config.get("general", "ee2ToggleEnabled",    ee2Enabled).set(ee2Enabled);
        config.get("general", "pyPingEnabled",       pyPingEnabled).set(pyPingEnabled);
        config.get("general", "itemEspEnabled",        itemEspEnabled).set(itemEspEnabled);
        config.get("general", "miningFatiguePingEnabled", miningFatiguePingEnabled).set(miningFatiguePingEnabled);
        config.get("general", "splitee2Enabled", splitee2Enabled).set(splitee2Enabled);

        config.get("leapnotif", "spot1Enabled", leapSpot1Enabled).set(leapSpot1Enabled);
        config.get("leapnotif", "spot2Enabled", leapSpot2Enabled).set(leapSpot2Enabled);
        config.get("leapnotif", "spot3Enabled", leapSpot3Enabled).set(leapSpot3Enabled);
        config.get("leapnotif", "spot4Enabled", leapSpot4Enabled).set(leapSpot4Enabled);

        config.get("melody", "melodyX", melodyX).set(melodyX);
        config.get("melody", "melodyY", melodyY).set(melodyY);
        config.get("melody", "melodyScale", melodyScale).set(melodyScale);

        config.get("leapnotif", "leapX", leapX).set(leapX);
        config.get("leapnotif", "leapY", leapY).set(leapY);
        config.get("leapnotif", "leapScale", leapScale).set(leapScale);

        config.get("stormtimer", "pingStartSeconds", stormPingStartSeconds).set(stormPingStartSeconds);
        config.get("stormtimer", "pingCount", stormPingCount).set(stormPingCount);

        config.save();
    }
}
