package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.UpgradePayload;

public final class UpgradeClientNetworking {
    private static boolean registered;

    private UpgradeClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(UpgradePayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> UpgradeOverlay.apply(payload)));
        registered = true;
    }
}
