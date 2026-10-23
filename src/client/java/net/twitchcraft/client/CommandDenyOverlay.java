package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Слева снизу: почему команда не прошла.
 * Та же плашка и анимация, что у ленты успешных команд.
 */
public final class CommandDenyOverlay {
    private static final long SLIDE_MS = 420;
    private static final long QUIET_TTL_MS = 5_000;
    private static final long BUSY_TTL_MS = 3_400;
    private static final long FLOOD_TTL_MS = 2_200;
    private static final int ACCENT = 0xFF_F0_A0_3C;

    private static final class Entry {
        final String user;
        final String commandRu;
        final String reasonId;
        final int seconds;
        final int nickColor;
        final long timeMs;
        float displaySlot;
        float targetSlot;

        Entry(String user, String commandRu, String reasonId, int seconds, int nickColor, long timeMs) {
            this.user = user;
            this.commandRu = commandRu;
            this.reasonId = reasonId;
            this.seconds = seconds;
            this.nickColor = nickColor;
            this.timeMs = timeMs;
            this.displaySlot = -0.9f;
            this.targetSlot = 0f;
        }
    }

    private static final Deque<Entry> ENTRIES = new ArrayDeque<>();

    private CommandDenyOverlay() {}

    public static void push(String user, String commandRu, String reasonId, int seconds, int nickColor) {
        if (!HudResolver.enabled(HudResolver.Element.COMMAND_DENY)) return;
        Entry neu = new Entry(user, commandRu, reasonId, seconds, nickColor, System.currentTimeMillis());
        ENTRIES.addFirst(neu);
        trimToCap(System.currentTimeMillis());
        reindex();
        HudUiSounds.denyTick();
    }

    private static void reindex() {
        int i = 0;
        for (Entry e : ENTRIES) e.targetSlot = i++;
    }

    public static void render(GuiGraphics g, float delta) {
        if (ENTRIES.isEmpty()) return;
        if (!HudResolver.enabled(HudResolver.Element.COMMAND_DENY)) return;

        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc)) return;

        float es = HudResolver.elementScale(HudResolver.Element.COMMAND_DENY);
        float opacity = HudResolver.opacity(HudResolver.Element.COMMAND_DENY);
        int accent = HudResolver.accent(HudResolver.Element.COMMAND_DENY, ACCENT);

        long now = System.currentTimeMillis();
        trimToCap(now);
        reindex();

        var el = HudResolver.el(HudResolver.Element.COMMAND_DENY);
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int index = 0;
        int boxW = HudRails.activityW();
        int boxH = HudLayout.s(42, es);
        int gap = HudRails.gap();
        int pitch = boxH + gap;
        int slidePx = HudLayout.s(28, es);
        int visible = 0;
        for (Entry e : ENTRIES) {
            if (alphaFor(e, now) * opacity > 0.02f) visible++;
        }
        int stackH = Math.max(boxH, visible * pitch - gap);
        var feedEl = HudResolver.el(HudResolver.Element.COMMAND_FEED);
        boolean sameCorner = feedEl.anchor != null && el.anchor != null
                && feedEl.anchor.equalsIgnoreCase(el.anchor);
        int feedH = sameCorner
                ? CommandFeedOverlay.occupiedHeight() + HypeTickerOverlay.occupiedHeight()
                + (CommandFeedOverlay.occupiedHeight() > 0 && HypeTickerOverlay.occupiedHeight() > 0
                ? HudRails.gap() : 0)
                : 0;
        boolean bottom = HudPlacement.isBottomStack(el.anchor);
        int padBottom = (bottom ? HudRails.hotbar() : 0) + feedH;
        HudPlacement.Rect pos = HudPlacement.place(el.anchor, el.offsetX, el.offsetY + padBottom,
                screenW, screenH, boxW, stackH, es);

        for (Entry entry : ENTRIES) {
            entry.displaySlot = HudAnim.approach(entry.displaySlot, entry.targetSlot, 0.28f);
            float alpha = alphaFor(entry, now) * opacity;
            if (alpha <= 0.02f) continue;

            float slide = slideFor(entry, now);
            int shake = Math.round(shakeFor(entry, now));
            int x = HudPlacement.isRightStack(el.anchor)
                    ? (int) (pos.x() + slidePx - slide * slidePx)
                    : (int) (pos.x() - slidePx + slide * slidePx);
            x += shake;
            int y = bottom
                    ? pos.y() + stackH - boxH - Math.round(entry.displaySlot * pitch)
                    : pos.y() + Math.round(entry.displaySlot * pitch);

            float pop = popScale(entry, now);
            int cardAccent = accentForReason(entry.reasonId, accent);
            drawCard(g, mc, x, y, boxW, boxH, entry, cardAccent, alpha * (1f - index * 0.07f), es, now, pop);
            index++;
        }
    }

    private static void drawCard(GuiGraphics g, Minecraft mc, int x, int y, int w, int h,
                                 Entry entry, int accent, float alpha, float es, long now, float pop) {
        HudCard.drawPremium(g, x, y, w, h, accent, alpha, pop, true, now);

        int pad = HudLayout.s(8, es);
        int av = HudLayout.s(22, es);
        int icon = HudLayout.s(14, es);
        int nameY = y + HudLayout.s(5, es);
        int avX = x + pad;
        int avY = y + (h - av) / 2;
        if (entry.seconds > 0) {
            float ratio = 1f - Mth.clamp((now - entry.timeMs) / (entry.seconds * 1000f), 0f, 1f);
            HudCard.drawCooldownRing(g, avX + av / 2, avY + av / 2,
                    av / 2 + 3, 2, ratio, accent, alpha);
        }
        HudAvatars.draw(g, mc.font, avX, avY, av, entry.user, alpha, false);
        int textX = avX + av + HudLayout.s(8, es);

        String badge = badgeFor(entry.reasonId);
        int badgeW = mc.font.width(badge) + HudLayout.s(14, es);
        int badgeX = x + w - badgeW - HudLayout.s(7, es);
        HudCard.drawBadge(g, mc.font, badgeX, y + HudLayout.s(5, es), badge, accent, alpha);

        String nick = truncate(entry.user, 12);
        while (nick.length() > 3 && textX + HudAvatars.nameWidth(mc.font, nick, 12) > badgeX - 4) {
            nick = nick.substring(0, nick.length() - 1);
        }
        HudAvatars.drawName(g, mc.font, textX, nameY, nick, entry.nickColor, alpha, 12);

        int stripY = y + HudLayout.s(20, es);
        int stripH = HudLayout.s(15, es);
        CommandIcons.draw(g, CommandIcons.spriteId(entry.reasonId), textX, stripY + (stripH - icon) / 2,
                icon, accent, alpha, false);
        int cmdX = textX + icon + HudLayout.s(4, es);
        int stripW = Math.max(HudLayout.s(48, es), w - (cmdX - x) - HudLayout.s(7, es));
        HudCard.drawCommandStrip(g, cmdX, stripY, stripW, stripH, accent, alpha);

        int innerPad = HudLayout.s(5, es);
        int textRight = cmdX + stripW - innerPad;
        int leftSec = secondsLeft(entry, now);
        String time = leftSec > 0 ? leftSec + "с" : "";
        int timeW = time.isEmpty() ? 0 : mc.font.width(time) + HudLayout.s(4, es);
        if (!time.isEmpty()) {
            g.drawString(mc.font, time, textRight - mc.font.width(time),
                    stripY + Math.max(2, (stripH - 8) / 2),
                    ((int) (alpha * 255) << 24) | 0xF5_D0_6E, false);
        }
        String reason = fit(mc.font, reasonLine(entry), stripW - innerPad * 2 - timeW);
        int cmdY = stripY + Math.max(2, (stripH - 8) / 2);
        g.drawString(mc.font, reason, cmdX + innerPad, cmdY,
                ((int) (alpha * 255) << 24) | 0xE8_E0_D4, false);

        float life = 1f - Mth.clamp((now - entry.timeMs) / (float) ttlMs(entry, now), 0f, 1f);
        HudCard.drawProgressBar(g, x + HudLayout.s(8, es), y + h - HudLayout.s(4, es),
                w - HudLayout.s(16, es), HudLayout.s(3, es), life, accent, alpha * 0.95f);
    }

    private static int secondsLeft(Entry e, long now) {
        if (e.seconds <= 0) return 0;
        return Math.max(0, e.seconds - (int) ((now - e.timeMs) / 1000L));
    }

    private static String fit(net.minecraft.client.gui.Font font, String text, int maxW) {
        if (text == null) return "";
        if (maxW < 8 || font.width(text) <= maxW) return text;
        String s = text;
        while (s.length() > 1 && font.width(s + "…") > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }

    private static String reasonLine(Entry e) {
        String base = switch (e.reasonId == null ? "" : e.reasonId) {
            case "spam" -> "кд сообщения";
            case "cooldown" -> "кд команды";
            case "rate" -> "слишком часто";
            case "disabled" -> "команда выключена";
            case "karma" -> "сначала напиши добро";
            case "shield" -> "щит стримера";
            case "hold" -> "держись — щит брата";
            case "evil_block" -> "урон заблокирован";
            case "evil_pace" -> "чат уже бил";
            case "pause" -> "чат на паузе";
            case "hardcore" -> "хардкор — нельзя";
            case "whitelist" -> "не в вайтлисте";
            case "offline" -> "стример оффлайн";
            case "active" -> "уже активно";
            case "help" -> "сначала помоги стримеру";
            default -> "команда не прошла";
        };
        if (e.seconds > 0 && ("spam".equals(e.reasonId) || "cooldown".equals(e.reasonId)
                || "shield".equals(e.reasonId) || "hold".equals(e.reasonId)
                || "evil_block".equals(e.reasonId)
                || "evil_pace".equals(e.reasonId) || "pause".equals(e.reasonId)
                || "active".equals(e.reasonId))) {
            return base;
        }
        if (!e.commandRu.isBlank() && !"spam".equals(e.reasonId) && !"cooldown".equals(e.reasonId)) {
            return base + " · !" + truncate(e.commandRu, 14);
        }
        return base;
    }

    private static String badgeFor(String reasonId) {
        return switch (reasonId == null ? "" : reasonId) {
            case "spam" -> "СООБЩ";
            case "cooldown" -> "КД";
            case "rate" -> "ЛИМИТ";
            case "shield" -> "ЩИТ";
            case "hold" -> "ДЕРЖИ";
            case "karma" -> "КАРМА";
            case "evil_block" -> "БЛОК";
            case "evil_pace" -> "ПАУЗА";
            case "pause" -> "СТОП";
            default -> "НЕТ";
        };
    }

    private static int accentForReason(String reasonId, int fallback) {
        return switch (reasonId == null ? "" : reasonId) {
            case "shield" -> 0xFF_F1_C4_0F;
            case "hold" -> 0xFF_F1_C4_0F;
            case "cooldown", "spam", "rate", "evil_pace" -> 0xFF_F0_A0_3C;
            case "evil_block" -> 0xFF_C0_39_2B;
            default -> fallback;
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

    private static float slideFor(Entry entry, long now) {
        return HudAnim.easeOutBack(Mth.clamp((now - entry.timeMs) / (float) SLIDE_MS, 0f, 1f));
    }

    private static float alphaFor(Entry entry, long now) {
        long age = now - entry.timeMs;
        long ttl = ttlMs(entry, now);
        long fadeStart = Math.max(800L, (long) (ttl * 0.52f));
        long fadeDur = Math.max(350L, ttl - fadeStart);
        float in = HudAnim.easeOutCubic(Mth.clamp(age / 220f, 0f, 1f));
        float out = age > fadeStart
                ? 1f - Mth.clamp((age - fadeStart) / (float) fadeDur, 0f, 1f) : 1f;
        return in * out;
    }

    private static float popScale(Entry entry, long now) {
        long age = now - entry.timeMs;
        if (age > 480) return 1f;
        float t = HudAnim.easeOutCubic(age / 480f);
        return 1f + 0.06f * (float) Math.sin(t * Math.PI);
    }

    private static float shakeFor(Entry entry, long now) {
        long age = now - entry.timeMs;
        if (age > 280) return 0f;
        float t = 1f - age / 280f;
        return (float) (Math.sin(age / 18.0) * 2.2 * t);
    }

    private static String truncate(String s, int max) {
        if (s == null || s.isBlank()) return "?";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
