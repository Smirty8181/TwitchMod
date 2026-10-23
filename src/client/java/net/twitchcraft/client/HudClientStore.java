package net.twitchcraft.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.HudDefaults;
import net.twitchcraft.config.TwitchCraftConfig;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Раскладка HUD — только этот ПК, не мир и не конфиг напарника.
 * Файл: config/twitchcraft-hud.json
 */
public final class HudClientStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TwitchCraftConfig.HudSettings hud;
    private static Path path;

    private HudClientStore() {}

    public static void load() {
        if (path == null) {
            path = FabricLoader.getInstance().getConfigDir().resolve("twitchcraft-hud.json");
        }
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                TwitchCraftConfig.HudSettings loaded = GSON.fromJson(reader, TwitchCraftConfig.HudSettings.class);
                if (loaded != null) {
                    hud = loaded;
                    fillMissing(hud);
                    return;
                }
            } catch (Exception e) {
                TwitchCraftMod.LOGGER.warn("Failed to load twitchcraft-hud.json, migrating from main config", e);
            }
        }
        hud = copy(ConfigManager.get() != null ? ConfigManager.get().hud : null);
        fillMissing(hud);
        save();
    }

    public static TwitchCraftConfig.HudSettings get() {
        if (hud == null) load();
        return hud;
    }

    /** Кладёт копию локального HUD в рабочий конфиг (меню K). */
    public static void applyTo(TwitchCraftConfig config) {
        if (config == null) return;
        config.hud = copy(get());
        fillMissing(config.hud);
    }

    public static void replaceAndSave(TwitchCraftConfig.HudSettings incoming) {
        hud = copy(incoming);
        fillMissing(hud);
        save();
    }

    public static void save() {
        if (hud == null) return;
        if (path == null) {
            path = FabricLoader.getInstance().getConfigDir().resolve("twitchcraft-hud.json");
        }
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(hud, writer);
            }
        } catch (IOException e) {
            TwitchCraftMod.LOGGER.error("Failed to save twitchcraft-hud.json", e);
        }
    }

    private static TwitchCraftConfig.HudSettings copy(TwitchCraftConfig.HudSettings src) {
        if (src == null) return new TwitchCraftConfig.HudSettings();
        TwitchCraftConfig.HudSettings copy = GSON.fromJson(GSON.toJson(src), TwitchCraftConfig.HudSettings.class);
        return copy != null ? copy : new TwitchCraftConfig.HudSettings();
    }

    private static void fillMissing(TwitchCraftConfig.HudSettings h) {
        if (h == null) return;
        if (h.globalScale <= 0) h.globalScale = 1f;
        if (h.sidebar == null) h.sidebar = HudDefaults.sidebar();
        if (h.commandFeed == null) h.commandFeed = HudDefaults.commandFeed();
        if (h.commandDeny == null) h.commandDeny = HudDefaults.commandDeny();
        if (h.deathToast == null) h.deathToast = HudDefaults.deathToast();
        if (h.pinnedDeathCoords == null) h.pinnedDeathCoords = HudDefaults.pinnedDeathCoords();
        if (h.donationToast == null) h.donationToast = HudDefaults.donationToast();
        if (h.comboChain == null) h.comboChain = HudDefaults.comboChain();
        if (h.donationWheel == null) h.donationWheel = HudDefaults.donationWheel();
        if (h.commandFx == null) h.commandFx = HudDefaults.commandFx();
        if (h.donationFx == null) h.donationFx = HudDefaults.donationFx();
        if (h.duoScore == null) h.duoScore = HudDefaults.duoScore();
        if (h.sidebarContent == null) h.sidebarContent = new TwitchCraftConfig.HudSidebarSections();
        if (h.commandFeedContent == null) h.commandFeedContent = new TwitchCraftConfig.HudCommandFeedSections();
    }
}
