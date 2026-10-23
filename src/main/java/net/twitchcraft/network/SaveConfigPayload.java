package net.twitchcraft.network;



import net.minecraft.network.RegistryFriendlyByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;

import net.minecraft.network.codec.StreamCodec;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.minecraft.resources.ResourceLocation;

import net.twitchcraft.TwitchCraftMod;



public record SaveConfigPayload(String settingsJson, String profileJson) implements CustomPacketPayload {

    public static final Type<SaveConfigPayload> TYPE = new Type<>(

            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "save_config"));



    public static final StreamCodec<RegistryFriendlyByteBuf, SaveConfigPayload> CODEC =

            StreamCodec.composite(

                    ByteBufCodecs.STRING_UTF8, SaveConfigPayload::settingsJson,

                    ByteBufCodecs.STRING_UTF8, SaveConfigPayload::profileJson,

                    SaveConfigPayload::new);



    @Override

    public Type<? extends CustomPacketPayload> type() {

        return TYPE;

    }

}

