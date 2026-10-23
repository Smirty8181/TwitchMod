package net.twitchcraft.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.GoodCommands;
import net.twitchcraft.config.AudiencePresets;
import net.twitchcraft.config.ConfigApply;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.LocalProfileStore;
import net.twitchcraft.config.PresetIO;
import net.twitchcraft.config.StreamGenrePresets;
import net.twitchcraft.config.StreamerProfile;
import net.twitchcraft.config.StreamerProfileMerge;
import net.twitchcraft.config.CustomCommandDefinition;
import net.twitchcraft.config.TwitchCraftConfig;
import net.twitchcraft.donation.DonationTierResolver;
import net.twitchcraft.stream.StreamTickHandler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class SettingsScreen extends Screen {
    private static final int BASE_FOOTER = 28;

    private record Tab(String id, String icon, String title, int color) {}
    private record ToggleHit(int x, int y, int w, int h, String label, Supplier<Boolean> get, java.util.function.Consumer<Boolean> set) {}
    private record IntHit(int x, int y, int w, int h, String label, int min, int max, Supplier<Integer> get, IntConsumer set) {}
    private record PresetHit(int x, int y, int w, int h, String label, Runnable apply, boolean requireOp) {}

    private record ThemeHit(int x, int y, int w, int h, UiTheme.Id theme) {}
    private record CategoryHit(int x, int y, int w, int h, String label, int color) {}

    private static final Tab[] TABS = {
            new Tab("guide", "🚀", "Старт", 0xFF_FF_85_C0),
            new Tab("twitch", "📡", "Twitch", TwitchUi.PURPLE),
            new Tab("stream", "⚡", "Стрим", 0xFF_BF_94_FF),
            new Tab("hud", "📊", "HUD", 0xFF_74_C0_FC),
            new Tab("balance", "⚖", "Баланс", 0xFF_FF_9F_43),
            new Tab("security", "🔒", "Защита", 0xFF_FF_55_66),
            new Tab("feedback", "🔔", "Отклик", 0xFF_55_FF_55),
            new Tab("donations", "💰", "Донаты", 0xFF_FF_D7_00),
            new Tab("commands", "⌨", "Команды", 0xFF_91_46_FF),
    };

    private final TwitchCraftConfig config;
    private StreamerProfile localProfile;
    private int selectedTab;
    private double contentScroll;
    private double sidebarScroll;
    private int contentHeight;
    private int hoveredTab = -1;
    private boolean closeHover;
    private boolean saveHover;
    private boolean zoomOutHover;
    private boolean zoomInHover;

    private TwitchMenuLayout layout;
    private int saveX, saveY, saveW, saveH;

    private EditBox mcName, twitchChannel, oauthBox, daBox;
    private EditBox moderatorsBox;
    private EditBox whitelistBox;
    private EditBox donationTokenBox;
    private EditBox featuredCmdBox;
    private final List<EditBox> tierCommandBoxes = new ArrayList<>();
    private final List<EditBox> tierLabelBoxes = new ArrayList<>();
    private final List<ToggleHit> toggles = new ArrayList<>();
    private final List<IntHit> intControls = new ArrayList<>();
    private final List<PresetHit> presetHits = new ArrayList<>();
    private final List<ThemeHit> themeHits = new ArrayList<>();
    private final List<CategoryHit> categoryHeaders = new ArrayList<>();
    private final Map<EditBox, Integer> fieldBaseY = new HashMap<>();
    private int lastFieldScroll = Integer.MIN_VALUE;
    private int cachedContentScroll = Integer.MIN_VALUE;
    private List<ToggleHit> visibleToggles = List.of();
    private List<IntHit> visibleIntControls = List.of();
    private List<PresetHit> visiblePresets = List.of();
    private List<ThemeHit> visibleThemes = List.of();
    private List<CategoryHit> visibleCategories = List.of();
    private final List<TwitchUi.LinkHit> linkHits = new ArrayList<>();
    /** Y превью темы (вкладка HUD) — совпадает с layout, не перекрывает кнопки. */
    private int hudMiniPreviewY = -1;
    private TwitchCraftConfig initialConfig;
    private final TextDraft initialDraft = new TextDraft();

    /** Черновик текстовых полей — сохраняется при переключении вкладок. */
    private static final class TextDraft {
        String twitchChannel = "";
        String oauthToken = "";
        String donationToken = "";
        String featuredCommandId = "lightning";
        String whitelistRaw = "";
        String moderatorsRaw = "";
        String sharedDonationToken = "";
        final List<String> tierCommands = new ArrayList<>();
        final List<String> tierLabels = new ArrayList<>();
    }

    private final TextDraft draft = new TextDraft();
    private EditBox searchBox;
    private String searchQuery = "";
    private boolean rebuilding;

    public SettingsScreen() {
        super(Component.empty());
        this.config = copy(ConfigManager.get());
        net.twitchcraft.config.HudLegacySync.ensure(this.config);
        HudClientStore.applyTo(this.config);
        this.localProfile = LocalProfileStore.load();
        resolveLocalProfileDefaults();
        loadDraftFromState();
        initialConfig = copy(config);
        snapshotDraft(initialDraft, draft);
        if (!localProfile.hasTwitch()) {
            selectedTab = 0;
        }
        UiTheme.apply(config);
    }

    private void resolveLocalProfileDefaults() {
        Minecraft mc = Minecraft.getInstance();
        String sessionName = mc.player != null ? mc.player.getGameProfile().name()
                : (mc.getUser() != null ? mc.getUser().getName() : "");
        if (localProfile.minecraftName == null || localProfile.minecraftName.isBlank()) {
            localProfile.minecraftName = sessionName;
        }
        if (localProfile.twitchChannel == null) localProfile.twitchChannel = "";
        if (localProfile.oauthToken == null) localProfile.oauthToken = "";
        if (localProfile.donationToken == null) localProfile.donationToken = "";
        if ((localProfile.twitchChannel == null || localProfile.twitchChannel.isBlank())
                && sessionName != null && !sessionName.isBlank()) {
            net.twitchcraft.stream.StreamerProfiles.byMinecraftName(sessionName).ifPresent(p -> {
                if (localProfile.twitchChannel.isBlank()) localProfile.twitchChannel = p.twitchChannel;
                if (localProfile.oauthToken.isBlank()) localProfile.oauthToken = p.oauthToken;
                if (localProfile.donationToken.isBlank()) localProfile.donationToken = p.donationToken;
            });
        }
    }

    @Override
    protected void init() {
        selectedTab = Mth.clamp(selectedTab, 0, TABS.length - 1);
        recomputeLayout();
        rebuildTab();
    }

    private void recomputeLayout() {
        layout = TwitchMenuLayout.compute(width, height, BASE_FOOTER);
        saveW = layout.s(72);
        saveH = layout.s(18);
        saveX = layout.panelR - saveW - layout.pad;
        saveY = layout.panelB - layout.footerH + layout.s(4);
    }

    @Override
    public void resize(net.minecraft.client.Minecraft client, int width, int height) {
        super.resize(client, width, height);
        recomputeLayout();
        lastFieldScroll = Integer.MIN_VALUE;
        cachedContentScroll = Integer.MIN_VALUE;
        applyScrollToFields();
    }

    private void rebuildTab() {
        boolean keepSearchFocus = searchBox != null && searchBox.isFocused();
        int savedSearchCursor = searchBox != null ? searchBox.getCursorPosition() : -1;
        rebuilding = true;
        clearWidgets();
        toggles.clear();
        intControls.clear();
        presetHits.clear();
        themeHits.clear();
        categoryHeaders.clear();
        hudMiniPreviewY = -1;
        tierCommandBoxes.clear();
        tierLabelBoxes.clear();
        fieldBaseY.clear();
        lastFieldScroll = Integer.MIN_VALUE;
        cachedContentScroll = Integer.MIN_VALUE;
        contentScroll = 0;
        searchBox = null;

        int row = layout.s(28);
        int boxH = layout.s(18);
        int fieldW = layout.contentR - layout.contentL;
        int y = layout.contentT;

        mcName = twitchChannel = oauthBox = daBox = null;
        moderatorsBox = null;
        whitelistBox = null;
        donationTokenBox = null;
        featuredCmdBox = null;

        String query = searchQuery == null ? "" : searchQuery.trim();
        if (!query.isBlank()) {
            y = buildSearchResults(y, row, query);
            contentHeight = Math.max(0, y - layout.contentT);
            applyScrollToFields();
            refreshScrollCache();
            addSearchBox(keepSearchFocus, savedSearchCursor);
            rebuilding = false;
            return;
        }

        switch (selectedTab) {
            case 0 -> {
                y += layout.s(6);
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(18), "§d§lДобро пожаловать в TwitchCraft", TwitchUi.PURPLE_LIGHT));
                y += layout.s(22);
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(14), MenuHints.guideWelcome(), TwitchUi.TEXT_DIM));
                y += layout.s(20);

                addPreset(y, "§d✦ §lОткрыть мастер настройки", () -> {
                    pullTextFields();
                    if (minecraft != null) MenuNavigation.openSetupWizard(this);
                });
                y += row;
                addPreset(y, "§a📦 Папка для напарника §8(jar + Physics + пресет + ПРОЧТИ)",
                        this::exportPartnerPack, false);
                y += row;
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(14), "§7Скинь архив папки — без OAuth и DonateX", TwitchUi.TEXT_DIM));
                y += layout.s(18);

                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(18), "§fШпаргалка", 0xFF_F5_D0_6E));
                y += layout.s(20);
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(14), "§7K настройки · R правила · I тесты · U команды · P статистика · колёсико — пинг",
                        TwitchUi.TEXT_DIM));
                y += layout.s(16);
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(14), "§7Свой Twitch в K → Twitch. КД как у напарника: Баланс → Как у напарника",
                        TwitchUi.TEXT_DIM));
                y += layout.s(16);
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(14), "§7Physics: Esc → Настройки → Physics Settings. Лаги — Max Objects вниз",
                        TwitchUi.TEXT_DIM));
                y += layout.s(18);

                boolean hasChannel = draft.twitchChannel != null && !draft.twitchChannel.isBlank();
                boolean hasOAuth = draft.oauthToken != null && !draft.oauthToken.isBlank();
                boolean connected = SidebarOverlay.isTwitchConnected();
                boolean hasDa = (draft.donationToken != null && !draft.donationToken.isBlank())
                        || localProfile.hasDonations();

                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(18), "§f📋 Чеклист первого запуска", TwitchUi.PURPLE));
                y += layout.s(22);
                addPreset(y, MenuHints.step(hasChannel && hasOAuth, 1, "Заполнить Twitch (канал + OAuth)"),
                        () -> selectTab(1)); y += row;
                addPreset(y, MenuHints.step(connected, 2, "Нажать §aСохранить §7и §f/twitchcraft connect"),
                        () -> selectTab(1)); y += row;
                addPreset(y, MenuHints.step(hasDa, 3, "Подключить DonateX (по желанию)"),
                        () -> selectTab(1)); y += row;
                addPreset(y, MenuHints.step(false, 4, "Настроить HUD (сайдбар, лента, смерти)"),
                        () -> selectTab(3)); y += row;
                y += layout.s(6);

                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(18), "§f⚙ Быстрый переход", 0xFF_74_C0_FC));
                y += layout.s(22);
                addPreset(y, "§d📡 Twitch — подключение чата", () -> selectTab(1)); y += row;
                addPreset(y, "§e⚡ Стрим — хаос, смерти, суд", () -> selectTab(2)); y += row;
                addPreset(y, "§b📊 HUD — тема и конструктор", () -> selectTab(3)); y += row;
                addPreset(y, "§6💰 Донаты — уровни и суммы", () -> selectTab(7)); y += row;
                addPreset(y, "§5⌨ Команды — КД и лимиты", () -> selectTab(8)); y += row;
                y += layout.s(6);

                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(18), "§f📚 Справка", 0xFF_FF_85_C0));
                y += layout.s(22);
                addPreset(y, "§d📖 Правила для стримера §8(R)", () -> {
                    pullTextFields();
                    if (minecraft != null) MenuNavigation.openRules(this);
                }); y += row;
                addPreset(y, "§b⌨ Конструктор своих команд §8(U)", () -> {
                    pullTextFields();
                    if (minecraft != null) MenuNavigation.openCommandCreator(this);
                }); y += row;
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(14), "§7K §8— §7настройки · §7R §8— §7правила · §7U §8— §7свои команды", TwitchUi.TEXT_DIM));
                y += layout.s(18);
            }
            case 1 -> {
                y += layout.s(52);
                addToggle(y, "§dРежим «2 стримера» (локальный сервер)", () -> config.stream.duoStreamMode,
                        v -> {
                            config.stream.duoStreamMode = v;
                            if (v) AudiencePresets.apply(config, AudiencePresets.DUO);
                        }); y += row;
                addToggle(y, "Автоподключение Twitch", () -> config.twitch.autoConnect, v -> config.twitch.autoConnect = v); y += row;
                addToggle(y, "§6Bits / сабы / рейды (IRC)", () -> config.twitch.eventSubEnabled, v -> config.twitch.eventSubEnabled = v); y += row;
                addIntRow(y, "Bits: минимум для яйца", 1, 10000, () -> config.twitch.bitsMinAmount, v -> config.twitch.bitsMinAmount = v);
                y += row + layout.s(6);

                int block = layout.s(54);
                int hintBlock = layout.s(68);
                mcName = field(layout.contentL, y + layout.s(14), fieldW, boxH, "mc", localProfile.minecraftName);
                mcName.setEditable(false);
                mcName.setTextColor(0xFF_A0_A0_A0);
                fieldBaseY.put(mcName, y + layout.s(14));
                y += block;

                twitchChannel = field(layout.contentL, y + layout.s(14), fieldW, boxH, "ch", draft.twitchChannel);
                twitchChannel.setHint(Component.literal(TwitchSetupHints.CH_PLACEHOLDER));
                fieldBaseY.put(twitchChannel, y + layout.s(14));
                y += block;

                oauthBox = field(layout.contentL, y + layout.s(14), fieldW, boxH, "oauth", mask(draft.oauthToken));
                oauthBox.setMaxLength(128);
                oauthBox.setHint(Component.literal(TwitchSetupHints.OAUTH_PLACEHOLDER));
                fieldBaseY.put(oauthBox, y + layout.s(14));
                y += hintBlock;

                daBox = field(layout.contentL, y + layout.s(14), fieldW, boxH, "da", mask(draft.donationToken));
                daBox.setMaxLength(1024);
                daBox.setHint(Component.literal(TwitchSetupHints.DA_PLACEHOLDER));
                fieldBaseY.put(daBox, y + layout.s(14));
                y += hintBlock + layout.s(8);
            }
            case 2 -> {
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(18), "§f🎲 Действия стримера", 0xFF_F1_C4_0F));
                y += layout.s(22);
                addPreset(y, "§d⚔ Вызов  §c−1 заряд  §7чат в форе +2, ничья = бомба",
                        () -> StreamerActionClient.send("challenge")); y += row;
                addPreset(y, "§e🕊 Милость  §7снимет блок урона, но тыква",
                        () -> StreamerActionClient.send("mercy")); y += row;
                addPreset(y, "§c📜 Контракт  §7бесплатно: 90с не умри или армия чата",
                        () -> StreamerActionClient.send("contract")); y += row;
                addPreset(y, "§6✦ Жертва  §7алмаз/тотем/незерит в руке → заряд",
                        () -> StreamerActionClient.send("sacrifice")); y += row;
                addPreset(y, "§c🗯 Насмешка  §7урон без КД 25с, смерть = армия",
                        () -> StreamerActionClient.send("taunt")); y += row;
                addPreset(y, "§e💰 Выкуп  §7бомба/охота: предмет в руке, без награды",
                        () -> StreamerActionClient.send("bargain")); y += row;
                addPreset(y, "§6🎲 Проверить кубик мира  §8/twitchcraft testdice",
                        () -> StreamerActionClient.send("testdice")); y += row;
                addPreset(y, "§6✦ Спавн груза/тотема/гостя  §8тест находки мира",
                        () -> StreamerActionClient.send("testdrop")); y += row;
                addPreset(y, "§c🎡 Проверить колесо хаоса  §8/twitchcraft testchaos",
                        () -> StreamerActionClient.send("testchaos")); y += row;
                addPreset(y, "§e🧪 Лаборатория тестов  §8клавиша I  · донаты · команды · шоу",
                        () -> Minecraft.getInstance().setScreen(new TestLabScreen())); y += row;
                addIntRow(y, "Щит кубика мира (мин)", 1, 15, () -> config.stream.diceShieldMinutes,
                        v -> config.stream.diceShieldMinutes = v); y += row;
                addIntRow(y, "Вызов: длительность (сек)", 10, 90, () -> config.stream.challengeSeconds,
                        v -> config.stream.challengeSeconds = v); y += row;
                addToggle(y, "Диктор вех: чат + озвучка (алмаз, Ад, Край)",
                        () -> config.stream.milestoneNarratorEnabled,
                        v -> config.stream.milestoneNarratorEnabled = v); y += row;
                addToggle(y, "Размер: !муравей / !титан",
                        () -> config.stream.sizePranksEnabled,
                        v -> config.stream.sizePranksEnabled = v); y += row;
                addIntRow(y, "Муравей (сек)", 8, 60, () -> config.stream.antSeconds,
                        v -> config.stream.antSeconds = v); y += row;
                addIntRow(y, "Титан (сек)", 6, 40, () -> config.stream.titanSeconds,
                        v -> config.stream.titanSeconds = v); y += row;
                addToggle(y, "Ритуал !сикссевен / !67",
                        () -> config.stream.sixSevenEnabled,
                        v -> config.stream.sixSevenEnabled = v); y += row;
                addPreset(y, "§cСбросить статистику этого мира §8смерти · команды · вехи",
                        () -> StreamerActionClient.send("resetstats")); y += row + layout.s(8);

                addToggle(y, "§dХаусмод — без КД для всего чата", () -> config.stream.houseMode, v -> config.stream.houseMode = v); y += row;
                addToggle(y, "Призраки зрителей [Viewer]", () -> config.stream.ghostMobsEnabled, v -> config.stream.ghostMobsEnabled = v); y += row;
                addToggle(y, "Имена мобов от зрителя", () -> config.stream.namedMobs, v -> config.stream.namedMobs = v); y += row;
                addToggle(y, "Суд чата при смерти", () -> config.stream.courtEnabled, v -> config.stream.courtEnabled = v); y += row;
                addIntRow(y, "Суд: длительность (сек)", 10, 120, () -> config.stream.courtDurationSeconds, v -> config.stream.courtDurationSeconds = v); y += row;
                addToggle(y, "Реванш при смерти", () -> config.stream.revengeOnDeath, v -> config.stream.revengeOnDeath = v); y += row;
                addIntRow(y, "Задержка реванша (сек)", 0, 30, () -> config.stream.revengeDelaySeconds, v -> config.stream.revengeDelaySeconds = v); y += row;
                addToggle(y, "Boss Bar: Чат vs Стример", () -> config.stream.battleBarEnabled, v -> config.stream.battleBarEnabled = v); y += row;
                addIntRow(y, "HP битвы", 20, 200, () -> config.stream.battleMaxHp, v -> config.stream.battleMaxHp = v); y += row;
                addToggle(y, "Счётчик хаоса", () -> config.stream.chaosMeterEnabled, v -> config.stream.chaosMeterEnabled = v); y += row;
                addIntRow(y, "Макс. хаос", 10, 200, () -> config.stream.chaosMeterMax, v -> config.stream.chaosMeterMax = v); y += row;
                addToggle(y, "Boss Bar хаоса", () -> config.stream.chaosBossBar, v -> config.stream.chaosBossBar = v); y += row;
                addPreset(y, "§5✦ Я проговорился  §7сжечь покровителя, если сказал ник",
                        () -> StreamerActionClient.send("saidname")); y += row;
                addToggle(y, "Покровитель чата (ник нельзя произносить)",
                        () -> config.stream.unnamedPatronEnabled,
                        v -> config.stream.unnamedPatronEnabled = v); y += row;
                addIntRow(y, "Покровитель: смена (мин)", 6, 30, () -> config.stream.unnamedPatronMinutes,
                        v -> config.stream.unnamedPatronMinutes = v); y += row;
                addToggle(y, "Загадка !????? — угадал, усиление 90с",
                        () -> config.stream.cipherRiddleEnabled,
                        v -> config.stream.cipherRiddleEnabled = v); y += row;
                addIntRow(y, "Загадка: длительность (сек)", 60, 600, () -> config.stream.cipherRiddleSeconds,
                        v -> config.stream.cipherRiddleSeconds = v); y += row;
                addToggle(y, "Случайные события", () -> config.stream.randomEventsEnabled,
                        v -> config.stream.randomEventsEnabled = v); y += row;
                addIntRow(y, "События: мин (от)", 1, 30, () -> config.stream.randomEventMinMinutes, v -> config.stream.randomEventMinMinutes = v); y += row;
                addIntRow(y, "События: мин (до)", 1, 60, () -> config.stream.randomEventMaxMinutes, v -> config.stream.randomEventMaxMinutes = v); y += row;
                addToggle(y, "Находки в мире (груз / тотем / гость)",
                        () -> config.stream.worldEncountersEnabled,
                        v -> config.stream.worldEncountersEnabled = v); y += row;
                addIntRow(y, "Находки: мин (от)", 4, 20, () -> config.stream.worldEncounterMinMinutes,
                        v -> config.stream.worldEncounterMinMinutes = v); y += row;
                addIntRow(y, "Находки: мин (до)", 6, 30, () -> config.stream.worldEncounterMaxMinutes,
                        v -> config.stream.worldEncounterMaxMinutes = v); y += row;
                addIntRow(y, "Голосование (сек)", 15, 120, () -> config.stream.voteDurationSeconds, v -> config.stream.voteDurationSeconds = v);
                y += row + layout.s(8);

                if (config.death == null) config.death = new TwitchCraftConfig.DeathSettings();
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(18), "§f☠ Смерти и штраф чата", 0xFF_FF_55_66));
                y += layout.s(22);
                addToggle(y, "Система смертей", () -> config.death.enabled, v -> config.death.enabled = v); y += row;
                addIntRow(y, "Смертей для штрафа", 2, 20, () -> config.death.deathsForPenalty,
                        v -> config.death.deathsForPenalty = v); y += row;
                addIntRow(y, "Окно смертей (мин)", 1, 60, () -> config.death.deathWindowMinutes,
                        v -> config.death.deathWindowMinutes = v); y += row;
                addIntRow(y, "Блок зла (мин)", 1, 30, () -> config.death.evilBlockMinutes,
                        v -> config.death.evilBlockMinutes = v); y += row;
                addToggle(y, "§cБлокировать команды с уроном (не пранки)", () -> config.death.blockEvilCommands,
                        v -> config.death.blockEvilCommands = v); y += row;
                addIntRow(y, "Пауза урона после смерти (сек)", 5, 60, () -> config.death.respawnGraceSeconds,
                        v -> config.death.respawnGraceSeconds = v); y += row;
                addIntRow(y, "Пауза между ударами чата (сек)", 4, 40, () -> config.death.evilPaceSeconds,
                        v -> config.death.evilPaceSeconds = v); y += row;
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(14), "§7Как показывать смерти на экране", TwitchUi.TEXT_DIM));
                y += layout.s(18);
                addPreset(y, "§e→ Открыть настройки смерти в HUD", () -> openHudCustomizer(2));
                y += row;
            }
            case 3 -> {
                y += layout.s(16);
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(18), "§f🎨 Тема интерфейса", TwitchUi.PURPLE));
                y += layout.s(22);
                int cols = 3;
                int gap = layout.s(8);
                int themeBtnH = layout.s(28);
                int themeRowH = themeBtnH + layout.s(6);
                int btnW = (fieldW - gap * (cols - 1)) / cols;
                UiTheme.Id[] themes = UiTheme.Id.values();
                int themeRows = (themes.length + cols - 1) / cols;
                for (int i = 0; i < themes.length; i++) {
                    int col = i % cols;
                    int rowIdx = i / cols;
                    int bx = layout.contentL + col * (btnW + gap);
                    int by = y + rowIdx * themeRowH;
                    themeHits.add(new ThemeHit(bx, by, btnW, themeBtnH, themes[i]));
                }
                y += themeRowH * themeRows + layout.s(10);

                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(18), "§f👁 Превью темы", TwitchUi.PURPLE_LIGHT));
                y += layout.s(20);
                hudMiniPreviewY = y;
                y += layout.s(52) + layout.s(8);

                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(18), "§f📐 HUD и оверлеи", TwitchUi.PURPLE));
                y += layout.s(22);
                addPreset(y, "§d✦ §lОткрыть конструктор HUD", () -> openHudCustomizer(0));
                y += row;
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(14), "§7Сайдбар · лента · донаты · смерти · координаты · цвета", TwitchUi.TEXT_DIM));
                y += layout.s(18);
            }
            case 4 -> {
                y += layout.s(10);
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(18), "§f🎬 Профиль стрима", TwitchUi.PURPLE));
                y += layout.s(20);
                String genre = config.streamGenre != null ? config.streamGenre : StreamGenrePresets.VANILLA;
                addPreset(y, (StreamGenrePresets.VANILLA.equals(genre) ? "§a▶ " : "§7  ")
                                + "Ванила — классический набор",
                        () -> StreamGenrePresets.apply(config, StreamGenrePresets.VANILLA)); y += row;
                addPreset(y, (StreamGenrePresets.HARDCORE.equals(genre) ? "§a▶ " : "§7  ")
                                + "Хардкор — без летала, жёстче КД",
                        () -> StreamGenrePresets.apply(config, StreamGenrePresets.HARDCORE)); y += row;
                addPreset(y, (StreamGenrePresets.MODPACK.equals(genre) ? "§a▶ " : "§7  ")
                                + "Модпак — гусь / лаки / мем-команды",
                        () -> StreamGenrePresets.apply(config, StreamGenrePresets.MODPACK)); y += row;
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(14), "§7Сбрасывает КД под размер аудитории, затем включает нужный набор",
                        TwitchUi.TEXT_DIM));
                y += layout.s(18);

                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(18), "§f👥 Размер аудитории", 0xFF_FF_9F_43));
                y += layout.s(20);
                String cur = config.audiencePreset != null ? config.audiencePreset : AudiencePresets.MEDIUM;
                addPreset(y, (AudiencePresets.SMALL.equals(cur) ? "§a▶ " : "§7  ") + "до 30 зрителей — быстрый чат",
                        () -> AudiencePresets.apply(config, AudiencePresets.SMALL)); y += row;
                addPreset(y, (AudiencePresets.MEDIUM.equals(cur) ? "§a▶ " : "§7  ") + "30–100 зрителей — стандарт",
                        () -> AudiencePresets.apply(config, AudiencePresets.MEDIUM)); y += row;
                addPreset(y, (AudiencePresets.LARGE.equals(cur) ? "§a▶ " : "§7  ") + "100–250 зрителей — жёстче",
                        () -> AudiencePresets.apply(config, AudiencePresets.LARGE)); y += row;
                addPreset(y, (AudiencePresets.MEGA.equals(cur) ? "§a▶ " : "§7  ") + "250+ зрителей — максимум",
                        () -> AudiencePresets.apply(config, AudiencePresets.MEGA)); y += row;
                y += layout.s(8);
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(18), "§f📦 Шаринг пресета", TwitchUi.PURPLE_LIGHT));
                y += layout.s(22);
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(14), "§7Без OAuth / токенов DonateX / ников — только баланс и шоу", TwitchUi.TEXT_DIM));
                y += layout.s(16);
                addPreset(y, "§a⬆ Экспорт пресета §8(буфер + файл)", this::exportPreset); y += row;
                addPreset(y, "§b⬇ Импорт пресета §8(из буфера)", this::importPreset); y += row;
                addPreset(y, "§d↔ Как у напарника §8(КД, HUD, шоу — без токенов)",
                        this::requestPartnerPreset, false); y += row;
                addPreset(y, "§a📦 Папка для напарника §8(скинуть архивом)",
                        this::exportPartnerPack, false); y += row;
                y += layout.s(6);
                addIntRow(y, "Антиспам зрителя (сек)", 0, 60, () -> config.security.antiSpamSeconds,
                        v -> config.security.antiSpamSeconds = v); y += row;
                addToggle(y, "Плавающий КД от активности чата", () -> config.stream.chatActivityCooldowns,
                        v -> config.stream.chatActivityCooldowns = v); y += row;
                addIntRow(y, "Лимит мобов/мин", 1, 60, () -> config.security.mobSpawnRateLimitPerMinute,
                        v -> config.security.mobSpawnRateLimitPerMinute = v); y += row;
                addIntRow(y, "Лимит TNT/мин", 1, 30, () -> config.security.tntRateLimitPerMinute,
                        v -> config.security.tntRateLimitPerMinute = v); y += row;
                addIntRow(y, "Лимит хаос/мин", 1, 40, () -> config.security.chaosRateLimitPerMinute,
                        v -> config.security.chaosRateLimitPerMinute = v); y += row;
                addIntRow(y, "Лимит эффекты/мин", 1, 40, () -> config.security.effectRateLimitPerMinute,
                        v -> config.security.effectRateLimitPerMinute = v); y += row;
                addIntRow(y, "Лимит движение/мин", 1, 40, () -> config.security.movementRateLimitPerMinute,
                        v -> config.security.movementRateLimitPerMinute = v); y += row;
                addIntRow(y, "Лимит инвент/мин", 1, 40, () -> config.security.inventoryRateLimitPerMinute,
                        v -> config.security.inventoryRateLimitPerMinute = v); y += row;
                addIntRow(y, "Лимит мир/мин", 1, 30, () -> config.security.worldRateLimitPerMinute,
                        v -> config.security.worldRateLimitPerMinute = v); y += row;
                addIntRow(y, "Лимит шоу/мин", 1, 30, () -> config.security.funRateLimitPerMinute,
                        v -> config.security.funRateLimitPerMinute = v);
                y += row;
                y += layout.s(8);
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(18), "§f⏱ Общие КД по пакам", 0xFF_FF_9F_43));
                y += layout.s(22);
                addToggle(y, "Общий КД на пак у этого стримера", () -> config.cooldownPacks.sharedPackCooldowns,
                        v -> config.cooldownPacks.sharedPackCooldowns = v); y += row;
                addIntRow(y, "§a+ Добро (сек)", 0, 300, () -> config.cooldownPacks.goodPackSeconds,
                        v -> config.cooldownPacks.goodPackSeconds = v); y += row;
                addIntRow(y, "§cХаос (сек)", 0, 300, () -> config.cooldownPacks.chaosPackSeconds,
                        v -> config.cooldownPacks.chaosPackSeconds = v); y += row;
                addIntRow(y, "Мобы (сек)", 0, 300, () -> config.cooldownPacks.mobPackSeconds,
                        v -> config.cooldownPacks.mobPackSeconds = v); y += row;
                addIntRow(y, "TNT (сек)", 0, 300, () -> config.cooldownPacks.tntPackSeconds,
                        v -> config.cooldownPacks.tntPackSeconds = v); y += row;
                addIntRow(y, "Эффекты (сек)", 0, 300, () -> config.cooldownPacks.effectPackSeconds,
                        v -> config.cooldownPacks.effectPackSeconds = v); y += row;
                addIntRow(y, "Движение (сек)", 0, 300, () -> config.cooldownPacks.movementPackSeconds,
                        v -> config.cooldownPacks.movementPackSeconds = v); y += row;
                addIntRow(y, "Инвентарь (сек)", 0, 300, () -> config.cooldownPacks.inventoryPackSeconds,
                        v -> config.cooldownPacks.inventoryPackSeconds = v); y += row;
                addIntRow(y, "Мир (сек)", 0, 300, () -> config.cooldownPacks.worldPackSeconds,
                        v -> config.cooldownPacks.worldPackSeconds = v); y += row;
                addIntRow(y, "Шоу (сек)", 0, 300, () -> config.cooldownPacks.funPackSeconds,
                        v -> config.cooldownPacks.funPackSeconds = v);
                y += row;
            }
            case 5 -> {
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(14), "§7Белый список — только эти ники могут вызывать команды", TwitchUi.TEXT_DIM));
                y += layout.s(18);
                addToggle(y, "Белый список (только из списка)", () -> config.security.whitelistEnabled, v -> config.security.whitelistEnabled = v); y += row;
                addToggle(y, "Блок в Hardcore", () -> config.security.disableInHardcore, v -> config.security.disableInHardcore = v); y += row;
                whitelistBox = field(layout.contentL, y, fieldW, boxH, "whitelist", draft.whitelistRaw); y += layout.s(38);
                moderatorsBox = field(layout.contentL, y, fieldW, boxH, "moderators", draft.moderatorsRaw);
                y += layout.s(38);
            }
            case 6 -> {
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(14), "§7Куда показывать результат команд зрителей (не настройки чата)", TwitchUi.TEXT_DIM));
                y += layout.s(18);
                addToggle(y, "Сообщения в Twitch-чат", () -> config.feedback.announceInTwitchChat, v -> config.feedback.announceInTwitchChat = v); y += row;
                addToggle(y, "Заголовок на экране", () -> config.feedback.showTitleInGame, v -> config.feedback.showTitleInGame = v); y += row;
                addToggle(y, "Строка над хотбаром", () -> config.feedback.showActionBar, v -> config.feedback.showActionBar = v); y += row;
                addToggle(y, "Звуки HUD (лента / донат / рулетка)", () -> config.ui.hudSounds, v -> config.ui.hudSounds = v);
                y += row;
            }
            case 7 -> {
                y += layout.s(16);
                addToggle(y, "§6DonateX", () -> config.donations.enabled, v -> config.donations.enabled = v); y += row;
                addIntRow(y, "Опрос API (сек, ≥5)", 5, 60, () -> config.donations.pollIntervalSeconds,
                        v -> config.donations.pollIntervalSeconds = v); y += row;
                addToggle(y, "Донаты без КД", () -> config.donations.bypassCooldown, v -> config.donations.bypassCooldown = v);
                y += row + layout.s(6);

                ensureDonationTiers();
                DonationTierResolver.sort(config.donations.tiers);
                for (int i = 0; i < config.donations.tiers.size(); i++) {
                    var tier = config.donations.tiers.get(i);
                    int idx = i;
                    addIntRow(y, "§6Tier " + (i + 1) + " §7· от (₽)", 1, 100_000,
                            () -> config.donations.tiers.get(idx).minAmount,
                            v -> config.donations.tiers.get(idx).minAmount = v);
                    y += row;

                    tierCommandBoxes.add(field(layout.contentL, y, fieldW, boxH, "cmd_" + i,
                            tierCommandDraft(i, tier.command)));
                    y += layout.s(22);
                    tierLabelBoxes.add(field(layout.contentL, y, fieldW, boxH, "lbl_" + i,
                            tierLabelDraft(i, tier.label)));
                    y += layout.s(22);

                    addToggle(y, "§d🎰 Рулетка (колесо на экране)", () -> config.donations.tiers.get(idx).wheel,
                            v -> config.donations.tiers.get(idx).wheel = v);
                    y += row;

                    if (config.donations.tiers.size() > 1) {
                        addPreset(y, "§c✕ Удалить уровень " + (i + 1), () -> {
                            pullTextFields();
                            if (config.donations.tiers.size() > 1) {
                                config.donations.tiers.remove(idx);
                                if (idx < draft.tierCommands.size()) draft.tierCommands.remove(idx);
                                if (idx < draft.tierLabels.size()) draft.tierLabels.remove(idx);
                                DonationTierResolver.sort(config.donations.tiers);
                                rebuildTab();
                            }
                        });
                        y += row;
                    }
                    y += layout.s(4);
                }

                addPreset(y, "§a+ Добавить уровень", () -> {
                    pullTextFields();
                    int next = config.donations.tiers.stream().mapToInt(t -> t.minAmount).max().orElse(50) + 50;
                    var tier = new TwitchCraftConfig.DonationTier(next, "lightning", "⚡ Новый уровень");
                    tier.wheel = false;
                    config.donations.tiers.add(tier);
                    draft.tierCommands.add("lightning");
                    draft.tierLabels.add("⚡ Новый уровень");
                    DonationTierResolver.sort(config.donations.tiers);
                    rebuildTab();
                });
                y += row + layout.s(8);

                donationTokenBox = field(layout.contentL, y, fieldW, boxH, "da_shared", mask(draft.sharedDonationToken));
                donationTokenBox.setMaxLength(1024);
                y += layout.s(38);
            }
            case 8 -> {
                y += layout.s(16);
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(16), "§7Плохие = только урон по HP / враги / TNT. Пранки не блокируются щитом.",
                        TwitchUi.TEXT_DIM));
                y += layout.s(20);

                y = addCommandGroup(y, row, "§c⚔ Урон", 0xFF_E7_4C_3C,
                        net.twitchcraft.commands.EvilCommands.damageAll(), "§c");
                y = addCommandGroup(y, row, "§e☺ Пранк", 0xFF_F1_C4_0F,
                        net.twitchcraft.commands.EvilCommands.prankAll(), "§e");
                y = addCommandGroup(y, row, "§a+ Добро", 0xFF_55_CC_55,
                        java.util.Arrays.asList(GoodCommands.all()), "§a");
                y += layout.s(6);
                categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                        layout.s(18), "§f📚 Свои команды", 0xFF_55_CC_FF));
                y += layout.s(22);
                addPreset(y, "§b⌨ Открыть конструктор команд §8(U)", () -> {
                    pullTextFields();
                    if (minecraft != null) MenuNavigation.openCommandCreator(this);
                }); y += row;
                addPreset(y, "§d📖 Справочник всех команд §8(R)", () -> {
                    pullTextFields();
                    if (minecraft != null) MenuNavigation.openRules(this);
                }); y += row;
            }
            default -> { }
        }

        contentHeight = Math.max(0, y - layout.contentT);
        applyScrollToFields();
        refreshScrollCache();
        addSearchBox(keepSearchFocus, savedSearchCursor);
        rebuilding = false;
    }

    private EditBox field(int x, int y, int w, int h, String id, String value) {
        EditBox box = new EditBox(font, x, y, w, h, Component.literal(id));
        box.setValue(value);
        box.setMaxLength(256);
        fieldBaseY.put(box, y);
        addRenderableWidget(box);
        return box;
    }

    private void applyScrollToFields() {
        if (layout == null || fieldBaseY.isEmpty()) return;
        int scroll = (int) contentScroll;
        if (scroll == lastFieldScroll) return;
        lastFieldScroll = scroll;

        int top = layout.contentT - 2;
        int bottom = layout.contentB + 2;
        for (var entry : fieldBaseY.entrySet()) {
            EditBox box = entry.getKey();
            int sy = entry.getValue() - scroll;
            box.setY(sy);
            boolean visible = sy + box.getHeight() >= top && sy <= bottom;
            if (visible == box.visible) continue;
            box.visible = visible;
            if (!visible && box.isFocused()) {
                box.setFocused(false);
            }
            box.active = visible;
        }
    }

    private void refreshScrollCache() {
        int scroll = (int) contentScroll;
        if (scroll == cachedContentScroll) return;
        cachedContentScroll = scroll;

        int top = layout.contentT;
        int bottom = layout.contentB;
        visibleCategories = filterVisible(categoryHeaders, scroll, top, bottom);
        visibleToggles = filterVisible(toggles, scroll, top, bottom);
        visibleThemes = filterVisible(themeHits, scroll, top, bottom);
        visiblePresets = filterVisible(presetHits, scroll, top, bottom);
        visibleIntControls = filterVisible(intControls, scroll, top, bottom);
    }

    private static <T> List<T> filterVisible(List<T> hits, int scroll, int top, int bottom) {
        if (hits.isEmpty()) return hits;
        List<T> out = new ArrayList<>();
        for (T hit : hits) {
            int y = hitY(hit) - scroll;
            int h = hitH(hit);
            if (y + h >= top && y <= bottom) {
                out.add(hit);
            }
        }
        return out;
    }

    private static int hitY(Object hit) {
        if (hit instanceof ToggleHit t) return t.y;
        if (hit instanceof IntHit t) return t.y;
        if (hit instanceof PresetHit t) return t.y;
        if (hit instanceof ThemeHit t) return t.y;
        if (hit instanceof CategoryHit t) return t.y;
        return 0;
    }

    private static int hitH(Object hit) {
        if (hit instanceof ToggleHit t) return t.h;
        if (hit instanceof IntHit t) return t.h;
        if (hit instanceof PresetHit t) return t.h;
        if (hit instanceof ThemeHit t) return t.h;
        if (hit instanceof CategoryHit t) return t.h;
        return 0;
    }

    private void renderVisibleFields(GuiGraphics g, int mouseX, int mouseY, float delta) {
        for (EditBox box : fieldBaseY.keySet()) {
            if (box.visible) {
                box.render(g, mouseX, mouseY, delta);
            }
        }
    }

    private void addToggle(int y, String label, Supplier<Boolean> get, java.util.function.Consumer<Boolean> set) {
        toggles.add(new ToggleHit(layout.contentL, y, layout.contentR - layout.contentL, layout.s(24), label, get, set));
    }

    private void addIntRow(int y, String label, int min, int max, Supplier<Integer> get, IntConsumer set) {
        intControls.add(new IntHit(layout.contentL, y, layout.contentR - layout.contentL, layout.s(24), label, min, max, get, set));
    }

    private int addCommandGroup(int y, int row, String title, int color,
                               java.util.List<ChatGameCommand> cmds, String nameColor) {
        categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                layout.s(18), title + " §8(" + cmds.size() + ")", color));
        y += layout.s(22);
        for (ChatGameCommand cmd : cmds) {
            y = addCommandControls(y, row, cmd, nameColor, true);
        }
        return y + layout.s(8);
    }

    private int addCommandControls(int y, int row, ChatGameCommand cmd, String nameColor, boolean includeCap) {
        var settings = config.getCommandSettings(cmd.getId(), cmd.getDefaultCooldown());
        String label = nameColor + "!" + cmd.getRussianName();
        addToggle(y, label + " — вкл", () -> settings.enabled, v -> settings.enabled = v);
        y += row;
        addIntRow(y, label + " · КД (сек)", 0, 300, () -> settings.cooldownSeconds, v -> settings.cooldownSeconds = v);
        y += row;
        if (includeCap) {
            addIntRow(y, label + " · макс/мин", 0, 30, () -> settings.maxUsesPerMinute, v -> settings.maxUsesPerMinute = v);
            y += row;
        }
        return y;
    }

    private int buildSearchResults(int y, int row, String query) {
        y += layout.s(6);
        categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                layout.s(16), "§dПоиск §f«" + query + "» §8— тумблер и КД", TwitchUi.PURPLE_LIGHT));
        y += layout.s(20);

        int found = 0;
        for (ChatGameCommand cmd : ChatGameCommand.values()) {
            if (cmd.getCategory() == ChatGameCommand.Category.MODERATOR) continue;
            if (!cmd.matchesQuery(query)) continue;
            String color = switch (net.twitchcraft.commands.EvilCommands.harm(cmd)) {
                case DAMAGE -> "§c";
                case PRANK -> "§e";
                case GOOD -> "§a";
                case MOD -> "§7";
            };
            y = addCommandControls(y, row, cmd, color, false);
            found++;
        }
        if (config.customCommands != null) {
            String q = query.toLowerCase(Locale.ROOT).replace("!", "").trim();
            for (CustomCommandDefinition def : config.customCommands) {
                String trigger = def.trigger != null ? def.trigger : "";
                String id = def.id != null ? def.id : "";
                if (!trigger.toLowerCase(Locale.ROOT).contains(q)
                        && !id.toLowerCase(Locale.ROOT).contains(q)) {
                    continue;
                }
                String label = "§b!" + def.displayName();
                addToggle(y, label + " — вкл", () -> def.enabled, v -> def.enabled = v);
                y += row;
                addIntRow(y, label + " · КД (сек)", 0, 300, () -> def.cooldownSeconds,
                        v -> def.cooldownSeconds = v);
                y += row;
                found++;
            }
        }
        if (found == 0) {
            categoryHeaders.add(new CategoryHit(layout.contentL, y, layout.contentR - layout.contentL,
                    layout.s(16), "§7Ничего. Попробуй русское имя или id: бомба, bomb, мина",
                    TwitchUi.TEXT_DIM));
            y += layout.s(20);
        }
        return y;
    }

    private void addSearchBox(boolean keepFocus, int cursor) {
        int w = Math.min(layout.s(118), Math.max(layout.s(72), layout.contentR - layout.contentL - layout.s(96)));
        int h = layout.s(14);
        int x = layout.closeX - w - layout.s(4);
        int y = layout.panelT + layout.pad + (layout.closeSize - h) / 2;
        searchBox = new EditBox(font, x, y, w, h, Component.literal("search"));
        searchBox.setHint(Component.literal("поиск: бомба"));
        searchBox.setMaxLength(40);
        searchBox.setValue(searchQuery == null ? "" : searchQuery);
        searchBox.setResponder(s -> {
            if (rebuilding) return;
            String next = s == null ? "" : s;
            if (next.equals(searchQuery)) return;
            searchQuery = next;
            rebuildTab();
        });
        addRenderableWidget(searchBox);
        if (keepFocus) {
            searchBox.setFocused(true);
            setFocused(searchBox);
            int len = searchBox.getValue().length();
            int pos = cursor < 0 ? len : Math.min(cursor, len);
            searchBox.setCursorPosition(pos);
        }
    }

    private void addPreset(int y, String label, Runnable apply) {
        addPreset(y, label, apply, true);
    }

    private void addPreset(int y, String label, Runnable apply, boolean requireOp) {
        presetHits.add(new PresetHit(layout.contentL, y, layout.contentR - layout.contentL, layout.s(22),
                label, apply, requireOp));
    }

    private void selectTab(int index) {
        pullTextFields();
        boolean searching = searchQuery != null && !searchQuery.isBlank();
        if (index == selectedTab && !searching) return;
        selectedTab = index;
        if (searching) searchQuery = "";
        init();
    }

    private void ensureDonationTiers() {
        if (config.donations.tiers.isEmpty()) {
            net.twitchcraft.config.ConfigDefaults.apply(config);
        }
    }

    private void pullTextFields() {
        if (twitchChannel != null) {
            draft.twitchChannel = twitchChannel.getValue().trim();
        }
        if (oauthBox != null && !oauthBox.getValue().contains("•")) {
            draft.oauthToken = oauthBox.getValue().trim();
        }
        if (daBox != null && !daBox.getValue().contains("•")) {
            draft.donationToken = daBox.getValue().trim();
        }
        if (featuredCmdBox != null && config.hud != null && config.hud.sidebarContent != null) {
            draft.featuredCommandId = featuredCmdBox.getValue().trim();
            config.hud.sidebarContent.featuredCommandId = draft.featuredCommandId.isBlank()
                    ? "lightning" : draft.featuredCommandId;
        }
        if (whitelistBox != null) {
            draft.whitelistRaw = whitelistBox.getValue();
        }
        if (moderatorsBox != null) {
            draft.moderatorsRaw = moderatorsBox.getValue();
        }
        if (donationTokenBox != null && !donationTokenBox.getValue().contains("•")) {
            draft.sharedDonationToken = donationTokenBox.getValue().trim();
        }
        for (int i = 0; i < tierCommandBoxes.size(); i++) {
            ensureTierDraftSize(i);
            draft.tierCommands.set(i, tierCommandBoxes.get(i).getValue().trim());
            draft.tierLabels.set(i, tierLabelBoxes.get(i).getValue().trim());
        }
        applyDraftToState();
    }

    private void loadDraftFromState() {
        net.twitchcraft.config.ConfigDefaults.apply(config);
        draft.twitchChannel = localProfile.twitchChannel != null ? localProfile.twitchChannel : "";
        draft.oauthToken = localProfile.oauthToken != null ? localProfile.oauthToken : "";
        draft.donationToken = localProfile.donationToken != null ? localProfile.donationToken : "";
        draft.featuredCommandId = config.scoreboard.featuredCommandId != null
                ? config.scoreboard.featuredCommandId : "lightning";
        draft.whitelistRaw = String.join(", ", config.security.whitelist);
        draft.moderatorsRaw = String.join(", ", config.security.moderators);
        draft.sharedDonationToken = config.donations.accessToken != null ? config.donations.accessToken : "";
        draft.tierCommands.clear();
        draft.tierLabels.clear();
        ensureDonationTiers();
        for (var tier : config.donations.tiers) {
            draft.tierCommands.add(tier.command != null ? tier.command : "lightning");
            draft.tierLabels.add(tier.label != null ? tier.label : "Донат");
        }
    }

    private void applyDraftToState() {
        localProfile.twitchChannel = draft.twitchChannel;
        localProfile.oauthToken = draft.oauthToken;
        localProfile.donationToken = draft.donationToken;
        config.scoreboard.featuredCommandId = draft.featuredCommandId.isBlank()
                ? "lightning" : draft.featuredCommandId;
        if (config.hud != null && config.hud.sidebarContent != null) {
            config.hud.sidebarContent.featuredCommandId = config.scoreboard.featuredCommandId;
        }
        config.security.whitelist = splitList(draft.whitelistRaw);
        config.security.moderators = splitList(draft.moderatorsRaw);
        if (!draft.donationToken.isBlank()) {
            config.donations.accessToken = draft.donationToken;
        } else if (!draft.sharedDonationToken.isBlank()) {
            config.donations.accessToken = draft.sharedDonationToken;
            if (localProfile.donationToken.isBlank()) {
                localProfile.donationToken = draft.sharedDonationToken;
            }
        }
        syncTierDraftsToConfig();
        DonationTierResolver.sort(config.donations.tiers);
    }

    private void syncTierDraftsToConfig() {
        ensureDonationTiers();
        for (int i = 0; i < config.donations.tiers.size(); i++) {
            ensureTierDraftSize(i);
            String cmd = draft.tierCommands.get(i).trim();
            String lbl = draft.tierLabels.get(i).trim();
            if (!cmd.isEmpty()) config.donations.tiers.get(i).command = cmd.toLowerCase(Locale.ROOT);
            if (!lbl.isEmpty()) config.donations.tiers.get(i).label = lbl;
        }
    }

    private void ensureTierDraftSize(int index) {
        while (draft.tierCommands.size() <= index) draft.tierCommands.add("lightning");
        while (draft.tierLabels.size() <= index) draft.tierLabels.add("Донат");
    }

    private String tierCommandDraft(int index, String fallback) {
        ensureTierDraftSize(index);
        String v = draft.tierCommands.get(index);
        return v != null && !v.isBlank() ? v : (fallback != null ? fallback : "lightning");
    }

    private String tierLabelDraft(int index, String fallback) {
        ensureTierDraftSize(index);
        String v = draft.tierLabels.get(index);
        return v != null && !v.isBlank() ? v : (fallback != null ? fallback : "Донат");
    }

    private static List<String> splitList(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) return out;
        for (String p : raw.split(",")) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private void applySidebarLive(boolean enabled) {
        config.scoreboard.enabled = enabled;
        ConfigManager.get().scoreboard.enabled = enabled;
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer() && mc.player != null) {
            var server = mc.getSingleplayerServer();
            ConfigManager.get().scoreboard.enabled = enabled;
            if (enabled) {
                StreamTickHandler.hud().refreshAll(server);
            } else {
                StreamTickHandler.hud().clear(server);
            }
        }
    }

    private void openHudCustomizer(int element) {
        pullTextFields();
        if (minecraft != null) {
            minecraft.setScreen(new HudCustomizerScreen(this, config, element));
        }
    }

    private void requestClose() {
        if (isDirty()) {
            MenuNavigation.confirmDiscard(this, () -> Minecraft.getInstance().setScreen(null));
        } else {
            super.onClose();
        }
    }

    @Override
    public void onClose() {
        if (isDirty()) {
            MenuNavigation.confirmDiscard(this, () -> super.onClose());
            return;
        }
        super.onClose();
    }

    private void snapshotDraft(TextDraft target, TextDraft source) {
        target.twitchChannel = source.twitchChannel;
        target.oauthToken = source.oauthToken;
        target.donationToken = source.donationToken;
        target.featuredCommandId = source.featuredCommandId;
        target.whitelistRaw = source.whitelistRaw;
        target.moderatorsRaw = source.moderatorsRaw;
        target.sharedDonationToken = source.sharedDonationToken;
        target.tierCommands.clear();
        target.tierCommands.addAll(source.tierCommands);
        target.tierLabels.clear();
        target.tierLabels.addAll(source.tierLabels);
    }

    private boolean isDirty() {
        pullTextFields();
        com.google.gson.Gson gson = new com.google.gson.Gson();
        if (!gson.toJson(config).equals(gson.toJson(initialConfig))) return true;
        TextDraft current = new TextDraft();
        snapshotDraft(current, draft);
        return !gson.toJson(current).equals(gson.toJson(initialDraft));
    }

    private void exportPartnerPack() {
        pullTextFields();
        Minecraft mc = Minecraft.getInstance();
        try {
            Path dir = PartnerPack.export(config);
            Util.getPlatform().openFile(dir.toFile());
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(
                        "§aПапка для напарника: §f" + dir.toAbsolutePath()), false);
                mc.player.displayClientMessage(Component.literal(
                        "§7Заархивируй и скинь. OAuth и DonateX внутрь не попадают."), false);
            }
        } catch (Exception e) {
            TwitchCraftMod.LOGGER.error("Partner pack export failed", e);
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(
                        "§cНе собрал папку: " + e.getMessage()), false);
            }
        }
    }

    private void exportPreset() {
        pullTextFields();
        String json = PresetIO.toJson(config);
        Minecraft mc = Minecraft.getInstance();
        if (mc.keyboardHandler != null) {
            mc.keyboardHandler.setClipboard(json);
        }
        var path = PresetIO.exportToFile(config);
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(
                    path != null
                            ? "§aПресет скопирован в буфер §7и сохранён: §f" + path.getFileName()
                            : "§aПресет скопирован в буфер обмена"
            ), false);
        }
    }

    private void importPreset() {
        if (!canEditSharedSettings()) return;
        Minecraft mc = Minecraft.getInstance();
        String clip = mc.keyboardHandler != null ? mc.keyboardHandler.getClipboard() : "";
        if (clip == null || clip.isBlank()) {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(
                        "§cБуфер пуст — скопируй JSON пресета и нажми снова"), false);
            }
            return;
        }
        String err = PresetIO.applyTo(config, clip);
        if (err != null) {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("§cИмпорт: " + err), false);
            }
            return;
        }
        loadDraftFromState();
        rebuildTab();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(
                    "§aПресет импортирован §7— нажми §aСохранить §7справа внизу"), false);
        }
    }

    private void requestPartnerPreset() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("§cЗайди в мир — пресет берётся с сервера"), false);
            }
            return;
        }
        StreamerActionClient.send("copypartner");
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§7Запрашиваю живой конфиг мира…"), true);
        }
    }

    public static void applyIncomingPreset(String json) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof SettingsScreen screen) {
            screen.applyPartnerJson(json);
            return;
        }
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(
                    "§eПресет мира пришёл — открой K → Баланс → Как у напарника"), false);
        }
    }

    private void applyPartnerJson(String json) {
        pullTextFields();
        String err = PresetIO.applyTo(config, json);
        Minecraft mc = Minecraft.getInstance();
        if (err != null) {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("§cПресет: " + err), false);
            }
            return;
        }
        loadDraftFromState();
        rebuildTab();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(canEditSharedSettings()
                    ? "§dКак у напарника §7— КД, HUD, шоу. Нажми §aСохранить"
                    : "§dЧерновик подставлен §7— без OP сохранится только HUD. КД пишет хост"), false);
        }
    }

    private void saveAndClose() {
        pullTextFields();
        net.twitchcraft.config.HudLegacySync.pushToLegacy(config);
        HudClientStore.replaceAndSave(config.hud);
        localProfile.setupWizardCompleted = true;
        LocalProfileStore.save(localProfile);
        ConfigManager.set(config);
        UiTheme.applyFromConfig();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !localProfile.minecraftName.isBlank()) {
            localProfile.minecraftName = mc.player.getGameProfile().name();
        }
        if (mc.hasSingleplayerServer() && mc.player != null) {
            var server = mc.getSingleplayerServer();
            var sp = server.getPlayerList().getPlayerByName(mc.player.getGameProfile().name());
            if (sp != null) {
                ConfigApply.apply(server, sp, config, localProfile);
            }
        } else if (mc.player != null) {
            ConfigClientNetworking.send(config, localProfile);
        } else {
            StreamerProfileMerge.upsertOffline(config, localProfile);
            ConfigManager.save();
        }
        initialConfig = copy(config);
        snapshotDraft(initialDraft, draft);
        super.onClose();
    }

    private boolean canEditSharedSettings() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return true;
        return mc.player.hasPermissions(2);
    }

    private void drawStreamerModeBanner(GuiGraphics g) {
        if (canEditSharedSettings()) return;
        int y = layout.panelT + layout.pad + layout.s(38);
        int w = layout.contentR - layout.contentL;
        TwitchUi.roundedPanel(g, layout.contentL, y, layout.contentL + w, y + layout.s(14), 0xCC_2A_1A_3A);
        g.fill(layout.contentL, y, layout.contentL + w, y + 1, TwitchUi.PURPLE);
        g.drawString(font, "§7Режим стримера §8— §fсохраняются Twitch/DA токены и тема UI",
                layout.contentL + 8, y + 3, TwitchUi.TEXT_DIM, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (layout == null) {
            recomputeLayout();
        }

        int maxScroll = Math.max(0, contentHeight - (layout.contentB - layout.contentT));
        contentScroll = Mth.clamp(contentScroll, 0, maxScroll);
        applyScrollToFields();
        refreshScrollCache();

        Tab tab = TABS[selectedTab];
        TwitchUi.drawDimBackground(g, width, height);
        TwitchUi.drawSettingsFrame(g, layout.panelL, layout.panelT, layout.panelR, layout.panelB,
                layout.sidebarW, tab.color());
        int headerY = layout.panelT + layout.pad + layout.s(4);
        int logoSize = layout.s(16);
        SmirtyLogo.drawWithGlow(g, layout.panelL + layout.pad + 2, headerY + 1, logoSize, tab.color());
        g.drawString(font, "§d§lНастройки", layout.panelL + layout.pad + logoSize + layout.s(6), headerY, TwitchUi.TEXT, false);
        g.drawString(font, "§8v" + ModVersion.friendly() + " §7· §d" + TwitchCraftMod.AUTHOR,
                layout.panelL + layout.pad + logoSize + layout.s(6), headerY + layout.s(11), TwitchUi.TEXT_DIM, false);
        g.drawString(font, tab.title(), layout.contentL, layout.panelT + layout.pad + layout.s(26), tab.color(), false);
        String subtitle = (searchQuery != null && !searchQuery.isBlank())
                ? "поиск по командам · очисти поле чтобы вернуться"
                : MenuHints.tabSubtitle(selectedTab);
        if (subtitle != null && !subtitle.isBlank()) {
            g.drawString(font, "§8" + subtitle, layout.contentL, layout.panelT + layout.pad + layout.s(36),
                    TwitchUi.TEXT_DIM, false);
        }
        TwitchUi.drawTabAccentLine(g, layout.contentL, layout.panelT + layout.pad + layout.s(44),
                layout.contentR - layout.contentL, tab.color());
        drawStreamerModeBanner(g);

        drawSidebar(g, mouseX, mouseY);
        drawFooter(g, mouseX, mouseY);

        g.enableScissor(layout.contentL, layout.contentT, layout.contentR, layout.contentB);
        linkHits.clear();
        drawScrollableContent(g, mouseX, mouseY);
        drawFieldLabels(g, mouseX, mouseY);
        renderVisibleFields(g, mouseX, mouseY, delta);
        g.disableScissor();

        closeHover = over(mouseX, mouseY, layout.closeX, layout.closeY, layout.closeSize, layout.closeSize);
        saveHover = over(mouseX, mouseY, saveX, saveY, saveW, saveH);
        TwitchUi.drawCloseButton(g, font, layout.closeX, layout.closeY, layout.closeSize, closeHover);
        TwitchUi.drawSaveButton(g, font, saveX, saveY, saveW, saveH, saveHover);
        if (searchBox != null) {
            searchBox.render(g, mouseX, mouseY, delta);
        }
    }

    /** Подсказки вкладки — скроллятся вместе с контентом. */
    private void drawScrollableTabHeaders(GuiGraphics g, int scroll) {
        int base = layout.contentT - scroll;
        if (selectedTab == 1 && mcName != null) {
            if (base + layout.s(48) >= layout.contentT && base <= layout.contentB) {
                TwitchUi.drawHint(g, font, layout.contentL, base + layout.s(2), TwitchSetupHints.INTRO_1);
                TwitchUi.drawHint(g, font, layout.contentL, base + layout.s(12), TwitchSetupHints.INTRO_2);
                drawTwitchStatusBadge(g, base + layout.s(20));
                TwitchUi.drawHint(g, font, layout.contentL, base + layout.s(34), TwitchSetupHints.EVENTS_HINT);
            }
        }
        if (selectedTab == 3) {
            int hy = base + layout.s(2);
            if (hy + layout.s(12) >= layout.contentT && hy <= layout.contentB) {
                TwitchUi.drawLabel(g, font, layout.contentL, hy,
                        "§7Тема меняет цвет sidebar, HUD, правил и настроек");
            }
            if (hudMiniPreviewY > 0) {
                int py = hudMiniPreviewY - scroll;
                if (py + layout.s(52) >= layout.contentT && py <= layout.contentB) {
                    TwitchUi.drawMiniPreview(g, font, layout.contentL, py,
                            layout.contentR - layout.contentL, UiTheme.get());
                }
            }
        }
        if (selectedTab == 4) {
            int hy = base + layout.s(2);
            if (hy + layout.s(12) >= layout.contentT && hy <= layout.contentB) {
                TwitchUi.drawLabel(g, font, layout.contentL, hy,
                        "§7Пресет баланса · §8" + AudiencePresets.label(config.audiencePreset));
            }
        }
        if (selectedTab == 7) {
            int hy = base + layout.s(2);
            if (hy + layout.s(12) >= layout.contentT && hy <= layout.contentB) {
                TwitchUi.drawLabel(g, font, layout.contentL, hy,
                        "§7Токен DonateX на вкладке Twitch · §8id: lightning, goldrush, disco, treasure, raid…");
            }
        }
        if (selectedTab == 8 && (searchQuery == null || searchQuery.isBlank())) {
            int hy = base + layout.s(2);
            if (hy + layout.s(12) >= layout.contentT && hy <= layout.contentB) {
                TwitchUi.drawLabel(g, font, layout.contentL, hy,
                        "§7Встроенные !команды · §bU §7— создать свои · §8" + (ChatGameCommand.values().length - 3) + " шт.");
            }
        }
    }

    private void drawTwitchStatusBadge(GuiGraphics g, int y) {
        String line = SidebarOverlay.twitchSettingsLine();
        int border = SidebarOverlay.isTwitchConnected() ? 0xFF_55_FF_88 : 0xFF_FF_66_66;
        int bg = SidebarOverlay.isTwitchConnected() ? 0xCC_1A_3D_28 : 0xCC_3D_1A_1A;
        int bw = Math.min(layout.contentR - layout.contentL, font.width(TwitchUi.stripFormat(line)) + 20);
        TwitchUi.roundedPanel(g, layout.contentL, y, layout.contentL + bw, y + layout.s(14), bg);
        g.fill(layout.contentL, y, layout.contentL + bw, y + 1, border);
        g.drawString(font, line, layout.contentL + 10, y + 3, TwitchUi.TEXT, false);
    }

    private void drawFieldLabels(GuiGraphics g, int mouseX, int mouseY) {
        int scroll = (int) contentScroll;
        if (selectedTab == 1 && mcName != null) {
            drawFieldGroup(g, mcName, scroll, TwitchSetupHints.MC_LABEL, TwitchSetupHints.MC_HINT, mouseX, mouseY);
            drawFieldGroup(g, twitchChannel, scroll, TwitchSetupHints.CH_LABEL, TwitchSetupHints.CH_HINT, mouseX, mouseY);
            drawFieldGroupLink(g, oauthBox, scroll, TwitchSetupHints.OAUTH_LABEL, TwitchSetupHints.OAUTH_HINT,
                    TwitchSetupHints.OAUTH_HINT2, mouseX, mouseY);
            drawFieldGroupLink(g, daBox, scroll, TwitchSetupHints.DA_LABEL, TwitchSetupHints.DA_HINT,
                    TwitchSetupHints.DA_HINT2, mouseX, mouseY);
        }
        if (selectedTab == 3 && featuredCmdBox != null) {
            drawFieldLabel(g, featuredCmdBox, scroll, "ID любимой (lightning)");
        }
        if (selectedTab == 5) {
            drawFieldLabel(g, whitelistBox, scroll, "Белый список (ник1, ник2)");
            drawFieldLabel(g, moderatorsBox, scroll, "Модераторы Twitch");
        }
        if (selectedTab == 7) {
            for (int i = 0; i < tierCommandBoxes.size(); i++) {
                drawFieldLabel(g, tierCommandBoxes.get(i), scroll, "§7Команда (уровень " + (i + 1) + ")");
                drawFieldLabel(g, tierLabelBoxes.get(i), scroll, "§7Название в алерте");
            }
            drawFieldLabel(g, donationTokenBox, scroll, "§7Общий токен DonateX (если один на двоих) · §8или вкладка Twitch");
        }
    }

    private void drawFieldGroup(GuiGraphics g, EditBox box, int scroll, String label, String hint,
                                int mouseX, int mouseY) {
        if (box == null) return;
        Integer base = fieldBaseY.get(box);
        if (base == null) return;
        int fy = base - scroll;
        if (fy + 30 < layout.contentT || fy - 14 > layout.contentB) return;
        g.drawString(font, label, layout.contentL, fy - layout.s(12), TwitchUi.TEXT, false);
        TwitchUi.drawHint(g, font, layout.contentL, fy + layout.s(20), hint);
    }

    private void drawFieldGroupLink(GuiGraphics g, EditBox box, int scroll, String label,
                                    TwitchSetupHints.HintWithLink hint, String secondLine,
                                    int mouseX, int mouseY) {
        if (box == null) return;
        Integer base = fieldBaseY.get(box);
        if (base == null) return;
        int fy = base - scroll;
        if (fy + 42 < layout.contentT || fy - 14 > layout.contentB) return;
        g.drawString(font, label, layout.contentL, fy - layout.s(12), TwitchUi.TEXT, false);
        TwitchUi.drawHintLink(g, font, layout.contentL, fy + layout.s(20), hint, linkHits, mouseX, mouseY);
        if (secondLine != null && !secondLine.isBlank()) {
            TwitchUi.drawHint(g, font, layout.contentL, fy + layout.s(32), secondLine);
        }
    }

    private void drawFieldGroup(GuiGraphics g, EditBox box, int scroll, String label,
                                TwitchSetupHints.HintWithLink hint, int mouseX, int mouseY) {
        drawFieldGroupLink(g, box, scroll, label, hint, null, mouseX, mouseY);
    }

    private void drawFieldLabel(GuiGraphics g, EditBox box, int scroll, String label) {
        if (box == null) return;
        Integer base = fieldBaseY.get(box);
        if (base == null) return;
        int fy = base - scroll;
        if (fy < layout.contentT - 14 || fy > layout.contentB) return;
        TwitchUi.drawLabel(g, font, layout.contentL, fy - layout.s(12), label);
    }

    private void drawScrollableContent(GuiGraphics g, int mx, int my) {
        int scroll = (int) contentScroll;
        int btnH = layout.s(18);

        drawScrollableTabHeaders(g, scroll);

        for (CategoryHit ch : visibleCategories) {
            int dy = ch.y - scroll;
            TwitchUi.drawCategoryHeader(g, font, ch.x, dy, ch.w, ch.label, ch.color);
        }

        for (ToggleHit t : visibleToggles) {
            int dy = t.y - scroll;
            boolean hover = over(mx, my, t.x, dy, t.w, t.h);
            TwitchUi.drawToggleRow(g, font, t.x, dy, t.w, t.label, t.get.get(), hover);
        }

        for (ThemeHit th : visibleThemes) {
            int dy = th.y - scroll;
            boolean selected = UiTheme.Id.fromKey(config.ui.theme) == th.theme;
            boolean hover = over(mx, my, th.x, dy, th.w, th.h);
            TwitchUi.drawThemeButton(g, font, th.x, dy, th.w, th.h, th.theme.label(),
                    UiTheme.palette(th.theme).accent(), selected, hover);
        }

        for (PresetHit p : visiblePresets) {
            int dy = p.y - scroll;
            boolean hover = over(mx, my, p.x, dy, p.w, p.h);
            TwitchUi.drawSmallButton(g, font, p.x, dy, p.w, p.h, p.label,
                    hover && (!p.requireOp || canEditSharedSettings()));
        }

        for (IntHit ic : visibleIntControls) {
            int dy = ic.y - scroll;
            TwitchUi.drawIntRow(g, font, ic.x, dy, ic.w, ic.label, ic.get.get());
            int btnW = layout.s(18);
            int minusX = ic.x + ic.w - layout.s(44);
            int plusX = ic.x + ic.w - layout.s(22);
            int btnY = dy + layout.s(3);
            TwitchUi.drawSmallButton(g, font, minusX, btnY, btnW, btnH, "−", over(mx, my, minusX, btnY, btnW, btnH));
            TwitchUi.drawSmallButton(g, font, plusX, btnY, btnW, btnH, "+", over(mx, my, plusX, btnY, btnW, btnH));
        }

        if (maxScrollHint() > 0) {
            TwitchUi.drawScrollbar(g, layout.contentR, layout.contentT, layout.contentB,
                    contentScroll, maxScrollHint(), layout.contentB - layout.contentT, contentHeight);
        }
    }

    private int maxScrollHint() {
        return Math.max(0, contentHeight - (layout.contentB - layout.contentT));
    }

    private void drawFooter(GuiGraphics g, int mx, int my) {
        int fy = layout.panelB - layout.footerH;
        TwitchUi.fillGradientH(g, layout.panelL + 1, fy, layout.panelR - 1, layout.panelB - 1, 0xF0_12_12_16, 0xF0_0E_0E_10);
        zoomOutHover = over(mx, my, layout.zoomOutX, layout.zoomOutY, layout.zoomBtnW, layout.zoomBtnH);
        zoomInHover = over(mx, my, layout.zoomInX, layout.zoomOutY, layout.zoomBtnW, layout.zoomBtnH);
        TwitchMenuLayout.drawZoomControls(layout, g, font, zoomOutHover, zoomInHover);
        g.drawString(font, MenuHints.SETTINGS_FOOTER,
                layout.panelL + layout.pad + layout.s(56), fy + layout.s(8), TwitchUi.TEXT_DIM, false);
        if (isDirty()) {
            g.drawString(font, "§c● несохранено", saveX - layout.s(72), fy + layout.s(8), 0xFF_FF_66_66, false);
        }
        String creditPlain = "by " + TwitchCraftMod.AUTHOR;
        g.drawString(font, "§7by §d" + TwitchCraftMod.AUTHOR,
                layout.panelR - layout.pad - font.width(creditPlain), fy + layout.s(8), TwitchUi.TEXT_DIM, false);
    }

    private void drawSidebar(GuiGraphics g, int mx, int my) {
        g.fill(layout.panelL + layout.sidebarW, layout.panelT + layout.headerH,
                layout.panelL + layout.sidebarW + 1, layout.panelB - layout.footerH, TwitchUi.BORDER);
        hoveredTab = -1;

        int totalTabsH = TABS.length * (layout.tabH + layout.tabGap) - layout.tabGap;
        int sidebarH = layout.sidebarB - layout.sidebarT;
        int maxSidebarScroll = Math.max(0, totalTabsH - sidebarH);
        sidebarScroll = Mth.clamp(sidebarScroll, 0, maxSidebarScroll);

        g.enableScissor(layout.panelL + layout.pad, layout.sidebarT, layout.panelL + layout.sidebarW, layout.sidebarB);
        int tabY = layout.sidebarT - (int) sidebarScroll;
        int tabW = layout.sidebarW - layout.pad * 2;
        for (int i = 0; i < TABS.length; i++) {
            Tab t = TABS[i];
            boolean active = i == selectedTab;
            if (tabY + layout.tabH >= layout.sidebarT && tabY <= layout.sidebarB) {
                boolean hover = over(mx, my, layout.panelL + layout.pad, tabY, tabW, layout.tabH);
                if (hover) hoveredTab = i;
                TwitchUi.drawTab(g, font, layout.panelL + layout.pad, tabY, tabW, layout.tabH,
                        t.icon(), t.title(), active, hover, t.color());
            }
            tabY += layout.tabH + layout.tabGap;
        }
        g.disableScissor();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x();
        double my = event.y();

        if (closeHover) { requestClose(); return true; }
        if (saveHover) { saveAndClose(); return true; }
        if (TwitchMenuLayout.handleZoomClick(layout, mx, my)) { recomputeLayout(); rebuildTab(); return true; }
        if (hoveredTab >= 0) { selectTab(hoveredTab); return true; }

        if (my < layout.contentT || my > layout.contentB) {
            return super.mouseClicked(event, doubleClick);
        }

        for (TwitchUi.LinkHit link : linkHits) {
            if (over(mx, my, link.x(), link.y(), link.w(), link.h())) {
                TwitchUi.openLink(link.url());
                return true;
            }
        }

        applyScrollToFields();
        int scroll = (int) contentScroll;
        int btnW = layout.s(18);
        int btnH = layout.s(18);

        for (ToggleHit t : toggles) {
            int dy = t.y - scroll;
            if (over(mx, my, t.x, dy, t.w, t.h)) {
                if (!canEditSharedSettings()) return true;
                t.set.accept(!t.get.get());
                return true;
            }
        }

        for (ThemeHit th : themeHits) {
            int dy = th.y - scroll;
            if (over(mx, my, th.x, dy, th.w, th.h)) {
                config.ui.theme = th.theme.key();
                UiTheme.apply(th.theme);
                return true;
            }
        }

        for (PresetHit p : presetHits) {
            int dy = p.y - scroll;
            if (over(mx, my, p.x, dy, p.w, p.h)) {
                if (p.requireOp && !canEditSharedSettings()) return true;
                p.apply.run();
                rebuildTab();
                return true;
            }
        }

        for (IntHit ic : intControls) {
            int dy = ic.y - scroll;
            int minusX = ic.x + ic.w - layout.s(44);
            int plusX = ic.x + ic.w - layout.s(22);
            int btnY = dy + layout.s(3);
            int step = TwitchUi.intAdjustStep();
            if (over(mx, my, minusX, btnY, btnW, btnH)) {
                if (!canEditSharedSettings()) return true;
                ic.set.accept(TwitchUi.adjustInt(ic.get.get(), -step, ic.min, ic.max));
                return true;
            }
            if (over(mx, my, plusX, btnY, btnW, btnH)) {
                if (!canEditSharedSettings()) return true;
                ic.set.accept(TwitchUi.adjustInt(ic.get.get(), step, ic.min, ic.max));
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (TwitchMenuLayout.handleZoomScroll(layout, mouseX, mouseY, scrollY)) {
            recomputeLayout();
            rebuildTab();
            return true;
        }
        if (mouseX >= layout.panelL && mouseX <= layout.panelL + layout.sidebarW
                && mouseY >= layout.sidebarT && mouseY <= layout.sidebarB) {
            int totalTabsH = TABS.length * (layout.tabH + layout.tabGap) - layout.tabGap;
            int maxSidebarScroll = Math.max(0, totalTabsH - (layout.sidebarB - layout.sidebarT));
            sidebarScroll = Mth.clamp(sidebarScroll - scrollY * layout.s(16), 0, maxSidebarScroll);
            return true;
        }
        if (mouseX >= layout.contentL && mouseX <= layout.contentR
                && mouseY >= layout.contentT && mouseY <= layout.contentB) {
            contentScroll = Mth.clamp(contentScroll - scrollY * layout.s(20), 0, maxScrollHint());
            lastFieldScroll = Integer.MIN_VALUE;
            cachedContentScroll = Integer.MIN_VALUE;
            applyScrollToFields();
            refreshScrollCache();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String mask(String token) {
        if (token == null || token.isBlank()) return "";
        return "••••••••";
    }

    private static String deathDisplayLabel(String mode) {
        if (mode == null) return "Плашка";
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "actionbar" -> "Над хотбаром";
            case "title" -> "Заголовок";
            case "sidebar" -> "Сайдбар";
            case "all" -> "Везде";
            default -> "Плашка";
        };
    }

    private static String cycleDeathDisplay(String mode) {
        String[] modes = {"overlay", "actionbar", "title", "sidebar", "all"};
        if (mode == null || mode.isBlank()) return modes[0];
        String cur = mode.toLowerCase(Locale.ROOT);
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(cur)) return modes[(i + 1) % modes.length];
        }
        return modes[0];
    }

    private static TwitchCraftConfig copy(TwitchCraftConfig src) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        return gson.fromJson(gson.toJson(src), TwitchCraftConfig.class);
    }
}
