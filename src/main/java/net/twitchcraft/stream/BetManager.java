package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * !ставка смерть / !ставка жив — чат угадывает, умрёт ли стример до конца таймера.
 * Без КД, как суд: чем больше народа впишется, тем веселее.
 */
public final class BetManager {
    private static final BetManager INSTANCE = new BetManager();

    private boolean active;
    private long endTime;
    private int durationSec;
    private final Map<String, Boolean> bets = new HashMap<>();

    private BetManager() {}

    public static BetManager get() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active && System.currentTimeMillis() < endTime;
    }

    public int secondsLeft() {
        if (!active) return 0;
        return (int) Math.max(0, (endTime - System.currentTimeMillis() + 999) / 1000);
    }

    public int deathBets() {
        int n = 0;
        for (boolean dies : bets.values()) if (dies) n++;
        return n;
    }

    public int liveBets() {
        return bets.size() - deathBets();
    }

    public String statusLine() {
        if (!isActive()) return "";
        return "§6§lСТАВКА §f" + secondsLeft() + "s §c☠" + deathBets() + " §a♥" + liveBets();
    }

    public static boolean isBetAlias(String firstWord) {
        if (firstWord == null) return false;
        String w = firstWord.toLowerCase(Locale.ROOT);
        return w.equals("ставка") || w.equals("bet") || w.equals("ставки") || w.equals("прогноз");
    }

    public void handle(MinecraftServer server, String twitchUser, String rawMessage) {
        Boolean dies = parseSide(rawMessage);
        ServerPlayer target = StreamTargets.resolve(server).orElse(null);
        if (target == null) return;

        if (dies == null) {
            if (isActive()) {
                AnnouncementService.announceCustom(target,
                        "§6Ставка §f" + secondsLeft() + "с §c☠" + deathBets() + " §a♥" + liveBets()
                                + " §8· §e!ставка смерть §7/ §e!ставка жив");
            } else {
                startRound(server, target);
                AnnouncementService.announceCustom(target,
                        "§6§lСТАВКИ ОТКРЫТЫ! §e!ставка смерть §7или §e!ставка жив §8· §f"
                                + durationSec + "с");
            }
            return;
        }

        if (!isActive()) {
            startRound(server, target);
        }

        String key = twitchUser.toLowerCase(Locale.ROOT);
        if (bets.containsKey(key)) {
            return;
        }
        bets.put(key, dies);
        if (bets.size() == 1 || bets.size() % 5 == 0) {
            AnnouncementService.announceCustom(target,
                    "§6Ставка §f" + twitchUser + " §7→ "
                            + (dies ? "§cумрёт" : "§aвыживет")
                            + " §8· §c" + deathBets() + " §7/ §a" + liveBets());
        }
        pushHud(server);
    }

    public void onStreamerDeath(MinecraftServer server, ServerPlayer player) {
        if (!isActive()) return;
        resolve(server, true);
    }

    public void tick(MinecraftServer server) {
        if (!active) return;
        if (System.currentTimeMillis() >= endTime) {
            resolve(server, false);
        }
    }

    public void pushHud(MinecraftServer server) {
        if (!isActive()) return;
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "BET",
                "СТАВКА",
                "умрёт или выживет?",
                secondsLeft(),
                durationSec,
                deathBets(),
                liveBets(),
                "☠ умрёт",
                "♥ жив",
                true));
    }

    private void startRound(MinecraftServer server, ServerPlayer target) {
        bets.clear();
        durationSec = Math.max(20, ConfigManager.get().stream.betSeconds);
        endTime = System.currentTimeMillis() + durationSec * 1000L;
        active = true;
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "СТАВКИ! Умрёт ли стример за " + durationSec + "с? Пиши: !ставка смерть  или  !ставка жив");
        pushHud(server);
    }

    private void resolve(MinecraftServer server, boolean died) {
        active = false;
        endTime = 0;
        List<String> winners = new ArrayList<>();
        for (var e : bets.entrySet()) {
            if (e.getValue() == died) winners.add(e.getKey());
        }
        int deathAcc = 0;
        int liveAcc = 0;
        for (boolean d : bets.values()) {
            if (d) deathAcc++;
            else liveAcc++;
        }
        final int death = deathAcc;
        final int live = liveAcc;
        String title = died ? "ЧАТ УГАДАЛ: СМЕРТЬ" : "СТРИМЕР ВЫЖИЛ";
        String winnerLine;
        if (winners.isEmpty()) {
            winnerLine = bets.isEmpty() ? "никто не ставил" : "никто не угадал";
        } else if (winners.size() <= 3) {
            winnerLine = "угадали: " + String.join(", ", winners);
        } else {
            winnerLine = winners.size() + " угадали · " + String.join(", ", winners.subList(0, 3)) + "…";
        }

        StreamTargets.resolve(server).ifPresent(p ->
                AnnouncementService.announceCustom(p,
                        (died ? "§c§l" : "§a§l") + title + " §7" + winnerLine
                                + " §8(☠" + death + " / ♥" + live + ")"));
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                title + "! ☠" + death + " / ♥" + live + " — " + winnerLine);
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "BET", title, winnerLine, 4, 4, death, live, "☠ умрёт", "♥ жив", true));
        bets.clear();
        ServerTaskQueue.schedule(server, 80, () -> ShowdownDirector.refresh(server));
    }

    private static Boolean parseSide(String raw) {
        if (raw == null) return null;
        String body = raw.startsWith("!") ? raw.substring(1).trim() : raw.trim();
        int sp = body.indexOf(' ');
        if (sp < 0) return null;
        String arg = body.substring(sp + 1).trim().toLowerCase(Locale.ROOT);
        if (arg.isEmpty()) return null;
        String first = arg.contains(" ") ? arg.substring(0, arg.indexOf(' ')) : arg;
        if (first.equals("смерть") || first.equals("умрет") || first.equals("умрёт")
                || first.equals("die") || first.equals("dead") || first.equals("death")
                || first.equals("погибнет") || first.equals("умрёт")) {
            return true;
        }
        if (first.equals("жив") || first.equals("выживет") || first.equals("live")
                || first.equals("survive") || first.equals("жизнь") || first.equals("alive")) {
            return false;
        }
        return null;
    }
}
