package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;

/** Клиент должен озвучить текст (!озвучка). */
public record TtsSpeakPayload(String speaker, String text) implements CustomPacketPayload {
    public static final Type<TtsSpeakPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "tts_speak"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TtsSpeakPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, TtsSpeakPayload::speaker,
                    ByteBufCodecs.STRING_UTF8, TtsSpeakPayload::text,
                    TtsSpeakPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, String speaker, String text) {
        ServerPlayNetworking.send(player, new TtsSpeakPayload(
                speaker != null ? speaker : "chat",
                text != null ? text : ""));
    }
}
