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

public record DicePayload(int face, String title, String subtitle, int extraSec)
        implements CustomPacketPayload {
    public static final Type<DicePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "dice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DicePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, DicePayload::face,
                    ByteBufCodecs.STRING_UTF8, DicePayload::title,
                    ByteBufCodecs.STRING_UTF8, DicePayload::subtitle,
                    ByteBufCodecs.INT, DicePayload::extraSec,
                    DicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void broadcast(MinecraftServer server, int face, String title, String subtitle, int extraSec) {
        if (server == null) return;
        DicePayload payload = new DicePayload(face, title, subtitle, extraSec);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
