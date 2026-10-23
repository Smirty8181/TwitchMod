package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.feedback.AnnouncementService;

public final class HouseMode {
    private HouseMode() {}

    public static boolean isActive() {
        return ConfigManager.get().stream.houseMode;
    }

    public static void set(MinecraftServer server, boolean enabled) {
        ConfigManager.get().stream.houseMode = enabled;
        ConfigManager.save();
        if (enabled) {
            EffectRunner.cooldowns().clearAll();
        }
        announceInternal(server, enabled);
    }

    public static void toggle(MinecraftServer server) {
        set(server, !isActive());
    }

    public static void announce(MinecraftServer server, boolean enabled) {
        announceInternal(server, enabled);
    }

    private static void announceInternal(MinecraftServer server, boolean enabled) {
        String gameMsg = enabled
                ? "§d§lХАУСМОД §f— все КД отключены!"
                : "§7Хаусмод выключен · КД снова работают";
        String chatMsg = enabled
                ? "ХАУСМОД ВКЛ! Все команды без задержки — веселимся!"
                : "Хаусмод выключен.";
        EffectRunner.resolveTarget(server).ifPresent(p -> AnnouncementService.announceCustom(p, gameMsg));
        TwitchCraftMod.getConnectionManager().sendChatMessage(chatMsg);
        TwitchCraftMod.LOGGER.info("House mode {}", enabled ? "ON" : "OFF");
    }
}
