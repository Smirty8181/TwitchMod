package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class RulesNetworking {
    private RulesNetworking() {}

    public static void registerServer() {
        NetworkRegistration.registerPlayS2C();
    }

    public static void openFor(ServerPlayer player) {
        ServerPlayNetworking.send(player, new OpenRulesPayload());
    }
}
