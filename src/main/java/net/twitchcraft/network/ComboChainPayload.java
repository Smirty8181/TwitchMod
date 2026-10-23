package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;

public record ComboChainPayload(String user1, String user2, String user3) implements CustomPacketPayload {
    public static final Type<ComboChainPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "combo_chain"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ComboChainPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ComboChainPayload::user1,
                    ByteBufCodecs.STRING_UTF8, ComboChainPayload::user2,
                    ByteBufCodecs.STRING_UTF8, ComboChainPayload::user3,
                    ComboChainPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, String user1, String user2, String user3) {
        ServerPlayNetworking.send(player, new ComboChainPayload(user1, user2, user3));
    }
}
