package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;

public record DonationAlertPayload(String donor, String amount, String currency, String label,
                                   int tierColor, int tierMinAmount, String effectId,
                                   String donorMessage, int screenFxLevel)
        implements CustomPacketPayload {
    public static final Type<DonationAlertPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "donation_alert"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DonationAlertPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, DonationAlertPayload::donor,
                    ByteBufCodecs.STRING_UTF8, DonationAlertPayload::amount,
                    ByteBufCodecs.STRING_UTF8, DonationAlertPayload::currency,
                    ByteBufCodecs.STRING_UTF8, DonationAlertPayload::label,
                    ByteBufCodecs.INT, DonationAlertPayload::tierColor,
                    ByteBufCodecs.INT, DonationAlertPayload::tierMinAmount,
                    ByteBufCodecs.STRING_UTF8, DonationAlertPayload::effectId,
                    ByteBufCodecs.STRING_UTF8, DonationAlertPayload::donorMessage,
                    ByteBufCodecs.INT, DonationAlertPayload::screenFxLevel,
                    DonationAlertPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, String donor, String amount, String currency,
                            String label, int tierColor, int tierMinAmount, String effectId,
                            String donorMessage, int screenFxLevel) {
        ServerPlayNetworking.send(player, new DonationAlertPayload(
                donor, amount, currency, label, tierColor, tierMinAmount,
                effectId, donorMessage != null ? donorMessage : "", screenFxLevel));
    }
}
