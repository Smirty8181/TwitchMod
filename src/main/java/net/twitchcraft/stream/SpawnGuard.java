package net.twitchcraft.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelData;

/** Спавн мира и кровать со спавнпоинтом нельзя снести. */
public final class SpawnGuard {
    private static final int SPAWN_RADIUS = 8;
    private static final int SPAWN_Y = 16;

    private SpawnGuard() {}

    public static boolean isProtected(Level level, BlockPos pos) {
        return spawnBedAt(level, pos) || inWorldSpawn(level, pos);
    }

    public static boolean blocksExplosion(Level level, BlockPos pos) {
        return isProtected(level, pos);
    }

    public static boolean replace(ServerLevel level, BlockPos pos, BlockState state) {
        if (isProtected(level, pos)) return false;
        return level.setBlockAndUpdate(pos, state);
    }

    public static boolean allowBreak(Level world, Player player, BlockPos pos) {
        if (!isProtected(world, pos)) return true;
        if (player != null) {
            String msg = spawnBedAt(world, pos)
                    ? "§cКровать спавна нельзя сломать"
                    : "§cСпавн защищён";
            player.displayClientMessage(Component.literal(msg), true);
        }
        return false;
    }

    private static boolean inWorldSpawn(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        LevelData.RespawnData spawn = serverLevel.getRespawnData();
        if (spawn == null || spawn.dimension() != serverLevel.dimension()) return false;
        BlockPos origin = spawn.pos();
        int dx = pos.getX() - origin.getX();
        int dz = pos.getZ() - origin.getZ();
        int dy = pos.getY() - origin.getY();
        return dx * dx + dz * dz <= SPAWN_RADIUS * SPAWN_RADIUS && Math.abs(dy) <= SPAWN_Y;
    }

    private static boolean spawnBedAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        MinecraftServer server = serverLevel.getServer();
        if (server == null) return false;
        for (ServerPlayer player : StreamTargets.onlineStreamers(server)) {
            if (isPlayerSpawnBlock(serverLevel, player, pos)) return true;
        }
        return false;
    }

    private static boolean isPlayerSpawnBlock(ServerLevel level, ServerPlayer player, BlockPos pos) {
        ServerPlayer.RespawnConfig cfg = player.getRespawnConfig();
        if (cfg == null) return false;
        LevelData.RespawnData data = cfg.respawnData();
        if (data == null || data.dimension() != level.dimension()) return false;
        BlockPos spawn = data.pos();
        if (pos.equals(spawn)) return true;
        return bedPair(level, spawn).equals(pos) || bedPair(level, pos).equals(spawn);
    }

    private static BlockPos bedPair(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock)) return pos;
        BedPart part = state.getValue(BedBlock.PART);
        Direction facing = state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        return pos.relative(part == BedPart.FOOT ? facing : facing.getOpposite());
    }
}
