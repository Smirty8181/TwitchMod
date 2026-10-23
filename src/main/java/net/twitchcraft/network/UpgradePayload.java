package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;

/** Меню апгрейда предмета (как upgrader.vip). */
public record UpgradePayload(
        String phase,
        String viewer,
        int secondsLeft,
        int chance,
        String fromId,
        String fromName,
        String toId,
        String toName,
        int selectedSlot,
        boolean canPick,
        int landAngle,
        boolean won
) implements CustomPacketPayload {
    public static final Type<UpgradePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "upgrade"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradePayload> CODEC =
            StreamCodec.of(UpgradePayload::write, UpgradePayload::read);

    private static void write(RegistryFriendlyByteBuf buf, UpgradePayload p) {
        ByteBufCodecs.STRING_UTF8.encode(buf, p.phase());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.viewer());
        ByteBufCodecs.INT.encode(buf, p.secondsLeft());
        ByteBufCodecs.INT.encode(buf, p.chance());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.fromId());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.fromName());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.toId());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.toName());
        ByteBufCodecs.INT.encode(buf, p.selectedSlot());
        ByteBufCodecs.BOOL.encode(buf, p.canPick());
        ByteBufCodecs.INT.encode(buf, p.landAngle());
        ByteBufCodecs.BOOL.encode(buf, p.won());
    }

    private static UpgradePayload read(RegistryFriendlyByteBuf buf) {
        return new UpgradePayload(
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.BOOL.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, UpgradePayload payload) {
        if (player == null || payload == null) return;
        ServerPlayNetworking.send(player, payload);
    }
}
