package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Кубик стримера — крутится, потом замирает на грани. */
public final class DiceOverlay {
    private static final long ROLL_MS = 1800;
    private static final long HOLD_MS = 2800;

    private static int face = 1;
    private static String title = "";
    private static String subtitle = "";
    private static long startMs;
    private static long showUntil;
    private static boolean active;
    private static boolean landed;
    private static int flickerFace = 1;

    private DiceOverlay() {}

    public static void start(int resultFace, String t, String sub) {
        face = Mth.clamp(resultFace, 1, 6);
        title = t == null ? "" : t;
        subtitle = sub == null ? "" : sub;
        startMs = System.currentTimeMillis();
        showUntil = startMs + ROLL_MS + HOLD_MS;
        active = true;
        landed = false;
        flickerFace = 1;
        HudUiSounds.diceRoll();
    }

    public static boolean isActive() {
        return active && System.currentTimeMillis() <= showUntil;
    }

    public static void render(GuiGraphics g, float delta) {
        if (!isActive()) {
            active = false;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc)) return;

        long now = System.currentTimeMillis();
        long elapsed = now - startMs;
        boolean rolling = elapsed < ROLL_MS;
        if (rolling) {
            float t = elapsed / (float) ROLL_MS;
            int speed = t < 0.55f ? 70 : (int) (70 + (t - 0.55f) * 400);
            flickerFace = 1 + (int) ((elapsed / speed) % 6);
        } else if (!landed) {
            landed = true;
            flickerFace = face;
            HudUiSounds.diceLand();
            int w = mc.getWindow().getGuiScaledWidth();
            ConfettiFx.burst(w / 2, HudLayout.s(90), face == 6 || face == 3 ? 18 : 10);
            if (face == 6) CommandScreenFx.triggerShield();
            else if (face == 3) CommandScreenFx.triggerGood(700);
            else if (face == 1) CommandScreenFx.triggerEvil(2, 700);
        }

        int shown = rolling ? flickerFace : face;
        float appear = HudAnim.easeOutBack(Mth.clamp(elapsed / 280f, 0f, 1f));
        float fadeOut = showUntil - now < 400 ? (showUntil - now) / 400f : 1f;
        float pulse = rolling
                ? 0.92f + 0.08f * (float) Math.sin(elapsed / 40.0)
                : 1f + 0.04f * (float) Math.sin(now / 180.0);
        float alpha = appear * fadeOut;

        int w = mc.getWindow().getGuiScaledWidth();
        int size = snapDieSize(HudLayout.s(84));
        int px = (w - size) / 2;
        int py = HudLayout.s(28);
        int accent = accentFor(shown);

        int overlay = ((int) (alpha * 55) << 24);
        g.fill(0, 0, w, mc.getWindow().getGuiScaledHeight(), overlay);
        int h = mc.getWindow().getGuiScaledHeight();
        TwitchUi.fillGradientV(g, 0, 0, w, HudLayout.s(22), (int) (alpha * 50) << 24 | (accent & 0xFFFFFF), 0);
        TwitchUi.fillGradientV(g, 0, h - HudLayout.s(22), w, h, 0, (int) (alpha * 50) << 24 | (accent & 0xFFFFFF));

        int cardPad = HudLayout.s(8);
        HudCard.draw(g, px - cardPad, py - cardPad, size + cardPad * 2, size + cardPad * 2, accent, alpha, pulse);
        drawDie(g, px, py, size, shown, accent, alpha);

        var font = mc.font;
        if (!rolling || elapsed > 900) {
            String head = rolling ? "§6§l…" : "§6§l" + face + "  " + title;
            int hw = font.width(TwitchUi.stripFormat(head));
            g.drawString(font, head, (w - hw) / 2, py + size + HudLayout.s(10), 0xFF_FF_EE_CC, false);
            if (!rolling && !subtitle.isBlank()) {
                int sw = font.width(subtitle);
                g.drawString(font, "§7" + subtitle, (w - sw) / 2, py + size + HudLayout.s(22), TwitchUi.TEXT_DIM, false);
            }
        } else {
            String wait = "§6§lКУБИК";
            int hw = font.width(TwitchUi.stripFormat(wait));
            g.drawString(font, wait, (w - hw) / 2, py + size + HudLayout.s(10), 0xFF_FF_EE_CC, false);
        }
    }

    private static void drawDie(GuiGraphics g, int x, int y, int size, int n, int accent, float alpha) {
        int a = (int) (alpha * 255);
        int face = argb(a, 0xF4, 0xF0, 0xE6);
        int edge = (a << 24) | (accent & 0xFFFFFF);
        g.fill(x, y, x + size, y + size, face);
        g.fill(x, y, x + size, y + 2, edge);
        g.fill(x, y + size - 2, x + size, y + size, edge);
        g.fill(x, y, x + 2, y + size, edge);
        g.fill(x + size - 2, y, x + size, y + size, edge);

        int pip = even(Math.max(6, size / 7));
        int gap = Math.max(4, (size - 3 * pip) / 4);
        int used = 3 * pip + 4 * gap;
        int origin = (size - used) / 2;
        int[] slot = {
                origin + gap,
                origin + gap * 2 + pip,
                origin + gap * 3 + pip * 2
        };

        int pipC = argb(a, 0x2A, 0x22, 0x28);
        int pipHi = argb(a, 0x3A, 0x32, 0x38);
        for (int[] d : pips(n)) {
            int cx = x + slot[d[0]];
            int cy = y + slot[d[1]];
            g.fill(cx, cy, cx + pip, cy + pip, pipC);
            g.fill(cx + 1, cy + 1, cx + pip - 1, cy + pip - 1, pipHi);
        }
    }

    /** Сетка 3×3, координаты 0..2. */
    private static int[][] pips(int n) {
        return switch (n) {
            case 1 -> new int[][] {{1, 1}};
            case 2 -> new int[][] {{0, 0}, {2, 2}};
            case 3 -> new int[][] {{0, 0}, {1, 1}, {2, 2}};
            case 4 -> new int[][] {{0, 0}, {2, 0}, {0, 2}, {2, 2}};
            case 5 -> new int[][] {{0, 0}, {2, 0}, {1, 1}, {0, 2}, {2, 2}};
            default -> new int[][] {{0, 0}, {0, 1}, {0, 2}, {2, 0}, {2, 1}, {2, 2}};
        };
    }

    private static int snapDieSize(int size) {
        return Math.max(48, (size / 6) * 6);
    }

    private static int even(int v) {
        return v & ~1;
    }

    private static int argb(int a, int r, int g, int b) {
        return (Mth.clamp(a, 0, 255) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private static int accentFor(int n) {
        return switch (n) {
            case 1 -> 0xFF_E74C3C;
            case 2 -> 0xFF_F39C12;
            case 3 -> 0xFF_2ECC71;
            case 4 -> 0xFF_9B59B6;
            case 5 -> 0xFF_E67E22;
            default -> 0xFF_F1C40F;
        };
    }
}
