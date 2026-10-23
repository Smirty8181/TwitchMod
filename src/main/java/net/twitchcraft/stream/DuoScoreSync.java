package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.twitchcraft.config.StreamerProfile;
import net.twitchcraft.network.DuoScorePayload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Каждому игроку: слева он сам, справа напарник на сервере. Ник и аватар — Twitch. */
public final class DuoScoreSync {
    private static String lastKey = "";

    private DuoScoreSync() {}

    public static void push(MinecraftServer server) {
        if (server == null) return;
        if (!net.twitchcraft.config.ConfigManager.get().stream.duoStreamMode) {
            if (!"off".equals(lastKey)) {
                lastKey = "off";
                DuoScorePayload.hideAll(server);
            }
            return;
        }
        List<ServerPlayer> online = new ArrayList<>(server.getPlayerList().getPlayers());
        String key = buildKey(online);
        if (key.equals(lastKey)) return;
        lastKey = key;

        if (online.size() < 2) {
            DuoScorePayload.hideAll(server);
            return;
        }

        for (ServerPlayer viewer : online) {
            ServerPlayer other = pickOther(viewer, online);
            if (other == null) {
                DuoScorePayload.send(viewer, DuoScorePayload.hidden());
                continue;
            }
            DuoScorePayload.send(viewer, new DuoScorePayload(
                    twitchOf(viewer), nickOf(viewer), DeathTracker.totalDeaths(viewer.getGameProfile().name()),
                    twitchOf(other), nickOf(other), DeathTracker.totalDeaths(other.getGameProfile().name()),
                    hp(other), maxHp(other), food(other),
                    true));
        }
    }

    private static String buildKey(List<ServerPlayer> online) {
        if (online.size() < 2) return "hidden";
        List<String> parts = new ArrayList<>();
        for (ServerPlayer p : online) {
            String name = p.getGameProfile().name();
            parts.add(name + ":" + DeathTracker.totalDeaths(name) + ":" + twitchOf(p)
                    + ":" + hp(p) + ":" + maxHp(p) + ":" + food(p));
        }
        parts.sort(Comparator.naturalOrder());
        return String.join("|", parts);
    }

    private static int hp(ServerPlayer player) {
        return Math.max(0, Math.round(player.getHealth()));
    }

    private static int maxHp(ServerPlayer player) {
        return Math.max(1, Math.round(player.getMaxHealth()));
    }

    private static int food(ServerPlayer player) {
        return Mth.clamp(player.getFoodData().getFoodLevel(), 0, 20);
    }

    private static ServerPlayer pickOther(ServerPlayer me, List<ServerPlayer> online) {
        String myName = me.getGameProfile().name();
        for (StreamerProfile profile : StreamerProfiles.active()) {
            if (profile.minecraftName == null || profile.minecraftName.equalsIgnoreCase(myName)) continue;
            for (ServerPlayer p : online) {
                if (p.getGameProfile().name().equalsIgnoreCase(profile.minecraftName)) return p;
            }
        }
        for (ServerPlayer p : online) {
            if (!p.getUUID().equals(me.getUUID())) return p;
        }
        return null;
    }

    private static String twitchOf(ServerPlayer player) {
        return StreamerProfiles.byMinecraftName(player.getGameProfile().name())
                .map(p -> p.twitchChannel)
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.replace("#", "").trim())
                .orElse("");
    }

    private static String nickOf(ServerPlayer player) {
        String twitch = twitchOf(player);
        if (!twitch.isBlank()) return twitch;
        return player.getGameProfile().name();
    }
}
