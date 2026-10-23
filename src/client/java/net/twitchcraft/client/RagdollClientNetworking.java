package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.RagdollPayload;

public final class RagdollClientNetworking {
    private static boolean registered;

    private RagdollClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(RagdollPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> RagdollClient.start(payload.entityId(), payload.ticks())));
        registered = true;
    }
}
