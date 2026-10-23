package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.GoodCommands;
import net.twitchcraft.donation.DonationTierResolver;
import net.twitchcraft.hud.SidebarPages;
import net.twitchcraft.network.SidebarStatePayload;
import net.twitchcraft.twitch.TwitchStatusFormatter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Sidebar — живой статус, плавный хаос, fade страниц. */
public final class SidebarOverlay {
    private static final String TITLE = "TwitchCraft";
    private static final int MIN_BOX_W = 124;
    private static SidebarStatePayload state = emptyState();
    private static int localElapsed;
    private static float chaosDisplay;
    private static int lastCommandPage = -1;
    private static long pageFadeStartMs;

    private record Line(String text, int color, LineKind kind, float barRatio, int accent, float alphaMul) {
        Line(String text, int color) {
            this(text, color, LineKind.TEXT, 0f, 0, 1f);
        }
        Line(String text, int color, LineKind kind) {
            this(text, color, kind, 0f, 0, 1f);
        }
        Line(String text, int color, LineKind kind, float barRatio, int accent) {
            this(text, color, kind, barRatio, accent, 1f);
        }
        static Line header(String text, int accent) {
            return new Line(text, TwitchUi.TEXT, LineKind.HEADER, 0f, accent, 1f);
        }
        static Line header(String text, int accent, float alphaMul) {
            return new Line(text, TwitchUi.TEXT, LineKind.HEADER, 0f, accent, alphaMul);
        }
        static Line highlight(String text, int accent) {
            return new Line(text, TwitchUi.TEXT, LineKind.HIGHLIGHT, 0f, accent, 1f);
        }
        static Line hit(String userCmd, int nickColor, int accent) {
            return new Line(userCmd, nickColor, LineKind.HIT, 0f, accent, 1f);
        }
        static Line spacer() {
            return new Line("", 0, LineKind.SPACER, 0f, 0, 1f);
        }
        static Line faded(String text, int color, float alphaMul) {
            return new Line(text, color, LineKind.TEXT, 0f, 0, alphaMul);
        }
    }

    private enum LineKind { TEXT, CHAOS_BAR, KARMA_BAR, SPACER, HEADER, HIGHLIGHT, HIT }

    private static final Set<String> GOOD_RU = new HashSet<>();

    static {
        for (ChatGameCommand cmd : GoodCommands.all()) {
            GOOD_RU.add(cmd.getRussianName().toLowerCase(Locale.ROOT));
        }
    }

    private SidebarOverlay() {}

    public static void applyState(SidebarStatePayload s) {
        state = s != null ? s : emptyState();
    }

    public static String twitchStatusLine() {
        return TwitchStatusFormatter.sidebarLine(state.twitchConnected(), state.twitchChannel(), state.twitchError());
    }

    public static String twitchSettingsLine() {
        return TwitchStatusFormatter.settingsLine(state.twitchConnected(), state.twitchChannel(), state.twitchError());
    }

    public static boolean isTwitchConnected() {
        return state.twitchConnected();
    }

    public static String lastDeathCoords() {
        String c = state.lastDeathCoords();
        return c != null ? c : "";
    }

    public static int totalDeaths() {
        return state.totalDeaths();
    }

    public static void clientTick() {
        localElapsed++;
    }

    public static void render(GuiGraphics g) {
        if (!HudResolver.enabled(HudResolver.Element.SIDEBAR)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.font == null || mc.options.hideGui || mc.screen != null) return;

        TwitchCraftConfig cfg = ConfigManager.get();
        var sections = HudResolver.sidebarSections();
        var el = HudResolver.el(HudResolver.Element.SIDEBAR);
        float es = HudResolver.elementScale(HudResolver.Element.SIDEBAR);
        float opacity = HudResolver.opacity(HudResolver.Element.SIDEBAR);

        List<Line> lines = buildLines(cfg, sections);
        if (lines.isEmpty() && !sections.title) return;

        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int padX = HudLayout.s(9, es);
        int padY = HudLayout.s(7, es);
        int lineH = HudLayout.s(10, es);
        int gap = HudLayout.s(2, es);
        int sectionGap = HudLayout.s(5, es);
        int barExtra = HudLayout.s(5, es);
        int headerH = HudLayout.s(12, es);
        int highlightH = HudLayout.s(14, es);

        boolean house = state.houseMode() && sections.houseMode;
        int accent = house ? TwitchUi.GOLD
                : HudResolver.accent(HudResolver.Element.SIDEBAR, TwitchUi.PURPLE);

        int maxLineW = sections.title ? TwitchUi.textWidthBold(font, TITLE) + HudLayout.s(36, es) : 0;
        for (Line line : lines) {
            maxLineW = Math.max(maxLineW, font.width(TwitchUi.stripFormat(line.text())));
        }
        int boxW = Mth.clamp(maxLineW + padX * 2 + HudLayout.s(4, es),
                HudLayout.s(160, es), Math.min(HudLayout.s(220, es), screenW / 3));
        HudRails.reportSidebarWidth(boxW);

        int boxH = padY * 2;
        if (sections.title) boxH += HudLayout.s(18, es);
        for (Line line : lines) {
            boxH += switch (line.kind) {
                case CHAOS_BAR, KARMA_BAR -> lineH + barExtra + gap;
                case SPACER -> sectionGap;
                case HEADER -> headerH + gap;
                case HIGHLIGHT, HIT -> highlightH + gap;
                case TEXT -> lineH + gap;
            };
        }

        HudPlacement.Rect pos = HudPlacement.place(el.anchor, el.offsetX, el.offsetY,
                screenW, mc.getWindow().getGuiScaledHeight(), boxW, boxH, es);
        int x = pos.x();
        int y = pos.y();
        if (HudPlacement.isRightStack(el.anchor)) {
            y += ShowdownOverlay.dockHeight();
        }
        int a = (int) (opacity * 255);

        // Каркас
        drawSidebarFrame(g, x, y, boxW, boxH, accent, opacity, house);

        int ty = y + padY;
        int contentW = boxW - padX * 2;

        if (sections.title) {
            ty = drawTitleRow(g, font, x, ty, boxW, padX, es, accent, a, house);
        }

        for (Line line : lines) {
            int la = (int) (a * Mth.clamp(line.alphaMul(), 0f, 1f));
            switch (line.kind) {
                case SPACER -> ty += sectionGap;
                case HEADER -> {
                    ty = drawSectionHeader(g, font, x + padX, ty, contentW, line.text(),
                            line.accent != 0 ? line.accent : accent, la, es);
                    ty += gap;
                }
                case HIGHLIGHT -> {
                    ty = drawHighlightRow(g, font, x + padX, ty, contentW, line.text(),
                            line.accent != 0 ? line.accent : accent, la, es);
                    ty += gap;
                }
                case HIT -> {
                    ty = drawHitRow(g, font, x + padX, ty, contentW, line.text(),
                            line.color, line.accent != 0 ? line.accent : accent, la, es);
                    ty += gap;
                }
                case CHAOS_BAR -> {
                    chaosDisplay = HudAnim.approach(chaosDisplay, line.barRatio, 0.14f);
                    g.drawString(font, line.text(), x + padX, ty, withAlpha(line.color(), la), false);
                    ty += lineH;
                    int barAccent = line.accent != 0 ? line.accent : TwitchUi.PURPLE_LIGHT;
                    drawChaosBar(g, x + padX, ty, contentW, HudLayout.s(4, es), chaosDisplay, barAccent, la);
                    ty += barExtra + gap;
                }
                case KARMA_BAR -> {
                    g.drawString(font, line.text(), x + padX, ty, withAlpha(line.color(), la), false);
                    ty += lineH;
                    HudCard.drawKarmaBar(g, x + padX, ty, contentW, HudLayout.s(4, es), line.barRatio, la);
                    ty += barExtra + gap;
                }
                case TEXT -> {
                    g.drawString(font, line.text(), x + padX, ty, withAlpha(line.color(), la), false);
                    ty += lineH + gap;
                }
            }
        }
    }

    private static void drawSidebarFrame(GuiGraphics g, int x, int y, int w, int h,
                                         int accent, float opacity, boolean house) {
        HudCard.drawPremium(g, x, y, w, h, accent, opacity, 1f, false, 0);
        if (house) {
            int a = (int) (opacity * 255);
            TwitchUi.fillGradientH(g, x + HudCard.ACCENT_W, y + 2, x + w - 1, y + 4,
                    (a / 3 << 24) | (TwitchUi.GOLD & 0xFFFFFF), 0x00_00_00_00);
        }
    }

    private static int drawTitleRow(GuiGraphics g, Font font, int x, int ty, int boxW, int padX,
                                    float es, int accent, int a, boolean house) {
        TwitchUi.fillGradientH(g, x + 3, ty - 2, x + boxW - 1, ty + HudLayout.s(14, es),
                (a / 7 << 24) | (accent & 0xFFFFFF), 0x00_00_00_00);

        String title = TwitchUi.TITLE_FORMAT;
        int titleW = font.width(title);
        g.drawString(font, title, x + padX, ty + 1, (a << 24) | 0xFFFFFF, false);

        boolean on = state.twitchConnected();
        String pill = on ? "ЭФИР" : "ВЫКЛ";
        int pw = font.width(pill) + HudLayout.s(8, es) + (on ? HudLayout.s(6, es) : 0);
        int ph = HudLayout.s(10, es);
        int px = x + boxW - padX - pw;
        int py = ty;
        int pillTop = on ? 0x1A_3A_28 : 0x2A_18_1C;
        int pillBot = on ? 0x12_28_1C : 0x1C_10_12;
        TwitchUi.fillGradientV(g, px, py, px + pw, py + ph,
                (a * 8 / 10 << 24) | pillTop, (a * 9 / 10 << 24) | pillBot);
        TwitchUi.roundedPanel(g, px, py, px + pw, py + ph, (a / 20 << 24) | 0xFF_FF_FF);

        int textX = px + HudLayout.s(4, es);
        if (on) {
            // Мигающая LIVE-точка
            float blink = 0.45f + 0.55f * (0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 320.0));
            int da = (int) (a * blink);
            int ds = HudLayout.s(3, es);
            int dx = px + HudLayout.s(3, es);
            int dy = py + (ph - ds) / 2;
            g.fill(dx, dy, dx + ds, dy + ds, (da << 24) | 0x2E_CC_71);
            textX = dx + ds + HudLayout.s(3, es);
        }

        TwitchUi.fillGradientH(g, px + 1, py + ph - 1, px + pw - 1, py + ph,
                (a << 24) | (on ? 0x2E_CC_71 : 0xE7_4C_3C),
                (a / 4 << 24) | (on ? 0x2E_CC_71 : 0xE7_4C_3C));
        g.drawString(font, (on ? "§a" : "§c") + pill, textX, py + 1,
                (a << 24) | 0xFFFFFF, false);

        if (house) {
            g.drawString(font, "§6⚡", x + padX + titleW + HudLayout.s(4, es), ty + 1,
                    (a << 24) | (TwitchUi.GOLD & 0xFFFFFF), false);
        }

        return ty + HudLayout.s(17, es);
    }

    private static int drawSectionHeader(GuiGraphics g, Font font, int x, int y, int w,
                                         String text, int accent, int a, float es) {
        int h = HudLayout.s(11, es);
        TwitchUi.fillGradientH(g, x, y + h / 2, x + w, y + h / 2 + 1,
                (a / 4 << 24) | (accent & 0xFFFFFF), 0x00_00_00_00);
        int tw = font.width(TwitchUi.stripFormat(text));
        TwitchUi.fillGradientV(g, x, y, x + tw + HudLayout.s(6, es), y + h,
                (a * 85 / 100 << 24) | 0x16_14_1E,
                (a * 92 / 100 << 24) | 0x10_0E_16);
        g.drawString(font, text, x + HudLayout.s(3, es), y + 1, (a << 24) | (accent & 0xFFFFFF), false);
        return y + h;
    }

    private static int drawHighlightRow(GuiGraphics g, Font font, int x, int y, int w,
                                        String text, int accent, int a, float es) {
        int h = HudLayout.s(13, es);
        TwitchUi.fillGradientV(g, x, y, x + w, y + h,
                (a * 45 / 100 << 24) | 0x1C_18_28,
                (a * 50 / 100 << 24) | 0x12_10_18);
        TwitchUi.fillGradientV(g, x, y + 1, x + 2, y + h - 1,
                (a << 24) | (accent & 0xFFFFFF),
                (a / 3 << 24) | (accent & 0xFFFFFF));
        g.drawString(font, text, x + HudLayout.s(5, es), y + 2, (a << 24) | 0xF0_F0_F5, false);
        return y + h;
    }

    private static int drawHitRow(GuiGraphics g, Font font, int x, int y, int w,
                                  String text, int nickColor, int accent, int a, float es) {
        int h = HudLayout.s(13, es);
        TwitchUi.fillGradientV(g, x, y, x + w, y + h,
                (a * 45 / 100 << 24) | 0x1C_18_28,
                (a * 50 / 100 << 24) | 0x12_10_18);
        TwitchUi.fillGradientV(g, x, y + 1, x + 2, y + h - 1,
                (a << 24) | (accent & 0xFFFFFF),
                (a / 3 << 24) | (accent & 0xFFFFFF));
        String user = text;
        String cmd = "";
        int tab = text == null ? -1 : text.indexOf('\t');
        if (tab >= 0) {
            user = text.substring(0, tab);
            cmd = text.substring(tab + 1);
        }
        int tx = x + HudLayout.s(5, es);
        HudAvatars.drawName(g, font, tx, y + 2, user, nickColor, a / 255f, 9);
        int nx = tx + HudAvatars.nameWidth(font, user, 9);
        g.drawString(font, "§8→ " + cmd, nx + HudLayout.s(3, es), y + 2,
                (a << 24) | 0xF0_F0_F5, false);
        return y + h;
    }

    private static void drawChaosBar(GuiGraphics g, int x, int y, int w, int h, float ratio, int accent, int a) {
        float r = Math.max(0f, Math.min(1f, ratio));
        g.fill(x, y, x + w, y + h, (a / 5 << 24) | 0x22_22_28);
        int fw = Math.max(1, (int) (w * r));
        int rgb = accent & 0xFFFFFF;
        int dark = TwitchUi.blendRgb(rgb, 0x000000, 0.35f) & 0xFFFFFF;
        TwitchUi.fillGradientH(g, x, y, x + fw, y + h, (a << 24) | rgb, (a << 24) | dark);
        if (r > 0.85f) {
            g.fill(x + fw - 2, y, x + fw, y + h, (a << 24) | 0xFF_66_66);
        }
    }

    private static int withAlpha(int color, int a) {
        return (a << 24) | (color & 0xFFFFFF);
    }

    private static List<Line> buildLines(TwitchCraftConfig cfg, TwitchCraftConfig.HudSidebarSections sections) {
        boolean compact = sections.compact;
        List<Line> lines = new ArrayList<>();
        int elapsed = localElapsed / 20;

        if (sections.statusLine) {
            String status = state.statusLine();
            if (status != null && !status.isBlank()) {
                lines.add(new Line(status, TwitchUi.TEXT_DIM));
            }
        }

        if (sections.houseMode && state.houseMode()) {
            lines.add(Line.highlight("§6§l⚡ ХАУСМОД ВКЛ", TwitchUi.GOLD));
        }

        if (sections.lastCommand) {
            String user = state.lastUser();
            String cmd = state.lastCommandRu();
            boolean good = isGoodCommandRu(cmd);
            int acc = good ? 0xFF_2E_CC_71 : 0xFF_E7_4C_3C;
            String lastLine = trunc(user, 9) + "\t" + (good ? "§a!" : "§c!") + trunc(cmd, 11);
            lines.add(Line.hit(lastLine, state.lastUserNickColor(), acc));
        }

        if (sections.stats) {
            lines.add(Line.header("§7СТАТ", TwitchUi.TEXT_DIM));
            lines.add(new Line("§a+" + state.goodPoints() + " §8добро  §c-" + state.evilPoints() + " §8зло",
                    0xFF_AA_AA_B2));
            lines.add(new Line(net.twitchcraft.stream.ViewerKarma.formatLine(state.karmaScore()),
                    state.karmaScore() >= 25 ? 0xFF_66_DD_88
                            : (state.karmaScore() <= -25 ? 0xFF_FF_66_66 : 0xFF_F1_C4_0F),
                    LineKind.KARMA_BAR,
                    (state.karmaScore() + 100) / 200f,
                    state.karmaScore() >= 0 ? 0xFF_2E_CC_71 : 0xFF_E7_4C_3C));
            if (sections.chaos && cfg.stream.chaosMeterEnabled) {
                int max = Math.max(1, state.chaosMax());
                lines.add(new Line("§5Хаос §f" + state.chaos() + "§8/§f" + max, TwitchUi.PURPLE_LIGHT,
                        LineKind.CHAOS_BAR, state.chaos() / (float) max, TwitchUi.PURPLE));
            }
            if (sections.penalty && !compact && state.penaltyActive()) {
                lines.add(new Line("§cКД×" + state.penaltyMult() + " §7ещё " + state.penaltySec() + "с",
                        0xFF_FF_9F_43));
            }
        }

        if (sections.showTopViewers && !compact) {
            boolean anyTop = false;
            String good = state.topGoodViewer();
            String evil = state.topEvilViewer();
            String active = state.topActiveViewer();
            if (good != null && !good.isBlank() && state.topGoodCount() > 0) {
                if (!anyTop) { lines.add(Line.header("§7ТОП", TwitchUi.TEXT_DIM)); anyTop = true; }
                lines.add(new Line("§a♥ " + trunc(good, 10) + " §8×" + state.topGoodCount(), 0xFF_66_DD_88));
            }
            if (evil != null && !evil.isBlank() && state.topEvilCount() > 0) {
                if (!anyTop) { lines.add(Line.header("§7ТОП", TwitchUi.TEXT_DIM)); anyTop = true; }
                lines.add(new Line("§c☠ " + trunc(evil, 10) + " §8×" + state.topEvilCount(), 0xFF_FF_66_66));
            }
            if (active != null && !active.isBlank() && state.topActiveCount() > 1
                    && !active.equalsIgnoreCase(good) && !active.equalsIgnoreCase(evil)) {
                if (!anyTop) { lines.add(Line.header("§7ТОП", TwitchUi.TEXT_DIM)); anyTop = true; }
                lines.add(new Line("§b⚡ " + trunc(active, 10) + " §8×" + state.topActiveCount(), 0xFF_74_C0_FC));
            }
        }

        if (sections.showStreaks && state.streakCount() >= 2) {
            String su = state.streakUser();
            if (su != null && !su.isBlank()) {
                String mark = state.streakGood() ? "§a♥" : "§c🔥";
                int acc = state.streakGood() ? 0xFF_66_DD_88 : 0xFF_FF_9F_43;
                lines.add(Line.highlight(mark + " §f" + trunc(su, 10) + " §8· §7серия §l" + state.streakCount(), acc));
            }
        }

        TwitchCraftConfig.DeathSettings deathCfg = cfg.death;
        if (deathCfg != null && deathCfg.enabled) {
            String coordsMode = deathCfg.coordsDisplay == null ? "overlay"
                    : deathCfg.coordsDisplay.toLowerCase(Locale.ROOT);
            boolean showInSidebar = sections.deathCoords
                    && (coordsMode.contains("sidebar") || "all".equals(coordsMode));
            boolean deathBlock = false;
            if (sections.deaths && deathCfg.showDeathCounter
                    && (state.totalDeaths() > 0 || state.deathsInWindow() > 0)) {
                lines.add(Line.header("§7СМЕРТИ", 0xFF_E7_4C_3C));
                deathBlock = true;
                lines.add(new Line("§c☠ §f" + state.totalDeaths()
                        + " §8| §7" + state.deathsInWindow() + "/" + deathCfg.deathsForPenalty
                        + " §8· §7" + deathCfg.deathWindowMinutes + "м", 0xFF_FF_66_66));
            }
            if (sections.evilBlock && state.evilBlockActive()) {
                if (!deathBlock) lines.add(Line.header("§7СМЕРТИ", 0xFF_E7_4C_3C));
                lines.add(Line.highlight("§4§lБЛОК ЗЛА §c" + formatSec(state.evilBlockSec()), 0xFF_FF_33_44));
            }
            if (showInSidebar && deathCfg.showCoordinates) {
                String coords = state.lastDeathCoords();
                if (coords != null && !coords.isBlank()) {
                    lines.add(new Line("§7" + coords, 0xFF_AA_AA_B2));
                }
            }
        }

        if (sections.donations && !compact) {
            List<String> tierLines = DonationTierResolver.sidebarLines(cfg);
            if (!tierLines.isEmpty()) {
                lines.add(Line.spacer());
                lines.add(Line.header("§6ДОНАТЫ", TwitchUi.GOLD));
                int shown = 0;
                for (String tierLine : tierLines) {
                    if (tierLine.contains("💰") || tierLine.contains("Донаты")) continue;
                    if (shown++ >= 4) break;
                    lines.add(new Line(tierLine, 0xFF_C8_C0_A8));
                }
            }
        }

        if (sections.commands) {
            lines.add(Line.spacer());
            int max = Math.min(Math.max(4, sections.maxCommandLines), compact ? 12 : 16);
            int count = 0;
            boolean headerDrawn = false;
            int rotateSec = Math.max(3, cfg.scoreboard.pageRotateSeconds);
            int pages = Math.max(1, SidebarPages.pageCount(cfg));
            int pageIndex = (elapsed / rotateSec) % pages;
            long now = System.currentTimeMillis();
            if (pageIndex != lastCommandPage) {
                lastCommandPage = pageIndex;
                pageFadeStartMs = now;
            }
            float pageFade = HudAnim.easeOutCubic(Mth.clamp((now - pageFadeStartMs) / 150f, 0f, 1f));

            for (String cmdLine : SidebarPages.buildSidebar(cfg, elapsed)) {
                if (count >= max) break;
                if (!headerDrawn && isCommandPageHeader(cmdLine)) {
                    String plain = TwitchUi.stripFormat(cmdLine);
                    lines.add(Line.header("§6" + plain, TwitchUi.GOLD, pageFade));
                    headerDrawn = true;
                    count++;
                    continue;
                }
                lines.add(Line.faded(cmdLine, colorForCommandLine(cmdLine), pageFade));
                count++;
            }
        }

        if (lines.size() > 40) return new ArrayList<>(lines.subList(0, 40));
        return lines;
    }

    private static boolean isCommandPageHeader(String line) {
        if (line == null || line.isBlank()) return false;
        return line.contains("Добро") || line.contains("Урон") || line.contains("Пранк")
                || line.contains("Свои") || line.contains("§a§l+") || line.contains("§c§l⚔")
                || line.contains("§e§l☺") || line.contains("§b§l✦");
    }

    private static int colorForCommandLine(String line) {
        if (line.startsWith("§6") || line.startsWith("§a+")) return TwitchUi.GOLD;
        if (line.contains("§a!")) return 0xFF_66_DD_88;
        if (line.contains("§e!")) return 0xFF_FF_CC_55;
        if (line.contains("§7!")) return 0xFF_BB_BB_C8;
        return TwitchUi.TEXT_DIM;
    }

    private static boolean isGoodCommandRu(String ru) {
        if (ru == null || ru.isBlank() || "-".equals(ru)) return false;
        return GOOD_RU.contains(ru.toLowerCase(Locale.ROOT));
    }

    private static String trunc(String s, int max) {
        if (s == null || s.isBlank()) return "-";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String formatSec(int sec) {
        int m = sec / 60;
        int s = sec % 60;
        return m > 0 ? m + "м " + s + "с" : s + "с";
    }

    private static SidebarStatePayload emptyState() {
        return new SidebarStatePayload("-", "-", 0, 0, 0, 50, 0,
                false, false, 1, 0, "", false, "", "§7ожидание сервера",
                0, 0, 0, false, "",
                "", 0, "", 0, "", 0, "", 0, false, 0, 0);
    }

    public static String streakUser() {
        return state.streakUser() != null ? state.streakUser() : "";
    }

    public static int streakCount() {
        return state.streakCount();
    }

    public static boolean streakGood() {
        return state.streakGood();
    }
}
