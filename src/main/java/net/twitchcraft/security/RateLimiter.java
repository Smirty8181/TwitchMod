package net.twitchcraft.security;

import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.config.TwitchCraftConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RateLimiter {
    private final Map<String, Scope> scopes = new HashMap<>();

    private static final class Scope {
        private final List<Long> mobTimestamps = new CopyOnWriteArrayList<>();
        private final List<Long> tntTimestamps = new CopyOnWriteArrayList<>();
        private final List<Long> chaosTimestamps = new CopyOnWriteArrayList<>();
        private final List<Long> effectTimestamps = new CopyOnWriteArrayList<>();
        private final List<Long> movementTimestamps = new CopyOnWriteArrayList<>();
        private final List<Long> inventoryTimestamps = new CopyOnWriteArrayList<>();
        private final List<Long> worldTimestamps = new CopyOnWriteArrayList<>();
        private final List<Long> funTimestamps = new CopyOnWriteArrayList<>();
        private final Map<String, List<Long>> commandTimestamps = new HashMap<>();
    }

    public boolean tryCategory(String playerName, ChatGameCommand command, TwitchCraftConfig.SecuritySettings security) {
        Scope s = of(playerName);
        return switch (command.getCategory()) {
            case MOB -> tryAcquire(s.mobTimestamps, security.mobSpawnRateLimitPerMinute);
            case TNT -> tryAcquire(s.tntTimestamps, security.tntRateLimitPerMinute);
            case CHAOS -> tryAcquire(s.chaosTimestamps, security.chaosRateLimitPerMinute);
            case EFFECT -> tryAcquire(s.effectTimestamps, security.effectRateLimitPerMinute);
            case MOVEMENT -> tryAcquire(s.movementTimestamps, security.movementRateLimitPerMinute);
            case INVENTORY -> tryAcquire(s.inventoryTimestamps, security.inventoryRateLimitPerMinute);
            case WORLD -> tryAcquire(s.worldTimestamps, security.worldRateLimitPerMinute);
            case FUN -> tryAcquire(s.funTimestamps, security.funRateLimitPerMinute);
            default -> true;
        };
    }

    public boolean tryCommand(String playerName, String commandId, int maxPerMinute) {
        if (maxPerMinute <= 0) return true;
        List<Long> list = of(playerName).commandTimestamps
                .computeIfAbsent(commandId, k -> new CopyOnWriteArrayList<>());
        return tryAcquire(list, maxPerMinute);
    }

    public boolean tryMobSpawn(String playerName, int limitPerMinute) {
        return tryAcquire(of(playerName).mobTimestamps, limitPerMinute);
    }

    public boolean tryInventory(String playerName, int limitPerMinute) {
        return tryAcquire(of(playerName).inventoryTimestamps, limitPerMinute);
    }

    public boolean tryEffect(String playerName, int limitPerMinute) {
        return tryAcquire(of(playerName).effectTimestamps, limitPerMinute);
    }

    private Scope of(String playerName) {
        String key = playerName == null || playerName.isBlank()
                ? "global" : playerName.toLowerCase(Locale.ROOT);
        return scopes.computeIfAbsent(key, n -> new Scope());
    }

    private boolean tryAcquire(List<Long> timestamps, int limitPerMinute) {
        if (limitPerMinute <= 0) return true;
        prune(timestamps);
        if (timestamps.size() >= limitPerMinute) {
            return false;
        }
        timestamps.add(System.currentTimeMillis());
        return true;
    }

    private void prune(List<Long> timestamps) {
        long cutoff = System.currentTimeMillis() - 60_000L;
        timestamps.removeIf(t -> t < cutoff);
    }
}
