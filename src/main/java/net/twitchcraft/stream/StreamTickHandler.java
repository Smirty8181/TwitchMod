package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.donation.WheelTimeFreeze;
import net.twitchcraft.hud.BattleBarManager;
import net.twitchcraft.hud.ChaosBossBar;
import net.twitchcraft.hud.ScoreboardHud;
import net.twitchcraft.stream.StreamTargets;

public final class StreamTickHandler {
    private static final ScoreboardHud HUD = new ScoreboardHud();
    private static final ChaosBossBar CHAOS_BAR = new ChaosBossBar();
    private static final BattleBarManager BATTLE_BAR = new BattleBarManager();
    private static int tickCounter;

    private StreamTickHandler() {}

    public static void tick(MinecraftServer server) {
        tickCounter++;
        ServerTaskQueue.tick();
        TwitchCraftMod.getConnectionManager().tick(server);
        TwitchCraftMod.getDonationPoller().tickPending(server);
        VoteManager.get().tick(server);
        CourtManager.get().tick(server);
        HuntManager.get().tick(server);
        BetManager.get().tick(server);
        BombManager.get().tick(server);
        ChatKingManager.get().tick(server);
        StreamerShield.tick(server);
        PartnerHold.tick(server);
        StreamerChallenge.get().tick(server);
        StreamerContract.get().tick(server);
        StreamerTaunt.get().tick(server);
        MorphManager.get().tick(server);
        if (GuideNpcManager.get().isActive()) {
            GuideNpcManager.get().tick(server);
        }
        SizePrank.tick(server);
        SixSevenRitual.tick(server);
        DuoTether.tick(server);
        DuoSleep.tick(server);
        OreVeinEvent.tick(server);
        ParkourChallenge.tick(server);
        ItemUpgrade.tick(server);
        WorldEncounter.tick(server);
        if (tickCounter % 20 == 0) {
            ViewerKarma.tick(server);
            ChatActivity.tick();
            StreamChronicle.tick();
            ChatMobBudget.tick(server);
            PartnerPing.tick(server);
            CampaignJourney.tick(server);
            StreamerCharge.tick(server);
            UnnamedPatron.tick(server);
            CipherRiddle.tick(server);
            RandomEventScheduler.tick(server);
            ShowdownDirector.tick(server);
            HUD.updateAll(server);
            var cfg = ConfigManager.get().stream;
            for (ServerPlayer target : StreamTargets.onlineStreamers(server)) {
                if (cfg.battleBarEnabled) {
                    BATTLE_BAR.update(target);
                    CHAOS_BAR.remove(target);
                } else if (cfg.chaosMeterEnabled && cfg.chaosBossBar) {
                    CHAOS_BAR.update(target);
                    BATTLE_BAR.remove(target);
                } else {
                    BATTLE_BAR.remove(target);
                    CHAOS_BAR.remove(target);
                }
            }
        }
    }

    public static void onServerStop(MinecraftServer server) {
        WheelTimeFreeze.forceUnfreeze(server);
        ServerTaskQueue.clear();
        net.twitchcraft.donation.PendingDonationQueue.clear();
        HUD.clear(server);
        CHAOS_BAR.clear();
        BATTLE_BAR.clear();
        GhostMobRegistry.cleanup();
        ChatMobBudget.clear();
        PartnerPing.clear();
        OreVeinEvent.clear();
        ParkourChallenge.clear();
        WorldEncounter.clear();
        ChatNewbie.reset();
        StreamTargets.resolve(server).ifPresent(p -> SizePrank.clear(server, p, false));
    }

    public static ScoreboardHud hud() {
        return HUD;
    }

    public static BattleBarManager battleBar() {
        return BATTLE_BAR;
    }

    public static void initBattle() {
        BATTLE_BAR.resetAll();
        RandomEventScheduler.init();
        WorldEncounter.init();
    }
}
