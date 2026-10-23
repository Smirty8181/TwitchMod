package net.twitchcraft.stream;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.twitchcraft.entity.ChatPhantomEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Потолок и TTL мобов чата, чтобы армии не копились до лагов.
 * Боссы живут дольше и не занимают общий слот.
 */
public final class ChatMobBudget {
    private static final int CAP = 8;
    private static final int GLOW_CAP = 6;
    private static final long TTL_MS = 50_000L;
    private static final long BOSS_TTL_MS = 8 * 60_000L;

    private record Tracked(UUID uuid, int entityId, ResourceKey<Level> dim, long spawnedAt, boolean boss) {}

    private static final Map<UUID, Tracked> LIVE = new ConcurrentHashMap<>();

    private ChatMobBudget() {}

    public static void track(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof Player) return;
        if (entity.level() == null) return;
        boolean boss = isBoss(entity);
        LIVE.put(entity.getUUID(), new Tracked(
                entity.getUUID(),
                entity.getId(),
                entity.level().dimension(),
                System.currentTimeMillis(),
                boss));
        MinecraftServer server = entity.level().getServer();
        if (server != null) {
            if (!boss) enforceCap(server);
            updateGlow(server);
        }
    }

    public static void tick(MinecraftServer server) {
        if (server == null || LIVE.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Tracked t : new ArrayList<>(LIVE.values())) {
            Entity e = find(server, t);
            if (e == null || !e.isAlive()) {
                LIVE.remove(t.uuid);
                GhostMobRegistry.forget(t.uuid);
                continue;
            }
            long ttl = t.boss ? BOSS_TTL_MS : TTL_MS;
            if (now - t.spawnedAt >= ttl) {
                despawn(e, t.uuid);
            }
        }
        updateGlow(server);
    }

    public static void forget(UUID uuid) {
        if (uuid != null) LIVE.remove(uuid);
    }

    public static void clear() {
        LIVE.clear();
    }

    /** Убрать мобов чата вокруг стримера (после смерти, чтобы не добили на респавне). */
    public static int despawnNear(ServerPlayer player, double radius) {
        if (player == null || player.level().getServer() == null) return 0;
        MinecraftServer server = player.level().getServer();
        double r2 = radius * radius;
        int n = 0;
        for (Tracked t : new ArrayList<>(LIVE.values())) {
            Entity e = find(server, t);
            if (e == null || e.level() != player.level()) continue;
            if (player.distanceToSqr(e) > r2) continue;
            despawn(e, t.uuid);
            n++;
        }
        return n;
    }

    private static void enforceCap(MinecraftServer server) {
        ArrayList<Tracked> regular = new ArrayList<>();
        for (Tracked t : LIVE.values()) {
            if (!t.boss) regular.add(t);
        }
        if (regular.size() <= CAP) return;
        regular.sort(Comparator.comparingLong(Tracked::spawnedAt));
        int extra = regular.size() - CAP;
        for (int i = 0; i < extra; i++) {
            Tracked t = regular.get(i);
            Entity e = find(server, t);
            if (e != null) despawn(e, t.uuid);
            else {
                LIVE.remove(t.uuid);
                GhostMobRegistry.forget(t.uuid);
            }
        }
    }

    private static void despawn(Entity e, UUID uuid) {
        if (e.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD,
                    e.getX(), e.getY() + 0.6, e.getZ(), 4, 0.2, 0.3, 0.2, 0.01);
        }
        e.discard();
        LIVE.remove(uuid);
        GhostMobRegistry.forget(uuid);
    }

    private static Entity find(MinecraftServer server, Tracked t) {
        ServerLevel level = server.getLevel(t.dim);
        if (level == null) return null;
        Entity e = level.getEntity(t.entityId);
        if (e == null || !e.getUUID().equals(t.uuid)) return null;
        return e;
    }

    private static void updateGlow(MinecraftServer server) {
        if (LIVE.isEmpty()) return;
        ArrayList<GlowCand> named = new ArrayList<>();
        for (Tracked t : LIVE.values()) {
            Entity e = find(server, t);
            if (e == null || !e.isAlive()) continue;
            if (e instanceof ArmorStand) {
                setGlow(e, false);
                continue;
            }
            if (t.boss) {
                setGlow(e, true);
                continue;
            }
            if (e.getCustomName() == null) {
                setGlow(e, false);
                continue;
            }
            double distSq = nearestStreamerDistSq(server, e);
            if (distSq == Double.MAX_VALUE) {
                setGlow(e, false);
                continue;
            }
            named.add(new GlowCand(e, distSq));
        }
        named.sort(Comparator.comparingDouble(GlowCand::distSq));
        for (int i = 0; i < named.size(); i++) {
            setGlow(named.get(i).entity, i < GLOW_CAP);
        }
    }

    private static double nearestStreamerDistSq(MinecraftServer server, Entity entity) {
        double best = Double.MAX_VALUE;
        for (ServerPlayer p : StreamTargets.onlineStreamers(server)) {
            if (p.level() != entity.level()) continue;
            double d = p.distanceToSqr(entity);
            if (d < best) best = d;
        }
        return best;
    }

    private static void setGlow(Entity entity, boolean on) {
        if (entity.hasGlowingTag() == on) return;
        entity.setGlowingTag(on);
    }

    private record GlowCand(Entity entity, double distSq) {}

    private static boolean isBoss(Entity entity) {
        EntityType<?> type = entity.getType();
        return type == EntityType.WITHER
                || type == EntityType.ENDER_DRAGON
                || type == EntityType.WARDEN
                || type == EntityType.ELDER_GUARDIAN
                || type == EntityType.RAVAGER
                || entity instanceof ChatPhantomEntity;
    }
}
