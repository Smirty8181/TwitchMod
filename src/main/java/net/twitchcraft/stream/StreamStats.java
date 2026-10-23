package net.twitchcraft.stream;

import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.EvilCommands;
import net.twitchcraft.commands.GoodCommands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StreamStats {
    private static final StreamStats GLOBAL = new StreamStats();
    private static final Map<String, StreamStats> BY_PLAYER = new ConcurrentHashMap<>();

    private volatile String lastUser = "-";
    private volatile String lastCommand = "-";
    private volatile String lastCommandRu = "-";
    private volatile String lastDonor = "-";
    private volatile int goodPoints;
    private volatile int evilPoints;
    private volatile int chaosMeter;
    private volatile int karmaScore;
    private volatile int totalCommands;

    private volatile String streakUser = "";
    private volatile int streakCount;
    private volatile boolean streakGood;

    private final Map<String, Integer> viewerCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> viewerGood = new ConcurrentHashMap<>();
    private final Map<String, Integer> viewerEvil = new ConcurrentHashMap<>();
    private final Map<String, String> viewerDisplayNames = new ConcurrentHashMap<>();
    /** userKey -> commandId -> count */
    private final Map<String, Map<String, Integer>> viewerCommands = new ConcurrentHashMap<>();

    private StreamStats() {}

    public static StreamStats global() {
        return GLOBAL;
    }

    /** @deprecated use {@link #forPlayer(String)} or {@link #global()} */
    public static StreamStats get() {
        return GLOBAL;
    }

    public static StreamStats forPlayer(String minecraftName) {
        if (minecraftName == null || minecraftName.isBlank()) return GLOBAL;
        return BY_PLAYER.computeIfAbsent(minecraftName.toLowerCase(Locale.ROOT), k -> new StreamStats());
    }

    /** Снимок для персистентности сезона. */
    public static final class StatsSnapshot {
        public int goodPoints;
        public int evilPoints;
        public int chaosMeter;
        public int karmaScore;
        public int totalCommands;
        public String lastUser = "-";
        public String lastCommand = "-";
        public String lastCommandRu = "-";
        public String lastDonor = "-";
        public String streakUser = "";
        public int streakCount;
        public boolean streakGood;
        public Map<String, Integer> viewerCounts = new ConcurrentHashMap<>();
        public Map<String, Integer> viewerGood = new ConcurrentHashMap<>();
        public Map<String, Integer> viewerEvil = new ConcurrentHashMap<>();
        public Map<String, String> viewerDisplayNames = new ConcurrentHashMap<>();
        public Map<String, Map<String, Integer>> viewerCommands = new ConcurrentHashMap<>();
    }

    public StatsSnapshot exportSnapshot() {
        StatsSnapshot s = new StatsSnapshot();
        s.goodPoints = goodPoints;
        s.evilPoints = evilPoints;
        s.chaosMeter = chaosMeter;
        s.karmaScore = karmaScore;
        s.totalCommands = totalCommands;
        s.lastUser = lastUser;
        s.lastCommand = lastCommand;
        s.lastCommandRu = lastCommandRu;
        s.lastDonor = lastDonor;
        s.streakUser = streakUser;
        s.streakCount = streakCount;
        s.streakGood = streakGood;
        s.viewerCounts = new ConcurrentHashMap<>(viewerCounts);
        s.viewerGood = new ConcurrentHashMap<>(viewerGood);
        s.viewerEvil = new ConcurrentHashMap<>(viewerEvil);
        s.viewerDisplayNames = new ConcurrentHashMap<>(viewerDisplayNames);
        s.viewerCommands = new ConcurrentHashMap<>();
        for (var e : viewerCommands.entrySet()) {
            s.viewerCommands.put(e.getKey(), new ConcurrentHashMap<>(e.getValue()));
        }
        return s;
    }

    public void importSnapshot(StatsSnapshot s) {
        if (s == null) return;
        goodPoints = s.goodPoints;
        evilPoints = s.evilPoints;
        chaosMeter = s.chaosMeter;
        karmaScore = s.karmaScore;
        totalCommands = s.totalCommands;
        lastUser = s.lastUser != null ? s.lastUser : "-";
        lastCommand = s.lastCommand != null ? s.lastCommand : "-";
        lastCommandRu = s.lastCommandRu != null ? s.lastCommandRu : "-";
        lastDonor = s.lastDonor != null ? s.lastDonor : "-";
        streakUser = s.streakUser != null ? s.streakUser : "";
        streakCount = s.streakCount;
        streakGood = s.streakGood;
        viewerCounts.clear();
        viewerGood.clear();
        viewerEvil.clear();
        viewerDisplayNames.clear();
        viewerCommands.clear();
        if (s.viewerCounts != null) viewerCounts.putAll(s.viewerCounts);
        if (s.viewerGood != null) viewerGood.putAll(s.viewerGood);
        if (s.viewerEvil != null) viewerEvil.putAll(s.viewerEvil);
        if (s.viewerDisplayNames != null) viewerDisplayNames.putAll(s.viewerDisplayNames);
        if (s.viewerCommands != null) {
            for (var e : s.viewerCommands.entrySet()) {
                viewerCommands.put(e.getKey(), new ConcurrentHashMap<>(
                        e.getValue() != null ? e.getValue() : Map.of()));
            }
        }
    }

    public static Map<String, StatsSnapshot> exportAll() {
        Map<String, StatsSnapshot> out = new ConcurrentHashMap<>();
        out.put("__global__", GLOBAL.exportSnapshot());
        for (var e : BY_PLAYER.entrySet()) {
            out.put(e.getKey(), e.getValue().exportSnapshot());
        }
        return out;
    }

    public static void resetAll() {
        GLOBAL.clear();
        BY_PLAYER.clear();
    }

    public void clear() {
        goodPoints = 0;
        evilPoints = 0;
        chaosMeter = 0;
        karmaScore = 0;
        totalCommands = 0;
        lastUser = "-";
        lastCommand = "-";
        lastCommandRu = "-";
        lastDonor = "-";
        streakUser = "";
        streakCount = 0;
        streakGood = false;
        viewerCounts.clear();
        viewerGood.clear();
        viewerEvil.clear();
        viewerDisplayNames.clear();
        viewerCommands.clear();
    }

    public static void restoreAll(Map<String, StatsSnapshot> data) {
        if (data == null || data.isEmpty()) return;
        StatsSnapshot globalSnap = data.get("__global__");
        if (globalSnap != null) GLOBAL.importSnapshot(globalSnap);
        BY_PLAYER.clear();
        for (var e : data.entrySet()) {
            if ("__global__".equals(e.getKey()) || e.getValue() == null) continue;
            StreamStats stats = new StreamStats();
            stats.importSnapshot(e.getValue());
            BY_PLAYER.put(e.getKey().toLowerCase(Locale.ROOT), stats);
        }
    }

    public void recordCommand(String user, ChatGameCommand command) {
        lastUser = user;
        lastCommand = command.getId();
        lastCommandRu = command.getRussianName();
        totalCommands++;
        String key = user.toLowerCase(Locale.ROOT);
        viewerCounts.merge(key, 1, Integer::sum);
        if (user != null && !user.isBlank()) {
            viewerDisplayNames.put(key, user);
            viewerCommands.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                    .merge(command.getId(), 1, Integer::sum);
        }

        if (GoodCommands.isGood(command)) {
            goodPoints++;
            viewerGood.merge(key, 1, Integer::sum);
            updateStreak(user, true);
        } else if (EvilCommands.isEvil(command)) {
            evilPoints++;
            viewerEvil.merge(key, 1, Integer::sum);
            updateStreak(user, false);
        } else {
            resetStreak();
        }
        mirrorGlobal();
    }

    private void updateStreak(String user, boolean good) {
        if (user == null || user.isBlank()) return;
        String key = user.toLowerCase(Locale.ROOT);
        if (key.equals(streakUser != null ? streakUser.toLowerCase(Locale.ROOT) : "") && good == streakGood) {
            streakCount++;
        } else {
            streakUser = user;
            streakCount = 1;
            streakGood = good;
        }
    }

    private void resetStreak() {
        streakUser = "";
        streakCount = 0;
    }

    public void recordDonation(String donor, String commandId) {
        lastDonor = donor;
        lastUser = donor;
        lastCommand = "donate:" + commandId;
        ChatGameCommand.fromId(commandId).ifPresentOrElse(
                c -> lastCommandRu = c.getRussianName(),
                () -> lastCommandRu = commandId
        );
        mirrorGlobal();
    }

    private void mirrorGlobal() {
        GLOBAL.lastUser = lastUser;
        GLOBAL.lastCommand = lastCommand;
        GLOBAL.lastCommandRu = lastCommandRu;
        GLOBAL.lastDonor = lastDonor;
    }

    public int addChaos(int amount, int max) {
        chaosMeter = Math.min(max, chaosMeter + amount);
        return chaosMeter;
    }

    public void resetChaos() {
        chaosMeter = 0;
    }

    public String getLastUser() { return lastUser; }
    public String getLastCommand() { return lastCommand; }
    public String getLastCommandRu() { return lastCommandRu; }
    public String getLastDonor() { return lastDonor; }
    public int getGoodPoints() { return goodPoints; }
    public int getEvilPoints() { return evilPoints; }
    public int getChaosMeter() { return chaosMeter; }
    public int getKarma() { return karmaScore; }
    public int getTotalCommands() { return totalCommands; }

    public int addKarma(int delta) {
        karmaScore = Math.max(-100, Math.min(100, karmaScore + delta));
        return karmaScore;
    }

    public String getStreakUser() { return streakUser != null ? streakUser : ""; }
    public int getStreakCount() { return streakCount; }
    public boolean isStreakGood() { return streakGood; }

    public void addGood() { goodPoints++; }
    public void addEvil() { evilPoints++; }

    public String getTopViewer() {
        return topEntry(viewerCounts);
    }

    public String getTopGoodViewer() {
        return topEntry(viewerGood);
    }

    public int getTopGoodCount() {
        return topCount(viewerGood);
    }

    public String getTopEvilViewer() {
        return topEntry(viewerEvil);
    }

    public int getTopEvilCount() {
        return topCount(viewerEvil);
    }

    public String getTopActiveViewer() {
        return topEntry(viewerCounts);
    }

    public int getTopActiveCount() {
        return topCount(viewerCounts);
    }

    /** Топ зрителей по числу команд + любимая и разбивка. */
    public List<ViewerCommandStat> snapshotViewerStats(int limit) {
        List<ViewerCommandStat> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : viewerCounts.entrySet()) {
            String key = e.getKey();
            String display = viewerDisplayNames.getOrDefault(key, key);
            Map<String, Integer> cmds = viewerCommands.getOrDefault(key, Map.of());
            List<Map.Entry<String, Integer>> ranked = cmds.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .toList();
            String favId = ranked.isEmpty() ? "" : ranked.get(0).getKey();
            String favRu = favId.isEmpty() ? "-" : ChatGameCommand.fromId(favId)
                    .map(ChatGameCommand::getRussianName).orElse(favId);
            int favCount = ranked.isEmpty() ? 0 : ranked.get(0).getValue();
            List<CommandCount> top = new ArrayList<>();
            for (int i = 0; i < Math.min(6, ranked.size()); i++) {
                var r = ranked.get(i);
                String ru = ChatGameCommand.fromId(r.getKey())
                        .map(ChatGameCommand::getRussianName).orElse(r.getKey());
                top.add(new CommandCount(r.getKey(), ru, r.getValue()));
            }
            out.add(new ViewerCommandStat(display, e.getValue(), favId, favRu, favCount, top));
        }
        out.sort(Comparator.comparingInt(ViewerCommandStat::total).reversed()
                .thenComparing(v -> v.name().toLowerCase(Locale.ROOT)));
        if (limit > 0 && out.size() > limit) {
            return new ArrayList<>(out.subList(0, limit));
        }
        return out;
    }

    private static String topEntry(Map<String, Integer> map) {
        return map.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }

    private static int topCount(Map<String, Integer> map) {
        return map.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    public record CommandCount(String id, String ru, int count) {}

    public record ViewerCommandStat(
            String name,
            int total,
            String favoriteId,
            String favoriteRu,
            int favoriteCount,
            List<CommandCount> topCommands
    ) {}
}
