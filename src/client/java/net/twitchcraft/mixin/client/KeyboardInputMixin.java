package net.twitchcraft.mixin.client;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.twitchcraft.client.InvertControlsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void twitchcraft$invertWasd(CallbackInfo ci) {
        if (!InvertControlsClient.isActive()) return;
        Input in = this.keyPresses;
        this.keyPresses = new Input(
                in.backward(), in.forward(), in.right(), in.left(),
                in.jump(), in.shift(), in.sprint());
        this.moveVector = new Vec2(-this.moveVector.x, -this.moveVector.y);
    }
}
