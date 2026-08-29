package com.ziyno.ziynoaddons;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.ClientCommandHandler;

@Mod(modid = ZiynoAddons.MODID, version = ZiynoAddons.VERSION)
public class ZiynoAddons {
    public static final String MODID = "ziyno funny mods";
    public static final String NAME = "ziyno funny mods";
    public static final String VERSION = "1.0";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.init(event.getSuggestedConfigurationFile());
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new WitherHitboxRenderer());
        MinecraftForge.EVENT_BUS.register(new ChatCountdownNecron());
        MinecraftForge.EVENT_BUS.register(new ChatCountTimerStorm());
        MinecraftForge.EVENT_BUS.register(new ChatCountTimerTerms());
        MinecraftForge.EVENT_BUS.register(new DungeonHighlight());
        MinecraftForge.EVENT_BUS.register(new StormOneTickTimer());
        MinecraftForge.EVENT_BUS.register(new RelicCountdownTimer());
        MinecraftForge.EVENT_BUS.register(new ChatCountdownTermsBW());
        MinecraftForge.EVENT_BUS.register(new DungeonLagTimer());
        MinecraftForge.EVENT_BUS.register(new PartyLocationAlert());
        MinecraftForge.EVENT_BUS.register(new MelodyAlert());
        MinecraftForge.EVENT_BUS.register(new MiningSpeedBoostTimer());
        MinecraftForge.EVENT_BUS.register(new ScoreboardUtils());
        MinecraftForge.EVENT_BUS.register(new DungeonClasses());
        MinecraftForge.EVENT_BUS.register(new DungeonChatListener());
        MinecraftForge.EVENT_BUS.register(new MiningFatiguePing());
        MinecraftForge.EVENT_BUS.register(new RelicNotif());
        MinecraftForge.EVENT_BUS.register(new SecretItemEsp());
        MinecraftForge.EVENT_BUS.register(new LeapNotif());
        MinecraftForge.EVENT_BUS.register(new AutoParty());
        MinecraftForge.EVENT_BUS.register(new BlockHighlighter());
        MinecraftForge.EVENT_BUS.register(new SheepBoxMod());
        MinecraftForge.EVENT_BUS.register(new HidePlayers());
        MinecraftForge.EVENT_BUS.register(new ExplosiveShotReformat());
        MinecraftForge.EVENT_BUS.register(new SSBlockClicks());

        ClientCommandHandler.instance.registerCommand(new CommandToggle());
        ClientCommandHandler.instance.registerCommand(new CommandMelodyPos());
        ClientCommandHandler.instance.registerCommand(new CommandMelodyScale());
        ClientCommandHandler.instance.registerCommand(new CommandLeapPos());
        ClientCommandHandler.instance.registerCommand(new CommandLeapScale());
        ClientCommandHandler.instance.registerCommand(new CommandAutoParty());
        ClientCommandHandler.instance.registerCommand(new CommandPing());

        DeathTickTimer.init();

        System.out.println("DIRT BLOCK >> " + Blocks.dirt.getUnlocalizedName());
    }
}
