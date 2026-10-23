package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.network.DonationAlertPayload;
import net.twitchcraft.network.DonationWheelPayload;

public final class DonationClientNetworking {
    private static boolean registered;

    private DonationClientNetworking() {}

    public static void register() {
        if (registered) return;

        ClientPlayNetworking.registerGlobalReceiver(DonationAlertPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> DonationOverlay.show(
                        payload.donor(), payload.amount(), payload.currency(), payload.label(),
                        payload.tierColor(), payload.tierMinAmount(), payload.effectId(),
                        payload.donorMessage(), payload.screenFxLevel())));

        ClientPlayNetworking.registerGlobalReceiver(DonationWheelPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> DonationWheelOverlay.start(
                        payload.donor(), payload.amount(), payload.currency(), payload.tierColor(),
                        payload.segments(), payload.segmentIds(), payload.segmentColors(),
                        payload.winnerIndex(), payload.winnerLabel())));

        registered = true;
    }
}
