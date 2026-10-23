package net.twitchcraft.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.client.render.ChatPhantomTextures;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Загрузка PNG в GPU через DynamicTexture — без «missing texture» в кэше. */
public final class TwitchTextureLoader {
    private static final List<ResourceLocation> ALL = new ArrayList<>();
    private static final Set<ResourceLocation> READY = new HashSet<>();
    private static final Map<ResourceLocation, DynamicTexture> DYNAMIC = new HashMap<>();
    private static final List<ResourceLocation> MISSING_LOGGED = new ArrayList<>();

    static {
        ALL.add(TwitchTextures.WHEEL_FRAME);
        ALL.add(TwitchTextures.WHEEL_POINTER);
        ALL.add(TwitchTextures.DONATION_BADGE);
        ALL.add(SmirtyLogo.TEXTURE);
        ALL.add(ChatPhantomTextures.BODY);
        ALL.add(ChatPhantomTextures.EYES);
        for (String id : WheelEffectIcons.allIds()) {
            ALL.add(WheelEffectIcons.texture(id));
        }
    }

    private TwitchTextureLoader() {}

    public static void ensureGuiTextures() {
        ensureAll();
    }

    public static void ensureAll() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        ResourceManager rm = mc.getResourceManager();
        if (rm == null) return;
        TextureManager tm = mc.getTextureManager();
        for (ResourceLocation id : ALL) {
            loadOne(tm, rm, id);
        }
    }

    public static boolean ensureReady(ResourceLocation id) {
        if (READY.contains(id) && isGpuReady(Minecraft.getInstance(), id)) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;
        ResourceManager rm = mc.getResourceManager();
        if (rm == null) return false;
        loadOne(mc.getTextureManager(), rm, id);
        return READY.contains(id) && isGpuReady(mc, id);
    }

    public static boolean isReady(ResourceLocation id) {
        return READY.contains(id);
    }

    public static void reload() {
        READY.clear();
        MISSING_LOGGED.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        TextureManager tm = mc.getTextureManager();
        for (ResourceLocation id : ALL) {
            DynamicTexture dyn = DYNAMIC.remove(id);
            if (dyn != null) {
                NativeImage px = dyn.getPixels();
                if (px != null) px.close();
            }
            try {
                tm.release(id);
            } catch (Exception ignored) {
            }
        }
        ensureAll();
    }

    /** ResourceManager ищет файл как есть — у PNG в пакете есть расширение. */
    public static ResourceLocation packLocation(ResourceLocation id) {
        String path = id.getPath();
        if (path.endsWith(".png")) return id;
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path + ".png");
    }

    private static void logMissingOnce(ResourceLocation id) {
        if (MISSING_LOGGED.contains(id)) return;
        MISSING_LOGGED.add(id);
        TwitchCraftMod.LOGGER.warn("TwitchCraft texture missing in resources: {}", id);
    }

    private static boolean isGpuReady(Minecraft mc, ResourceLocation id) {
        if (mc == null) return false;
        AbstractTexture tex = mc.getTextureManager().getTexture(id);
        if (tex == null) return false;
        GpuTexture gpu = tex.getTexture();
        return gpu != null;
    }

    private static void loadOne(TextureManager tm, ResourceManager rm, ResourceLocation id) {
        if (READY.contains(id) && isGpuReady(Minecraft.getInstance(), id)) return;
        ResourceLocation packId = packLocation(id);
        if (!rm.getResource(packId).isPresent()) {
            logMissingOnce(id);
            return;
        }
        try (InputStream in = rm.open(packId)) {
            NativeImage image = NativeImage.read(in);
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
                TwitchCraftMod.LOGGER.warn("TwitchCraft texture empty: {}", id);
                return;
            }

            DynamicTexture prev = DYNAMIC.remove(id);
            if (prev != null) {
                NativeImage px = prev.getPixels();
                if (px != null) px.close();
            }
            try {
                tm.release(id);
            } catch (Exception ignored) {
            }

            DynamicTexture dyn = new DynamicTexture(() -> "twitchcraft:" + id.getPath(), image);
            dyn.setFilter(false, false);
            dyn.setClamp(true);
            tm.register(id, dyn);
            dyn.upload();
            DYNAMIC.put(id, dyn);
            READY.add(id);
        } catch (Exception e) {
            TwitchCraftMod.LOGGER.warn("Failed to load texture {}", id, e);
        }
    }
}
