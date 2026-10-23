package net.twitchcraft.config;

import net.twitchcraft.commands.ChatGameCommand;

import java.util.Locale;
import java.util.Set;

/**
 * Быстрые профили стрима: ванила / хардкор / модпак.
 * Меняют включённость команд и жёсткость, без ручной правки каждого КД.
 * Размер аудитории ({@link AudiencePresets}) применяется отдельно и может идти после.
 */
public final class StreamGenrePresets {
    public static final String VANILLA = "vanilla";
    public static final String HARDCORE = "hardcore";
    public static final String MODPACK = "modpack";

    private static final Set<String> MODPACK_IDS = Set.of(
            "goose", "chaoschickens", "brainrot", "prime", "kva", "amongus", "oia",
            "vine", "troll", "lucky", "unlucky", "verylucky", "goldenhen",
            "luckyroulette", "primerave", "piggyback", "carryon", "viewerarmy"
    );

    private static final Set<String> LETHAL_IDS = Set.of(
            "wither", "dragon", "phantom", "clear", "pit", "lava", "tntrain", "creeperstorm"
    );

    private StreamGenrePresets() {}

    public static void apply(TwitchCraftConfig config, String genre) {
        if (config == null) return;
        String g = genre == null || genre.isBlank() ? VANILLA : genre.toLowerCase(Locale.ROOT);
        // Базовые КД от баланса аудитории — чтобы не стакать множители
        String audience = config.audiencePreset != null ? config.audiencePreset : AudiencePresets.MEDIUM;
        AudiencePresets.apply(config, audience);
        config.streamGenre = g;
        switch (g) {
            case HARDCORE -> applyHardcore(config);
            case MODPACK -> applyModpack(config);
            default -> applyVanilla(config);
        }
    }

    public static void applyVanilla(TwitchCraftConfig config) {
        config.streamGenre = VANILLA;
        setModpackCommands(config, false);
        setLethal(config, false);
        // классика
        enable(config, "lightning", true);
        enable(config, "creeper", true);
        enable(config, "army", true);
        enable(config, "heal", true);
        enable(config, "tnt", true);

        config.security.disableInHardcore = true;
        config.security.antiSpamSeconds = Math.max(config.security.antiSpamSeconds, 6);
        if (config.death != null) {
            config.death.enabled = true;
            config.death.blockEvilCommands = true;
            config.death.deathsForPenalty = 3;
            config.death.evilBlockMinutes = 5;
            config.death.deathWindowMinutes = 10;
        }
        if (config.stream != null) {
            config.stream.revengeOnDeath = true;
            config.stream.courtEnabled = true;
            config.stream.houseMode = false;
        }
    }

    public static void applyHardcore(TwitchCraftConfig config) {
        config.streamGenre = HARDCORE;
        setModpackCommands(config, false);
        setLethal(config, false);
        // эффекты чата работают даже в hardcore-мире
        config.security.disableInHardcore = false;
        config.security.antiSpamSeconds = Math.max(10, config.security.antiSpamSeconds);
        config.security.mobSpawnRateLimitPerMinute = Math.min(config.security.mobSpawnRateLimitPerMinute, 8);
        config.security.tntRateLimitPerMinute = Math.min(config.security.tntRateLimitPerMinute, 3);
        config.security.chaosRateLimitPerMinute = Math.min(config.security.chaosRateLimitPerMinute, 6);

        enable(config, "lightning", true);
        enable(config, "creeper", true);
        enable(config, "heal", true);
        enable(config, "save", true);
        enable(config, "totem", true);
        enable(config, "tnt", false);
        enable(config, "anvil", false);
        enable(config, "meteor", false);

        if (config.death != null) {
            config.death.enabled = true;
            config.death.blockEvilCommands = true;
            config.death.deathsForPenalty = 2;
            config.death.evilBlockMinutes = 10;
            config.death.deathWindowMinutes = 15;
            config.death.showCoordinates = true;
        }
        if (config.stream != null) {
            config.stream.revengeOnDeath = true;
            config.stream.courtEnabled = true;
            config.stream.houseMode = false;
            config.stream.chaosMeterMax = Math.min(config.stream.chaosMeterMax, 40);
        }
        scaleCooldowns(config, 1.35);
    }

    public static void applyModpack(TwitchCraftConfig config) {
        config.streamGenre = MODPACK;
        setModpackCommands(config, true);
        // редкие боссы остаются выключенными, пока стример не включит вручную
        setLethal(config, false);
        config.security.disableInHardcore = true;
        config.security.funRateLimitPerMinute = Math.max(config.security.funRateLimitPerMinute, 14);
        if (config.stream != null) {
            config.stream.namedMobs = true;
            config.stream.ghostMobsEnabled = true;
            config.stream.houseMode = false;
        }
        if (config.death != null) {
            config.death.enabled = true;
            config.death.blockEvilCommands = true;
            config.death.deathsForPenalty = 3;
            config.death.evilBlockMinutes = 5;
        }
        // мем-команды чуть быстрее
        for (String id : MODPACK_IDS) {
            var s = config.getCommandSettings(id, 20);
            s.enabled = true;
            s.cooldownSeconds = Math.max(8, (int) Math.round(s.cooldownSeconds * 0.85));
        }
        scaleCooldowns(config, 0.95);
    }

    public static String label(String genre) {
        return switch (genre != null ? genre.toLowerCase(Locale.ROOT) : VANILLA) {
            case HARDCORE -> "Хардкор — без летала, жёстче КД";
            case MODPACK -> "Модпак — мем-команды вкл";
            default -> "Ванила — классический набор";
        };
    }

    public static String shortLabel(String genre) {
        return switch (genre != null ? genre.toLowerCase(Locale.ROOT) : VANILLA) {
            case HARDCORE -> "хардкор";
            case MODPACK -> "модпак";
            default -> "ванила";
        };
    }

    private static void setModpackCommands(TwitchCraftConfig config, boolean enabled) {
        for (String id : MODPACK_IDS) {
            enable(config, id, enabled);
        }
    }

    private static void setLethal(TwitchCraftConfig config, boolean enabled) {
        for (String id : LETHAL_IDS) {
            enable(config, id, enabled);
        }
    }

    private static void enable(TwitchCraftConfig config, String id, boolean on) {
        int defCd = ChatGameCommand.fromId(id).map(ChatGameCommand::getDefaultCooldown).orElse(20);
        config.getCommandSettings(id, defCd).enabled = on;
    }

    private static void scaleCooldowns(TwitchCraftConfig config, double mult) {
        if (Math.abs(mult - 1.0) < 0.01) return;
        for (ChatGameCommand cmd : ChatGameCommand.values()) {
            if (cmd.getCategory() == ChatGameCommand.Category.MODERATOR) continue;
            var s = config.getCommandSettings(cmd.getId(), cmd.getDefaultCooldown());
            s.cooldownSeconds = Math.max(5, (int) Math.round(s.cooldownSeconds * mult));
        }
    }
}
