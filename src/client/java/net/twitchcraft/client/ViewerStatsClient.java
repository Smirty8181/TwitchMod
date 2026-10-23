package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.RequestViewerStatsPayload;
import net.twitchcraft.network.ViewerStatsPayload;

import java.util.ArrayList;
import java.util.List;

public final class ViewerStatsClient {
    private static List<ViewerStatsPayload.ViewerRow> latest = List.of();
    private static long updatedAt;

    private ViewerStatsClient() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ViewerStatsPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> {
                    latest = payload.viewers() != null ? List.copyOf(payload.viewers()) : List.of();
                    updatedAt = System.currentTimeMillis();
                    if (ctx.client().screen instanceof ViewerStatsScreen screen) {
                        screen.onStatsUpdated(latest);
                    }
                }));
    }

    public static void request() {
        if (ClientPlayNetworking.canSend(RequestViewerStatsPayload.TYPE)) {
            ClientPlayNetworking.send(new RequestViewerStatsPayload());
        }
    }

    public static List<ViewerStatsPayload.ViewerRow> latest() {
        return latest;
    }

    public static long updatedAt() {
        return updatedAt;
    }

    public static List<ViewerStatsPayload.ViewerRow> copyLatest() {
        return new ArrayList<>(latest);
    }
}
