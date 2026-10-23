package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.AABB;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;

/**
 * После смерти — не добивать на респавне.
 * Чат всё ещё может шутить, но урон и мобы ждут, пока стример встанет.
 */
public final class DeathGrace {
    private DeathGrace() {}

    public static void onDeath(MinecraftServer server, ServerPlayer player) {
        if (player == null || server == null) return;
        if (HouseMode.isActive()) return;
        TwitchCraftConfig.DeathSettings cfg = ConfigManager.get().death;
        if (cfg == null || !cfg.enabled) return;

        int grace = Math.max(8, cfg.respawnGraceSeconds);
        StreamerShield.activate(server, player, grace, "после смерти");
        cleanupArena(player);
        BombManager bomb = BombManager.get();
        if (bomb.isActive()) {
            bomb.defuse(server, "смерть");
        }
    }

    public static void onRespawn(ServerPlayer player) {
        if (player == null || !StreamTargets.isTarget(player)) return;
        if (HouseMode.isActive()) return;
        TwitchCraftConfig.DeathSettings cfg = ConfigManager.get().death;
        if (cfg == null || !cfg.enabled) return;

        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 8, 2, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 6, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 8, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 10, 0, true, false, true));
        cleanupArena(player);
    }

    private static void cleanupArena(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        ChatMobBudget.despawnNear(player, 18.0);
        AABB box = player.getBoundingBox().inflate(14.0);
        for (PrimedTnt tnt : level.getEntitiesOfClass(PrimedTnt.class, box)) {
            tnt.discard();
        }
    }
}
