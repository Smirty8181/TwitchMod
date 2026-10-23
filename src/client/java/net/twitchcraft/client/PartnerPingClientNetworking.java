package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.PartnerPingPayload;

public final class PartnerPingClientNetworking {
    private static boolean registered;

    private PartnerPingClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(PartnerPingPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> PartnerPingOverlay.apply(
                        payload.fromName(), payload.dim(),
                        payload.x(), payload.y(), payload.z(),
                        payload.seconds())));
        registered = true;
    }
}
