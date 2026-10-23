package net.twitchcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.ArrayList;
import java.util.List;

public record DonationWheelPayload(
        String donor,
        String amount,
        String currency,
        int tierColor,
        List<String> segments,
        List<String> segmentIds,
        List<Integer> segmentColors,
        int winnerIndex,
        String winnerLabel
) implements CustomPacketPayload {
    public static final Type<DonationWheelPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "donation_wheel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DonationWheelPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, DonationWheelPayload::donor,
                    ByteBufCodecs.STRING_UTF8, DonationWheelPayload::amount,
                    ByteBufCodecs.STRING_UTF8, DonationWheelPayload::currency,
                    ByteBufCodecs.INT, DonationWheelPayload::tierColor,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), DonationWheelPayload::segments,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), DonationWheelPayload::segmentIds,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.INT), DonationWheelPayload::segmentColors,
                    ByteBufCodecs.INT, DonationWheelPayload::winnerIndex,
                    ByteBufCodecs.STRING_UTF8, DonationWheelPayload::winnerLabel,
                    DonationWheelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, String donor, String amount, String currency,
                            int tierColor, List<String> segments, List<String> segmentIds,
                            List<Integer> segmentColors, int winnerIndex, String winnerLabel) {
        ServerPlayNetworking.send(player, new DonationWheelPayload(
                donor, amount, currency, tierColor, segments, segmentIds, segmentColors, winnerIndex, winnerLabel));
    }
}
