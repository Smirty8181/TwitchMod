package net.twitchcraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.twitchcraft.TwitchCraftMod;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Экспорт/импорт шарингового пресета без секретов (OAuth, DA-токены, ники).
 * Формат: JSON с полем {@code twitchcraftPresetVersion}.
 */
public final class PresetIO {
    public static final int PRESET_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type COMMANDS_TYPE = new TypeToken<Map<String, TwitchCraftConfig.CommandSettings>>() {}.getType();
    private static final Type TIERS_TYPE = new TypeToken<List<TwitchCraftConfig.DonationTier>>() {}.getType();
    private static final Type CUSTOM_TYPE = new TypeToken<List<CustomCommandDefinition>>() {}.getType();

    private PresetIO() {}

    /** DTO для шаринга — без токенов и привязки к стримеру. */
    public static final class ShareablePreset {
        public int twitchcraftPresetVersion = PRESET_VERSION;
        public String name = "TwitchCraft preset";
        public String audiencePreset = "medium";
        public String streamGenre = "vanilla";
        public boolean balancePresetApplied = true;
        public TwitchCraftConfig.SecuritySettings security;
        public TwitchCraftConfig.FeedbackSettings feedback;
        public TwitchCraftConfig.ScoreboardSettings scoreboard;
        public TwitchCraftConfig.UiSettings ui;
        public TwitchCraftConfig.StreamSettings stream;
        public TwitchCraftConfig.DeathSettings death;
        public TwitchCraftConfig.HudSettings hud;
        public TwitchCraftConfig.CooldownPackSettings cooldownPacks;
        public Map<String, TwitchCraftConfig.CommandSettings> commands;
        public List<CustomCommandDefinition> customCommands;
        public boolean donationsEnabled;
        public int donationsPollIntervalSeconds = 6;
        public boolean donationsBypassCooldown = true;
        public List<TwitchCraftConfig.DonationTier> donationTiers;
        public boolean twitchAutoConnect = true;
        public boolean twitchEventSubEnabled = true;
        public int twitchBitsMinAmount = 100;
    }

    public static ShareablePreset fromConfig(TwitchCraftConfig config) {
        ShareablePreset p = new ShareablePreset();
        if (config == null) return p;
        p.name = "TwitchCraft · " + (config.audiencePreset != null ? config.audiencePreset : "medium");
        p.audiencePreset = config.audiencePreset;
        p.streamGenre = config.streamGenre != null ? config.streamGenre : "vanilla";
        p.balancePresetApplied = config.balancePresetApplied;
        p.security = deepCopy(config.security, TwitchCraftConfig.SecuritySettings.class);
        if (p.security != null) {
            p.security.whitelist = new ArrayList<>();
            p.security.moderators = new ArrayList<>();
            p.security.whitelistEnabled = false;
        }
        p.feedback = deepCopy(config.feedback, TwitchCraftConfig.FeedbackSettings.class);
        p.scoreboard = deepCopy(config.scoreboard, TwitchCraftConfig.ScoreboardSettings.class);
        p.ui = deepCopy(config.ui, TwitchCraftConfig.UiSettings.class);
        p.stream = deepCopy(config.stream, TwitchCraftConfig.StreamSettings.class);
        p.death = deepCopy(config.death, TwitchCraftConfig.DeathSettings.class);
        p.hud = deepCopy(config.hud, TwitchCraftConfig.HudSettings.class);
        p.cooldownPacks = deepCopy(config.cooldownPacks, TwitchCraftConfig.CooldownPackSettings.class);
        p.commands = config.commands != null
                ? GSON.fromJson(GSON.toJson(config.commands), COMMANDS_TYPE) : new HashMap<>();
        p.customCommands = config.customCommands != null
                ? GSON.fromJson(GSON.toJson(config.customCommands), CUSTOM_TYPE) : new ArrayList<>();
        if (config.donations != null) {
            p.donationsEnabled = config.donations.enabled;
            p.donationsPollIntervalSeconds = config.donations.pollIntervalSeconds;
            p.donationsBypassCooldown = config.donations.bypassCooldown;
            p.donationTiers = config.donations.tiers != null
                    ? GSON.fromJson(GSON.toJson(config.donations.tiers), TIERS_TYPE) : new ArrayList<>();
        }
        if (config.twitch != null) {
            p.twitchAutoConnect = config.twitch.autoConnect;
            p.twitchEventSubEnabled = config.twitch.eventSubEnabled;
            p.twitchBitsMinAmount = config.twitch.bitsMinAmount;
        }
        return p;
    }

    public static String toJson(TwitchCraftConfig config) {
        return GSON.toJson(fromConfig(config));
    }

    /** Применяет пресет в рабочую копию конфига (токены/каналы не трогает). @return ошибка или null */
    public static String applyTo(TwitchCraftConfig target, String json) {
        if (target == null || json == null || json.isBlank()) {
            return "Пустой пресет";
        }
        ShareablePreset p;
        try {
            p = GSON.fromJson(json.trim(), ShareablePreset.class);
        } catch (Exception e) {
            return "Не удалось разобрать JSON: " + e.getMessage();
        }
        if (p == null) return "Пустой пресет";
        if (p.twitchcraftPresetVersion <= 0) {
            return "Это не пресет TwitchCraft (нет twitchcraftPresetVersion)";
        }
        if (p.security != null) {
            var wl = target.security != null ? target.security.whitelist : null;
            var mods = target.security != null ? target.security.moderators : null;
            boolean wlOn = target.security != null && target.security.whitelistEnabled;
            target.security = p.security;
            if (wl != null) target.security.whitelist = wl;
            if (mods != null) target.security.moderators = mods;
            target.security.whitelistEnabled = wlOn;
        }
        if (p.feedback != null) target.feedback = p.feedback;
        if (p.scoreboard != null) target.scoreboard = p.scoreboard;
        if (p.ui != null) target.ui = p.ui;
        if (p.stream != null) target.stream = p.stream;
        if (p.death != null) target.death = p.death;
        if (p.hud != null) target.hud = p.hud;
        if (p.cooldownPacks != null) target.cooldownPacks = p.cooldownPacks;
        if (p.commands != null) target.commands = p.commands;
        if (p.customCommands != null) target.customCommands = p.customCommands;
        if (target.donations == null) target.donations = new TwitchCraftConfig.DonationSettings();
        target.donations.enabled = p.donationsEnabled;
        target.donations.pollIntervalSeconds = p.donationsPollIntervalSeconds;
        target.donations.bypassCooldown = p.donationsBypassCooldown;
        if (p.donationTiers != null) target.donations.tiers = p.donationTiers;
        if (target.twitch == null) target.twitch = new TwitchCraftConfig.TwitchSettings();
        target.twitch.autoConnect = p.twitchAutoConnect;
        target.twitch.eventSubEnabled = p.twitchEventSubEnabled;
        target.twitch.bitsMinAmount = p.twitchBitsMinAmount;
        if (p.audiencePreset != null && !p.audiencePreset.isBlank()) {
            target.audiencePreset = p.audiencePreset;
        }
        if (p.streamGenre != null && !p.streamGenre.isBlank()) {
            target.streamGenre = p.streamGenre;
        }
        target.balancePresetApplied = true;
        ConfigValidator.sanitize(target);
        HudLegacySync.ensure(target);
        return null;
    }

    /** Пишет файл в {@code config/twitchcraft-presets/}. @return путь или null */
    public static Path exportToFile(TwitchCraftConfig config) {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("twitchcraft-presets");
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path file = dir.resolve("preset-" + stamp + ".json");
        try {
            Files.createDirectories(dir);
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(fromConfig(config), writer);
            }
            return file;
        } catch (IOException e) {
            TwitchCraftMod.LOGGER.error("Failed to export preset", e);
            return null;
        }
    }

    private static <T> T deepCopy(Object src, Class<T> type) {
        if (src == null) return null;
        return GSON.fromJson(GSON.toJson(src), type);
    }
}
