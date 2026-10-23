package net.twitchcraft.commands;

import net.twitchcraft.config.CustomCommandDefinition;
import net.twitchcraft.config.TwitchCraftConfig;

/** Общие кулдауны по «пакам» категорий (хаос, добро, мобы…). */
public final class CategoryCooldownResolver {
    private CategoryCooldownResolver() {}

    public static String scopedKey(String playerName, String key) {
        String scope = playerName == null || playerName.isBlank()
                ? "global" : playerName.toLowerCase(java.util.Locale.ROOT);
        return scope + ":" + key;
    }

    public static String cooldownKey(ChatGameCommand cmd, TwitchCraftConfig config) {
        if (config.cooldownPacks == null || !config.cooldownPacks.sharedPackCooldowns) {
            return cmd.getId();
        }
        return "pack:" + packId(cmd);
    }

    public static String cooldownKey(CustomCommandDefinition def, TwitchCraftConfig config) {
        if (config.cooldownPacks == null || !config.cooldownPacks.sharedPackCooldowns) {
            return def.cooldownKey();
        }
        if (def.positive) return "pack:good";
        String type = def.actionType == null ? "effect" : def.actionType.toLowerCase();
        return switch (type) {
            case "item" -> "pack:inventory";
            case "mob" -> "pack:mob";
            default -> "pack:effect";
        };
    }

    public static int cooldownSeconds(ChatGameCommand cmd, TwitchCraftConfig config, int perCommandSeconds) {
        if (config.cooldownPacks == null || !config.cooldownPacks.sharedPackCooldowns) {
            return perCommandSeconds;
        }
        return packSeconds(config, packId(cmd));
    }

    public static int cooldownSeconds(CustomCommandDefinition def, TwitchCraftConfig config) {
        if (config.cooldownPacks == null || !config.cooldownPacks.sharedPackCooldowns) {
            return def.cooldownSeconds;
        }
        if (def.positive) return config.cooldownPacks.goodPackSeconds;
        String type = def.actionType == null ? "effect" : def.actionType.toLowerCase();
        return switch (type) {
            case "item" -> config.cooldownPacks.inventoryPackSeconds;
            case "mob" -> config.cooldownPacks.mobPackSeconds;
            default -> config.cooldownPacks.effectPackSeconds;
        };
    }

    public static String packLabel(ChatGameCommand cmd) {
        if (GoodCommands.isGood(cmd)) return "Добро";
        return switch (cmd.getCategory()) {
            case MOB -> "Мобы";
            case TNT -> "TNT";
            case CHAOS -> "Хаос";
            case EFFECT -> "Эффекты";
            case MOVEMENT -> "Движение";
            case INVENTORY -> "Инвентарь";
            case WORLD -> "Мир";
            case FUN -> "Шоу";
            default -> "Команда";
        };
    }

    private static String packId(ChatGameCommand cmd) {
        if (GoodCommands.isGood(cmd)) return "good";
        return switch (cmd.getCategory()) {
            case MOB -> "mob";
            case TNT -> "tnt";
            case CHAOS -> "chaos";
            case EFFECT -> "effect";
            case MOVEMENT -> "movement";
            case INVENTORY -> "inventory";
            case WORLD -> "world";
            case FUN -> "fun";
            default -> cmd.getId();
        };
    }

    private static int packSeconds(TwitchCraftConfig config, String pack) {
        var p = config.cooldownPacks;
        return switch (pack) {
            case "good" -> p.goodPackSeconds;
            case "mob" -> p.mobPackSeconds;
            case "tnt" -> p.tntPackSeconds;
            case "chaos" -> p.chaosPackSeconds;
            case "effect" -> p.effectPackSeconds;
            case "movement" -> p.movementPackSeconds;
            case "inventory" -> p.inventoryPackSeconds;
            case "world" -> p.worldPackSeconds;
            case "fun" -> p.funPackSeconds;
            default -> 30;
        };
    }
}
