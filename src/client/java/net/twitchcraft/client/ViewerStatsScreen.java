package net.twitchcraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.network.ViewerStatsPayload;

import java.util.ArrayList;
import java.util.List;

/** HUD статистики зрителей (клавиша P): кто сколько команд юзал и любимая. */
public class ViewerStatsScreen extends Screen {
    private static final int BASE_FOOTER = 22;
    private static final int ROW_H = 36;

    private List<ViewerStatsPayload.ViewerRow> rows = new ArrayList<>();
    private double scroll;
    private boolean closeHover;
    private boolean refreshHover;
    private boolean waiting = true;
    private TwitchMenuLayout layout;
    private int selected = -1;

    public ViewerStatsScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        UiTheme.applyFromConfig();
        layout = TwitchMenuLayout.compute(width, height, BASE_FOOTER);
        rows = ViewerStatsClient.copyLatest();
        waiting = rows.isEmpty();
        ViewerStatsClient.request();
    }

    public void onStatsUpdated(List<ViewerStatsPayload.ViewerRow> updated) {
        rows = updated != null ? new ArrayList<>(updated) : new ArrayList<>();
        waiting = false;
        selected = Mth.clamp(selected, -1, rows.size() - 1);
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
        drawList(g, mouseX, mouseY);
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
        g.drawString(font, "§e§lСтатистика чата",
                layout.panelL + layout.pad + 2, y + layout.s(26), TwitchUi.TEXT, false);
        g.drawString(font, "§7Кто сколько раз юзал команды · любимая",
                layout.panelL + layout.pad + 2, y + layout.s(38), TwitchUi.TEXT_DIM, false);
        g.fill(layout.panelL + layout.pad, layout.panelT + layout.headerH - layout.s(4),
                layout.panelR - layout.pad, layout.panelT + layout.headerH - layout.s(3),
                TwitchUi.PURPLE & 0x66_FFFFFF);
    }

    private void drawList(GuiGraphics g, int mx, int my) {
        int listL = layout.panelL + layout.pad;
        int listR = layout.panelR - layout.pad;
        int listT = layout.panelT + layout.headerH + layout.s(4);
        int listB = layout.panelB - layout.footerH - layout.s(4);
        int listH = listB - listT;
        int listW = listR - listL;

        if (waiting && rows.isEmpty()) {
            String msg = "§7Загрузка…";
            g.drawString(font, msg, listL + (listW - font.width(TwitchUi.stripFormat(msg))) / 2,
                    listT + listH / 2 - 4, TwitchUi.TEXT_DIM, false);
            return;
        }
        if (rows.isEmpty()) {
            String msg = "§7Пока никто не использовал команды";
            g.drawString(font, msg, listL + (listW - font.width(TwitchUi.stripFormat(msg))) / 2,
                    listT + listH / 2 - 4, TwitchUi.TEXT_DIM, false);
            return;
        }

        // header columns
        g.drawString(font, "§8Зритель", listL + 6, listT, TwitchUi.TEXT_DIM, false);
        g.drawString(font, "§8Всего", listL + listW / 2 - 20, listT, TwitchUi.TEXT_DIM, false);
        g.drawString(font, "§8Любимая", listL + listW - 120, listT, TwitchUi.TEXT_DIM, false);

        int bodyT = listT + 12;
        int bodyH = listB - bodyT;
        int totalH = rows.size() * ROW_H;
        int maxScroll = Math.max(0, totalH - bodyH);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        g.enableScissor(listL, bodyT, listR, listB);
        int y = bodyT - (int) scroll;
        for (int i = 0; i < rows.size(); i++) {
            ViewerStatsPayload.ViewerRow row = rows.get(i);
            if (y + ROW_H >= bodyT && y <= listB) {
                boolean sel = i == selected;
                boolean hover = mx >= listL && mx < listR && my >= y && my < y + ROW_H - 2;
                int bg = sel ? TwitchUi.BG_TAB_ACTIVE : (hover ? 0xE8_14_12_1E : TwitchUi.BG_CARD);
                TwitchUi.roundedPanel(g, listL, y, listR, y + ROW_H - 3, bg);
                if (sel || hover) {
                    TwitchUi.fillGradientV(g, listL, y, listL + 2, y + ROW_H - 3,
                            TwitchUi.PURPLE, TwitchUi.ACCENT_ALT);
                }

                String name = "§f" + row.name();
                String total = "§e" + row.total();
                String fav = row.favoriteRu() == null || row.favoriteRu().isBlank() || "-".equals(row.favoriteRu())
                        ? "§8—"
                        : "§d!" + row.favoriteRu() + " §8×§f" + row.favoriteCount();

                g.drawString(font, name, listL + 8, y + 6, TwitchUi.TEXT, false);
                g.drawString(font, total, listL + listW / 2 - 10, y + 6, TwitchUi.TEXT, false);
                g.drawString(font, fav, listL + listW - Math.min(140, font.width(TwitchUi.stripFormat(fav)) + 8),
                        y + 6, TwitchUi.TEXT, false);

                String breakdown = formatTop(row);
                g.drawString(font, breakdown, listL + 8, y + 18, TwitchUi.TEXT_DIM, false);
            }
            y += ROW_H;
        }
        g.disableScissor();

        if (maxScroll > 0) {
            TwitchUi.drawScrollbar(g, listR - 4, bodyT, listB, scroll, maxScroll, bodyH, totalH);
        }
    }

    private String formatTop(ViewerStatsPayload.ViewerRow row) {
        if (row.top() == null || row.top().isEmpty()) return "§8нет данных";
        StringBuilder sb = new StringBuilder("§7");
        int shown = 0;
        for (ViewerStatsPayload.CmdUse c : row.top()) {
            if (shown >= 5) break;
            if (shown > 0) sb.append(" §8· ");
            sb.append("§b!").append(c.ru()).append("§8×§f").append(c.count());
            shown++;
        }
        String s = sb.toString();
        int maxW = (layout.panelR - layout.panelL) - layout.pad * 2 - 20;
        if (font.width(TwitchUi.stripFormat(s)) > maxW) {
            while (s.length() > 8 && font.width(TwitchUi.stripFormat(s) + "…") > maxW) {
                s = s.substring(0, s.length() - 1);
            }
            return s + "§8…";
        }
        return s;
    }

    private void drawFooter(GuiGraphics g, int mx, int my) {
        int fy = layout.panelB - layout.footerH;
        g.fill(layout.panelL + 1, fy, layout.panelR - 1, fy + 1, TwitchUi.BORDER);
        TwitchUi.fillGradientH(g, layout.panelL + 1, fy + 1, layout.panelR - 1, layout.panelB - 1,
                0xF0_12_12_16, 0xF0_0E_0E_10);

        int btnW = layout.s(72);
        int btnH = layout.s(14);
        int btnX = layout.panelL + layout.pad;
        int btnY = fy + (layout.footerH - btnH) / 2;
        refreshHover = over(mx, my, btnX, btnY, btnW, btnH);
        int btnBg = refreshHover ? TwitchUi.SAVE_BTN_HOVER : TwitchUi.SAVE_BTN;
        TwitchUi.roundedPanel(g, btnX, btnY, btnX + btnW, btnY + btnH, btnBg);
        String label = "Обновить";
        g.drawString(font, label, btnX + (btnW - font.width(label)) / 2, btnY + 3, TwitchUi.TEXT, false);

        g.drawString(font, "§7P — закрыть · колесо — скролл · " + rows.size() + " зрителей",
                btnX + btnW + layout.s(10), fy + layout.s(7), TwitchUi.TEXT_DIM, false);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll -= v * 18;
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mx = event.x();
        double my = event.y();
        if (closeHover) {
            onClose();
            return true;
        }
        if (refreshHover) {
            waiting = rows.isEmpty();
            ViewerStatsClient.request();
            return true;
        }

        int listL = layout.panelL + layout.pad;
        int listR = layout.panelR - layout.pad;
        int listT = layout.panelT + layout.headerH + layout.s(4) + 12;
        int listB = layout.panelB - layout.footerH - layout.s(4);
        if (mx >= listL && mx < listR && my >= listT && my < listB) {
            int idx = (int) ((my - listT + scroll) / ROW_H);
            if (idx >= 0 && idx < rows.size()) {
                selected = idx;
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_P || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
