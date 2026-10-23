package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;

import java.util.List;
import java.util.Optional;

/** Целевые игроки для команд чата и донатов. */
public final class StreamTargets {
    private StreamTargets() {}

    public static List<String> names() {
        return StreamerProfiles.active().stream().map(p -> p.minecraftName).toList();
    }

    public static boolean isTarget(ServerPlayer player) {
        if (player == null) return false;
        List<String> targets = names();
        if (targets.isEmpty()) return false;
        String nick = player.getGameProfile().name();
        return targets.stream().anyMatch(t -> t.equalsIgnoreCase(nick));
    }

    public static boolean hasConfiguredTargets() {
        return !names().isEmpty();
    }

    public static Optional<ServerPlayer> resolve(MinecraftServer server) {
        return StreamerProfiles.resolveNext(server);
    }

    public static Optional<ServerPlayer> resolve(MinecraftServer server, String minecraftName) {
        return StreamerProfiles.resolvePlayer(server, minecraftName);
    }

    public static List<ServerPlayer> onlineStreamers(MinecraftServer server) {
        return StreamerProfiles.onlineStreamers(server);
    }

    public static Optional<ServerPlayer> partner(MinecraftServer server, ServerPlayer me) {
        if (me == null || server == null) return Optional.empty();
        for (ServerPlayer p : onlineStreamers(server)) {
            if (!p.getUUID().equals(me.getUUID())) return Optional.of(p);
        }
        return Optional.empty();
    }

    /** Синхронизирует legacy targetPlayers из профилей. */
    public static void syncLegacyTargets(TwitchCraftConfig config) {
        config.targetPlayers.clear();
        for (var p : StreamerProfiles.active()) {
            config.targetPlayers.add(p.minecraftName);
        }
        if (!config.targetPlayers.isEmpty()) {
            config.targetPlayer = String.join(", ", config.targetPlayers);
        }
    }
}
