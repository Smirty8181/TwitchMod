package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Проверка, что целевой игрок всё ещё в сети. */
public final class TargetPlayerUtil {
    private TargetPlayerUtil() {}

    public static boolean isOnline(MinecraftServer server, ServerPlayer player) {
        return player != null && player.isAlive()
                && server.getPlayerList().getPlayer(player.getUUID()) != null;
    }
}
