package net.twitchcraft.stream;

import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.EvilCommands;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.CustomCommandDefinition;
import net.twitchcraft.config.TwitchCraftConfig;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Плавающий КД от живого чата: сколько пишут, кто пишет, что пишут (добро/зло),
 * сколько стример умирает и насколько мир уже в хаосе.
 * Тишина режет КД, рейд и мясо растягивают пропорционально — не ступеньками.
 */
public final class ChatActivity {
    private static final long MSG_WINDOW_MS = 60_000L;
    private static final long UNIQUE_WINDOW_MS = 90_000L;
    private static final long PACE_WINDOW_MS = 60_000L;
    private static final long INTENT_WINDOW_MS = 90_000L;
    private static final long HEAT_WINDOW_MS = 180_000L;

    private static final double MIN_MULT = 0.42;
    private static final double MAX_MULT = 3.50;
    private static final double CROWD_EMA_MIN = 0.42;
    private static final double CROWD_EMA_MAX = 2.60;

    private static final ConcurrentLinkedDeque<Long> MESSAGES = new ConcurrentLinkedDeque<>();
    private static final ConcurrentHashMap<String, Long> CHATTERS = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedDeque<Pulse> PULSES = new ConcurrentLinkedDeque<>();

    private static volatile double crowdEma = 1.0;
    private static volatile double lastPreview = 1.0;

    public enum Kind {
        GOOD, EVIL, PRANK, MOD
    }

    private static final class Pulse {
        final long time;
        final Kind kind;
        final String id;
        final String user;

        Pulse(long time, Kind kind, String id, String user) {
            this.time = time;
            this.kind = kind;
            this.id = id;
            this.user = user;
        }
    }

    private ChatActivity() {}

    public static void onMessage(String user) {
        long now = System.currentTimeMillis();
        MESSAGES.addLast(now);
        if (user != null && !user.isBlank()) {
            CHATTERS.put(user.toLowerCase(Locale.ROOT), now);
        }
        prune(now);
    }

    public static void onCommand(String commandId) {
        onCommand(commandId, Kind.PRANK, "");
    }

    public static void onCommand(ChatGameCommand command, String user) {
        if (command == null) return;
        onCommand(command.getId(), kindOf(command), user);
    }

    public static void onCommand(CustomCommandDefinition def, String user) {
        if (def == null) return;
        onCommand(def.cooldownKey(), def.positive ? Kind.GOOD : Kind.EVIL, user);
    }

    public static void onCommand(String commandId, Kind kind, String user) {
        if (commandId == null || commandId.isBlank()) return;
        long now = System.currentTimeMillis();
        String id = commandId.toLowerCase(Locale.ROOT);
        String who = user == null ? "" : user.toLowerCase(Locale.ROOT);
        PULSES.addLast(new Pulse(now, kind == null ? Kind.PRANK : kind, id, who));
        prune(now);
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        prune(now);
        double target = crowdSignal(now);
        crowdEma = clamp(crowdEma * 0.82 + target * 0.18, CROWD_EMA_MIN, CROWD_EMA_MAX);
        lastPreview = crowdEma;
    }

    public static int scaleCooldown(int baseSeconds) {
        return scaleCooldown(baseSeconds, null, Kind.PRANK, null);
    }

    public static int scaleCooldown(int baseSeconds, String commandId) {
        return scaleCooldown(baseSeconds, commandId, Kind.PRANK, null);
    }

    public static int scaleCooldown(int baseSeconds, ChatGameCommand command, ServerPlayer target) {
        if (command == null) return scaleCooldown(baseSeconds, null, Kind.PRANK, target);
        return scaleCooldown(baseSeconds, command.getId(), kindOf(command), target);
    }

    public static int scaleCooldown(int baseSeconds, CustomCommandDefinition def, ServerPlayer target) {
        if (def == null) return scaleCooldown(baseSeconds, null, Kind.PRANK, target);
        return scaleCooldown(baseSeconds, def.cooldownKey(),
                def.positive ? Kind.GOOD : Kind.EVIL, target);
    }

    public static int scaleCooldown(int baseSeconds, String commandId, Kind kind, ServerPlayer target) {
        if (baseSeconds <= 0) return baseSeconds;
        if (!enabled()) return baseSeconds;
        if (HouseMode.isActive()) return baseSeconds;
        Kind k = kind == null ? Kind.PRANK : kind;
        double m = combined(commandId, k, target);
        int scaled = (int) Math.round(baseSeconds * m);
        int floor = Math.max(3, (int) Math.round(baseSeconds * MIN_MULT));
        int ceil = Math.max(baseSeconds, (int) Math.round(baseSeconds * MAX_MULT));
        if (floor > ceil) floor = ceil;
        return Math.max(floor, Math.min(ceil, scaled));
    }

    public static double multiplier() {
        return lastPreview;
    }

    public static double previewMultiplier(ServerPlayer player) {
        if (!enabled()) return 1.0;
        double death = deathFactor(player, Kind.EVIL);
        double chaos = chaosFactor(player, Kind.EVIL);
        double m = clamp(
                crowdEma
                        + (death - 1.0) * 1.05
                        + (chaos - 1.0) * 0.45,
                MIN_MULT, MAX_MULT);
        lastPreview = m;
        return m;
    }

    public static String sidebarHint() {
        return sidebarHint(null);
    }

    public static String sidebarHint(ServerPlayer player) {
        if (!enabled()) return "";
        double m = player == null ? crowdEma : previewMultiplier(player);
        String tag = tag(player, m);
        return "§dКД ×" + fmt(m) + " " + tagColor(tag) + tag;
    }

    public static String compactHint() {
        return compactHint(null);
    }

    public static String compactHint(ServerPlayer player) {
        if (!enabled()) return "";
        double m = player == null ? crowdEma : previewMultiplier(player);
        String tag = tag(player, m);
        return "§8×" + fmt(m) + " " + tagColor(tag) + tag;
    }

    private static boolean enabled() {
        return ConfigManager.get().stream.chatActivityCooldowns;
    }

    private static Kind kindOf(ChatGameCommand command) {
        return switch (EvilCommands.harm(command)) {
            case GOOD -> Kind.GOOD;
            case DAMAGE -> Kind.EVIL;
            case MOD -> Kind.MOD;
            case PRANK -> Kind.PRANK;
        };
    }

    private static double combined(String commandId, Kind kind, ServerPlayer target) {
        long now = System.currentTimeMillis();
        prune(now);
        double heat = heatFactor(commandId, now);
        double intent = intentFactor(kind, now);
        double death = deathFactor(target, kind);
        double chaos = chaosFactor(target, kind);
        double karma = karmaFactor(target, kind, commandId);
        return clamp(
                crowdEma
                        + (heat - 1.0) * 0.75
                        + (intent - 1.0) * 0.95
                        + (death - 1.0) * 1.05
                        + (chaos - 1.0) * 0.45
                        + (karma - 1.0) * 0.55,
                MIN_MULT, MAX_MULT);
    }

    private static double crowdSignal(long now) {
        int msgs = MESSAGES.size();
        int unique = uniqueCount(now);
        int cmds = countPulses(now, PACE_WINDOW_MS);
        double crowd = clamp(0.48 + msgs / 20.0, 0.48, 3.20);
        double uniq = clamp(0.78 + unique / 10.0, 0.78, 2.40);
        double pace = clamp(0.88 + cmds / 16.0, 0.88, 2.10);
        return 1.0
                + (crowd - 1.0) * 0.90
                + (uniq - 1.0) * 0.55
                + (pace - 1.0) * 0.45;
    }

    private static int uniqueCount(long now) {
        int unique = 0;
        long cut = now - UNIQUE_WINDOW_MS;
        for (long seen : CHATTERS.values()) {
            if (seen >= cut) unique++;
        }
        return unique;
    }

    private static int countPulses(long now, long windowMs) {
        long cut = now - windowMs;
        int n = 0;
        for (Pulse p : PULSES) {
            if (p.time >= cut) n++;
        }
        return n;
    }

    /** Одна и та же команда + сколько разных ников её жмут. */
    private static double heatFactor(String commandId, long now) {
        if (commandId == null || commandId.isBlank()) return 1.0;
        String id = commandId.toLowerCase(Locale.ROOT);
        long cut = now - HEAT_WINDOW_MS;
        int n = 0;
        Set<String> users = new HashSet<>();
        for (Pulse p : PULSES) {
            if (p.time < cut || !id.equals(p.id)) continue;
            n++;
            if (p.user != null && !p.user.isBlank()) users.add(p.user);
        }
        if (n <= 1) return 1.0;
        int uniq = Math.max(1, users.size());
        return clamp(1.0 + (n - 1) * 0.14 + (uniq - 1) * 0.16, 1.0, 2.70);
    }

    private static double intentFactor(Kind kind, long now) {
        Mix mix = mix(now);
        if (mix.total() == 0) return 1.0;
        return switch (kind) {
            case EVIL -> clamp(1.0 + mix.evilShare() * 1.15 - mix.goodShare() * 0.12, 0.85, 2.20);
            case GOOD -> clamp(1.0 - mix.evilShare() * 0.45 + mix.goodShare() * 0.55, 0.50, 1.60);
            case PRANK -> clamp(1.0 + (mix.evilShare() + mix.goodShare()) * 0.12, 0.90, 1.35);
            case MOD -> 1.0;
        };
    }

    private static double deathFactor(ServerPlayer target, Kind kind) {
        if (target == null) return 1.0;
        TwitchCraftConfig.DeathSettings cfg = ConfigManager.get().death;
        if (cfg == null || !cfg.enabled) return 1.0;
        String name = target.getGameProfile().name();
        int d = DeathTracker.deathsInWindow(name);
        int need = Math.max(1, cfg.deathsForPenalty);
        double death = 1.0 + (d / (double) need) * 1.55;
        int ago = DeathTracker.secondsSinceDeath(name);
        if (kind == Kind.EVIL && ago < 50) {
            death += (50 - ago) / 50.0 * 0.85;
        } else if (kind == Kind.GOOD && ago < 50) {
            death *= 0.82;
        }
        if (kind == Kind.PRANK || kind == Kind.MOD) {
            death = 1.0 + (death - 1.0) * 0.45;
        }
        return clamp(death, 0.70, 3.20);
    }

    private static double chaosFactor(ServerPlayer target, Kind kind) {
        if (target == null) return 1.0;
        int max = Math.max(1, ConfigManager.get().stream.chaosMeterMax);
        double r = clamp(StreamStats.forPlayer(target.getGameProfile().name()).getChaosMeter() / (double) max,
                0.0, 1.0);
        return switch (kind) {
            case EVIL -> clamp(1.0 + r * 0.70, 1.0, 1.70);
            case PRANK -> clamp(1.0 + r * 0.40, 1.0, 1.40);
            case GOOD -> clamp(1.0 - r * 0.25, 0.75, 1.0);
            case MOD -> 1.0;
        };
    }

    private static double karmaFactor(ServerPlayer target, Kind kind, String commandId) {
        if (target == null) return 1.0;
        if (commandId != null && commandId.equalsIgnoreCase(ChatGameCommand.LIGHTNING.getId())) {
            return 1.0;
        }
        int k = ViewerKarma.score(target);
        if (kind == Kind.EVIL && k < 0) {
            return 1.0 + Math.min(0.55, (-k) / 100.0 * 0.55);
        }
        if (kind == Kind.GOOD && k < 0) {
            return 1.0 - Math.min(0.30, (-k) / 100.0 * 0.30);
        }
        return 1.0;
    }

    private static String tag(ServerPlayer player, double m) {
        Mix mix = mix(System.currentTimeMillis());
        double death = deathFactor(player, Kind.EVIL);
        double cEx = crowdEma - 1.0;
        double dEx = death - 1.0;
        if (dEx >= 0.38 && dEx >= cEx * 0.85) return "мясо";
        if (m >= 1.65 || crowdEma >= 1.65) return "рейд";
        if (m <= 0.72 && crowdEma <= 0.78) return "тихо";
        if (mix.total() >= 3 && mix.evilShare() >= 0.58) return "зло";
        if (mix.total() >= 3 && mix.goodShare() >= 0.58) return "добро";
        return "норма";
    }

    private static String tagColor(String tag) {
        return switch (tag) {
            case "тихо", "добро" -> "§a";
            case "рейд" -> "§d";
            case "мясо", "зло" -> "§c";
            default -> "§7";
        };
    }

    private static Mix mix(long now) {
        long cut = now - INTENT_WINDOW_MS;
        int g = 0, e = 0, p = 0;
        for (Pulse pulse : PULSES) {
            if (pulse.time < cut) continue;
            switch (pulse.kind) {
                case GOOD -> g++;
                case EVIL -> e++;
                case PRANK -> p++;
                case MOD -> {
                }
            }
        }
        return new Mix(g, e, p);
    }

    private record Mix(int good, int evil, int prank) {
        int total() {
            return good + evil + prank;
        }

        double evilShare() {
            return total() == 0 ? 0 : evil / (double) total();
        }

        double goodShare() {
            return total() == 0 ? 0 : good / (double) total();
        }
    }

    private static String fmt(double m) {
        return String.format(Locale.US, "%.1f", m);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static void prune(long now) {
        long msgCutoff = now - MSG_WINDOW_MS;
        Long head;
        while ((head = MESSAGES.peekFirst()) != null && head < msgCutoff) {
            MESSAGES.pollFirst();
        }
        long uniqueCutoff = now - UNIQUE_WINDOW_MS;
        Iterator<Map.Entry<String, Long>> it = CHATTERS.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() < uniqueCutoff) it.remove();
        }
        long pulseCut = now - HEAT_WINDOW_MS;
        Pulse pulse;
        while ((pulse = PULSES.peekFirst()) != null && pulse.time < pulseCut) {
            PULSES.pollFirst();
        }
    }
}
