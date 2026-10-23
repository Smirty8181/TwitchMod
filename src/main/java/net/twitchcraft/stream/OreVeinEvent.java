package net.twitchcraft.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.feedback.AnnouncementService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** !шахта — жила угля/железа рядом, исчезает если не успели добыть. */
public final class OreVeinEvent {
    private static final Map<UUID, Vein> ACTIVE = new ConcurrentHashMap<>();
    private static final int LIFE_TICKS = 20 * 22;

    private record Placed(BlockPos pos, BlockState previous, BlockState ore) {}

    private static final class Vein {
        final ServerLevel level;
        final List<Placed> blocks;
        int ticksLeft;
        final String viewer;

        Vein(ServerLevel level, List<Placed> blocks, String viewer) {
            this.level = level;
            this.blocks = blocks;
            this.ticksLeft = LIFE_TICKS;
            this.viewer = viewer;
        }
    }

    private OreVeinEvent() {}

    public static void start(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        Vein old = ACTIVE.remove(p.getUUID());
        if (old != null) restore(old);

        ServerLevel level = p.level();
        BlockPos origin = findGround(level, p.blockPosition().relative(p.getDirection(), 4));
        List<Placed> placed = new ArrayList<>();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 3) continue;
                    if (r.nextFloat() > 0.55f) continue;
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (SpawnGuard.isProtected(level, pos)) continue;
                    BlockState prev = level.getBlockState(pos);
                    if (!canCarve(prev)) continue;
                    BlockState ore = r.nextBoolean()
                            ? Blocks.IRON_ORE.defaultBlockState()
                            : Blocks.COAL_ORE.defaultBlockState();
                    if (!SpawnGuard.replace(level, pos, ore)) continue;
                    placed.add(new Placed(pos.immutable(), prev, ore));
                }
            }
        }
        if (placed.size() < 4) {
            restoreList(level, placed);
            AnnouncementService.announceCustom(p, "§eШахта §7не влезла — мало места");
            ctx.fail();
            return;
        }
        ACTIVE.put(p.getUUID(), new Vein(level, placed, ctx.twitchUser));
        level.playSound(null, origin, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.1f, 0.7f);
        level.sendParticles(ParticleTypes.CRIT, origin.getX() + 0.5, origin.getY() + 1, origin.getZ() + 0.5,
                18, 1.2, 0.6, 1.2, 0.04);
        AnnouncementService.announceCustom(p,
                "§6§lШахта §7жила §e" + placed.size() + " §7руд · 22с · от §f" + ctx.twitchUser);
    }

    public static void tick(MinecraftServer server) {
        ACTIVE.entrySet().removeIf(e -> {
            Vein vein = e.getValue();
            vein.ticksLeft--;
            if (vein.ticksLeft > 0 && server.getPlayerList().getPlayer(e.getKey()) != null) {
                return false;
            }
            restore(vein);
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null) {
                AnnouncementService.announceCustom(p, "§8Шахта осыпалась");
            }
            return true;
        });
    }

    public static String statusLine(ServerPlayer player) {
        Vein v = ACTIVE.get(player.getUUID());
        if (v == null) return "";
        return "§6⛏ ШАХТА §f" + Math.max(1, (v.ticksLeft + 19) / 20) + "с";
    }

    public static void clear() {
        for (Vein v : ACTIVE.values()) restore(v);
        ACTIVE.clear();
    }

    private static void restore(Vein vein) {
        restoreList(vein.level, vein.blocks);
    }

    private static void restoreList(ServerLevel level, List<Placed> blocks) {
        for (Placed placed : blocks) {
            BlockState now = level.getBlockState(placed.pos());
            if (now.equals(placed.ore())) {
                SpawnGuard.replace(level, placed.pos(), placed.previous());
            }
        }
    }

    private static boolean canCarve(BlockState state) {
        if (state.isAir()) return true;
        var b = state.getBlock();
        return b == Blocks.STONE || b == Blocks.DEEPSLATE || b == Blocks.DIRT || b == Blocks.GRASS_BLOCK
                || b == Blocks.GRAVEL || b == Blocks.ANDESITE || b == Blocks.DIORITE || b == Blocks.GRANITE
                || b == Blocks.TUFF || b == Blocks.COBBLESTONE || b == Blocks.COAL_ORE || b == Blocks.IRON_ORE;
    }

    private static BlockPos findGround(ServerLevel level, BlockPos start) {
        BlockPos pos = start;
        for (int i = 0; i < 8 && level.getBlockState(pos).isAir(); i++) {
            pos = pos.below();
        }
        return pos;
    }
}
