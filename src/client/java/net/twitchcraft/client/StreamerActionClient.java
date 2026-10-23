package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.twitchcraft.network.StreamerActionPayload;

public final class StreamerActionClient {
    private StreamerActionClient() {}

    public static void send(String action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null) return;
        ClientPlayNetworking.send(new StreamerActionPayload(action));
    }
}
