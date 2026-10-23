package net.twitchcraft.hud;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;
import net.twitchcraft.donation.DonationState;
import net.twitchcraft.donation.DonationTierResolver;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.network.SidebarStatePayload;
import net.twitchcraft.stream.CourtManager;
import net.twitchcraft.stream.Graveyard;
import net.twitchcraft.stream.HouseMode;
import net.twitchcraft.stream.DeathTracker;
import net.twitchcraft.stream.DuoScoreSync;
import net.twitchcraft.stream.StreamStats;
import net.twitchcraft.stream.StreamTargets;
import net.twitchcraft.stream.VoteManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Сервер: шлёт только динамику sidebar. Команды клиент рисует сам. */
public final class ScoreboardHud {
    private int elapsedSeconds;
    private final Map<UUID, Integer> lastSig = new HashMap<>();

    public void updateAll(MinecraftServer server) {
        elapsedSeconds++;
        DuoScoreSync.push(server);
        if (!ConfigManager.get().scoreboard.enabled) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            pushState(player);
        }
    }

    public void refreshAll(MinecraftServer server) {
        if (!ConfigManager.get().scoreboard.enabled) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            pushState(player);
        }
    }

    public void refreshPlayer(MinecraftServer server, ServerPlayer player) {
        if (!ConfigManager.get().scoreboard.enabled) return;
        pushState(player);
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        if (player != null) lastSig.remove(player.getUUID());
    }

    public void clear(MinecraftServer server) {
        elapsedSeconds = 0;
        lastSig.clear();
    }

    private void pushState(ServerPlayer player) {
        TwitchCraftConfig config = ConfigManager.get();
        String name = player.getGameProfile().name();
        StreamStats stats = StreamStats.forPlayer(name);
        var cdMgr = EffectRunner.cooldowns();
        var twitch = TwitchCraftMod.getConnectionManager();
        boolean evilBlock = DeathTracker.isEvilBlocked(name);
        String status = buildStatusLine(config, player);

        int sig = Objects.hash(
                stats.getLastUser(), stats.getLastCommandRu(),
                stats.getGoodPoints(), stats.getEvilPoints(), stats.getChaosMeter(), stats.getKarma(),
                config.stream.houseMode, cdMgr.isPenaltyActive(name),
                cdMgr.penaltySecondsLeft(name), status,
                DeathTracker.totalDeaths(name), DeathTracker.deathsInWindow(name),
                DeathTracker.evilBlockSecondsLeft(name), evilBlock,
                stats.getTopGoodCount(), stats.getTopEvilCount(), stats.getTopActiveCount(),
                stats.getStreakCount(), twitch.isConnected(),
                Math.round(net.twitchcraft.stream.ChatActivity.previewMultiplier(player) * 10));
        Integer prev = lastSig.get(player.getUUID());
        if (prev != null && prev == sig && elapsedSeconds % 3 != 0) return;
        lastSig.put(player.getUUID(), sig);

        SidebarStatePayload.send(player, new SidebarStatePayload(
                safe(stats.getLastUser()),
                safe(stats.getLastCommandRu()),
                stats.getGoodPoints(),
                stats.getEvilPoints(),
                stats.getChaosMeter(),
                config.stream.chaosMeterMax,
                elapsedSeconds,
                config.stream.houseMode,
                !HouseMode.isActive() && cdMgr.isPenaltyActive(name),
                (int) cdMgr.penaltyMultiplier(name),
                cdMgr.penaltySecondsLeft(name),
                status,
                twitch.isConnected(),
                twitch.isConnected() ? twitch.getChannel() : "",
                twitch.isConnected() ? "" : twitch.getLastError(),
                DeathTracker.totalDeaths(name),
                DeathTracker.deathsInWindow(name),
                DeathTracker.evilBlockSecondsLeft(name),
                evilBlock,
                DeathTracker.lastCoordsLine(name),
                safe(stats.getTopGoodViewer()),
                stats.getTopGoodCount(),
                safe(stats.getTopEvilViewer()),
                stats.getTopEvilCount(),
                safe(stats.getTopActiveViewer()),
                stats.getTopActiveCount(),
                safe(stats.getStreakUser()),
                stats.getStreakCount(),
                stats.isStreakGood(),
                stats.getKarma(),
                net.twitchcraft.stream.TwitchNickColors.get(stats.getLastUser())
        ));
    }

    private String buildStatusLine(TwitchCraftConfig config, ServerPlayer player) {
        CourtManager court = CourtManager.get();
        if (court.isActive()) {
            return "§e§lСУД §f" + court.secondsLeft() + "s §c!" + court.guiltyVotes()
                    + " §a!" + court.innocentVotes();
        }
        String bomb = net.twitchcraft.stream.BombManager.get().statusLine();
        if (!bomb.isEmpty()) return bomb;
        String hunt = net.twitchcraft.stream.HuntManager.get().statusLine();
        if (!hunt.isEmpty()) return hunt;
        String bet = net.twitchcraft.stream.BetManager.get().statusLine();
        if (!bet.isEmpty()) return bet;
        VoteManager vote = VoteManager.get();
        if (vote.isActive()) {
            return "§d§lГОЛОС §f" + vote.secondsLeft() + "с  §e!1 §f" + shortOpt(vote, 0)
                    + " §e!2 §f" + shortOpt(vote, 1)
                    + " §e!3 §f" + shortOpt(vote, 2);
        }
        String taunt = net.twitchcraft.stream.StreamerTaunt.get().statusLine();
        if (!taunt.isEmpty()) return taunt;
        String contract = net.twitchcraft.stream.StreamerContract.get().statusLine();
        if (!contract.isEmpty()) return contract;
        String challenge = net.twitchcraft.stream.StreamerChallenge.get().statusLine();
        if (!challenge.isEmpty()) return challenge;
        String shield = net.twitchcraft.stream.StreamerShield.statusLine();
        if (!shield.isEmpty()) return shield;
        String parkour = net.twitchcraft.stream.ParkourChallenge.statusLine(player);
        if (!parkour.isEmpty()) return parkour;
        String vein = net.twitchcraft.stream.OreVeinEvent.statusLine(player);
        if (!vein.isEmpty()) return vein;
        String size = net.twitchcraft.stream.SizePrank.statusLine();
        if (!size.isEmpty()) return size;
        String sixty = net.twitchcraft.stream.SixSevenRitual.statusLine();
        if (!sixty.isEmpty()) return sixty;
        String morph = net.twitchcraft.stream.MorphManager.get().statusLine();
        if (!morph.isEmpty()) return morph;
        String king = net.twitchcraft.stream.ChatKingManager.get().statusLine();
        if (!king.isEmpty()) return king;
        String upgrade = net.twitchcraft.stream.ItemUpgrade.statusLine();
        if (!upgrade.isEmpty()) return upgrade;
        String cipher = net.twitchcraft.stream.CipherRiddle.statusLine();
        if (!cipher.isEmpty()) return cipher;
        String patron = net.twitchcraft.stream.UnnamedPatron.statusLine();
        if (!patron.isEmpty()) return patron;
        if (config.scoreboard.showGraveyard) {
            var graves = Graveyard.get().recent(1);
            if (!graves.isEmpty()) {
                Graveyard.Entry g = graves.getFirst();
                return "§8☠ §7" + truncate(g.viewer(), 8) + " §c" + g.mobRu();
            }
        }
        if (config.stream.duoStreamMode
                && StreamTargets.onlineStreamers(player.level().getServer()).size() > 1) {
            String duo = "§d♦ §f" + truncate(player.getGameProfile().name(), 12);
            String cd = net.twitchcraft.stream.ChatActivity.compactHint(player);
            return cd.isEmpty() ? duo : duo + "  " + cd;
        }
        if (config.scoreboard.showDonations && config.donations.enabled && DonationState.isConnected()) {
            String name = player.getGameProfile().name();
            String don;
            if (StreamTargets.isTarget(player)) {
                don = "§6✦ §e" + truncate(DonationState.lastDonorFor(name), 10)
                        + " §7" + DonationState.lastAmountFor(name);
            } else {
                don = "§6✦ §e" + truncate(DonationState.lastDonor(), 10) + " §7" + DonationState.lastAmount();
            }
            String cd = net.twitchcraft.stream.ChatActivity.compactHint(player);
            return cd.isEmpty() ? don : don + "  " + cd;
        }
        String cdHint = net.twitchcraft.stream.ChatActivity.sidebarHint(player);
        if (!cdHint.isEmpty()) return cdHint;
        return "";
    }

    private static String shortOpt(VoteManager vote, int index) {
        String label = vote.optionLabelRu(index).replace("!", "");
        return truncate(label, 8) + "§8×§f" + vote.votes(index);
    }

    private static String safe(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null || s.isBlank()) return "-";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
