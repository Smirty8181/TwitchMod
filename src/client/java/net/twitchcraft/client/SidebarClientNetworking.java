package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.SidebarStatePayload;

public final class SidebarClientNetworking {
    private static boolean registered;

    private SidebarClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(SidebarStatePayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> SidebarOverlay.applyState(payload)));
        registered = true;
    }
}
