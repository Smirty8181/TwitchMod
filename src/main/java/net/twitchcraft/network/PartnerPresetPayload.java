package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;

/** Сервер → клиент: шаринговый пресет мира без токенов. */
public record PartnerPresetPayload(String json) implements CustomPacketPayload {
    public static final Type<PartnerPresetPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "partner_preset"));

    private static final int MAX_JSON = 262144;

    public static final StreamCodec<RegistryFriendlyByteBuf, PartnerPresetPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(MAX_JSON), PartnerPresetPayload::json,
                    PartnerPresetPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, String json) {
        if (player == null || json == null || json.isBlank()) return;
        String clipped = json.length() > MAX_JSON ? json.substring(0, MAX_JSON) : json;
        ServerPlayNetworking.send(player, new PartnerPresetPayload(clipped));
    }
}
