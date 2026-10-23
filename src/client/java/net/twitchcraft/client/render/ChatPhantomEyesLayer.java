package net.twitchcraft.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.twitchcraft.client.TwitchTextureLoader;

/** Свечение глаз Chat Phantom. */
public class ChatPhantomEyesLayer extends EyesLayer<PhantomRenderState, PhantomModel> {
    private static final ResourceLocation VANILLA_EYES =
            ResourceLocation.withDefaultNamespace("textures/entity/phantom_eyes.png");

    private ResourceLocation cachedTex;
    private RenderType eyesType;

    public ChatPhantomEyesLayer(RenderLayerParent<PhantomRenderState, PhantomModel> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        TwitchTextureLoader.ensureGuiTextures();
        ResourceLocation tex = TwitchTextureLoader.isReady(ChatPhantomTextures.EYES)
                ? ChatPhantomTextures.EYES : VANILLA_EYES;
        if (eyesType == null || !tex.equals(cachedTex)) {
            cachedTex = tex;
            eyesType = RenderType.eyes(tex);
        }
        return eyesType;
    }
}
