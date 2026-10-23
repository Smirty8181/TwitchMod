package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.WorldDropPayload;

public final class WorldDropClientNetworking {
    private static boolean registered;

    private WorldDropClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(WorldDropPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> WorldDropOverlay.apply(
                        payload.kind(), payload.viewer(), payload.dim(),
                        payload.x(), payload.y(), payload.z(),
                        payload.secondsLeft(), payload.totalSeconds(),
                        payload.claimed(), payload.active())));
        registered = true;
    }
}
