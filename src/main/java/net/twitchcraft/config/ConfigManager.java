package net.twitchcraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.twitchcraft.TwitchCraftMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TwitchCraftConfig config = new TwitchCraftConfig();
    private static Path configPath;
    private static int revision;

    private ConfigManager() {}

    public static void load() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("twitchcraft.json");
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                TwitchCraftConfig loaded = GSON.fromJson(reader, TwitchCraftConfig.class);
                if (loaded != null) {
                    config = loaded;
                    ensureDefaults();
                    revision++;
                }
            } catch (IOException e) {
                TwitchCraftMod.LOGGER.error("Failed to load config", e);
            }
        } else {
            ensureDefaults();
            save();
        }
    }

    private static void ensureDefaults() {
        ConfigDefaults.apply(config);
        ConfigValidator.sanitize(config);
    }

    public static void save() {
        if (configPath == null) {
            configPath = FabricLoader.getInstance().getConfigDir().resolve("twitchcraft.json");
        }
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(config, writer);
            }
            revision++;
        } catch (IOException e) {
            TwitchCraftMod.LOGGER.error("Failed to save config", e);
        }
    }

    public static TwitchCraftConfig get() {
        return config;
    }

    /** Растёт при load/save/set — кэши HUD и профилей сбрасываются по этому номеру. */
    public static int revision() {
        return revision;
    }

    public static void set(TwitchCraftConfig newConfig) {
        config = newConfig;
        ensureDefaults();
        save();
    }
}
