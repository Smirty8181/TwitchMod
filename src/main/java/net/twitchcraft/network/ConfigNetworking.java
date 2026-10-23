package net.twitchcraft.network;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.config.ConfigApply;
import net.twitchcraft.config.ConfigValidator;
import net.twitchcraft.config.StreamerProfile;
import net.twitchcraft.config.TwitchCraftConfig;

public final class ConfigNetworking {
    private static final Gson GSON = new Gson();

    private ConfigNetworking() {}

    public static void registerServer() {
        NetworkRegistration.registerPlayC2S();

        ServerPlayNetworking.registerGlobalReceiver(SaveConfigPayload.TYPE, (payload, ctx) -> {
            var server = ctx.player().level().getServer();
            if (server == null) return;
            server.execute(() -> {
                ServerPlayer player = ctx.player();

                if (!ConfigApply.canEdit(player)) {
                    player.sendSystemMessage(Component.literal("§cНет прав на настройки TwitchCraft"));
                    return;
                }

                String reject = ConfigValidator.rejectOversized(payload.settingsJson(), payload.profileJson());
                if (reject != null) {
                    player.sendSystemMessage(Component.literal("§cКонфиг слишком большой"));
                    return;
                }

                TwitchCraftConfig shared = GSON.fromJson(payload.settingsJson(), TwitchCraftConfig.class);
                if (shared == null) {
                    player.sendSystemMessage(Component.literal("§cОшибка конфига"));
                    return;
                }

                StreamerProfile profile = payload.profileJson().isBlank()
                        ? new StreamerProfile()
                        : GSON.fromJson(payload.profileJson(), StreamerProfile.class);
                if (profile == null) profile = new StreamerProfile();

                ConfigApply.apply(server, player, shared, profile);

                if (ConfigApply.canEditSharedSettings(player)) {
                    player.sendSystemMessage(Component.literal("§aTwitchCraft: настройки сохранены"));
                } else {
                    player.sendSystemMessage(Component.literal("§aTwitchCraft: профиль и тема UI сохранены"));
                }
            });
        });
    }
}
