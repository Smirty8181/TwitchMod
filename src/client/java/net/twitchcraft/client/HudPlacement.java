package net.twitchcraft.client;

import net.minecraft.util.Mth;

import java.util.Locale;

/** Расчёт позиции HUD-элемента по якорю и отступам. */
public final class HudPlacement {
    public record Rect(int x, int y) {}

    private HudPlacement() {}

    public static Rect place(String anchor, int offsetX, int offsetY,
                             int screenW, int screenH, int contentW, int contentH,
                             float elementScale) {
        int ox = HudLayout.s(offsetX, elementScale);
        int oy = HudLayout.s(offsetY, elementScale);
        String a = anchor == null ? "TOP_RIGHT" : anchor.toUpperCase(Locale.ROOT);

        int x = switch (a) {
            case "TOP_LEFT", "CENTER_LEFT" -> ox;
            case "TOP_CENTER", "CENTER", "BOTTOM_CENTER" -> (screenW - contentW) / 2 + ox;
            case "TOP_RIGHT", "CENTER_RIGHT" -> screenW - contentW - ox;
            case "BOTTOM_LEFT" -> ox;
            case "BOTTOM_RIGHT" -> screenW - contentW - ox;
            default -> screenW - contentW - ox;
        };

        int y = switch (a) {
            case "TOP_LEFT", "TOP_CENTER", "TOP_RIGHT" -> oy;
            case "BOTTOM_LEFT", "BOTTOM_CENTER", "BOTTOM_RIGHT" -> screenH - contentH - oy;
            case "CENTER_LEFT", "CENTER", "CENTER_RIGHT" -> (screenH - contentH) / 2 + oy;
            default -> oy;
        };

        return new Rect(Mth.clamp(x, 0, Math.max(0, screenW - contentW)),
                Mth.clamp(y, 0, Math.max(0, screenH - contentH)));
    }

    public static boolean isRightStack(String anchor) {
        if (anchor == null) return false;
        String a = anchor.toUpperCase(Locale.ROOT);
        return a.endsWith("_RIGHT");
    }

    public static boolean isLeftStack(String anchor) {
        if (anchor == null) return false;
        String a = anchor.toUpperCase(Locale.ROOT);
        return a.endsWith("_LEFT");
    }

    public static boolean isBottomStack(String anchor) {
        if (anchor == null) return true;
        return anchor.toUpperCase(Locale.ROOT).startsWith("BOTTOM");
    }

    public static boolean isTopStack(String anchor) {
        if (anchor == null) return false;
        return anchor.toUpperCase(Locale.ROOT).startsWith("TOP");
    }
}
