package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.DuoScorePayload;

public final class DuoScoreClientNetworking {
    private static boolean registered;

    private DuoScoreClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(DuoScorePayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> DuoScoreOverlay.apply(
                        payload.leftTwitch(), payload.leftName(), payload.leftDeaths(),
                        payload.rightTwitch(), payload.rightName(), payload.rightDeaths(),
                        payload.rightHp(), payload.rightMaxHp(), payload.rightFood(),
                        payload.active())));
        registered = true;
    }
}
