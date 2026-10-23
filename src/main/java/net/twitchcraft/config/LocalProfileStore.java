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

/** Локальные Twitch/DA данные — только на этом ПК (config/twitchcraft-local.json). */
public final class LocalProfileStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path path;

    private LocalProfileStore() {}

    public static StreamerProfile load() {
        if (path == null) {
            path = FabricLoader.getInstance().getConfigDir().resolve("twitchcraft-local.json");
        }
        if (!Files.exists(path)) return new StreamerProfile();
        try (Reader reader = Files.newBufferedReader(path)) {
            StreamerProfile loaded = GSON.fromJson(reader, StreamerProfile.class);
            return loaded != null ? loaded : new StreamerProfile();
        } catch (IOException e) {
            TwitchCraftMod.LOGGER.warn("Failed to load local profile", e);
            return new StreamerProfile();
        }
    }

    public static void save(StreamerProfile profile) {
        if (path == null) {
            path = FabricLoader.getInstance().getConfigDir().resolve("twitchcraft-local.json");
        }
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(profile, writer);
            }
        } catch (IOException e) {
            TwitchCraftMod.LOGGER.error("Failed to save local profile", e);
        }
    }
}
