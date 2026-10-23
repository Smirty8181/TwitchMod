package net.twitchcraft.client;



import com.google.gson.Gson;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.twitchcraft.config.StreamerProfile;

import net.twitchcraft.config.TwitchCraftConfig;

import net.twitchcraft.network.SaveConfigPayload;



public final class ConfigClientNetworking {

    private static final Gson GSON = new Gson();



    private ConfigClientNetworking() {}



    public static void send(TwitchCraftConfig sharedSettings, StreamerProfile localProfile) {

        ClientPlayNetworking.send(new SaveConfigPayload(

                GSON.toJson(sharedSettings),

                GSON.toJson(localProfile)));

    }

}

