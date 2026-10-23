package net.twitchcraft.twitch;

/** Текст статуса Twitch IRC для HUD и настроек. */
public final class TwitchStatusFormatter {
    private TwitchStatusFormatter() {}

    public static String sidebarLine(boolean connected, String channel, String error) {
        if (connected && channel != null && !channel.isBlank()) {
            return "§a● §fTwitch §7#" + truncate(channel, 14);
        }
        return offlineLine(error, 18);
    }

    public static String settingsLine(boolean connected, String channel, String error) {
        if (connected && channel != null && !channel.isBlank()) {
            return "§a● §lПодключено §7к §f#" + truncate(channel, 20);
        }
        return offlineLine(error, 36);
    }

    /** Человекочитаемое описание сбоя подключения (для логов и UI). */
    public static String describeFailure(Throwable e) {
        if (e == null) return "неизвестная ошибка";
        String msg = e.getMessage();
        String raw = msg != null && !msg.isBlank() ? msg : e.getClass().getSimpleName();
        return sanitize(raw);
    }

    private static String offlineLine(String error, int maxErr) {
        if (error != null && error.toLowerCase().contains("не настроен")) {
            return "§7○ §8Twitch §7— укажи канал и OAuth (вкладка Twitch)";
        }
        if (error != null && !error.isBlank()) {
            return "§c○ §8Twitch §7— " + truncate(sanitize(error), maxErr);
        }
        return "§c○ §8Twitch §7— нет связи · §f/twitchcraft connect";
    }

    private static String sanitize(String error) {
        if (error == null || error.isBlank()) return error;
        String lower = error.toLowerCase();
        if (lower.contains("kotlin") || lower.contains("noclassdeffounderror")
                || lower.contains("oauth2credential") || lower.contains("classnotfoundexception")) {
            return "библиотека Twitch не загружена — переустанови JAR мода";
        }
        if (error.contains("/") && (error.contains("enum") || error.contains("class"))) {
            return "ошибка Twitch IRC — /twitchcraft connect";
        }
        return error;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
