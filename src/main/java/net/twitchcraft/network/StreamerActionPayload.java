package net.twitchcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.twitchcraft.TwitchCraftMod;

/** Клиент → сервер: кубик / милость / вызов. */
public record StreamerActionPayload(String action) implements CustomPacketPayload {
    public static final Type<StreamerActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "streamer_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StreamerActionPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, StreamerActionPayload::action,
                    StreamerActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
