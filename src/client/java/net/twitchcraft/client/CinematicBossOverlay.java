package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Кинематографичный intro для редких команд (визер / дракон / фантом). */
public final class CinematicBossOverlay {
    private static String title = "";
    private static String subtitle = "";
    private static int accent = 0xFF_AA_44_FF;
    private static long startMs;
    private static long endMs;
    private static boolean active;

    private CinematicBossOverlay() {}

    public static void trigger(String commandId, String viewer) {
        String id = commandId != null ? commandId.toLowerCase() : "";
        String user = viewer != null && !viewer.isBlank() ? viewer : "чат";
        switch (id) {
            case "wither" -> start(
                    "§0§l☠ ВИЗЕР",
                    "§7" + user + " §8призвал §cсмерть",
                    0xFF_30_30_30, 3200);
            case "dragon" -> start(
                    "§5§l🐉 ДРАКОН КРАЯ",
                    "§d" + user + " §8открыл §5портал хаоса",
                    0xFF_C0_40_FF, 3600);
            case "phantom" -> start(
                    "§5§l👻 ПРИЗРАК ЧАТА",
                    "§d" + user + " §8натравил фантома чата",
                    0xFF_88_44_CC, 3200);
            default -> {
            }
        }
    }

    public static boolean isRare(String commandId) {
        if (commandId == null) return false;
        return switch (commandId.toLowerCase()) {
            case "wither", "dragon", "phantom" -> true;
            default -> false;
        };
    }

    private static void start(String t, String sub, int color, int durationMs) {
        title = t;
        subtitle = sub;
        accent = color;
        startMs = System.currentTimeMillis();
        endMs = startMs + durationMs;
        active = true;
        CommandScreenFx.triggerEvil(3, durationMs);
        HudUiSounds.death();
    }

    public static void render(GuiGraphics g, float delta) {
        if (!active) return;
        long now = System.currentTimeMillis();
        if (now > endMs) {
            active = false;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc)) return;

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        long age = now - startMs;
        long left = endMs - now;
        float fadeIn = HudAnim.easeOutCubic(Mth.clamp(age / 420f, 0f, 1f));
        float fadeOut = Mth.clamp(left / 500f, 0f, 1f);
        float a = fadeIn * fadeOut;
        if (a < 0.02f) return;

        int rgb = accent & 0xFFFFFF;
        // затемнение + vignette
        int dim = (int) (200 * a);
        g.fill(0, 0, w, h, (dim << 24) | 0x05_02_08);
        int band = HudLayout.s(70);
        TwitchUi.fillGradientV(g, 0, 0, w, band, ((int) (220 * a) << 24) | rgb, 0x00_00_00_00);
        TwitchUi.fillGradientV(g, 0, h - band, w, h, 0x00_00_00_00, ((int) (220 * a) << 24) | rgb);

        // боковые полосы
        int side = HudLayout.s(18);
        TwitchUi.fillGradientH(g, 0, 0, side, h, ((int) (160 * a) << 24) | rgb, 0x00_00_00_00);
        TwitchUi.fillGradientH(g, w - side, 0, w, h, 0x00_00_00_00, ((int) (160 * a) << 24) | rgb);

        // центральная плашка
        int boxW = Math.min(HudLayout.s(340), w - 40);
        int boxH = HudLayout.s(54);
        int bx = (w - boxW) / 2;
        int by = h / 2 - HudLayout.s(40);
        int grow = (int) ((1f - fadeIn) * 20);
        bx -= grow / 2;
        boxW += grow;

        TwitchUi.roundedPanel(g, bx - 2, by - 2, bx + boxW + 2, by + boxH + 2,
                ((int) (180 * a) << 24) | rgb);
        TwitchUi.fillGradientV(g, bx, by, bx + boxW, by + boxH,
                ((int) (230 * a) << 24) | 0x10_0C_14,
                ((int) (230 * a) << 24) | 0x08_06_0C);
        TwitchUi.fillGradientH(g, bx + 4, by + 2, bx + boxW - 4, by + 5,
                ((int) (220 * a) << 24) | rgb, ((int) (40 * a) << 24) | 0xFF_FF_FF);

        int titleY = by + HudLayout.s(12);
        int subY = by + HudLayout.s(30);
        int tw = mc.font.width(strip(title));
        int sx = bx + (boxW - tw) / 2;
        // лёгкий «кино»-сдвиг
        int shake = age < 600 ? (int) (Math.sin(age / 40.0) * 2 * (1f - age / 600f)) : 0;
        g.drawString(mc.font, title, sx + shake, titleY, ((int) (255 * a) << 24) | 0xFFFFFF, false);
        int sw = mc.font.width(strip(subtitle));
        g.drawString(mc.font, subtitle, bx + (boxW - sw) / 2, subY,
                ((int) (220 * a) << 24) | TwitchUi.TEXT_DIM, false);

        // нижний тикер
        String tick = "§8TWITCHCRAFT  ·  РЕДКОЕ СОБЫТИЕ";
        g.drawString(mc.font, tick, (w - mc.font.width(strip(tick))) / 2,
                by + boxH + HudLayout.s(10), ((int) (160 * a) << 24) | rgb, false);
    }

    private static String strip(String s) {
        return s == null ? "" : s.replaceAll("§.", "");
    }
}
