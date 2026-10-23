package net.twitchcraft.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.layers.PhantomEyesLayer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.resources.ResourceLocation;
import net.twitchcraft.client.TwitchTextureLoader;

/** Chat Phantom — фиолетовый скин + свечение глаз. */
public class ChatPhantomRenderer extends PhantomRenderer {
    private static final ResourceLocation VANILLA_BODY =
            ResourceLocation.withDefaultNamespace("textures/entity/phantom.png");

    public ChatPhantomRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.layers.removeIf(layer -> layer instanceof PhantomEyesLayer);
        this.addLayer(new ChatPhantomEyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(PhantomRenderState state) {
        TwitchTextureLoader.ensureGuiTextures();
        return TwitchTextureLoader.isReady(ChatPhantomTextures.BODY)
                ? ChatPhantomTextures.BODY : VANILLA_BODY;
    }
}
