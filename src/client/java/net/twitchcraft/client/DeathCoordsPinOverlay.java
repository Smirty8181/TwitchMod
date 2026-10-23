package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Закреплённые координаты смерти + стрелка-компас до трупа. */
public final class DeathCoordsPinOverlay {
    private static final Pattern COORDS = Pattern.compile("X:(-?\\d+)\\s*Y:(-?\\d+)\\s*Z:(-?\\d+)");
    private static final double NEAR_BLOCKS = 48.0;

    private DeathCoordsPinOverlay() {}

    public static void render(GuiGraphics g, float delta) {
        TwitchCraftConfig cfg = ConfigManager.get();
        TwitchCraftConfig.DeathSettings death = cfg.death;
        if (death == null || !death.enabled || !death.showCoordinates) return;

        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc) || mc.player == null) return;

        DeathPos pos = resolveDeathPos(mc);
        if (pos == null) return;

        if (death.pinCoordinates && HudResolver.enabled(HudResolver.Element.PINNED_DEATH_COORDS)) {
            renderPinPanel(g, mc, death, pos);
        }
        if (death.showDeathArrow) {
            renderDeathArrow(g, mc, pos);
        }
    }

    private static void renderPinPanel(GuiGraphics g, Minecraft mc, TwitchCraftConfig.DeathSettings death, DeathPos pos) {
        int deaths = SidebarOverlay.totalDeaths();
        if (deaths <= 0) deaths = 1;

        var el = HudResolver.el(HudResolver.Element.PINNED_DEATH_COORDS);
        float es = HudResolver.elementScale(HudResolver.Element.PINNED_DEATH_COORDS);
        float opacity = HudResolver.opacity(HudResolver.Element.PINNED_DEATH_COORDS);
        int accent = HudResolver.accent(HudResolver.Element.PINNED_DEATH_COORDS, 0xFF_CC_44_55);

        float nearPulse = nearFactor(mc, pos);
        float pulse = nearPulse > 0
                ? 0.72f + 0.28f * (0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 220.0))
                : 1f;
        float drawOpacity = opacity * pulse;

        var font = mc.font;
        String title = nearPulse > 0 ? "§c§l☠ РЯДОМ · #" + deaths : "§c§l☠ СМЕРТЬ #" + deaths;
        String line = "§eX:" + pos.x + " §fY:" + pos.y + " §bZ:" + pos.z;
        int padX = HudLayout.s(8, es);
        int padY = HudLayout.s(6, es);
        int lineH = HudLayout.s(10, es);
        int panelW = Math.max(HudLayout.s(120, es),
                Math.max(font.width(TwitchUi.stripFormat(title)), font.width(TwitchUi.stripFormat(line))) + padX * 2);
        int panelH = padY * 2 + lineH * 2 + HudLayout.s(2, es);

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        HudPlacement.Rect place = HudPlacement.place(el.anchor, el.offsetX, el.offsetY, sw, sh, panelW, panelH, es);

        HudCard.draw(g, place.x(), place.y(), panelW, panelH, accent, drawOpacity, 1f);
        if (nearPulse > 0) {
            int a = (int) (drawOpacity * 40 * nearPulse);
            TwitchUi.fillGradientV(g, place.x(), place.y(), place.x() + 2, place.y() + panelH,
                    (a << 24) | 0xFF_66_66, (a / 3 << 24) | 0xFF_66_66);
        }

        int tx = place.x() + padX;
        int ty = place.y() + padY;
        int textA = (int) (drawOpacity * 255);
        g.drawString(font, title, tx, ty, (textA << 24) | (accent & 0xFFFFFF), false);
        g.drawString(font, line, tx, ty + lineH + HudLayout.s(2, es),
                (textA << 24) | (TwitchUi.TEXT & 0xFFFFFF), false);
    }

    private static void renderDeathArrow(GuiGraphics g, Minecraft mc, DeathPos pos) {
        if (pos.wrongDim) {
            int sw = mc.getWindow().getGuiScaledWidth();
            int sh = mc.getWindow().getGuiScaledHeight();
            String msg = "§8☠ другой мир";
            int tw = mc.font.width(TwitchUi.stripFormat(msg));
            g.drawString(mc.font, msg, (sw - tw) / 2, sh / 2 + 28, 0xAA_AA_AA_AA, false);
            return;
        }

        double dx = (pos.x + 0.5) - mc.player.getX();
        double dz = (pos.z + 0.5) - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz + Math.pow((pos.y + 0.5) - mc.player.getY(), 2));
        // yaw 0 = south (+Z); target yaw as in vanilla compass
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float relative = Mth.wrapDegrees(targetYaw - mc.player.getYRot());

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int cx = sw / 2;
        int cy = sh / 2 + 36;

        float near = nearFactor(mc, pos);
        int accent = near > 0 ? 0xFF_FF_55_55 : 0xFF_EE_CC_55;
        int ring = 0x88_00_00_00;
        int a = near > 0
                ? (int) (180 + 75 * (0.5f + 0.5f * Math.sin(System.currentTimeMillis() / 180.0)))
                : 200;

        // кольцо
        drawRing(g, cx, cy, 16, ring);
        drawRing(g, cx, cy, 15, (a << 24) | (accent & 0xFFFFFF));

        // стрелка (0° = вверх = «вперёд к трупу» когда relative=0)
        drawArrow(g, cx, cy, relative, 13, (Math.min(255, a + 40) << 24) | (accent & 0xFFFFFF));
        // центр
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF_FF_FF_FF);

        String distText = dist < 10 ? String.format("§c%.0fм", dist) : String.format("§7%.0fм", dist);
        int dw = mc.font.width(TwitchUi.stripFormat(distText));
        g.drawString(mc.font, distText, cx - dw / 2, cy + 20, 0xFF_DD_DD_DD, false);
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
        // tip
        int tx = cx + (int) Math.round(Math.sin(rad) * len);
        int ty = cy - (int) Math.round(Math.cos(rad) * len);
        // base corners
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
        drawThickLine(g, lx, ly, rx, ry, color, 1);
    }

    private static void drawThickLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color, int thickness) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) + 1;
        for (int i = 0; i <= steps; i++) {
            float t = steps == 0 ? 0f : (float) i / steps;
            int x = Math.round(x0 + (x1 - x0) * t);
            int y = Math.round(y0 + (y1 - y0) * t);
            g.fill(x - thickness, y - thickness, x + thickness + 1, y + thickness + 1, color);
        }
    }

    private static float nearFactor(Minecraft mc, DeathPos pos) {
        if (pos.wrongDim) return 0f;
        double dist = Math.sqrt(mc.player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5));
        if (dist > NEAR_BLOCKS) return 0f;
        return (float) Mth.clamp(1.0 - dist / NEAR_BLOCKS, 0.15, 1.0);
    }

    private static DeathPos resolveDeathPos(Minecraft mc) {
        if (DeathOverlay.hasPinnedCoords()) {
            String dim = DeathOverlay.pinDim();
            String playerDim = mc.level != null ? mc.level.dimension().location().getPath() : "";
            boolean wrong = dim != null && !dim.isBlank() && !playerDim.isBlank() && !dim.equals(playerDim);
            return new DeathPos(DeathOverlay.pinX(), DeathOverlay.pinY(), DeathOverlay.pinZ(), wrong);
        }
        String raw = SidebarOverlay.lastDeathCoords();
        if (raw == null || raw.isBlank()) return null;
        Matcher m = COORDS.matcher(TwitchUi.stripFormat(raw));
        if (!m.find()) return null;
        return new DeathPos(
                Integer.parseInt(m.group(1)),
                Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)),
                false);
    }

    private record DeathPos(int x, int y, int z, boolean wrongDim) {}
}
