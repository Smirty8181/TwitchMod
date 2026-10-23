package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Стрелка и дистанция до пинга напарника. */
public final class PartnerPingOverlay {
    private static final int ACCENT = 0xFF_4E_D4_C4;
    private static String fromName = "";
    private static String dim = "";
    private static int x;
    private static int y;
    private static int z;
    private static long until;

    private PartnerPingOverlay() {}

    public static void apply(String name, String dimension, int px, int py, int pz, int seconds) {
        fromName = name == null || name.isBlank() ? "напарник" : name;
        dim = dimension == null ? "" : dimension;
        x = px;
        y = py;
        z = pz;
        until = System.currentTimeMillis() + Math.max(1, seconds) * 1000L;
    }

    public static void render(GuiGraphics g, float delta) {
        long now = System.currentTimeMillis();
        if (now >= until) return;

        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc) || mc.player == null || mc.level == null) return;

        float left = (until - now) / 1000f;
        boolean wrongDim = !dim.isBlank() && !mc.level.dimension().location().getPath().equals(dim);

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int cx = sw / 2;
        int cy = sh / 2 - 48;

        if (wrongDim) {
            String msg = "§b" + fromName + " §8· другой мир";
            int tw = mc.font.width(TwitchUi.stripFormat(msg));
            g.drawString(mc.font, msg, cx - tw / 2, cy, ACCENT, false);
            return;
        }

        double dx = (x + 0.5) - mc.player.getX();
        double dy = (y + 0.5) - mc.player.getY();
        double dz = (z + 0.5) - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float relative = Mth.wrapDegrees(targetYaw - mc.player.getYRot());

        int a = (int) (210 + 40 * Math.sin(now / 180.0));
        int color = (a << 24) | (ACCENT & 0xFFFFFF);
        drawRing(g, cx, cy, 14, 0x88_00_00_00);
        drawRing(g, cx, cy, 13, color);
        drawArrow(g, cx, cy, relative, 11, (Math.min(255, a + 30) << 24) | (ACCENT & 0xFFFFFF));
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF_FF_FF_FF);

        String title = "§b" + fromName + " §8· §f" + (int) left + "с";
        String distText = dist < 8 ? String.format("§b%.0fм", dist) : String.format("§7%.0fм", dist);
        int tw = mc.font.width(TwitchUi.stripFormat(title));
        int dw = mc.font.width(TwitchUi.stripFormat(distText));
        g.drawString(mc.font, title, cx - tw / 2, cy + 18, ACCENT, false);
        g.drawString(mc.font, distText, cx - dw / 2, cy + 28, 0xFF_DD_DD_DD, false);
    }

    private static void drawRing(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int i = 0; i < 36; i++) {
            double a0 = Math.toRadians(i * 10);
            double a1 = Math.toRadians((i + 1) * 10);
            int x0 = cx + (int) Math.round(Math.cos(a0) * r);
            int y0 = cy + (int) Math.round(Math.sin(a0) * r);
            int x1 = cx + (int) Math.round(Math.cos(a1) * r);
            int y1 = cy + (int) Math.round(Math.sin(a1) * r);
            drawThickLine(g, x0, y0, x1, y1, color, 1);
        }
    }

    private static void drawArrow(GuiGraphics g, int cx, int cy, float degrees, int len, int color) {
        double rad = Math.toRadians(degrees);
        int tx = cx + (int) Math.round(Math.sin(rad) * len);
        int ty = cy - (int) Math.round(Math.cos(rad) * len);
        double back = rad + Math.PI;
        int bx = cx + (int) Math.round(Math.sin(back) * len * 0.35);
        int by = cy - (int) Math.round(Math.cos(back) * len * 0.35);
        double left = rad + Math.PI * 0.78;
        double right = rad - Math.PI * 0.78;
        int lx = cx + (int) Math.round(Math.sin(left) * len * 0.55);
        int ly = cy - (int) Math.round(Math.cos(left) * len * 0.55);
        int rx = cx + (int) Math.round(Math.sin(right) * len * 0.55);
        int ry = cy - (int) Math.round(Math.cos(right) * len * 0.55);
        drawThickLine(g, bx, by, tx, ty, color, 2);
        drawThickLine(g, tx, ty, lx, ly, color, 2);
        drawThickLine(g, tx, ty, rx, ry, color, 2);
    }

    private static void drawThickLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color, int thickness) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) + 1;
        for (int i = 0; i <= steps; i++) {
            float t = steps == 0 ? 0f : (float) i / steps;
            int px = Math.round(x0 + (x1 - x0) * t);
            int py = Math.round(y0 + (y1 - y0) * t);
            g.fill(px - thickness, py - thickness, px + thickness + 1, py + thickness + 1, color);
        }
    }
}
