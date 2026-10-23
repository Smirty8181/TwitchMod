package net.twitchcraft.client;

/** Пресеты цветов для конструктора HUD. */
public final class HudColors {
    public static final int THEME = 0;
    public static final int[] PRESETS = {
            THEME,
            0xFF_FF_7A_A8,
            0xFF_7A_D4_FF,
            0xFF_4A_E8_A0,
            0xFF_FF_5A_6E,
            0xFF_FF_C4_8A,
            0xFF_B4_7A_FF,
            0xFF_FF_FF_FF,
            0xFF_FF_9F_43,
            0xFF_00_F0_D0
    };

    public static final String[] PRESET_NAMES = {
            "Тема", "Сакура", "Циан", "Зелёный", "Красный", "Персик",
            "Лаванда", "Белый", "Оранж", "Неон"
    };

    private HudColors() {}

    public static int cycle(int current) {
        int idx = 0;
        for (int i = 0; i < PRESETS.length; i++) {
            if (PRESETS[i] == current) {
                idx = (i + 1) % PRESETS.length;
                break;
            }
        }
        return PRESETS[idx];
    }

    public static String label(int color) {
        for (int i = 0; i < PRESETS.length; i++) {
            if (PRESETS[i] == color) return PRESET_NAMES[i];
        }
        return color == 0 ? "Тема" : "§#" + Integer.toHexString(color & 0xFFFFFF);
    }

    public static final String[] ANCHORS = {
            "TOP_LEFT", "TOP_CENTER", "TOP_RIGHT",
            "CENTER_LEFT", "CENTER", "CENTER_RIGHT",
            "BOTTOM_LEFT", "BOTTOM_CENTER", "BOTTOM_RIGHT"
    };

    public static final String[] ANCHOR_LABELS = {
            "↖ Слева сверху", "↑ Сверху", "↗ Справа сверху",
            "← Слева", "◎ Центр", "→ Справа",
            "↙ Слева снизу", "↓ Снизу", "↘ Справа снизу"
    };

    public static String cycleAnchor(String current) {
        for (int i = 0; i < ANCHORS.length; i++) {
            if (ANCHORS[i].equalsIgnoreCase(current)) {
                return ANCHORS[(i + 1) % ANCHORS.length];
            }
        }
        return ANCHORS[0];
    }

    public static String anchorLabel(String anchor) {
        for (int i = 0; i < ANCHORS.length; i++) {
            if (ANCHORS[i].equalsIgnoreCase(anchor)) return ANCHOR_LABELS[i];
        }
        return ANCHOR_LABELS[2];
    }
}
