package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.twitchcraft.config.HudLegacySync;
import net.twitchcraft.config.TwitchCraftConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** Единый конструктор HUD — позиция, цвет, секции. */
public class HudCustomizerScreen extends Screen {
    private static final int BASE_FOOTER = 30;
    private static final String[] ELEMENT_LABELS = {
            "Сайдбар", "Лента", "Смерть", "Координаты", "Донат", "Комбо", "Рулетка",
            "Вспышки команд", "Вспышки донатов", "Дуо"
    };
    private static final String[] ELEMENT_HINTS = {
            "Список команд и статистика — по умолчанию слева сверху",
            "Плашки «зритель → команда» (позиция в настройках)",
            "Всплывающий алерт при смерти стримера",
            "Закреплённые координаты последней смерти",
            "Уведомление о донате на экране",
            "Комбо-цепочка одинаковых команд чата",
            "Колесо рулетки за крупный донат",
            "Цветные вспышки экрана от !команд",
            "Цветные вспышки экрана от донатов",
            "Плашка смертей дуо — HP и голод напарника сверху, счёт снизу"
    };

    private record ToggleHit(int x, int y, int w, int h, String label, Supplier<Boolean> get,
                             java.util.function.Consumer<Boolean> set) {}
    private record IntHit(int x, int y, int w, int h, String label, int min, int max,
                          Supplier<Integer> get, IntConsumer set) {}
    private record PresetHit(int x, int y, int w, int h, String label, Runnable action) {}
    private record TabHit(int x, int y, int w, int h, int index) {}
    private record HeaderHit(int x, int y, int w, int h, String label) {}

    private final Screen parent;
    private final TwitchCraftConfig config;
    private TwitchMenuLayout layout;
    private int selectedElement;
    private double contentScroll;
    private int scrollAreaTop;
    private int contentHeight;
    private boolean closeHover;
    private boolean backHover;

    private final List<TabHit> tabHits = new ArrayList<>();
    private final List<ToggleHit> toggles = new ArrayList<>();
    private final List<IntHit> intControls = new ArrayList<>();
    private final List<PresetHit> presetHits = new ArrayList<>();
    private final List<HeaderHit> headers = new ArrayList<>();

    public HudCustomizerScreen(Screen parent, TwitchCraftConfig config) {
        this(parent, config, 0);
    }

    public HudCustomizerScreen(Screen parent, TwitchCraftConfig config, int initialElement) {
        super(Component.literal("HUD"));
        this.parent = parent;
        this.config = config;
        this.selectedElement = Mth.clamp(initialElement, 0, ELEMENT_LABELS.length - 1);
        HudClientStore.applyTo(config);
        HudLegacySync.ensure(config);
    }

    @Override
    protected void init() {
        layout = TwitchMenuLayout.compute(width, height, BASE_FOOTER);
        rebuildContent();
    }

    private void rebuildContent() {
        toggles.clear();
        intControls.clear();
        presetHits.clear();
        tabHits.clear();
        headers.clear();
        contentScroll = 0;

        int tabW = (layout.contentR - layout.contentL - layout.s(16)) / 3;
        int tabH = layout.s(24);
        int tabY = layout.contentT + layout.s(2);
        int tabRows = (ELEMENT_LABELS.length + 2) / 3;
        for (int i = 0; i < ELEMENT_LABELS.length; i++) {
            int col = i % 3;
            int rowIdx = i / 3;
            tabHits.add(new TabHit(
                    layout.contentL + col * (tabW + layout.s(4)),
                    tabY + rowIdx * (tabH + layout.s(4)),
                    tabW, tabH, i));
        }
        scrollAreaTop = tabY + tabRows * (tabH + layout.s(4)) + layout.s(10);

        int y = scrollAreaTop;
        int row = layout.s(28);
        var el = currentElement();

        addHeader(y, "§f⚙ Общие настройки HUD");
        y += layout.s(20);
        addIntRow(y, "Масштаб всего HUD (%)", 50, 150,
                () -> Math.round(config.hud.globalScale <= 0 ? 100 : config.hud.globalScale * 100),
                v -> config.hud.globalScale = v / 100f);
        y += row;

        addHeader(y, "§f📍 " + ELEMENT_LABELS[selectedElement] + " §8— " + ELEMENT_HINTS[selectedElement]);
        y += layout.s(20);

        addToggle(y, "§aПоказывать на экране", () -> el.enabled, v -> el.enabled = v);
        y += row;
        addPreset(y, "§eПозиция: " + HudColors.anchorLabel(el.anchor), () -> {
            el.anchor = HudColors.cycleAnchor(el.anchor);
            rebuildContent();
        });
        y += row;
        addIntRow(y, "Отступ X (px)", -200, 200, () -> el.offsetX, v -> el.offsetX = v);
        y += row;
        addIntRow(y, "Отступ Y (px)", -200, 200, () -> el.offsetY, v -> el.offsetY = v);
        y += row;
        addIntRow(y, "Масштаб элемента (%)", 50, 200, () -> Math.round(el.scale * 100), v -> el.scale = v / 100f);
        y += row;
        addIntRow(y, "Прозрачность (%)", 20, 100, () -> Math.round(el.opacity * 100), v -> el.opacity = v / 100f);
        y += row;

        if (selectedElement != 7 && selectedElement != 8) {
            addPreset(y, "§dЦвет: " + HudColors.label(el.accentColor), () -> {
                el.accentColor = HudColors.cycle(el.accentColor);
                rebuildContent();
            });
            y += row;
        }

        switch (selectedElement) {
            case 0 -> y = buildSidebarSections(y, row);
            case 1 -> y = buildFeedSections(y, row);
            case 2 -> y = buildDeathSections(y, row);
            case 3 -> y = buildPinnedCoords(y, row);
            case 7 -> y = buildCommandFx(y, row);
            default -> y += layout.s(8);
        }

        contentHeight = y - scrollAreaTop + layout.s(24);
    }

    private static final String[] FEATURED_COMMANDS = {
            "lightning", "heal", "diamond", "tnt", "creeper", "drop", "launch", "save"
    };

    private int buildSidebarSections(int y, int row) {
        var s = config.hud.sidebarContent;
        addHeader(y, "§f📋 Что показывать в сайдбаре");
        y += layout.s(20);
        addToggle(y, "Заголовок TwitchCraft", () -> s.title, v -> s.title = v); y += row;
        addToggle(y, "Последняя команда", () -> s.lastCommand, v -> s.lastCommand = v); y += row;
        addToggle(y, "§6★ Любимая команда", () -> s.featuredCommand, v -> s.featuredCommand = v); y += row;
        addPreset(y, "§6ID команды: §f" + (s.featuredCommandId != null ? s.featuredCommandId : "lightning"), () -> {
            String cur = s.featuredCommandId != null ? s.featuredCommandId : "lightning";
            int idx = 0;
            for (int i = 0; i < FEATURED_COMMANDS.length; i++) {
                if (FEATURED_COMMANDS[i].equalsIgnoreCase(cur)) { idx = i; break; }
            }
            s.featuredCommandId = FEATURED_COMMANDS[(idx + 1) % FEATURED_COMMANDS.length];
            rebuildContent();
        }); y += row;
        addToggle(y, "Статистика добро / зло", () -> s.stats, v -> s.stats = v); y += row;
        addToggle(y, "§a♥ Топ добрых зрителей", () -> s.showTopViewers, v -> s.showTopViewers = v); y += row;
        addToggle(y, "§c🔥 Серии команд", () -> s.showStreaks, v -> s.showStreaks = v); y += row;
        addToggle(y, "Полоса хаоса", () -> s.chaos, v -> s.chaos = v); y += row;
        addToggle(y, "Штраф КД", () -> s.penalty, v -> s.penalty = v); y += row;
        addToggle(y, "Смерти / блок зла", () -> s.deaths, v -> { s.deaths = v; s.evilBlock = v; }); y += row;
        addToggle(y, "Координаты смерти", () -> s.deathCoords, v -> s.deathCoords = v); y += row;
        addToggle(y, "Донаты", () -> s.donations, v -> s.donations = v); y += row;
        addToggle(y, "Список команд", () -> s.commands, v -> s.commands = v); y += row;
        addToggle(y, "Статус / кладбище", () -> s.statusLine, v -> s.statusLine = v); y += row;
        addToggle(y, "Компактный режим", () -> s.compact, v -> s.compact = v); y += row;
        addIntRow(y, "Смена страницы (сек)", 3, 30, () -> s.pageRotateSeconds, v -> s.pageRotateSeconds = v); y += row;
        addIntRow(y, "Строк команд", 3, 12, () -> s.maxCommandLines, v -> s.maxCommandLines = v); y += row;
        return y;
    }

    private int buildDeathSections(int y, int row) {
        if (config.death == null) config.death = new TwitchCraftConfig.DeathSettings();
        var d = config.death;
        addHeader(y, "§f☠ Отображение смертей");
        y += layout.s(20);
        addToggle(y, "Счётчик смертей", () -> d.showDeathCounter, v -> d.showDeathCounter = v); y += row;
        addToggle(y, "Координаты в сайдбаре", () -> d.showCoordinates, v -> d.showCoordinates = v); y += row;
        addToggle(y, "Измерение (Ад/Край)", () -> d.showDimension, v -> d.showDimension = v); y += row;
        addToggle(y, "Всплывающий алерт", () -> d.showDeathToast, v -> d.showDeathToast = v); y += row;
        addPreset(y, "§eКуда выводить: " + deathDisplayLabel(d.coordsDisplay), () -> {
            d.coordsDisplay = cycleDeathDisplay(d.coordsDisplay);
            rebuildContent();
        }); y += row;
        return y;
    }

    private static String deathDisplayLabel(String mode) {
        if (mode == null) return "Оверлей";
        return switch (mode.toLowerCase()) {
            case "actionbar" -> "Над хотбаром";
            case "title" -> "Заголовок";
            case "sidebar" -> "Сайдбар";
            case "all" -> "Везде";
            default -> "Оверлей";
        };
    }

    private static String cycleDeathDisplay(String mode) {
        String[] modes = {"overlay", "actionbar", "title", "sidebar", "all"};
        if (mode == null) return modes[1];
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equalsIgnoreCase(mode)) return modes[(i + 1) % modes.length];
        }
        return modes[0];
    }

    private int buildFeedSections(int y, int row) {
        var f = config.hud.commandFeedContent;
        addHeader(y, "§f💬 Лента команд");
        y += layout.s(20);
        addToggle(y, "Аватары зрителей", () -> f.avatars, v -> f.avatars = v); y += row;
        addToggle(y, "Бейджи категорий", () -> f.badges, v -> f.badges = v); y += row;
        addToggle(y, "§c🔥 Серии на карточке", () -> f.showStreaks, v -> f.showStreaks = v); y += row;
        addToggle(y, "Прогресс-бар", () -> f.progressBar, v -> f.progressBar = v); y += row;
        addToggle(y, "Вспышка экрана", () -> f.screenFx, v -> f.screenFx = v); y += row;
        addIntRow(y, "Макс. карточек", 1, 4, () -> f.maxEntries, v -> f.maxEntries = v); y += row;
        addPreset(y, "§aЦвет добра: " + HudColors.label(config.hud.commandFeed.goodColor), () -> {
            config.hud.commandFeed.goodColor = HudColors.cycle(config.hud.commandFeed.goodColor);
            rebuildContent();
        }); y += row;
        addPreset(y, "§cЦвет зла: " + HudColors.label(config.hud.commandFeed.evilColor), () -> {
            config.hud.commandFeed.evilColor = HudColors.cycle(config.hud.commandFeed.evilColor);
            rebuildContent();
        }); y += row;
        return y;
    }

    private int buildPinnedCoords(int y, int row) {
        if (config.death == null) config.death = new TwitchCraftConfig.DeathSettings();
        addHeader(y, "§f📌 Закрепление координат");
        y += layout.s(20);
        addToggle(y, "§e📌 Закрепить на экране", () -> config.death.pinCoordinates,
                v -> config.death.pinCoordinates = v); y += row;
        addToggle(y, "Показывать координаты", () -> config.death.showCoordinates,
                v -> config.death.showCoordinates = v); y += row;
        addToggle(y, "§6➜ Стрелка до трупа", () -> config.death.showDeathArrow,
                v -> config.death.showDeathArrow = v); y += row;
        return y;
    }

    private int buildCommandFx(int y, int row) {
        var fx = config.hud.commandFx;
        addHeader(y, "§f✨ Цвета вспышек");
        y += layout.s(20);
        addPreset(y, "§cЦвет зла: " + HudColors.label(fx.evilColor), () -> {
            fx.evilColor = HudColors.cycle(fx.evilColor);
            rebuildContent();
        }); y += row;
        addPreset(y, "§aЦвет добра: " + HudColors.label(fx.goodColor), () -> {
            fx.goodColor = HudColors.cycle(fx.goodColor);
            rebuildContent();
        }); y += row;
        return y;
    }

    private TwitchCraftConfig.HudElementConfig currentElement() {
        return switch (selectedElement) {
            case 0 -> config.hud.sidebar;
            case 1 -> config.hud.commandFeed;
            case 2 -> config.hud.deathToast;
            case 3 -> config.hud.pinnedDeathCoords;
            case 4 -> config.hud.donationToast;
            case 5 -> config.hud.comboChain;
            case 6 -> config.hud.donationWheel;
            case 7 -> config.hud.commandFx;
            case 8 -> config.hud.donationFx;
            default -> config.hud.duoScore != null ? config.hud.duoScore : net.twitchcraft.config.HudDefaults.duoScore();
        };
    }

    private void addHeader(int y, String label) {
        headers.add(new HeaderHit(layout.contentL, y, layout.contentR - layout.contentL, layout.s(16), label));
    }

    private void addToggle(int y, String label, Supplier<Boolean> get, java.util.function.Consumer<Boolean> set) {
        toggles.add(new ToggleHit(layout.contentL, y, layout.contentR - layout.contentL, layout.s(24), label, get, set));
    }

    private void addIntRow(int y, String label, int min, int max, Supplier<Integer> get, IntConsumer set) {
        intControls.add(new IntHit(layout.contentL, y, layout.contentR - layout.contentL, layout.s(24), label, min, max, get, set));
    }

    private void addPreset(int y, String label, Runnable action) {
        presetHits.add(new PresetHit(layout.contentL, y, layout.contentR - layout.contentL, layout.s(22), label, action));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (layout == null) layout = TwitchMenuLayout.compute(width, height, BASE_FOOTER);
        TwitchUi.drawDimBackground(g, width, height);
        TwitchUi.drawSettingsFrame(g, layout.panelL, layout.panelT, layout.panelR, layout.panelB,
                layout.s(0), TwitchUi.PURPLE);

        g.drawString(font, TwitchUi.TITLE_FORMAT + " §8· §7HUD", layout.panelL + layout.pad,
                layout.panelT + layout.pad + layout.s(4), TwitchUi.TEXT, false);
        g.drawString(font, "§7" + ELEMENT_HINTS[selectedElement],
                layout.contentL, layout.panelT + layout.pad + layout.s(26), TwitchUi.TEXT_DIM, false);
        TwitchUi.drawTabAccentLine(g, layout.contentL, layout.panelT + layout.pad + layout.s(38),
                layout.contentR - layout.contentL, TwitchUi.PURPLE);

        // Фиксированные вкладки — не скроллятся
        g.enableScissor(layout.contentL, layout.contentT, layout.contentR, scrollAreaTop);
        for (TabHit tab : tabHits) {
            boolean active = tab.index == selectedElement;
            boolean hover = over(mouseX, mouseY, tab.x, tab.y, tab.w, tab.h);
            int bg = active ? TwitchUi.BG_TAB_ACTIVE : (hover ? 0xE8_14_12_1E : TwitchUi.BG_TAB);
            TwitchUi.roundedPanel(g, tab.x, tab.y, tab.x + tab.w, tab.y + tab.h, bg);
            if (active) {
                TwitchUi.fillGradientH(g, tab.x + 2, tab.y + tab.h - 2, tab.x + tab.w - 2, tab.y + tab.h,
                        TwitchUi.PURPLE, TwitchUi.ACCENT_ALT);
            }
            String label = ELEMENT_LABELS[tab.index];
            int tw = font.width(label);
            g.drawString(font, label, tab.x + (tab.w - tw) / 2, tab.y + (tab.h - 8) / 2,
                    active ? TwitchUi.PURPLE_LIGHT : TwitchUi.TEXT_DIM, false);
        }
        g.disableScissor();

        TwitchUi.fillGradientH(g, layout.contentL, scrollAreaTop - 1, layout.contentR, scrollAreaTop,
                TwitchUi.PURPLE & 0x55_FFFFFF, TwitchUi.ACCENT_ALT & 0x22_FFFFFF);

        int scroll = (int) contentScroll;
        int maxScroll = Math.max(0, contentHeight - (layout.contentB - scrollAreaTop));
        contentScroll = Mth.clamp(contentScroll, 0, maxScroll);

        g.enableScissor(layout.contentL, scrollAreaTop, layout.contentR, layout.contentB);

        for (HeaderHit h : headers) {
            int dy = h.y - scroll;
            if (dy + h.h < scrollAreaTop || dy > layout.contentB) continue;
            TwitchUi.drawCategoryHeader(g, font, h.x, dy, h.w, h.label, TwitchUi.PURPLE_LIGHT);
        }
        for (ToggleHit t : toggles) {
            int dy = t.y - scroll;
            if (dy + t.h < scrollAreaTop || dy > layout.contentB) continue;
            TwitchUi.drawToggleRow(g, font, t.x, dy, t.w, t.label, t.get.get(),
                    over(mouseX, mouseY, t.x, dy, t.w, t.h));
        }
        for (PresetHit p : presetHits) {
            int dy = p.y - scroll;
            if (dy + p.h < scrollAreaTop || dy > layout.contentB) continue;
            TwitchUi.drawSmallButton(g, font, p.x, dy, p.w, p.h, p.label,
                    over(mouseX, mouseY, p.x, dy, p.w, p.h));
        }
        for (IntHit ic : intControls) {
            int dy = ic.y - scroll;
            if (dy + ic.h < scrollAreaTop || dy > layout.contentB) continue;
            TwitchUi.drawIntRow(g, font, ic.x, dy, ic.w, ic.label, ic.get.get());
            int btnW = layout.s(18);
            int minusX = ic.x + ic.w - layout.s(44);
            int plusX = ic.x + ic.w - layout.s(22);
            int btnY = dy + layout.s(3);
            TwitchUi.drawSmallButton(g, font, minusX, btnY, btnW, layout.s(18), "−",
                    over(mouseX, mouseY, minusX, btnY, btnW, layout.s(18)));
            TwitchUi.drawSmallButton(g, font, plusX, btnY, btnW, layout.s(18), "+",
                    over(mouseX, mouseY, plusX, btnY, btnW, layout.s(18)));
        }

        if (maxScroll > 0) {
            TwitchUi.drawScrollbar(g, layout.contentR, scrollAreaTop, layout.contentB,
                    contentScroll, maxScroll, layout.contentB - scrollAreaTop, contentHeight);
        }
        g.disableScissor();

        int fy = layout.panelB - layout.footerH;
        backHover = over(mouseX, mouseY, layout.panelL + layout.pad, fy + layout.s(4), layout.s(80), layout.s(18));
        TwitchUi.drawSmallButton(g, font, layout.panelL + layout.pad, fy + layout.s(4), layout.s(80), layout.s(18),
                "§7← Назад", backHover);
        closeHover = over(mouseX, mouseY, layout.closeX, layout.closeY, layout.closeSize, layout.closeSize);
        TwitchUi.drawCloseButton(g, font, layout.closeX, layout.closeY, layout.closeSize, closeHover);
        g.drawString(font, "§8Колёсико §7— прокрутка · §8Shift §7— шаг ±10",
                layout.panelL + layout.pad + layout.s(90), fy + layout.s(8), TwitchUi.TEXT_DIM, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x();
        double my = event.y();
        if (closeHover || backHover) {
            applyAndClose();
            return true;
        }
        for (TabHit tab : tabHits) {
            if (over(mx, my, tab.x, tab.y, tab.w, tab.h)) {
                selectedElement = tab.index;
                rebuildContent();
                return true;
            }
        }
        if (my < scrollAreaTop) return true;

        int scroll = (int) contentScroll;
        for (ToggleHit t : toggles) {
            int dy = t.y - scroll;
            if (over(mx, my, t.x, dy, t.w, t.h)) {
                t.set.accept(!t.get.get());
                persistHud();
                return true;
            }
        }
        for (PresetHit p : presetHits) {
            int dy = p.y - scroll;
            if (over(mx, my, p.x, dy, p.w, p.h)) {
                p.action.run();
                persistHud();
                return true;
            }
        }
        int btnW = layout.s(18);
        for (IntHit ic : intControls) {
            int dy = ic.y - scroll;
            int minusX = ic.x + ic.w - layout.s(44);
            int plusX = ic.x + ic.w - layout.s(22);
            int btnY = dy + layout.s(3);
            int step = TwitchUi.intAdjustStep();
            if (over(mx, my, minusX, btnY, btnW, layout.s(18))) {
                ic.set.accept(TwitchUi.adjustInt(ic.get.get(), -step, ic.min, ic.max));
                persistHud();
                return true;
            }
            if (over(mx, my, plusX, btnY, btnW, layout.s(18))) {
                ic.set.accept(TwitchUi.adjustInt(ic.get.get(), step, ic.min, ic.max));
                persistHud();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY >= scrollAreaTop && mouseX >= layout.contentL && mouseX <= layout.contentR
                && mouseY <= layout.contentB) {
            int max = Math.max(0, contentHeight - (layout.contentB - scrollAreaTop));
            contentScroll = Mth.clamp(contentScroll - scrollY * layout.s(16), 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void persistHud() {
        HudLegacySync.pushToLegacy(config);
        HudClientStore.replaceAndSave(config.hud);
        HudResolver.onConfigSaved();
    }

    private void applyAndClose() {
        persistHud();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void onClose() {
        applyAndClose();
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
