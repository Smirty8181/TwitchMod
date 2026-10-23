package net.twitchcraft.client;

import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.twitchcraft.commands.CustomCommandRegistry;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.CustomCommandDefinition;
import net.twitchcraft.config.LocalProfileStore;
import net.twitchcraft.config.StreamerProfile;
import net.twitchcraft.config.TwitchCraftConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Конструктор пользовательских команд чата (клавиша U). */
public class CustomCommandCreatorScreen extends Screen {
    private static final int BASE_FOOTER = 30;
    private static final Gson GSON = new Gson();

    private enum Step { LIST, NAME, TYPE, PICK, SETTINGS }

    private record BtnHit(int x, int y, int w, int h, String label, Runnable action) {}
    private record RowHit(int x, int y, int w, int h, int index) {}

    private final TwitchCraftConfig config;
    private final StreamerProfile profile;
    private TwitchMenuLayout layout;

    private Step step = Step.LIST;
    private CustomCommandDefinition draft;
    private int editingIndex = -1;
    private String statusMessage = "";
    private int statusColor = TwitchUi.TEXT_DIM;

    private EditBox nameBox;
    private EditBox searchBox;
    private double listScroll;
    private double pickScroll;
    private String searchQuery = "";

    private final List<BtnHit> buttons = new ArrayList<>();
    private final List<RowHit> rows = new ArrayList<>();
    private boolean closeHover;
    private boolean saveHover;
    private boolean zoomOutHover;
    private boolean zoomInHover;
    private final Screen returnTo;

    public CustomCommandCreatorScreen() {
        this(null);
    }

    public CustomCommandCreatorScreen(Screen returnTo) {
        super(Component.empty());
        this.returnTo = returnTo;
        this.config = copy(ConfigManager.get());
        this.profile = LocalProfileStore.load();
        UiTheme.apply(config);
    }

    @Override
    protected void init() {
        layout = TwitchMenuLayout.compute(width, height, BASE_FOOTER);
        rebuildFields();
    }

    private void rebuildFields() {
        clearWidgets();
        buttons.clear();
        if (layout == null) layout = TwitchMenuLayout.compute(width, height, BASE_FOOTER);

        int fieldW = layout.contentR - layout.contentL - layout.s(8);
        int fieldH = layout.s(18);
        int fieldY = layout.contentT + layout.s(52);

        if (step == Step.NAME) {
            nameBox = new EditBox(font, layout.contentL, fieldY, fieldW, fieldH,
                    Component.literal("!команда"));
            nameBox.setMaxLength(20);
            nameBox.setValue(draft != null && draft.trigger != null ? draft.trigger : "");
            nameBox.setHint(Component.literal("напр. суперхил"));
            addRenderableWidget(nameBox);
            setFocused(nameBox);
        } else if (step == Step.PICK) {
            searchBox = new EditBox(font, layout.contentL, layout.contentT + layout.s(36),
                    fieldW, fieldH, Component.literal("Поиск"));
            searchBox.setMaxLength(48);
            searchBox.setValue(searchQuery);
            searchBox.setResponder(q -> {
                searchQuery = q;
                pickScroll = 0;
            });
            addRenderableWidget(searchBox);
            setFocused(searchBox);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (layout == null) layout = TwitchMenuLayout.compute(width, height, BASE_FOOTER);

        TwitchUi.drawDimBackground(g, width, height);
        TwitchUi.glassPanel(g, layout.panelL, layout.panelT, layout.panelR, layout.panelB);
        TwitchUi.accentBar(g, layout.panelL + 1, layout.panelT + 1, layout.panelR - 1, TwitchUi.PURPLE);

        drawHeader(g);
        drawStepContent(g, mouseX, mouseY);
        drawFooter(g, mouseX, mouseY);

        closeHover = over(mouseX, mouseY, layout.closeX, layout.closeY, layout.closeSize, layout.closeSize);
        TwitchUi.drawCloseButton(g, font, layout.closeX, layout.closeY, layout.closeSize, closeHover);

        for (BtnHit btn : buttons) {
            if (btn.label.isEmpty()) continue;
            boolean hover = over(mouseX, mouseY, btn.x, btn.y, btn.w, btn.h);
            if ("save".equals(btn.label)) {
                TwitchUi.drawSaveButton(g, font, btn.x, btn.y, btn.w, btn.h, hover);
                g.drawString(font, "§f§lСохранить", btn.x + layout.s(10), btn.y + layout.s(6), TwitchUi.TEXT, false);
            } else if ("create".equals(btn.label)) {
                TwitchUi.drawSaveButton(g, font, btn.x, btn.y, btn.w, btn.h, hover);
                g.drawString(font, "§f§l+ Создать", btn.x + layout.s(10), btn.y + layout.s(6), TwitchUi.TEXT, false);
            } else {
                TwitchUi.drawSmallButton(g, font, btn.x, btn.y, btn.w, btn.h, btn.label, hover);
            }
        }
    }

    private void drawHeader(GuiGraphics g) {
        int y = layout.panelT + layout.pad + layout.s(4);
        int logoSize = layout.s(16);
        SmirtyLogo.drawInline(g, layout.panelL + layout.pad + 2, y + 1, logoSize, TwitchUi.PURPLE);
        g.drawString(font, "§b§lКонструктор §f§lкоманд", layout.panelL + layout.pad + logoSize + layout.s(6), y, TwitchUi.TEXT, false);
        g.drawString(font, "§7Создавай свои !команды для чата", layout.panelL + layout.pad + logoSize + layout.s(6),
                y + layout.s(11), TwitchUi.TEXT_DIM, false);

        String stepTitle = switch (step) {
            case LIST -> "§f§lМои команды";
            case NAME -> "§f§lШаг 1 · Имя команды";
            case TYPE -> "§f§lШаг 2 · Тип действия";
            case PICK -> "§f§lШаг 3 · " + pickTitle();
            case SETTINGS -> "§f§lШаг 4 · Настройки";
        };
        g.drawString(font, stepTitle, layout.contentL, layout.panelT + layout.pad + layout.s(28), TwitchUi.ACCENT_ALT, false);
        g.fill(layout.contentL, layout.panelT + layout.headerH - layout.s(4), layout.contentR,
                layout.panelT + layout.headerH - layout.s(3), 0x66_55_CC_FF);

        if (!statusMessage.isBlank()) {
            g.drawString(font, statusMessage, layout.contentL, layout.contentB - layout.s(14), statusColor, false);
        }
    }

    private String pickTitle() {
        if (draft == null) return "Выбор";
        return switch (draft.actionType) {
            case "item" -> "Предмет";
            case "mob" -> "Моб";
            default -> "Эффект";
        };
    }

    private void drawStepContent(GuiGraphics g, int mouseX, int mouseY) {
        buttons.clear();
        rows.clear();
        switch (step) {
            case LIST -> drawList(g, mouseX, mouseY);
            case NAME -> drawNameStep(g);
            case TYPE -> drawTypeStep(g, mouseX, mouseY);
            case PICK -> drawPickStep(g, mouseX, mouseY);
            case SETTINGS -> drawSettingsStep(g, mouseX, mouseY);
        }
    }

    private void drawList(GuiGraphics g, int mouseX, int mouseY) {
        int x = layout.contentL;
        int y = layout.contentT;
        int w = layout.contentR - layout.contentL;
        int h = layout.contentB - layout.contentT - layout.s(20);

        TwitchUi.drawHint(g, font, x, y, "§7Зрители пишут §e!имя §7в Twitch-чате. Нажми §f+ Создать §7или выбери команду для редактирования.");
        y += layout.s(18);

        List<CustomCommandDefinition> cmds = config.customCommands;
        int rowH = layout.s(34);
        int gap = layout.s(4);
        int totalH = cmds.isEmpty() ? rowH : cmds.size() * (rowH + gap);
        int maxScroll = Math.max(0, totalH - (h - layout.s(30)));
        listScroll = Mth.clamp(listScroll, 0, maxScroll);

        g.enableScissor(x, y, layout.contentR, layout.contentB - layout.s(24));
        int ry = y - (int) listScroll;

        if (cmds.isEmpty()) {
            TwitchUi.roundedPanel(g, x, ry, x + w, ry + rowH, TwitchUi.BG_CARD);
            g.drawString(font, "§8Пока нет своих команд — нажми «+ Создать»", x + layout.s(10), ry + layout.s(12),
                    TwitchUi.TEXT_DIM, false);
            ry += rowH + gap;
        } else {
            for (int i = 0; i < cmds.size(); i++) {
                CustomCommandDefinition def = cmds.get(i);
                boolean hover = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + rowH;
                drawCommandRow(g, x, ry, w, rowH, def, hover);
                rows.add(new RowHit(x, ry, w, rowH, i));
                ry += rowH + gap;
            }
        }
        g.disableScissor();

        if (maxScroll > 0) {
            TwitchUi.drawScrollbar(g, layout.contentR - 4, y, layout.contentB - layout.s(24),
                    listScroll, maxScroll, h - layout.s(30), totalH);
        }

        int btnW = layout.s(90);
        int btnH = layout.s(20);
        int btnX = layout.contentR - btnW;
        int btnY = layout.contentB - layout.s(22);
        buttons.add(new BtnHit(btnX, btnY, btnW, btnH, "create", this::startCreate));
    }

    private void drawCommandRow(GuiGraphics g, int x, int y, int w, int h,
                                CustomCommandDefinition def, boolean hover) {
        int accent = def.positive ? 0xFF_55_CC_88 : typeAccent(def.actionType);
        TwitchUi.roundedPanel(g, x, y, x + w, y + h, hover ? 0xF0_28_28_32 : TwitchUi.BG_CARD);
        g.fill(x, y + 2, x + 3, y + h - 2, accent);
        String prefix = def.enabled ? "§e!" : "§8!";
        g.drawString(font, prefix + def.displayName(), x + layout.s(10), y + layout.s(6), TwitchUi.GOLD, false);
        String desc = describe(def);
        g.drawString(font, "§7" + desc, x + layout.s(10), y + layout.s(18), TwitchUi.TEXT_DIM, false);
        String cd = def.cooldownSeconds + "с";
        int cdW = font.width(cd) + layout.s(8);
        int cdX = x + w - cdW - layout.s(8);
        TwitchUi.roundedPanel(g, cdX, y + layout.s(8), cdX + cdW, y + layout.s(20), 0xFF_2A_2A_32);
        g.drawString(font, cd, cdX + layout.s(4), y + layout.s(10), TwitchUi.TEXT_DIM, false);

        int del = layout.s(16);
        int delX = x + w - cdW - layout.s(28);
        boolean delHover = false;
        TwitchUi.drawSmallButton(g, font, delX, y + layout.s(8), del, del, "§c✕", delHover);
    }

    private static String describe(CustomCommandDefinition def) {
        return switch (def.actionType == null ? "effect" : def.actionType) {
            case "item" -> "Предмет · " + shortId(def.itemId) + " ×" + def.itemCount;
            case "mob" -> "Моб · " + shortId(def.entityId) + " ×" + def.mobCount;
            default -> "Эффект · " + shortId(def.effectId) + " · " + def.durationSeconds + "с · ур." + (def.amplifier + 1);
        };
    }

    private static String shortId(String id) {
        if (id == null) return "?";
        int slash = id.indexOf(':');
        return slash >= 0 ? id.substring(slash + 1) : id;
    }

    private static int typeAccent(String type) {
        return switch (type == null ? "effect" : type) {
            case "item" -> 0xFF_FF_AA_44;
            case "mob" -> 0xFF_FF_55_66;
            default -> 0xFF_91_46_FF;
        };
    }

    private void drawNameStep(GuiGraphics g) {
        TwitchUi.drawLabel(g, font, layout.contentL, layout.contentT + layout.s(18), "Имя в чате (без «!»):");
        if (nameBox != null) {
            nameBox.render(g, 0, 0, 0);
        }
        TwitchUi.drawHint(g, font, layout.contentL, layout.contentT + layout.s(78),
                "§8Латиница, кириллица, цифры. Мин. 2 символа. Нельзя дублировать встроенные команды.");

        int btnW = layout.s(72);
        int btnY = layout.contentB - layout.s(24);
        buttons.add(new BtnHit(layout.contentL, btnY, btnW, layout.s(20), "§7Назад", () -> goTo(Step.LIST)));
        buttons.add(new BtnHit(layout.contentL + btnW + layout.s(8), btnY, btnW, layout.s(20), "§aДалее", this::confirmName));
    }

    private void drawTypeStep(GuiGraphics g, int mouseX, int mouseY) {
        int x = layout.contentL;
        int y = layout.contentT + layout.s(8);
        int w = layout.contentR - layout.contentL;
        int cardH = layout.s(52);
        int gap = layout.s(8);

        TwitchUi.drawHint(g, font, x, y - layout.s(4), "§7Выбери тип команды:");
        drawTypeCard(g, x, y, w, cardH, "[1]", "ЭФФЕКТ", "Наложить зелье на стримера", "effect", mouseX, mouseY);
        y += cardH + gap;
        drawTypeCard(g, x, y, w, cardH, "[2]", "ПРЕДМЕТ", "Выдать предмет в инвентарь", "item", mouseX, mouseY);
        y += cardH + gap;
        drawTypeCard(g, x, y, w, cardH, "[3]", "МОБ", "Заспавнить моба рядом", "mob", mouseX, mouseY);

        int btnW = layout.s(72);
        int btnY = layout.contentB - layout.s(24);
        buttons.add(new BtnHit(layout.contentL, btnY, btnW, layout.s(20), "§7Назад", () -> goTo(Step.NAME)));
    }

    private void drawTypeCard(GuiGraphics g, int x, int y, int w, int h,
                              String badge, String title, String desc, String type, int mx, int my) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        int accent = typeAccent(type);
        int bg = hover ? 0xF0_2A_2A_38 : TwitchUi.BG_CARD;
        TwitchUi.roundedPanel(g, x, y, x + w, y + h, bg);
        g.fill(x, y + 2, x + 4, y + h - 2, accent);
        TwitchUi.fillGradientH(g, x, y, x + w, y + 3, accent, accent & 0x33_FFFFFF);

        g.drawString(font, badge, x + layout.s(10), y + layout.s(8), accent, false);
        g.drawString(font, "§f§l" + title, x + layout.s(36), y + layout.s(6), TwitchUi.TEXT, false);
        g.drawString(font, "§7" + desc, x + layout.s(36), y + layout.s(20), TwitchUi.TEXT_DIM, false);

        String action = hover ? "§a> НАЖМИ" : "§8нажми";
        int actionW = font.width(TwitchUi.stripFormat(action));
        g.drawString(font, action, x + w - actionW - layout.s(10), y + (h - 8) / 2, hover ? 0xFF_66_FF_99 : TwitchUi.TEXT_DIM, false);

        buttons.add(new BtnHit(x, y, w, h, "", () -> selectType(type)));
    }

    private void drawPickStep(GuiGraphics g, int mouseX, int mouseY) {
        if (searchBox != null) searchBox.render(g, 0, 0, 0);

        List<RegistryCatalog.Entry> source = pickSource();
        List<RegistryCatalog.Entry> filtered = RegistryCatalog.filter(source, searchQuery);

        int x = layout.contentL;
        int y = layout.contentT + layout.s(60);
        int w = layout.contentR - layout.contentL;
        int bottom = layout.contentB - layout.s(28);
        int rowH = layout.s(24);
        int iconSize = layout.s(16);
        int textX = x + iconSize + layout.s(8);
        int totalH = filtered.size() * rowH;
        int contentH = bottom - y;
        int maxScroll = Math.max(0, totalH - contentH);
        pickScroll = Mth.clamp(pickScroll, 0, maxScroll);

        g.enableScissor(x, y, layout.contentR, bottom);
        int ry = y - (int) pickScroll;
        String selected = pickSelectedId();
        for (int i = 0; i < filtered.size(); i++) {
            RegistryCatalog.Entry entry = filtered.get(i);
            boolean sel = entry.id().equals(selected);
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + rowH;
            int bg = sel ? 0xE0_2A_3A_55 : (hover ? 0xD8_28_28_32 : 0xC8_1C_1C_22);
            g.fill(x, ry, x + w, ry + rowH - 1, bg);
            if (sel) g.fill(x, ry, x + 2, ry + rowH - 1, TwitchUi.PURPLE);
            ItemStack icon = entry.icon();
            if (!icon.isEmpty()) {
                int iconY = ry + (rowH - iconSize) / 2;
                g.renderItem(icon, x + layout.s(4), iconY);
                g.renderItemDecorations(font, icon, x + layout.s(4), iconY);
            }
            g.drawString(font, (sel ? "§b● " : "§8  ") + entry.label(), textX, ry + layout.s(7),
                    sel ? TwitchUi.TEXT : TwitchUi.TEXT_DIM, false);
            String idShort = shortId(entry.id());
            g.drawString(font, "§8" + idShort, x + w - font.width(idShort) - layout.s(6), ry + layout.s(7),
                    TwitchUi.TEXT_DIM, false);
            rows.add(new RowHit(x, ry, w, rowH, i));
            ry += rowH;
        }
        g.disableScissor();

        if (maxScroll > 0) {
            TwitchUi.drawScrollbar(g, layout.contentR - 4, y, bottom, pickScroll, maxScroll, contentH, totalH);
        }

        g.drawString(font, "§7Найдено: §f" + filtered.size(), x, layout.contentT + layout.s(44), TwitchUi.TEXT_DIM, false);

        int btnW = layout.s(72);
        int btnY = layout.contentB - layout.s(24);
        buttons.add(new BtnHit(layout.contentL, btnY, btnW, layout.s(20), "§7Назад", () -> goTo(Step.TYPE)));
        buttons.add(new BtnHit(layout.contentL + btnW + layout.s(8), btnY, btnW, layout.s(20), "§aДалее", () -> goTo(Step.SETTINGS)));
    }

    private List<RegistryCatalog.Entry> pickSource() {
        if (draft == null) return List.of();
        return switch (draft.actionType) {
            case "item" -> RegistryCatalog.items();
            case "mob" -> RegistryCatalog.mobs();
            default -> RegistryCatalog.effects();
        };
    }

    private String pickSelectedId() {
        if (draft == null) return "";
        return switch (draft.actionType) {
            case "item" -> draft.itemId;
            case "mob" -> draft.entityId;
            default -> draft.effectId;
        };
    }

    private ItemStack previewIcon() {
        if (draft == null) return ItemStack.EMPTY;
        return switch (draft.actionType == null ? "effect" : draft.actionType) {
            case "item" -> RegistryCatalog.iconFor(draft.itemId);
            case "mob" -> RegistryCatalog.iconFor(draft.entityId);
            default -> RegistryCatalog.iconFor(draft.effectId);
        };
    }

    private void drawSettingsStep(GuiGraphics g, int mouseX, int mouseY) {
        if (draft == null) return;
        int x = layout.contentL;
        int y = layout.contentT + layout.s(4);
        int w = layout.contentR - layout.contentL;

        TwitchUi.drawFeatureCard(g, font, x, y, w, TwitchUi.PURPLE, "§e!" + draft.displayName(),
                "Превью", describe(draft));
        ItemStack preview = previewIcon();
        if (!preview.isEmpty()) {
            int px = layout.contentR - layout.s(28);
            int py = y + layout.s(8);
            g.renderItem(preview, px, py);
            g.renderItemDecorations(font, preview, px, py);
        }
        y += layout.s(58);

        String type = draft.actionType == null ? "effect" : draft.actionType;
        if ("effect".equals(type)) {
            drawSliderRow(g, x, y, w, "Длительность", () -> draft.durationSeconds, 5, 600, v -> draft.durationSeconds = v);
            y += layout.s(28);
            drawSliderRow(g, x, y, w, "Сила (уровень)", () -> draft.amplifier + 1, 1, 10, v -> draft.amplifier = v - 1);
            y += layout.s(28);
        } else if ("item".equals(type)) {
            drawSliderRow(g, x, y, w, "Количество", () -> draft.itemCount, 1, 64, v -> draft.itemCount = v);
            y += layout.s(28);
        } else {
            drawSliderRow(g, x, y, w, "Количество мобов", () -> draft.mobCount, 1, 10, v -> draft.mobCount = v);
            y += layout.s(28);
        }

        drawSliderRow(g, x, y, w, "Кулдаун (сек)", () -> draft.cooldownSeconds, 5, 300, v -> draft.cooldownSeconds = v);
        y += layout.s(28);

        boolean posHover = over(mouseX, mouseY, x, y, w, layout.s(24));
        TwitchUi.drawToggleRow(g, font, x, y, w, "§aПоложительная команда (+Добро)", draft.positive, posHover);
        buttons.add(new BtnHit(x, y, w, layout.s(24), "", () -> draft.positive = !draft.positive));
        y += layout.s(30);

        boolean enHover = over(mouseX, mouseY, x, y, w, layout.s(24));
        TwitchUi.drawToggleRow(g, font, x, y, w, "Включена", draft.enabled, enHover);
        buttons.add(new BtnHit(x, y, w, layout.s(24), "", () -> draft.enabled = !draft.enabled));

        int btnW = layout.s(72);
        int btnY = layout.contentB - layout.s(24);
        buttons.add(new BtnHit(layout.contentL, btnY, btnW, layout.s(20), "§7Назад", () -> goTo(Step.PICK)));
        buttons.add(new BtnHit(layout.contentL + btnW + layout.s(8), btnY, btnW + layout.s(24), layout.s(20), "save", this::saveDraft));
    }

    private void drawSliderRow(GuiGraphics g, int x, int y, int w, String label,
                               java.util.function.Supplier<Integer> get, int min, int max,
                               java.util.function.IntConsumer setter) {
        int value = get.get();
        TwitchUi.drawIntRow(g, font, x, y, w, label, value);
        int btn = layout.s(16);
        int minusX = x + w - layout.s(70);
        int plusX = x + w - layout.s(36);
        buttons.add(new BtnHit(minusX, y + layout.s(4), btn, btn, "−",
                () -> setter.accept(TwitchUi.adjustInt(get.get(), -TwitchUi.intAdjustStep(), min, max))));
        buttons.add(new BtnHit(plusX, y + layout.s(4), btn, btn, "+",
                () -> setter.accept(TwitchUi.adjustInt(get.get(), TwitchUi.intAdjustStep(), min, max))));
        TwitchUi.drawSmallButton(g, font, minusX, y + layout.s(4), btn, btn, "−", false);
        TwitchUi.drawSmallButton(g, font, plusX, y + layout.s(4), btn, btn, "+", false);
    }

    private void drawFooter(GuiGraphics g, int mouseX, int mouseY) {
        TwitchMenuLayout.drawZoomControls(layout, g, font, zoomOutHover, zoomInHover);
        int fy = layout.panelB - layout.footerH;
        g.drawString(font, MenuHints.COMMANDS_FOOTER,
                layout.panelL + layout.pad + layout.s(56), fy + layout.s(8), TwitchUi.TEXT_DIM, false);
        if (step == Step.SETTINGS || step == Step.LIST) {
            saveHover = over(mouseX, mouseY, layout.contentR - layout.s(80), layout.contentB - layout.s(22),
                    layout.s(72), layout.s(18));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        if (closeHover) {
            onClose();
            return true;
        }
        if (TwitchMenuLayout.handleZoomClick(layout, mx, my)) {
            layout = TwitchMenuLayout.compute(width, height, BASE_FOOTER);
            rebuildFields();
            return true;
        }
        for (BtnHit btn : buttons) {
            if (over(mx, my, btn.x, btn.y, btn.w, btn.h)) {
                btn.action.run();
                return true;
            }
        }
        if (step == Step.LIST) {
            for (RowHit row : rows) {
                if (over(mx, my, row.x, row.y, row.w, row.h)) {
                    int delX = row.x + row.w - layout.s(28);
                    if (mx >= delX && mx < delX + layout.s(16)) {
                        deleteAt(row.index);
                    } else {
                        editAt(row.index);
                    }
                    return true;
                }
            }
        }
        if (step == Step.PICK) {
            List<RegistryCatalog.Entry> filtered = RegistryCatalog.filter(pickSource(), searchQuery);
            for (RowHit row : rows) {
                if (over(mx, my, row.x, row.y, row.w, row.h) && row.index < filtered.size()) {
                    selectEntry(filtered.get(row.index));
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (TwitchMenuLayout.handleZoomScroll(layout, mouseX, mouseY, scrollY)) {
            layout = TwitchMenuLayout.compute(width, height, BASE_FOOTER);
            rebuildFields();
            return true;
        }
        if (step == Step.LIST) {
            listScroll -= scrollY * layout.s(12);
            return true;
        }
        if (step == Step.PICK) {
            pickScroll -= scrollY * layout.s(10);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            if (step != Step.LIST) {
                goTo(Step.LIST);
                return true;
            }
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private void startCreate() {
        draft = new CustomCommandDefinition();
        draft.id = "custom_" + UUID.randomUUID().toString().substring(0, 8);
        draft.enabled = true;
        editingIndex = -1;
        statusMessage = "";
        goTo(Step.NAME);
    }

    private void editAt(int index) {
        if (index < 0 || index >= config.customCommands.size()) return;
        draft = copyDef(config.customCommands.get(index));
        editingIndex = index;
        statusMessage = "";
        goTo(Step.SETTINGS);
    }

    private void deleteAt(int index) {
        if (index < 0 || index >= config.customCommands.size()) return;
        config.customCommands.remove(index);
        setStatus("§cКоманда удалена. Нажми «Сохранить» внизу или создай новую.", 0xFF_FF_88_88);
    }

    private void confirmName() {
        if (nameBox == null) return;
        String name = nameBox.getValue().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-zа-яё0-9_]", "");
        if (name.length() < 2) {
            setStatus("§cИмя слишком короткое (мин. 2 символа)", 0xFF_FF_66_66);
            return;
        }
        if (CustomCommandRegistry.conflictsWithBuiltIn(name)) {
            setStatus("§cТакая команда уже есть во встроенных (!" + name + ")", 0xFF_FF_66_66);
            return;
        }
        if (CustomCommandRegistry.conflictsWithCustom(name, draft != null ? draft.id : null, config.customCommands)) {
            setStatus("§cУ тебя уже есть команда !" + name, 0xFF_FF_66_66);
            return;
        }
        if (draft != null) draft.trigger = name;
        setStatus("", TwitchUi.TEXT_DIM);
        goTo(Step.TYPE);
    }

    private void selectType(String type) {
        if (draft == null) return;
        draft.actionType = type;
        searchQuery = "";
        pickScroll = 0;
        goTo(Step.PICK);
    }

    private void selectEntry(RegistryCatalog.Entry entry) {
        if (draft == null) return;
        switch (draft.actionType) {
            case "item" -> draft.itemId = entry.id();
            case "mob" -> draft.entityId = entry.id();
            default -> draft.effectId = entry.id();
        }
    }

    private void saveDraft() {
        if (draft == null) return;
        if (draft.trigger == null || draft.trigger.length() < 2) {
            setStatus("§cСначала задай имя команды", 0xFF_FF_66_66);
            goTo(Step.NAME);
            return;
        }
        if (editingIndex >= 0 && editingIndex < config.customCommands.size()) {
            config.customCommands.set(editingIndex, copyDef(draft));
        } else {
            if (config.customCommands.size() >= 40) {
                setStatus("§cМаксимум 40 своих команд", 0xFF_FF_66_66);
                return;
            }
            config.customCommands.add(copyDef(draft));
        }
        ConfigClientNetworking.send(config, profile);
        ConfigManager.set(copy(config));
        setStatus("§aКоманда !" + draft.displayName() + " сохранена!", 0xFF_66_FF_99);
        draft = null;
        editingIndex = -1;
        goTo(Step.LIST);
    }

    private void goTo(Step next) {
        step = next;
        rebuildFields();
    }

    private void setStatus(String msg, int color) {
        statusMessage = msg;
        statusColor = color;
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static TwitchCraftConfig copy(TwitchCraftConfig src) {
        return GSON.fromJson(GSON.toJson(src), TwitchCraftConfig.class);
    }

    private static CustomCommandDefinition copyDef(CustomCommandDefinition src) {
        return GSON.fromJson(GSON.toJson(src), CustomCommandDefinition.class);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
