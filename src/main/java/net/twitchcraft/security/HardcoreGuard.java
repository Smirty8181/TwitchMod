package net.twitchcraft.security;

import net.minecraft.server.level.ServerLevel;
import net.twitchcraft.config.TwitchCraftConfig;

public final class HardcoreGuard {
    private HardcoreGuard() {}

    public static boolean isBlocked(ServerLevel world, TwitchCraftConfig.SecuritySettings security) {
        if (!security.disableInHardcore) {
            return false;
        }
        return world.getLevelData().isHardcore();
    }
}
