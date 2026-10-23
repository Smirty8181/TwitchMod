package net.twitchcraft.commands;

import net.twitchcraft.config.CustomCommandDefinition;
import net.twitchcraft.config.TwitchCraftConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CustomCommandRegistry {
    private CustomCommandRegistry() {}

    public static Optional<CustomCommandDefinition> fromMessage(String message, TwitchCraftConfig config) {
        if (message == null || !message.startsWith("!") || config.customCommands == null) {
            return Optional.empty();
        }
        String body = message.substring(1).trim();
        int space = body.indexOf(' ');
        String name = (space >= 0 ? body.substring(0, space) : body).toLowerCase(Locale.ROOT);
        for (CustomCommandDefinition def : config.customCommands) {
            if (def == null || !def.enabled) continue;
            if (def.trigger != null && def.trigger.equalsIgnoreCase(name)) {
                return Optional.of(def);
            }
        }
        return Optional.empty();
    }

    public static Optional<CustomCommandDefinition> fromId(String id, TwitchCraftConfig config) {
        if (id == null || id.isBlank() || config.customCommands == null) return Optional.empty();
        String key = id.toLowerCase(Locale.ROOT);
        for (CustomCommandDefinition def : config.customCommands) {
            if (def != null && def.id != null && def.id.equalsIgnoreCase(key)) {
                return Optional.of(def);
            }
        }
        return Optional.empty();
    }

    public static Map<String, CustomCommandDefinition> aliasMap(TwitchCraftConfig config) {
        Map<String, CustomCommandDefinition> map = new HashMap<>();
        if (config.customCommands == null) return map;
        for (CustomCommandDefinition def : config.customCommands) {
            if (def == null || def.trigger == null || def.trigger.isBlank()) continue;
            map.put(def.trigger.toLowerCase(Locale.ROOT), def);
        }
        return map;
    }

    public static boolean conflictsWithBuiltIn(String trigger) {
        if (trigger == null || trigger.isBlank()) return true;
        return ChatGameCommand.fromMessage("!" + trigger).isPresent();
    }

    public static boolean conflictsWithCustom(String trigger, String exceptId, List<CustomCommandDefinition> list) {
        if (trigger == null || trigger.isBlank() || list == null) return false;
        String key = trigger.toLowerCase(Locale.ROOT);
        for (CustomCommandDefinition def : list) {
            if (def == null || def.trigger == null) continue;
            if (exceptId != null && exceptId.equals(def.id)) continue;
            if (def.trigger.equalsIgnoreCase(key)) return true;
        }
        return false;
    }
}
