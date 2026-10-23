package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.PartnerPresetPayload;

public final class PartnerPresetClientNetworking {
    private static boolean registered;

    private PartnerPresetClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(PartnerPresetPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> SettingsScreen.applyIncomingPreset(payload.json())));
        registered = true;
    }
}
