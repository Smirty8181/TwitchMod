package net.twitchcraft.stream;



import net.minecraft.server.level.ServerLevel;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.damagesource.DamageSource;

import net.minecraft.world.entity.Entity;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.player.Player;

import net.twitchcraft.TwitchCraftMod;

import net.twitchcraft.feedback.AnnouncementService;



import java.util.Locale;

import java.util.Map;

import java.util.UUID;

import java.util.concurrent.ConcurrentHashMap;



public final class GhostMobRegistry {

    public record GhostInfo(String viewer, String mobRu, String streamer) {}



    private static final Map<UUID, GhostInfo> MOBS = new ConcurrentHashMap<>();



    private GhostMobRegistry() {}



    public static void register(Entity entity, String viewer, String mobRu, String streamer) {

        if (viewer == null || viewer.isBlank()) return;

        String owner = streamer != null ? streamer : "";

        MOBS.put(entity.getUUID(), new GhostInfo(viewer, mobRu, owner));
        ChatMobBudget.track(entity);

    }



    public static void onMobDeath(LivingEntity mob, DamageSource source, ServerLevel level) {

        GhostInfo info = MOBS.remove(mob.getUUID());
        ChatMobBudget.forget(mob.getUUID());
        if (info == null) return;



        Entity killer = source.getEntity();

        if (killer instanceof Player) {
            boolean streamerKill = killer instanceof ServerPlayer sp && StreamTargets.isTarget(sp);
            String msg = streamerKill
                    ? ("Стример убил моба " + info.viewer + "!")
                    : (info.viewer + " уничтожил своего " + info.mobRu + "! +1 добро");
            TwitchCraftMod.LOGGER.info("Ghost mob killed: {}", msg);
            if (!info.streamer.isBlank()) {
                StreamStats.forPlayer(info.streamer).addGood();
            } else {
                StreamStats.global().addGood();
            }
            announce(level, info.streamer, (streamerKill ? "§6" : "§a") + msg);
            TwitchCraftMod.getConnectionManager().sendChatMessage(msg);
            if (streamerKill) {
                StreamerCharge.onGhostKilledByStreamer(level.getServer(), (ServerPlayer) killer, info.viewer);
            }
        }

    }



    private static void announce(ServerLevel level, String streamer, String msg) {

        if (!streamer.isBlank()) {

            ServerPlayer target = level.getServer().getPlayerList().getPlayerByName(streamer);

            if (target != null) {

                AnnouncementService.announceCustom(target, msg);

                return;

            }

        }

        level.players().forEach(p -> AnnouncementService.announceCustom(p, msg));

    }



    public static GhostInfo get(UUID entityId) {

        return MOBS.get(entityId);

    }

    public static void forget(UUID entityId) {
        if (entityId != null) MOBS.remove(entityId);
    }

    public static void cleanup() {

        MOBS.clear();

    }

}


