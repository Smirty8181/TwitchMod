package net.twitchcraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import net.twitchcraft.stream.SpawnGuard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerExplosion.class)
public class ServerExplosionMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
    private void twitchcraft$keepSpawn(CallbackInfoReturnable<List<BlockPos>> cir) {
        List<BlockPos> raw = cir.getReturnValue();
        if (raw == null || raw.isEmpty()) return;
        List<BlockPos> kept = new ArrayList<>(raw.size());
        for (BlockPos pos : raw) {
            if (!SpawnGuard.blocksExplosion(level, pos)) kept.add(pos);
        }
        if (kept.size() != raw.size()) cir.setReturnValue(kept);
    }
}
