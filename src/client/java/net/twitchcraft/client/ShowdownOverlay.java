package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Правая колонка, сверху: событие стыкуется с сайдбаром. */
public final class ShowdownOverlay {
    private static String kind = "";
    private static String title = "";
    private static String subtitle = "";
    private static int secondsLeft;
    private static int totalSeconds = 1;
    private static int leftScore;
    private static int rightScore;
    private static String leftLabel = "";
    private static String rightLabel = "";
    private static boolean active;
    private static long lastPacketAt;
    private static long appearAt;
    private static int lastPanelH;

    private ShowdownOverlay() {}

    public static void update(String k, String t, String sub, int left, int total,
                              int lScore, int rScore, String lLabel, String rLabel, boolean on) {
        kind = k == null ? "" : k;
        title = t == null ? "" : t;
        subtitle = sub == null ? "" : sub;
        secondsLeft = left;
        totalSeconds = Math.max(1, total);
        leftScore = lScore;
        rightScore = rScore;
        leftLabel = lLabel == null ? "" : lLabel;
        rightLabel = rLabel == null ? "" : rLabel;
        boolean was = active;
        active = on;
        lastPacketAt = System.currentTimeMillis();
        if (on && !was) appearAt = lastPacketAt;
    }

    public static int dockHeight() {
        if (!isLive()) return 0;
        return lastPanelH > 0 ? lastPanelH + HudRails.gap() : HudLayout.s(42);
    }

    public static boolean isLive() {
        return active && System.currentTimeMillis() - lastPacketAt <= 2500;
    }

    public static void render(GuiGraphics g, float delta) {
        if (!isLive()) {
            lastPanelH = 0;
            if (System.currentTimeMillis() - lastPacketAt > 2500) active = false;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc)) return;

        int w = mc.getWindow().getGuiScaledWidth();
        float es = 1f;
        float slide = HudAnim.easeOutBack(Mth.clamp((System.currentTimeMillis() - appearAt) / 360f, 0f, 1f));
        boolean six = "SIXSEVEN".equals(kind);
        boolean vote = "VOTE".equals(kind);
        boolean cipher = "CIPHER".equals(kind);
        boolean hold = "HOLD".equals(kind);
        int panelW = HudRails.rightColW();
        int panelH = HudLayout.s(six ? 50 : (vote ? 78 : (cipher ? 50 : (hold ? 42 : (hasScores() ? 46 : 36)))), es);
        lastPanelH = panelH;
        int px = HudRails.rightX(w, panelW) + (int) ((1f - slide) * HudLayout.s(22, es));
        int py = HudRails.margin();
        int accent = accentOf(kind);
        float pulse = urgent(kind)
                ? 0.88f + 0.12f * (float) Math.sin(System.currentTimeMillis() / 180.0)
                : 1f;

        HudCard.drawPremium(g, px, py, panelW, panelH, accent, 0.97f * slide, pulse, urgent(kind),
                System.currentTimeMillis());

        var font = mc.font;
        int pad = HudLayout.s(8, es);
        int left = px + pad;
        int right = px + panelW - pad;

        String timer = secondsLeft + "с";
        int tw = font.width(timer);
        g.drawString(font, "§f" + timer, right - tw, py + HudLayout.s(5, es), 0xFF_FF_FF_FF, false);

        if (six) {
            drawSixSeven(g, font, px, py, panelW, panelH, left, accent, es);
            return;
        }
        if (vote) {
            drawVote(g, font, px, py, panelW, panelH, left, pad, accent, es);
            return;
        }
        if (cipher) {
            g.drawString(font, "§e§lЗАГАДКА", left, py + HudLayout.s(5, es), 0xFF_FF_EE_CC, false);
            String line = subtitle.isBlank() ? "!????? · !?????" : subtitle;
            g.drawString(font, "§e" + (line.length() > 22 ? line.substring(0, 21) + "…" : line),
                    left, py + HudLayout.s(16, es), TwitchUi.TEXT, false);
            String cats = (leftLabel + "  " + rightLabel).trim();
            if (!cats.isBlank()) {
                g.drawString(font, "§8" + cats, left, py + HudLayout.s(26, es), TwitchUi.TEXT_DIM, false);
            }
            float ratio = Mth.clamp(secondsLeft / (float) totalSeconds, 0f, 1f);
            HudCard.drawProgressBar(g, px + pad, py + panelH - HudLayout.s(6, es),
                    panelW - pad * 2, 3, ratio, accent, 0.95f);
            return;
        }

        String head = header();
        g.drawString(font, head, left, py + HudLayout.s(5, es), 0xFF_FF_EE_CC, false);

        String mid = subtitle;
        if (!mid.isBlank()) {
            int max = "TETHER".equals(kind) || "HOLD".equals(kind) ? 26 : 18;
            if (mid.length() > max) mid = mid.substring(0, max - 1) + "…";
            g.drawString(font, "§7" + mid, left, py + HudLayout.s(16, es), TwitchUi.TEXT_DIM, false);
        }

        if (hasScores()) {
            String scores = "§c" + leftLabel + " §f" + leftScore + "  §8·  §a" + rightLabel + " §f" + rightScore;
            g.drawString(font, scores, left, py + HudLayout.s(26, es), TwitchUi.TEXT, false);
        }

        float ratio = Mth.clamp(secondsLeft / (float) totalSeconds, 0f, 1f);
        HudCard.drawProgressBar(g, px + pad, py + panelH - HudLayout.s(6, es),
                panelW - pad * 2, 3, ratio, accent, 0.95f);
    }

    private static void drawSixSeven(GuiGraphics g, Font font, int px, int py, int panelW, int panelH,
                                     int left, int accent, float es) {
        int have = Math.max(0, leftScore);
        int need = Math.max(1, rightScore);
        int remain = Math.max(0, need - have);
        String last = subtitle.isBlank() ? "чат" : subtitle;
        if (last.length() > 12) last = last.substring(0, 11) + "…";

        g.drawString(font, "§e§l67", left, py + HudLayout.s(5, es), 0xFF_FF_EE_CC, false);
        g.drawString(font, "§f" + have + "§8/§e" + need + " §7ников",
                left, py + HudLayout.s(16, es), TwitchUi.TEXT, false);
        String hint = remain <= 0 ? "§aсобрали!" : "§7ещё §e" + remain + " §8· §f" + last;
        g.drawString(font, hint, left, py + HudLayout.s(26, es), TwitchUi.TEXT_DIM, false);

        float ratio = Mth.clamp(have / (float) need, 0f, 1f);
        HudCard.drawProgressBar(g, px + HudLayout.s(8, es), py + panelH - HudLayout.s(6, es),
                panelW - HudLayout.s(16, es), 3, ratio, accent, 0.95f);
    }

    private static void drawVote(GuiGraphics g, Font font, int px, int py, int panelW, int panelH,
                                 int left, int pad, int accent, float es) {
        g.drawString(font, "§d§lГОЛОС", left, py + HudLayout.s(5, es), 0xFF_FF_EE_CC, false);
        VoteRow[] rows = parseVoteRows();
        int max = 1;
        for (VoteRow row : rows) max = Math.max(max, row.votes);
        int rowY = py + HudLayout.s(16, es);
        int barW = panelW - pad * 2 - HudLayout.s(36, es);
        for (int i = 0; i < rows.length; i++) {
            VoteRow row = rows[i];
            String tag = "§e!" + (i + 1) + " §f" + trunc(row.name, 10);
            g.drawString(font, tag, left, rowY, TwitchUi.TEXT, false);
            String n = "§f" + row.votes;
            g.drawString(font, n, px + panelW - pad - font.width(TwitchUi.stripFormat(n)),
                    rowY, TwitchUi.TEXT, false);
            float ratio = Mth.clamp(row.votes / (float) max, 0f, 1f);
            if (row.votes == 0) ratio = 0.04f;
            int by = rowY + HudLayout.s(9, es);
            HudCard.drawProgressBar(g, left, by, barW, HudLayout.s(3, es), ratio, accent, 0.95f);
            rowY += HudLayout.s(16, es);
        }
        float timeRatio = Mth.clamp(secondsLeft / (float) totalSeconds, 0f, 1f);
        HudCard.drawProgressBar(g, px + pad, py + panelH - HudLayout.s(5, es),
                panelW - pad * 2, 2, timeRatio, accent, 0.9f);
    }

    private record VoteRow(String name, int votes) {}

    private static VoteRow[] parseVoteRows() {
        VoteRow[] rows = new VoteRow[] {
                new VoteRow(leftLabel.isBlank() ? "1" : leftLabel, leftScore),
                new VoteRow("2", 0),
                new VoteRow(rightLabel.isBlank() ? "3" : rightLabel, 0)
        };
        if (subtitle == null || !subtitle.contains("|")) return rows;
        String[] parts = subtitle.split("\\|");
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            String p = parts[i];
            int colon = p.lastIndexOf(':');
            if (colon <= 0) {
                rows[i] = new VoteRow(p, i == 0 ? leftScore : 0);
                continue;
            }
            String name = p.substring(0, colon);
            int v = 0;
            try {
                v = Integer.parseInt(p.substring(colon + 1).trim());
            } catch (NumberFormatException ignored) {
            }
            rows[i] = new VoteRow(name, v);
        }
        return rows;
    }

    private static String trunc(String s, int max) {
        if (s == null) return "?";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String header() {
        String head = switch (kind) {
            case "HUNT" -> "§c§lОХОТА";
            case "BET" -> "§6§lСТАВКА";
            case "BOMB" -> "§4§lБОМБА";
            case "KING" -> "§6§lКОРОЛЬ";
            case "SHIELD" -> "§6§lЩИТ";
            case "CHALLENGE" -> "§d§lВЫЗОВ";
            case "DICE" -> "§6§lКУБИК";
            case "SIZE" -> "§e§l" + (title.isBlank() ? "РАЗМЕР" : title);
            case "MORPH" -> "§d§lМОРФ";
            case "CONTRACT" -> "§c§lКОНТРАКТ";
            case "TAUNT" -> "§c§lНАСМЕШКА";
            case "TETHER" -> "§d§lСВЯЗЬ";
            case "HOLD" -> "§e§lДЕРЖИСЬ";
            case "VOTE" -> "§d§lГОЛОС";
            case "CIPHER" -> "§e§lЗАГАДКА";
            default -> "§e§l" + title;
        };
        if (!title.isBlank() && isResultTitle(title)) {
            head = "§e§l" + title;
        }
        return head;
    }

    private static boolean urgent(String k) {
        return "BOMB".equals(k) || "CHALLENGE".equals(k) || "SIXSEVEN".equals(k)
                || "VOTE".equals(k) || "HOLD".equals(k);
    }

    private static boolean isResultTitle(String t) {
        return !t.equals("ОХОТА") && !t.equals("СТАВКА") && !t.equals("БОМБА")
                && !t.equals("КОРОЛЬ ЧАТА") && !t.equals("ЩИТ") && !t.equals("ВЫЗОВ")
                && !t.equals("КУБИК") && !t.equals("СВЯЗЬ") && !t.equals("ДЕРЖИСЬ")
                && !t.equals("ГРУЗ") && !t.equals("ТОТЕМ") && !t.equals("ГОСТЬ");
    }

    private static boolean hasScores() {
        return "BET".equals(kind) || leftScore + rightScore > 0 && !"BOMB".equals(kind) && !"SIXSEVEN".equals(kind);
    }

    private static int accentOf(String k) {
        return switch (k) {
            case "HUNT" -> 0xFF_E74C3C;
            case "BET" -> 0xFF_F1C40F;
            case "BOMB" -> 0xFF_C0392B;
            case "KING" -> 0xFF_F39C12;
            case "SHIELD" -> 0xFF_F1C40F;
            case "CHALLENGE" -> 0xFF_9B59B6;
            case "DICE" -> 0xFF_E67E22;
            case "SIZE" -> 0xFF_1ABC9C;
            case "MORPH" -> 0xFF_8E44AD;
            case "SIXSEVEN" -> 0xFF_F1C40F;
            case "CONTRACT" -> 0xFF_C0392B;
            case "TAUNT" -> 0xFF_E74C3C;
            case "TETHER" -> 0xFF_C39BD3;
            case "HOLD" -> 0xFF_F1C40F;
            case "VOTE" -> 0xFF_9B59B6;
            case "CIPHER" -> 0xFF_F1C40F;
            case "WORLD" -> 0xFF_F39C12;
            default -> 0xFF_9B59B6;
        };
    }
}
