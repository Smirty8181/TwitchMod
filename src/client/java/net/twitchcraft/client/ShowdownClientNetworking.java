package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.ShowdownPayload;

public final class ShowdownClientNetworking {
    private static boolean registered;

    private ShowdownClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(ShowdownPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> ShowdownOverlay.update(
                        payload.kind(),
                        payload.title(),
                        payload.subtitle(),
                        payload.secondsLeft(),
                        payload.totalSeconds(),
                        payload.leftScore(),
                        payload.rightScore(),
                        payload.leftLabel(),
                        payload.rightLabel(),
                        payload.active())));
        registered = true;
    }
}
