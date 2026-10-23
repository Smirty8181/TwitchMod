package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Справа снизу: HP/голод напарника над счётом смертей. Ник и аватар с Twitch. */
public final class DuoScoreOverlay {
    private static final int ACCENT = 0xFF_E7_4C_3C;
    private static final int GOLD = 0xFF_F5_D0_6E;
    private static final int HP_OK = 0xFF_55_CC_55;
    private static final long PULSE_MS = 420;

    private static String leftTwitch = "";
    private static String leftName = "";
    private static String rightTwitch = "";
    private static String rightName = "";
    private static int leftDeaths;
    private static int rightDeaths;
    private static int rightHp;
    private static int rightMaxHp = 20;
    private static int rightFood;
    private static boolean active;
    private static long pulseUntil;

    private DuoScoreOverlay() {}

    public static void apply(String lTwitch, String lName, int lDeaths,
                             String rTwitch, String rName, int rDeaths,
                             int rHp, int rMaxHp, int rFood, boolean on) {
        boolean changed = on && (lDeaths != leftDeaths || rDeaths != rightDeaths
                || rHp < rightHp || !active);
        leftTwitch = safe(lTwitch);
        leftName = safe(lName);
        rightTwitch = safe(rTwitch);
        rightName = safe(rName);
        leftDeaths = Math.max(0, lDeaths);
        rightDeaths = Math.max(0, rDeaths);
        rightHp = Math.max(0, rHp);
        rightMaxHp = Math.max(1, rMaxHp);
        rightFood = Mth.clamp(rFood, 0, 20);
        active = on && !leftName.isBlank() && !rightName.isBlank();
        if (changed && active) pulseUntil = System.currentTimeMillis() + PULSE_MS;
        if (active) {
            TwitchAvatarCache.request(avatarKey(leftTwitch, leftName));
            TwitchAvatarCache.request(avatarKey(rightTwitch, rightName));
        }
    }

    public static void render(GuiGraphics g, float delta) {
        if (!active) return;
        if (!HudResolver.enabled(HudResolver.Element.DUO_SCORE)) return;

        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc)) return;

        Font font = mc.font;
        var el = HudResolver.el(HudResolver.Element.DUO_SCORE);
        float es = HudResolver.elementScale(HudResolver.Element.DUO_SCORE);
        float opacity = HudResolver.opacity(HudResolver.Element.DUO_SCORE);
        int accent = HudResolver.accent(HudResolver.Element.DUO_SCORE, ACCENT);

        long now = System.currentTimeMillis();
        boolean lowHp = rightHp * 4 <= rightMaxHp;
        float pulse = now < pulseUntil || lowHp
                ? 1f + 0.08f * Mth.sin((now % PULSE_MS) / (float) PULSE_MS * ((float) Math.PI * 2f))
                : 1f;

        int av = HudLayout.s(16, es);
        int pad = HudLayout.s(6, es);
        int gap = HudLayout.s(4, es);
        String score = leftDeaths + " : " + rightDeaths;
        int leftW = av + gap + font.width(leftName);
        int midW = font.width(score);
        int rightW = font.width(rightName) + gap + av;
        int w = pad * 2 + leftW + HudLayout.s(10, es) + midW + HudLayout.s(10, es) + rightW;
        w = Math.max(w, HudLayout.s(148, es));
        int vitalsH = HudLayout.s(22, es);
        int scoreH = HudLayout.s(28, es);
        int h = vitalsH + scoreH;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        HudPlacement.Rect pos = HudPlacement.place(el.anchor, el.offsetX, el.offsetY, sw, sh, w, h, es);
        int x = pos.x();
        int y = pos.y();

        HudCard.drawPremium(g, x, y, w, h, lowHp ? ACCENT : accent, opacity * pulse, 1f,
                leftDeaths != rightDeaths || lowHp, System.currentTimeMillis());

        int dim = ((int) (opacity * 255) << 24) | 0x9E_96_AC;
        int nameCol = ((int) (opacity * 255) << 24) | 0xF4_F0_E8;
        int innerL = x + pad;
        int innerR = x + w - pad;
        int barH = Math.max(3, HudLayout.s(4, es));
        int labelW = font.width("еда");
        int numW = font.width("20");
        int barX = innerL + labelW + gap;
        int barW = Math.max(HudLayout.s(40, es), innerR - barX - numW - 2);

        g.drawString(font, "HP", innerL, y + 2, lowHp ? ACCENT : HP_OK, false);
        HudCard.drawProgressBar(g, barX, y + 4, barW, barH,
                rightHp / (float) rightMaxHp, lowHp ? ACCENT : HP_OK, opacity);
        g.drawString(font, String.valueOf(rightHp), barX + barW + 2, y + 2, nameCol, false);

        int foodY = y + HudLayout.s(12, es);
        g.drawString(font, "еда", innerL, foodY, GOLD, false);
        HudCard.drawProgressBar(g, barX, foodY + 2, barW, barH,
                rightFood / 20f, GOLD, opacity);
        g.drawString(font, String.valueOf(rightFood), barX + barW + 2, foodY, nameCol, false);

        boolean leftLead = leftDeaths > rightDeaths;
        boolean rightLead = rightDeaths > leftDeaths;
        int hot = ((int) (opacity * 255) << 24) | (accent & 0xFFFFFF);
        int mid = ((int) (opacity * 255) << 24) | (GOLD & 0xFFFFFF);

        int scoreY = y + vitalsH;
        int cy = scoreY + (scoreH - 8) / 2;
        int cursor = x + pad;
        HudAvatars.draw(g, font, cursor, scoreY + (scoreH - av) / 2, av, avatarKey(leftTwitch, leftName), opacity, false);
        cursor += av + gap;
        g.drawString(font, leftName, cursor, cy, leftLead ? hot : nameCol, false);
        cursor += font.width(leftName) + HudLayout.s(10, es);
        g.drawString(font, score, cursor, cy, leftLead || rightLead ? mid : dim, false);
        cursor += midW + HudLayout.s(10, es);
        g.drawString(font, rightName, cursor, cy, rightLead ? hot : nameCol, false);
        cursor += font.width(rightName) + gap;
        HudAvatars.draw(g, font, cursor, scoreY + (scoreH - av) / 2, av, avatarKey(rightTwitch, rightName), opacity, false);
    }

    private static String avatarKey(String twitch, String fallback) {
        return !twitch.isBlank() ? twitch : fallback;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
