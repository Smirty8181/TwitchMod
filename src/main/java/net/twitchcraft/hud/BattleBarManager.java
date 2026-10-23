package net.twitchcraft.hud;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.commands.ChatGameCommand;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BattleBarManager {
    private final Map<String, ChatVsStreamerBar> bars = new ConcurrentHashMap<>();

    public ChatVsStreamerBar forPlayer(ServerPlayer player) {
        return bars.computeIfAbsent(key(player), k -> {
            ChatVsStreamerBar bar = new ChatVsStreamerBar();
            bar.reset();
            return bar;
        });
    }

    public void onCommand(MinecraftServer server, ServerPlayer target, ChatGameCommand command) {
        forPlayer(target).onCommand(server, target, command);
    }

    public void onStreamerDeath(ServerPlayer player) {
        forPlayer(player).onStreamerDeath();
    }

    public void onGhostKilled(ServerPlayer player) {
        forPlayer(player).onGhostKilled();
    }

    public void update(ServerPlayer player) {
        forPlayer(player).update(player);
    }

    public void remove(ServerPlayer player) {
        ChatVsStreamerBar bar = bars.get(key(player));
        if (bar != null) bar.remove(player);
    }

    public void clear() {
        bars.values().forEach(ChatVsStreamerBar::clear);
        bars.clear();
    }

    public void resetAll() {
        bars.clear();
    }

    private static String key(ServerPlayer player) {
        return player.getGameProfile().name().toLowerCase(Locale.ROOT);
    }
}
