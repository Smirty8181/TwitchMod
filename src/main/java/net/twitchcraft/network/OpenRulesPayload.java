package net.twitchcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.twitchcraft.TwitchCraftMod;

public record OpenRulesPayload() implements CustomPacketPayload {
    public static final Type<OpenRulesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "open_rules"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenRulesPayload> CODEC =
            StreamCodec.unit(new OpenRulesPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
