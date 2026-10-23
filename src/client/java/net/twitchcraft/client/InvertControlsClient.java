package net.twitchcraft.client;

public final class InvertControlsClient {
    private static long untilMs;

    private InvertControlsClient() {}

    public static void start(int ticks) {
        untilMs = System.currentTimeMillis() + Math.max(1, ticks) * 50L;
    }

    public static boolean isActive() {
        return System.currentTimeMillis() < untilMs;
    }
}
