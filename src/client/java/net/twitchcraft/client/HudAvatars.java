package net.twitchcraft.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

/** Круглый аватар зрителя: Twitch-фото или заглушка из ника. */
public final class HudAvatars {
    private HudAvatars() {}

    public static int colorFor(String name) {
        if (name == null || name.isBlank()) return TwitchUi.PURPLE;
        int hash = name.toLowerCase().hashCode();
        int[] palette = new int[]{
                0xFF_9B_59_FF, 0xFF_74_C0_FC, 0xFF_2E_CC_71, 0xFF_FF_9F_43,
                0xFF_FF_6B_6B, 0xFF_FF_85_C0, 0xFF_E8_C547, 0xFF_1A_BC_9C
        };
        return palette[Math.floorMod(hash, palette.length)];
    }

    public static char initial(String name) {
        if (name == null || name.isBlank()) return '?';
        return Character.toUpperCase(name.charAt(0));
    }

    public static void drawName(GuiGraphics g, Font font, int x, int y, String name,
                                int rgb, float alpha, int maxChars) {
        int a = (int) (alpha * 255);
        if (a < 6) return;
        String user = name == null || name.isBlank() ? "?" : name.trim();
        if (user.length() > maxChars) user = user.substring(0, maxChars - 1) + "…";
        int at = (a << 24) | 0x9E_96_AC;
        int nick = (a << 24) | (rgb == 0 ? 0xF4_F2_F8 : (rgb & 0xFFFFFF));
        g.drawString(font, "@", x, y, at, false);
        g.drawString(font, "§l" + user, x + font.width("@"), y, nick, false);
    }

    public static int nameWidth(Font font, String name, int maxChars) {
        String user = name == null || name.isBlank() ? "?" : name.trim();
        if (user.length() > maxChars) user = user.substring(0, maxChars - 1) + "…";
        return font.width("@") + font.width("§l" + user);
    }

    public static void draw(GuiGraphics g, Font font, int x, int y, int size, String name, float alpha) {
        draw(g, font, x, y, size, name, alpha, false);
    }

    public static void draw(GuiGraphics g, Font font, int x, int y, int size, String name,
                            float alpha, boolean good) {
        int a = (int) (alpha * 255);
        if (a < 4) return;

        TwitchAvatarCache.request(name);
        ResourceLocation tex = TwitchAvatarCache.getIfReady(name);
        if (tex != null && blitAvatar(g, tex, x, y, size, a)) {
            drawRingAndDot(g, x, y, size, a, good, colorFor(name));
            return;
        }
        drawStub(g, font, x, y, size, name, a, good);
    }

    private static boolean blitAvatar(GuiGraphics g, ResourceLocation tex, int x, int y, int size, int a) {
        try {
            // рамка под фото
            g.fill(x, y, x + size, y + size, (a << 24) | 0x20_18_28);
            int inset = Math.max(1, size / 12);
            g.blit(RenderPipelines.GUI_TEXTURED, tex,
                    x + inset, y + inset, 0f, 0f,
                    size - inset * 2, size - inset * 2,
                    size - inset * 2, size - inset * 2);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void drawStub(GuiGraphics g, Font font, int x, int y, int size, String name,
                                 int a, boolean good) {
        int col = colorFor(name);
        int light = TwitchUi.blendRgb(col, 0xFFFFFF, 0.28f) & 0xFFFFFF;
        int dark = TwitchUi.blendRgb(col, 0x000000, 0.35f) & 0xFFFFFF;

        int glow = (a / 5 << 24) | (col & 0xFFFFFF);
        g.fill(x - 1, y + 2, x + size + 1, y + size - 2, glow);
        g.fill(x + 2, y - 1, x + size - 2, y, glow);
        g.fill(x + 2, y + size, x + size - 2, y + size + 1, glow);

        int ring = (a << 24) | (light & 0xFFFFFF);
        g.fill(x, y, x + size, y + size, ring);

        TwitchUi.fillGradientV(g, x + 1, y + 1, x + size - 1, y + size - 1,
                (a << 24) | (light & 0xFFFFFF), (a << 24) | (dark & 0xFFFFFF));
        g.fill(x + 2, y + 2, x + size - 2, y + size - 2, (a << 24) | (col & 0xFFFFFF));

        TwitchUi.fillGradientV(g, x + 2, y + 2, x + size - 2, y + size / 2,
                (a / 4 << 24) | 0xFF_FF_FF, 0x00_00_00_00);

        String letter = String.valueOf(initial(name));
        int tw = font.width(letter);
        int lx = x + (size - tw) / 2;
        int ly = y + (size - 8) / 2;
        g.drawString(font, letter, lx + 1, ly + 1, (a / 3 << 24) | 0x00_00_00, false);
        g.drawString(font, "§f§l" + letter, lx, ly, (a << 24) | 0xFFFFFF, false);

        drawRingAndDot(g, x, y, size, a, good, col);
    }

    /** Огонёк / плюс вокруг аватара при серии ×3+. */
    public static void drawStreak(GuiGraphics g, Font font, int x, int y, int size, int count,
                                 boolean good, float alpha, long now) {
        int a = (int) (alpha * 255);
        if (a < 8 || size < 8 || count < 3 || font == null) return;
        int rgb = good ? 0x2E_CC_71 : 0xFF_9F_43;
        float pulse = 0.72f + 0.28f * (float) Math.sin(now / 160.0);
        int ga = (int) (a * pulse);
        int ring = (Math.min(220, ga) << 24) | rgb;
        g.fill(x - 2, y - 2, x + size + 2, y, ring);
        g.fill(x - 2, y + size, x + size + 2, y + size + 2, ring);
        g.fill(x - 2, y, x, y + size, ring);
        g.fill(x + size, y, x + size + 2, y + size, ring);
        int sparks = Math.min(8, 3 + count);
        double spin = now / 180.0;
        int cx = x + size / 2;
        int cy = y + size / 2;
        int rad = size / 2 + 3;
        for (int i = 0; i < sparks; i++) {
            double ang = spin + (Math.PI * 2.0 * i) / sparks;
            int px = cx + (int) Math.round(Math.cos(ang) * rad);
            int py = cy + (int) Math.round(Math.sin(ang) * rad);
            int ds = good ? 2 : 3;
            g.fill(px - 1, py - 1, px + ds - 1, py + ds - 1, (ga << 24) | rgb);
            g.fill(px, py, px + 1, py + 1, (ga << 24) | 0xFF_FF_FF);
        }
        String mark = "×" + Math.min(99, count);
        int tw = font.width(mark);
        int bx = x + Math.max(0, size - tw);
        int by = y + size - 8;
        g.fill(bx - 1, by - 1, x + size + 1, y + size + 1, (a * 7 / 10 << 24));
        g.drawString(font, mark, bx, by, (a << 24) | rgb, false);
    }

    private static void drawRingAndDot(GuiGraphics g, int x, int y, int size, int a, boolean good, int col) {
        // тонкая обводка
        int edge = (a << 24) | (TwitchUi.blendRgb(col, 0xFFFFFF, 0.4f) & 0xFFFFFF);
        g.fill(x, y, x + size, y + 1, edge);
        g.fill(x, y + size - 1, x + size, y + size, edge);
        g.fill(x, y, x + 1, y + size, edge);
        g.fill(x + size - 1, y, x + size, y + size, edge);

        int dot = good ? 0xFF_55_FF_88 : 0xFF_FF_66_66;
        int ds = Math.max(2, size / 7);
        g.fill(x + size - ds - 1, y + size - ds - 1, x + size, y + size, (a << 24) | (dot & 0xFFFFFF));
        g.fill(x + size - ds, y + size - ds, x + size - 1, y + size - 1, (a << 24) | 0xFF_FF_FF);
    }
}
