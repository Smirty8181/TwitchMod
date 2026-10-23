package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.stream.StreamStats;

public final class ViewerStatsNetworking {
    private ViewerStatsNetworking() {}

    public static void registerServer() {
        NetworkRegistration.registerPlayC2S();
        ServerPlayNetworking.registerGlobalReceiver(RequestViewerStatsPayload.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            var server = player.level().getServer();
            if (server == null) return;
            server.execute(() -> {
                StreamStats stats = StreamStats.forPlayer(player.getGameProfile().name());
                ViewerStatsPayload.send(player, stats);
            });
        });
    }
}
