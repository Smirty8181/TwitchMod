package net.twitchcraft.client;

import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.HudDefaults;
import net.twitchcraft.config.HudLegacySync;
import net.twitchcraft.config.TwitchCraftConfig;

/** Доступ к настройкам HUD для оверлеев. */
public final class HudResolver {
    public enum Element {
        SIDEBAR, COMMAND_FEED, COMMAND_DENY, DEATH_TOAST, PINNED_DEATH_COORDS, DONATION_TOAST,
        COMBO_CHAIN, DONATION_WHEEL, COMMAND_FX, DONATION_FX, DUO_SCORE
    }

    private HudResolver() {}

    public static TwitchCraftConfig.HudSettings hud() {
        TwitchCraftConfig.HudSettings local = HudClientStore.get();
        if (local != null) return local;
        TwitchCraftConfig cfg = ConfigManager.get();
        if (cfg.hud == null) {
            HudLegacySync.ensure(cfg);
        }
        return cfg.hud;
    }

    public static TwitchCraftConfig.HudElementConfig el(Element element) {
        var hud = hud();
        if (hud == null) {
            return switch (element) {
                case SIDEBAR -> HudDefaults.sidebar();
                case COMMAND_FEED -> HudDefaults.commandFeed();
                case COMMAND_DENY -> HudDefaults.commandDeny();
                case DEATH_TOAST -> HudDefaults.deathToast();
                case PINNED_DEATH_COORDS -> HudDefaults.pinnedDeathCoords();
                case DONATION_TOAST -> HudDefaults.donationToast();
                case COMBO_CHAIN -> HudDefaults.comboChain();
                case DONATION_WHEEL -> HudDefaults.donationWheel();
                case COMMAND_FX -> HudDefaults.commandFx();
                case DONATION_FX -> HudDefaults.donationFx();
                case DUO_SCORE -> HudDefaults.duoScore();
            };
        }
        TwitchCraftConfig.HudElementConfig c = switch (element) {
            case SIDEBAR -> hud.sidebar;
            case COMMAND_FEED -> hud.commandFeed;
            case COMMAND_DENY -> hud.commandDeny;
            case DEATH_TOAST -> hud.deathToast;
            case PINNED_DEATH_COORDS -> hud.pinnedDeathCoords;
            case DONATION_TOAST -> hud.donationToast;
            case COMBO_CHAIN -> hud.comboChain;
            case DONATION_WHEEL -> hud.donationWheel;
            case COMMAND_FX -> hud.commandFx;
            case DONATION_FX -> hud.donationFx;
            case DUO_SCORE -> hud.duoScore;
        };
        if (c != null) return c;
        return switch (element) {
            case SIDEBAR -> HudDefaults.sidebar();
            case COMMAND_FEED -> HudDefaults.commandFeed();
            case COMMAND_DENY -> HudDefaults.commandDeny();
            case DEATH_TOAST -> HudDefaults.deathToast();
            case PINNED_DEATH_COORDS -> HudDefaults.pinnedDeathCoords();
            case DONATION_TOAST -> HudDefaults.donationToast();
            case COMBO_CHAIN -> HudDefaults.comboChain();
            case DONATION_WHEEL -> HudDefaults.donationWheel();
            case COMMAND_FX -> HudDefaults.commandFx();
            case DONATION_FX -> HudDefaults.donationFx();
            case DUO_SCORE -> HudDefaults.duoScore();
        };
    }

    public static boolean enabled(Element element) {
        var cfg = el(element);
        return cfg != null && cfg.enabled;
    }

    public static float opacity(Element element) {
        float o = el(element).opacity;
        return o <= 0 ? 1f : Math.min(1f, o);
    }

    public static float elementScale(Element element) {
        float s = el(element).scale;
        return s <= 0 ? 1f : s;
    }

    public static int accent(Element element, int fallback) {
        int c = el(element).accentColor;
        return c != 0 ? (c | 0xFF_00_00_00) : fallback;
    }

    public static int goodColor(int fallback) {
        int c = el(Element.COMMAND_FEED).goodColor;
        if (c == 0) c = el(Element.COMMAND_FX).goodColor;
        return c != 0 ? (c | 0xFF_00_00_00) : fallback;
    }

    public static int evilColor(int fallback) {
        int c = el(Element.COMMAND_FEED).evilColor;
        if (c == 0) c = el(Element.COMMAND_FX).evilColor;
        return c != 0 ? (c | 0xFF_00_00_00) : fallback;
    }

    public static TwitchCraftConfig.HudSidebarSections sidebarSections() {
        var sc = hud().sidebarContent;
        return sc != null ? sc : new TwitchCraftConfig.HudSidebarSections();
    }

    public static TwitchCraftConfig.HudCommandFeedSections feedSections() {
        var fc = hud().commandFeedContent;
        return fc != null ? fc : new TwitchCraftConfig.HudCommandFeedSections();
    }

    public static void onConfigSaved() {
        HudClientStore.save();
        net.twitchcraft.hud.SidebarPages.invalidate();
    }
}
