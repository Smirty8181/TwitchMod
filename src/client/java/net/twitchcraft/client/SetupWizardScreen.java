package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.AudiencePresets;
import net.twitchcraft.config.ConfigApply;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.LocalProfileStore;
import net.twitchcraft.config.StreamerProfile;
import net.twitchcraft.config.StreamerProfileMerge;
import net.twitchcraft.config.TwitchCraftConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Мастер первой настройки: канал → OAuth → пресет аудитории → (опц.) DA → сохранить.
 * Не требует правки JSON вручную.
 */
public class SetupWizardScreen extends Screen {
    private static final String[] STEP_TITLES = {
            "Привет!",
            "Twitch",
            "Аудитория",
            "Донаты",
            "Готово"
    };

    private final Screen returnTo;
    private final TwitchCraftConfig config;
    private final StreamerProfile profile;
    private int step;
    private EditBox channelBox;
    private EditBox oauthBox;
    private EditBox daBox;
    private String statusMsg = "";
    private int statusColor = TwitchUi.TEXT_DIM;

    private final List<Hit> hits = new ArrayList<>();
    private record Hit(int x, int y, int w, int h, Runnable action) {}

    public SetupWizardScreen() {
        this(null);
    }

    public SetupWizardScreen(Screen returnTo) {
        super(Component.literal("TwitchCraft Setup"));
        this.returnTo = returnTo;
        this.config = copy(ConfigManager.get());
        net.twitchcraft.config.HudLegacySync.ensure(this.config);
        this.profile = LocalProfileStore.load();
        resolveDefaults();
    }

    private void resolveDefaults() {
        Minecraft mc = Minecraft.getInstance();
        String sessionName = mc.player != null ? mc.player.getGameProfile().name()
                : (mc.getUser() != null ? mc.getUser().getName() : "");
        if (profile.minecraftName == null || profile.minecraftName.isBlank()) {
            profile.minecraftName = sessionName;
        }
        if (profile.twitchChannel == null) profile.twitchChannel = "";
        if (profile.oauthToken == null) profile.oauthToken = "";
        if (profile.donationToken == null) profile.donationToken = "";
    }

    @Override
    protected void init() {
        clearWidgets();
        hits.clear();
        channelBox = oauthBox = daBox = null;

        int panelW = Math.min(420, width - 40);
        int panelH = Math.min(280, height - 40);
        int panelL = (width - panelW) / 2;
        int panelT = (height - panelH) / 2;
        int fieldW = panelW - 48;
        int fieldH = 18;
        int x = panelL + 24;

        if (step == 1) {
            // Labels at panelT+56.. ; channel field under CH_HINT
            channelBox = new EditBox(font, x, panelT + 96, fieldW, fieldH, Component.literal("channel"));
            channelBox.setValue(profile.twitchChannel != null ? profile.twitchChannel : "");
            channelBox.setMaxLength(64);
            channelBox.setHint(Component.literal(TwitchSetupHints.CH_PLACEHOLDER));
            addRenderableWidget(channelBox);

            oauthBox = new EditBox(font, x, panelT + 160, fieldW, fieldH, Component.literal("oauth"));
            oauthBox.setValue(maskToken(profile.oauthToken));
            oauthBox.setMaxLength(128);
            oauthBox.setHint(Component.literal(TwitchSetupHints.OAUTH_PLACEHOLDER));
            addRenderableWidget(oauthBox);
        } else if (step == 3) {
            daBox = new EditBox(font, x, panelT + 150, fieldW, fieldH, Component.literal("da"));
            daBox.setValue(maskToken(profile.donationToken));
            daBox.setMaxLength(1024);
            daBox.setHint(Component.literal(TwitchSetupHints.DA_PLACEHOLDER));
            addRenderableWidget(daBox);
        }
    }

    private void pullFields() {
        if (channelBox != null) {
            profile.twitchChannel = channelBox.getValue().trim();
        }
        if (oauthBox != null && !oauthBox.getValue().contains("•")) {
            profile.oauthToken = oauthBox.getValue().trim();
        }
        if (daBox != null && !daBox.getValue().contains("•")) {
            profile.donationToken = daBox.getValue().trim();
        }
    }

    private boolean canAdvanceFromTwitch() {
        pullFields();
        return profile.twitchChannel != null && !profile.twitchChannel.isBlank()
                && profile.oauthToken != null && !profile.oauthToken.isBlank();
    }

    private void next() {
        if (step == 1 && !canAdvanceFromTwitch()) {
            statusMsg = "Укажи канал и OAuth-токен";
            statusColor = 0xFF_FF_66_66;
            return;
        }
        pullFields();
        if (step < STEP_TITLES.length - 1) {
            step++;
            init();
        }
    }

    private void back() {
        pullFields();
        if (step > 0) {
            step--;
            init();
        } else {
            closeWizard(false);
        }
    }

    private void finish(boolean connect) {
        pullFields();
        if (!canAdvanceFromTwitch()) {
            step = 1;
            init();
            statusMsg = "Сначала заполни Twitch (канал + OAuth)";
            statusColor = 0xFF_FF_66_66;
            return;
        }
        if (config.audiencePreset == null || config.audiencePreset.isBlank()) {
            AudiencePresets.apply(config, AudiencePresets.MEDIUM);
        }
        if (profile.hasDonations()) {
            config.donations.enabled = true;
            config.donations.accessToken = profile.donationToken;
        }
        config.twitch.autoConnect = connect || config.twitch.autoConnect;
        profile.setupWizardCompleted = true;

        LocalProfileStore.save(profile);
        ConfigManager.set(config);
        UiTheme.applyFromConfig();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !profile.minecraftName.isBlank()) {
            profile.minecraftName = mc.player.getGameProfile().name();
            LocalProfileStore.save(profile);
        }
        if (mc.hasSingleplayerServer() && mc.player != null) {
            var server = mc.getSingleplayerServer();
            var sp = server.getPlayerList().getPlayerByName(mc.player.getGameProfile().name());
            if (sp != null) {
                ConfigApply.apply(server, sp, config, profile);
                if (connect) {
                    TwitchCraftMod.getConnectionManager().connect(server);
                }
            }
        } else if (mc.player != null) {
            ConfigClientNetworking.send(config, profile);
        } else {
            StreamerProfileMerge.upsertOffline(config, profile);
            ConfigManager.save();
        }

        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(
                    connect
                            ? "§a§lTwitchCraft §7— настройка сохранена. Подключаюсь к Twitch…"
                            : "§a§lTwitchCraft §7— настройка сохранена. §f/twitchcraft connect §7когда будешь готов."
            ), false);
        }
        closeWizard(true);
    }

    private void closeWizard(boolean completed) {
        Minecraft mc = Minecraft.getInstance();
        if (!completed && profile.setupWizardCompleted) {
            // already done earlier
        }
        if (returnTo != null) {
            mc.setScreen(returnTo);
        } else {
            mc.setScreen(null);
        }
    }

    private void skipWizard() {
        profile.setupWizardCompleted = true;
        LocalProfileStore.save(profile);
        closeWizard(true);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(
                    "§7Мастер пропущен. Открой §fK §7или §f🚀 Старт §7в настройках позже."), false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        hits.clear();
        TwitchUi.drawDimBackground(g, width, height);

        int panelW = Math.min(420, width - 40);
        int panelH = Math.min(280, height - 40);
        int panelL = (width - panelW) / 2;
        int panelT = (height - panelH) / 2;
        int panelR = panelL + panelW;
        int panelB = panelT + panelH;

        TwitchUi.glassPanel(g, panelL, panelT, panelR, panelB);
        TwitchUi.accentBar(g, panelL + 1, panelT + 1, panelR - 1, TwitchUi.PURPLE);

        g.drawString(font, "§d§lМастер настройки", panelL + 16, panelT + 12, TwitchUi.TEXT, false);
        g.drawString(font, "§8шаг " + (step + 1) + "/" + STEP_TITLES.length + " · " + STEP_TITLES[step],
                panelL + 16, panelT + 24, TwitchUi.TEXT_DIM, false);

        // progress dots
        int dotsY = panelT + 40;
        int total = STEP_TITLES.length;
        int dotGap = 14;
        int dotsW = total * dotGap;
        int dotsX = panelL + (panelW - dotsW) / 2;
        for (int i = 0; i < total; i++) {
            int cx = dotsX + i * dotGap + 4;
            int color = i <= step ? TwitchUi.PURPLE : 0xFF_40_38_50;
            g.fill(cx, dotsY, cx + 8, dotsY + 4, color);
        }

        int contentX = panelL + 24;
        int contentY = panelT + 56;
        int contentW = panelW - 48;

        switch (step) {
            case 0 -> renderWelcome(g, contentX, contentY, contentW);
            case 1 -> renderTwitch(g, contentX, contentY, contentW, mouseX, mouseY);
            case 2 -> renderAudience(g, contentX, contentY, contentW, mouseX, mouseY);
            case 3 -> renderDonations(g, contentX, contentY, contentW, mouseX, mouseY);
            case 4 -> renderDone(g, contentX, contentY, contentW, mouseX, mouseY);
        }

        if (!statusMsg.isBlank()) {
            g.drawString(font, statusMsg, contentX, panelB - 52, statusColor, false);
        }

        // footer buttons
        int btnH = 18;
        int btnY = panelB - 28;
        int backW = 70;
        int nextW = 90;

        boolean backHover = over(mouseX, mouseY, panelL + 16, btnY, backW, btnH);
        TwitchUi.roundedPanel(g, panelL + 16, btnY, panelL + 16 + backW, btnY + btnH,
                backHover ? 0xFF_3A_30_48 : 0xFF_28_22_36);
        g.drawCenteredString(font, step == 0 ? "Позже" : "Назад",
                panelL + 16 + backW / 2, btnY + 5, TwitchUi.TEXT);
        hits.add(new Hit(panelL + 16, btnY, backW, btnH, this::back));

        if (step < 4) {
            int nx = panelR - 16 - nextW;
            boolean nextHover = over(mouseX, mouseY, nx, btnY, nextW, btnH);
            TwitchUi.roundedPanel(g, nx, btnY, nx + nextW, btnY + btnH,
                    nextHover ? TwitchUi.SAVE_BTN_HOVER : TwitchUi.SAVE_BTN);
            g.drawCenteredString(font, "§lДалее", nx + nextW / 2, btnY + 5, 0xFF_FF_FF_FF);
            hits.add(new Hit(nx, btnY, nextW, btnH, this::next));
        }

        if (step == 0) {
            int skipW = 100;
            int skipX = panelL + (panelW - skipW) / 2;
            boolean skipHover = over(mouseX, mouseY, skipX, btnY, skipW, btnH);
            g.drawCenteredString(font, skipHover ? "§7Пропустить" : "§8Пропустить",
                    skipX + skipW / 2, btnY + 5, TwitchUi.TEXT_DIM);
            hits.add(new Hit(skipX, btnY, skipW, btnH, this::skipWizard));
        }

        super.render(g, mouseX, mouseY, delta);
    }

    private void renderWelcome(GuiGraphics g, int x, int y, int w) {
        g.drawString(font, "§fTwitchCraft за 1 минуту", x, y, TwitchUi.TEXT, false);
        y += 16;
        TwitchUi.drawWrappedText(g, font, x, y, w,
                "§7Зрители пишут §f!команды§7 — на стриме эффекты. Токены §dтолько на этом ПК§7. "
                        + "§fK§7 настройки · §fR§7 правила · §fI§7 тесты · §fU§7 команды · §fP§7 статистика. Physics: Esc → Настройки.",
                TwitchUi.TEXT_DIM);
        y += 48;
        g.drawString(font, "§a1. §7Свой канал + свой OAuth", x, y, TwitchUi.TEXT_DIM, false);
        g.drawString(font, "§a2. §7Размер аудитории", x, y + 12, TwitchUi.TEXT_DIM, false);
        g.drawString(font, "§a3. §7DonateX §8(по желанию)", x, y + 24, TwitchUi.TEXT_DIM, false);
    }

    private void renderTwitch(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        // y starts at panelT+56 — keep in sync with init() field Y
        String mc = profile.minecraftName != null ? profile.minecraftName : "?";
        g.drawString(font, "§8Minecraft: §f" + mc, x, y, TwitchUi.TEXT_DIM, false);
        g.drawString(font, TwitchSetupHints.CH_LABEL, x, y + 14, TwitchUi.TEXT, false);
        g.drawString(font, TwitchSetupHints.CH_HINT, x, y + 26, TwitchUi.TEXT_DIM, false);
        // channelBox @ panelT+96 (= y+40)
        g.drawString(font, TwitchSetupHints.OAUTH_LABEL, x, y + 78, TwitchUi.TEXT, false);
        int linkY = y + 90;
        g.drawString(font, "§7Получить: ", x, linkY, TwitchUi.TEXT_DIM, false);
        int linkX = x + font.width("§7Получить: ");
        String link = "§n§btwitchapps.com/tmi";
        g.drawString(font, link, linkX, linkY, TwitchUi.ACCENT_ALT, false);
        hits.add(new Hit(linkX, linkY, font.width(link), 10,
                () -> TwitchUi.openLink(TwitchSetupHints.OAUTH_URL)));
        // oauthBox @ panelT+160 (= y+104)
    }

    private void renderAudience(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        g.drawString(font, "§fСколько обычно зрителей?", x, y, TwitchUi.TEXT, false);
        y += 16;
        g.drawString(font, "§7Это настроит КД и лимиты спавна.", x, y, TwitchUi.TEXT_DIM, false);
        y += 20;

        String[] ids = {
                AudiencePresets.SMALL, AudiencePresets.MEDIUM,
                AudiencePresets.LARGE, AudiencePresets.MEGA, AudiencePresets.DUO
        };
        for (String id : ids) {
            boolean sel = id.equalsIgnoreCase(config.audiencePreset);
            String label = (sel ? "§a▶ " : "§7  ") + AudiencePresets.label(id);
            boolean hover = over(mouseX, mouseY, x, y, w, 18);
            TwitchUi.roundedPanel(g, x, y, x + w, y + 18,
                    hover || sel ? 0xFF_2A_1E_38 : 0xFF_18_14_22);
            g.drawString(font, label, x + 8, y + 5, TwitchUi.TEXT, false);
            hits.add(new Hit(x, y, w, 18, () -> {
                AudiencePresets.apply(config, id);
                statusMsg = "Выбрано: " + AudiencePresets.label(id);
                statusColor = 0xFF_55_FF_55;
            }));
            y += 22;
        }
    }

    private void renderDonations(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        g.drawString(font, "§fDonateX §8(необязательно)", x, y, TwitchUi.TEXT, false);
        TwitchUi.drawWrappedText(g, font, x, y + 14, w,
                "§7Донаты запускают эффекты и колесо. Можно пропустить — добавишь позже во вкладке Twitch.",
                TwitchUi.TEXT_DIM);
        g.drawString(font, TwitchSetupHints.DA_LABEL, x, y + 56, TwitchUi.TEXT, false);
        int linkY = y + 70;
        g.drawString(font, "§7Кабинет: ", x, linkY, TwitchUi.TEXT_DIM, false);
        int linkX = x + font.width("§7Кабинет: ");
        String link = "§n§bdonatex.gg";
        g.drawString(font, link, linkX, linkY, TwitchUi.ACCENT_ALT, false);
        hits.add(new Hit(linkX, linkY, font.width(link), 10,
                () -> TwitchUi.openLink(TwitchSetupHints.DA_URL)));
        g.drawString(font, TwitchSetupHints.DA_HINT2, x, y + 84, TwitchUi.TEXT_DIM, false);
        // daBox @ panelT+150
    }

    private void renderDone(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY) {
        g.drawString(font, "§a§lВсё готово!", x, y, TwitchUi.TEXT, false);
        y += 16;
        String ch = profile.twitchChannel != null && !profile.twitchChannel.isBlank()
                ? profile.twitchChannel : "—";
        g.drawString(font, "§7Канал: §d" + ch, x, y, TwitchUi.TEXT_DIM, false);
        y += 12;
        g.drawString(font, "§7Аудитория: §f" + AudiencePresets.label(config.audiencePreset),
                x, y, TwitchUi.TEXT_DIM, false);
        y += 12;
        g.drawString(font, profile.hasDonations() ? "§7Донаты: §aподключены" : "§7Донаты: §8позже",
                x, y, TwitchUi.TEXT_DIM, false);
        y += 24;

        int btnW = w;
        int btnH = 22;
        boolean connectHover = over(mouseX, mouseY, x, y, btnW, btnH);
        TwitchUi.roundedPanel(g, x, y, x + btnW, y + btnH,
                connectHover ? TwitchUi.SAVE_BTN_HOVER : TwitchUi.SAVE_BTN);
        g.drawCenteredString(font, "§lСохранить и подключить", x + btnW / 2, y + 7, 0xFF_FF_FF_FF);
        hits.add(new Hit(x, y, btnW, btnH, () -> finish(true)));
        y += 28;

        boolean saveHover = over(mouseX, mouseY, x, y, btnW, btnH);
        TwitchUi.roundedPanel(g, x, y, x + btnW, y + btnH,
                saveHover ? 0xFF_3A_30_48 : 0xFF_28_22_36);
        g.drawCenteredString(font, "Сохранить без подключения", x + btnW / 2, y + 7, TwitchUi.TEXT);
        hits.add(new Hit(x, y, btnW, btnH, () -> finish(false)));
        y += 30;
        g.drawString(font, "§8Потом: K — настройки · R — правила · P — статистика",
                x, y, TwitchUi.TEXT_DIM, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x();
        double my = event.y();
        for (Hit h : hits) {
            if (over(mx, my, h.x, h.y, h.w, h.h)) {
                h.action.run();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        closeWizard(false);
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String maskToken(String token) {
        if (token == null || token.isBlank()) return "";
        return "••••••••";
    }

    private static TwitchCraftConfig copy(TwitchCraftConfig src) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        return gson.fromJson(gson.toJson(src), TwitchCraftConfig.class);
    }
}
