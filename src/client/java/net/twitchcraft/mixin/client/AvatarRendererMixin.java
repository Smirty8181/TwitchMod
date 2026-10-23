package net.twitchcraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.twitchcraft.client.RagdollClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("HEAD"), cancellable = true)
    private void twitchcraft$hideName(Avatar avatar, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (RagdollClient.hidesLiving(avatar.getId())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void twitchcraft$lieDown(AvatarRenderState state, PoseStack poseStack, float yRot, float anim,
                                     CallbackInfo ci) {
        if (RagdollClient.hidesLiving(state.id) || !RagdollClient.isActive(state.id)) return;
        float lie = RagdollClient.lieAmount(state.id);
        poseStack.mulPose(Axis.XP.rotationDegrees(82f * lie));
        poseStack.translate(0.0, -0.55 * lie, 0.0);
    }
}
