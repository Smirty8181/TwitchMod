package net.twitchcraft.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.twitchcraft.TwitchCraftMod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * !тряпка: живой игрок замирает, клиент спавнит физический труп из мода Ragdolls.
 * Если мода нет — лежачая поза без бесконечного кручения.
 */
public final class RagdollClient {
    private static final boolean RAGDOLLS = FabricLoader.getInstance().isModLoaded("ragdolls");
    private static final Map<Integer, Session> SESSIONS = new ConcurrentHashMap<>();

    private record Session(long startMs, long untilMs, boolean physics, Object corpse, CameraType savedCam) {}

    private RagdollClient() {}

    public static void start(int entityId, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        Session prev = SESSIONS.remove(entityId);
        if (prev != null) {
            finish(mc, entityId, prev);
        }

        long now = System.currentTimeMillis();
        long until = now + Math.max(1, ticks) * 50L;
        Entity entity = mc.level != null ? mc.level.getEntity(entityId) : null;
        Object corpse = (entity != null && !(entity instanceof net.minecraft.world.entity.player.Player))
                ? spawnPhysicsCorpse(entity) : null;
        boolean physics = corpse != null;

        CameraType saved = null;
        if (physics && mc.player != null && mc.player.getId() == entityId) {
            saved = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        SESSIONS.put(entityId, new Session(now, until, physics, corpse, saved));
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        for (var it = SESSIONS.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            Session session = entry.getValue();
            int id = entry.getKey();
            if (now < session.untilMs) {
                if (session.physics && mc.player != null && mc.player.getId() == id
                        && mc.options.getCameraType().isFirstPerson()) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                }
                continue;
            }
            it.remove();
            finish(mc, id, session);
        }
    }

    public static boolean isActive(int entityId) {
        Session s = SESSIONS.get(entityId);
        return s != null && System.currentTimeMillis() < s.untilMs;
    }

    public static boolean hidesLiving(int entityId) {
        Session s = SESSIONS.get(entityId);
        return s != null && s.physics && System.currentTimeMillis() < s.untilMs;
    }

    /** 0 в начале, 1 в конце. */
    public static float progress(int entityId) {
        Session s = SESSIONS.get(entityId);
        if (s == null) return 1f;
        float span = Math.max(1f, s.untilMs - s.startMs);
        return Mth.clamp((System.currentTimeMillis() - s.startMs) / span, 0f, 1f);
    }

    /** Быстро ложится (~0.35с). */
    public static float lieAmount(int entityId) {
        Session s = SESSIONS.get(entityId);
        if (s == null) return 0f;
        return Mth.clamp((System.currentTimeMillis() - s.startMs) / 350f, 0f, 1f);
    }

    public static float twitch(int entityId) {
        float p = progress(entityId);
        return (1f - p) * (1f - p);
    }

    private static void finish(Minecraft mc, int entityId, Session session) {
        removePhysicsCorpse(session.corpse);
        if (session.savedCam != null && mc.player != null && mc.player.getId() == entityId) {
            mc.options.setCameraType(session.savedCam);
        }
    }

    private static Object spawnPhysicsCorpse(Entity entity) {
        if (!RAGDOLLS) return null;
        try {
            Class<?> ragdollCl = Class.forName("net.caoqt.ragdolls.ragdoll.Ragdoll");
            Constructor<?> ctor = ragdollCl.getConstructor(float.class, float.class, float.class, Entity.class);
            Object ragdoll = ctor.newInstance(
                    (float) entity.getX(),
                    (float) entity.getY(),
                    (float) entity.getZ(),
                    entity);
            Class<?> handler = Class.forName("net.caoqt.ragdolls.client.ClientEventHandler");
            handler.getMethod("addCorpse", ragdollCl).invoke(null, ragdoll);

            Object skeleton = ragdollCl.getField("skeleton").get(ragdoll);
            if (skeleton != null) {
                @SuppressWarnings("unchecked")
                List<Object> nodes = (List<Object>) skeleton.getClass().getField("nodeList").get(skeleton);
                Method impulse = Class.forName("net.caoqt.ragdolls.ragdoll.physics.RagdollLimb")
                        .getMethod("impulse", float.class, float.class, float.class);
                double yaw = Math.toRadians(entity.getYRot());
                double fx = -Math.sin(yaw) * 0.10;
                double fz = Math.cos(yaw) * 0.10;
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (Object node : nodes) {
                    impulse.invoke(node,
                            (float) (fx + r.nextDouble(-0.035, 0.035)),
                            (float) (0.05 + r.nextDouble(0.0, 0.04)),
                            (float) (fz + r.nextDouble(-0.035, 0.035)));
                }
            }
            return ragdoll;
        } catch (Throwable t) {
            TwitchCraftMod.LOGGER.warn("Ragdolls corpse failed, limp fallback: {}", t.toString());
            return null;
        }
    }

    private static void removePhysicsCorpse(Object ragdoll) {
        if (ragdoll == null || !RAGDOLLS) return;
        try {
            Class<?> handler = Class.forName("net.caoqt.ragdolls.client.ClientEventHandler");
            @SuppressWarnings("unchecked")
            List<Object> corpses = (List<Object>) handler.getField("corpses").get(null);
            corpses.remove(ragdoll);
        } catch (Throwable ignored) {
        }
    }
}
