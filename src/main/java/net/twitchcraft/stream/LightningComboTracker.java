package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.twitchcraft.feedback.AnnouncementService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 3× !молния за 15 сек → «ГРОЗА» (5 ударов). */
public final class LightningComboTracker {
    private static final int WINDOW_MS = 15_000;
    private static final int COMBO_HITS = 3;
    private static final int STORM_STRIKES = 5;
    private static final int STRIKE_DELAY_TICKS = 6;

    private static final Map<String, List<Long>> recentByTarget = new ConcurrentHashMap<>();

    public static void onPlayerDisconnect(String playerName) {
        if (playerName != null) recentByTarget.remove(playerName);
    }

    public static void onLightning(MinecraftServer server, ServerPlayer target, String twitchUser) {
        String key = target.getGameProfile().name();
        long now = System.currentTimeMillis();
        List<Long> hits = recentByTarget.computeIfAbsent(key, k -> new ArrayList<>());
        synchronized (hits) {
            hits.removeIf(t -> now - t > WINDOW_MS);
            hits.add(now);
            if (hits.size() >= COMBO_HITS) {
                hits.clear();
                triggerStorm(server, target, twitchUser);
            }
        }
    }

    private static void triggerStorm(MinecraftServer server, ServerPlayer target, String twitchUser) {
        AnnouncementService.announceComboStorm(target, twitchUser);
        for (int i = 0; i < STORM_STRIKES; i++) {
            int strike = i;
            ServerTaskQueue.schedule(server, strike * STRIKE_DELAY_TICKS, () -> {
                if (!TargetPlayerUtil.isOnline(server, target)) return;
                strikeNear(target);
            });
        }
    }

    private static void strikeNear(ServerPlayer player) {
        ServerLevel level = player.level();
        double ox = (level.random.nextDouble() - 0.5) * 4.0;
        double oz = (level.random.nextDouble() - 0.5) * 4.0;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.COMMAND);
        if (bolt != null) {
            bolt.setPos(player.getX() + ox, player.getY(), player.getZ() + oz);
            level.addFreshEntity(bolt);
        }
    }
}
