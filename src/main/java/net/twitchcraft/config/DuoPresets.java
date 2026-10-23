package net.twitchcraft.config;

/** Баланс и производительность для локального сервера на 2 стримеров. */
public final class DuoPresets {
    private DuoPresets() {}

    public static void apply(TwitchCraftConfig config) {
        config.stream.duoStreamMode = true;
        config.stream.randomEventMinMinutes = 5;
        config.stream.randomEventMaxMinutes = 10;
        config.stream.chaosMeterMax = 40;
        config.stream.battleMaxHp = 80;
        config.scoreboard.pageRotateSeconds = 10;
        config.scoreboard.maxCommandLines = 5;
        config.donations.pollIntervalSeconds = 6;
        config.donations.bypassCooldown = true;
        config.security.antiSpamSeconds = 20;
        BalancePresets.applyDuo(config);
    }
}
