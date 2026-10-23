package net.twitchcraft.client;

import net.minecraft.util.Mth;

/** Общие easing-кривые HUD (везде одинаковые). */
public final class HudAnim {
    private HudAnim() {}

    public static float easeInOutCubic(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }

    public static float easeOutCubic(float t) {
        t = Mth.clamp(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    public static float easeInCubic(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t * t * t;
    }

    public static float easeOutQuint(float t) {
        t = Mth.clamp(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u * u * u;
    }

    /** Плавный подход display → target (фрейм-независимый коэффициент 0..1). */
    public static float approach(float display, float target, float speed) {
        return display + (target - display) * Mth.clamp(speed, 0f, 1f);
    }

    public static float easeOutBack(float t) {
        t = Mth.clamp(t, 0f, 1f);
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(t - 1f, 3) + c1 * (float) Math.pow(t - 1f, 2);
    }

    public static float bounce(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return (float) Math.abs(Math.sin(t * Math.PI * 3) * (1f - t));
    }
}
