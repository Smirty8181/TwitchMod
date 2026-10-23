package net.twitchcraft.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.twitchcraft.network.PartnerPingPayload;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Средняя кнопка мыши: метка напарнику на 20 секунд. */
public final class PartnerPing {
    public static final int DURATION_SEC = 20;
    private static final long COOLDOWN_MS = 1_500L;
    private static final double REACH = 80.0;

    private record Beam(ResourceKey<Level> dim, BlockPos pos, long until) {}

    private static final Map<UUID, Long> NEXT_AT = new ConcurrentHashMap<>();
    private static final Map<UUID, Beam> BEAMS = new ConcurrentHashMap<>();

    private PartnerPing() {}

    public static void ping(ServerPlayer from) {
        if (from == null) return;
        MinecraftServer server = from.level().getServer();
        if (server == null) return;

        long now = System.currentTimeMillis();
        Long next = NEXT_AT.get(from.getUUID());
        if (next != null && now < next) return;
        NEXT_AT.put(from.getUUID(), now + COOLDOWN_MS);

        ServerPlayer partner = StreamTargets.partner(server, from).orElse(null);
        if (partner == null) {
            from.displayClientMessage(Component.literal("§7Напарник не в сети"), true);
            return;
        }

        BlockPos pos = aimPos(from);
        String dim = from.level().dimension().location().getPath();
        String fromName = from.getGameProfile().name();

        PartnerPingPayload.send(partner, fromName, dim, pos.getX(), pos.getY(), pos.getZ(), DURATION_SEC);
        partner.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.9f, 1.35f);
        partner.displayClientMessage(Component.literal(
                "§b" + fromName + " §7пингует §f" + pos.getX() + " " + pos.getY() + " " + pos.getZ()), true);

        from.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.45f, 1.3f);
        from.displayClientMessage(Component.literal("§bПинг → §f" + partner.getGameProfile().name()), true);

        BEAMS.put(partner.getUUID(), new Beam(from.level().dimension(), pos, now + DURATION_SEC * 1000L));
        if (from.level() instanceof ServerLevel level) burst(level, pos);
    }

    public static void tick(MinecraftServer server) {
        if (BEAMS.isEmpty()) return;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Beam>> it = BEAMS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Beam> e = it.next();
            Beam beam = e.getValue();
            if (now >= beam.until) {
                it.remove();
                continue;
            }
            ServerLevel level = server.getLevel(beam.dim);
            if (level == null) continue;
            double x = beam.pos.getX() + 0.5;
            double y = beam.pos.getY() + 0.2;
            double z = beam.pos.getZ() + 0.5;
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 5, 0.08, 1.1, 0.08, 0.01);
        }
    }

    public static void clear() {
        NEXT_AT.clear();
        BEAMS.clear();
    }

    private static BlockPos aimPos(ServerPlayer from) {
        Vec3 eye = from.getEyePosition(1f);
        Vec3 look = from.getViewVector(1f);
        Vec3 end = eye.add(look.scale(REACH));
        HitResult hit = from.level().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from));
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult block) {
            return block.getBlockPos();
        }
        HitResult entityHit = from.pick(REACH, 1f, false);
        if (entityHit instanceof EntityHitResult entity && entity.getEntity() != null) {
            return entity.getEntity().blockPosition();
        }
        return BlockPos.containing(from.getX(), from.getY(), from.getZ());
    }

    private static void burst(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.4;
        double z = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 12, 0.15, 0.8, 0.15, 0.04);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 8, 0.2, 0.2, 0.2, 0.02);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, 1.4f);
    }
}
