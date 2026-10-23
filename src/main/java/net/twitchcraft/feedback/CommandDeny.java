package net.twitchcraft.feedback;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.network.CommandDenyPayload;
import net.twitchcraft.stream.StreamTargets;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Сервер шлёт стримеру причину отказа команды. */
public final class CommandDeny {
    public static final String SPAM = "spam";
    public static final String COOLDOWN = "cooldown";
    public static final String RATE = "rate";
    public static final String DISABLED = "disabled";
    public static final String KARMA = "karma";
    public static final String SHIELD = "shield";
    public static final String HOLD = "hold";
    public static final String EVIL_BLOCK = "evil_block";
    public static final String EVIL_PACE = "evil_pace";
    public static final String HARDCORE = "hardcore";
    public static final String WHITELIST = "whitelist";
    public static final String OFFLINE = "offline";
    public static final String ACTIVE = "active";
    public static final String HELP = "help";
    public static final String PAUSE = "pause";

    private static final Map<String, Long> LAST_TOAST = new HashMap<>();

    private CommandDeny() {}

    public static void send(ServerPlayer player, String user, String commandRu, String reasonId, int seconds) {
        if (player == null || !allowToast(user)) return;
        CommandDenyPayload.send(player, user, commandRu, reasonId, seconds);
    }

    public static void send(MinecraftServer server, String targetPlayerName,
                            String user, String commandRu, String reasonId, int seconds) {
        java.util.Optional<ServerPlayer> hit = targetPlayerName != null && !targetPlayerName.isBlank()
                ? StreamTargets.resolve(server, targetPlayerName)
                : java.util.Optional.empty();
        hit.or(() -> StreamTargets.resolve(server))
                .ifPresent(p -> send(p, user, commandRu, reasonId, seconds));
    }

    private static boolean allowToast(String user) {
        String key = user == null ? "?" : user.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        Long last = LAST_TOAST.get(key);
        if (last != null && now - last < 850L) return false;
        LAST_TOAST.put(key, now);
        return true;
    }
}
