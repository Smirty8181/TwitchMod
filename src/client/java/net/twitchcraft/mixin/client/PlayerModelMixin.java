package net.twitchcraft.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.twitchcraft.client.RagdollClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void twitchcraft$limpFallback(AvatarRenderState state, CallbackInfo ci) {
        if (RagdollClient.hidesLiving(state.id) || !RagdollClient.isActive(state.id)) return;
        HumanoidModel<?> body = (HumanoidModel<?>) (Object) this;
        PlayerModel self = (PlayerModel) (Object) this;
        float lie = RagdollClient.lieAmount(state.id);
        float twitch = RagdollClient.twitch(state.id);
        float t = state.ageInTicks;
        body.head.xRot = 0.55f * lie + (float) Math.sin(t * 0.7) * 0.12f * twitch;
        body.head.yRot = (float) Math.cos(t * 0.35) * 0.08f * twitch;
        body.head.zRot = 0.35f * lie;
        body.hat.xRot = body.head.xRot;
        body.hat.yRot = body.head.yRot;
        body.hat.zRot = body.head.zRot;
        body.body.xRot = 0.12f * lie;
        body.body.zRot = (float) Math.sin(t * 0.2) * 0.04f * twitch;
        body.rightArm.zRot = 1.15f * lie;
        body.leftArm.zRot = -1.15f * lie;
        body.rightArm.xRot = -0.2f * lie + (float) Math.sin(t * 0.9) * 0.08f * twitch;
        body.leftArm.xRot = -0.15f * lie + (float) Math.cos(t * 0.85) * 0.08f * twitch;
        body.rightLeg.xRot = 0.15f * lie;
        body.leftLeg.xRot = -0.08f * lie;
        body.rightLeg.zRot = 0.12f * lie;
        body.leftLeg.zRot = -0.12f * lie;
        copyRot(self.leftSleeve, body.leftArm);
        copyRot(self.rightSleeve, body.rightArm);
        copyRot(self.leftPants, body.leftLeg);
        copyRot(self.rightPants, body.rightLeg);
        copyRot(self.jacket, body.body);
    }

    private static void copyRot(ModelPart to, ModelPart from) {
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
    }
}
