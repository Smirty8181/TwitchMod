package net.twitchcraft.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.twitchcraft.client.HotbarShuffleOverlay;
import net.twitchcraft.client.SidebarOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class InGameHudMixin {
    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    private void twitchcraft$hideHotbarDuringFly(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (HotbarShuffleOverlay.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void twitchcraft$renderSidebar(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        SidebarOverlay.render(graphics);
        HotbarShuffleOverlay.render(graphics);
    }
}
