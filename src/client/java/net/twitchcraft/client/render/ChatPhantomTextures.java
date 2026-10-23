package net.twitchcraft.client.render;

import net.minecraft.resources.ResourceLocation;
import net.twitchcraft.TwitchCraftMod;

/** Текстуры Chat Phantom — UV как у ванильного фантома (64×64). */
public final class ChatPhantomTextures {
    public static final ResourceLocation BODY = ResourceLocation.fromNamespaceAndPath(
            TwitchCraftMod.MOD_ID, "textures/entity/chat_phantom");
    public static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            TwitchCraftMod.MOD_ID, "textures/entity/chat_phantom_eyes");

    private ChatPhantomTextures() {}
}
