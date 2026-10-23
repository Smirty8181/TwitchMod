package net.twitchcraft.stream;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

/**
 * Главы прохождения: надземье → ад → край.
 * Чат узнаёт этап и получает команды-навигаторы.
 */
public final class CampaignJourney {
    private static boolean seenNether;
    private static boolean seenEnd;
    private static boolean killedDragon;
    private static String chapter = "надземье";

    private CampaignJourney() {}

    public static String sidebarHint() {
        return switch (chapter) {
            case "ад" -> "§cАд";
            case "край" -> "§5Край";
            case "дракон" -> "§dДракон пал";
            default -> "§aНадземье";
        };
    }

    public static boolean hasSeenNether() {
        return seenNether;
    }

    public static boolean hasSeenEnd() {
        return seenEnd;
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer p : StreamTargets.onlineStreamers(server)) {
            var dim = p.level().dimension();
            if (dim == Level.NETHER) {
                if (!seenNether) {
                    seenNether = true;
                    onFirstNether(server, p);
                }
                chapter = "ад";
            } else if (dim == Level.END) {
                if (!seenEnd) {
                    seenEnd = true;
                    onFirstEnd(server, p);
                }
                chapter = killedDragon ? "дракон" : "край";
            } else {
                chapter = killedDragon ? "дракон" : "надземье";
            }
        }
    }

    public static void onBossKill(MinecraftServer server, ServerPlayer killer, LivingEntity killed) {
        if (killer == null || killed == null || !StreamTargets.isTarget(killer)) return;
        if (killed.getType() != EntityType.ENDER_DRAGON || killedDragon) return;
        killedDragon = true;
        chapter = "дракон";
        ServerLevel level = killer.level();
        level.playSound(null, killer.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                killer.getX(), killer.getY() + 1, killer.getZ(), 40, 0.6, 1.0, 0.6, 0.2);
        StreamerCharge.earn(server, killer, "дракон", 1, true);
        MilestoneNarrator.onDragon(server, killer);
        AnnouncementService.announceCustom(killer, "§d§lДРАКОН ПОВЕРЖЕН §7чат прошёл с тобой Майнкрафт");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "ДРАКОН ПАЛ! Чат и стример прошли Майнкрафт.");
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "CHALLENGE", "ДРАКОН ПАЛ", "проходжение", 6, 6, 0, 1,
                "чат", "стример", true));
        ServerTaskQueue.schedule(server, 120, () -> ShowdownDirector.refresh(server));
    }

    public static Snapshot export() {
        return new Snapshot(seenNether, seenEnd, killedDragon, chapter);
    }

    public static void reset() {
        seenNether = false;
        seenEnd = false;
        killedDragon = false;
        chapter = "надземье";
    }

    public static void restore(Snapshot s) {
        if (s == null) return;
        seenNether = s.seenNether;
        seenEnd = s.seenEnd;
        killedDragon = s.killedDragon;
        if (s.chapter != null && !s.chapter.isBlank()) chapter = s.chapter;
    }

    private static void onFirstNether(MinecraftServer server, ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
        AnnouncementService.announceCustom(player,
                "§c§lАД ОТКРЫТ §7чат: §e!крепость !бастион !огнестойкость");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "АД ОТКРЫТ! Ведите стримера: !крепость !бастион !огнестойкость !портал");
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "CHALLENGE", "АД ОТКРЫТ", "глава 2", 8, 8, 0, 0,
                "!крепость", "!огнестойкость", true));
        ServerTaskQueue.schedule(server, 160, () -> ShowdownDirector.refresh(server));
        MilestoneNarrator.onFirstNether(server, player);
    }

    private static void onFirstEnd(MinecraftServer server, ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 300, 0));
        AnnouncementService.announceCustom(player,
                "§5§lКРАЙ §7чат: §e!энд §7если заблудился. !дракон только здесь");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "КРАЙ! Если стример потерялся — пишите !энд. Дракона чат может звать только здесь.");
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "CHALLENGE", "КРАЙ", "глава 3", 8, 8, 0, 0,
                "!энд", "дракон здесь", true));
        ServerTaskQueue.schedule(server, 160, () -> ShowdownDirector.refresh(server));
        MilestoneNarrator.onFirstEnd(server, player);
    }

    public record Snapshot(boolean seenNether, boolean seenEnd, boolean killedDragon, String chapter) {}
}
