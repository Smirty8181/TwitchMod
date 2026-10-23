package net.twitchcraft.twitch;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.eventsub.condition.ChannelFollowV2Condition;
import com.github.twitch4j.eventsub.events.ChannelFollowEvent;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;
import com.github.twitch4j.helix.domain.User;
import net.minecraft.server.MinecraftServer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.stream.TwitchHypeEffects;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Фоллоу Twitch больше не приходит по IRC — подписка EventSub channel.follow v2.
 * Нужен oauth с {@code moderator:read:followers} (владелец канала или модер).
 */
public final class TwitchFollowHook {
    private static final ConcurrentHashMap<String, Long> LAST = new ConcurrentHashMap<>();

    private TwitchFollowHook() {}

    public static void attach(TwitchClient client, String channel, String targetPlayer, MinecraftServer server) {
        if (client == null) return;
        client.getEventManager().onEvent(ChannelFollowEvent.class, event -> {
            String nick = event.getUserName();
            if (nick == null || nick.isBlank()) nick = event.getUserLogin();
            if (nick == null || nick.isBlank()) return;
            String key = nick.toLowerCase(java.util.Locale.ROOT);
            long now = System.currentTimeMillis();
            Long prev = LAST.put(key, now);
            if (prev != null && now - prev < 8_000L) return;
            String who = nick;
            MinecraftServer srv = server;
            if (srv == null || !srv.isRunning()) return;
            srv.execute(() -> TwitchHypeEffects.onFollow(srv, targetPlayer, who));
        });

        Thread t = new Thread(() -> subscribe(client, channel), "TwitchCraft-Follow-" + channel);
        t.setDaemon(true);
        t.start();
    }

    private static void subscribe(TwitchClient client, String channel) {
        try {
            var helix = client.getHelix();
            var socket = client.getEventSocket();
            if (helix == null || socket == null) {
                TwitchCraftMod.LOGGER.warn("TwitchCraft: helix/eventsub выкл — фоллоу на экране не будут");
                return;
            }
            List<User> self = helix.getUsers(null, null, null).execute().getUsers();
            if (self == null || self.isEmpty()) {
                TwitchCraftMod.LOGGER.warn("TwitchCraft: не удалось узнать id токена для фоллоу");
                return;
            }
            String modId = self.getFirst().getId();
            List<User> broadcasters = helix.getUsers(null, null, List.of(channel)).execute().getUsers();
            if (broadcasters == null || broadcasters.isEmpty()) {
                TwitchCraftMod.LOGGER.warn("TwitchCraft: канал {} не найден в Helix", channel);
                return;
            }
            String bcId = broadcasters.getFirst().getId();
            var condition = ChannelFollowV2Condition.builder()
                    .broadcasterUserId(bcId)
                    .moderatorUserId(modId)
                    .build();
            boolean ok = socket.register(SubscriptionTypes.CHANNEL_FOLLOW_V2, condition);
            if (ok) {
                TwitchCraftMod.LOGGER.info("TwitchCraft: фоллоу EventSub на #{}", channel);
            } else {
                TwitchCraftMod.LOGGER.warn(
                        "TwitchCraft: фоллоу на #{} не подписались — токену нужно право moderator:read:followers",
                        channel);
            }
        } catch (Exception e) {
            TwitchCraftMod.LOGGER.warn(
                    "TwitchCraft: фоллоу EventSub не взлетел (#{}) — {}",
                    channel, e.getMessage());
        }
    }
}
