package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.EvilCommands;
import net.twitchcraft.commands.GoodCommands;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/** Лента «зритель → команда» — плавный стек, при флуде не забивает экран. */
public final class CommandFeedOverlay {
    private static final long SLIDE_MS = 420;
    private static final long QUIET_TTL_MS = 7_000;
    private static final long BUSY_TTL_MS = 4_200;
    private static final long FLOOD_TTL_MS = 2_500;

    private static final class Entry {
        final String user;
        final String commandRu;
        final String commandId;
        final int nickColor;
        final int cooldownSec;
        final long timeMs;
        float displaySlot;
        float targetSlot;

        Entry(String user, String commandRu, String commandId, int nickColor, int cooldownSec, long timeMs) {
            this.user = user;
            this.commandRu = commandRu;
            this.commandId = commandId;
            this.nickColor = nickColor;
            this.cooldownSec = cooldownSec;
            this.timeMs = timeMs;
            this.displaySlot = -0.85f;
            this.targetSlot = 0f;
        }
    }

    private static final Deque<Entry> ENTRIES = new ArrayDeque<>();

    private CommandFeedOverlay() {}

    public static void push(String twitchUser, String commandRu, String commandId) {
        push(twitchUser, commandRu, commandId, 0);
    }

    public static void push(String twitchUser, String commandRu, String commandId, int nickColor) {
        push(twitchUser, commandRu, commandId, nickColor, 0);
    }

    public static void push(String twitchUser, String commandRu, String commandId, int nickColor, int cooldownSec) {
        if (!HudResolver.enabled(HudResolver.Element.COMMAND_FEED)) return;
        Entry neu = new Entry(twitchUser, commandRu, commandId, nickColor, cooldownSec, System.currentTimeMillis());
        ENTRIES.addFirst(neu);
        trimToCap(System.currentTimeMillis());
        reindexTargets();
        HudUiSounds.feedTick();
    }

    private static void reindexTargets() {
        int i = 0;
        for (Entry e : ENTRIES) {
            e.targetSlot = i++;
        }
    }

    public static void render(GuiGraphics g, float delta) {
        if (ENTRIES.isEmpty()) return;
        if (!HudResolver.enabled(HudResolver.Element.COMMAND_FEED)) return;

        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc)) return;

        var feed = HudResolver.feedSections();
        float es = HudResolver.elementScale(HudResolver.Element.COMMAND_FEED);
        float opacity = HudResolver.opacity(HudResolver.Element.COMMAND_FEED);

        long now = System.currentTimeMillis();
        trimToCap(now);
        reindexTargets();

        var el = HudResolver.el(HudResolver.Element.COMMAND_FEED);
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int index = 0;
        float stackSpeed = 0.28f;
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
        int tickerH = HypeTickerOverlay.occupiedHeight();
        int join = tickerH > 0 ? gap : 0;
        int combinedH = stackH + join + tickerH;
        boolean bottom = HudPlacement.isBottomStack(el.anchor);
        int padBottom = bottom ? HudRails.hotbar() : 0;
        HudPlacement.Rect pos = HudPlacement.place(el.anchor, el.offsetX, el.offsetY + padBottom,
                screenW, screenH, boxW, combinedH, es);
        int feedY0 = pos.y() + (tickerH > 0 ? tickerH + join : 0);

        for (Entry entry : ENTRIES) {
            entry.displaySlot = HudAnim.approach(entry.displaySlot, entry.targetSlot, stackSpeed);

            float alpha = alphaFor(entry, now) * opacity;
            if (alpha <= 0.02f) continue;

            float slide = slideFor(entry, now);
            int accent = accentFor(entry.commandId);
            int x = HudPlacement.isRightStack(el.anchor)
                    ? (int) (pos.x() + slidePx - slide * slidePx)
                    : (int) (pos.x() - slidePx + slide * slidePx);
            int y = bottom
                    ? feedY0 + stackH - boxH - Math.round(entry.displaySlot * pitch)
                    : feedY0 + Math.round(entry.displaySlot * pitch);

            float pop = popScale(entry, now);
            float popAlpha = alpha * (0.94f + 0.06f * pop);
            drawCard(g, mc, x, y, boxW, boxH, entry, accent, popAlpha * (1f - index * 0.06f),
                    index == 0, now, feed, es, pop);
            index++;
        }
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
        int pitch = HudLayout.s(42, es) + HudRails.gap();
        return vis * pitch - HudRails.gap();
    }

    private static float popScale(Entry entry, long now) {
        long age = now - entry.timeMs;
        if (age > 520) return 1f;
        float t = HudAnim.easeOutCubic(age / 520f);
        return 1f + 0.05f * (float) Math.sin(t * Math.PI);
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

    private static int cap(long now) {
        int cfg = Math.max(1, HudResolver.feedSections().maxEntries);
        return HudFeedPressure.cap(cfg, pressure(now), 3, 3, 2);
    }

    private static long ttlMs(Entry entry, long now) {
        int idx = Math.max(0, Math.round(entry.targetSlot));
        return HudFeedPressure.ttlMs(pressure(now), idx, QUIET_TTL_MS, BUSY_TTL_MS, FLOOD_TTL_MS);
    }

    private static void trimToCap(long now) {
        prune(now);
        int max = cap(now);
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
        float t = Mth.clamp((now - entry.timeMs) / (float) SLIDE_MS, 0f, 1f);
        return HudAnim.easeOutBack(t);
    }

    private static float alphaFor(Entry entry, long now) {
        long age = now - entry.timeMs;
        long ttl = ttlMs(entry, now);
        long fadeStart = Math.max(900L, (long) (ttl * 0.52f));
        long fadeDur = Math.max(400L, ttl - fadeStart);
        float in = HudAnim.easeOutCubic(Mth.clamp(age / 280f, 0f, 1f));
        float out = age > fadeStart
                ? 1f - Mth.clamp((age - fadeStart) / (float) fadeDur, 0f, 1f) : 1f;
        return in * out;
    }

    private static int cardWidth(Minecraft mc, Entry entry, float es) {
        int maxW = mc.getWindow().getGuiScaledWidth() - HudLayout.s(24, es);
        String line = truncate(entry.user, 14) + "  !" + entry.commandRu;
        return Math.min(maxW, Math.max(HudLayout.s(180, es), mc.font.width(line) + HudLayout.s(76, es)));
    }

    private static void drawCard(GuiGraphics g, Minecraft mc, int x, int y, int w, int h,
                                 Entry entry, int accent, float alpha, boolean highlight, long now,
                                 net.twitchcraft.config.TwitchCraftConfig.HudCommandFeedSections feed,
                                 float es, float pop) {
        boolean good = isGood(entry.commandId);
        HudCard.drawPremium(g, x, y, w, h, accent, alpha, pop, highlight, now);

        int pad = HudLayout.s(7, es);
        int avatarSize = HudLayout.s(28, es);
        int icon = HudLayout.s(14, es);
        int nameY = y + HudLayout.s(5, es);
        int textX = x + pad;

        if (feed.avatars) {
            int avX = textX;
            int avY = y + (h - avatarSize) / 2;
            float cdRatio = cooldownRatio(entry, now);
            if (cdRatio > 0f) {
                HudCard.drawCooldownRing(g, avX + avatarSize / 2, avY + avatarSize / 2,
                        avatarSize / 2 + 2, 2, cdRatio, accent, alpha);
            }
            HudAvatars.draw(g, mc.font, avX, avY, avatarSize, entry.user, alpha, good);
            int streakN = streakCountFor(entry.user);
            if (streakN >= 3) {
                HudAvatars.drawStreak(g, mc.font, avX, avY, avatarSize,
                        streakN, SidebarOverlay.streakGood(), alpha, now);
            }
            textX += avatarSize + HudLayout.s(8, es);
        } else {
            int big = HudLayout.s(18, es);
            int ix = textX;
            int iy = y + (h - big) / 2;
            float cdRatio = cooldownRatio(entry, now);
            if (cdRatio > 0f) {
                HudCard.drawCooldownRing(g, ix + big / 2, iy + big / 2,
                        big / 2 + 1, 2, cdRatio, accent, alpha);
            }
            CommandIcons.draw(g, entry.commandId, ix, iy, big, accent, alpha);
            textX += big + HudLayout.s(6, es);
        }

        int right = x + w - HudLayout.s(7, es);
        if (feed.badges) {
            String badge = categoryBadge(entry.commandId, good);
            int badgeW = mc.font.width(badge) + HudLayout.s(14, es);
            right -= badgeW;
            HudCard.drawBadge(g, mc.font, right, y + HudLayout.s(5, es), badge, accent, alpha);
            right -= HudLayout.s(4, es);
        }
        String streak = feed.showStreaks ? streakLabel(entry.user) : null;
        if (streak != null && streakCountFor(entry.user) < 3) {
            String streakBadge = "×" + SidebarOverlay.streakCount();
            int streakAccent = SidebarOverlay.streakGood() ? 0xFF_55_CC_55 : 0xFF_FF_66_66;
            int sw = mc.font.width(streakBadge) + HudLayout.s(14, es);
            right -= sw;
            HudCard.drawBadge(g, mc.font, right, y + HudLayout.s(5, es), streakBadge, streakAccent, alpha);
            right -= HudLayout.s(4, es);
        }

        String user = truncate(entry.user, 13);
        String nameDrawn = user;
        while (user.length() > 3 && textX + HudAvatars.nameWidth(mc.font, nameDrawn, 16) > right) {
            user = user.substring(0, user.length() - 1);
            nameDrawn = user + "…";
        }
        HudAvatars.drawName(g, mc.font, textX, nameY, nameDrawn, entry.nickColor, alpha, 16);

        int stripY = y + HudLayout.s(20, es);
        int stripH = HudLayout.s(15, es);
        int cmdX = textX;
        if (feed.avatars) {
            int ix = cmdX;
            int iy = stripY + (stripH - icon) / 2;
            CommandIcons.draw(g, entry.commandId, ix, iy, icon, accent, alpha, false);
            cmdX += icon + HudLayout.s(4, es);
        }
        int stripW = Math.max(HudLayout.s(48, es), w - (cmdX - x) - HudLayout.s(7, es));
        HudCard.drawCommandStrip(g, cmdX, stripY, stripW, stripH, accent, alpha);

        String cmdPlain = isHype(entry.commandId) ? entry.commandRu : "!" + entry.commandRu;
        String cmdColor = isHype(entry.commandId) ? hypeColor(entry.commandId)
                : (good ? "§a§l" : (isPrank(entry.commandId) ? "§e§l" : "§c§l"));
        int cmdY = stripY + Math.max(2, (stripH - 8) / 2);
        g.drawString(mc.font, cmdColor + cmdPlain, cmdX + HudLayout.s(5, es), cmdY,
                ((int) (alpha * 255) << 24) | (good ? 0xB8_FF_C4 : 0xFF_B8_B8), false);

        if (highlight && feed.progressBar) {
            float life = 1f - Mth.clamp((now - entry.timeMs) / (float) ttlMs(entry, now), 0f, 1f);
            HudCard.drawProgressBar(g, x + HudLayout.s(8, es), y + h - HudLayout.s(4, es),
                    w - HudLayout.s(16, es), HudLayout.s(3, es), life, accent, alpha * 0.92f);
        }
    }

    private static float cooldownRatio(Entry entry, long now) {
        if (entry.cooldownSec <= 0) return 0f;
        long total = entry.cooldownSec * 1000L;
        long left = total - (now - entry.timeMs);
        if (left <= 0) return 0f;
        return Mth.clamp(left / (float) total, 0f, 1f);
    }

    private static int streakCountFor(String user) {
        if (user == null || user.isBlank()) return 0;
        String su = SidebarOverlay.streakUser();
        if (su == null || su.isBlank() || !user.equalsIgnoreCase(su)) return 0;
        return SidebarOverlay.streakCount();
    }

    private static String streakLabel(String user) {
        if (user == null || user.isBlank()) return null;
        String su = SidebarOverlay.streakUser();
        if (su == null || su.isBlank() || SidebarOverlay.streakCount() < 2) return null;
        if (!user.equalsIgnoreCase(su)) return null;
        return SidebarOverlay.streakGood()
                ? "§a♥×" + SidebarOverlay.streakCount()
                : "§c🔥×" + SidebarOverlay.streakCount();
    }

    private static boolean isHype(String commandId) {
        return "firstmsg".equals(commandId) || "follow".equals(commandId);
    }

    private static String hypeColor(String commandId) {
        return "follow".equals(commandId) ? "§d§l" : "§e§l";
    }

    private static boolean isPrank(String commandId) {
        return ChatGameCommand.fromId(commandId).map(EvilCommands::isPrank).orElse(false);
    }

    private static boolean isGood(String commandId) {
        if (isHype(commandId)) return true;
        return ChatGameCommand.fromId(commandId).map(GoodCommands::isGood).orElse(false);
    }

    private static String categoryBadge(String commandId, boolean good) {
        if ("firstmsg".equals(commandId)) return "ЧАТ";
        if ("follow".equals(commandId)) return "ТВИЧ";
        if (good) return "ДОБРО";
        return ChatGameCommand.fromId(commandId)
                .map(EvilCommands::categoryTag)
                .orElse("ЧАТ");
    }

    private static int accentFor(String commandId) {
        if ("firstmsg".equals(commandId)) {
            return HudResolver.accent(HudResolver.Element.COMMAND_FEED, 0xFF_F0_C0_40);
        }
        if ("follow".equals(commandId) || "sub".equals(commandId)) {
            return HudResolver.goodColor(0xFF_E8_7A_D0);
        }
        if ("raid".equals(commandId) || "miss".equals(commandId)) {
            return HudResolver.accent(HudResolver.Element.COMMAND_FEED, 0xFF_FF_5A_4A);
        }
        return ChatGameCommand.fromId(commandId)
                .map(cmd -> switch (EvilCommands.harm(cmd)) {
                    case GOOD -> HudResolver.goodColor(0xFF_2E_CC_71);
                    case DAMAGE -> HudResolver.evilColor(0xFF_E7_4C_3C);
                    case PRANK -> 0xFF_F1_C4_0F;
                    case MOD -> TwitchUi.PURPLE;
                })
                .orElseGet(() -> isGood(commandId)
                        ? HudResolver.goodColor(0xFF_55_CC_55)
                        : HudResolver.accent(HudResolver.Element.COMMAND_FEED, TwitchUi.PURPLE));
    }

    private static String truncate(String s, int max) {
        if (s == null) return "?";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
