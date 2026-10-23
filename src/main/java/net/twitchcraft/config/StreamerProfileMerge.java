package net.twitchcraft.config;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Objects;

/** Слияние профилей стримеров на сервере (дуо: каждый сохраняет только себя). */
public final class StreamerProfileMerge {
    private StreamerProfileMerge() {}

    /** @return true если изменились Twitch/DA данные (нужен reconnect) */
    public static boolean upsert(TwitchCraftConfig config, ServerPlayer player, StreamerProfile incoming) {
        if (config.streamers == null) config.streamers = new ArrayList<>();
        String playerName = player.getGameProfile().name();
        StreamerProfile profile = sanitize(incoming, playerName);

        StreamerProfile existing = find(config, playerName);
        boolean twitchChanged = existing == null || !tokensEqual(existing, profile);

        if (existing != null) {
            existing.minecraftName = profile.minecraftName;
            if (!profile.twitchChannel.isBlank()) existing.twitchChannel = profile.twitchChannel;
            if (!profile.oauthToken.isBlank()) existing.oauthToken = profile.oauthToken;
            if (!profile.donationToken.isBlank()) existing.donationToken = profile.donationToken;
        } else {
            config.streamers.add(profile);
        }

        syncLegacyTwitchFields(config);
        return twitchChanged;
    }

    public static void upsertOffline(TwitchCraftConfig config, StreamerProfile profile) {
        if (config.streamers == null) config.streamers = new ArrayList<>();
        if (profile.minecraftName == null || profile.minecraftName.isBlank()) return;

        StreamerProfile existing = find(config, profile.minecraftName);
        if (existing != null) {
            existing.twitchChannel = profile.twitchChannel;
            existing.oauthToken = profile.oauthToken;
            existing.donationToken = profile.donationToken;
        } else {
            config.streamers.add(copy(profile));
        }
        syncLegacyTwitchFields(config);
    }

    /** Копирует общие настройки (только OP). */
    public static void applySharedSettings(TwitchCraftConfig target, TwitchCraftConfig incoming) {
        target.ui = incoming.ui;
        target.security = incoming.security;
        target.feedback = incoming.feedback;
        target.scoreboard = incoming.scoreboard;
        target.donations.enabled = incoming.donations.enabled;
        target.donations.pollIntervalSeconds = incoming.donations.pollIntervalSeconds;
        target.donations.bypassCooldown = incoming.donations.bypassCooldown;
        target.donations.accessToken = incoming.donations.accessToken;
        target.donations.tiers = incoming.donations.tiers;
        target.stream = incoming.stream;
        target.commands = incoming.commands;
        target.customCommands = incoming.customCommands;
        target.cooldownPacks = incoming.cooldownPacks;
        target.death = incoming.death;
        target.twitch.autoConnect = incoming.twitch.autoConnect;
        target.twitch.eventSubEnabled = incoming.twitch.eventSubEnabled;
        target.twitch.bitsMinAmount = incoming.twitch.bitsMinAmount;
        target.audiencePreset = incoming.audiencePreset;
        target.streamGenre = incoming.streamGenre;
        target.balancePresetApplied = incoming.balancePresetApplied;
    }

    /** Стример без OP: только тема UI. */
    public static void applyStreamerUiOnly(TwitchCraftConfig target, TwitchCraftConfig incoming) {
        if (incoming.ui != null && incoming.ui.theme != null && !incoming.ui.theme.isBlank()) {
            if (target.ui == null) target.ui = new TwitchCraftConfig.UiSettings();
            target.ui.theme = incoming.ui.theme;
        }
    }

    private static StreamerProfile sanitize(StreamerProfile incoming, String playerName) {
        StreamerProfile p = copy(incoming);
        p.minecraftName = playerName;
        if (p.twitchChannel == null) p.twitchChannel = "";
        if (p.oauthToken == null) p.oauthToken = "";
        if (p.donationToken == null) p.donationToken = "";
        return p;
    }

    private static StreamerProfile find(TwitchCraftConfig config, String name) {
        return config.streamers.stream()
                .filter(p -> p.minecraftName != null && p.minecraftName.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private static boolean tokensEqual(StreamerProfile a, StreamerProfile b) {
        return Objects.equals(norm(a.twitchChannel), norm(b.twitchChannel))
                && Objects.equals(norm(a.oauthToken), norm(b.oauthToken))
                && Objects.equals(norm(a.donationToken), norm(b.donationToken));
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim();
    }

    private static StreamerProfile copy(StreamerProfile src) {
        StreamerProfile p = new StreamerProfile();
        p.minecraftName = src.minecraftName != null ? src.minecraftName : "";
        p.twitchChannel = src.twitchChannel != null ? src.twitchChannel : "";
        p.oauthToken = src.oauthToken != null ? src.oauthToken : "";
        p.donationToken = src.donationToken != null ? src.donationToken : "";
        return p;
    }

    private static void syncLegacyTwitchFields(TwitchCraftConfig config) {
        config.targetPlayers.clear();
        if (config.streamers != null) {
            for (StreamerProfile p : config.streamers) {
                if (p.minecraftName != null && !p.minecraftName.isBlank()) {
                    config.targetPlayers.add(p.minecraftName);
                }
            }
        }
        config.targetPlayer = String.join(", ", config.targetPlayers);

        if (config.streamers == null || config.streamers.size() != 1) {
            return;
        }
        StreamerProfile only = config.streamers.getFirst();
        config.twitch.channel = only.twitchChannel != null ? only.twitchChannel : "";
        config.twitch.oauthToken = only.oauthToken != null ? only.oauthToken : "";
        config.donations.accessToken = only.donationToken != null ? only.donationToken : "";
    }
}
