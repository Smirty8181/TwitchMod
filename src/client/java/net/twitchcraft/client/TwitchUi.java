package net.twitchcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import net.twitchcraft.rules.RulesContent;

import java.util.List;

/** Общий UI TwitchCraft — аниме-минимализм, мягкие градиенты. */
public final class TwitchUi {
    public static int PURPLE = 0xFF_FF_7A_A8;
    public static int PURPLE_DARK = 0xFF_C4_3A_72;
    public static int PURPLE_LIGHT = 0xFF_FF_C4_DC;
    public static int ACCENT_ALT = 0xFF_7A_D4_FF;
    public static int BG_PANEL = 0xF8_07_08_10;
    public static int BG_CARD = 0xEC_0E_10_1A;
    public static int BG_TAB = 0xD8_0A_0C_14;
    public static int BG_TAB_ACTIVE = 0xF0_18_12_24;
    public static int BORDER = 0xFF_2C_28_3A;
    public static int TEXT = 0xFF_F8_F4_FA;
    public static int TEXT_DIM = 0xFF_9E_96_AC;
    public static int HUD_EDGE = 0x66_FF_7A_A8;
    public static int SAVE_BTN = 0xFF_D4_4A_82;
    public static int SAVE_BTN_HOVER = 0xFF_F0_6A_9E;
    public static String TITLE_FORMAT = "§d§lTwitch§b§lCraft";
    /** Мягкий персиковый акцент. */
    public static int GOLD = 0xFF_FF_C4_8A;

    private TwitchUi() {}

    public static boolean shouldDrawHud(Minecraft mc) {
        return mc.player != null && mc.font != null && !mc.options.hideGui
                && (mc.screen == null || mc.screen instanceof TestLabScreen);
    }

    public static int wrappedLineHeight(Font font, String text, int maxWidth) {
        return Math.max(1, font.split(Component.literal(text), maxWidth).size());
    }

    public static void drawWrappedText(GuiGraphics g, Font font, int x, int y, int maxWidth, String text, int color) {
        int lineY = y;
        for (var line : font.split(Component.literal(text), maxWidth)) {
            g.drawString(font, line, x, lineY, color, false);
            lineY += 10;
        }
    }

    public static void syncTheme(UiTheme.Palette p) {
        PURPLE = p.accent();
        PURPLE_DARK = p.accentDark();
        PURPLE_LIGHT = p.accentLight();
        ACCENT_ALT = p.accentAlt();
        BG_PANEL = p.bgPanel();
        BG_CARD = p.bgCard();
        BG_TAB = p.bgTab();
        BG_TAB_ACTIVE = p.bgTabActive();
        BORDER = p.border();
        TEXT = p.text();
        TEXT_DIM = p.textDim();
        HUD_EDGE = p.hudEdge();
        SAVE_BTN = p.saveBtn();
        SAVE_BTN_HOVER = p.saveBtnHover();
        TITLE_FORMAT = p.titleFormat();
        GOLD = blendRgb(p.accentLight(), 0xFF_FF_C4_8A, 0.35f);
    }

    public static void fillGradientH(GuiGraphics g, int x1, int y1, int x2, int y2, int left, int right) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0) return;
        int steps = Math.min(Math.max(w / 4, 3), 20);
        for (int i = 0; i < steps; i++) {
            float t = (i + 0.5f) / steps;
            int c = lerpArgb(left, right, t);
            int xa = x1 + i * w / steps;
            int xb = x1 + (i + 1) * w / steps;
            if (xb > xa) g.fill(xa, y1, xb, y2, c);
        }
    }

    public static void fillGradientV(GuiGraphics g, int x1, int y1, int x2, int y2, int top, int bottom) {
        g.fillGradient(x1, y1, x2, y2, top, bottom);
    }

    public static int lerpArgb(int c1, int c2, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void panelShadow(GuiGraphics g, int l, int t, int r, int b, int spread) {
        int s = Math.min(Math.max(spread, 1), 4);
        for (int i = s; i >= 1; i--) {
            int alpha = 18 * (s - i + 1) / s;
            g.fill(l + i, t + i, r + i, b + i, (alpha << 24));
        }
    }

    public static void roundedPanel(GuiGraphics g, int l, int t, int r, int b, int fillColor) {
        g.fill(l + 2, t, r - 2, b, fillColor);
        g.fill(l, t + 2, r, b - 2, fillColor);
        g.fill(l + 1, t + 1, r - 1, b - 1, fillColor);
    }

    /** Стеклянная панель: V-градиент + блик + dual-accent кромка. */
    public static void glassPanel(GuiGraphics g, int l, int t, int r, int b) {
        panelShadow(g, l, t, r, b, 3);
        fillGradientV(g, l, t, r, b,
                (BG_PANEL & 0x00_FFFFFF) | 0xF0_00_00_00,
                (BG_CARD & 0x00_FFFFFF) | 0xF8_00_00_00);
        roundedPanel(g, l, t, r, b, BG_PANEL);
        fillGradientV(g, l + 2, t + 1, r - 2, t + Math.max(10, (b - t) / 5),
                0x28_FF_FF_FF, 0x00_FF_FF_FF);
        fillGradientH(g, l + 2, t, r - 2, t + 2, PURPLE, ACCENT_ALT & 0xAA_FFFFFF);
        g.fill(l + 2, b - 1, r - 2, b, (BORDER & 0x00_FFFFFF) | 0x55_00_00_00);
    }

    public static void accentBar(GuiGraphics g, int l, int t, int r, int color) {
        fillGradientH(g, l, t, r, t + 2, color, ACCENT_ALT);
        fillGradientH(g, l, t + 2, r, t + 3, (color & 0x00_FFFFFF) | 0x44_00_00_00, 0x00_00_00_00);
    }

    public static void drawDimBackground(GuiGraphics g, int width, int height) {
        fillGradientV(g, 0, 0, width, height, 0xD0_04_04_0A, 0xC8_08_06_10);
        fillGradientH(g, 0, 0, Math.max(1, width / 5), height, (PURPLE & 0x00_FFFFFF) | 0x18_00_00_00, 0x00_00_00_00);
        fillGradientH(g, width * 4 / 5, 0, width, height, 0x00_00_00_00, (ACCENT_ALT & 0x00_FFFFFF) | 0x14_00_00_00);
    }

    public static void drawTab(GuiGraphics g, Font font, int x, int y, int w, int h,
                               String icon, String label, boolean active, boolean hover, int accent) {
        if (active) {
            fillGradientV(g, x, y, x + w, y + h,
                    (BG_TAB_ACTIVE & 0x00_FFFFFF) | 0xF0_00_00_00,
                    (accent & 0x00_FFFFFF) | 0x28_00_00_00);
            roundedPanel(g, x, y, x + w, y + h, BG_TAB_ACTIVE);
            fillGradientV(g, x, y + 1, x + 2, y + h - 1, accent, PURPLE_DARK);
            fillGradientH(g, x + 2, y + h - 2, x + w - 1, y + h, accent & 0x88_FFFFFF, 0x00_00_00_00);
        } else {
            int bg = hover ? (blendRgb(BG_TAB, accent, 0.12f) & 0x00_FFFFFF) | 0xE8_00_00_00 : BG_TAB;
            roundedPanel(g, x, y, x + w, y + h, bg);
            if (hover) {
                fillGradientH(g, x + 2, y, x + w - 2, y + 1, accent & 0x44_FFFFFF, 0x00_00_00_00);
            }
        }
        g.drawString(font, icon, x + 8, y + (h - 8) / 2, active ? accent : TEXT_DIM, false);
        g.drawString(font, label, x + 22, y + (h - 8) / 2, active ? TEXT : TEXT_DIM, false);
    }

    public static void drawCloseButton(GuiGraphics g, Font font, int x, int y, int size, boolean hover) {
        int bg = hover ? 0xFF_E8_4A_6A : 0xCC_2A_18_24;
        roundedPanel(g, x, y, x + size, y + size, bg);
        if (hover) {
            fillGradientV(g, x + 1, y + 1, x + size - 1, y + size / 2, 0x55_FF_FF_FF, 0x00_00_00_00);
        }
        g.drawString(font, "✕", x + size / 2 - 3, y + size / 2 - 4, 0xFF_FF_FF_FF, false);
    }

    public static void drawSaveButton(GuiGraphics g, Font font, int x, int y, int w, int h, boolean hover) {
        int top = hover ? SAVE_BTN_HOVER : SAVE_BTN;
        int bot = blendRgb(top, 0x000000, 0.25f);
        fillGradientV(g, x, y, x + w, y + h, top, bot);
        roundedPanel(g, x, y, x + w, y + h, top);
        fillGradientV(g, x + 2, y + 1, x + w - 2, y + h / 2, 0x44_FF_FF_FF, 0x00_00_00_00);
        fillGradientH(g, x + 2, y, x + w - 2, y + 1, ACCENT_ALT & 0x66_FFFFFF, 0x00_00_00_00);
        g.drawString(font, "§f§lСохранить", x + (w - font.width("Сохранить")) / 2, y + (h - 8) / 2, TEXT, false);
    }

    public static void drawToggleRow(GuiGraphics g, Font font, int x, int y, int w, String label, boolean on, boolean hover) {
        roundedPanel(g, x, y, x + w, y + 24,
                hover ? (blendRgb(BG_CARD, PURPLE, 0.08f) & 0x00_FFFFFF) | 0xF0_00_00_00 : BG_CARD);
        g.fill(x, y + 4, x + 2, y + 20, on ? PURPLE : BORDER);
        g.drawString(font, label, x + 10, y + 8, TEXT, false);
        int pillW = 30;
        int pillH = 12;
        int pillX = x + w - pillW - 10;
        int pillY = y + 6;
        if (on) {
            fillGradientH(g, pillX, pillY, pillX + pillW, pillY + pillH, PURPLE, ACCENT_ALT);
        }
        roundedPanel(g, pillX, pillY, pillX + pillW, pillY + pillH,
                on ? (PURPLE & 0x00_FFFFFF) | 0xEE_00_00_00 : 0xFF_22_20_2A);
        int knob = on ? pillX + pillW - 12 : pillX + 2;
        g.fill(knob, pillY + 1, knob + 10, pillY + pillH - 1, on ? 0xFF_FF_FF_FF : 0xFF_77_72_82);
        g.drawString(font, on ? "§aВКЛ" : "§cВЫКЛ", pillX - font.width(on ? "ВКЛ" : "ВЫКЛ") - 4, y + 8,
                on ? 0xFF_7A_E8_B0 : 0xFF_E0_70_80, false);
    }

    public static int blendRgb(int c1, int c2, float t) {
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return 0xFF_00_00_00 | (r << 16) | (g << 8) | b;
    }

    public static void drawMiniPreview(GuiGraphics g, Font font, int x, int y, int w, UiTheme.Palette palette) {
        int h = 52;
        roundedPanel(g, x, y, x + w, y + h, palette.bgCard());
        fillGradientH(g, x + 2, y + 2, x + w - 2, y + 5, palette.accent(), palette.accentAlt());
        g.fill(x + 8, y + 12, x + 10, y + 30, palette.accent());
        g.drawString(font, "§fСайдбар", x + 16, y + 12, palette.text(), false);
        g.drawString(font, "§7!молния", x + 16, y + 24, palette.textDim(), false);
        int cardY = y + 36;
        roundedPanel(g, x + 8, cardY, x + w - 8, cardY + 12, 0xE0_0C_0C_14);
        fillGradientV(g, x + 8, cardY, x + 10, cardY + 12, palette.accent(), palette.accentAlt());
        g.drawString(font, "§6500₽ §fДонат", x + 14, cardY + 2, palette.text(), false);
    }

    public static int intAdjustStep() {
        return hasShiftDown() ? 10 : 1;
    }

    public static boolean hasShiftDown() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;
        var window = mc.getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static int adjustInt(int current, int delta, int min, int max) {
        return Mth.clamp(current + delta, min, max);
    }

    public static void drawIntRow(GuiGraphics g, Font font, int x, int y, int w, String label, int value) {
        roundedPanel(g, x, y, x + w, y + 24, BG_CARD);
        fillGradientH(g, x, y, x + 2, y + 24, PURPLE & 0x88_FFFFFF, ACCENT_ALT & 0x44_FFFFFF);
        g.drawString(font, label, x + 10, y + 8, TEXT, false);
        String val = String.valueOf(value);
        g.drawString(font, "§e" + val, x + w - font.width(val) - 50, y + 8, GOLD, false);
    }

    public static void drawSmallButton(GuiGraphics g, Font font, int x, int y, int w, int h, String text, boolean hover) {
        if (hover) {
            fillGradientH(g, x, y, x + w, y + h, PURPLE & 0x55_FFFFFF, ACCENT_ALT & 0x33_FFFFFF);
        }
        roundedPanel(g, x, y, x + w, y + h, hover ? blendRgb(0xFF_3A_34_48, PURPLE, 0.25f) : 0xFF_28_26_34);
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2, TEXT, false);
    }

    public static void drawThemeButton(GuiGraphics g, Font font, int x, int y, int w, int h,
                                     String label, int accent, boolean selected, boolean hover) {
        int bg = selected ? blendAccent(0xE0_18_14_24) : (hover ? 0xF0_1A_18_22 : BG_CARD);
        roundedPanel(g, x, y, x + w, y + h, bg);
        if (selected) {
            fillGradientV(g, x, y + 2, x + 2, y + h - 2, accent, ACCENT_ALT);
            fillGradientH(g, x, y, x + w, y + 1, accent, accent & 0x33_FFFFFF);
            fillGradientH(g, x, y + h - 1, x + w, y + h, accent & 0x88_FFFFFF, 0x00_00_00_00);
        }
        fillGradientH(g, x + 4, y + 3, x + w - 4, y + 6, accent, ACCENT_ALT & 0xAA_FFFFFF);
        String text = label;
        int textW = font.width(text);
        int maxW = w - 10;
        if (textW > maxW) {
            String plain = stripFormat(text);
            while (plain.length() > 2 && font.width(text) > maxW) {
                plain = plain.substring(0, plain.length() - 1);
                text = plain + "…";
            }
            textW = font.width(text);
        }
        int textX = x + Math.max(6, (w - textW) / 2);
        g.drawString(font, text, textX, y + 10, selected ? TEXT : TEXT_DIM, false);
    }

    public static void drawLabel(GuiGraphics g, Font font, int x, int y, String text) {
        g.drawString(font, text, x, y, TEXT_DIM, false);
    }

    public static void drawCommandCard(GuiGraphics g, Font font, int x, int y, int w, int accent,
                                       String command, String desc, int cd) {
        int h = 30;
        fillGradientV(g, x, y, x + w, y + h,
                (BG_CARD & 0x00_FFFFFF) | 0xF0_00_00_00,
                (accent & 0x00_FFFFFF) | 0x12_00_00_00);
        roundedPanel(g, x, y, x + w, y + h, BG_CARD);
        fillGradientV(g, x, y + 2, x + 2, y + h - 2, accent, ACCENT_ALT);
        g.drawString(font, "§e" + command, x + 10, y + 6, GOLD, false);
        String cdLabel = cd > 0 ? cd + "с" : "∞";
        int cdW = font.width(cdLabel) + 8;
        int cdX = x + w - cdW - 6;
        roundedPanel(g, cdX, y + 5, cdX + cdW, y + 17, cd > 0 ? 0xFF_22_20_2A : blendAccent(0x3D_2A_4A));
        g.drawString(font, cdLabel, cdX + 4, y + 7, cd > 0 ? TEXT_DIM : PURPLE_LIGHT, false);
        String shortDesc = desc.length() > 52 ? desc.substring(0, 51) + "…" : desc;
        g.drawString(font, "§7" + shortDesc, x + 10, y + 18, TEXT_DIM, false);
    }

    public static void drawFeatureCard(GuiGraphics g, Font font, int x, int y, int w, int accent,
                                       String icon, String title, String desc) {
        String[] lines = desc.split("\n");
        int h = 22 + lines.length * 10;
        roundedPanel(g, x, y, x + w, y + h, BG_CARD);
        fillGradientH(g, x, y, x + w, y + 2, accent, ACCENT_ALT & 0x66_FFFFFF);
        g.drawString(font, icon, x + 8, y + 8, TEXT, false);
        g.drawString(font, "§f§l" + title, x + 26, y + 8, TEXT, false);
        int ly = y + 20;
        for (String line : lines) {
            g.drawString(font, line, x + 10, ly, TEXT_DIM, false);
            ly += 10;
        }
    }

    public static void drawServerCmd(GuiGraphics g, Font font, int x, int y, int w, String cmd, String desc) {
        int h = 22;
        roundedPanel(g, x, y, x + w, y + h, 0xD8_0C_0E_14);
        g.drawString(font, "§b" + cmd, x + 8, y + 4, ACCENT_ALT, false);
        g.drawString(font, "§8— §7" + desc, x + 8 + font.width(cmd) + 4, y + 4, TEXT_DIM, false);
    }

    public static void drawScrollbar(GuiGraphics g, int x, int top, int bottom, double scroll, int maxScroll, int contentH, int totalH) {
        g.fill(x, top, x + 3, bottom, 0x33_00_00_00);
        if (maxScroll <= 0) return;
        int thumbH = Mth.clamp((int) ((float) contentH / totalH * contentH), 18, contentH);
        int thumbY = top + (int) (scroll / maxScroll * (contentH - thumbH));
        fillGradientV(g, x, thumbY, x + 3, thumbY + thumbH, PURPLE, ACCENT_ALT);
    }

    public static void drawHudPanel(GuiGraphics g, int x, int y, int w, int h, int accent, float pulse) {
        HudCard.drawPremium(g, x, y, w, h, accent, Mth.clamp(pulse, 0.55f, 1f), 1f, false, 0);
    }

    public static void drawSettingsFrame(GuiGraphics g, int l, int t, int r, int b, int sidebarW, int accent) {
        panelShadow(g, l, t, r, b, 4);
        glassPanel(g, l, t, r, b);
        fillGradientV(g, l + 1, t + 3, l + sidebarW, b - 1,
                (accent & 0x00_FFFFFF) | 0x18_00_00_00, 0x00_00_00_00);
        accentBar(g, l + 1, t + 1, r - 1, accent);
        fillGradientV(g, l + sidebarW, t + 4, l + sidebarW + 1, b - layoutFooterOffset(b, t),
                accent & 0x66_FFFFFF, ACCENT_ALT & 0x22_FFFFFF);
    }

    private static int layoutFooterOffset(int b, int t) {
        return 28;
    }

    public static void drawTabAccentLine(GuiGraphics g, int x, int y, int w, int color) {
        fillGradientH(g, x, y, x + w, y + 2, color, ACCENT_ALT & 0x66_FFFFFF);
        g.fill(x, y + 2, x + w, y + 3, color & 0x33_FFFFFF);
    }

    public static void drawCategoryHeader(GuiGraphics g, Font font, int x, int y, int w, String label, int accent) {
        fillGradientH(g, x, y, x + w, y + 1, accent, ACCENT_ALT & 0x44_FFFFFF);
        g.drawString(font, "§l" + label, x + 6, y + 4, accent, false);
    }

    public static int textWidthBold(Font font, String plain) {
        return (int) (font.width(plain) * 1.14f) + 2;
    }

    public static void drawHint(GuiGraphics g, Font font, int x, int y, String text) {
        g.drawString(font, text, x, y, TEXT_DIM, false);
    }

    public record LinkHit(int x, int y, int w, int h, String url) {}

    public static String stripFormat(String text) {
        return text.replaceAll("§.", "");
    }

    public static void drawHintLink(GuiGraphics g, Font font, int x, int y,
                                    TwitchSetupHints.HintWithLink hint, List<LinkHit> hits,
                                    int mouseX, int mouseY) {
        int cx = x;
        if (!hint.before().isEmpty()) {
            g.drawString(font, hint.before(), cx, y, TEXT_DIM, false);
            cx += font.width(stripFormat(hint.before()));
        }
        int linkW = font.width(stripFormat(hint.linkText()));
        boolean hover = mouseX >= cx && mouseX < cx + linkW && mouseY >= y && mouseY < y + 9;
        int linkColor = hover ? PURPLE_LIGHT : ACCENT_ALT;
        g.drawString(font, hint.linkText(), cx, y, linkColor, false);
        hits.add(new LinkHit(cx, y, linkW, 9, hint.url()));
        cx += linkW;
        if (!hint.after().isEmpty()) {
            g.drawString(font, hint.after(), cx, y, TEXT_DIM, false);
        }
    }

    public static void openLink(String url) {
        net.minecraft.Util.getPlatform().openUri(url);
    }

    public static int blockHeight(Font font, RulesContent.Block block, int contentW, float scale) {
        return switch (block) {
            case RulesContent.Block.Header h -> Math.round(18 * scale);
            case RulesContent.Block.Paragraph p -> Math.round(
                    (wrappedLineHeight(font, p.text(), contentW - (int) (8 * scale)) * 10 + 4) * scale);
            case RulesContent.Block.Spacer s -> Math.round(s.px() * scale);
            case RulesContent.Block.Divider d -> Math.round(10 * scale);
            case RulesContent.Block.CommandCard c -> Math.round(34 * scale);
            case RulesContent.Block.FeatureCard f ->
                    Math.round((22 + f.description().split("\n").length * 10 + 4) * scale);
            case RulesContent.Block.ServerCmd s -> Math.round(26 * scale);
        };
    }

    private static int blendAccent(int baseRgb) {
        return (baseRgb & 0x00_FFFFFF) | ((PURPLE & 0x33) << 24);
    }
}
