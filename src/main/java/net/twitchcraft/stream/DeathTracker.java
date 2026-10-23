package net.twitchcraft.stream;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.DeathAlertPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Смерти стримера: координаты, счётчик, штраф на плохие команды. */
public final class DeathTracker {
    private static final Map<String, PlayerState> BY_PLAYER = new ConcurrentHashMap<>();

    private static final class PlayerState {
        int totalDeaths;
        final List<Long> recentTimes = new ArrayList<>();
        long evilBlockUntil;
        long lastDeathAt;
        int lastX, lastY, lastZ;
        String lastDim = "";
    }

    private DeathTracker() {}

    public static void onDeath(ServerPlayer player) {
        onDeath(player, DeathCause.UNKNOWN);
    }

    public static void onDeath(ServerPlayer player, DeathCause cause) {
        TwitchCraftConfig.DeathSettings cfg = deathCfg();
        if (!cfg.enabled) return;

        String key = player.getGameProfile().name().toLowerCase(Locale.ROOT);
        PlayerState st = BY_PLAYER.computeIfAbsent(key, k -> new PlayerState());
        long now = System.currentTimeMillis();

        st.totalDeaths++;
        st.lastDeathAt = now;
        st.lastX = player.blockPosition().getX();
        st.lastY = player.blockPosition().getY();
        st.lastZ = player.blockPosition().getZ();
        st.lastDim = player.level().dimension().location().getPath();

        st.recentTimes.add(now);
        pruneRecent(st, cfg.deathWindowMinutes);

        int inWindow = st.recentTimes.size();
        boolean triggeredPenalty = false;
        boolean wantBlock = cfg.blockEvilCommands;
        if (wantBlock && inWindow >= cfg.deathsForPenalty) {
            st.evilBlockUntil = now + cfg.evilBlockMinutes * 60_000L;
            st.recentTimes.clear();
            triggeredPenalty = true;
        }

        String coords = formatCoords(st, cfg);
        DeathCause resolved = cause == null ? DeathCause.UNKNOWN : cause;
        StreamChronicle.death(player.getGameProfile().name(), resolved, st.totalDeaths, inWindow,
                cfg.deathsForPenalty, coords, triggeredPenalty);
        broadcastDeath(player, st, inWindow, coords, triggeredPenalty, cfg, resolved);
        DuoScoreSync.push(player.level().getServer());
    }

    public static boolean isEvilBlocked(String minecraftName) {
        if (minecraftName == null) return false;
        PlayerState st = BY_PLAYER.get(minecraftName.toLowerCase(Locale.ROOT));
        if (st == null) return false;
        if (System.currentTimeMillis() >= st.evilBlockUntil) {
            st.evilBlockUntil = 0;
            return false;
        }
        return deathCfg().blockEvilCommands;
    }

    public static int evilBlockSecondsLeft(String minecraftName) {
        PlayerState st = BY_PLAYER.get(minecraftName.toLowerCase(Locale.ROOT));
        if (st == null || st.evilBlockUntil <= 0) return 0;
        long left = st.evilBlockUntil - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) ((left + 999) / 1000);
    }

    public static int totalDeaths(String minecraftName) {
        PlayerState st = BY_PLAYER.get(minecraftName.toLowerCase(Locale.ROOT));
        return st == null ? 0 : st.totalDeaths;
    }

    public static int deathsInWindow(String minecraftName) {
        PlayerState st = BY_PLAYER.get(minecraftName.toLowerCase(Locale.ROOT));
        if (st == null) return 0;
        pruneRecent(st, deathCfg().deathWindowMinutes);
        return st.recentTimes.size();
    }

    /** Секунд с последней смерти. Большое число, если ещё не умирал. */
    public static int secondsSinceDeath(String minecraftName) {
        if (minecraftName == null || minecraftName.isBlank()) return Integer.MAX_VALUE / 4;
        PlayerState st = BY_PLAYER.get(minecraftName.toLowerCase(Locale.ROOT));
        if (st == null || st.lastDeathAt <= 0L) return Integer.MAX_VALUE / 4;
        return (int) Math.max(0L, (System.currentTimeMillis() - st.lastDeathAt) / 1000L);
    }

    public static String lastCoordsLine(String minecraftName) {
        PlayerState st = BY_PLAYER.get(minecraftName.toLowerCase(Locale.ROOT));
        if (st == null || st.totalDeaths == 0) return "";
        return formatCoords(st, deathCfg());
    }

    public static void clearEvilBlock(String minecraftName) {
        if (minecraftName == null) return;
        PlayerState st = BY_PLAYER.get(minecraftName.toLowerCase(Locale.ROOT));
        if (st != null) st.evilBlockUntil = 0;
    }

    /** Если мод могил проглотил AFTER_DEATH — всё равно считаем смерть на респавне. */
    public static void onRespawnIfMissed(ServerPlayer player) {
        if (player == null || !StreamTargets.isTarget(player)) return;
        if (!deathCfg().enabled) return;
        String key = player.getGameProfile().name().toLowerCase(Locale.ROOT);
        PlayerState st = BY_PLAYER.get(key);
        long now = System.currentTimeMillis();
        if (st != null && now - st.lastDeathAt < 4000L) return;
        onDeath(player, DeathCause.UNKNOWN);
    }

    /** Только накопительный счётчик смертей (для сезона). */
    public static Map<String, Integer> exportTotals() {
        Map<String, Integer> out = new ConcurrentHashMap<>();
        for (var e : BY_PLAYER.entrySet()) {
            if (e.getValue().totalDeaths > 0) {
                out.put(e.getKey(), e.getValue().totalDeaths);
            }
        }
        return out;
    }

    public static void resetAll() {
        BY_PLAYER.clear();
    }

    public static void restoreTotals(Map<String, Integer> totals) {
        if (totals == null || totals.isEmpty()) return;
        for (var e : totals.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue() <= 0) continue;
            String key = e.getKey().toLowerCase(Locale.ROOT);
            PlayerState st = BY_PLAYER.computeIfAbsent(key, k -> new PlayerState());
            st.totalDeaths = Math.max(st.totalDeaths, e.getValue());
        }
    }

    public static Map<String, Snapshot> exportFull() {
        Map<String, Snapshot> out = new ConcurrentHashMap<>();
        long now = System.currentTimeMillis();
        for (var e : BY_PLAYER.entrySet()) {
            PlayerState st = e.getValue();
            Snapshot s = new Snapshot();
            s.totalDeaths = st.totalDeaths;
            s.recentTimes = new ArrayList<>(st.recentTimes);
            s.evilBlockUntil = st.evilBlockUntil > now ? st.evilBlockUntil : 0;
            s.lastX = st.lastX;
            s.lastY = st.lastY;
            s.lastZ = st.lastZ;
            s.lastDim = st.lastDim != null ? st.lastDim : "";
            out.put(e.getKey(), s);
        }
        return out;
    }

    public static void restoreFull(Map<String, Snapshot> snapshots, Map<String, Integer> totals) {
        if (snapshots != null && !snapshots.isEmpty()) {
            long now = System.currentTimeMillis();
            for (var e : snapshots.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                String key = e.getKey().toLowerCase(Locale.ROOT);
                Snapshot s = e.getValue();
                PlayerState st = BY_PLAYER.computeIfAbsent(key, k -> new PlayerState());
                st.totalDeaths = Math.max(st.totalDeaths, s.totalDeaths);
                st.lastX = s.lastX;
                st.lastY = s.lastY;
                st.lastZ = s.lastZ;
                st.lastDim = s.lastDim != null ? s.lastDim : "";
                st.evilBlockUntil = s.evilBlockUntil > now ? s.evilBlockUntil : 0;
                st.lastDeathAt = 0L;
                if (s.recentTimes != null) {
                    st.recentTimes.clear();
                    st.recentTimes.addAll(s.recentTimes);
                    pruneRecent(st, deathCfg().deathWindowMinutes);
                    for (long t : st.recentTimes) {
                        if (t > st.lastDeathAt) st.lastDeathAt = t;
                    }
                }
            }
            return;
        }
        restoreTotals(totals);
    }

    public static final class Snapshot {
        public int totalDeaths;
        public List<Long> recentTimes = new ArrayList<>();
        public long evilBlockUntil;
        public int lastX, lastY, lastZ;
        public String lastDim = "";
    }

    private static void pruneRecent(PlayerState st, int windowMinutes) {
        long cutoff = System.currentTimeMillis() - windowMinutes * 60_000L;
        st.recentTimes.removeIf(t -> t < cutoff);
    }

    private static String formatCoords(PlayerState st, TwitchCraftConfig.DeathSettings cfg) {
        if (!cfg.showCoordinates) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("X:").append(st.lastX).append(" Y:").append(st.lastY).append(" Z:").append(st.lastZ);
        if (cfg.showDimension && st.lastDim != null && !st.lastDim.isBlank()) {
            sb.append(" §8(").append(dimRu(st.lastDim)).append(")");
        }
        return sb.toString();
    }

    private static String dimRu(String dim) {
        return switch (dim) {
            case "overworld" -> "Овермир";
            case "the_nether" -> "Ад";
            case "the_end" -> "Край";
            default -> dim;
        };
    }

    private static void broadcastDeath(ServerPlayer player, PlayerState st, int inWindow,
                                       String coords, boolean penalty, TwitchCraftConfig.DeathSettings cfg,
                                       DeathCause cause) {
        String display = cfg.coordsDisplay == null ? "overlay" : cfg.coordsDisplay.toLowerCase(Locale.ROOT);
        String victim = player.getGameProfile().name();
        String killer = cause.hasKiller() ? cause.killer() : "";
        String why = cause.detail() == null ? "" : cause.detail();

        if (cfg.showDeathToast || display.contains("overlay") || "all".equals(display)) {
            DeathAlertPayload.sendToStreamers(player.level().getServer(),
                    st.totalDeaths, inWindow, st.lastX, st.lastY, st.lastZ,
                    st.lastDim, evilBlockSecondsLeft(victim), penalty,
                    victim, killer, why);
        }

        String killerLine = cause.hasKiller()
                ? "§cУбил: §f" + cause.killer() + (why.isBlank() ? "" : " §8· §7" + why)
                : "§cУмер: §f" + why;
        String deathMsg = "§c☠ Смерть #" + st.totalDeaths
                + (cfg.showDeathCounter ? " §8(" + inWindow + "/" + cfg.deathsForPenalty + " за "
                + cfg.deathWindowMinutes + "м)" : "")
                + " §8| " + killerLine;
        if (!coords.isBlank() && (display.contains("actionbar") || display.contains("all"))) {
            deathMsg += " §7| " + coords;
        }

        player.connection.send(new ClientboundSetTitlesAnimationPacket(6, 55, 12));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal(cause.hasKiller() ? "§c§lУБИЛ" : "§c§lУМЕР")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal(cause.hasKiller()
                        ? "§f" + cause.killer() + (why.isBlank() ? "" : " §8· §7" + why)
                        : "§f" + why)));

        TwitchCraftMod.getConnectionManager().sendChatMessage(cause.twitchLine(victim));

        if (display.contains("actionbar") || "all".equals(display) || penalty) {
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(deathMsg)));
        }

        if (penalty) {
            String pen = "§4§lБЛОК ЗЛА §c" + cfg.evilBlockMinutes + " мин — чат не бьёт";
            AnnouncementService.announceCustom(player, pen);
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(pen)));
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("§4§lБЛОК ЗЛА")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.literal("§cкоманды с уроном · " + cfg.evilBlockMinutes + " мин")));
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    "БЛОК ЗЛА: стример умер " + cfg.deathsForPenalty + " раз — урон из чата выключен на "
                            + cfg.evilBlockMinutes + " мин.");
        }
    }

    private static TwitchCraftConfig.DeathSettings deathCfg() {
        var cfg = ConfigManager.get().death;
        return cfg != null ? cfg : new TwitchCraftConfig.DeathSettings();
    }
}
