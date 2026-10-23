package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;

public record InvertControlsPayload(int ticks) implements CustomPacketPayload {
    public static final Type<InvertControlsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "invert_wasd"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InvertControlsPayload> CODEC =
            StreamCodec.of(InvertControlsPayload::write, InvertControlsPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, InvertControlsPayload p) {
        ByteBufCodecs.VAR_INT.encode(buf, p.ticks());
    }

    private static InvertControlsPayload read(RegistryFriendlyByteBuf buf) {
        return new InvertControlsPayload(ByteBufCodecs.VAR_INT.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer target, int ticks) {
        if (target == null) return;
        ServerPlayNetworking.send(target, new InvertControlsPayload(Math.max(1, ticks)));
    }
}
