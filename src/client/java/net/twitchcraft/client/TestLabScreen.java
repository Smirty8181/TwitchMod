package net.twitchcraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;
import net.twitchcraft.stream.HouseMode;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Меню тестов стримера — клавиша I. Донаты, колесо, кубик, команды. */
public class TestLabScreen extends Screen {
    private static final int BASE_FOOTER = 24;

    private enum Tab {
        DONATE("💰", "Донаты", 0xFF_FF_D7_00),
        SHOW("🎡", "Шоу", 0xFF_E7_4C_3C),
        COMMANDS("⌨", "Команды", 0xFF_91_46_FF),
        STREAMER("⚡", "Стример", 0xFF_74_C0_FC);

        final String icon;
        final String title;
        final int color;

        Tab(String icon, String title, int color) {
            this.icon = icon;
            this.title = title;
            this.color = color;
        }
    }

    private record Hit(int x, int y, int w, int h, String title, String sub, int color, Runnable run) {}

    private Tab tab = Tab.DONATE;
    private ChatGameCommand.Category cmdFilter;
    private double scroll;
    private String status = "§7Жми кнопку — эффект сразу";
    private TwitchMenuLayout layout;
    private final List<Hit> hits = new ArrayList<>();
    private final List<Hit> tabHits = new ArrayList<>();
    private final List<Hit> filterHits = new ArrayList<>();
    private boolean closeHover;
    private int contentT;
    private int contentB;

    public TestLabScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        UiTheme.applyFromConfig();
        layout = TwitchMenuLayout.compute(width, height, BASE_FOOTER);
        scroll = 0;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (layout == null) {
            layout = TwitchMenuLayout.compute(width, height, BASE_FOOTER);
        }
        hits.clear();
        tabHits.clear();
        filterHits.clear();

        TwitchUi.drawDimBackground(g, width, height);
        TwitchUi.glassPanel(g, layout.panelL, layout.panelT, layout.panelR, layout.panelB);
        TwitchUi.accentBar(g, layout.panelL + 1, layout.panelT + 1, layout.panelR - 1, tab.color);

        drawHeader(g, mouseX, mouseY);
        drawBody(g, mouseX, mouseY);
        drawFooter(g);

        closeHover = over(mouseX, mouseY, layout.closeX, layout.closeY, layout.closeSize, layout.closeSize);
        TwitchUi.drawCloseButton(g, font, layout.closeX, layout.closeY, layout.closeSize, closeHover);
    }

    private void drawHeader(GuiGraphics g, int mx, int my) {
        int y = layout.panelT + layout.pad + layout.s(2);
        int logo = layout.s(16);
        SmirtyLogo.drawInline(g, layout.panelL + layout.pad + 2, y + 1, logo, tab.color);
        g.drawString(font, TwitchUi.TITLE_FORMAT, layout.panelL + layout.pad + logo + layout.s(6), y, TwitchUi.TEXT, false);
        g.drawString(font, "§8v" + ModVersion.friendly() + " §7· §eлаборатория тестов",
                layout.panelL + layout.pad + logo + layout.s(6), y + layout.s(11), TwitchUi.TEXT_DIM, false);

        int tabY = layout.panelT + layout.headerH - layout.tabH - layout.s(4);
        int tabX = layout.panelL + layout.pad;
        int tabW = (layout.panelR - layout.panelL - layout.pad * 2 - layout.s(22) - layout.tabGap * 3) / 4;
        for (Tab t : Tab.values()) {
            boolean active = t == tab;
            boolean hover = over(mx, my, tabX, tabY, tabW, layout.tabH);
            TwitchUi.drawTab(g, font, tabX, tabY, tabW, layout.tabH, t.icon, t.title, active, hover, t.color);
            tabHits.add(new Hit(tabX, tabY, tabW, layout.tabH, t.title, "", t.color, () -> switchTab(t)));
            tabX += tabW + layout.tabGap;
        }
    }

    private void drawBody(GuiGraphics g, int mx, int my) {
        int listL = layout.panelL + layout.pad;
        int listR = layout.panelR - layout.pad;
        contentT = layout.panelT + layout.headerH + layout.s(4);
        contentB = layout.panelB - layout.footerH - layout.s(4);

        int innerT = contentT;
        if (tab == Tab.COMMANDS) {
            innerT = drawFilters(g, mx, my, listL, listR, contentT) + layout.s(4);
        }

        List<Hit> cards = buildCards(listL, listR, innerT);
        int totalH = cards.isEmpty() ? 0 : (cards.get(cards.size() - 1).y + cards.get(cards.size() - 1).h - innerT + 4);
        int maxScroll = Math.max(0, totalH - (contentB - innerT));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        g.enableScissor(listL, innerT, listR, contentB);
        for (Hit hit : cards) {
            int y = hit.y - (int) scroll;
            if (y + hit.h < innerT || y > contentB) continue;
            boolean hover = over(mx, my, hit.x, y, hit.w, hit.h);
            drawCard(g, hit.x, y, hit.w, hit.h, hit.title, hit.sub, hit.color, hover);
            hits.add(new Hit(hit.x, y, hit.w, hit.h, hit.title, hit.sub, hit.color, hit.run));
        }
        g.disableScissor();

        if (maxScroll > 0) {
            TwitchUi.drawScrollbar(g, listR - 3, innerT, contentB, scroll, maxScroll, contentB - innerT, totalH);
        }
    }

    private int drawFilters(GuiGraphics g, int mx, int my, int listL, int listR, int top) {
        int x = listL;
        int y = top;
        int h = layout.s(16);
        int gap = layout.s(4);
        ChatGameCommand.Category[] cats = {
                null,
                ChatGameCommand.Category.MOB,
                ChatGameCommand.Category.TNT,
                ChatGameCommand.Category.CHAOS,
                ChatGameCommand.Category.INVENTORY,
                ChatGameCommand.Category.MOVEMENT,
                ChatGameCommand.Category.EFFECT,
                ChatGameCommand.Category.WORLD,
                ChatGameCommand.Category.FUN
        };
        for (ChatGameCommand.Category cat : cats) {
            String label = cat == null ? "Все" : catRu(cat);
            int w = Math.max(layout.s(28), font.width(label) + 12);
            if (x + w > listR) {
                x = listL;
                y += h + gap;
            }
            boolean active = cmdFilter == cat;
            boolean hover = over(mx, my, x, y, w, h);
            int bg = active ? TwitchUi.BG_TAB_ACTIVE : (hover ? 0xE8_14_12_1E : TwitchUi.BG_TAB);
            TwitchUi.roundedPanel(g, x, y, x + w, y + h, bg);
            if (active) {
                g.fill(x, y + h - 2, x + w, y + h, tab.color);
            }
            g.drawString(font, label, x + 6, y + 4, active ? TwitchUi.TEXT : TwitchUi.TEXT_DIM, false);
            ChatGameCommand.Category pick = cat;
            filterHits.add(new Hit(x, y, w, h, label, "", tab.color, () -> {
                cmdFilter = pick;
                scroll = 0;
            }));
            x += w + gap;
        }
        return y + h;
    }

    private List<Hit> buildCards(int listL, int listR, int top) {
        int gap = layout.s(6);
        int cols = tab == Tab.COMMANDS ? 3 : 2;
        int cardW = (listR - listL - gap * (cols - 1) - 4) / cols;
        int cardH = layout.s(tab == Tab.COMMANDS ? 22 : 28);
        List<CardSpec> specs = specs();
        List<Hit> out = new ArrayList<>();
        for (int i = 0; i < specs.size(); i++) {
            CardSpec s = specs.get(i);
            int col = i % cols;
            int row = i / cols;
            int x = listL + col * (cardW + gap);
            int y = top + row * (cardH + gap);
            out.add(new Hit(x, y, cardW, cardH, s.title, s.sub, s.color, s.run));
        }
        return out;
    }

    private record CardSpec(String title, String sub, int color, Runnable run) {}

    private List<CardSpec> specs() {
        return switch (tab) {
            case DONATE -> donateSpecs();
            case SHOW -> showSpecs();
            case COMMANDS -> commandSpecs();
            case STREAMER -> streamerSpecs();
        };
    }

    private List<CardSpec> donateSpecs() {
        List<TwitchCraftConfig.DonationTier> tiers = new ArrayList<>(ConfigManager.get().donations.tiers);
        tiers.sort(Comparator.comparingInt(t -> t.minAmount));
        List<CardSpec> out = new ArrayList<>();
        for (var tier : tiers) {
            String label = plain(tier.label);
            if (label.isBlank()) label = tier.command;
            String title = "§6" + tier.minAmount + "₽  §f" + label;
            String sub = tier.wheel ? "§7рулетка" : "§8" + (tier.command == null ? "" : tier.command);
            int amount = tier.minAmount;
            boolean close = true;
            out.add(new CardSpec(title, sub, 0xFF_FF_D7_00, () -> fire("testdonate:" + amount, close, title)));
        }
        if (out.isEmpty()) {
            out.add(new CardSpec("§7Уровней нет", "§8проверь настройки донатов", TwitchUi.TEXT_DIM, () -> {}));
        }
        return out;
    }

    private List<CardSpec> showSpecs() {
        List<CardSpec> out = new ArrayList<>();
        out.add(new CardSpec("§6Кубик мира", "§7реальный бросок 1–6", 0xFF_F1_C4_0F,
                () -> fire("testdice", true, "§6Кубик мира")));
        out.add(new CardSpec("§6Находка мира", "§7груз / тотем / гость рядом", 0xFF_1A_BC_9C,
                () -> fire("testdrop", true, "§6Находка мира")));
        out.add(new CardSpec("§cКолесо хаоса", "§7наказание как при полной полоске", 0xFF_E7_4C_3C,
                () -> fire("testchaos", true, "§cКолесо хаоса")));
        out.add(new CardSpec("§6Рулетка доната", "§7как донат 500₽", 0xFF_FF_D7_00,
                () -> fire("testdonate:500", true, "§6Рулетка 500₽")));
        boolean haus = HouseMode.isActive();
        out.add(new CardSpec(haus ? "§dХаусмод §aВКЛ" : "§7Хаусмод выкл",
                haus ? "§7жми — вернуть КД" : "§7жми — снять все КД",
                0xFF_E9_1E_63,
                () -> fire("testhaus", false, haus ? "§7Хаусмод выкл" : "§dХаусмод ВКЛ")));
        out.add(new CardSpec("§dСвязь", "§7связать вас двоих на 45с", 0xFF_C3_9B_D3,
                () -> fire("testtether", true, "§dСвязь")));
        return out;
    }

    private List<CardSpec> commandSpecs() {
        List<CardSpec> out = new ArrayList<>();
        for (ChatGameCommand cmd : ChatGameCommand.values()) {
            if (cmd.getCategory() == ChatGameCommand.Category.MODERATOR) continue;
            if (cmdFilter != null && cmd.getCategory() != cmdFilter) continue;
            String ru = cmd.getRussianName();
            String title = "§f!" + ru;
            String sub = "§8" + cmd.getCategoryRu();
            String id = cmd.getId();
            out.add(new CardSpec(title, sub, cmd.getCategory().accent(),
                    () -> fire("testcmd:" + id, false, title)));
        }
        return out;
    }

    private List<CardSpec> streamerSpecs() {
        return List.of(
                new CardSpec("§dВызов", "§7−1 заряд, чат в форе", 0xFF_BF_94_FF,
                        () -> fire("challenge", true, "§dВызов")),
                new CardSpec("§eМилость", "§7снять блок урона, тыква", 0xFF_F1_C4_0F,
                        () -> fire("mercy", false, "§eМилость")),
                new CardSpec("§cКонтракт", "§790с не умри", 0xFF_E7_4C_3C,
                        () -> fire("contract", true, "§cКонтракт")),
                new CardSpec("§6Жертва", "§7ценный предмет → заряд", 0xFF_FF_9F_43,
                        () -> fire("sacrifice", false, "§6Жертва")),
                new CardSpec("§cНасмешка", "§7урон без КД 25с", 0xFF_FF_6B_6B,
                        () -> fire("taunt", true, "§cНасмешка")),
                new CardSpec("§eВыкуп", "§7бомба/охота предметом", 0xFF_FF_C4_8A,
                        () -> fire("bargain", false, "§eВыкуп")),
                new CardSpec("§cСброс статистики", "§7смерти · команды · вехи", 0xFF_C0_39_2B,
                        () -> fire("resetstats", false, "§cСтатистика сброшена"))
        );
    }

    private void drawCard(GuiGraphics g, int x, int y, int w, int h, String title, String sub, int color, boolean hover) {
        int bg = hover ? TwitchUi.BG_TAB_ACTIVE : TwitchUi.BG_CARD;
        TwitchUi.roundedPanel(g, x, y, x + w, y + h, bg);
        g.fill(x, y + 2, x + 2, y + h - 2, color);
        if (hover) {
            TwitchUi.fillGradientH(g, x + 2, y, x + w - 1, y + 1, color & 0x88_FFFFFF, 0x00_00_00_00);
        }
        g.drawString(font, title, x + 8, y + (sub.isBlank() ? (h - 8) / 2 : 4), TwitchUi.TEXT, false);
        if (!sub.isBlank() && h >= 24) {
            g.drawString(font, sub, x + 8, y + 14, TwitchUi.TEXT_DIM, false);
        }
    }

    private void drawFooter(GuiGraphics g) {
        int fy = layout.panelB - layout.footerH;
        g.fill(layout.panelL + layout.pad, fy, layout.panelR - layout.pad, fy + 1, TwitchUi.BORDER & 0x66_FFFFFF);
        g.drawString(font, status, layout.panelL + layout.pad, fy + layout.s(8), TwitchUi.TEXT, false);
        String hint = "§8I / Esc — закрыть";
        g.drawString(font, hint, layout.panelR - layout.pad - font.width(TwitchUi.stripFormat(hint)),
                fy + layout.s(8), TwitchUi.TEXT_DIM, false);
    }

    private void switchTab(Tab next) {
        tab = next;
        scroll = 0;
        status = "§7" + next.title;
    }

    private void fire(String action, boolean close, String label) {
        StreamerActionClient.send(action);
        status = "§a▶ " + label;
        if (close && minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mx = event.x();
        double my = event.y();
        if (closeHover) {
            onClose();
            return true;
        }
        for (Hit hit : tabHits) {
            if (over(mx, my, hit.x, hit.y, hit.w, hit.h)) {
                hit.run.run();
                return true;
            }
        }
        for (Hit hit : filterHits) {
            if (over(mx, my, hit.x, hit.y, hit.w, hit.h)) {
                hit.run.run();
                return true;
            }
        }
        for (Hit hit : hits) {
            if (over(mx, my, hit.x, hit.y, hit.w, hit.h)) {
                hit.run.run();
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll -= v * 22;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_I || event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        int idx = event.key() - GLFW.GLFW_KEY_1;
        if (idx >= 0 && idx < Tab.values().length) {
            switchTab(Tab.values()[idx]);
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

    private static String plain(String label) {
        if (label == null || label.isBlank()) return "";
        String s = label.strip();
        if (!s.isEmpty()) {
            int cp = s.codePointAt(0);
            if (!Character.isLetterOrDigit(cp)) {
                s = s.substring(Character.charCount(cp)).strip();
            }
        }
        return s;
    }

    private static String catRu(ChatGameCommand.Category cat) {
        return switch (cat) {
            case MOB -> "Мобы";
            case TNT -> "TNT";
            case CHAOS -> "Хаос";
            case INVENTORY -> "Инвентарь";
            case MOVEMENT -> "Движение";
            case EFFECT -> "Эффекты";
            case WORLD -> "Мир";
            case FUN -> "Шоу";
            case MODERATOR -> "Модер";
        };
    }
}
