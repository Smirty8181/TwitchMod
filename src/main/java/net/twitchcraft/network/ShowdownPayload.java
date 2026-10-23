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

/** Живой оверлей мини-игр чата: охота, ставка, бомба, король. */
public record ShowdownPayload(
        String kind,
        String title,
        String subtitle,
        int secondsLeft,
        int totalSeconds,
        int leftScore,
        int rightScore,
        String leftLabel,
        String rightLabel,
        boolean active
) implements CustomPacketPayload {
    public static final Type<ShowdownPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "showdown"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShowdownPayload> CODEC =
            StreamCodec.of(ShowdownPayload::write, ShowdownPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, ShowdownPayload p) {
        ByteBufCodecs.STRING_UTF8.encode(buf, p.kind());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.title());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.subtitle());
        ByteBufCodecs.INT.encode(buf, p.secondsLeft());
        ByteBufCodecs.INT.encode(buf, p.totalSeconds());
        ByteBufCodecs.INT.encode(buf, p.leftScore());
        ByteBufCodecs.INT.encode(buf, p.rightScore());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.leftLabel());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.rightLabel());
        ByteBufCodecs.BOOL.encode(buf, p.active());
    }

    private static ShowdownPayload read(RegistryFriendlyByteBuf buf) {
        return new ShowdownPayload(
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.BOOL.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static ShowdownPayload hidden() {
        return new ShowdownPayload("", "", "", 0, 1, 0, 0, "", "", false);
    }

    public static void send(ServerPlayer player, ShowdownPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void broadcast(MinecraftServer server, ShowdownPayload payload) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, payload);
        }
    }

    public static void hide(MinecraftServer server) {
        broadcast(server, hidden());
    }
}
