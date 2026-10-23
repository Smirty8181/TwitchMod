package net.twitchcraft.hud;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.stream.StreamStats;

public final class ChaosBossBar {
    private ServerBossEvent bossEvent;

    public void update(ServerPlayer player) {
        var stream = ConfigManager.get().stream;
        if (!stream.chaosMeterEnabled || !stream.chaosBossBar) {
            remove(player);
            return;
        }

        if (bossEvent == null) {
            bossEvent = new ServerBossEvent(
                    Component.empty(),
                    BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.PROGRESS
            );
            bossEvent.setDarkenScreen(false);
            bossEvent.setPlayBossMusic(false);
            bossEvent.setCreateWorldFog(false);
        }

        int cur = StreamStats.forPlayer(player.getGameProfile().name()).getChaosMeter();
        int max = Math.max(1, stream.chaosMeterMax);
        bossEvent.setProgress((float) cur / max);
        bossEvent.setName(Component.literal("§4🔥 §f" + cur + "§7/§f" + max));

        if (!bossEvent.getPlayers().contains(player)) {
            bossEvent.addPlayer(player);
        }
    }

    public void remove(ServerPlayer player) {
        if (bossEvent != null) {
            bossEvent.removePlayer(player);
        }
    }

    public void clear() {
        if (bossEvent != null) {
            bossEvent.removeAllPlayers();
            bossEvent = null;
        }
    }
}
