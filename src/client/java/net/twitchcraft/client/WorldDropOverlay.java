package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Только компас к находке. Без верхней плашки. */
public final class WorldDropOverlay {
    private static String kind = "";
    private static String viewer = "";
    private static String dim = "";
    private static int x;
    private static int y;
    private static int z;
    private static int secondsLeft;
    private static int totalSeconds = 1;
    private static boolean claimed;
    private static boolean active;
    private static long lastPacketAt;
    private static long appearAt;
    private static long claimedAt;

    private WorldDropOverlay() {}

    public static void apply(String k, String who, String dimension,
                             int px, int py, int pz,
                             int left, int total, boolean took, boolean on) {
        boolean was = isLive();
        kind = k == null ? "" : k;
        viewer = who == null ? "" : who;
        dim = dimension == null ? "" : dimension;
        x = px;
        y = py;
        z = pz;
        secondsLeft = left;
        totalSeconds = Math.max(1, total);
        if (took && !claimed) claimedAt = System.currentTimeMillis();
        claimed = took;
        active = on;
        lastPacketAt = System.currentTimeMillis();
        if (on && !was) {
            appearAt = lastPacketAt;
            claimedAt = 0;
        }
        if (!on) active = false;
    }

    public static boolean isLive() {
        if (!active) return false;
        if (System.currentTimeMillis() - lastPacketAt > 2500) return false;
        if (claimed && claimedAt > 0 && System.currentTimeMillis() - claimedAt > 1400) return false;
        return true;
    }

    public static void render(GuiGraphics g, float delta) {
        if (!isLive()) {
            if (System.currentTimeMillis() - lastPacketAt > 2500) active = false;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc) || mc.player == null || mc.level == null) return;
        if (claimed) return;

        int accent = accentOf(kind);
        boolean urgent = secondsLeft <= 8;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        renderCompass(g, mc, sw, sh, accent, urgent);
    }

    private static void renderCompass(GuiGraphics g, Minecraft mc, int sw, int sh, int accent, boolean urgent) {
        if (!dim.isBlank() && !mc.level.dimension().location().getPath().equals(dim)) return;
        double dx = (x + 0.5) - mc.player.getX();
        double dy = (y + 0.5) - mc.player.getY();
        double dz = (z + 0.5) - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 4.5) return;

        int cx = sw / 2;
        int cy = sh / 2 + HudLayout.s(22);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float relative = Mth.wrapDegrees(targetYaw - mc.player.getYRot());
        int a = urgent
                ? (int) (170 + 50 * Math.sin(System.currentTimeMillis() / 160.0))
                : 175;
        int color = (a << 24) | (accent & 0xFFFFFF);
        drawRing(g, cx, cy, 11, 0x66_00_00_00);
        drawRing(g, cx, cy, 10, color);
        drawArrow(g, cx, cy, relative, 8, (Math.min(255, a + 35) << 24) | (accent & 0xFFFFFF));
        g.fill(cx, cy, cx + 1, cy + 1, 0xFF_FF_FF_FF);
    }

    private static int accentOf(String k) {
        return switch (k) {
            case "SHRINE" -> 0xFF_9B_59_B6;
            case "GUEST" -> 0xFF_FF_7A_A8;
            default -> 0xFF_F1_C4_0F;
        };
    }

    private static void drawRing(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int i = 0; i < 28; i++) {
            double a0 = Math.toRadians(i * (360.0 / 28));
            double a1 = Math.toRadians((i + 1) * (360.0 / 28));
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
        int bx = cx + (int) Math.round(Math.sin(back) * len * 0.3);
        int by = cy - (int) Math.round(Math.cos(back) * len * 0.3);
        double left = rad + Math.PI * 0.78;
        double right = rad - Math.PI * 0.78;
        int lx = cx + (int) Math.round(Math.sin(left) * len * 0.5);
        int ly = cy - (int) Math.round(Math.cos(left) * len * 0.5);
        int rx = cx + (int) Math.round(Math.sin(right) * len * 0.5);
        int ry = cy - (int) Math.round(Math.cos(right) * len * 0.5);
        drawThickLine(g, bx, by, tx, ty, color, 1);
        drawThickLine(g, tx, ty, lx, ly, color, 1);
        drawThickLine(g, tx, ty, rx, ry, color, 1);
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
