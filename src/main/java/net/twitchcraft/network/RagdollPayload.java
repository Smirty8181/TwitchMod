package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;

/** Сервер → клиент: этот игрок 5 сек — тряпичная кукла, без клона. */
public record RagdollPayload(int entityId, int ticks) implements CustomPacketPayload {
    public static final Type<RagdollPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "ragdoll"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RagdollPayload> CODEC =
            StreamCodec.of(RagdollPayload::write, RagdollPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, RagdollPayload p) {
        ByteBufCodecs.INT.encode(buf, p.entityId());
        ByteBufCodecs.VAR_INT.encode(buf, p.ticks());
    }

    private static RagdollPayload read(RegistryFriendlyByteBuf buf) {
        return new RagdollPayload(ByteBufCodecs.INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendToWorld(ServerPlayer target, int ticks) {
        if (target == null) return;
        RagdollPayload payload = new RagdollPayload(target.getId(), Math.max(1, ticks));
        for (ServerPlayer viewer : target.level().players()) {
            ServerPlayNetworking.send(viewer, payload);
        }
    }
}
