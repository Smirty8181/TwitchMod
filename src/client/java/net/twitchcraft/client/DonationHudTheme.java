package net.twitchcraft.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.twitchcraft.donation.DonationEffectCatalog;
import net.twitchcraft.donation.DonationTierStyle;

/** Донат-toast — та же карточка, что у ленты команд. */
public final class DonationHudTheme {
    private DonationHudTheme() {}

    public static int panelHeight(int tierMin, boolean hasMessage) {
        int base = HudLayout.s(46);
        return hasMessage ? base + HudLayout.s(14) : base;
    }

    public static int panelWidth(int screenW, int tierMin) {
        return Math.min(HudLayout.s(276), screenW - HudLayout.s(24));
    }

    public static void drawPremiumCard(GuiGraphics g, Font font, int x, int y, int w, int h,
                                       int tierColor, float fade, int tierMin,
                                       String amountLine, String donor, String effect,
                                       String effectId, String message) {
        int alpha = (int) (fade * 255);
        if (alpha < 8) return;

        HudCard.drawPremium(g, x, y, w, h, tierColor, fade, 1f, true, System.currentTimeMillis());

        int pad = HudLayout.s(8);
        int av = HudLayout.s(24);
        int avX = x + pad;
        int avY = y + HudLayout.s(8);
        HudAvatars.draw(g, font, avX, avY, av, donor, fade, true);

        int textX = avX + av + HudLayout.s(7);

        String badge = tierMin >= 1000 ? "1K" : tierMin >= 500 ? "500+"
                : tierMin >= 200 ? "200+" : tierMin >= 100 ? "100+" : "50+";
        HudCard.drawBadge(g, font, x + w - font.width(badge) - HudLayout.s(18), y + HudLayout.s(6),
                badge, tierColor, fade);

        String name = truncate(donor, 16);
        g.drawString(font, "§7@§f§l" + name, textX, y + HudLayout.s(6),
                (alpha << 24) | 0xF4_F2_F8, false);

        int stripY = y + HudLayout.s(19);
        int stripH = HudLayout.s(15);
        int stripW = Math.max(HudLayout.s(72), w - (textX - x) - HudLayout.s(8));
        HudCard.drawCommandStrip(g, textX, stripY, stripW, stripH, tierColor, fade);
        String fx = truncate(effect == null || effect.isBlank()
                ? DonationEffectCatalog.exclusiveLabel(effectId) : effect, 18);
        String amountHero = DonationTierStyle.colorCodeForAmount(tierMin) + "§l" + amountLine
                + (fx.isBlank() ? "" : " §8· §7" + fx);
        g.drawString(font, amountHero, textX + HudLayout.s(5), stripY + Math.max(2, (stripH - 8) / 2),
                (alpha << 24) | 0xFFFFFF, false);

        if (message != null && !message.isBlank()) {
            int msgY = y + h - HudLayout.s(16);
            HudCard.drawCommandStrip(g, x + pad, msgY - 2, w - pad * 2, HudLayout.s(12), tierColor, fade * 0.7f);
            g.drawString(font, "§8«§7" + truncate(message, 32) + "§8»",
                    x + pad + HudLayout.s(4), msgY, (alpha << 24) | (TwitchUi.TEXT_DIM & 0xFFFFFF), false);
        }
    }

    public static void drawProgress(GuiGraphics g, int x, int y, int w, int h, int tierColor,
                                    float fade, float progress) {
        int pad = HudLayout.s(8);
        HudCard.drawProgressBar(g, x + pad, y + h - HudLayout.s(5),
                w - pad * 2, 3, Mth.clamp(progress, 0f, 1f), tierColor, fade);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
