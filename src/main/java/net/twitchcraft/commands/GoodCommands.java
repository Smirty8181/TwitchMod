package net.twitchcraft.commands;

import net.twitchcraft.config.TwitchCraftConfig;

import java.util.Arrays;
import java.util.List;

/** Положительные команды чата — лечат, помогают, дают +добро. */
public final class GoodCommands {
    private static final List<ChatGameCommand> ALL = List.of(
            ChatGameCommand.HEAL,
            ChatGameCommand.HELP,
            ChatGameCommand.REGEN,
            ChatGameCommand.FEED,
            ChatGameCommand.SAVE,
            ChatGameCommand.TOTEM,
            ChatGameCommand.ELYTRA,
            ChatGameCommand.DIAMOND,
            ChatGameCommand.XP,
            ChatGameCommand.FIREWORK,
            ChatGameCommand.ANTIDOTE,
            ChatGameCommand.GAPPLE,
            ChatGameCommand.TORCHES,
            ChatGameCommand.BREAD,
            ChatGameCommand.PLANKS,
            ChatGameCommand.POTION,
            ChatGameCommand.BUDDY,
            ChatGameCommand.RESPECT,
            ChatGameCommand.DEFUSE,
            ChatGameCommand.LOCATE_VILLAGE,
            ChatGameCommand.LOCATE_FORTRESS,
            ChatGameCommand.LOCATE_BASTION,
            ChatGameCommand.LOCATE_STRONGHOLD,
            ChatGameCommand.PORTAL_KIT,
            ChatGameCommand.FIRE_RES,
            ChatGameCommand.ENDER_EYE,
            ChatGameCommand.ORE_VEIN
    );

    private GoodCommands() {}

    public static boolean isGood(ChatGameCommand cmd) {
        return cmd != null && ALL.contains(cmd);
    }

    public static ChatGameCommand[] all() {
        return ALL.toArray(ChatGameCommand[]::new);
    }

    public static ChatGameCommand[] enabled(TwitchCraftConfig config) {
        return Arrays.stream(all())
                .filter(cmd -> config.getCommandSettings(cmd.getId(), cmd.getDefaultCooldown()).enabled)
                .toArray(ChatGameCommand[]::new);
    }

    public static String sidebarHint() {
        StringBuilder sb = new StringBuilder();
        for (ChatGameCommand cmd : all()) {
            if (!sb.isEmpty()) sb.append(" §8· ");
            sb.append("§a!").append(cmd.getRussianName());
        }
        return sb.toString();
    }
}
