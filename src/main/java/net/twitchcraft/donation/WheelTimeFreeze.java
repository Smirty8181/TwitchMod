package net.twitchcraft.donation;

import net.minecraft.network.protocol.game.ClientboundTickingStatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.twitchcraft.stream.StreamerProfiles;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Заморозка мира на время рулетки — ref-count, чтобы не залипала при нескольких спинах. */
public final class WheelTimeFreeze {
    private static int freezeDepth;

    private WheelTimeFreeze() {}

    public static boolean shouldFreeze(MinecraftServer server) {
        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return false;
        if (players.size() <= 1) return true;

        Set<String> streamers = StreamerProfiles.active().stream()
                .map(p -> p.minecraftName.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (streamers.isEmpty()) return false;

        return players.stream()
                .map(p -> p.getGameProfile().name().toLowerCase(Locale.ROOT))
                .allMatch(streamers::contains);
    }

    public static void freeze(MinecraftServer server) {
        if (!shouldFreeze(server)) return;
        freezeDepth++;
        if (freezeDepth != 1) return;

        ServerTickRateManager mgr = server.tickRateManager();
        if (!mgr.isFrozen()) {
            mgr.setFrozen(true);
            syncClients(server);
        }
    }

    public static void unfreeze(MinecraftServer server) {
        if (freezeDepth <= 0) return;
        freezeDepth--;
        if (freezeDepth > 0) return;

        ServerTickRateManager mgr = server.tickRateManager();
        if (mgr.isFrozen()) {
            mgr.setFrozen(false);
            syncClients(server);
        }
    }

    /** Аварийный сброс при остановке сервера. */
    public static void forceUnfreeze(MinecraftServer server) {
        freezeDepth = 0;
        ServerTickRateManager mgr = server.tickRateManager();
        if (mgr.isFrozen()) {
            mgr.setFrozen(false);
            syncClients(server);
        }
    }

    private static void syncClients(MinecraftServer server) {
        server.getPlayerList().broadcastAll(
                ClientboundTickingStatePacket.from(server.tickRateManager()));
    }
}
