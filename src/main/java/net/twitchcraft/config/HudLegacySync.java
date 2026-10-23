package net.twitchcraft.config;

import java.util.Locale;

/**
 * Синхронизация hud ↔ legacy ui/scoreboard.
 * HUD — источник правды; legacy только зеркало и одноразовая миграция.
 */
public final class HudLegacySync {
    private HudLegacySync() {}

    public static void ensure(TwitchCraftConfig config) {
        if (config.hud == null) config.hud = new TwitchCraftConfig.HudSettings();
        var hud = config.hud;

        boolean freshSidebar = hud.sidebar == null;
        boolean freshFeed = hud.commandFeed == null;
        boolean freshDonation = hud.donationToast == null;
        boolean freshSidebarContent = hud.sidebarContent == null;

        if (freshSidebar) hud.sidebar = HudDefaults.sidebar();
        if (freshFeed) hud.commandFeed = HudDefaults.commandFeed();
        if (hud.commandDeny == null) hud.commandDeny = HudDefaults.commandDeny();
        if (hud.deathToast == null) hud.deathToast = HudDefaults.deathToast();
        if (hud.pinnedDeathCoords == null) hud.pinnedDeathCoords = HudDefaults.pinnedDeathCoords();
        if (freshDonation) hud.donationToast = HudDefaults.donationToast();
        if (hud.comboChain == null) hud.comboChain = HudDefaults.comboChain();
        if (hud.donationWheel == null) hud.donationWheel = HudDefaults.donationWheel();
        if (hud.commandFx == null) hud.commandFx = HudDefaults.commandFx();
        if (hud.donationFx == null) hud.donationFx = HudDefaults.donationFx();
        if (hud.duoScore == null) hud.duoScore = HudDefaults.duoScore();
        if (freshSidebarContent) hud.sidebarContent = new TwitchCraftConfig.HudSidebarSections();
        if (hud.commandFeedContent == null) hud.commandFeedContent = new TwitchCraftConfig.HudCommandFeedSections();

        if (config.ui != null) {
            if (hud.globalScale <= 0 && config.ui.hudScale > 0) {
                hud.globalScale = config.ui.hudScale;
            }
            // Миграция позиции — только для только что созданных элементов
            if (freshFeed && config.ui.feedPosition != null
                    && "left".equalsIgnoreCase(config.ui.feedPosition)) {
                hud.commandFeed.anchor = "CENTER_LEFT";
                hud.commandFeed.offsetX = 8;
                hud.commandFeed.offsetY = 72;
            }
            if (freshDonation && config.ui.donationPosition != null
                    && "bottom".equalsIgnoreCase(config.ui.donationPosition)) {
                hud.donationToast.anchor = "BOTTOM_CENTER";
                hud.donationToast.offsetY = 56;
            }
            if (freshSidebarContent) {
                hud.sidebarContent.compact = config.ui.sidebarCompact;
            }
        }

        // Старый конфиг без hud.* — один раз взять флаги из scoreboard
        if (config.scoreboard != null) {
            if (freshSidebar) {
                hud.sidebar.enabled = config.scoreboard.enabled;
            }
            if (freshSidebarContent) {
                var sc = hud.sidebarContent;
                sc.commands = config.scoreboard.showCommands;
                sc.donations = config.scoreboard.showDonations;
                sc.stats = config.scoreboard.showStats;
                sc.graveyard = config.scoreboard.showGraveyard;
                sc.featuredCommand = config.scoreboard.showFeaturedCommand;
                sc.maxCommandLines = config.scoreboard.maxCommandLines;
                sc.pageRotateSeconds = config.scoreboard.pageRotateSeconds;
                if (config.scoreboard.featuredCommandId != null) {
                    sc.featuredCommandId = config.scoreboard.featuredCommandId;
                }
            }
            if (freshFeed) {
                hud.commandFeed.enabled = config.scoreboard.showCommandFeed;
            }
        }

        pushToLegacy(config);
    }

    /** Записать hud-настройки обратно в ui/scoreboard для совместимости. */
    public static void pushToLegacy(TwitchCraftConfig config) {
        if (config.hud == null || config.ui == null || config.scoreboard == null) return;
        var hud = config.hud;

        config.ui.hudScale = hud.globalScale > 0 ? hud.globalScale : 1f;
        if (hud.sidebarContent != null) {
            config.ui.sidebarCompact = hud.sidebarContent.compact;
        }

        String feedAnchor = norm(hud.commandFeed != null ? hud.commandFeed.anchor : null);
        config.ui.feedPosition = feedAnchor.endsWith("_LEFT") ? "left" : "bottom";

        String donAnchor = norm(hud.donationToast != null ? hud.donationToast.anchor : null);
        config.ui.donationPosition = donAnchor.startsWith("BOTTOM") ? "bottom" : "top";

        if (hud.sidebar != null) {
            config.scoreboard.enabled = hud.sidebar.enabled;
        }
        var sc = hud.sidebarContent;
        if (sc != null) {
            config.scoreboard.showCommands = sc.commands;
            config.scoreboard.showDonations = sc.donations;
            config.scoreboard.showStats = sc.stats;
            config.scoreboard.showGraveyard = sc.graveyard;
            config.scoreboard.showFeaturedCommand = sc.featuredCommand;
            config.scoreboard.maxCommandLines = sc.maxCommandLines;
            config.scoreboard.pageRotateSeconds = sc.pageRotateSeconds;
            config.scoreboard.featuredCommandId = sc.featuredCommandId;
        }
        if (hud.commandFeed != null) {
            config.scoreboard.showCommandFeed = hud.commandFeed.enabled;
        }
    }

    private static String norm(String s) {
        return s == null ? "" : s.toUpperCase(Locale.ROOT);
    }
}
