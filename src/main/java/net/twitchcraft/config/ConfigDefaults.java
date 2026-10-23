package net.twitchcraft.config;

import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.stream.StreamerProfiles;

import java.util.ArrayList;
import java.util.HashMap;

public final class ConfigDefaults {
    private ConfigDefaults() {}

    public static void apply(TwitchCraftConfig config) {
        StreamerProfiles.migrateLegacy(config);
        if (config.twitch == null) config.twitch = new TwitchCraftConfig.TwitchSettings();
        if (config.security == null) config.security = new TwitchCraftConfig.SecuritySettings();
        if (config.feedback == null) config.feedback = new TwitchCraftConfig.FeedbackSettings();
        if (config.scoreboard == null) config.scoreboard = new TwitchCraftConfig.ScoreboardSettings();
        if (config.donations == null) config.donations = new TwitchCraftConfig.DonationSettings();
        if (config.stream == null) config.stream = new TwitchCraftConfig.StreamSettings();
        if (config.commands == null) config.commands = new HashMap<>();
        if (config.customCommands == null) config.customCommands = new ArrayList<>();
        if (config.cooldownPacks == null) config.cooldownPacks = new TwitchCraftConfig.CooldownPackSettings();
        if (config.death == null) config.death = new TwitchCraftConfig.DeathSettings();
        if (config.hud == null) config.hud = new TwitchCraftConfig.HudSettings();
        HudLegacySync.ensure(config);
        if (config.ui == null) config.ui = new TwitchCraftConfig.UiSettings();
        if (config.donations.tiers == null) config.donations.tiers = new ArrayList<>();
        if (config.security.whitelist == null) config.security.whitelist = new ArrayList<>();
        if (config.security.moderators == null) config.security.moderators = new ArrayList<>();

        for (ChatGameCommand cmd : ChatGameCommand.values()) {
            if (cmd == ChatGameCommand.WITHER || cmd == ChatGameCommand.DRAGON || cmd == ChatGameCommand.CHAT_PHANTOM) {
                if (!config.commands.containsKey(cmd.getId())) {
                    config.getCommandSettings(cmd.getId(), cmd.getDefaultCooldown()).enabled = false;
                }
            }
        }

        if (config.donations.tiers.isEmpty()) {
            addDefaultTiers(config);
        } else {
            ensurePhantomDonationTier(config);
            boolean changed = repairDonationTiers(config);
            changed |= ensureExclusiveTiers(config);
            if (changed) {
                net.twitchcraft.config.ConfigManager.save();
            }
        }
        ensureTierCurrency(config);

        if (!config.balancePresetApplied) {
            if (config.stream != null && config.stream.duoStreamMode) {
                AudiencePresets.apply(config, AudiencePresets.DUO);
            } else {
                AudiencePresets.apply(config, config.audiencePreset != null ? config.audiencePreset : AudiencePresets.MEDIUM);
            }
            config.balancePresetApplied = true;
            config.commandBalanceVersion = BalancePresets.CHAOS_VERSION;
            net.twitchcraft.config.ConfigManager.save();
        } else if (config.commandBalanceVersion < BalancePresets.CHAOS_VERSION) {
            BalancePresets.applyStreamTune(config);
            config.commandBalanceVersion = BalancePresets.CHAOS_VERSION;
            net.twitchcraft.config.ConfigManager.save();
        }
    }

    private static void addDefaultTiers(TwitchCraftConfig config) {
        addTier(config, 50, "lightning", "⚡ Спонсорская гроза", false);
        addTier(config, 75, "goldrush", "✦ Золотая лихорадка", false);
        addTier(config, 100, "tntrain", "💰 Денежный дождь", false);
        addTier(config, 150, "disco", "✦ Диско-хаос", false);
        addTier(config, 200, "army", "⚔ Легион донатера", false);
        addTier(config, 300, "treasure", "✦ Сундук донатера", false);
        addTier(config, 500, "wheel", "🎰 Рулетка", true);
        addTier(config, 750, "raid", "✦ Рейд донатера", false);
        config.donations.tiers.add(phantomTier());
        net.twitchcraft.donation.DonationTierResolver.sort(config.donations.tiers);
    }

    private static boolean ensureExclusiveTiers(TwitchCraftConfig config) {
        boolean changed = false;
        changed |= ensureTier(config, 75, "goldrush", "✦ Золотая лихорадка");
        changed |= ensureTier(config, 150, "disco", "✦ Диско-хаос");
        changed |= ensureTier(config, 300, "treasure", "✦ Сундук донатера");
        changed |= ensureTier(config, 750, "raid", "✦ Рейд донатера");
        if (changed) {
            net.twitchcraft.donation.DonationTierResolver.sort(config.donations.tiers);
        }
        return changed;
    }

    private static boolean ensureTier(TwitchCraftConfig config, int amount, String command, String label) {
        boolean hasCmd = config.donations.tiers.stream()
                .anyMatch(t -> command.equalsIgnoreCase(t.command));
        if (hasCmd) return false;
        boolean amountTaken = config.donations.tiers.stream().anyMatch(t -> t.minAmount == amount);
        if (amountTaken) return false;
        addTier(config, amount, command, label, false);
        return true;
    }

    private static void addTier(TwitchCraftConfig config, int amount, String command, String label, boolean wheel) {
        var tier = new TwitchCraftConfig.DonationTier(amount, command, label);
        tier.currency = "RUB";
        tier.wheel = wheel;
        config.donations.tiers.add(tier);
    }

    private static TwitchCraftConfig.DonationTier phantomTier() {
        var tier = new TwitchCraftConfig.DonationTier(1000, "phantom", "👻 Призрак чата");
        tier.currency = "RUB";
        tier.wheel = false;
        return tier;
    }

    private static void ensureTierCurrency(TwitchCraftConfig config) {
        for (var tier : config.donations.tiers) {
            if (tier.currency == null || tier.currency.isBlank()) {
                tier.currency = "RUB";
            }
        }
    }

    private static boolean repairDonationTiers(TwitchCraftConfig config) {
        boolean changed = false;
        for (var tier : config.donations.tiers) {
            if (tier.minAmount == 500) {
                if (!tier.wheel) {
                    tier.wheel = true;
                    changed = true;
                }
                if (tier.command == null || tier.command.isBlank()) {
                    tier.command = "wheel";
                    changed = true;
                }
            }
            if (tier.minAmount >= 1000) {
                if (tier.wheel) {
                    tier.wheel = false;
                    changed = true;
                }
                if (tier.command == null || tier.command.isBlank() || "wheel".equalsIgnoreCase(tier.command)) {
                    tier.command = "phantom";
                    changed = true;
                }
            }
            if ("phantom".equalsIgnoreCase(tier.command)
                    && (tier.label == null || tier.label.isBlank()
                    || "👻 Chat Phantom".equals(tier.label)
                    || "Chat Phantom".equals(tier.label))) {
                tier.label = "👻 Призрак чата";
                changed = true;
            }
        }
        return changed;
    }

    private static void ensurePhantomDonationTier(TwitchCraftConfig config) {
        boolean has = config.donations.tiers.stream()
                .anyMatch(t -> "phantom".equalsIgnoreCase(t.command));
        if (!has) {
            config.donations.tiers.add(phantomTier());
            net.twitchcraft.donation.DonationTierResolver.sort(config.donations.tiers);
        }
    }
}
