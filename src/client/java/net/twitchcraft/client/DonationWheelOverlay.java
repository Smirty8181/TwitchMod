package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.twitchcraft.donation.DonationWheelService;

import java.util.ArrayList;
import java.util.List;

/**
 * Донат-рулетка и колесо хаоса — горизонтальный барабан.
 * Центр = результат. Индексы сегментов/иконок/лейблов всегда совпадают.
 */
public final class DonationWheelOverlay {
    private static final float FULL_LOOPS = 6f;

    private static String donor = "";
    private static String amount = "";
    private static int tierColor = TwitchUi.PURPLE;
    private static List<String> segments = List.of();
    private static List<String> segmentIds = List.of();
    private static List<Integer> segmentColors = List.of();
    private static int winnerIndex;
    private static String winnerLabel = "";
    private static long startMs;
    private static long showUntil;
    private static boolean active;
    private static boolean confettiDone;

    public static final long SPIN_MS = DonationWheelService.SPIN_MS;
    public static final long HOLD_MS = DonationWheelService.HOLD_MS;
    private static final long TOTAL_MS = SPIN_MS + HOLD_MS;
    private static final int SLOTS = 5;

    private static final int GOLD = 0xFF_F5_D0_6E;
    private static final int GOLD_DARK = 0xFF_8B_69_14;
    private static final int CHAOS_RED = 0xFF_E7_4C_3C;

    private DonationWheelOverlay() {}

    public static boolean isActive() {
        return active && System.currentTimeMillis() <= showUntil;
    }

    public static void start(String donorName, String amountText, String currency, int color,
                             List<String> segmentLabels, List<String> commandIds, List<Integer> colors,
                             int winIndex, String winLabel) {
        donor = donorName != null ? donorName : "";
        TwitchAvatarCache.request(donor);
        String cur = currency != null ? currency.trim() : "";
        if ("RUB".equalsIgnoreCase(cur) || "RUR".equalsIgnoreCase(cur)) cur = "₽";
        amount = amountText + (cur.isEmpty() ? "" : " " + cur);
        tierColor = color != 0 ? color : TwitchUi.PURPLE;

        segments = padStrings(segmentLabels, SLOTS, "Приз");
        segmentIds = padStrings(commandIds, SLOTS, "event").stream().map(s -> s.toLowerCase()).toList();
        segmentColors = padColors(colors, SLOTS);
        winnerIndex = Mth.clamp(winIndex, 0, SLOTS - 1);
        winnerLabel = segments.get(winnerIndex);
        if (winLabel != null && !winLabel.isBlank()) {
            winnerLabel = winLabel;
            segments = replaceAt(segments, winnerIndex, winnerLabel);
        }

        startMs = System.currentTimeMillis();
        showUntil = startMs + TOTAL_MS;
        active = true;
        confettiDone = false;
        ConfettiFx.clear();
    }

    public static void render(GuiGraphics g, float delta) {
        TwitchTextureLoader.ensureGuiTextures();

        long now = System.currentTimeMillis();
        if (!active || now > showUntil) {
            active = false;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc)) return;

        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        long elapsed = now - startMs;
        boolean done = elapsed >= SPIN_MS;
        float spinT = Mth.clamp(elapsed / (float) SPIN_MS, 0f, 1f);
        float travel = FULL_LOOPS * SLOTS + winnerIndex;
        float pos = done ? travel : travel * HudAnim.easeOutQuint(spinT);

        g.fill(0, 0, sw, sh, 0xB8_04_03_08);

        drawHeader(g, font, sw);
        drawReel(g, font, sw, sh, pos, done, elapsed);

        if (done) {
            if (!confettiDone) {
                ConfettiFx.burst(sw / 2, sh / 2 - HudLayout.s(20), 16);
                confettiDone = true;
                HudUiSounds.wheelStop();
            }
            ConfettiFx.render(g, sw, sh);
            drawResult(g, font, sw, sh, elapsed);
        } else {
            drawSpinHint(g, font, sw, sh, spinT);
        }
    }

    private static void drawHeader(GuiGraphics g, Font font, int sw) {
        int w = Math.min(HudLayout.s(320), sw - 24);
        int x = (sw - w) / 2;
        int y = HudLayout.s(14);
        int h = HudLayout.s(36);
        boolean chaos = isChaosWheel();
        int accent = themeAccent();

        HudCard.drawPremium(g, x, y, w, h, accent, 1f, 1f, true, System.currentTimeMillis());

        int av = HudLayout.s(22);
        int avX = x + HudLayout.s(8);
        int avY = y + (h - av) / 2;
        HudAvatars.draw(g, font, avX, avY, av, chaos ? "ХАОС" : donor, 1f, !chaos);

        int textX = avX + av + HudLayout.s(7);
        String title = chaos ? "§c§lКОЛЕСО ХАОСА" : "§6§lРУЛЕТКА ДОНАТА";
        g.drawString(font, title, textX, y + HudLayout.s(7), chaos ? 0xFF_FF_6B_6B : GOLD, false);
        String sub = chaos
                ? "§7полоска заполнена — наказание"
                : "§f" + truncate(donor, 16) + " §8· §6" + amount;
        g.drawString(font, sub, textX, y + HudLayout.s(19), TwitchUi.TEXT, false);
    }

    private static boolean isChaosWheel() {
        return "ХАОС".equalsIgnoreCase(donor) || "CHAOS".equalsIgnoreCase(donor);
    }

    private static int themeAccent() {
        return isChaosWheel() ? CHAOS_RED : GOLD;
    }

    private static void drawReel(GuiGraphics g, Font font, int sw, int sh, float pos, boolean done, long elapsed) {
        int cx = sw / 2;
        int cy = sh / 2 - HudLayout.s(8);
        int sideIcon = 48;
        int centerIcon = 64;
        int pitch = HudLayout.s(78);
        int trackW = Math.min(sw - 20, pitch * 3 + HudLayout.s(28));
        int trackH = HudLayout.s(92);
        int tx = cx - trackW / 2;
        int ty = cy - trackH / 2;
        int accent = themeAccent();

        // Трек — одна тёмная полоса, без карточек друг на друге
        g.fill(tx + 3, ty + 3, tx + trackW + 3, ty + trackH + 3, 0x55_00_00_00);
        TwitchUi.fillGradientV(g, tx, ty, tx + trackW, ty + trackH, 0xF4_0A_08_10, 0xF6_06_05_0C);
        TwitchUi.roundedPanel(g, tx, ty, tx + trackW, ty + trackH, 0xF0_0C_0A_12);
        g.fill(tx, ty, tx + trackW, ty + 2, accent);
        g.fill(tx, ty + trackH - 2, tx + trackW, ty + trackH, GOLD_DARK);
        g.fill(tx, ty, tx + 2, ty + trackH, (0x66 << 24) | (accent & 0xFFFFFF));
        g.fill(tx + trackW - 2, ty, tx + trackW, ty + trackH, (0x44 << 24) | (accent & 0xFFFFFF));

        int win = HudLayout.s(76);
        int wx = cx - win / 2;
        int wy = cy - win / 2;

        g.enableScissor(tx + 4, ty + 4, tx + trackW - 4, ty + trackH - 4);
        for (int idx = 0; idx < SLOTS; idx++) {
            float raw = idx - (pos % SLOTS);
            while (raw <= -SLOTS / 2f) raw += SLOTS;
            while (raw > SLOTS / 2f) raw -= SLOTS;
            if (Math.abs(raw) > 1.7f) continue;

            float px = cx + raw * pitch;
            boolean center = Math.abs(raw) < 0.38f || (done && idx == winnerIndex && Math.abs(raw) < 0.55f);
            float dim = center ? 1f : Mth.clamp(1.15f - Math.abs(raw) * 0.42f, 0.35f, 0.85f);
            drawSlot(g, (int) px, cy, center ? centerIcon : sideIcon, idx, center, dim, elapsed);
        }
        g.disableScissor();

        // Рамка победителя поверх — пустая, иконку не затирает
        drawSelectFrame(g, wx, wy, win, win, accent, done);
        drawPointer(g, cx, wy - HudLayout.s(10), accent);

        int shown = done ? winnerIndex : Math.floorMod(Math.round(pos), SLOTS);
        String name = plainLabel(segments.get(shown));
        String label = "§f§l" + name;
        int lw = font.width(TwitchUi.stripFormat(label));
        int ly = ty + trackH + HudLayout.s(8);
        g.drawString(font, label, cx - lw / 2, ly, TwitchUi.TEXT, false);
    }

    private static void drawSlot(GuiGraphics g, int cx, int cy, int icon, int idx,
                                 boolean highlight, float dim, long elapsed) {
        int box = icon + HudLayout.s(highlight ? 8 : 6);
        int x = cx - box / 2;
        int y = cy - box / 2;
        int accent = segmentColor(idx);
        int a = (int) (dim * 255);

        TwitchUi.roundedPanel(g, x, y, x + box, y + box, (Math.max(90, a) << 24) | 0x22_1C_2A);
        g.fill(x + 2, y + 1, x + box - 2, y + 3, (a << 24) | (accent & 0xFFFFFF));

        WheelEffectIcons.draw(g, segmentIds.get(idx), cx, cy, icon, 0, false, elapsed);
        if (dim < 0.9f) {
            g.fill(x, y, x + box, y + box, ((int) ((1f - dim) * 50) << 24));
        }
    }

    private static void drawSelectFrame(GuiGraphics g, int x, int y, int w, int h, int accent, boolean done) {
        int glow = done ? 0x66 : 0x44;
        g.fill(x - 3, y - 3, x + w + 3, y + h + 3, (glow << 24) | (accent & 0xFFFFFF));
        g.fill(x - 1, y - 1, x + w + 1, y + 2, accent);
        g.fill(x - 1, y + h - 2, x + w + 1, y + h + 1, GOLD_DARK);
        g.fill(x - 1, y, x + 2, y + h, accent);
        g.fill(x + w - 2, y, x + w + 1, y + h, GOLD_DARK);
        g.fill(x, y, x + w, y + 1, 0x66_FF_FF_FF);
    }

    private static void drawPointer(GuiGraphics g, int cx, int tipY, int accent) {
        int h = HudLayout.s(12);
        int half = HudLayout.s(9);
        for (int i = 0; i < h; i++) {
            int t = (i * half) / Math.max(1, h - 1);
            g.fill(cx - t, tipY + i, cx + t + 1, tipY + i + 1, i < 2 ? 0xFF_FF_F3_C8 : accent);
        }
        g.fill(cx - 1, tipY + h, cx + 2, tipY + h + HudLayout.s(4), accent);
    }

    private static void drawSpinHint(GuiGraphics g, Font font, int sw, int sh, float spinT) {
        int barW = HudLayout.s(200);
        int bx = (sw - barW) / 2;
        int by = sh - HudLayout.s(36);
        HudCard.drawProgressBar(g, bx, by, barW, 4, HudAnim.easeOutQuint(spinT), themeAccent(), 1f);
        String tip = isChaosWheel() ? "§cКрутим наказание…" : "§6Крутим рулетку…";
        g.drawString(font, tip, sw / 2 - font.width(TwitchUi.stripFormat(tip)) / 2,
                by - HudLayout.s(12), TwitchUi.TEXT_DIM, false);
    }

    private static void drawResult(GuiGraphics g, Font font, int sw, int sh, long elapsed) {
        String title = plainLabel(winnerLabel);
        String cmdId = segmentIds.get(winnerIndex);
        int accent = segmentColor(winnerIndex);

        float pop = Mth.clamp((elapsed - SPIN_MS) / 200f, 0f, 1f);
        float ease = HudAnim.easeOutCubic(pop);

        int icon = 64;
        int pad = HudLayout.s(12);
        int textW = Math.max(font.width(title) + 8, HudLayout.s(130));
        int cardW = Math.min(sw - 24, pad * 2 + icon + HudLayout.s(12) + textW);
        int cardH = HudLayout.s(72);
        int cardX = (sw - cardW) / 2;
        int cardY = sh - HudLayout.s(86) - (int) ((1f - ease) * 14);

        HudCard.drawPremium(g, cardX, cardY, cardW, cardH, accent, 1f, 1f, true, System.currentTimeMillis());
        WheelEffectIcons.draw(g, cmdId, cardX + pad + icon / 2, cardY + cardH / 2, icon, 0, false, elapsed);

        int tx = cardX + pad + icon + HudLayout.s(10);
        g.drawString(font, isChaosWheel() ? "§c§lНАКАЗАНИЕ" : "§6§lВЫПАЛО",
                tx, cardY + HudLayout.s(16), isChaosWheel() ? 0xFF_FF_6B_6B : GOLD, false);
        g.drawString(font, "§f§l" + truncate(title, 28), tx, cardY + HudLayout.s(32), TwitchUi.TEXT, false);
    }

    private static String plainLabel(String label) {
        if (label == null || label.isBlank()) return "Приз";
        String s = label.strip();
        if (!s.isEmpty()) {
            int cp = s.codePointAt(0);
            if (!Character.isLetterOrDigit(cp)) {
                s = s.substring(Character.charCount(cp)).strip();
            }
        }
        return s.isEmpty() ? "Приз" : s;
    }

    private static int segmentColor(int i) {
        if (i >= 0 && i < segmentColors.size()) return segmentColors.get(i);
        return 0xFF_7C_3A_ED;
    }

    private static List<String> padStrings(List<String> src, int n, String fill) {
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String v = (src != null && i < src.size() && src.get(i) != null && !src.get(i).isBlank())
                    ? src.get(i) : fill;
            out.add(v);
        }
        return List.copyOf(out);
    }

    private static List<Integer> padColors(List<Integer> src, int n) {
        int[] defs = {0xFF_7C_3A_ED, 0xFF_25_63_EB, 0xFF_EA_B308, 0xFF_EF_44_44, 0xFF_10_B981};
        List<Integer> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(src != null && i < src.size() && src.get(i) != null ? src.get(i) : defs[i % defs.length]);
        }
        return List.copyOf(out);
    }

    private static List<String> replaceAt(List<String> list, int idx, String value) {
        List<String> copy = new ArrayList<>(list);
        if (idx >= 0 && idx < copy.size()) copy.set(idx, value);
        return List.copyOf(copy);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
