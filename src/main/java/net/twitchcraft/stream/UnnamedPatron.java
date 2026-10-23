package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.feedback.AnnouncementService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Тихий покровитель чата. Ник нельзя произносить стримерам.
 * Письмо в майн/Twitch-чате стримера или кнопка «я проговорился» — сжигает.
 */
public final class UnnamedPatron {
    private static final Map<String, Long> BANNED_UNTIL = new ConcurrentHashMap<>();

    private static String patron = "";
    private static long until;
    private static long nextPickAt = System.currentTimeMillis() + 90_000L;
    private static boolean announced;

    private UnnamedPatron() {}

    public static void tick(MinecraftServer server) {
        if (!enabled()) {
            patron = "";
            return;
        }
        if (HouseMode.isActive()) return;
        long now = System.currentTimeMillis();
        if (isActive() && now >= until) {
            expireQuiet(server);
        }
        if (!isActive() && now >= nextPickAt) {
            pick(server);
        }
    }

    public static boolean isActive() {
        return enabled() && patron != null && !patron.isBlank() && System.currentTimeMillis() < until;
    }

    public static boolean isPatron(String user) {
        return isActive() && user != null && patron.equalsIgnoreCase(user.trim());
    }

    public static String statusLine() {
        if (!isActive()) return "";
        return "§5✦ ПОКРОВИТЕЛЬ §8ник нельзя";
    }

    public static int applyCooldown(String user, int seconds) {
        if (seconds <= 0 || !isPatron(user)) return seconds;
        return Math.max(4, (int) Math.round(seconds * 0.45));
    }

    public static void tryAmplify(MinecraftServer server, ServerPlayer target, String user, ChatGameCommand command) {
        if (!isPatron(user) || command == null || target == null) return;
        if (catastrophic(command)) return;
        if (ThreadLocalRandom.current().nextFloat() > 0.40f) return;
        CommandContext bonus = new CommandContext(server, target, user, "!" + command.getId(), command, true);
        try {
            command.execute(bonus);
        } catch (Exception e) {
            TwitchCraftMod.LOGGER.debug("Patron amplify failed: {}", command.getId(), e);
        }
        AnnouncementService.announceCustom(target, "§5✦ усиление §7без имени");
    }

    public static void onTwitchLine(MinecraftServer server, String user, String message) {
        if (server == null || user == null || message == null) return;
        if (StreamerProfiles.isStreamerIdentity(user)) {
            onStreamerText(server, message);
        }
    }

    public static void onStreamerText(MinecraftServer server, String text) {
        if (!isActive() || text == null || text.isBlank()) return;
        if (mentions(text, patron)) {
            burn(server, "стример назвал ник");
        }
    }

    public static void confess(MinecraftServer server, ServerPlayer player) {
        if (player == null) return;
        if (!isActive()) {
            AnnouncementService.announceCustom(player, "§7Покровителя сейчас нет");
            return;
        }
        burn(server, player.getGameProfile().name() + " проговорился");
    }

    private static void pick(MinecraftServer server) {
        List<String> pool = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (String nick : ChatViewerRegistry.recentDisplayNames()) {
            if (StreamerProfiles.isStreamerIdentity(nick)) continue;
            Long ban = BANNED_UNTIL.get(nick.toLowerCase(Locale.ROOT));
            if (ban != null && ban > now) continue;
            if (nick.length() < 3) continue;
            pool.add(nick);
        }
        int minutes = Math.max(6, ConfigManager.get().stream.unnamedPatronMinutes);
        if (pool.isEmpty()) {
            nextPickAt = now + 45_000L;
            return;
        }
        patron = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        until = now + minutes * 60_000L;
        nextPickAt = until + 25_000L;
        announced = true;
        announceAll(server,
                "§5✦ §dПокровитель среди чата. §7Скажешь ник вслух — сгорит.");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "Покровитель выбран. Стримеры: не произносите ники. Кто это — знает только удача.");
        TwitchCraftMod.LOGGER.info("Unnamed patron picked (hidden)");
    }

    private static void expireQuiet(MinecraftServer server) {
        patron = "";
        until = 0;
        nextPickAt = System.currentTimeMillis() + 20_000L;
        if (announced) {
            announceAll(server, "§8Покровитель ушёл в тень");
            announced = false;
        }
    }

    private static void burn(MinecraftServer server, String why) {
        String name = patron;
        patron = "";
        until = 0;
        nextPickAt = System.currentTimeMillis() + 90_000L;
        if (name != null && !name.isBlank()) {
            BANNED_UNTIL.put(name.toLowerCase(Locale.ROOT), System.currentTimeMillis() + 20 * 60_000L);
        }
        announceAll(server, "§c§lПОКРОВИТЕЛЬ СГОРЕЛ: §f" + name);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "Покровитель раскрыт: " + name + " (" + why + "). Усиление снято.");
    }

    static boolean mentions(String text, String nick) {
        if (text == null || nick == null) return false;
        String n = compact(nick);
        if (n.length() < 3) return false;
        String t = compact(text);
        if (t.equals(n)) return true;
        if (n.length() >= 4 && t.contains(n)) return true;
        String padded = " " + t.replaceAll("[^a-z0-9а-яё]+", " ") + " ";
        return padded.contains(" " + n + " ");
    }

    private static String compact(String s) {
        return s.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").trim();
    }

    private static boolean catastrophic(ChatGameCommand command) {
        return command == ChatGameCommand.WITHER || command == ChatGameCommand.DRAGON
                || command == ChatGameCommand.CHAT_PHANTOM || command == ChatGameCommand.BOMB
                || command == ChatGameCommand.TNT_RAIN || command == ChatGameCommand.METEOR
                || command == ChatGameCommand.CLEAR || command == ChatGameCommand.STOP;
    }

    private static boolean enabled() {
        return ConfigManager.get().stream.unnamedPatronEnabled;
    }

    private static void announceAll(MinecraftServer server, String line) {
        if (server == null) return;
        for (ServerPlayer p : StreamTargets.onlineStreamers(server)) {
            AnnouncementService.announceCustom(p, line);
        }
    }
}
