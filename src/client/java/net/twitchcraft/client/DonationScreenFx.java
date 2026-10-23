package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Edge FX доната — тонкие градиентные полосы. */
public final class DonationScreenFx {
    private static int level;
    private static int tierMin;
    private static long untilMs;
    private static long startMs;

    private DonationScreenFx() {}

    public static void trigger(int screenFxLevel, int durationMs) {
        trigger(screenFxLevel, durationMs, 50);
    }

    public static void trigger(int screenFxLevel, int durationMs, int minAmount) {
        level = Math.max(level, screenFxLevel);
        tierMin = Math.max(tierMin, minAmount);
        long now = System.currentTimeMillis();
        untilMs = Math.max(untilMs, now + durationMs);
        if (startMs == 0) startMs = now;
    }

    public static void render(GuiGraphics g) {
        if (DonationWheelOverlay.isActive()) return;
        long now = System.currentTimeMillis();
        if (now > untilMs || level <= 0) {
            level = 0;
            tierMin = 0;
            startMs = 0;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc)) return;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        float fade = Mth.clamp((untilMs - now) / 700f, 0f, 1f);
        long age = now - startMs;
        float kick = age < 160 ? (1f - age / 160f) : 0f;

        int color = tierMin >= 1000 ? 0x9B_59_B6 : tierMin >= 500 ? 0xF5_D0_6E
                : tierMin >= 200 ? 0xFF_9F_43 : tierMin >= 100 ? 0xFF_D7_00 : TwitchUi.PURPLE & 0xFFFFFF;

        int band = HudLayout.s(tierMin >= 500 ? 16 : 10);
        int a = Mth.clamp((int) ((16 + level * 5) * fade + kick * 36), 0, 80);
        int solid = (a << 24) | (color & 0xFFFFFF);
        int soft = 0x00_00_00_00;

        TwitchUi.fillGradientV(g, 0, 0, w, band, solid, soft);
        TwitchUi.fillGradientV(g, 0, h - band, w, h, soft, solid);
        TwitchUi.fillGradientH(g, 0, 0, HudLayout.s(5), h, solid, soft);
        TwitchUi.fillGradientH(g, w - HudLayout.s(5), 0, w, h, soft, solid);

        if (kick > 0.2f) {
            int flash = (int) (18 * kick);
            g.fill(0, 0, w, h, (flash << 24) | (color & 0xFFFFFF));
        }
    }
}
