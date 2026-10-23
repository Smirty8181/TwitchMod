package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/** Узкая лента саб / фоллоу / рейд / первое сообщение — отдельно от !команд. */
public final class HypeTickerOverlay {
    private static final long SLIDE_MS = 380;
    private static final long QUIET_TTL_MS = 6_500;
    private static final long BUSY_TTL_MS = 4_000;
    private static final long FLOOD_TTL_MS = 2_600;

    private static final class Entry {
        final String user;
        final String line;
        final String kind;
        final int nickColor;
        final long timeMs;
        float displaySlot;
        float targetSlot;

        Entry(String user, String line, String kind, int nickColor, long timeMs) {
            this.user = user;
            this.line = line;
            this.kind = kind;
            this.nickColor = nickColor;
            this.timeMs = timeMs;
            this.displaySlot = -0.8f;
            this.targetSlot = 0f;
        }
    }

    private static final Deque<Entry> ENTRIES = new ArrayDeque<>();

    private HypeTickerOverlay() {}

    public static boolean isHype(String commandId) {
        if (commandId == null) return false;
        return "follow".equals(commandId) || "sub".equals(commandId)
                || "raid".equals(commandId) || "firstmsg".equals(commandId)
                || "miss".equals(commandId);
    }

    public static void push(String user, String line, String kind, int nickColor) {
        if (!HudResolver.enabled(HudResolver.Element.COMMAND_FEED)) return;
        Entry neu = new Entry(user, line, kind, nickColor, System.currentTimeMillis());
        ENTRIES.addFirst(neu);
        trimToCap(System.currentTimeMillis());
        reindex();
        HudUiSounds.feedTick();
    }

    private static void reindex() {
        int i = 0;
        for (Entry e : ENTRIES) e.targetSlot = i++;
    }

    public static int occupiedHeight() {
        if (ENTRIES.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        int vis = 0;
        for (Entry e : ENTRIES) {
            if (alphaFor(e, now) > 0.02f) vis++;
        }
        if (vis <= 0) return 0;
        float es = HudResolver.elementScale(HudResolver.Element.COMMAND_FEED);
        int pitch = HudLayout.s(22, es) + HudRails.gap();
        return vis * pitch - HudRails.gap();
    }

    public static void render(GuiGraphics g, float delta) {
        if (ENTRIES.isEmpty()) return;
        if (!HudResolver.enabled(HudResolver.Element.COMMAND_FEED)) return;

        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc)) return;

        float es = HudResolver.elementScale(HudResolver.Element.COMMAND_FEED);
        float opacity = HudResolver.opacity(HudResolver.Element.COMMAND_FEED);
        long now = System.currentTimeMillis();
        trimToCap(now);
        reindex();
        if (ENTRIES.isEmpty()) return;

        var el = HudResolver.el(HudResolver.Element.COMMAND_FEED);
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int boxW = HudRails.activityW();
        int boxH = HudLayout.s(22, es);
        int gap = HudRails.gap();
        int pitch = boxH + gap;
        int slidePx = HudLayout.s(22, es);
        int visible = 0;
        for (Entry e : ENTRIES) {
            if (alphaFor(e, now) * opacity > 0.02f) visible++;
        }
        int stackH = Math.max(boxH, visible * pitch - gap);
        int feedH = CommandFeedOverlay.occupiedHeight();
        int join = feedH > 0 ? gap : 0;
        int combinedH = stackH + join + feedH;
        boolean bottom = HudPlacement.isBottomStack(el.anchor);
        int padBottom = bottom ? HudRails.hotbar() : 0;
        HudPlacement.Rect pos = HudPlacement.place(el.anchor, el.offsetX, el.offsetY + padBottom,
                screenW, screenH, boxW, combinedH, es);

        int index = 0;
        for (Entry entry : ENTRIES) {
            entry.displaySlot = HudAnim.approach(entry.displaySlot, entry.targetSlot, 0.3f);
            float alpha = alphaFor(entry, now) * opacity;
            if (alpha <= 0.02f) continue;

            float slide = HudAnim.easeOutBack(Mth.clamp((now - entry.timeMs) / (float) SLIDE_MS, 0f, 1f));
            int accent = accentFor(entry.kind);
            int x = HudPlacement.isRightStack(el.anchor)
                    ? (int) (pos.x() + slidePx - slide * slidePx)
                    : (int) (pos.x() - slidePx + slide * slidePx);
            int y = pos.y() + Math.round(entry.displaySlot * pitch);
            drawRow(g, mc, x, y, boxW, boxH, entry, accent, alpha * (1f - index * 0.08f), es);
            index++;
        }
    }

    private static void drawRow(GuiGraphics g, Minecraft mc, int x, int y, int w, int h,
                                Entry entry, int accent, float alpha, float es) {
        HudCard.drawPremium(g, x, y, w, h, accent, alpha, 1f, true, entry.timeMs);
        int pad = HudLayout.s(5, es);
        int icon = HudLayout.s(12, es);
        CommandIcons.draw(g, entry.kind, x + pad, y + (h - icon) / 2, icon, accent, alpha, false);

        int textX = x + pad + icon + HudLayout.s(4, es);
        String badge = badgeFor(entry.kind);
        int badgeW = mc.font.width(badge) + HudLayout.s(12, es);
        HudCard.drawBadge(g, mc.font, x + w - badgeW - HudLayout.s(5, es), y + (h - HudLayout.s(11, es)) / 2,
                badge, accent, alpha);

        HudAvatars.drawName(g, mc.font, textX, y + HudLayout.s(3, es), entry.user,
                entry.nickColor, alpha, 11);
        String line = entry.line == null ? "" : entry.line;
        if (line.length() > 18) line = line.substring(0, 17) + "…";
        g.drawString(mc.font, "§7" + line, textX, y + HudLayout.s(12, es),
                ((int) (alpha * 255) << 24) | 0xC8_C0_D0, false);
    }

    private static String badgeFor(String kind) {
        return switch (kind == null ? "" : kind) {
            case "sub" -> "САБ";
            case "follow" -> "ФОЛЛ";
            case "raid" -> "РЕЙД";
            case "firstmsg" -> "ЧАТ";
            case "miss" -> "ПРОМАХ";
            default -> "ТВИЧ";
        };
    }

    private static int accentFor(String kind) {
        return switch (kind == null ? "" : kind) {
            case "follow" -> HudResolver.goodColor(0xFF_E8_7A_D0);
            case "sub" -> HudResolver.goodColor(0xFF_C4_5A_E8);
            case "raid" -> HudResolver.accent(HudResolver.Element.COMMAND_FEED, 0xFF_FF_5A_4A);
            case "firstmsg" -> HudResolver.accent(HudResolver.Element.COMMAND_FEED, 0xFF_F0_C0_40);
            case "miss" -> 0xFF_F1_C4_0F;
            default -> TwitchUi.PURPLE;
        };
    }

    private static int countNewerThan(long now, long windowMs) {
        int n = 0;
        for (Entry e : ENTRIES) {
            if (now - e.timeMs < windowMs) n++;
        }
        return n;
    }

    private static int pressure(long now) {
        return HudFeedPressure.level(ENTRIES.size(), countNewerThan(now, 2000), countNewerThan(now, 5000));
    }

    private static long ttlMs(Entry entry, long now) {
        int idx = Math.max(0, Math.round(entry.targetSlot));
        return HudFeedPressure.ttlMs(pressure(now), idx, QUIET_TTL_MS, BUSY_TTL_MS, FLOOD_TTL_MS);
    }

    private static void trimToCap(long now) {
        prune(now);
        int max = HudFeedPressure.cap(3, pressure(now), 2, 2, 1);
        while (ENTRIES.size() > max) ENTRIES.removeLast();
    }

    private static void prune(long now) {
        int i = 0;
        int p = pressure(now);
        Iterator<Entry> it = ENTRIES.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            long ttl = HudFeedPressure.ttlMs(p, i, QUIET_TTL_MS, BUSY_TTL_MS, FLOOD_TTL_MS);
            if (now - e.timeMs > ttl) it.remove();
            else i++;
        }
    }

    private static float alphaFor(Entry entry, long now) {
        long age = now - entry.timeMs;
        long ttl = ttlMs(entry, now);
        long fadeStart = Math.max(900L, (long) (ttl * 0.55f));
        long fadeDur = Math.max(350L, ttl - fadeStart);
        float in = HudAnim.easeOutCubic(Mth.clamp(age / 240f, 0f, 1f));
        float out = age > fadeStart
                ? 1f - Mth.clamp((age - fadeStart) / (float) fadeDur, 0f, 1f) : 1f;
        return in * out;
    }
}
