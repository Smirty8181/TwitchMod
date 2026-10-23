package net.twitchcraft.twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import net.minecraft.server.MinecraftServer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.StreamerProfile;
import net.twitchcraft.config.TwitchCraftConfig;
import net.twitchcraft.stream.StreamerProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TwitchConnectionManager {
    private final Map<String, TwitchClient> clients = new ConcurrentHashMap<>();
    private final Map<String, String> channelToPlayer = new ConcurrentHashMap<>();
    private final ChatCommandProcessor processor = new ChatCommandProcessor();
    private volatile MinecraftServer server;
    private volatile boolean connected;
    private volatile String lastError = "";
    private volatile long connectedSince;
    private long nextReconnectAt;
    private long reconnectBackoffMs = 30_000;
    private int reconnectTick;

    private static final int RECONNECT_CHECK_INTERVAL = 800;

    public void connect(MinecraftServer server) {
        this.server = server;
        disconnect();

        List<ConnectionSpec> specs = buildSpecs();
        if (specs.isEmpty()) {
            lastError = "Twitch не настроен (канал + OAuth)";
            TwitchCraftMod.LOGGER.warn("TwitchCraft: no twitch profiles configured");
            return;
        }

        int ok = 0;
        for (ConnectionSpec spec : specs) {
            if (connectOne(spec)) ok++;
        }

        connected = ok > 0;
        if (connected) {
            connectedSince = System.currentTimeMillis();
            lastError = "";
            reconnectBackoffMs = 30_000;
            TwitchCraftMod.LOGGER.info("TwitchCraft connected to {} channel(s)", ok);
        }
    }

    /** Периодическая проверка IRC и автореконнект. */
    public void tick(MinecraftServer server) {
        this.server = server;
        if (!ConfigManager.get().twitch.autoConnect) return;
        if (server == null || !server.isRunning()) return;

        reconnectTick++;
        if (reconnectTick < RECONNECT_CHECK_INTERVAL) return;
        reconnectTick = 0;

        if (isHealthy()) {
            reconnectBackoffMs = 30_000;
            return;
        }

        long now = System.currentTimeMillis();
        if (now < nextReconnectAt) return;

        TwitchCraftMod.LOGGER.warn("TwitchCraft IRC disconnected — auto-reconnect in {}s backoff",
                reconnectBackoffMs / 1000);
        connect(server);
        nextReconnectAt = now + reconnectBackoffMs;
        reconnectBackoffMs = Math.min(reconnectBackoffMs * 2, 300_000);
    }

    private boolean isHealthy() {
        if (clients.isEmpty() || !connected) return false;
        for (Map.Entry<String, TwitchClient> entry : clients.entrySet()) {
            try {
                if (!entry.getValue().getChat().isChannelJoined(entry.getKey())) return false;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private record ConnectionSpec(String channel, String token, String targetPlayer) {}

    private List<ConnectionSpec> buildSpecs() {
        List<ConnectionSpec> specs = new ArrayList<>();
        for (StreamerProfile profile : StreamerProfiles.active()) {
            if (profile.hasTwitch()) {
                specs.add(new ConnectionSpec(
                        normalizeChannel(profile.twitchChannel),
                        normalizeToken(profile.oauthToken),
                        profile.minecraftName));
            }
        }

        if (specs.isEmpty()) {
            TwitchCraftConfig config = ConfigManager.get();
            String channel = normalizeChannel(config.twitch.channel);
            String token = normalizeToken(config.twitch.oauthToken);
            if (!channel.isEmpty() && !token.isEmpty()) {
                String target = StreamerProfiles.active().stream()
                        .map(p -> p.minecraftName).findFirst().orElse("");
                specs.add(new ConnectionSpec(channel, token, target));
            }
        }
        return specs;
    }

    private boolean connectOne(ConnectionSpec spec) {
        try {
            OAuth2Credential credential = new OAuth2Credential("twitch", spec.token);
            TwitchClient twitchClient = TwitchClientBuilder.builder()
                    .withEnableChat(true)
                    .withEnableHelix(true)
                    .withEnableEventSocket(true)
                    .withChatAccount(credential)
                    .withDefaultAuthToken(credential)
                    .build();

            String target = spec.targetPlayer;
            twitchClient.getEventManager().onEvent(ChannelMessageEvent.class,
                    event -> onChatMessage(event, target));
            TwitchRewardHandler.register(twitchClient, target, server);
            TwitchFollowHook.attach(twitchClient, spec.channel, target, server);
            var chat = twitchClient.getChat();
            try {
                if (!chat.isChannelJoined(spec.channel)) {
                    chat.joinChannel(spec.channel);
                }
            } catch (Exception ignored) {
                chat.joinChannel(spec.channel);
            }

            clients.put(spec.channel, twitchClient);
            channelToPlayer.put(spec.channel, target);
            TwitchCraftMod.LOGGER.info("TwitchCraft joined #{} → {}", spec.channel, target.isBlank() ? "auto" : target);
            return true;
        } catch (Throwable e) {
            lastError = TwitchStatusFormatter.describeFailure(e);
            TwitchCraftMod.LOGGER.error("Failed to connect Twitch channel {}", spec.channel, e);
            return false;
        }
    }

    public void disconnect() {
        for (TwitchClient c : clients.values()) {
            try {
                c.close();
            } catch (Exception e) {
                TwitchCraftMod.LOGGER.debug("Error closing Twitch client", e);
            }
        }
        clients.clear();
        channelToPlayer.clear();
        connected = false;
    }

    private void onChatMessage(ChannelMessageEvent event, String targetPlayer) {
        if (server == null || !server.isRunning()) return;
        String user = event.getUser().getName();
        String message = event.getMessage();
        String channel = normalizeChannel(event.getChannel().getName());
        String mapped = channelToPlayer.getOrDefault(channel, targetPlayer);
        net.twitchcraft.stream.ChatViewerRegistry.seen(user);
        net.twitchcraft.stream.ChatActivity.onMessage(user);
        if (event.getMessageEvent() != null) {
            event.getMessageEvent().getUserChatColor()
                    .ifPresent(hex -> net.twitchcraft.stream.TwitchNickColors.rememberHex(user, hex));
        }
        boolean firstOnChannel = event.isDesignatedFirstMessage() || event.isUserIntroduction();
        MinecraftServer srv = server;
        server.execute(() -> {
            net.twitchcraft.stream.ChatNewbie.onMessage(srv, user, mapped, firstOnChannel);
            net.twitchcraft.stream.RaidNameBinder.onChatter(srv, user);
            net.twitchcraft.stream.UnnamedPatron.onTwitchLine(srv, user, message);
            processor.handle(srv, user, message, mapped);
        });
    }

    public void sendChatMessage(String message) {
        if (!ConfigManager.get().feedback.announceInTwitchChat) return;
        if (clients.isEmpty()) return;
        String trimmed = message.length() > 500 ? message.substring(0, 500) : message;
        for (Map.Entry<String, TwitchClient> entry : clients.entrySet()) {
            try {
                entry.getValue().getChat().sendMessage(entry.getKey(), trimmed, java.util.Map.of());
            } catch (Exception e) {
                TwitchCraftMod.LOGGER.debug("Failed to send chat to {}", entry.getKey(), e);
            }
        }
    }

    public void sendChatMessageTo(String channel, String message) {
        if (!ConfigManager.get().feedback.announceInTwitchChat) return;
        TwitchClient c = clients.get(normalizeChannel(channel));
        if (c == null) return;
        try {
            String trimmed = message.length() > 500 ? message.substring(0, 500) : message;
            c.getChat().sendMessage(normalizeChannel(channel), trimmed, java.util.Map.of());
        } catch (Exception e) {
            TwitchCraftMod.LOGGER.debug("Failed to send chat", e);
        }
    }

    public boolean isConnected() {
        return connected && !clients.isEmpty();
    }

    public int channelCount() {
        return clients.size();
    }

    public String getLastError() {
        return lastError;
    }

    public long getConnectedSince() {
        return connectedSince;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public String getChannel() {
        if (clients.size() == 1) return clients.keySet().iterator().next();
        return clients.size() + " channels";
    }

    public boolean testCommand(MinecraftServer server, String commandId) {
        return executeById(server, "Тест", commandId, true, null);
    }

    public boolean executeById(MinecraftServer server, String user, String commandId,
                               boolean bypassCooldown, String targetPlayerName) {
        return processor.executeById(server, user, commandId, bypassCooldown, targetPlayerName);
    }

    private static String normalizeChannel(String channel) {
        if (channel == null) return "";
        return channel.trim().toLowerCase(Locale.ROOT).replace("#", "");
    }

    private static String normalizeToken(String token) {
        if (token == null) return "";
        String t = token.trim();
        if (t.startsWith("oauth:")) t = t.substring(6);
        return t;
    }
}
