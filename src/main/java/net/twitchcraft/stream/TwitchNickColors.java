package net.twitchcraft.stream;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Цвет ника Twitch: тег IRC {@code color}, иначе палитра как у чата Twitch.
 * Helix /users цвет не отдаёт — его несёт IRC.
 */
public final class TwitchNickColors {
    /** Официальные дефолты Twitch, если зритель не ставил свой цвет. */
    private static final int[] TWITCH_DEFAULTS = {
            0xFF_00_00, 0x00_00_FF, 0x00_FF_00, 0xB2_22_22, 0xFF_7F_50,
            0x9A_CD_32, 0xFF_45_00, 0x2E_8B_57, 0xDA_A5_20, 0xD2_69_1E,
            0x5F_9E_A0, 0x1E_90_FF, 0xFF_69_B4, 0x8A_2B_E2, 0x00_FF_7F
    };

    private static final ConcurrentHashMap<String, Integer> CACHE = new ConcurrentHashMap<>();

    private TwitchNickColors() {}

    public static void rememberHex(String nick, String hex) {
        int rgb = parseHex(hex);
        if (rgb == 0 || nick == null || nick.isBlank()) return;
        CACHE.put(key(nick), rgb);
    }

    public static int get(String nick) {
        if (nick == null || nick.isBlank()) return 0xF0_F0_F5;
        Integer hit = CACHE.get(key(nick));
        if (hit != null && hit != 0) return hit;
        return fallback(nick);
    }

    public static int fallback(String nick) {
        if (nick == null || nick.isBlank()) return 0xF0_F0_F5;
        int idx = Math.floorMod(key(nick).hashCode(), TWITCH_DEFAULTS.length);
        return TWITCH_DEFAULTS[idx];
    }

    public static int parseHex(String hex) {
        if (hex == null || hex.isBlank()) return 0;
        String s = hex.trim();
        if (s.charAt(0) == '#') s = s.substring(1);
        if (s.length() != 6) return 0;
        try {
            int v = Integer.parseInt(s, 16);
            return v == 0 ? 0x00_00_01 : v;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String key(String nick) {
        return nick.trim().toLowerCase(Locale.ROOT);
    }
}
