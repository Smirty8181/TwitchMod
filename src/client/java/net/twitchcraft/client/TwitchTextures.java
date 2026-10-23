package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.twitchcraft.TwitchCraftMod;

/** Текстуры GUI TwitchCraft. */
public final class TwitchTextures {
    public static final ResourceLocation WHEEL_FRAME = ResourceLocation.fromNamespaceAndPath(
            TwitchCraftMod.MOD_ID, "textures/gui/wheel_frame");
    public static final ResourceLocation WHEEL_POINTER = ResourceLocation.fromNamespaceAndPath(
            TwitchCraftMod.MOD_ID, "textures/gui/wheel_pointer");
    public static final ResourceLocation DONATION_BADGE = ResourceLocation.fromNamespaceAndPath(
            TwitchCraftMod.MOD_ID, "textures/gui/donation_badge");

    private TwitchTextures() {}

    public static boolean blit(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h) {
        return blit(g, texture, x, y, w, h, w, h);
    }

    public static boolean blit(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h, int srcW, int srcH) {
        if (!isAvailable(texture) || !TwitchTextureLoader.ensureReady(texture)) {
            return false;
        }
        try {
            g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, w, h, srcW, srcH, srcW, srcH);
            return true;
        } catch (Exception e) {
            TwitchCraftMod.LOGGER.debug("GUI blit failed for {}: {}", texture, e.toString());
            return false;
        }
    }

    static boolean isAvailable(ResourceLocation texture) {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.getResourceManager().getResource(TwitchTextureLoader.packLocation(texture)).isPresent();
    }
}
