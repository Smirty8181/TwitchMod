package net.twitchcraft.stream;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.twitchcraft.TwitchCraftMod;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * После рейда безымянные мобы получают ники тех, кто только что зашёл в чат.
 * 5 новых ников = 5 подписанных мобов.
 */
public final class RaidNameBinder {
    private static final long WINDOW_MS = 25_000L;
    private static final ConcurrentLinkedDeque<Slot> PENDING = new ConcurrentLinkedDeque<>();
    private static final Set<String> CLAIMED = ConcurrentHashMap.newKeySet();
    private static final Set<String> PRESENT_AT_START = ConcurrentHashMap.newKeySet();
    private static volatile long until;

    private record Slot(UUID uuid, int entityId, ResourceKey<Level> dim) {}

    private RaidNameBinder() {}

    public static boolean isActive() {
        return System.currentTimeMillis() < until;
    }

    public static void begin(Iterable<SlotLike> unnamed, Set<String> alreadyInChat, String raider) {
        PENDING.clear();
        CLAIMED.clear();
        PRESENT_AT_START.clear();
        if (alreadyInChat != null) PRESENT_AT_START.addAll(alreadyInChat);
        if (raider != null && !raider.isBlank()) {
            CLAIMED.add(raider.toLowerCase(Locale.ROOT));
        }
        if (unnamed != null) {
            for (SlotLike s : unnamed) {
                if (s != null) PENDING.add(new Slot(s.uuid(), s.entityId(), s.dim()));
            }
        }
        until = System.currentTimeMillis() + WINDOW_MS;
    }

    public static void onChatter(MinecraftServer server, String user) {
        if (server == null || user == null || user.isBlank()) return;
        if (System.currentTimeMillis() > until) return;
        String key = user.toLowerCase(Locale.ROOT);
        if (PRESENT_AT_START.contains(key) || CLAIMED.contains(key)) return;
        if (isBot(key)) return;
        Slot slot = PENDING.pollFirst();
        if (slot == null) return;
        CLAIMED.add(key);
        String display = user.trim();
        if (display.length() > 16) display = display.substring(0, 16);
        Entity entity = find(server, slot);
        if (entity == null || !entity.isAlive()) return;
        entity.setCustomName(Component.literal("§c[" + display + "]"));
        entity.setCustomNameVisible(true);
        String streamer = "";
        if (entity.level() instanceof ServerLevel level) {
            var near = StreamTargets.onlineStreamers(level.getServer());
            if (!near.isEmpty()) streamer = near.getFirst().getGameProfile().name();
        }
        GhostMobRegistry.register(entity, display, "рейд", streamer);
        TwitchCraftMod.LOGGER.info("TwitchCraft raid nick {} → mob", display);
    }

    public interface SlotLike {
        UUID uuid();
        int entityId();
        ResourceKey<Level> dim();
    }

    public static SlotLike slot(Entity entity) {
        if (entity == null || entity.level() == null) return null;
        return new SlotLike() {
            @Override
            public UUID uuid() {
                return entity.getUUID();
            }

            @Override
            public int entityId() {
                return entity.getId();
            }

            @Override
            public ResourceKey<Level> dim() {
                return entity.level().dimension();
            }
        };
    }

    private static Entity find(MinecraftServer server, Slot slot) {
        ServerLevel level = server.getLevel(slot.dim());
        if (level == null) return null;
        Entity e = level.getEntity(slot.entityId());
        if (e == null || !e.getUUID().equals(slot.uuid())) return null;
        return e;
    }

    private static boolean isBot(String key) {
        return key.equals("nightbot") || key.equals("streamelements") || key.equals("moobot")
                || key.equals("fossabot") || key.equals("wizebot");
    }
}
