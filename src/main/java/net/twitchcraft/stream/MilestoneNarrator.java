package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.twitchcraft.config.ConfigManager;

/**
 * NPC орёт на вехи прохождения. Первый алмаз, Ад, Край, смерть…
 */
public final class MilestoneNarrator {
    private static boolean iron;
    private static boolean diamond;
    private static boolean debris;
    private static boolean nether;
    private static boolean end;
    private static boolean dragon;
    private static boolean death;

    private MilestoneNarrator() {}

    public static void onBlockBreak(MinecraftServer server, ServerPlayer player, Block block) {
        if (!enabled() || player == null) return;
        if (!iron && (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE)) {
            iron = true;
            shout(server, player, "Железо есть. База.");
        } else if (!diamond && (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE)) {
            diamond = true;
            shout(server, player, "СИКС СЕВЕН АЛМАЗЫ");
        } else if (!debris && block == Blocks.ANCIENT_DEBRIS) {
            debris = true;
            shout(server, player, "Незерит? Рил ток. Копай ещё.");
        }
    }

    public static void onFirstNether(MinecraftServer server, ServerPlayer player) {
        if (!enabled() || nether) return;
        nether = true;
        shout(server, player, "Охаешечки Датабаёшечки");
    }

    public static void onFirstEnd(MinecraftServer server, ServerPlayer player) {
        if (!enabled() || end) return;
        end = true;
        shout(server, player, "Край. Камдаун бро. Дракона зови.");
    }

    public static void onDragon(MinecraftServer server, ServerPlayer player) {
        if (!enabled() || dragon) return;
        dragon = true;
        shout(server, player, "СИКС СЕВЕН ДРАКОН ПАЛ");
    }

    public static void onFirstDeath(MinecraftServer server, ServerPlayer player) {
        onDeath(server, player, null, null);
    }

    public static void onDeath(MinecraftServer server, ServerPlayer player, String killer, String how) {
        if (!enabled() || player == null) return;
        boolean chatter = killer != null && !killer.isBlank();
        if (chatter) {
            death = true;
            String mob = how == null || how.isBlank() ? "" : " " + how;
            shout(server, player, "Я тебя убил." + mob, killer);
            return;
        }
        if (death) return;
        death = true;
        shout(server, player, "Ну такое. Эщкере в респавн.");
    }

    public static void markDeath() {
        death = true;
    }

    public static Snapshot export() {
        return new Snapshot(iron, diamond, debris, nether, end, dragon, death);
    }

    public static void reset() {
        iron = false;
        diamond = false;
        debris = false;
        nether = false;
        end = false;
        dragon = false;
        death = false;
    }

    public static void restore(Snapshot s) {
        if (s == null) return;
        iron = s.iron;
        diamond = s.diamond;
        debris = s.debris;
        nether = s.nether;
        end = s.end;
        dragon = s.dragon;
        death = s.death;
    }

    private static void shout(MinecraftServer server, ServerPlayer player, String line) {
        shout(server, player, line, null);
    }

    private static void shout(MinecraftServer server, ServerPlayer player, String line, String viewer) {
        GuideNpcManager.get().comment(server, player, line, viewer);
    }

    private static boolean enabled() {
        return ConfigManager.get().stream.milestoneNarratorEnabled;
    }

    public record Snapshot(boolean iron, boolean diamond, boolean debris,
                           boolean nether, boolean end, boolean dragon, boolean death) {}
}
