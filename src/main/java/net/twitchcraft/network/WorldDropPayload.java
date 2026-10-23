package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;

/** Сервер → клиент: находка в мире (груз / тотем / гость). */
public record WorldDropPayload(
        String kind,
        String viewer,
        String dim,
        int x,
        int y,
        int z,
        int secondsLeft,
        int totalSeconds,
        boolean claimed,
        boolean active
) implements CustomPacketPayload {
    public static final Type<WorldDropPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "world_drop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorldDropPayload> CODEC =
            StreamCodec.of(WorldDropPayload::write, WorldDropPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, WorldDropPayload p) {
        ByteBufCodecs.STRING_UTF8.encode(buf, p.kind());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.viewer());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.dim());
        ByteBufCodecs.INT.encode(buf, p.x());
        ByteBufCodecs.INT.encode(buf, p.y());
        ByteBufCodecs.INT.encode(buf, p.z());
        ByteBufCodecs.VAR_INT.encode(buf, p.secondsLeft());
        ByteBufCodecs.VAR_INT.encode(buf, p.totalSeconds());
        ByteBufCodecs.BOOL.encode(buf, p.claimed());
        ByteBufCodecs.BOOL.encode(buf, p.active());
    }

    private static WorldDropPayload read(RegistryFriendlyByteBuf buf) {
        return new WorldDropPayload(
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.BOOL.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static WorldDropPayload hidden() {
        return new WorldDropPayload("", "", "", 0, 0, 0, 0, 1, false, false);
    }

    public static void send(ServerPlayer player, WorldDropPayload payload) {
        if (player == null) return;
        ServerPlayNetworking.send(player, payload);
    }

    public static void broadcast(MinecraftServer server, WorldDropPayload payload) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, payload);
        }
    }

    public static void hide(MinecraftServer server) {
        broadcast(server, hidden());
    }
}
