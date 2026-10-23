package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.ComboChainPayload;

public final class ComboChainClientNetworking {
    private static boolean registered;

    private ComboChainClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(ComboChainPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> ComboChainOverlay.show(
                        payload.user1(), payload.user2(), payload.user3())));
        registered = true;
    }
}
