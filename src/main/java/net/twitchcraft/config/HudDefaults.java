package net.twitchcraft.config;

/** Дефолтные раскладки HUD-элементов. */
public final class HudDefaults {
    private HudDefaults() {}

    public static TwitchCraftConfig.HudElementConfig sidebar() {
        var c = new TwitchCraftConfig.HudElementConfig();
        c.anchor = "TOP_LEFT";
        c.offsetX = 6;
        c.offsetY = 8;
        return c;
    }

    public static TwitchCraftConfig.HudElementConfig commandFeed() {
        var c = new TwitchCraftConfig.HudElementConfig();
        c.anchor = "BOTTOM_LEFT";
        c.offsetX = 0;
        c.offsetY = 0;
        return c;
    }

    public static TwitchCraftConfig.HudElementConfig commandDeny() {
        var c = new TwitchCraftConfig.HudElementConfig();
        c.anchor = "BOTTOM_LEFT";
        c.offsetX = 0;
        c.offsetY = 0;
        c.accentColor = 0xFF_F0_A0_3C;
        return c;
    }

    public static TwitchCraftConfig.HudElementConfig deathToast() {
        var c = new TwitchCraftConfig.HudElementConfig();
        c.anchor = "TOP_CENTER";
        c.offsetX = 0;
        c.offsetY = 48;
        return c;
    }

    public static TwitchCraftConfig.HudElementConfig pinnedDeathCoords() {
        var c = new TwitchCraftConfig.HudElementConfig();
        c.anchor = "BOTTOM_LEFT";
        c.offsetX = 8;
        c.offsetY = 8;
        c.scale = 1.0f;
        return c;
    }

    public static TwitchCraftConfig.HudElementConfig donationToast() {
        var c = new TwitchCraftConfig.HudElementConfig();
        c.anchor = "TOP_CENTER";
        c.offsetX = 0;
        c.offsetY = 14;
        return c;
    }

    public static TwitchCraftConfig.HudElementConfig comboChain() {
        var c = new TwitchCraftConfig.HudElementConfig();
        c.anchor = "CENTER";
        c.offsetX = 0;
        c.offsetY = -60;
        return c;
    }

    public static TwitchCraftConfig.HudElementConfig donationWheel() {
        var c = new TwitchCraftConfig.HudElementConfig();
        c.anchor = "CENTER";
        c.offsetX = 0;
        c.offsetY = 4;
        c.scale = 1.0f;
        return c;
    }

    public static TwitchCraftConfig.HudElementConfig commandFx() {
        var c = new TwitchCraftConfig.HudElementConfig();
        c.enabled = true;
        c.evilColor = 0xFF_44_22_33;
        c.goodColor = 0xFF_22_55_33;
        return c;
    }

    public static TwitchCraftConfig.HudElementConfig duoScore() {
        var c = new TwitchCraftConfig.HudElementConfig();
        c.anchor = "BOTTOM_RIGHT";
        c.offsetX = 8;
        c.offsetY = 8;
        c.accentColor = 0xFF_E7_4C_3C;
        return c;
    }

    public static TwitchCraftConfig.HudElementConfig donationFx() {
        var c = new TwitchCraftConfig.HudElementConfig();
        c.enabled = true;
        return c;
    }
}
