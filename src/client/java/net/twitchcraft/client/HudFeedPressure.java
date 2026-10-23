package net.twitchcraft.client;

/** Тихий чат — плашка читается. Флуд — быстрее схлопывается, экран не забивается. */
final class HudFeedPressure {
    static final int HARD_CAP = 4;

    private HudFeedPressure() {}

    static int level(int stack, int in2s, int in5s) {
        if (in2s >= 3 || in5s >= 5 || stack >= 4) return 2;
        if (in5s >= 2 || stack >= 3) return 1;
        return 0;
    }

    static long ttlMs(int level, int stackIndex, long quiet, long busy, long flood) {
        long base = level >= 2 ? flood : level >= 1 ? busy : quiet;
        return Math.max(1600L, base - Math.max(0, stackIndex) * 750L);
    }

    static int cap(int configured, int level, int quietCap, int busyCap, int floodCap) {
        int want = level >= 2 ? floodCap : level >= 1 ? busyCap : quietCap;
        return Math.max(1, Math.min(Math.min(configured, HARD_CAP), want));
    }
}
