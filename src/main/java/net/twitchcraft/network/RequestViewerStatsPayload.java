package net.twitchcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.twitchcraft.TwitchCraftMod;

/** C2S: запрос статистики зрителей (экран P). */
public record RequestViewerStatsPayload() implements CustomPacketPayload {
    public static final Type<RequestViewerStatsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "request_viewer_stats"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestViewerStatsPayload> CODEC =
            StreamCodec.unit(new RequestViewerStatsPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
