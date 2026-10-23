package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;

/** Сервер → клиент: метка напарника. */
public record PartnerPingPayload(String fromName, String dim, int x, int y, int z, int seconds)
        implements CustomPacketPayload {
    public static final Type<PartnerPingPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "partner_ping"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PartnerPingPayload> CODEC =
            StreamCodec.of(PartnerPingPayload::write, PartnerPingPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, PartnerPingPayload p) {
        ByteBufCodecs.STRING_UTF8.encode(buf, p.fromName());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.dim());
        ByteBufCodecs.INT.encode(buf, p.x());
        ByteBufCodecs.INT.encode(buf, p.y());
        ByteBufCodecs.INT.encode(buf, p.z());
        ByteBufCodecs.VAR_INT.encode(buf, p.seconds());
    }

    private static PartnerPingPayload read(RegistryFriendlyByteBuf buf) {
        return new PartnerPingPayload(
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, String fromName, String dim, int x, int y, int z, int seconds) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new PartnerPingPayload(
                fromName != null ? fromName : "напарник",
                dim != null ? dim : "",
                x, y, z,
                Math.max(1, seconds)));
    }
}
