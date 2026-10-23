package net.twitchcraft.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Physics Mod 3.1.21 ждёт EMF-поле типа fastutil Map.
 * EMF 3.2.4 сменил тип на java.util.Map → NoSuchFieldError при смерти моба.
 * Пропускаем сломанный EMF-хук: физика трупа остаётся, без кастомной модели EMF.
 */
@Mixin(targets = "net.diebuddies.compat.EMF", remap = false)
public class PhysicsEmfCompatMixin {
    @Shadow(remap = false)
    private static boolean emfModel;

    @Inject(method = "prepare", at = @At("HEAD"), cancellable = true, remap = false)
    private static void twitchcraft$skipBrokenEmfField(CallbackInfo ci) {
        emfModel = false;
        ci.cancel();
    }
}
