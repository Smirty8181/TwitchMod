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
import net.twitchcraft.stream.StreamTargets;

public record DeathAlertPayload(int totalDeaths, int windowDeaths, int x, int y, int z,
                                String dimension, int evilBlockSec, boolean penaltyTriggered,
                                String victim, String killer, String cause)
        implements CustomPacketPayload {
    public static final Type<DeathAlertPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "death_alert"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeathAlertPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, DeathAlertPayload::totalDeaths,
                    ByteBufCodecs.INT, DeathAlertPayload::windowDeaths,
                    ByteBufCodecs.INT, DeathAlertPayload::x,
                    ByteBufCodecs.INT, DeathAlertPayload::y,
                    ByteBufCodecs.INT, DeathAlertPayload::z,
                    ByteBufCodecs.STRING_UTF8, DeathAlertPayload::dimension,
                    ByteBufCodecs.INT, DeathAlertPayload::evilBlockSec,
                    ByteBufCodecs.BOOL, DeathAlertPayload::penaltyTriggered,
                    ByteBufCodecs.STRING_UTF8, DeathAlertPayload::victim,
                    ByteBufCodecs.STRING_UTF8, DeathAlertPayload::killer,
                    ByteBufCodecs.STRING_UTF8, DeathAlertPayload::cause,
                    DeathAlertPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, int total, int window, int x, int y, int z,
                            String dim, int evilSec, boolean penalty,
                            String victim, String killer, String cause) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new DeathAlertPayload(
                total, window, x, y, z, dim == null ? "" : dim, evilSec, penalty,
                nz(victim), nz(killer), nz(cause)));
    }

    public static void sendToStreamers(MinecraftServer server, int total, int window, int x, int y, int z,
                                       String dim, int evilSec, boolean penalty,
                                       String victim, String killer, String cause) {
        if (server == null) return;
        DeathAlertPayload payload = new DeathAlertPayload(
                total, window, x, y, z, dim == null ? "" : dim, evilSec, penalty,
                nz(victim), nz(killer), nz(cause));
        for (ServerPlayer p : StreamTargets.onlineStreamers(server)) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
