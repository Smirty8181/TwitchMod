package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Полноэкранный алерт комбо-цепочки чата. */
public final class ComboChainOverlay {
    private static String user1 = "";
    private static String user2 = "";
    private static String user3 = "";
    private static long showUntil;
    private static long showStart;

    private ComboChainOverlay() {}

    public static void show(String u1, String u2, String u3) {
        if (!HudResolver.enabled(HudResolver.Element.COMBO_CHAIN)) return;
        user1 = u1;
        user2 = u2;
        user3 = u3;
        showStart = System.currentTimeMillis();
        showUntil = showStart + 6000;
    }

    public static void render(GuiGraphics g, float delta) {
        if (System.currentTimeMillis() > showUntil) return;
        if (!HudResolver.enabled(HudResolver.Element.COMBO_CHAIN)) return;
        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc)) return;

        var el = HudResolver.el(HudResolver.Element.COMBO_CHAIN);
        float es = HudResolver.elementScale(HudResolver.Element.COMBO_CHAIN);
        float opacity = HudResolver.opacity(HudResolver.Element.COMBO_CHAIN);

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        long elapsed = System.currentTimeMillis() - showStart;
        float fadeIn = Math.min(1f, elapsed / 400f);
        float fadeOut = (showUntil - System.currentTimeMillis()) < 800
                ? (showUntil - System.currentTimeMillis()) / 800f : 1f;
        float alpha = fadeIn * fadeOut * opacity;

        int overlay = ((int) (alpha * 40) << 24);
        g.fill(0, 0, w, h, overlay);

        int panelW = Math.min(w - 40, HudLayout.s(292, es));
        int panelH = HudLayout.s(74, es);
        HudPlacement.Rect pos = HudPlacement.place(el.anchor, el.offsetX, el.offsetY, w, h, panelW, panelH, es);
        int px = pos.x();
        int py = pos.y();

        int accent = HudResolver.accent(HudResolver.Element.COMBO_CHAIN, 0xFF_FF_44_55);
        HudCard.drawPremium(g, px, py, panelW, panelH, accent, alpha, 1f, true, System.currentTimeMillis());

        int cx = px + panelW / 2;
        var font = mc.font;
        HudCard.drawBadge(g, font, px + HudLayout.s(10, es), py + HudLayout.s(8, es),
                "ЦЕПЬ", accent, alpha);

        String title = "КОМБО";
        int titleW = font.width(title);
        g.drawString(font, "§c§l" + title, cx - titleW / 2, py + HudLayout.s(9, es), 0xFF_FF_66_77, false);

        String chain = "§e!" + user1 + " §8→ §a!" + user2 + " §8→ §c!" + user3;
        int chainW = font.width(TwitchUi.stripFormat(chain));
        int stripY = py + HudLayout.s(26, es);
        HudCard.drawCommandStrip(g, px + HudLayout.s(12, es), stripY,
                panelW - HudLayout.s(24, es), HudLayout.s(16, es), accent, alpha);
        g.drawString(font, chain, cx - chainW / 2, stripY + HudLayout.s(4, es), TwitchUi.TEXT, false);

        String sub = "Молния → Крипер → TNT";
        int subW = font.width(sub);
        g.drawString(font, "§7" + sub, cx - subW / 2, py + HudLayout.s(46, es), TwitchUi.TEXT_DIM, false);

        float progress = 1f - (float) (System.currentTimeMillis() - showStart) / 6000f;
        HudCard.drawProgressBar(g, px + HudLayout.s(12, es), py + panelH - HudLayout.s(8, es),
                panelW - HudLayout.s(24, es), HudLayout.s(3, es), Mth.clamp(progress, 0, 1), accent, alpha);
    }
}
