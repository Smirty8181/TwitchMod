package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.DicePayload;

public final class DiceClientNetworking {
    private static boolean registered;

    private DiceClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(DicePayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> DiceOverlay.start(
                        payload.face(), payload.title(), payload.subtitle())));
        registered = true;
    }
}
