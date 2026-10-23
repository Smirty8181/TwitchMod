package net.twitchcraft.config;

/** Пресеты баланса под размер аудитории Twitch. */
public final class AudiencePresets {
    public static final String SMALL = "small";
    public static final String MEDIUM = "medium";
    public static final String LARGE = "large";
    public static final String MEGA = "mega";
    public static final String DUO = "duo";

    private AudiencePresets() {}

    public static void apply(TwitchCraftConfig config, String preset) {
        if (preset == null || preset.isBlank()) preset = MEDIUM;
        config.audiencePreset = preset.toLowerCase();
        switch (config.audiencePreset) {
            case SMALL -> applySmall(config);
            case LARGE -> applyLarge(config);
            case MEGA -> applyMega(config);
            case DUO -> DuoPresets.apply(config);
            default -> applyMedium(config);
        }
        config.balancePresetApplied = true;
    }

    public static void applySmall(TwitchCraftConfig config) {
        BalancePresets.apply(config);
        scale(config, 0.75, 1.25, 5);
        config.scoreboard.pageRotateSeconds = 6;
        config.scoreboard.maxCommandLines = 6;
        config.security.mobSpawnRateLimitPerMinute = 16;
        config.security.tntRateLimitPerMinute = 8;
        config.security.antiSpamSeconds = 5;
    }

    public static void applyMedium(TwitchCraftConfig config) {
        BalancePresets.apply(config);
        config.scoreboard.pageRotateSeconds = 8;
        config.scoreboard.maxCommandLines = 5;
        config.security.antiSpamSeconds = 20;
    }

    public static void applyLarge(TwitchCraftConfig config) {
        BalancePresets.apply(config);
        scale(config, 1.35, 0.75, 20);
        config.scoreboard.pageRotateSeconds = 10;
        config.scoreboard.maxCommandLines = 5;
        config.security.mobSpawnRateLimitPerMinute = 10;
        config.security.tntRateLimitPerMinute = 4;
        config.security.chaosRateLimitPerMinute = 8;
        config.security.antiSpamSeconds = 20;
        config.stream.chaosMeterMax = 45;
    }

    public static void applyMega(TwitchCraftConfig config) {
        BalancePresets.apply(config);
        scale(config, 1.75, 0.55, 12);
        config.scoreboard.pageRotateSeconds = 12;
        config.scoreboard.maxCommandLines = 4;
        config.security.mobSpawnRateLimitPerMinute = 8;
        config.security.tntRateLimitPerMinute = 3;
        config.security.chaosRateLimitPerMinute = 6;
        config.security.effectRateLimitPerMinute = 10;
        config.security.antiSpamSeconds = 12;
        config.stream.chaosMeterMax = 40;
        config.stream.randomEventMinMinutes = 5;
        config.stream.randomEventMaxMinutes = 9;
    }

    private static void scale(TwitchCraftConfig config, double cdMult, double usesMult, int antiSpam) {
        for (net.twitchcraft.commands.ChatGameCommand cmd : net.twitchcraft.commands.ChatGameCommand.values()) {
            if (cmd.getCategory() == net.twitchcraft.commands.ChatGameCommand.Category.MODERATOR) continue;
            var s = config.getCommandSettings(cmd.getId(), cmd.getDefaultCooldown());
            s.cooldownSeconds = Math.max(5, (int) Math.round(s.cooldownSeconds * cdMult));
            if (s.maxUsesPerMinute > 0) {
                s.maxUsesPerMinute = Math.max(1, (int) Math.round(s.maxUsesPerMinute * usesMult));
            }
        }
        config.security.antiSpamSeconds = antiSpam;
    }

    public static String label(String preset) {
        return switch (preset != null ? preset : MEDIUM) {
            case SMALL -> "до 30 зрителей";
            case LARGE -> "100–250 зрителей";
            case MEGA -> "250+ зрителей";
            case DUO -> "2 стримера (LAN)";
            default -> "30–100 зрителей";
        };
    }
}
