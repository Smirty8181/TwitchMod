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

public record DuoScorePayload(
        String leftTwitch,
        String leftName,
        int leftDeaths,
        String rightTwitch,
        String rightName,
        int rightDeaths,
        int rightHp,
        int rightMaxHp,
        int rightFood,
        boolean active
) implements CustomPacketPayload {
    public static final Type<DuoScorePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "duo_score"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DuoScorePayload> CODEC =
            StreamCodec.of(DuoScorePayload::write, DuoScorePayload::read);

    private static void write(RegistryFriendlyByteBuf buf, DuoScorePayload p) {
        ByteBufCodecs.STRING_UTF8.encode(buf, p.leftTwitch());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.leftName());
        ByteBufCodecs.INT.encode(buf, p.leftDeaths());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.rightTwitch());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.rightName());
        ByteBufCodecs.INT.encode(buf, p.rightDeaths());
        ByteBufCodecs.INT.encode(buf, p.rightHp());
        ByteBufCodecs.INT.encode(buf, p.rightMaxHp());
        ByteBufCodecs.INT.encode(buf, p.rightFood());
        ByteBufCodecs.BOOL.encode(buf, p.active());
    }

    private static DuoScorePayload read(RegistryFriendlyByteBuf buf) {
        return new DuoScorePayload(
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.BOOL.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static DuoScorePayload hidden() {
        return new DuoScorePayload("", "", 0, "", "", 0, 0, 20, 0, false);
    }

    public static void send(ServerPlayer player, DuoScorePayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void hideAll(MinecraftServer server) {
        if (server == null) return;
        DuoScorePayload hidden = hidden();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, hidden);
        }
    }
}
