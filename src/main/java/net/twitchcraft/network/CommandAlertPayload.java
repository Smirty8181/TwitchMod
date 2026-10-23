package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.stream.TwitchNickColors;

public record CommandAlertPayload(String twitchUser, String commandRu, String commandId,
                                  int nickColor, int cooldownSec)
        implements CustomPacketPayload {
    public static final Type<CommandAlertPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "command_alert"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CommandAlertPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, CommandAlertPayload::twitchUser,
                    ByteBufCodecs.STRING_UTF8, CommandAlertPayload::commandRu,
                    ByteBufCodecs.STRING_UTF8, CommandAlertPayload::commandId,
                    ByteBufCodecs.INT, CommandAlertPayload::nickColor,
                    ByteBufCodecs.VAR_INT, CommandAlertPayload::cooldownSec,
                    CommandAlertPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, String twitchUser, String commandRu, String commandId) {
        send(player, twitchUser, commandRu, commandId, 0);
    }

    public static void send(ServerPlayer player, String twitchUser, String commandRu, String commandId, int cooldownSec) {
        ServerPlayNetworking.send(player, new CommandAlertPayload(
                twitchUser != null ? twitchUser : "чат",
                commandRu != null ? commandRu : "",
                commandId != null ? commandId : "",
                TwitchNickColors.get(twitchUser),
                Math.max(0, cooldownSec)));
    }

    public static void sendToStreamers(net.minecraft.server.MinecraftServer server,
                                       String twitchUser, String commandRu, String commandId) {
        sendToStreamers(server, twitchUser, commandRu, commandId, 0);
    }

    public static void sendToStreamers(net.minecraft.server.MinecraftServer server,
                                       String twitchUser, String commandRu, String commandId, int cooldownSec) {
        if (server == null) return;
        for (ServerPlayer p : net.twitchcraft.stream.StreamTargets.onlineStreamers(server)) {
            send(p, twitchUser, commandRu, commandId, cooldownSec);
        }
    }
}
