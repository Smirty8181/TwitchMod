package net.twitchcraft.client;

import net.minecraft.util.Mth;
import net.twitchcraft.config.ConfigManager;

/** Масштаб и позиции HUD-оверлеев. */
public final class HudLayout {
    private HudLayout() {}

    public static float globalScale() {
        var hud = HudResolver.hud();
        float g = hud != null ? hud.globalScale : 0f;
        if (g <= 0) {
            var cfg = ConfigManager.get();
            if (cfg.ui != null) g = cfg.ui.hudScale;
        }
        return Mth.clamp(g <= 0 ? 1f : g, 0.5f, 1.5f);
    }

    public static int s(int v) {
        return s(v, 1f);
    }

    public static int s(int v, float elementScale) {
        float es = elementScale <= 0 ? 1f : elementScale;
        return Math.max(1, Math.round(v * globalScale() * es));
    }

    public static boolean donationTop() {
        var el = HudResolver.el(HudResolver.Element.DONATION_TOAST);
        String a = el.anchor == null ? "TOP_CENTER" : el.anchor.toUpperCase();
        return !a.startsWith("BOTTOM");
    }

    public static boolean feedLeft() {
        return HudPlacement.isLeftStack(HudResolver.el(HudResolver.Element.COMMAND_FEED).anchor);
    }

    public static boolean sidebarCompact() {
        return HudResolver.sidebarSections().compact;
    }
}
