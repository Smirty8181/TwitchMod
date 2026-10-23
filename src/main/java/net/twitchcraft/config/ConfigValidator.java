package net.twitchcraft.config;

import net.twitchcraft.commands.ChatGameCommand;

import java.util.ArrayList;
import java.util.Locale;

/** Валидация и санитизация конфига (C2S, reload). */
public final class ConfigValidator {
    public static final int MAX_JSON_CHARS = 256_000;

    private ConfigValidator() {}

    public static String rejectOversized(String settingsJson, String profileJson) {
        if (settingsJson != null && settingsJson.length() > MAX_JSON_CHARS) {
            return "settings payload too large";
        }
        if (profileJson != null && profileJson.length() > MAX_JSON_CHARS) {
            return "profile payload too large";
        }
        return null;
    }

    public static void sanitize(TwitchCraftConfig config) {
        if (config == null) return;
        ConfigDefaults.apply(config);

        if (config.security != null) {
            config.security.antiSpamSeconds = clamp(config.security.antiSpamSeconds, 0, 300);
            config.security.mobSpawnRateLimitPerMinute = clamp(config.security.mobSpawnRateLimitPerMinute, 0, 120);
            config.security.tntRateLimitPerMinute = clamp(config.security.tntRateLimitPerMinute, 0, 60);
            config.security.chaosRateLimitPerMinute = clamp(config.security.chaosRateLimitPerMinute, 0, 120);
            config.security.effectRateLimitPerMinute = clamp(config.security.effectRateLimitPerMinute, 0, 120);
            config.security.movementRateLimitPerMinute = clamp(config.security.movementRateLimitPerMinute, 0, 120);
            config.security.inventoryRateLimitPerMinute = clamp(config.security.inventoryRateLimitPerMinute, 0, 120);
            config.security.worldRateLimitPerMinute = clamp(config.security.worldRateLimitPerMinute, 0, 60);
            config.security.funRateLimitPerMinute = clamp(config.security.funRateLimitPerMinute, 0, 120);
        }

        if (config.stream != null) {
            config.stream.chaosMeterMax = clamp(config.stream.chaosMeterMax, 1, 10_000);
            config.stream.voteDurationSeconds = clamp(config.stream.voteDurationSeconds, 5, 300);
            config.stream.courtDurationSeconds = clamp(config.stream.courtDurationSeconds, 5, 300);
            config.stream.courtPenaltyMinutes = clamp(config.stream.courtPenaltyMinutes, 1, 120);
            config.stream.diceShieldMinutes = clamp(config.stream.diceShieldMinutes, 1, 15);
            config.stream.challengeSeconds = clamp(config.stream.challengeSeconds, 10, 90);
            config.stream.antSeconds = clamp(config.stream.antSeconds, 8, 60);
            config.stream.titanSeconds = clamp(config.stream.titanSeconds, 6, 40);
            config.stream.unnamedPatronMinutes = clamp(config.stream.unnamedPatronMinutes, 6, 30);
            config.stream.cipherRiddleSeconds = clamp(config.stream.cipherRiddleSeconds, 60, 600);
            config.stream.randomEventMinMinutes = clamp(config.stream.randomEventMinMinutes, 2, 20);
            config.stream.randomEventMaxMinutes = clamp(config.stream.randomEventMaxMinutes,
                    Math.max(config.stream.randomEventMinMinutes, 3), 30);
            config.stream.worldEncounterMinMinutes = clamp(config.stream.worldEncounterMinMinutes, 4, 20);
            config.stream.worldEncounterMaxMinutes = clamp(config.stream.worldEncounterMaxMinutes,
                    Math.max(config.stream.worldEncounterMinMinutes, 6), 30);
        }

        if (config.donations != null) {
            config.donations.pollIntervalSeconds = clamp(config.donations.pollIntervalSeconds, 5, 120);
        }

        if (config.scoreboard != null) {
            config.scoreboard.maxCommandLines = clamp(config.scoreboard.maxCommandLines, 1, 50);
            config.scoreboard.pageRotateSeconds = clamp(config.scoreboard.pageRotateSeconds, 3, 600);
        }

        if (config.ui != null) {
            config.ui.hudScale = clampFloat(config.ui.hudScale, 0.75f, 1.35f, 1f);
            if (config.ui.donationPosition == null) config.ui.donationPosition = "top";
            if (config.ui.feedPosition == null) config.ui.feedPosition = "bottom";
            if (config.ui.theme == null || config.ui.theme.isBlank()) config.ui.theme = "anime";
        }

        if (config.twitch != null) {
            config.twitch.bitsMinAmount = clamp(config.twitch.bitsMinAmount, 1, 10_000);
        }

        if (config.death != null) {
            config.death.deathsForPenalty = clamp(config.death.deathsForPenalty, 2, 20);
            config.death.deathWindowMinutes = clamp(config.death.deathWindowMinutes, 1, 60);
            config.death.evilBlockMinutes = clamp(config.death.evilBlockMinutes, 1, 30);
            if (config.death.respawnGraceSeconds < 8) config.death.respawnGraceSeconds = 20;
            config.death.respawnGraceSeconds = clamp(config.death.respawnGraceSeconds, 8, 60);
            if (config.death.evilPaceSeconds < 8) config.death.evilPaceSeconds = 12;
            config.death.evilPaceSeconds = clamp(config.death.evilPaceSeconds, 8, 30);
        }

        if (config.commands != null) {
            BalancePresets.snapOutrageousCooldowns(config);
            for (ChatGameCommand cmd : ChatGameCommand.values()) {
                var settings = config.getCommandSettings(cmd.getId(), cmd.getDefaultCooldown());
                settings.cooldownSeconds = clamp(settings.cooldownSeconds, 0, 3600);
                settings.maxUsesPerMinute = clamp(settings.maxUsesPerMinute, 0, 120);
            }
        }

        if (config.customCommands != null) {
            if (config.customCommands.size() > 40) {
                config.customCommands = new ArrayList<>(config.customCommands.subList(0, 40));
            }
            for (var def : config.customCommands) {
                if (def == null) continue;
                if (def.trigger != null) {
                    def.trigger = def.trigger.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-zа-яё0-9_]", "");
                }
                if (def.id == null || def.id.isBlank()) {
                    def.id = "custom_" + (def.trigger == null ? "cmd" : def.trigger);
                }
                def.cooldownSeconds = clamp(def.cooldownSeconds, 0, 3600);
                def.maxUsesPerMinute = clamp(def.maxUsesPerMinute, 0, 120);
                def.durationSeconds = clamp(def.durationSeconds, 1, 3600);
                def.amplifier = clamp(def.amplifier, 0, 255);
                def.itemCount = clamp(def.itemCount, 1, 64);
                def.mobCount = clamp(def.mobCount, 1, 10);
                if (def.actionType == null || def.actionType.isBlank()) def.actionType = "effect";
            }
        }

        if (config.cooldownPacks != null) {
            var p = config.cooldownPacks;
            p.goodPackSeconds = clamp(p.goodPackSeconds, 0, 3600);
            p.chaosPackSeconds = clamp(p.chaosPackSeconds, 0, 3600);
            p.mobPackSeconds = clamp(p.mobPackSeconds, 0, 3600);
            p.tntPackSeconds = clamp(p.tntPackSeconds, 0, 3600);
            p.effectPackSeconds = clamp(p.effectPackSeconds, 0, 3600);
            p.movementPackSeconds = clamp(p.movementPackSeconds, 0, 3600);
            p.inventoryPackSeconds = clamp(p.inventoryPackSeconds, 0, 3600);
            p.worldPackSeconds = clamp(p.worldPackSeconds, 0, 3600);
            p.funPackSeconds = clamp(p.funPackSeconds, 0, 3600);
        }

        if (config.death != null) {
            var d = config.death;
            d.deathsForPenalty = clamp(d.deathsForPenalty, 2, 20);
            d.deathWindowMinutes = clamp(d.deathWindowMinutes, 1, 60);
            d.evilBlockMinutes = clamp(d.evilBlockMinutes, 1, 30);
            d.respawnGraceSeconds = clamp(d.respawnGraceSeconds, 8, 60);
            d.evilPaceSeconds = clamp(d.evilPaceSeconds, 8, 30);
            if (d.coordsDisplay == null || d.coordsDisplay.isBlank()) d.coordsDisplay = "overlay";
        }

        if (config.hud != null) {
            config.hud.globalScale = clampFloat(config.hud.globalScale, 0.5f, 1.5f, 1f);
            sanitizeElement(config.hud.sidebar);
            sanitizeElement(config.hud.commandFeed);
            if (config.hud.commandDeny == null) config.hud.commandDeny = HudDefaults.commandDeny();
            sanitizeElement(config.hud.commandDeny);
            sanitizeElement(config.hud.deathToast);
            sanitizeElement(config.hud.pinnedDeathCoords);
            sanitizeElement(config.hud.donationToast);
            sanitizeElement(config.hud.comboChain);
            sanitizeElement(config.hud.donationWheel);
            sanitizeElement(config.hud.commandFx);
            sanitizeElement(config.hud.donationFx);
            if (config.hud.duoScore == null) config.hud.duoScore = HudDefaults.duoScore();
            sanitizeElement(config.hud.duoScore);
            if (config.hud.sidebarContent != null) {
                config.hud.sidebarContent.maxCommandLines = clamp(config.hud.sidebarContent.maxCommandLines, 3, 24);
                config.hud.sidebarContent.pageRotateSeconds = clamp(config.hud.sidebarContent.pageRotateSeconds, 3, 60);
            }
            if (config.hud.commandFeedContent != null) {
                config.hud.commandFeedContent.maxEntries = clamp(config.hud.commandFeedContent.maxEntries, 1, 4);
            }
            HudLegacySync.pushToLegacy(config);
        }
    }

    private static void sanitizeElement(TwitchCraftConfig.HudElementConfig el) {
        if (el == null) return;
        el.scale = clampFloat(el.scale, 0.5f, 2f, 1f);
        el.opacity = clampFloat(el.opacity, 0.2f, 1f, 1f);
        el.offsetX = clamp(el.offsetX, -500, 500);
        el.offsetY = clamp(el.offsetY, -500, 500);
        if (el.anchor == null || el.anchor.isBlank()) el.anchor = "TOP_RIGHT";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max, float fallback) {
        if (value <= 0) return fallback;
        return Math.max(min, Math.min(max, value));
    }
}
