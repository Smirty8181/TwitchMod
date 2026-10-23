package net.twitchcraft.client;

/**
 * Сетка HUD: две колонки, общие отступы.
 * Справа — статус. Слева — лента команд.
 */
public final class HudRails {
    private static int sidebarW;

    private HudRails() {}

    public static int margin() {
        return HudLayout.s(6);
    }

    public static int gap() {
        return HudLayout.s(5);
    }

    public static int hotbar() {
        return HudLayout.s(70);
    }

    public static int rightColW() {
        return Math.max(HudLayout.s(168), sidebarW);
    }

    public static int activityW() {
        return HudLayout.s(196);
    }

    public static int left() {
        return margin();
    }

    public static int rightX(int screenW, int boxW) {
        return screenW - boxW - margin();
    }

    public static void reportSidebarWidth(int w) {
        sidebarW = Math.max(0, w);
    }
}
