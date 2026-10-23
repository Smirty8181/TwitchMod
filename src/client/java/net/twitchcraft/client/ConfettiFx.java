package net.twitchcraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Confetti для добра / джекпота — короткие вспышки, без мусора на экране. */
public final class ConfettiFx {
    private record Particle(float x, float y, float vx, float vy, int color, long born, int size, int kind) {}

    private static final List<Particle> PARTICLES = new ArrayList<>();
    private static final int MAX = 42;
    private static final long LIFE_MS = 1500;

    private ConfettiFx() {}

    public static void burst(int cx, int cy, int count) {
        burst(cx, cy, count, new int[] {
                0xFF_FF_D7_00, 0xFF_9B_59_FF, 0xFF_FF_6B_6B, 0xFF_2E_CC_71,
                0xFF_74_C0_FC, 0xFF_E8_C547, 0xFF_FF_7A_A8
        });
    }

    public static void burstGood(int cx, int cy, int count) {
        burst(cx, cy, count, new int[] {
                0xFF_2E_CC_71, 0xFF_A8_FF_C4, 0xFF_FF_D7_00, 0xFF_74_C0_FC, 0xFF_FF_FF_FF
        });
    }

    public static void burstEvil(int cx, int cy, int count) {
        burst(cx, cy, count, new int[] {
                0xFF_E7_4C_3C, 0xFF_C0_39_2B, 0xFF_FF_9F_43, 0xFF_FF_66_66
        });
    }

    private static void burst(int cx, int cy, int count, int[] colors) {
        long now = System.currentTimeMillis();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = Math.min(count, MAX);
        for (int i = 0; i < n; i++) {
            float angle = (float) (rnd.nextDouble() * Math.PI * 2);
            float speed = 2.2f + rnd.nextFloat() * 4.4f;
            PARTICLES.add(new Particle(
                    cx, cy,
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed - 2.6f,
                    colors[rnd.nextInt(colors.length)],
                    now,
                    2 + rnd.nextInt(4),
                    rnd.nextInt(3)
            ));
        }
        while (PARTICLES.size() > MAX) PARTICLES.removeFirst();
    }

    public static void render(GuiGraphics g, int sw, int sh) {
        long now = System.currentTimeMillis();
        Iterator<Particle> it = PARTICLES.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            long age = now - p.born;
            if (age > LIFE_MS) {
                it.remove();
                continue;
            }
            float t = age / 1000f;
            float x = p.x + p.vx * age * 0.055f;
            float y = p.y + p.vy * age * 0.055f + t * t * 18f;
            float alpha = 1f - Mth.clamp(age / (float) LIFE_MS, 0f, 1f);
            int a = (int) (alpha * 220);
            int c = (a << 24) | (p.color & 0xFFFFFF);
            int s = p.size;
            if (p.kind == 1) {
                g.fill((int) x, (int) y, (int) x + s + 2, (int) y + Math.max(1, s / 2), c);
            } else if (p.kind == 2) {
                g.fill((int) x, (int) y, (int) x + 1, (int) y + s + 1, c);
                g.fill((int) x - 1, (int) y + 1, (int) x + 2, (int) y + 2, c);
            } else {
                g.fill((int) x, (int) y, (int) x + s, (int) y + s, c);
            }
        }
    }

    public static void clear() {
        PARTICLES.clear();
    }
}
