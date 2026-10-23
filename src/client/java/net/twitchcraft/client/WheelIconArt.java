package net.twitchcraft.client;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

/** Запасная отрисовка тех же 16×16 спрайтов, если PNG не загрузился. */
public final class WheelIconArt {
    private WheelIconArt() {}

    public static void draw(GuiGraphics g, String commandId, int cx, int cy, int size,
                            boolean highlight, long elapsed) {
        draw(g, commandId, cx, cy, size, highlight, elapsed, false);
    }

    public static void draw(GuiGraphics g, String commandId, int cx, int cy, int size,
                            boolean highlight, long elapsed, boolean withFrame) {
        String id = normalize(commandId);
        int glyph = Math.max(16, size);
        int scale = Math.max(1, glyph / WheelIconSprites.GRID);
        int drawn = scale * WheelIconSprites.GRID;
        int ox = cx - drawn / 2;
        int oy = cy - drawn / 2;

        if (highlight) {
            float pulse = 0.65f + 0.35f * (float) Math.sin(elapsed * 0.12);
            int glow = ((int) (pulse * 140) << 24) | 0xFFD700;
            g.fill(ox - 3, oy - 3, ox + drawn + 3, oy + drawn + 3, glow);
        }
        if (withFrame) {
            g.fill(ox - 2, oy - 2, ox + drawn + 2, oy + drawn + 2, 0xFF_22_1C_2A);
        }

        String[] rows = WheelIconSprites.rows(id);
        boolean[][] fill = new boolean[WheelIconSprites.GRID][WheelIconSprites.GRID];
        int[][] colors = new int[WheelIconSprites.GRID][WheelIconSprites.GRID];
        for (int r = 0; r < WheelIconSprites.GRID && r < rows.length; r++) {
            String line = rows[r];
            for (int c = 0; c < WheelIconSprites.GRID && c < line.length(); c++) {
                int rgb = WheelIconSprites.color(line.charAt(c));
                if (rgb == 0) continue;
                fill[r][c] = true;
                colors[r][c] = rgb;
            }
        }
        int ink = 0xFF_0A_08_0C;
        for (int r = 0; r < WheelIconSprites.GRID; r++) {
            for (int c = 0; c < WheelIconSprites.GRID; c++) {
                if (!fill[r][c]) continue;
                paint(g, ox, oy, scale, c - 1, r, ink);
                paint(g, ox, oy, scale, c + 1, r, ink);
                paint(g, ox, oy, scale, c, r - 1, ink);
                paint(g, ox, oy, scale, c, r + 1, ink);
            }
        }
        for (int r = 0; r < WheelIconSprites.GRID; r++) {
            for (int c = 0; c < WheelIconSprites.GRID; c++) {
                if (fill[r][c]) paint(g, ox, oy, scale, c, r, 0xFF_00_00_00 | colors[r][c]);
            }
        }
    }

    private static void paint(GuiGraphics g, int ox, int oy, int scale, int c, int r, int color) {
        if (c < 0 || r < 0 || c >= WheelIconSprites.GRID || r >= WheelIconSprites.GRID) return;
        int x1 = ox + c * scale;
        int y1 = oy + r * scale;
        g.fill(x1, y1, x1 + scale, y1 + scale, color);
    }

    private static String normalize(String id) {
        if (id == null || id.isBlank()) return "event";
        return id.toLowerCase(Locale.ROOT).trim();
    }
}
