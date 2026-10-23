package net.twitchcraft.commands;

import net.twitchcraft.config.CustomCommandDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * «лохие = только то, что бьёт по HP или спавнит врага.
 * Пранки (крутилка, табличка, тыква) — не зло и не блокируются щитом/штрафом.
 */
public final class EvilCommands {
    public enum Harm {
        DAMAGE, PRANK, GOOD, MOD
    }

    /** Прямой урон, враждебные мобы, TNT, падение, голод/яд. */
    private static final Set<ChatGameCommand> DAMAGE = Set.of(
            ChatGameCommand.LIGHTNING,
            ChatGameCommand.CREEPER,
            ChatGameCommand.ZOMBIE,
            ChatGameCommand.SKELETON,
            ChatGameCommand.SPIDER,
            ChatGameCommand.ENDERMAN,
            ChatGameCommand.WITCH,
            ChatGameCommand.SLIME,
            ChatGameCommand.GHAST,
            ChatGameCommand.ARMY,
            ChatGameCommand.WITHER,
            ChatGameCommand.DRAGON,
            ChatGameCommand.WOLF,
            ChatGameCommand.TNT,
            ChatGameCommand.TNT_RAIN,
            ChatGameCommand.LAVA,
            ChatGameCommand.PIT,
            ChatGameCommand.METEOR,
            ChatGameCommand.ANVIL,
            ChatGameCommand.STORM,
            ChatGameCommand.FIRE,
            ChatGameCommand.CREEPER_STORM,
            ChatGameCommand.HURT,
            ChatGameCommand.POISON,
            ChatGameCommand.HUNGER,
            ChatGameCommand.BOSS,
            ChatGameCommand.CHAT_PHANTOM,
            ChatGameCommand.FPV,
            ChatGameCommand.TCK,
            ChatGameCommand.TRAP,
            ChatGameCommand.BOMB,
            ChatGameCommand.LAUNCH,
            ChatGameCommand.UP,
            ChatGameCommand.GRAVITY,
            ChatGameCommand.CHAOS_CHICKENS,
            ChatGameCommand.AMONGUS,
            ChatGameCommand.VIEWER_ARMY,
            ChatGameCommand.LUCKY,
            ChatGameCommand.UNLUCKY,
            ChatGameCommand.VERY_LUCKY,
            ChatGameCommand.LUCKY_ROULETTE,
            ChatGameCommand.STEAL,
            ChatGameCommand.HEIST,
            ChatGameCommand.CURSE,
            ChatGameCommand.CLEAR,
            ChatGameCommand.IRON_GOLEM,
            ChatGameCommand.GOOSE,
            ChatGameCommand.WATER,
            ChatGameCommand.EVENT
    );

    private EvilCommands() {}

    public static Harm harm(ChatGameCommand cmd) {
        if (cmd == null) return Harm.PRANK;
        if (cmd.getCategory() == ChatGameCommand.Category.MODERATOR) return Harm.MOD;
        if (GoodCommands.isGood(cmd)) return Harm.GOOD;
        if (DAMAGE.contains(cmd)) return Harm.DAMAGE;
        return Harm.PRANK;
    }

    /** Только команды с уроном / врагами. */
    public static boolean isEvil(ChatGameCommand cmd) {
        return harm(cmd) == Harm.DAMAGE;
    }

    public static boolean isPrank(ChatGameCommand cmd) {
        return harm(cmd) == Harm.PRANK;
    }

    public static boolean isEvilCustom(CustomCommandDefinition def) {
        if (def == null) return true;
        return !def.positive;
    }

    public static List<ChatGameCommand> damageAll() {
        return Arrays.stream(ChatGameCommand.values())
                .filter(EvilCommands::isEvil)
                .toList();
    }

    public static List<ChatGameCommand> prankAll() {
        return Arrays.stream(ChatGameCommand.values())
                .filter(EvilCommands::isPrank)
                .toList();
    }

    public static String categoryTag(ChatGameCommand cmd) {
        return switch (harm(cmd)) {
            case GOOD -> "ДОБРО";
            case DAMAGE -> "УРОН";
            case PRANK -> "ПРАНК";
            case MOD -> "МОДЕР";
        };
    }
}
