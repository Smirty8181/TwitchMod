package net.twitchcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Разметка масштабируемой панели меню. */
public final class TwitchMenuLayout {
    private static final int BASE_W = 560;
    private static final int BASE_H = 340;
    private static final int BASE_SIDEBAR = 132;
    private static final int BASE_HEADER = 62;
    private static final int BASE_PAD = 10;
    private static final int BASE_TAB_H = 24;
    private static final int BASE_TAB_GAP = 3;
    private static final int BASE_CLOSE = 18;
    private static final int BASE_ZOOM_BTN = 16;

    public final int panelL, panelT, panelR, panelB;
    public final int contentL, contentT, contentR, contentB;
    public final int sidebarT, sidebarB;
    public final int sidebarW, headerH, footerH, pad, tabH, tabGap, closeSize;
    public final int closeX, closeY;
    public final int zoomOutX, zoomOutY, zoomInX, zoomLabelX, zoomBtnW, zoomBtnH;
    public final float scale;

    private TwitchMenuLayout(int screenW, int screenH, int baseFooterH, float scale) {
        this.scale = scale;
        int pw = Math.min(Math.round(BASE_W * scale), (int) (screenW * 0.96f));
        int ph = Math.min(Math.round(BASE_H * scale), (int) (screenH * 0.96f));
        panelL = (screenW - pw) / 2;
        panelT = (screenH - ph) / 2;
        panelR = panelL + pw;
        panelB = panelT + ph;

        sidebarW = Math.round(BASE_SIDEBAR * scale);
        headerH = Math.round(BASE_HEADER * scale);
        footerH = Math.round(baseFooterH * scale);
        pad = Math.max(6, Math.round(BASE_PAD * scale));
        tabH = Math.max(18, Math.round(BASE_TAB_H * scale));
        tabGap = Math.max(2, Math.round(BASE_TAB_GAP * scale));
        closeSize = Math.max(14, Math.round(BASE_CLOSE * scale));

        contentL = panelL + sidebarW + pad;
        contentR = panelR - pad - Math.max(4, Math.round(6 * scale));
        contentT = panelT + headerH + pad;
        contentB = panelB - footerH - pad;
        sidebarT = panelT + headerH + pad;
        sidebarB = panelB - footerH - pad;

        closeX = panelR - closeSize - pad;
        closeY = panelT + pad;

        zoomBtnW = Math.max(14, Math.round(BASE_ZOOM_BTN * scale));
        zoomBtnH = Math.max(14, Math.round(BASE_ZOOM_BTN * scale));
        zoomOutY = panelB - footerH + Math.max(3, Math.round(5 * scale));
        zoomOutX = panelL + pad;
        zoomInX = zoomOutX + zoomBtnW + Math.max(28, Math.round(36 * scale));
        zoomLabelX = zoomOutX + zoomBtnW + Math.max(4, Math.round(6 * scale));
    }

    public static TwitchMenuLayout compute(int screenW, int screenH, int baseFooterH) {
        return new TwitchMenuLayout(screenW, screenH, baseFooterH, TwitchMenuScale.get());
    }

    public int s(int base) {
        return Math.round(base * scale);
    }

    public boolean inPanel(double mx, double my) {
        return mx >= panelL && mx < panelR && my >= panelT && my < panelB;
    }

    public boolean adjustScale(float delta) {
        return TwitchMenuScale.adjust(delta);
    }

    public static void drawZoomControls(TwitchMenuLayout l, net.minecraft.client.gui.GuiGraphics g,
                                        net.minecraft.client.gui.Font font,
                                        boolean outHover, boolean inHover) {
        TwitchUi.drawSmallButton(g, font, l.zoomOutX, l.zoomOutY, l.zoomBtnW, l.zoomBtnH, "−", outHover);
        String label = TwitchMenuScale.percent() + "%";
        g.drawString(font, "§7" + label, l.zoomLabelX, l.zoomOutY + (l.zoomBtnH - 8) / 2, TwitchUi.TEXT_DIM, false);
        TwitchUi.drawSmallButton(g, font, l.zoomInX, l.zoomOutY, l.zoomBtnW, l.zoomBtnH, "+", inHover);
    }

    public static boolean handleZoomClick(TwitchMenuLayout l, double mx, double my) {
        if (mx >= l.zoomOutX && mx < l.zoomOutX + l.zoomBtnW
                && my >= l.zoomOutY && my < l.zoomOutY + l.zoomBtnH) {
            return TwitchMenuScale.adjust(-0.05f);
        }
        if (mx >= l.zoomInX && mx < l.zoomInX + l.zoomBtnW
                && my >= l.zoomOutY && my < l.zoomOutY + l.zoomBtnH) {
            return TwitchMenuScale.adjust(0.05f);
        }
        return false;
    }

    public static boolean handleZoomScroll(TwitchMenuLayout l, double mx, double my, double scrollY) {
        if (!isControlDown() && !isOverFooter(l, mx, my)) return false;
        if (!l.inPanel(mx, my)) return false;
        float delta = scrollY > 0 ? 0.05f : -0.05f;
        return TwitchMenuScale.adjust(delta);
    }

    public static boolean isControlDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private static boolean isOverFooter(TwitchMenuLayout l, double mx, double my) {
        int fy = l.panelB - l.footerH;
        return mx >= l.panelL && mx < l.panelR && my >= fy && my < l.panelB;
    }
}
