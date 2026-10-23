package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.CommandDenyPayload;

public final class CommandDenyClientNetworking {
    private static boolean registered;

    private CommandDenyClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(CommandDenyPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> {
                    TwitchAvatarCache.request(payload.twitchUser());
                    CommandDenyOverlay.push(
                            payload.twitchUser(),
                            payload.commandRu(),
                            payload.reasonId(),
                            payload.seconds(),
                            payload.nickColor());
                }));
        registered = true;
    }
}
