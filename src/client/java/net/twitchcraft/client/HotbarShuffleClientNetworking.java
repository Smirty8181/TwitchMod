package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.HotbarShufflePayload;

public final class HotbarShuffleClientNetworking {
    private static boolean registered;

    private HotbarShuffleClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(HotbarShufflePayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> HotbarShuffleOverlay.start(payload)));
        registered = true;
    }
}
