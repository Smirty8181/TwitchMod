package net.twitchcraft.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Масштаб меню R/K — общий для обоих экранов, сохраняется в config. */
public final class TwitchMenuScale {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final float MIN = 0.65f;
    private static final float MAX = 1.55f;

    private static float scale = 1.0f;
    private static Path configPath;

    private TwitchMenuScale() {}

    public static void load() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("twitchcraft-ui.json");
        if (!Files.exists(configPath)) return;
        try (Reader r = Files.newBufferedReader(configPath)) {
            UiFile file = GSON.fromJson(r, UiFile.class);
            if (file != null) {
                scale = Mth.clamp(file.menuScale, MIN, MAX);
            }
        } catch (IOException ignored) {
        }
    }

    public static void save() {
        if (configPath == null) {
            configPath = FabricLoader.getInstance().getConfigDir().resolve("twitchcraft-ui.json");
        }
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer w = Files.newBufferedWriter(configPath)) {
                GSON.toJson(new UiFile(scale), w);
            }
        } catch (IOException ignored) {
        }
    }

    public static float get() {
        return scale;
    }

    public static boolean set(float value) {
        float clamped = Mth.clamp(value, MIN, MAX);
        if (Math.abs(clamped - scale) < 0.001f) return false;
        scale = clamped;
        save();
        return true;
    }

    public static boolean adjust(float delta) {
        return set(scale + delta);
    }

    public static int percent() {
        return Math.round(scale * 100);
    }

    private static final class UiFile {
        float menuScale = 1.0f;

        UiFile() {}

        UiFile(float menuScale) {
            this.menuScale = menuScale;
        }
    }
}
