package net.twitchcraft.twitch;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.chat.events.channel.CheerEvent;
import com.github.twitch4j.chat.events.channel.RaidEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.common.events.domain.EventUser;
import net.minecraft.server.MinecraftServer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.stream.TwitchHypeEffects;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/** Сабы, рейды и биты с живого Twitch IRC. */
public final class TwitchRewardHandler {
    private static final ConcurrentHashMap<String, Long> LAST_RAID = new ConcurrentHashMap<>();

    private TwitchRewardHandler() {}

    public static void register(TwitchClient client, String targetPlayer, MinecraftServer server) {
        if (!ConfigManager.get().twitch.eventSubEnabled) return;

        ChatCommandProcessor processor = new ChatCommandProcessor();
        var cfg = ConfigManager.get().twitch;

        client.getEventManager().onEvent(SubscriptionEvent.class, event -> {
            if (server == null || !server.isRunning()) return;
            String nick = userName(event.getUser());
            if (nick.isBlank()) return;
            boolean gifted = Boolean.TRUE.equals(event.getGifted());
            String gifter = gifted ? userName(event.getGiftedBy()) : "";
            server.execute(() -> TwitchHypeEffects.onSub(server, targetPlayer, nick, gifted, gifter));
        });

        client.getEventManager().onEvent(RaidEvent.class, event -> {
            if (server == null || !server.isRunning()) return;
            String raider = userName(event.getRaider());
            if (raider.isBlank()) return;
            int viewers = event.getViewers() == null ? 1 : Math.max(1, event.getViewers());
            String key = raider.toLowerCase(Locale.ROOT) + ":" + (targetPlayer == null ? "" : targetPlayer);
            long now = System.currentTimeMillis();
            Long prev = LAST_RAID.get(key);
            if (prev != null && now - prev < 15_000L) return;
            LAST_RAID.put(key, now);
            server.execute(() -> TwitchHypeEffects.onRaid(server, targetPlayer, raider, viewers));
        });

        client.getEventManager().onEvent(CheerEvent.class, event -> {
            if (server == null || !server.isRunning()) return;
            Integer bits = event.getBits();
            if (bits == null || bits < cfg.bitsMinAmount) return;
            String user = userName(event.getUser());
            if (user.isBlank()) return;
            server.execute(() -> {
                processor.executeById(server, user, "vieweregg", true, targetPlayer);
                TwitchCraftMod.LOGGER.info("TwitchCraft bits {} from {}", bits, user);
            });
        });
    }

    private static String userName(EventUser user) {
        if (user == null) return "";
        String n = user.getName();
        return n == null ? "" : n.trim();
    }
}
