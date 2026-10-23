package net.twitchcraft.stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.twitchcraft.TwitchCraftMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Статистика живёт в папке мира. Новый / пересозданный мир = чистый лист.
 */
public final class StreamStateStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int AUTOSAVE_INTERVAL_TICKS = 20 * 60 * 5;
    private static Path path;
    private static int ticksSinceSave;
    private static long boundSeed;
    private static String boundWorld = "";

    private StreamStateStore() {}

    public static void load(MinecraftServer server) {
        resetMemory();
        path = worldStatePath(server);
        boundSeed = worldSeed(server);
        boundWorld = worldName(server);

        Path legacy = FabricLoader.getInstance().getConfigDir().resolve("twitchcraft-stream-state.json");
        if (Files.exists(legacy)) {
            try {
                Files.deleteIfExists(legacy);
                TwitchCraftMod.LOGGER.info("TwitchCraft: removed legacy global stats (now per-world)");
            } catch (IOException ignored) {
            }
        }

        if (path == null || !Files.exists(path)) {
            TwitchCraftMod.LOGGER.info("TwitchCraft: new world — stats empty ({})", boundWorld);
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            StateFile data = GSON.fromJson(reader, StateFile.class);
            if (data == null) return;
            if (data.worldSeed != 0 && data.worldSeed != boundSeed) {
                TwitchCraftMod.LOGGER.info("TwitchCraft: world seed changed — stats reset");
                resetMemory();
                save(server);
                return;
            }
            StreamStats.restoreAll(data.players != null ? data.players : Map.of());
            Graveyard.get().restore(data.graveyard != null ? data.graveyard : List.of());
            DeathTracker.restoreFull(data.deathStates, data.deathTotals);
            CampaignJourney.restore(data.campaign);
            MilestoneNarrator.restore(data.milestones);
            TwitchCraftMod.LOGGER.info("TwitchCraft: loaded world stats ({} players, {} graves)",
                    data.players != null ? data.players.size() : 0,
                    data.graveyard != null ? data.graveyard.size() : 0);
        } catch (Exception e) {
            TwitchCraftMod.LOGGER.warn("Failed to load world stream state", e);
            resetMemory();
        }
    }

    public static void save() {
        save(null);
    }

    public static void save(MinecraftServer server) {
        if (path == null && server != null) {
            path = worldStatePath(server);
            boundSeed = worldSeed(server);
            boundWorld = worldName(server);
        }
        if (path == null) return;
        StateFile data = new StateFile();
        data.version = 2;
        data.savedAt = System.currentTimeMillis();
        data.worldSeed = boundSeed;
        data.worldName = boundWorld;
        data.players = StreamStats.exportAll();
        data.graveyard = Graveyard.get().exportEntries();
        data.deathTotals = DeathTracker.exportTotals();
        data.deathStates = DeathTracker.exportFull();
        data.campaign = CampaignJourney.export();
        data.milestones = MilestoneNarrator.export();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            TwitchCraftMod.LOGGER.error("Failed to save world stream state", e);
        }
    }

    public static void resetAndSave(MinecraftServer server) {
        resetMemory();
        if (server != null) {
            path = worldStatePath(server);
            boundSeed = worldSeed(server);
            boundWorld = worldName(server);
        }
        save(server);
        DuoScoreSync.push(server);
        TwitchCraftMod.LOGGER.info("TwitchCraft: stats reset for world {}", boundWorld);
    }

    public static void tickAutosave() {
        ticksSinceSave++;
        if (ticksSinceSave >= AUTOSAVE_INTERVAL_TICKS) {
            ticksSinceSave = 0;
            save();
        }
    }

    public static void resetMemory() {
        StreamStats.resetAll();
        Graveyard.get().clear();
        DeathTracker.resetAll();
        CampaignJourney.reset();
        MilestoneNarrator.reset();
        ViewerKarma.reset();
        StreamerCharge.reset();
    }

    private static Path worldStatePath(MinecraftServer server) {
        if (server == null) return null;
        return server.getWorldPath(LevelResource.ROOT).resolve("twitchcraft-stream-state.json");
    }

    private static long worldSeed(MinecraftServer server) {
        try {
            return server.overworld().getSeed();
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String worldName(MinecraftServer server) {
        try {
            return server.getWorldData().getLevelName();
        } catch (Exception e) {
            return "";
        }
    }

    public static final class StateFile {
        public int version = 2;
        public long savedAt;
        public long worldSeed;
        public String worldName = "";
        public Map<String, StreamStats.StatsSnapshot> players = new HashMap<>();
        public List<Graveyard.Entry> graveyard = new ArrayList<>();
        public Map<String, Integer> deathTotals = new HashMap<>();
        public Map<String, DeathTracker.Snapshot> deathStates = new HashMap<>();
        public CampaignJourney.Snapshot campaign;
        public MilestoneNarrator.Snapshot milestones;
    }
}
