package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Edge FX команд по типу:
 * добро — зелёный sweep слева; зло — красный pulse справа.
 */
public final class CommandScreenFx {
    private static int evilLevel;
    private static int goodLevel;
    private static long evilUntil;
    private static long goodUntil;
    private static long evilStart;
    private static long goodStart;
    private static long scareUntil;
    private static long scareStart;
    private static long shieldUntil;
    private static long shieldStart;

    private static long vignetteUntil;
    private static long vignetteStart;
    private static int vignetteRgb;

    private CommandScreenFx() {}

    public static void triggerVignette(int rgb, int durationMs) {
        long now = System.currentTimeMillis();
        vignetteStart = now;
        vignetteUntil = now + Math.max(200, durationMs);
        vignetteRgb = rgb & 0xFFFFFF;
    }

    public static void triggerEvil(int level, int durationMs) {
        evilLevel = Math.max(evilLevel, level);
        long now = System.currentTimeMillis();
        if (now > evilUntil) evilStart = now;
        evilUntil = Math.max(evilUntil, now + durationMs);
    }

    public static void triggerJumpscare() {
        long now = System.currentTimeMillis();
        scareStart = now;
        scareUntil = now + 900;
        triggerEvil(3, 900);
    }

    public static void triggerShield() {
        long now = System.currentTimeMillis();
        if (now > shieldUntil) shieldStart = now;
        shieldUntil = now + 1400;
        triggerGood(900);
    }

    public static void triggerGood(int durationMs) {
        goodLevel = Math.max(goodLevel, 1);
        long now = System.currentTimeMillis();
        if (now > goodUntil) goodStart = now;
        goodUntil = Math.max(goodUntil, now + durationMs);
    }

    public static void triggerAngel() {
        goodLevel = 3;
        long now = System.currentTimeMillis();
        if (now > goodUntil) goodStart = now;
        goodUntil = now + 1600;
        triggerShield();
    }

    public static void triggerMorph() {
        long now = System.currentTimeMillis();
        scareStart = now;
        scareUntil = now + 420;
        triggerGood(900);
        triggerEvil(1, 500);
    }

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc)) return;
        long now = System.currentTimeMillis();
        if (now <= vignetteUntil) {
            renderCategoryVignette(g, mc, now);
        }

        if (HudResolver.enabled(HudResolver.Element.COMMAND_FX)) {
            if (now <= goodUntil && goodLevel > 0) {
                renderGoodSweep(g, mc, now);
            } else {
                goodLevel = 0;
            }

            if (now <= evilUntil && evilLevel > 0) {
                renderEvilPulse(g, mc, now);
            } else {
                evilLevel = 0;
            }

            if (now <= scareUntil) {
                renderJumpscare(g, mc, now);
            }
            if (now <= shieldUntil) {
                renderShieldGold(g, mc, now);
            }
        }
        ConfettiFx.render(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private static void renderCategoryVignette(GuiGraphics g, Minecraft mc, long now) {
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        long dur = Math.max(1, vignetteUntil - vignetteStart);
        float t = Mth.clamp((now - vignetteStart) / (float) dur, 0f, 1f);
        float fade = t < 0.22f ? t / 0.22f : 1f - (t - 0.22f) / 0.78f;
        fade = Mth.clamp(fade, 0f, 1f);
        int a = Mth.clamp((int) (92 * fade), 0, 110);
        int color = vignetteRgb;
        int solid = (a << 24) | color;
        int band = HudLayout.s(22);
        TwitchUi.fillGradientV(g, 0, 0, w, band, solid, 0x00_00_00_00);
        TwitchUi.fillGradientV(g, 0, h - band, w, h, 0x00_00_00_00, solid);
        TwitchUi.fillGradientH(g, 0, 0, band, h, solid, 0x00_00_00_00);
        TwitchUi.fillGradientH(g, w - band, 0, w, h, 0x00_00_00_00, solid);
        HudCard.cornerTicks(g, HudLayout.s(3), HudLayout.s(3), w - HudLayout.s(6), h - HudLayout.s(6),
                solid, HudLayout.s(18));
    }

    private static void renderShieldGold(GuiGraphics g, Minecraft mc, long now) {
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        float t = Mth.clamp((now - shieldStart) / 1400f, 0f, 1f);
        float fade = t < 0.2f ? t / 0.2f : 1f - (t - 0.2f) / 0.8f;
        int color = 0xF1C40F;
        int a = Mth.clamp((int) (48 * fade), 0, 70);
        int solid = (a << 24) | color;
        TwitchUi.fillGradientV(g, 0, 0, w, HudLayout.s(16), solid, 0x00_00_00_00);
        TwitchUi.fillGradientV(g, 0, h - HudLayout.s(16), w, h, 0x00_00_00_00, solid);
        TwitchUi.fillGradientH(g, 0, 0, HudLayout.s(18), h, solid, 0x00_00_00_00);
        TwitchUi.fillGradientH(g, w - HudLayout.s(18), 0, w, h, 0x00_00_00_00, solid);
    }

    private static void renderJumpscare(GuiGraphics g, Minecraft mc, long now) {
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        float t = Mth.clamp((now - scareStart) / 900f, 0f, 1f);
        float flash = t < 0.18f ? 1f - t / 0.18f : 0f;
        float dark = t < 0.35f ? 0.55f : Mth.clamp(1f - (t - 0.35f) / 0.65f, 0f, 0.55f);
        if (flash > 0.05f) {
            g.fill(0, 0, w, h, ((int) (flash * 200) << 24) | 0x00_FF_FF_FF);
        }
        int a = (int) (dark * 170);
        g.fill(0, 0, w, HudLayout.s(28), (a << 24));
        g.fill(0, h - HudLayout.s(28), w, h, (a << 24));
        TwitchUi.fillGradientH(g, 0, 0, HudLayout.s(40), h, (a << 24), 0x00_00_00_00);
        TwitchUi.fillGradientH(g, w - HudLayout.s(40), 0, w, h, 0x00_00_00_00, (a << 24));
    }

    private static void renderGoodSweep(GuiGraphics g, Minecraft mc, long now) {
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        long dur = Math.max(1, goodUntil - goodStart);
        float t = HudAnim.easeOutCubic(Mth.clamp((now - goodStart) / (float) dur, 0f, 1f));
        float fade = Mth.clamp((goodUntil - now) / 280f, 0f, 1f);
        int color = HudResolver.goodColor(0xFF_2E_CC_71) & 0xFFFFFF;

        int band = HudLayout.s(34 + goodLevel * 6);
        int travel = w / 2;
        int x1 = (int) ((travel + band) * t) - band;
        int x2 = x1 + band;
        int alpha = Mth.clamp((int) (70 * fade * (1f - t * 0.22f)), 0, 110);
        int solid = (alpha << 24) | color;

        TwitchUi.fillGradientH(g, Math.max(0, x1), 0, Math.min(w, x2), h, solid, 0x00_00_00_00);
        TwitchUi.fillGradientH(g, 0, 0, HudLayout.s(10), h, (alpha * 8 / 10 << 24) | color, 0x00_00_00_00);
        TwitchUi.fillGradientV(g, 0, 0, w, HudLayout.s(8), (alpha * 6 / 10 << 24) | color, 0x00_00_00_00);
        TwitchUi.fillGradientV(g, 0, h - HudLayout.s(8), w, h, 0x00_00_00_00, (alpha * 6 / 10 << 24) | color);
        HudCard.cornerTicks(g, HudLayout.s(4), HudLayout.s(4), w - HudLayout.s(8), h - HudLayout.s(8),
                (alpha << 24) | color, HudLayout.s(14));
    }

    private static void renderEvilPulse(GuiGraphics g, Minecraft mc, long now) {
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        long dur = Math.max(1, evilUntil - evilStart);
        float age = Mth.clamp((now - evilStart) / (float) dur, 0f, 1f);
        // быстрый удар в начале, потом спад
        float pulse = age < 0.22f
                ? HudAnim.easeOutCubic(age / 0.22f)
                : 1f - HudAnim.easeInCubic((age - 0.22f) / 0.78f);
        float fade = Mth.clamp((evilUntil - now) / 320f, 0f, 1f);
        int color = HudResolver.evilColor(0xFF_E7_4C_3C) & 0xFFFFFF;

        int band = HudLayout.s(14 + evilLevel * 7);
        int grow = (int) (band * (0.7f + pulse * 0.9f));
        int alpha = Mth.clamp((int) ((28 + evilLevel * 12) * pulse * fade), 0, 100);
        int solid = (alpha << 24) | color;

        TwitchUi.fillGradientH(g, w - grow, 0, w, h, 0x00_00_00_00, solid);
        TwitchUi.fillGradientV(g, 0, 0, w, HudLayout.s(evilLevel >= 2 ? 18 : 10), solid, 0x00_00_00_00);
        TwitchUi.fillGradientV(g, 0, h - HudLayout.s(evilLevel >= 2 ? 18 : 10), w, h, 0x00_00_00_00, solid);
        HudCard.cornerTicks(g, HudLayout.s(3), HudLayout.s(3), w - HudLayout.s(6), h - HudLayout.s(6),
                (alpha << 24) | color, HudLayout.s(16));

        if (pulse > 0.55f && evilLevel >= 2) {
            int flash = (int) (18 * (pulse - 0.55f) / 0.45f);
            g.fill(0, 0, w, h, (flash << 24) | color);
        }
    }
}
