package net.twitchcraft.donation;

import net.twitchcraft.commands.ChatGameCommand;

import java.util.Optional;

/** Эксклюзивные донат-эффекты — не то же самое, что !команды в чате. */
public final class DonationEffectCatalog {
    private DonationEffectCatalog() {}

    public static boolean isExclusive(String commandId) {
        if (commandId == null) return false;
        return switch (commandId.toLowerCase()) {
            case "lightning", "tntrain", "army", "phantom",
                    "goldrush", "disco", "treasure", "raid" -> true;
            default -> false;
        };
    }

    public static String exclusiveLabel(String commandId) {
        if (commandId == null) return "Донат";
        return switch (commandId.toLowerCase()) {
            case "lightning" -> "⚡ Спонсорская гроза";
            case "goldrush" -> "✦ Золотая лихорадка";
            case "tntrain" -> "💰 Денежный дождь";
            case "disco" -> "✦ Диско-хаос";
            case "army" -> "⚔ Легион донатера";
            case "treasure" -> "✦ Сундук донатера";
            case "raid" -> "✦ Рейд донатера";
            case "phantom" -> "👻 Призрак чата";
            case "wheel" -> "🎰 Рулетка";
            default -> ChatGameCommand.fromId(commandId)
                    .map(c -> "✦ " + c.getRussianName())
                    .orElse(commandId);
        };
    }

    public static String exclusiveSubtitle(String commandId) {
        return switch (commandId.toLowerCase()) {
            case "lightning" -> "5 молний + кольцо энергии";
            case "goldrush" -> "золото + изумруды + бафф";
            case "tntrain" -> "12 TNT + золотой дождь";
            case "disco" -> "фейерверки + крутилка + свечение";
            case "army" -> "Бронированный отряд с лидером";
            case "treasure" -> "алмазы, яблоки, шанс тотема";
            case "raid" -> "пиллагеры + равагер + заклинатель";
            case "phantom" -> "Босс 120 HP · ярость <50%";
            default -> "Эксклюзив доната";
        };
    }

    public static int toastDurationMs(int tierMin) {
        if (tierMin >= 1000) return 9000;
        if (tierMin >= 200) return 7500;
        if (tierMin >= 100) return 6500;
        return 5500;
    }

    public static int screenFxLevel(int tierMin) {
        if (tierMin >= 1000) return 3;
        if (tierMin >= 200) return 2;
        if (tierMin >= 100) return 1;
        return 1;
    }

    public static Optional<String> resolveLabel(String commandId, String tierLabel) {
        if (isExclusive(commandId)) {
            return Optional.of(exclusiveLabel(commandId));
        }
        if (tierLabel != null && !tierLabel.isBlank()) {
            return Optional.of(tierLabel);
        }
        return Optional.empty();
    }
}
