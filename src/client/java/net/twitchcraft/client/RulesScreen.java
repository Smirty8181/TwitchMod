package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.rules.RulesContent;

import java.util.ArrayList;
import java.util.List;

public class RulesScreen extends Screen {
    private static final int BASE_FOOTER = 22;

    private List<RulesContent.Page> pages = List.of();
    private int selectedPage;
    private double contentScroll;
    private double sidebarScroll;
    private int hoveredTab = -1;
    private boolean closeHover;
    private boolean zoomOutHover;
    private boolean zoomInHover;

    private TwitchMenuLayout layout;
    private int cachedPage = -1;
    private float cachedScale = -1f;
    private int cachedContentW;
    private List<Integer> cachedHeights = List.of();
    private final Screen returnTo;

    public RulesScreen() {
        this(null);
    }

    public RulesScreen(Screen returnTo) {
        super(Component.empty());
        this.returnTo = returnTo;
    }

    @Override
    protected void init() {
        ConfigManager.load();
        UiTheme.applyFromConfig();
        pages = RulesContent.allPages();
        selectedPage = Mth.clamp(selectedPage, 0, pages.size() - 1);
        layout = TwitchMenuLayout.compute(width, height, BASE_FOOTER);
        invalidateHeightCache();
    }

    private void invalidateHeightCache() {
        cachedPage = -1;
        cachedHeights = List.of();
    }

    private void selectPage(int index) {
        if (index == selectedPage) return;
        selectedPage = index;
        contentScroll = 0;
        invalidateHeightCache();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (layout == null) {
            layout = TwitchMenuLayout.compute(width, height, BASE_FOOTER);
        }
        TwitchUi.drawDimBackground(g, width, height);

        TwitchUi.glassPanel(g, layout.panelL, layout.panelT, layout.panelR, layout.panelB);
        TwitchUi.accentBar(g, layout.panelL + 1, layout.panelT + 1, layout.panelR - 1, TwitchUi.PURPLE);

        drawHeader(g);
        drawSidebar(g, font, mouseX, mouseY);
        drawContentArea(g, mouseX, mouseY);
        drawFooter(g, mouseX, mouseY);

        closeHover = over(mouseX, mouseY, layout.closeX, layout.closeY, layout.closeSize, layout.closeSize);
        TwitchUi.drawCloseButton(g, font, layout.closeX, layout.closeY, layout.closeSize, closeHover);
    }

    private void drawHeader(GuiGraphics g) {
        int y = layout.panelT + layout.pad + layout.s(4);
        int logoSize = layout.s(16);
        SmirtyLogo.drawInline(g, layout.panelL + layout.pad + 2, y + 1, logoSize, TwitchUi.PURPLE);
        g.drawString(font, TwitchUi.TITLE_FORMAT, layout.panelL + layout.pad + logoSize + layout.s(6), y, TwitchUi.TEXT, false);
        g.drawString(font, "§8v" + ModVersion.friendly() + " §7· §d" + TwitchCraftMod.AUTHOR,
                layout.panelL + layout.pad + logoSize + layout.s(6), y + layout.s(11), TwitchUi.TEXT_DIM, false);

        RulesContent.Page page = pages.get(selectedPage);
        g.drawString(font, page.subtitle(), layout.panelL + layout.pad + 2, y + layout.s(22), TwitchUi.TEXT_DIM, false);

        g.drawString(font, page.icon() + " §l" + page.title(), layout.contentL, layout.panelT + layout.pad + layout.s(28), page.accentColor(), false);
        g.fill(layout.contentL, layout.panelT + layout.headerH - layout.s(4), layout.contentR, layout.panelT + layout.headerH - layout.s(3), page.accentColor() & 0x66_FFFFFF);
    }

    private void drawSidebar(GuiGraphics g, net.minecraft.client.gui.Font font, int mx, int my) {
        g.fill(layout.panelL + layout.sidebarW, layout.panelT + layout.headerH,
                layout.panelL + layout.sidebarW + 1, layout.panelB - layout.footerH, TwitchUi.BORDER);

        hoveredTab = -1;
        int totalTabsH = pages.size() * (layout.tabH + layout.tabGap) - layout.tabGap;
        int sidebarH = layout.sidebarB - layout.sidebarT;
        int maxSidebarScroll = Math.max(0, totalTabsH - sidebarH);
        sidebarScroll = Mth.clamp(sidebarScroll, 0, maxSidebarScroll);

        g.enableScissor(layout.panelL + layout.pad, layout.sidebarT, layout.panelL + layout.sidebarW, layout.sidebarB);
        int tabY = layout.sidebarT - (int) sidebarScroll;
        int tabW = layout.sidebarW - layout.pad * 2;

        for (int i = 0; i < pages.size(); i++) {
            RulesContent.Page page = pages.get(i);
            boolean active = i == selectedPage;
            boolean hover = mx >= layout.panelL + layout.pad && mx < layout.panelL + layout.sidebarW - layout.pad
                    && my >= tabY && my < tabY + layout.tabH;
            if (hover) hoveredTab = i;
            if (tabY + layout.tabH >= layout.sidebarT && tabY <= layout.sidebarB) {
                TwitchUi.drawTab(g, font, layout.panelL + layout.pad, tabY, tabW, layout.tabH,
                        page.icon(), page.title(), active, hover, page.accentColor());
            }
            tabY += layout.tabH + layout.tabGap;
        }
        g.disableScissor();

        if (maxSidebarScroll > 0) {
            TwitchUi.drawScrollbar(g, layout.panelL + layout.sidebarW - 5, layout.sidebarT, layout.sidebarB,
                    sidebarScroll, maxSidebarScroll, sidebarH, totalTabsH);
        }
    }

    private void drawContentArea(GuiGraphics g, int mx, int my) {
        RulesContent.Page page = pages.get(selectedPage);
        int contentW = layout.contentR - layout.contentL;
        int contentH = layout.contentB - layout.contentT;

        List<Integer> heights = blockHeights(page, contentW);
        int totalH = heights.stream().mapToInt(Integer::intValue).sum();
        int maxScroll = Math.max(0, totalH - contentH);
        contentScroll = Mth.clamp(contentScroll, 0, maxScroll);

        g.enableScissor(layout.contentL, layout.contentT, layout.contentR, layout.contentB);
        int y = layout.contentT - (int) contentScroll;
        for (int i = 0; i < page.blocks().size(); i++) {
            RulesContent.Block block = page.blocks().get(i);
            int h = heights.get(i);
            if (y + h >= layout.contentT && y <= layout.contentB) {
                renderBlock(g, block, layout.contentL, y, contentW, page.accentColor());
            }
            y += h;
        }
        g.disableScissor();

        if (maxScroll > 0) {
            TwitchUi.drawScrollbar(g, layout.contentR, layout.contentT, layout.contentB,
                    contentScroll, maxScroll, contentH, totalH);
        }
    }

    private void renderBlock(GuiGraphics g, RulesContent.Block block, int x, int y, int w, int accent) {
        switch (block) {
            case RulesContent.Block.Header h -> {
                g.drawString(font, h.text(), x + 2, y + layout.s(4), accent, false);
                g.fill(x, y + layout.s(16), x + w - layout.s(8), y + layout.s(17), accent & 0x44_FFFFFF);
            }
            case RulesContent.Block.Paragraph p ->
                    TwitchUi.drawWrappedText(g, font, x + 2, y + layout.s(2), w - layout.s(8), p.text(), TwitchUi.TEXT_DIM);
            case RulesContent.Block.Spacer ignored -> { }
            case RulesContent.Block.Divider d -> {
                g.fill(x + layout.s(4), y + layout.s(4), x + w - layout.s(4), y + layout.s(5), TwitchUi.BORDER);
            }
            case RulesContent.Block.CommandCard c ->
                    TwitchUi.drawCommandCard(g, font, x, y, w - layout.s(4), accent, c.command(), c.description(), c.cooldownSec());
            case RulesContent.Block.FeatureCard f ->
                    TwitchUi.drawFeatureCard(g, font, x, y, w - layout.s(4), accent, f.icon(), f.title(), f.description());
            case RulesContent.Block.ServerCmd sc ->
                    TwitchUi.drawServerCmd(g, font, x, y, w - layout.s(4), sc.command(), sc.description());
        }
    }

    private void drawFooter(GuiGraphics g, int mx, int my) {
        int fy = layout.panelB - layout.footerH;
        g.fill(layout.panelL + 1, fy, layout.panelR - 1, fy + 1, TwitchUi.BORDER);
        TwitchUi.fillGradientH(g, layout.panelL + 1, fy + 1, layout.panelR - 1, layout.panelB - 1, 0xF0_12_12_16, 0xF0_0E_0E_10);

        zoomOutHover = over(mx, my, layout.zoomOutX, layout.zoomOutY, layout.zoomBtnW, layout.zoomBtnH);
        zoomInHover = over(mx, my, layout.zoomInX, layout.zoomOutY, layout.zoomBtnW, layout.zoomBtnH);
        TwitchMenuLayout.drawZoomControls(layout, g, font, zoomOutHover, zoomInHover);

        g.drawString(font, MenuHints.RULES_FOOTER,
                layout.panelL + layout.pad + layout.s(56), fy + layout.s(7), TwitchUi.TEXT_DIM, false);
        String creditPlain = "by " + TwitchCraftMod.AUTHOR;
        g.drawString(font, "§7by §d" + TwitchCraftMod.AUTHOR,
                layout.panelR - layout.pad - font.width(creditPlain), fy + layout.s(7), TwitchUi.TEXT_DIM, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            if (closeHover) {
                onClose();
                return true;
            }
            if (TwitchMenuLayout.handleZoomClick(layout, event.x(), event.y())) {
                layout = TwitchMenuLayout.compute(width, height, BASE_FOOTER);
                invalidateHeightCache();
                return true;
            }
            if (hoveredTab >= 0) {
                selectPage(hoveredTab);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (TwitchMenuLayout.handleZoomScroll(layout, mouseX, mouseY, scrollY)) {
            layout = TwitchMenuLayout.compute(width, height, BASE_FOOTER);
            invalidateHeightCache();
            return true;
        }
        if (mouseX >= layout.panelL + layout.pad && mouseX < layout.panelL + layout.sidebarW
                && mouseY >= layout.sidebarT && mouseY <= layout.sidebarB) {
            int totalTabsH = pages.size() * (layout.tabH + layout.tabGap) - layout.tabGap;
            int max = Math.max(0, totalTabsH - (layout.sidebarB - layout.sidebarT));
            sidebarScroll = Mth.clamp(sidebarScroll - scrollY * layout.s(20), 0, max);
            return true;
        }
        if (mouseX >= layout.contentL && mouseX <= layout.contentR && mouseY >= layout.contentT && mouseY <= layout.contentB) {
            contentScroll = Mth.clamp(contentScroll - scrollY * layout.s(18), 0, contentMaxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private List<Integer> blockHeights(RulesContent.Page page, int contentW) {
        if (selectedPage == cachedPage && cachedScale == layout.scale && cachedContentW == contentW && !cachedHeights.isEmpty()) {
            return cachedHeights;
        }
        List<Integer> heights = new ArrayList<>();
        for (RulesContent.Block block : page.blocks()) {
            heights.add(TwitchUi.blockHeight(font, block, contentW, layout.scale));
        }
        cachedPage = selectedPage;
        cachedScale = layout.scale;
        cachedContentW = contentW;
        cachedHeights = heights;
        return heights;
    }

    private int contentMaxScroll() {
        RulesContent.Page page = pages.get(selectedPage);
        int contentH = layout.contentB - layout.contentT;
        int contentW = layout.contentR - layout.contentL;
        int total = blockHeights(page, contentW).stream().mapToInt(Integer::intValue).sum();
        return Math.max(0, total - contentH);
    }

    @Override
    public void onClose() {
        Minecraft mc = Minecraft.getInstance();
        if (returnTo != null) {
            mc.setScreen(returnTo);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
