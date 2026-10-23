package net.twitchcraft.config;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.stream.HouseMode;
import net.twitchcraft.stream.StreamTargets;
import net.twitchcraft.stream.StreamTickHandler;

/** Применение конфига на сервере (K-меню, сеть, /reload). */
public final class ConfigApply {
    private ConfigApply() {}

    /** Полная замена (хост / одиночка). */
    public static void apply(MinecraftServer server, TwitchCraftConfig newConfig) {
        boolean wasHouse = ConfigManager.get().stream.houseMode;
        TwitchCraftConfig before = copy(ConfigManager.get());
        ConfigValidator.sanitize(newConfig);
        StreamTargets.syncLegacyTargets(newConfig);
        ConfigManager.set(newConfig);
        applyScoreboard(server, before, newConfig);
        finishApply(server, wasHouse, true);
    }

    /** Сохранение из K-меню: общие настройки (OP) + профиль стримера. */
    public static void apply(MinecraftServer server, ServerPlayer player,
                             TwitchCraftConfig sharedSettings, StreamerProfile localProfile) {
        if (!canEdit(player)) return;

        TwitchCraftConfig current = ConfigManager.get();
        boolean wasHouse = current.stream.houseMode;
        TwitchCraftConfig merged = copy(current);

        if (canEditSharedSettings(player)) {
            ConfigValidator.sanitize(sharedSettings);
            StreamerProfileMerge.applySharedSettings(merged, sharedSettings);
        } else {
            StreamerProfileMerge.applyStreamerUiOnly(merged, sharedSettings);
        }

        boolean twitchChanged = StreamerProfileMerge.upsert(merged, player, localProfile);

        boolean servicesChanged = twitchChanged
                || merged.twitch.autoConnect != current.twitch.autoConnect
                || merged.donations.enabled != current.donations.enabled
                || merged.donations.pollIntervalSeconds != current.donations.pollIntervalSeconds
                || !java.util.Objects.equals(merged.donations.accessToken, current.donations.accessToken)
                || donationProfilesChanged(current, merged);

        StreamTargets.syncLegacyTargets(merged);
        ConfigManager.set(merged);
        applyScoreboard(server, current, merged);
        finishApply(server, wasHouse, servicesChanged);
        TwitchCraftMod.LOGGER.info("TwitchCraft: saved settings for {}", player.getGameProfile().name());
    }

    private static void applyScoreboard(MinecraftServer server, TwitchCraftConfig before, TwitchCraftConfig after) {
        boolean customChanged = !customCommandsEqual(before.customCommands, after.customCommands);
        if (after.scoreboard.enabled) {
            if (!before.scoreboard.enabled || !scoreboardContentEquals(before.scoreboard, after.scoreboard) || customChanged) {
                StreamTickHandler.hud().refreshAll(server);
            }
        } else if (before.scoreboard.enabled) {
            StreamTickHandler.hud().clear(server);
        }
    }

    private static boolean customCommandsEqual(java.util.List<net.twitchcraft.config.CustomCommandDefinition> a,
                                               java.util.List<net.twitchcraft.config.CustomCommandDefinition> b) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        return java.util.Objects.equals(gson.toJson(a), gson.toJson(b));
    }

    private static boolean scoreboardContentEquals(TwitchCraftConfig.ScoreboardSettings a,
                                                   TwitchCraftConfig.ScoreboardSettings b) {
        return a.showCommands == b.showCommands
                && a.showDonations == b.showDonations
                && a.showStats == b.showStats
                && a.showGraveyard == b.showGraveyard
                && a.showFeaturedCommand == b.showFeaturedCommand
                && a.showCommandFeed == b.showCommandFeed
                && a.maxCommandLines == b.maxCommandLines
                && a.pageRotateSeconds == b.pageRotateSeconds
                && java.util.Objects.equals(a.featuredCommandId, b.featuredCommandId);
    }

    private static void finishApply(MinecraftServer server, boolean wasHouse, boolean restartServices) {
        boolean nowHouse = ConfigManager.get().stream.houseMode;
        if (nowHouse) {
            EffectRunner.cooldowns().clearAll();
        }
        if (wasHouse != nowHouse) {
            HouseMode.announce(server, nowHouse);
        }
        if (restartServices) {
            restartServices(server);
        }
    }

    private static TwitchCraftConfig copy(TwitchCraftConfig src) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        return gson.fromJson(gson.toJson(src), TwitchCraftConfig.class);
    }

    public static void restartServices(MinecraftServer server) {
        try {
            TwitchCraftMod.getConnectionManager().connect(server);
        } catch (Throwable t) {
            TwitchCraftMod.LOGGER.error("TwitchCraft: failed to connect Twitch IRC", t);
        }
        TwitchCraftMod.getDonationPoller().stop();
        if (ConfigManager.get().donations.enabled) {
            TwitchCraftMod.getDonationPoller().start(server);
            TwitchCraftMod.LOGGER.info("DonateX poller (re)started");
        }
        TwitchCraftMod.LOGGER.info("TwitchCraft services restarted (Twitch + DonateX)");
    }

    private static boolean donationProfilesChanged(TwitchCraftConfig before, TwitchCraftConfig after) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String b = gson.toJson(before.streamers);
        String a = gson.toJson(after.streamers);
        return !java.util.Objects.equals(b, a);
    }

    public static boolean canEdit(ServerPlayer player) {
        if (player.hasPermissions(2)) return true;
        return StreamTargets.isTarget(player);
    }

    public static boolean canEditSharedSettings(ServerPlayer player) {
        return player.hasPermissions(2);
    }
}
