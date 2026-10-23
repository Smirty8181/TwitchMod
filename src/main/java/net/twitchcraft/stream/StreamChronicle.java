package net.twitchcraft.stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.EvilCommands;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.CustomCommandDefinition;
import net.twitchcraft.feedback.CommandDeny;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Дневник стрима: каждая команда, отказ, смерть, донат и ник, кто жал.
 * Файлы: {@code logs/twitchcraft/YYYY-MM-DD.log} и {@code .jsonl}.
 */
public final class StreamChronicle {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Object LOCK = new Object();
    private static final ConcurrentHashMap<String, ViewerRow> VIEWERS = new ConcurrentHashMap<>();

    private static Path dir;
    private static String day = "";
    private static BufferedWriter textOut;
    private static BufferedWriter jsonOut;
    private static int commandsOk;
    private static int commandsDeny;
    private static int deaths;
    private static int donations;
    private static int unknown;
    private static int tickSec;
    private static boolean openLogged;

    private StreamChronicle() {}

    public static void open(MinecraftServer server) {
        if (!enabled()) return;
        synchronized (LOCK) {
            ensureOpen();
            if (openLogged) return;
            openLogged = true;
            String world = server != null ? server.getWorldData().getLevelName() : "?";
            line("info", "старт мира «" + world + "»");
            json("info", Map.of("world", world));
        }
    }

    public static void close() {
        synchronized (LOCK) {
            if (textOut == null && jsonOut == null) {
                VIEWERS.clear();
                return;
            }
            writeSummaryUnlocked(true);
            closeWriters();
            VIEWERS.clear();
            commandsOk = commandsDeny = deaths = donations = unknown = 0;
            tickSec = 0;
            openLogged = false;
            day = "";
        }
    }

    public static void tick() {
        if (!enabled()) return;
        tickSec++;
        if (tickSec % 300 == 0) {
            synchronized (LOCK) {
                ensureOpen();
                writeSummaryUnlocked(false);
            }
        }
    }

    public static String pathHint() {
        Path d = logsDir();
        return d == null ? "logs/twitchcraft" : d.toAbsolutePath().toString();
    }

    public static void command(String viewer, ChatGameCommand command, String streamer, int cdSec, double cdMult) {
        if (command == null) return;
        command(viewer, command.getId(), command.getRussianName(), streamer,
                kindLabel(command), cdSec, cdMult, true, "");
    }

    public static void command(String viewer, CustomCommandDefinition def, String streamer, int cdSec, double cdMult) {
        if (def == null) return;
        String ru = def.displayName();
        command(viewer, def.cooldownKey(), ru, streamer,
                def.positive ? "добро" : "зло", cdSec, cdMult, true, "");
    }

    public static void deny(String viewer, String commandRu, String reasonId, int left, String streamer) {
        if (!enabled()) return;
        String reason = reasonRu(reasonId);
        bump(viewer, false, "");
        commandsDeny++;
        String who = clean(viewer);
        String cmd = clean(commandRu);
        String line = "deny  " + who + " → !" + cmd
                + (left > 0 ? "  " + reason + " " + left + "с" : "  " + reason)
                + (blank(streamer) ? "" : "  на " + clean(streamer));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("u", who);
        row.put("c", cmd);
        row.put("why", reasonId == null ? "" : reasonId);
        row.put("left", left);
        row.put("p", clean(streamer));
        write("deny", line, row);
    }

    public static void unknown(String viewer, String trigger, String streamer) {
        if (!enabled() || blank(trigger) || trigger.length() > 24) return;
        unknown++;
        bump(viewer, false, "");
        String who = clean(viewer);
        String cmd = clean(trigger);
        write("miss", "miss  " + who + " → !" + cmd,
                Map.of("u", who, "c", cmd, "p", clean(streamer)));
    }

    public static void death(String streamer, DeathCause cause, int total, int window, int need,
                             String coords, boolean evilBlock) {
        if (!enabled()) return;
        deaths++;
        String p = clean(streamer);
        String killer = cause != null && cause.hasKiller() ? clean(cause.killer()) : "";
        String why = cause != null ? clean(cause.detail()) : "";
        String line = "death " + p
                + (killer.isBlank() ? "  умер: " + why : "  убил: " + killer + (why.isBlank() ? "" : " (" + why + ")"))
                + "  #" + total + "  (" + window + "/" + need + ")"
                + (coords == null || coords.isBlank() ? "" : "  " + strip(coords))
                + (evilBlock ? "  БЛОК ЗЛА" : "");
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("p", p);
        row.put("killer", killer);
        row.put("why", why);
        row.put("chat", cause != null && cause.fromChat());
        row.put("total", total);
        row.put("window", window);
        row.put("need", need);
        row.put("block", evilBlock);
        write("death", line, row);
    }

    public static void donation(String donor, double amount, String currency, String streamer,
                                String effect, boolean wheel) {
        if (!enabled()) return;
        donations++;
        String who = clean(donor);
        String p = clean(streamer);
        String fx = clean(effect);
        String amt = amount == Math.rint(amount)
                ? String.valueOf((long) amount)
                : String.format(Locale.US, "%.2f", amount);
        String cur = blank(currency) ? "RUB" : currency;
        String line = "donat " + who + "  " + amt + " " + cur + " → " + p
                + (fx.isBlank() ? "" : "  " + fx)
                + (wheel ? "  колесо" : "");
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("u", who);
        row.put("amount", amount);
        row.put("cur", cur);
        row.put("p", p);
        row.put("c", fx);
        row.put("wheel", wheel);
        write("donat", line, row);
    }

    public static void note(String text) {
        if (!enabled() || blank(text)) return;
        write("info", "info  " + strip(text), Map.of("msg", strip(text)));
    }

    private static void command(String viewer, String id, String ru, String streamer,
                                String kind, int cdSec, double cdMult, boolean ok, String extra) {
        if (!enabled()) return;
        bump(viewer, ok, kind);
        if (ok) commandsOk++;
        String who = clean(viewer);
        String name = clean(blank(ru) ? id : ru);
        String p = clean(streamer);
        String k = blank(kind) ? "пранк" : kind;
        String line = "cmd   " + who + " → !" + name
                + (p.isBlank() ? "" : "  на " + p)
                + "  " + k
                + (cdSec > 0 ? "  КД " + cdSec + "с" : "")
                + (cdMult > 0 && Math.abs(cdMult - 1.0) > 0.04
                ? "  ×" + String.format(Locale.US, "%.1f", cdMult) : "")
                + (blank(extra) ? "" : "  " + extra);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("u", who);
        row.put("id", clean(id));
        row.put("c", name);
        row.put("p", p);
        row.put("kind", k);
        row.put("cd", cdSec);
        row.put("x", Math.round(cdMult * 10.0) / 10.0);
        write("cmd", line, row);
    }

    private static String kindLabel(ChatGameCommand command) {
        return switch (EvilCommands.harm(command)) {
            case GOOD -> "добро";
            case DAMAGE -> "зло";
            case MOD -> "модер";
            case PRANK -> "пранк";
        };
    }

    private static String reasonRu(String id) {
        if (id == null) return "отказ";
        return switch (id) {
            case CommandDeny.SPAM -> "антиспам";
            case CommandDeny.COOLDOWN -> "КД";
            case CommandDeny.RATE -> "лимит";
            case CommandDeny.DISABLED -> "выкл";
            case CommandDeny.SHIELD -> "щит";
            case CommandDeny.HOLD -> "держись";
            case CommandDeny.EVIL_BLOCK -> "блок зла";
            case CommandDeny.EVIL_PACE -> "пауза урона";
            case CommandDeny.HARDCORE -> "хардкор";
            case CommandDeny.WHITELIST -> "вайтлист";
            case CommandDeny.OFFLINE -> "оффлайн";
            case CommandDeny.PAUSE -> "пауза";
            default -> id;
        };
    }

    private static void bump(String viewer, boolean ok, String kind) {
        if (blank(viewer) || isBot(viewer)) return;
        String key = viewer.toLowerCase(Locale.ROOT);
        ViewerRow row = VIEWERS.computeIfAbsent(key, k -> new ViewerRow(clean(viewer)));
        if (ok) {
            row.ok++;
            if ("добро".equals(kind)) row.good++;
            else if ("зло".equals(kind)) row.evil++;
            else row.prank++;
        } else {
            row.deny++;
        }
    }

    private static boolean isBot(String viewer) {
        String k = viewer.toLowerCase(Locale.ROOT);
        return k.equals("nightbot") || k.equals("streamelements") || k.equals("moobot")
                || k.equals("fossabot") || k.equals("wizebot");
    }

    private static boolean enabled() {
        var stream = ConfigManager.get().stream;
        return stream == null || stream.sessionLogEnabled;
    }

    private static void write(String kind, String human, Map<String, Object> payload) {
        synchronized (LOCK) {
            ensureOpen();
            line(kind, human);
            json(kind, payload);
        }
    }

    private static void line(String kind, String human) {
        if (textOut == null) return;
        try {
            textOut.write(CLOCK.format(LocalDateTime.now()));
            textOut.write("  ");
            textOut.write(human);
            textOut.newLine();
            textOut.flush();
        } catch (IOException e) {
            TwitchCraftMod.LOGGER.debug("StreamChronicle text write failed", e);
        }
        if ("death".equals(kind) || "donat".equals(kind)) {
            TwitchCraftMod.LOGGER.info("TwitchCraft {}", human);
        } else if ("cmd".equals(kind)) {
            TwitchCraftMod.LOGGER.info("TwitchCraft {}", human);
        }
    }

    private static void json(String kind, Map<String, Object> payload) {
        if (jsonOut == null) return;
        try {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("t", STAMP.format(LocalDateTime.now()));
            row.put("k", kind);
            if (payload != null) row.putAll(payload);
            jsonOut.write(GSON.toJson(row));
            jsonOut.newLine();
            jsonOut.flush();
        } catch (IOException e) {
            TwitchCraftMod.LOGGER.debug("StreamChronicle json write failed", e);
        }
    }

    private static void ensureOpen() {
        String today = LocalDate.now().toString();
        if (textOut != null && jsonOut != null && today.equals(day)) return;
        closeWriters();
        day = today;
        Path folder = logsDir();
        if (folder == null) return;
        try {
            Files.createDirectories(folder);
            textOut = Files.newBufferedWriter(folder.resolve(today + ".log"), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            jsonOut = Files.newBufferedWriter(folder.resolve(today + ".jsonl"), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            TwitchCraftMod.LOGGER.warn("TwitchCraft: не удалось открыть logs/twitchcraft", e);
        }
    }

    private static void closeWriters() {
        try {
            if (textOut != null) textOut.close();
        } catch (IOException ignored) {
        }
        try {
            if (jsonOut != null) jsonOut.close();
        } catch (IOException ignored) {
        }
        textOut = null;
        jsonOut = null;
    }

    private static void writeSummaryUnlocked(boolean closing) {
        Path folder = logsDir();
        if (folder == null) return;
        List<ViewerRow> top = new ArrayList<>(VIEWERS.values());
        top.sort(Comparator.comparingInt((ViewerRow r) -> r.ok + r.deny).reversed());
        StringBuilder sb = new StringBuilder();
        sb.append("=== TwitchCraft ").append(LocalDate.now()).append(closing ? " (конец)" : "").append(" ===\n");
        sb.append("команд: ").append(commandsOk)
                .append("  отказов: ").append(commandsDeny)
                .append("  неизвестных: ").append(unknown)
                .append("  смертей: ").append(deaths)
                .append("  донатов: ").append(donations)
                .append("  зрителей: ").append(VIEWERS.size()).append('\n');
        int n = 0;
        for (ViewerRow r : top) {
            if (n >= 40) break;
            n++;
            sb.append(String.format(Locale.ROOT, "%2d. %-16s  %d ок / %d отказ  зло %d  добро %d  пранк %d%n",
                    n, r.display, r.ok, r.deny, r.evil, r.good, r.prank));
        }
        sb.append("файлы: ").append(day).append(".log  +  ").append(day).append(".jsonl\n");
        try {
            Files.createDirectories(folder);
            Files.writeString(folder.resolve("viewers-latest.txt"), sb.toString(), StandardCharsets.UTF_8);
            if (textOut != null && closing) {
                textOut.write("--------\n");
                textOut.write(sb.toString());
                textOut.flush();
            }
        } catch (IOException e) {
            TwitchCraftMod.LOGGER.debug("StreamChronicle summary failed", e);
        }
    }

    private static Path logsDir() {
        if (dir != null) return dir;
        try {
            dir = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("twitchcraft");
            return dir;
        } catch (Exception e) {
            return null;
        }
    }

    private static String clean(String s) {
        if (s == null) return "";
        String t = strip(s).trim();
        if (t.length() > 32) t = t.substring(0, 32);
        return t;
    }

    private static String strip(String s) {
        return s.replaceAll("§.", "").replace('\n', ' ').replace('\r', ' ');
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static final class ViewerRow {
        final String display;
        int ok;
        int deny;
        int good;
        int evil;
        int prank;

        ViewerRow(String display) {
            this.display = display;
        }
    }
}
