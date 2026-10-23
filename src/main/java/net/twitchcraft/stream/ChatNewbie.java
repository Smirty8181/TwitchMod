package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.twitchcraft.config.StreamerProfile;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Цыплёнок только на first-msg канала: человек впервые написал в этот Twitch-чат.
 * Не «первый за стрим» — флаг шлёт сам Twitch ({@code first-msg=1}).
 */
public final class ChatNewbie {
    private static final Set<String> DEBOUNCE = ConcurrentHashMap.newKeySet();

    private ChatNewbie() {}

    public static void reset() {
        DEBOUNCE.clear();
    }

    public static void onMessage(MinecraftServer server, String user, String targetPlayer, boolean firstOnChannel) {
        if (!firstOnChannel || server == null || user == null || user.isBlank()) return;
        String key = user.toLowerCase(Locale.ROOT).trim();
        if (key.isEmpty() || isBot(key) || isStreamer(key)) return;
        if (!DEBOUNCE.add(key)) return;
        TwitchHypeEffects.onNewbie(server, targetPlayer, user.trim());
    }

    private static boolean isStreamer(String key) {
        for (StreamerProfile p : StreamerProfiles.active()) {
            if (p.twitchChannel != null && key.equals(p.twitchChannel.toLowerCase(Locale.ROOT).replace("#", ""))) {
                return true;
            }
            if (p.minecraftName != null && key.equals(p.minecraftName.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBot(String key) {
        return key.equals("nightbot") || key.equals("streamelements") || key.equals("moobot")
                || key.equals("fossabot") || key.equals("wizebot");
    }
}
