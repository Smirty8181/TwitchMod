package net.twitchcraft.commands;

import net.twitchcraft.stream.HouseMode;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class CooldownManager {
    private final Map<String, Long> lastUse = new HashMap<>();
    private final Map<String, Penalty> penalties = new HashMap<>();

    private record Penalty(double multiplier, long until) {}

    public int effectiveCooldown(int baseSeconds) {
        return effectiveCooldown("global", baseSeconds);
    }

    public int effectiveCooldown(String commandId, int baseSeconds) {
        if (HouseMode.isActive()) return 0;
        if (baseSeconds <= 0) return 0;
        Penalty p = penalties.get(scopeOf(commandId));
        if (p != null && System.currentTimeMillis() < p.until && p.multiplier > 1.0) {
            return (int) Math.ceil(baseSeconds * p.multiplier);
        }
        return baseSeconds;
    }

    public void applyPenalty(double mult, long durationMs) {
        applyPenalty("global", mult, durationMs);
    }

    public void applyPenalty(String playerName, double mult, long durationMs) {
        penalties.put(norm(playerName), new Penalty(mult, System.currentTimeMillis() + durationMs));
    }

    public boolean isPenaltyActive() {
        return isPenaltyActive("global");
    }

    public boolean isPenaltyActive(String playerName) {
        Penalty p = penalties.get(norm(playerName));
        return p != null && System.currentTimeMillis() < p.until && p.multiplier > 1.0;
    }

    public int penaltySecondsLeft() {
        return penaltySecondsLeft("global");
    }

    public int penaltySecondsLeft(String playerName) {
        Penalty p = penalties.get(norm(playerName));
        if (p == null || System.currentTimeMillis() >= p.until) return 0;
        return (int) ((p.until - System.currentTimeMillis() + 999) / 1000);
    }

    public double penaltyMultiplier() {
        return penaltyMultiplier("global");
    }

    public double penaltyMultiplier(String playerName) {
        Penalty p = penalties.get(norm(playerName));
        if (p == null || System.currentTimeMillis() >= p.until) return 1.0;
        return p.multiplier > 1.0 ? p.multiplier : 1.0;
    }

    public boolean isReady(String commandId, int cooldownSeconds) {
        if (HouseMode.isActive()) return true;
        cooldownSeconds = effectiveCooldown(commandId, cooldownSeconds);
        if (cooldownSeconds <= 0) return true;
        Long last = lastUse.get(commandId);
        if (last == null) return true;
        return System.currentTimeMillis() - last >= cooldownSeconds * 1000L;
    }

    public long secondsRemaining(String commandId, int cooldownSeconds) {
        cooldownSeconds = effectiveCooldown(commandId, cooldownSeconds);
        Long last = lastUse.get(commandId);
        if (last == null) return 0;
        long elapsed = System.currentTimeMillis() - last;
        long required = cooldownSeconds * 1000L;
        return elapsed >= required ? 0 : (required - elapsed + 999) / 1000;
    }

    public void markUsed(String commandId) {
        lastUse.put(commandId, System.currentTimeMillis());
    }

    public void clear(String commandId) {
        lastUse.remove(commandId);
    }

    public void clearScope(String playerName) {
        String prefix = norm(playerName) + ":";
        lastUse.keySet().removeIf(k -> k.startsWith(prefix));
        penalties.remove(norm(playerName));
    }

    public void clearAll() {
        lastUse.clear();
        penalties.clear();
    }

    private static String scopeOf(String commandId) {
        if (commandId == null) return "global";
        int i = commandId.indexOf(':');
        return i <= 0 ? "global" : commandId.substring(0, i);
    }

    private static String norm(String playerName) {
        if (playerName == null || playerName.isBlank()) return "global";
        return playerName.toLowerCase(Locale.ROOT);
    }
}
