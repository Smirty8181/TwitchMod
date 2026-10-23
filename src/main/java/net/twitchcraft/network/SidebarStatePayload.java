package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;

/** Динамика sidebar (статы, последняя команда) — команды рисует клиент из config. */
public record SidebarStatePayload(
        String lastUser,
        String lastCommandRu,
        int goodPoints,
        int evilPoints,
        int chaos,
        int chaosMax,
        int elapsedSeconds,
        boolean houseMode,
        boolean penaltyActive,
        int penaltyMult,
        int penaltySec,
        String statusLine,
        boolean twitchConnected,
        String twitchChannel,
        String twitchError,
        int totalDeaths,
        int deathsInWindow,
        int evilBlockSec,
        boolean evilBlockActive,
        String lastDeathCoords,
        String topGoodViewer,
        int topGoodCount,
        String topEvilViewer,
        int topEvilCount,
        String topActiveViewer,
        int topActiveCount,
        String streakUser,
        int streakCount,
        boolean streakGood,
        int karmaScore,
        int lastUserNickColor
) implements CustomPacketPayload {
    public static final Type<SidebarStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "sidebar_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SidebarStatePayload> CODEC =
            StreamCodec.of(SidebarStatePayload::write, SidebarStatePayload::read);

    private static void write(RegistryFriendlyByteBuf buf, SidebarStatePayload p) {
        ByteBufCodecs.STRING_UTF8.encode(buf, p.lastUser());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.lastCommandRu());
        ByteBufCodecs.INT.encode(buf, p.goodPoints());
        ByteBufCodecs.INT.encode(buf, p.evilPoints());
        ByteBufCodecs.INT.encode(buf, p.chaos());
        ByteBufCodecs.INT.encode(buf, p.chaosMax());
        ByteBufCodecs.INT.encode(buf, p.elapsedSeconds());
        ByteBufCodecs.BOOL.encode(buf, p.houseMode());
        ByteBufCodecs.BOOL.encode(buf, p.penaltyActive());
        ByteBufCodecs.INT.encode(buf, p.penaltyMult());
        ByteBufCodecs.INT.encode(buf, p.penaltySec());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.statusLine());
        ByteBufCodecs.BOOL.encode(buf, p.twitchConnected());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.twitchChannel());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.twitchError());
        ByteBufCodecs.INT.encode(buf, p.totalDeaths());
        ByteBufCodecs.INT.encode(buf, p.deathsInWindow());
        ByteBufCodecs.INT.encode(buf, p.evilBlockSec());
        ByteBufCodecs.BOOL.encode(buf, p.evilBlockActive());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.lastDeathCoords());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.topGoodViewer());
        ByteBufCodecs.INT.encode(buf, p.topGoodCount());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.topEvilViewer());
        ByteBufCodecs.INT.encode(buf, p.topEvilCount());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.topActiveViewer());
        ByteBufCodecs.INT.encode(buf, p.topActiveCount());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.streakUser());
        ByteBufCodecs.INT.encode(buf, p.streakCount());
        ByteBufCodecs.BOOL.encode(buf, p.streakGood());
        ByteBufCodecs.INT.encode(buf, p.karmaScore());
        ByteBufCodecs.INT.encode(buf, p.lastUserNickColor());
    }

    private static SidebarStatePayload read(RegistryFriendlyByteBuf buf) {
        return new SidebarStatePayload(
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, SidebarStatePayload state) {
        ServerPlayNetworking.send(player, state);
    }
}
