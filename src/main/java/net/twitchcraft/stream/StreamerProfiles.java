package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.StreamerProfile;
import net.twitchcraft.config.TwitchCraftConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public final class StreamerProfiles {
    private static final AtomicInteger ROUND = new AtomicInteger();
    private static List<StreamerProfile> cached = List.of();
    private static int cachedRevision = Integer.MIN_VALUE;

    private StreamerProfiles() {}

    public static List<StreamerProfile> active() {
        int rev = ConfigManager.revision();
        if (cachedRevision == rev) return cached;
        TwitchCraftConfig config = ConfigManager.get();
        List<StreamerProfile> list = new ArrayList<>();
        if (config.streamers != null) {
            for (StreamerProfile p : config.streamers) {
                if (p.minecraftName != null && !p.minecraftName.isBlank()) {
                    list.add(p);
                }
            }
        }
        if (list.isEmpty()) {
            StreamerProfile legacy = new StreamerProfile();
            legacy.minecraftName = firstTargetName(config);
            legacy.twitchChannel = config.twitch.channel;
            legacy.oauthToken = config.twitch.oauthToken;
            legacy.donationToken = config.donations.accessToken;
            if (legacy.minecraftName != null && !legacy.minecraftName.isBlank()) {
                list.add(legacy);
            }
        }
        cached = List.copyOf(list);
        cachedRevision = rev;
        return cached;
    }

    public static Optional<StreamerProfile> byMinecraftName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        return active().stream()
                .filter(p -> p.minecraftName.equalsIgnoreCase(name))
                .findFirst();
    }

    public static Optional<StreamerProfile> byTwitchChannel(String channel) {
        if (channel == null || channel.isBlank()) return Optional.empty();
        String ch = channel.toLowerCase(Locale.ROOT).replace("#", "");
        return active().stream()
                .filter(p -> p.twitchChannel != null
                        && p.twitchChannel.toLowerCase(Locale.ROOT).replace("#", "").equals(ch))
                .findFirst();
    }

    public static boolean isStreamerIdentity(String nick) {
        if (nick == null || nick.isBlank()) return false;
        String n = nick.toLowerCase(Locale.ROOT).replace("#", "").trim();
        for (StreamerProfile p : active()) {
            if (p.minecraftName != null && p.minecraftName.equalsIgnoreCase(nick.trim())) return true;
            if (p.twitchChannel != null && p.twitchChannel.toLowerCase(Locale.ROOT).replace("#", "").equals(n)) {
                return true;
            }
        }
        return false;
    }

    public static List<ServerPlayer> onlineStreamers(MinecraftServer server) {
        List<ServerPlayer> result = new ArrayList<>();
        for (StreamerProfile p : active()) {
            ServerPlayer sp = server.getPlayerList().getPlayerByName(p.minecraftName);
            if (sp != null) result.add(sp);
        }
        return result;
    }

    public static Optional<ServerPlayer> resolvePlayer(MinecraftServer server, String minecraftName) {
        if (minecraftName != null && !minecraftName.isBlank()) {
            ServerPlayer p = server.getPlayerList().getPlayerByName(minecraftName);
            if (p != null) return Optional.of(p);
        }
        return resolveNext(server);
    }

    public static Optional<ServerPlayer> resolveNext(MinecraftServer server) {
        List<ServerPlayer> online = new ArrayList<>();
        for (StreamerProfile p : active()) {
            ServerPlayer sp = server.getPlayerList().getPlayerByName(p.minecraftName);
            if (sp != null) online.add(sp);
        }
        if (online.isEmpty()) {
            return Optional.empty();
        }
        if (online.size() == 1) return Optional.of(online.getFirst());
        int idx = Math.floorMod(ROUND.getAndIncrement(), online.size());
        return Optional.of(online.get(idx));
    }

    public static void migrateLegacy(TwitchCraftConfig config) {
        if (config.streamers == null) config.streamers = new ArrayList<>();
        if (!config.streamers.isEmpty()) return;

        List<String> names = new ArrayList<>();
        if (config.targetPlayers != null) {
            names.addAll(config.targetPlayers.stream().filter(s -> s != null && !s.isBlank()).toList());
        }
        if (names.isEmpty() && config.targetPlayer != null && !config.targetPlayer.isBlank()) {
            for (String part : config.targetPlayer.split(",")) {
                String t = part.trim();
                if (!t.isEmpty()) names.add(t);
            }
        }

        if (names.isEmpty()) return;

        boolean splitCredentials = names.size() > 1;
        for (int i = 0; i < names.size(); i++) {
            StreamerProfile p = new StreamerProfile();
            p.minecraftName = names.get(i);
            if (!splitCredentials || i == 0) {
                p.twitchChannel = config.twitch.channel;
                p.oauthToken = config.twitch.oauthToken;
                p.donationToken = config.donations.accessToken;
            }
            config.streamers.add(p);
        }
    }

    private static String firstTargetName(TwitchCraftConfig config) {
        if (config.targetPlayers != null && !config.targetPlayers.isEmpty()) {
            return config.targetPlayers.getFirst();
        }
        if (config.targetPlayer != null && !config.targetPlayer.isBlank()) {
            return config.targetPlayer.split(",")[0].trim();
        }
        return "";
    }
}
