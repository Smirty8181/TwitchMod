package net.twitchcraft.donation;



import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;



import java.util.ArrayList;

import java.util.Comparator;

import java.util.List;

import java.util.Locale;

import java.util.Optional;



/** Подбор tier доната по сумме и валюте. */

public final class DonationTierResolver {

    private DonationTierResolver() {}

    private static List<String> cachedSidebar = List.of();
    private static int cachedSidebarRev = Integer.MIN_VALUE;



    /**

     * Ближайший tier снизу: 599₽ → tier 500₽, 1000₽ → tier 1000₽.

     * Берётся максимальный minAmount, который не превышает сумму доната.

     */

    public static Optional<TwitchCraftConfig.DonationTier> resolve(double amount, String currency,

                                                                   List<TwitchCraftConfig.DonationTier> tiers) {

        if (tiers == null || tiers.isEmpty() || amount <= 0) return Optional.empty();

        String cur = normalizeCurrency(currency);

        return tiers.stream()

                .filter(t -> currencyMatches(t, cur))

                .filter(t -> amount >= t.minAmount)

                .max(Comparator.comparingInt(t -> t.minAmount));

    }



    public static String normalizeCurrency(String currency) {

        if (currency == null || currency.isBlank()) return "RUB";

        String c = currency.trim().toUpperCase(Locale.ROOT);

        return switch (c) {

            case "₽", "РУБ", "РУБ.", "RUR", "RUBLE", "RUBLES" -> "RUB";

            case "$", "USD", "US DOLLAR" -> "USD";

            case "€", "EUR", "EURO" -> "EUR";

            default -> c;

        };

    }



    public static boolean currencyMatches(TwitchCraftConfig.DonationTier tier, String donationCurrency) {

        if (tier.currency == null || tier.currency.isBlank()) return true;

        return normalizeCurrency(tier.currency).equals(normalizeCurrency(donationCurrency));

    }



    public static boolean usesWheel(TwitchCraftConfig.DonationTier tier) {
        return tier != null && tier.wheel;
    }



    public static boolean isPhantomTier(TwitchCraftConfig.DonationTier tier) {

        if (tier == null) return false;

        String cmd = tier.command != null ? tier.command.toLowerCase(Locale.ROOT) : "";

        return "phantom".equals(cmd) || tier.minAmount >= 1000;

    }



    public static void sort(List<TwitchCraftConfig.DonationTier> tiers) {

        if (tiers == null) return;

        tiers.sort(Comparator.comparingInt(t -> t.minAmount));

    }



    public static String hint(List<TwitchCraftConfig.DonationTier> tiers) {

        if (tiers == null || tiers.isEmpty()) return "нет tiers — K → Донаты или config/twitchcraft.json";

        List<TwitchCraftConfig.DonationTier> sorted = new ArrayList<>(tiers);

        sort(sorted);

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < sorted.size(); i++) {

            var t = sorted.get(i);

            if (!sb.isEmpty()) sb.append(", ");

            int nextMin = i + 1 < sorted.size() ? sorted.get(i + 1).minAmount : -1;

            sb.append(formatAmountBracket(t.minAmount, nextMin));

            if (t.currency != null && !t.currency.isBlank()) sb.append(t.currency);

            sb.append("→").append(t.label != null ? t.label : t.command);

            if (t.wheel) sb.append("🎰");

        }

        return sb.toString();

    }



    /** Строки донат-tiers для sidebar scoreboard. */

    public static List<String> sidebarLines(TwitchCraftConfig config) {

        int rev = ConfigManager.revision();
        if (cachedSidebarRev == rev) return cachedSidebar;

        List<String> lines = new ArrayList<>();

        if (config == null || !config.scoreboard.showDonations) {
            cachedSidebar = List.of();
            cachedSidebarRev = rev;
            return cachedSidebar;
        }

        List<TwitchCraftConfig.DonationTier> tiers = config.donations.tiers;

        if (tiers == null || tiers.isEmpty()) {
            cachedSidebar = List.of();
            cachedSidebarRev = rev;
            return cachedSidebar;
        }



        lines.add("§6💰 §fДонаты");

        List<TwitchCraftConfig.DonationTier> sorted = new ArrayList<>(tiers);

        sort(sorted);

        int maxTiers = 9;
        for (int i = 0; i < sorted.size() && i < maxTiers; i++) {

            int nextMin = i + 1 < sorted.size() ? sorted.get(i + 1).minAmount : -1;

            lines.add(formatSidebarTierCompact(sorted.get(i), nextMin));

        }

        if (sorted.size() > maxTiers) {
            lines.add("§8… +" + (sorted.size() - maxTiers) + " tier");
        }

        if (config.twitch.eventSubEnabled) {

            lines.add("§7" + config.twitch.bitsMinAmount + " bits §8→ §c!яйца");

        }

        cachedSidebar = List.copyOf(lines);
        cachedSidebarRev = ConfigManager.revision();
        return cachedSidebar;

    }



    public static String formatSidebarTier(TwitchCraftConfig.DonationTier tier) {

        return formatSidebarTier(tier, -1);

    }



    public static String formatSidebarTier(TwitchCraftConfig.DonationTier tier, int nextTierMin) {

        String currency = tier.currency != null && !tier.currency.isBlank() ? tier.currency : "₽";

        String amountLabel = formatAmountBracket(tier.minAmount, nextTierMin) + currency;

        String effectId = tier.command != null ? tier.command : "";

        String effect;

        if (tier.wheel) {

            effect = "🎰 Рулетка";

        } else if (DonationEffectCatalog.isExclusive(effectId)) {

            effect = DonationEffectCatalog.exclusiveLabel(effectId);

        } else {

            effect = resolveEffectName(tier);

        }

        String wheel = tier.wheel ? " §d🎰" : "";

        String badge = DonationEffectCatalog.isExclusive(effectId) ? " §6✦" : "";

        return "§6" + amountLabel + badge + " §8→ §d" + effect + wheel;

    }



    /** Короткая строка tier для sidebar HUD. */

    public static String formatSidebarTierCompact(TwitchCraftConfig.DonationTier tier, int nextTierMin) {

        String currency = tier.currency != null && !tier.currency.isBlank() ? tier.currency : "₽";

        String amountLabel = formatAmountBracket(tier.minAmount, nextTierMin) + currency;

        String effect = sidebarEffectShort(tier);

        return "§7" + amountLabel + " §8→ §f" + effect;

    }



    private static String sidebarEffectShort(TwitchCraftConfig.DonationTier tier) {

        String effectId = tier.command != null ? tier.command : "";

        if (tier.wheel) return "Рулетка";

        if (DonationEffectCatalog.isExclusive(effectId)) {

            String label = DonationEffectCatalog.exclusiveLabel(effectId);

            return truncLabel(label, 14);

        }

        if (tier.command != null && !tier.command.isBlank()) {

            Optional<ChatGameCommand> cmd = ChatGameCommand.fromId(tier.command);

            if (cmd.isPresent()) return "!" + cmd.get().getRussianName();

        }

        if (tier.label != null && !tier.label.isBlank()) return truncLabel(tier.label, 14);

        return tier.command != null ? truncLabel(tier.command, 14) : "?";

    }



    private static String truncLabel(String s, int max) {

        if (s == null || s.isBlank()) return "?";

        return s.length() <= max ? s : s.substring(0, max - 1) + "…";

    }



    /** 500+ означает «от 500 до следующего tier». */

    public static String formatAmountBracket(int minAmount, int nextTierMin) {

        if (nextTierMin > minAmount) {

            return minAmount + "+";

        }

        return String.valueOf(minAmount);

    }



    private static String resolveEffectName(TwitchCraftConfig.DonationTier tier) {

        if (tier.wheel) {

            return "🎰 Рулетка";

        }

        if (tier.command != null && !tier.command.isBlank()) {

            Optional<ChatGameCommand> cmd = ChatGameCommand.fromId(tier.command);

            if (cmd.isPresent()) {

                return "!" + cmd.get().getRussianName();

            }

        }

        if (tier.label != null && !tier.label.isBlank()) {

            return tier.label;

        }

        return tier.command != null ? tier.command : "?";

    }

}


