package net.twitchcraft.mixin.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Ragdolls 1.2.2 собран под 1.21.11: {@code RenderLayers} ({@code class_12249}) в 1.21.10 нет.
 * display() падает NoClassDefFoundError — подменяем на ванильный entityCutout.
 */
@Mixin(targets = "net.caoqt.ragdolls.ragdoll.Ragdoll", remap = false)
public class RagdollDisplayMixin {
    @Redirect(
            method = "display",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/class_12249;method_75990(Lnet/minecraft/class_2960;)Lnet/minecraft/class_1921;",
                    remap = false
            ),
            remap = false
    )
    private static RenderType twitchcraft$entityCutout(ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }
}
