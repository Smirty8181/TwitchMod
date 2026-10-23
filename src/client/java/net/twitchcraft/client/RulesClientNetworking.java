package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.OpenRulesPayload;

public final class RulesClientNetworking {
    private RulesClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OpenRulesPayload.TYPE, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new RulesScreen())));
    }
}
