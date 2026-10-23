package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.twitchcraft.network.HotbarShufflePayload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Полёт предметов по хотбару: shuffle — дуги слот→слот,
 * обмен — свои всасываются в портал, чужие вылетают в слоты.
 */
public final class HotbarShuffleOverlay {
    private static final ResourceLocation HOTBAR =
            ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation HOTBAR_SEL =
            ResourceLocation.withDefaultNamespace("hud/hotbar_selection");
    private static final ResourceLocation OFFHAND_L =
            ResourceLocation.withDefaultNamespace("hud/hotbar_offhand_left");
    private static final ResourceLocation OFFHAND_R =
            ResourceLocation.withDefaultNamespace("hud/hotbar_offhand_right");

    private static int mode;
    private static ItemStack[] outgoing;
    private static ItemStack[] incoming;
    private static int[] dest;
    private static long startMs;
    private static long durMs;
    private static int selectedSlot;

    private HotbarShuffleOverlay() {}

    public static void start(HotbarShufflePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || payload == null) return;
        mode = payload.mode();
        dest = new int[9];
        outgoing = new ItemStack[9];
        incoming = new ItemStack[9];
        int[] d = payload.dest();
        ItemStack[] in = payload.incoming();
        for (int i = 0; i < 9; i++) {
            outgoing[i] = mc.player.getInventory().getItem(i).copy();
            dest[i] = d != null && i < d.length ? Mth.clamp(d[i], 0, 8) : i;
            incoming[i] = in != null && i < in.length && in[i] != null ? in[i].copy() : ItemStack.EMPTY;
        }
        selectedSlot = mc.player.getInventory().getSelectedSlot();
        startMs = System.currentTimeMillis();
        durMs = HotbarShufflePayload.clientDurationMs(mode);
    }

    public static boolean isActive() {
        return outgoing != null && System.currentTimeMillis() - startMs < durMs;
    }

    public static void render(GuiGraphics g) {
        if (outgoing == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        long age = System.currentTimeMillis() - startMs;
        if (age >= durMs) {
            clear();
            return;
        }

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int left = w / 2 - 91;
        int top = h - 22;
        int portalX = left + 91 - 8;
        int portalY = top - 18;

        blitHotbarChrome(g, mc.player, left, top, w);

        if (mode == HotbarShufflePayload.MODE_SWAP) {
            renderSwap(g, mc, left, top, portalX, portalY, age);
        } else {
            renderShuffle(g, mc, left, top, age);
        }
    }

    private static void renderShuffle(GuiGraphics g, Minecraft mc, int left, int top, long age) {
        List<Fly> flies = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = outgoing[i];
            if (stack == null || stack.isEmpty()) continue;
            float delay = i * 32f;
            float raw = Mth.clamp((age - delay) / 820f, 0f, 1f);
            if (age < delay) raw = 0f;
            float t = HudAnim.easeInOutCubic(raw);
            int fromX = left + 3 + i * 20;
            int toX = left + 3 + dest[i] * 20;
            int fromY = top + 3;
            int toY = top + 3;
            int span = Math.abs(toX - fromX);
            float hop = span == 0 ? 6f : 18f + span * 0.28f + (i % 2) * 14f;
            if ((i / 2) % 2 == 1) hop += 8f;
            float bx = quad(fromX, (fromX + toX) * 0.5f, toX, t);
            float by = quad(fromY, fromY - hop, toY, t);
            float squash = t > 0.86f ? 1f - (t - 0.86f) / 0.14f * 0.16f : 1f;
            float pulse = 1f + (float) Math.sin(t * Math.PI) * 0.16f;
            flies.add(new Fly(stack, bx, by, pulse, squash, 1f, t, fromX, fromY));
        }
        flies.sort(Comparator.comparingDouble((Fly f) -> f.y).reversed());
        for (Fly f : flies) {
            drawTrail(g, f.fromX, f.fromY, Math.round(f.x), Math.round(f.y), f.t, 0xF5_D0_6E);
            drawFlyingItem(g, mc, f.stack, f.x, f.y, f.scaleX, f.scaleY, f.alpha);
        }
    }

    private static void renderSwap(GuiGraphics g, Minecraft mc, int left, int top,
                                   int portalX, int portalY, long age) {
        float portalOpen = Mth.clamp(age / 220f, 0f, 1f);
        float portalHold = Mth.clamp((durMs - 80 - age) / 220f, 0f, 1f);
        float portal = Math.min(portalOpen, portalHold);
        drawPortal(g, portalX + 8, portalY + 8, portal, age);

        List<Fly> flies = new ArrayList<>(18);

        for (int i = 0; i < 9; i++) {
            ItemStack stack = outgoing[i];
            if (stack == null || stack.isEmpty()) continue;
            float delay = i * 24f;
            float raw = Mth.clamp((age - delay) / 420f, 0f, 1f);
            if (raw <= 0f) {
                flies.add(new Fly(stack, left + 3 + i * 20, top + 3, 1f, 1f, 1f, 0f,
                        left + 3 + i * 20, top + 3));
                continue;
            }
            float t = HudAnim.easeInCubic(raw);
            int fromX = left + 3 + i * 20;
            int fromY = top + 3;
            float spin = t * 4.2f + i * 0.55f;
            float dist = 1f - t;
            float x = portalX + (fromX - portalX) * dist + (float) Math.cos(spin) * 10f * dist;
            float y = portalY + (fromY - portalY) * dist - t * 8f + (float) Math.sin(spin) * 6f * dist;
            float s = 1f - t * 0.72f;
            float a = 1f - t * 0.92f;
            if (a > 0.04f) {
                flies.add(new Fly(stack, x, y, s, s, a, t, fromX, fromY));
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = incoming[i];
            if (stack == null || stack.isEmpty()) continue;
            float delay = 380f + i * 30f;
            float raw = Mth.clamp((age - delay) / 780f, 0f, 1f);
            if (raw <= 0f) continue;
            float t = HudAnim.easeOutBack(raw);
            int toX = left + 3 + i * 20;
            int toY = top + 3;
            float spin = (1f - Mth.clamp(raw, 0f, 1f)) * 3.4f + i * 0.4f;
            float x = portalX + (toX - portalX) * t + (float) Math.cos(spin) * 8f * (1f - Mth.clamp(raw, 0f, 1f));
            float y = portalY + (toY - portalY) * t
                    - (float) Math.sin(Mth.clamp(raw, 0f, 1f) * Math.PI) * 22f;
            float s = 0.42f + 0.58f * Mth.clamp(raw, 0f, 1f);
            float squash = raw > 0.88f ? 1f - (raw - 0.88f) / 0.12f * 0.14f : 1f;
            flies.add(new Fly(stack, x, y, s, s * squash, 0.4f + 0.6f * Mth.clamp(raw, 0f, 1f),
                    raw, portalX, portalY));
        }

        flies.sort(Comparator.comparingDouble((Fly f) -> f.y).reversed());
        for (Fly f : flies) {
            drawTrail(g, f.fromX, f.fromY, Math.round(f.x), Math.round(f.y), f.t, 0xC4_5A_E8);
            drawFlyingItem(g, mc, f.stack, f.x, f.y, f.scaleX, f.scaleY, f.alpha);
        }
    }

    private static void drawPortal(GuiGraphics g, int cx, int cy, float open, long age) {
        if (open < 0.05f) return;
        float pulse = 0.82f + 0.18f * (float) Math.sin(age / 70.0);
        int r = Math.round((7f + open * 9f) * pulse);
        int aCore = (int) (open * 110);
        int aRing = (int) (open * 70);
        g.fill(cx - r - 2, cy - r - 2, cx + r + 2, cy + r + 2, (aRing / 2 << 24) | 0xF5_D0_6E);
        g.fill(cx - r, cy - r, cx + r, cy + r, (aCore << 24) | 0xC4_5A_E8);
        g.fill(cx - r + 3, cy - r + 3, cx + r - 3, cy + r - 3, (aCore << 24) | 0x2A_14_38);
        int spark = 4;
        for (int s = 0; s < spark; s++) {
            double ang = age / 90.0 + s * (Math.PI * 2 / spark);
            int sx = cx + (int) (Math.cos(ang) * (r + 2));
            int sy = cy + (int) (Math.sin(ang) * (r + 2));
            g.fill(sx - 1, sy - 1, sx + 2, sy + 2, (aRing << 24) | 0xF5_D0_6E);
        }
    }

    private static float quad(float a, float b, float c, float t) {
        float u = 1f - t;
        return u * u * a + 2f * u * t * b + t * t * c;
    }

    private static void drawTrail(GuiGraphics g, int x0, int y0, int x1, int y1, float t, int rgb) {
        if (t < 0.1f || t > 0.9f) return;
        int steps = 5;
        for (int s = 1; s <= steps; s++) {
            float u = s / (float) (steps + 1);
            int x = Math.round(x0 + (x1 - x0) * (1f - u * 0.55f));
            int y = Math.round(y0 + (y1 - y0) * (1f - u * 0.55f));
            int a = (int) ((1f - u) * 48 * (1f - Math.abs(t - 0.5f) * 0.7f));
            if (a < 8) continue;
            int sz = 3 + (int) ((1f - u) * 5);
            g.fill(x + 8 - sz / 2, y + 8 - sz / 2, x + 8 + sz / 2, y + 8 + sz / 2, (a << 24) | (rgb & 0xFFFFFF));
        }
    }

    private static void drawFlyingItem(GuiGraphics g, Minecraft mc, ItemStack stack,
                                       float x, float y, float scaleX, float scaleY, float alpha) {
        if (alpha < 0.05f || scaleX < 0.08f) return;
        int glow = (int) (alpha * 55);
        int ix = Math.round(x);
        int iy = Math.round(y);
        g.fill(ix - 1, iy - 1, ix + 17, iy + 17, (glow << 24) | 0xF5_D0_6E);
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x + 8f, y + 8f);
        pose.scale(Math.max(0.08f, scaleX), Math.max(0.08f, scaleY));
        pose.translate(-8f, -8f);
        g.renderItem(stack, 0, 0);
        if (alpha > 0.5f && scaleX > 0.55f) {
            g.renderItemDecorations(mc.font, stack, 0, 0);
        }
        pose.popMatrix();
    }

    private static void blitHotbarChrome(GuiGraphics g, LocalPlayer player, int left, int top, int screenW) {
        blitSprite(g, HOTBAR, left, top, 182, 22);
        int sel = Mth.clamp(selectedSlot, 0, 8);
        blitSprite(g, HOTBAR_SEL, left - 1 + sel * 20, top - 1, 24, 23);

        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty()) {
            HumanoidArm arm = player.getMainArm().getOpposite();
            int mid = screenW / 2;
            int ox;
            int oy = top - 1;
            if (arm == HumanoidArm.LEFT) {
                blitSprite(g, OFFHAND_L, mid - 91 - 29, oy, 29, 24);
                ox = mid - 91 - 26;
            } else {
                blitSprite(g, OFFHAND_R, mid + 91, oy, 29, 24);
                ox = mid + 91 + 10;
            }
            g.renderItem(off, ox, top + 3);
            g.renderItemDecorations(Minecraft.getInstance().font, off, ox, top + 3);
        }
    }

    private static void blitSprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h) {
        try {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h);
        } catch (Exception ignored) {
            HudCard.drawPremium(g, x - 2, y - 2, w + 4, h + 4, 0xFF_C4_5A_E8, 0.92f, 1f, false, 0);
        }
    }

    private static void clear() {
        outgoing = null;
        incoming = null;
        dest = null;
    }

    private record Fly(ItemStack stack, float x, float y, float scaleX, float scaleY,
                       float alpha, float t, int fromX, int fromY) {}
}
