package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.DeathAlertPayload;

public final class DeathClientNetworking {
    private static boolean registered;

    private DeathClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(DeathAlertPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> DeathOverlay.show(
                        payload.totalDeaths(), payload.windowDeaths(),
                        payload.x(), payload.y(), payload.z(),
                        payload.dimension(), payload.evilBlockSec(), payload.penaltyTriggered(),
                        payload.victim(), payload.killer(), payload.cause())));
        registered = true;
    }
}
