package net.twitchcraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.twitchcraft.TwitchCraftMod;

/** Компактная пиксель-голова Smirty для UI. */
public final class SmirtyLogo {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TwitchCraftMod.MOD_ID, "textures/gui/smirty_logo");

    private static final int PURPLE_BG = 0xFF_3A_12_62;
    private static final int GOLD = 0xFF_FF_D7_00;
    private static final int YELLOW = 0xFF_FF_D7_00;
    private static final int GREY = 0xFF_8A_8A_98;
    private static final int WHITE = 0xFF_FF_FF_FF;
    private static final int PUPIL = 0xFF_18_18_22;
    private static final int RED = 0xFF_FF_44_44;

    private static final int[][] BADGE_FACE = {
            {0, 0, YELLOW, YELLOW, YELLOW, YELLOW, 0, 0},
            {0, YELLOW, YELLOW, YELLOW, YELLOW, YELLOW, YELLOW, 0},
            {YELLOW, GREY, WHITE, PUPIL, PUPIL, WHITE, GREY, YELLOW},
            {YELLOW, GREY, WHITE, PUPIL, PUPIL, WHITE, GREY, YELLOW},
            {YELLOW, GREY, GREY, RED, RED, GREY, GREY, YELLOW},
            {YELLOW, GREY, YELLOW, YELLOW, YELLOW, YELLOW, GREY, YELLOW},
            {0, YELLOW, YELLOW, YELLOW, YELLOW, YELLOW, YELLOW, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
    };

    private SmirtyLogo() {}

    /** Маленький значок для шапок меню и sidebar — читается с 14px. */
    public static void drawInline(GuiGraphics g, int x, int y, int size, int accent) {
        int border = accent != 0 ? accent : TwitchUi.PURPLE;
        drawBadge(g, x, y, size, border);
        if (accent != 0) {
            int line = (accent & 0xFFFFFF) | 0xCC000000;
            g.fill(x, y + size, x + size, y + size + 1, line);
        }
    }

    /** Лого в центре колеса — только процедур, без GPU-проверок каждый кадр. */
    public static void drawWheelHub(GuiGraphics g, int cx, int cy, int size) {
        drawBadge(g, cx - size / 2, cy - size / 2, size, TwitchUi.GOLD);
    }

    public static void draw(GuiGraphics g, int x, int y, int size) {
        drawWithGlow(g, x, y, size, 0);
    }

    /** Крупное лого с подсветкой — рулетка и акценты. */
    public static void drawWithGlow(GuiGraphics g, int x, int y, int size, int accent) {
        if (size < 20) {
            drawInline(g, x, y, size, accent);
            return;
        }

        int border = accent != 0 ? accent : GOLD;
        int glow = (border & 0xFFFFFF) | 0x44000000;
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, glow);

        if (size >= 24 && drawTexture(g, x, y, size)) {
            return;
        }
        drawBadge(g, x, y, size, border);
    }

    private static void drawBadge(GuiGraphics g, int x, int y, int size, int borderColor) {
        g.fill(x, y, x + size, y + size, PURPLE_BG);
        int b = Math.max(1, size / 16);
        g.fill(x, y, x + size, y + b, borderColor);
        g.fill(x, y + size - b, x + size, y + size, borderColor);
        g.fill(x, y, x + b, y + size, borderColor);
        g.fill(x + size - b, y, x + size, y + size, borderColor);

        int inset = Math.max(2, size / 8);
        drawBadgeFace(g, x + inset, y + inset, size - inset * 2);
    }

    private static void drawBadgeFace(GuiGraphics g, int x, int y, int size) {
        int px = Math.max(1, size / 8);
        int drawW = px * 8;
        int drawH = px * 8;
        int ox = x + (size - drawW) / 2;
        int oy = y + (size - drawH) / 2;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int c = BADGE_FACE[row][col];
                if (c == 0) continue;
                g.fill(ox + col * px, oy + row * px, ox + (col + 1) * px, oy + (row + 1) * px, 0xFF_00_00_00 | c);
            }
        }
    }

    private static boolean drawTexture(GuiGraphics g, int x, int y, int size) {
        if (!TwitchTextures.isAvailable(TEXTURE)) return false;
        try {
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, size, size, 64, 64);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
