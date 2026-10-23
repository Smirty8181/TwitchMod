package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.InvertControlsPayload;

public final class InvertControlsClientNetworking {
    private static boolean registered;

    private InvertControlsClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(InvertControlsPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> InvertControlsClient.start(payload.ticks())));
        registered = true;
    }
}
