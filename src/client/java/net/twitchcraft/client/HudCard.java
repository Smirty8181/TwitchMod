package net.twitchcraft.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** HUD-карточки — стекло, dual-accent, мягкий glow. */
public final class HudCard {
    public static final int ACCENT_W = 3;

    private HudCard() {}

    public static void draw(GuiGraphics g, int x, int y, int w, int h, int accent, float alpha, float pulse) {
        drawPremium(g, x, y, w, h, accent, alpha, pulse, false, 0);
    }

    public static void drawPremium(GuiGraphics g, int x, int y, int w, int h, int accent,
                                   float alpha, float pulse, boolean highlight, long nowMs) {
        int a = (int) (alpha * pulse * 255);
        if (a < 6) return;

        int rgb = accent & 0xFFFFFF;
        int lit = TwitchUi.blendRgb(rgb, 0xFFFFFF, 0.42f) & 0xFFFFFF;
        int dim = TwitchUi.blendRgb(rgb, 0x000000, 0.48f) & 0xFFFFFF;
        int alt = TwitchUi.ACCENT_ALT & 0xFFFFFF;
        int ar = (rgb >> 16) & 0xFF;
        int ag = (rgb >> 8) & 0xFF;
        int ab = rgb & 0xFF;

        TwitchUi.panelShadow(g, x, y, x + w, y + h, highlight ? 4 : 3);

        int top = argb(a * 90 / 100, 0x13, 0x10, 0x1A);
        int bot = argb(a * 97 / 100, 0x07, 0x06, 0x0C);
        TwitchUi.fillGradientV(g, x, y, x + w, y + h, top, bot);
        TwitchUi.roundedPanel(g, x, y, x + w, y + h, argb(a / 26, 0xFF, 0xFF, 0xFF));

        int washW = Math.min(w / 3, 52);
        TwitchUi.fillGradientH(g, x, y + 1, x + washW, y + h - 1,
                argb(a / 9, ar, ag, ab), 0x00_00_00_00);

        int sheenH = Math.max(7, h / 4);
        TwitchUi.fillGradientV(g, x + ACCENT_W + 1, y + 1, x + w - 2, y + sheenH,
                argb(a / 6, 0xFF, 0xFF, 0xFF), 0x00_00_00_00);

        int accentA = highlight ? a : (a * 88 / 100);
        TwitchUi.fillGradientV(g, x, y + 1, x + ACCENT_W, y + h - 1,
                (accentA << 24) | lit,
                (accentA / 3 << 24) | dim);
        g.fill(x + 1, y + 2, x + 2, y + h - 2, argb(a / 4, 0xFF, 0xFF, 0xFF));

        TwitchUi.fillGradientH(g, x + ACCENT_W, y, x + w - 1, y + 1,
                (accentA * 88 / 100 << 24) | rgb,
                (accentA / 6 << 24) | alt);
        g.fill(x + ACCENT_W + 1, y + 1, x + w - 2, y + 2, argb(a / 12, 0xFF, 0xFF, 0xFF));

        TwitchUi.fillGradientH(g, x + ACCENT_W, y + h - 1, x + w - 1, y + h,
                argb(a / 5, 0x22, 0x1C, 0x2A),
                (a / 8 << 24) | alt);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, (a / 10 << 24) | alt);

        if (highlight) {
            long t = nowMs <= 0 ? System.currentTimeMillis() : nowMs;
            float glow = 0.58f + 0.42f * (float) Math.sin(t / 210.0);
            int ga = Mth.clamp((int) (a * 0.24f * glow), 8, 92);
            int glowC = (ga << 24) | lit;
            g.fill(x - 1, y - 1, x + w + 1, y, glowC);
            g.fill(x - 1, y + h, x + w + 1, y + h + 1, glowC);
            g.fill(x - 1, y, x, y + h, glowC);
            g.fill(x + w, y, x + w + 1, y + h, glowC);

            TwitchUi.fillGradientV(g, x + ACCENT_W + 1, y + 1, x + w - 2, y + 9,
                    argb((int) (a * 0.14f * glow), ar, ag, ab), 0x00_00_00_00);
            TwitchUi.fillGradientH(g, x + ACCENT_W, y + 1, x + w / 2, y + 2,
                    argb((int) (a * 0.35f * glow), 0xFF, 0xFF, 0xFF), 0x00_00_00_00);
        }

        if (compact(w, h)) {
            cornerTicks(g, x, y, w, h, (accentA << 24) | lit, Math.min(8, Math.max(4, w / 16)));
        }
    }

    public static void drawIconWell(GuiGraphics g, int x, int y, int size, int accent, float alpha) {
        int a = (int) (alpha * 255);
        if (a < 6) return;
        int rgb = accent & 0xFFFFFF;
        int lit = TwitchUi.blendRgb(rgb, 0xFFFFFF, 0.35f) & 0xFFFFFF;
        g.fill(x + 1, y + 2, x + size + 1, y + size + 2, (a / 6 << 24));
        TwitchUi.fillGradientV(g, x, y, x + size, y + size,
                argb(a * 72 / 100, 0x18, 0x14, 0x22),
                argb(a * 88 / 100, 0x0A, 0x08, 0x12));
        TwitchUi.roundedPanel(g, x, y, x + size, y + size, argb(a / 16, 0xFF, 0xFF, 0xFF));
        g.fill(x, y, x + size, y + 1, (a << 24) | lit);
        g.fill(x, y, x + 1, y + size, (a * 75 / 100 << 24) | rgb);
        TwitchUi.fillGradientV(g, x + 1, y + 1, x + size - 1, y + size / 3,
                argb(a / 5, 0xFF, 0xFF, 0xFF), 0x00_00_00_00);
        g.fill(x + 1, y + size - 1, x + size - 1, y + size, argb(a / 8, 0x00, 0x00, 0x00));
    }

    public static void drawCommandStrip(GuiGraphics g, int x, int y, int w, int h, int accent, float alpha) {
        int a = (int) (alpha * 255);
        if (a < 4 || w < 4 || h < 2) return;
        int rgb = accent & 0xFFFFFF;
        int lit = TwitchUi.blendRgb(rgb, 0xFFFFFF, 0.28f) & 0xFFFFFF;
        int ar = (rgb >> 16) & 0xFF;
        int ag = (rgb >> 8) & 0xFF;
        int ab = rgb & 0xFF;
        TwitchUi.fillGradientV(g, x, y, x + w, y + h,
                argb(a * 22 / 100, ar, ag, ab),
                argb(a * 10 / 100, 0x0C, 0x0A, 0x14));
        TwitchUi.roundedPanel(g, x, y, x + w, y + h, argb(a / 18, 0xFF, 0xFF, 0xFF));
        g.fill(x, y + 1, x + 2, y + h - 1, (a * 80 / 100 << 24) | rgb);
        TwitchUi.fillGradientH(g, x + 2, y, x + w - 1, y + 1,
                (a / 2 << 24) | lit, 0x00_00_00_00);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, argb(a / 10, 0x00, 0x00, 0x00));
    }

    public static void drawBadge(GuiGraphics g, Font font, int x, int y, String text, int accent, float alpha) {
        int a = (int) (alpha * 255);
        if (a < 8 || text == null || text.isBlank()) return;
        int rgb = accent & 0xFFFFFF;
        int lit = TwitchUi.blendRgb(rgb, 0xFFFFFF, 0.48f) & 0xFFFFFF;
        int tw = font.width(text);
        int padX = HudLayout.s(5);
        int bw = tw + padX * 2;
        int bh = HudLayout.s(11);
        TwitchUi.fillGradientV(g, x, y, x + bw, y + bh,
                argb(a * 78 / 100, 0x14, 0x12, 0x1C),
                argb(a * 88 / 100, 0x0A, 0x08, 0x12));
        TwitchUi.roundedPanel(g, x, y, x + bw, y + bh, argb(a / 16, 0xFF, 0xFF, 0xFF));
        g.fill(x, y + 1, x + 2, y + bh - 1, (a << 24) | rgb);
        TwitchUi.fillGradientH(g, x + 2, y, x + bw - 1, y + 1,
                (a / 2 << 24) | lit, 0x00_00_00_00);
        g.fill(x + 2, y + bh - 1, x + bw - 2, y + bh, (a << 24) | rgb);
        g.drawString(font, text, x + padX, y + HudLayout.s(2),
                (a << 24) | lit, false);
    }

    public static void drawCooldownRing(GuiGraphics g, int cx, int cy, int radius,
                                        int thickness, float ratio, int accent, float alpha) {
        int a = (int) (alpha * 255);
        if (a < 8 || radius < 4) return;
        float r = Mth.clamp(ratio, 0f, 1f);
        int thick = Math.max(2, thickness);
        int rgb = accent & 0xFFFFFF;
        int lit = TwitchUi.blendRgb(rgb, 0xFFFFFF, 0.45f) & 0xFFFFFF;
        int dim = TwitchUi.blendRgb(rgb, 0x000000, 0.35f) & 0xFFFFFF;
        int steps = Math.max(36, radius * 5);
        int remain = Math.max(0, Math.round(steps * r));
        for (int rad = radius - thick + 1; rad <= radius; rad++) {
            if (rad < 3) continue;
            for (int i = 0; i < steps; i++) {
                double ang = -Math.PI / 2.0 + (Math.PI * 2.0 * i) / steps;
                int px = cx + (int) Math.round(Math.cos(ang) * rad);
                int py = cy + (int) Math.round(Math.sin(ang) * rad);
                boolean on = i < remain;
                int col = on
                        ? ((a << 24) | (rad == radius ? lit : rgb))
                        : ((a * 55 / 100 << 24) | dim);
                g.fill(px - 1, py - 1, px + 1, py + 1, col);
            }
        }
        if (remain > 0 && r < 0.98f) {
            double ang = -Math.PI / 2.0 + (Math.PI * 2.0 * remain) / steps;
            int hx = cx + (int) Math.round(Math.cos(ang) * radius);
            int hy = cy + (int) Math.round(Math.sin(ang) * radius);
            g.fill(hx - 1, hy - 1, hx + 2, hy + 2, (a << 24) | 0xFF_FF_FF);
        }
    }

    public static void drawProgressBar(GuiGraphics g, int x, int y, int w, int h,
                                       float ratio, int accent, float alpha) {
        int a = (int) (alpha * 255);
        if (a < 4 || w < 2) return;
        int rgb = accent & 0xFFFFFF;
        float r = Mth.clamp(ratio, 0f, 1f);
        int lit = TwitchUi.blendRgb(rgb, 0xFFFFFF, 0.28f);
        int hot = TwitchUi.blendRgb(rgb, 0xFFFFFF, 0.58f);

        g.fill(x, y, x + w, y + h, argb(a / 5, 0x14, 0x12, 0x1A));
        TwitchUi.roundedPanel(g, x, y, x + w, y + h, argb(a / 14, 0xFF, 0xFF, 0xFF));

        int fw = Math.max(1, (int) (w * r));
        if (fw > 0) {
            TwitchUi.fillGradientH(g, x, y, x + fw, y + h,
                    (a << 24) | rgb, (a << 24) | (lit & 0xFFFFFF));
            if (h >= 2) {
                g.fill(x, y, x + fw, y + 1, (a * 55 / 100 << 24) | (hot & 0xFFFFFF));
            }
            if (fw >= 3) {
                g.fill(x + fw - 2, y, x + fw, y + h, (a << 24) | (hot & 0xFFFFFF));
            }
        }
    }

    /** Шкала кармы −100…+100: красное влево, зелёное вправо, центр — ноль. */
    public static void drawKarmaBar(GuiGraphics g, int x, int y, int w, int h, float karmaNorm, int alpha) {
        int a = Mth.clamp(alpha, 0, 255);
        float n = Mth.clamp(karmaNorm, 0f, 1f);
        g.fill(x, y, x + w, y + h, argb(a / 5, 0x14, 0x12, 0x1A));
        TwitchUi.roundedPanel(g, x, y, x + w, y + h, argb(a / 14, 0xFF, 0xFF, 0xFF));
        int mid = x + w / 2;
        g.fill(mid, y - 1, mid + 1, y + h + 1, argb(a * 2 / 3, 0xF0, 0xF0, 0xF5));
        if (n >= 0.5f) {
            int fw = Math.max(1, (int) ((n - 0.5f) * 2f * (w / 2f)));
            TwitchUi.fillGradientH(g, mid, y, mid + fw, y + h,
                    (a << 24) | 0x2E_CC_71, (a << 24) | 0xA8_FF_C4);
            if (h >= 2) g.fill(mid, y, mid + fw, y + 1, argb(a / 2, 0xFF, 0xFF, 0xFF));
        } else {
            int fw = Math.max(1, (int) ((0.5f - n) * 2f * (w / 2f)));
            TwitchUi.fillGradientH(g, mid - fw, y, mid, y + h,
                    (a << 24) | 0xC0_39_2B, (a << 24) | 0xE7_4C_3C);
            if (h >= 2) g.fill(mid - fw, y, mid, y + 1, argb(a / 2, 0xFF, 0xFF, 0xFF));
        }
    }

    public static void cornerTicks(GuiGraphics g, int x, int y, int w, int h, int color, int len) {
        int n = Math.max(3, len);
        g.fill(x, y, x + n, y + 1, color);
        g.fill(x, y, x + 1, y + n, color);
        g.fill(x + w - n, y, x + w, y + 1, color);
        g.fill(x + w - 1, y, x + w, y + n, color);
        g.fill(x, y + h - 1, x + n, y + h, color);
        g.fill(x, y + h - n, x + 1, y + h, color);
        g.fill(x + w - n, y + h - 1, x + w, y + h, color);
        g.fill(x + w - 1, y + h - n, x + w, y + h, color);
    }

    public static void drawDivider(GuiGraphics g, Font font, int x, int y, int w, float alpha) {
        int a = (int) (alpha * 255);
        TwitchUi.fillGradientH(g, x, y, x + w, y + 1,
                (a / 3 << 24) | (TwitchUi.PURPLE & 0xFFFFFF),
                0x00_00_00_00);
    }

    private static boolean compact(int w, int h) {
        return h <= 110 && w <= 340;
    }

    private static int argb(int a, int r, int g, int b) {
        return ((Mth.clamp(a, 0, 255)) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
