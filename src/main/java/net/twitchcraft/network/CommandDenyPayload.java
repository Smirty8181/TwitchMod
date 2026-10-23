package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;

/** Почему команда чата не сработала — карточка слева снизу. */
public record CommandDenyPayload(String twitchUser, String commandRu, String reasonId, int seconds, int nickColor)
        implements CustomPacketPayload {
    public static final Type<CommandDenyPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "command_deny"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CommandDenyPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, CommandDenyPayload::twitchUser,
                    ByteBufCodecs.STRING_UTF8, CommandDenyPayload::commandRu,
                    ByteBufCodecs.STRING_UTF8, CommandDenyPayload::reasonId,
                    ByteBufCodecs.VAR_INT, CommandDenyPayload::seconds,
                    ByteBufCodecs.INT, CommandDenyPayload::nickColor,
                    CommandDenyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, String twitchUser, String commandRu, String reasonId, int seconds) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new CommandDenyPayload(
                twitchUser != null ? twitchUser : "чат",
                commandRu != null ? commandRu : "",
                reasonId != null ? reasonId : "other",
                Math.max(0, seconds),
                net.twitchcraft.stream.TwitchNickColors.get(twitchUser)));
    }
}
