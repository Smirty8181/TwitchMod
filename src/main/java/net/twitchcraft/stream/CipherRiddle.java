package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * На экране !????? — угадал команду, получил личное усиление.
 */
public final class CipherRiddle {
    private static final int SLOTS = 2;
    private static final ChatGameCommand[] sealed = new ChatGameCommand[SLOTS];
    private static final boolean[] solved = new boolean[SLOTS];
    private static long until;
    private static int durationSec = 180;
    private static final Map<String, Long> BLESS = new ConcurrentHashMap<>();
    private static ChatGameCommand lastA;
    private static ChatGameCommand lastB;
    private static long nextRollAt;

    private CipherRiddle() {}

    public static void tick(MinecraftServer server) {
        if (!enabled()) {
            clearSeals();
            nextRollAt = 0;
            return;
        }
        if (HouseMode.isActive()) return;
        long now = System.currentTimeMillis();
        BLESS.entrySet().removeIf(e -> e.getValue() < now);
        if (until == 0) {
            if (nextRollAt == 0) nextRollAt = now + 40_000L;
            if (now < nextRollAt) return;
            roll(server);
            return;
        }
        if (now >= until) {
            clearSeals();
            nextRollAt = now + 35_000L;
            until = 0;
            return;
        }
    }

    public static boolean isActive() {
        return enabled() && until > System.currentTimeMillis() && !allSolved();
    }

    public static String statusLine() {
        if (!isActive()) {
            String bless = topBless();
            return bless;
        }
        StringBuilder sb = new StringBuilder("§eЗАГАДКА");
        for (int i = 0; i < SLOTS; i++) {
            if (sealed[i] == null) continue;
            sb.append("  §e").append(solved[i] ? "!" + sealed[i].getRussianName() : mask(sealed[i]));
        }
        sb.append("  §8").append(secondsLeft()).append("с");
        return sb.toString();
    }

    public static int secondsLeft() {
        if (until <= 0) return 0;
        return (int) Math.max(0, (until - System.currentTimeMillis() + 999) / 1000);
    }

    public static int applyCooldown(String user, int seconds) {
        if (seconds <= 0 || !isBlessed(user)) return seconds;
        return Math.max(4, (int) Math.round(seconds * 0.55));
    }

    public static boolean isBlessed(String user) {
        if (user == null || user.isBlank()) return false;
        Long t = BLESS.get(user.toLowerCase(Locale.ROOT));
        return t != null && t > System.currentTimeMillis();
    }

    public static void tryGuess(MinecraftServer server, String user, ChatGameCommand command) {
        if (!isActive() || command == null || user == null || user.isBlank()) return;
        if (StreamerProfiles.isStreamerIdentity(user)) return;
        for (int i = 0; i < SLOTS; i++) {
            if (solved[i] || sealed[i] == null) continue;
            if (sealed[i] != command) continue;
            solved[i] = true;
            BLESS.put(user.toLowerCase(Locale.ROOT), System.currentTimeMillis() + 90_000L);
            String ru = command.getRussianName();
            StreamTargets.resolve(server).ifPresent(p ->
                    AnnouncementService.announceCustom(p,
                            "§e§lУГАДАЛ! §f" + user + " §7→ §e!" + ru + " §8усиление 90с"));
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    user + " угадал !" + ru + "! Его команды сильнее 90 секунд.");
            if (allSolved()) until = System.currentTimeMillis() + 8_000L;
            return;
        }
    }

    public static void pushHud(MinecraftServer server) {
        if (!isActive()) return;
        String a = slotMask(0);
        String b = slotMask(1);
        String catA = slotCat(0);
        String catB = slotCat(1);
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "CIPHER",
                "ЗАГАДКА",
                a + " · " + b,
                secondsLeft(),
                Math.max(1, durationSec),
                0, 0,
                catA,
                catB,
                true));
    }

    private static void roll(MinecraftServer server) {
        durationSec = Math.max(60, ConfigManager.get().stream.cipherRiddleSeconds);
        ChatGameCommand[] pool = ChatGameCommand.riddlePool(ConfigManager.get());
        if (pool.length < 2) pool = ChatGameCommand.funPool(ConfigManager.get());
        List<ChatGameCommand> opts = new ArrayList<>();
        for (ChatGameCommand c : pool) {
            if (c == lastA || c == lastB) continue;
            opts.add(c);
        }
        if (opts.size() < 2) {
            opts.clear();
            for (ChatGameCommand c : pool) opts.add(c);
        }
        if (opts.size() < 2) {
            until = System.currentTimeMillis() + 25_000L;
            return;
        }
        java.util.Collections.shuffle(opts, ThreadLocalRandom.current());
        sealed[0] = opts.get(0);
        sealed[1] = opts.get(1);
        solved[0] = solved[1] = false;
        lastA = sealed[0];
        lastB = sealed[1];
        until = System.currentTimeMillis() + durationSec * 1000L;
        StreamTargets.resolve(server).ifPresent(p ->
                AnnouncementService.announceCustom(p,
                        "§eЗАГАДКА §7напиши команду: §e" + mask(sealed[0]) + " §8/ §e" + mask(sealed[1])));
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "Загадка чата: " + mask(sealed[0]) + "  и  " + mask(sealed[1])
                        + " — угадай !командой, получишь усиление.");
    }

    private static void clearSeals() {
        sealed[0] = sealed[1] = null;
        solved[0] = solved[1] = false;
        until = 0;
    }

    private static boolean allSolved() {
        return (sealed[0] == null || solved[0]) && (sealed[1] == null || solved[1]);
    }

    static String mask(ChatGameCommand cmd) {
        if (cmd == null) return "!???";
        String ru = cmd.getRussianName();
        if (ru.length() <= 3) return "!???";
        StringBuilder sb = new StringBuilder("!");
        sb.append(ru.charAt(0));
        for (int i = 1; i < ru.length(); i++) sb.append('?');
        return sb.toString();
    }

    private static String slotMask(int i) {
        if (sealed[i] == null) return "!???";
        return solved[i] ? "!" + sealed[i].getRussianName() : mask(sealed[i]);
    }

    private static String slotCat(int i) {
        if (sealed[i] == null) return "";
        return sealed[i].getCategoryRu();
    }

    private static String topBless() {
        long now = System.currentTimeMillis();
        String best = "";
        long left = 0;
        for (var e : BLESS.entrySet()) {
            if (e.getValue() > now && e.getValue() - now > left) {
                left = e.getValue() - now;
                best = e.getKey();
            }
        }
        if (best.isBlank()) return "";
        return "§a✦ " + best + " §7усилен §f" + ((left + 999) / 1000) + "с";
    }

    private static boolean enabled() {
        return ConfigManager.get().stream.cipherRiddleEnabled;
    }
}
