package net.twitchcraft.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Недавние ники из Twitch-чата (любые сообщения, не только команды). */
public final class ChatViewerRegistry {
    private static final int MAX = 64;
    private static final Set<String> ORDERED = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final ConcurrentHashMap<String, String> DISPLAY = new ConcurrentHashMap<>();

    private ChatViewerRegistry() {}

    public static void seen(String username) {
        if (username == null || username.isBlank()) return;
        String display = username.trim();
        if (display.length() > 16) display = display.substring(0, 16);
        String key = display.toLowerCase(Locale.ROOT);
        if (key.equals("nightbot") || key.equals("streamelements") || key.equals("moobot")
                || key.equals("fossabot") || key.equals("streamelements")) {
            return;
        }
        synchronized (ORDERED) {
            ORDERED.remove(key);
            ORDERED.add(key);
            while (ORDERED.size() > MAX) {
                String first = ORDERED.iterator().next();
                ORDERED.remove(first);
                DISPLAY.remove(first);
            }
        }
        DISPLAY.put(key, display);
    }

    public static List<String> recentDisplayNames() {
        List<String> out = new ArrayList<>();
        synchronized (ORDERED) {
            for (String key : ORDERED) {
                out.add(DISPLAY.getOrDefault(key, key));
            }
        }
        return out;
    }

    /** Случайные уникальные ники; если мало — дополняет fallback. */
    public static List<String> randomViewers(int count, String fallbackSeed) {
        List<String> pool = new ArrayList<>();
        synchronized (ORDERED) {
            for (String key : ORDERED) {
                String d = DISPLAY.getOrDefault(key, key);
                pool.add(d);
            }
        }
        Collections.shuffle(pool);
        List<String> out = new ArrayList<>();
        for (String n : pool) {
            if (out.size() >= count) break;
            out.add(n);
        }
        int i = 0;
        while (out.size() < count) {
            String seed = fallbackSeed != null && !fallbackSeed.isBlank() ? fallbackSeed : "viewer";
            String fake = seed.length() > 10 ? seed.substring(0, 10) : seed;
            out.add(fake + (i + 1));
            i++;
        }
        return out;
    }

    public static int size() {
        synchronized (ORDERED) {
            return ORDERED.size();
        }
    }

    /** Снимок ников, которые уже писали — чтобы рейд не красил старый чат в «рейдеров». */
    public static Set<String> snapshotKeys() {
        synchronized (ORDERED) {
            return new java.util.HashSet<>(ORDERED);
        }
    }
}
