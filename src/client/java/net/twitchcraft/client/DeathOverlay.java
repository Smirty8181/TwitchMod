package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.twitchcraft.config.ConfigManager;

/** Алерт смерти — shake + красный edge, штраф дольше. */
public final class DeathOverlay {
    private static int totalDeaths;
    private static int windowDeaths;
    private static int x, y, z;
    private static String dimension = "";
    private static int evilBlockSec;
    private static boolean penalty;
    private static String victim = "";
    private static String killer = "";
    private static String cause = "";
    private static long showUntil;
    private static long showStart;
    private static float slide;

    /** Последние coords для пина (даже после скрытия toast). */
    private static int pinX, pinY, pinZ;
    private static String pinDim = "";
    private static boolean hasPin;

    private DeathOverlay() {}

    public static void show(int total, int window, int px, int py, int pz,
                            String dim, int evilSec, boolean penaltyTriggered,
                            String victimName, String killerName, String causeRu) {
        // Пин всегда — для стрелки/закрепления, даже если toast выключен
        pinX = px;
        pinY = py;
        pinZ = pz;
        pinDim = dim != null ? dim : "";
        hasPin = true;

        if (!HudResolver.enabled(HudResolver.Element.DEATH_TOAST)) return;
        var deathCfg = ConfigManager.get().death;
        if (deathCfg != null && !deathCfg.showDeathToast) return;

        totalDeaths = total;
        windowDeaths = window;
        x = px;
        y = py;
        z = pz;
        dimension = pinDim;
        evilBlockSec = evilSec;
        penalty = penaltyTriggered;
        victim = victimName == null ? "" : victimName;
        killer = killerName == null ? "" : killerName;
        cause = causeRu == null ? "" : causeRu;
        showStart = System.currentTimeMillis();
        showUntil = showStart + (penalty ? 8500 : 7000);
        slide = 40f;

        HudUiSounds.death();
        if (HudResolver.enabled(HudResolver.Element.COMMAND_FX)) {
            CommandScreenFx.triggerEvil(penalty ? 3 : 2, penalty ? 1600 : 700);
        }
    }

    public static boolean hasPinnedCoords() {
        return hasPin;
    }

    public static int pinX() { return pinX; }
    public static int pinY() { return pinY; }
    public static int pinZ() { return pinZ; }
    public static String pinDim() { return pinDim != null ? pinDim : ""; }

    public static void render(GuiGraphics g, float delta) {
        if (System.currentTimeMillis() > showUntil) return;
        if (!HudResolver.enabled(HudResolver.Element.DEATH_TOAST)) return;
        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc)) return;

        var el = HudResolver.el(HudResolver.Element.DEATH_TOAST);
        float es = HudResolver.elementScale(HudResolver.Element.DEATH_TOAST);
        float opacity = HudResolver.opacity(HudResolver.Element.DEATH_TOAST);

        long now = System.currentTimeMillis();
        float fadeIn = HudAnim.easeOutCubic(Math.min(1f, (now - showStart) / 350f));
        float fadeOut = (showUntil - now) < 900 ? (showUntil - now) / 900f : 1f;
        float alpha = fadeIn * fadeOut * opacity;
        slide = HudAnim.approach(slide, 0f, 0.22f);

        // Shake 2–3 кадра (дольше при штрафе)
        int shakeMs = penalty ? 280 : 140;
        int shakeX = 0;
        long age = now - showStart;
        if (age < shakeMs) {
            float wave = (float) Math.sin(age * (penalty ? 0.55 : 0.7));
            shakeX = Math.round(wave * (penalty ? 4.5f : 3f));
        }

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int aByte = (int) (alpha * 255);

        int vig = (int) ((penalty ? 90 : 55) * alpha);
        g.fill(0, 0, sw, HudLayout.s(22, es), vig << 24);
        g.fill(0, sh - HudLayout.s(22, es), sw, sh, vig << 24);
        TwitchUi.fillGradientH(g, 0, 0, HudLayout.s(28, es), sh, (vig << 24) | 0xC0_20_20, 0x00_00_00_00);
        TwitchUi.fillGradientH(g, sw - HudLayout.s(28, es), 0, sw, sh, 0x00_00_00_00, (vig << 24) | 0xC0_20_20);

        int panelW = Math.min(sw - HudLayout.s(40, es), HudLayout.s(268, es));
        int panelH = penalty ? HudLayout.s(96, es) : HudLayout.s(84, es);
        HudPlacement.Rect pos = HudPlacement.place(el.anchor, el.offsetX, el.offsetY,
                sw, sh, panelW, panelH, es);
        int px = pos.x() + shakeX;
        int py = pos.y() + (int) slide;

        int accent = penalty
                ? HudResolver.evilColor(0xFF_FF_33_44)
                : HudResolver.accent(HudResolver.Element.DEATH_TOAST, 0xFF_CC_44_55);
        HudCard.drawPremium(g, px, py, panelW, panelH, accent, alpha, 1f, true, now);

        var font = mc.font;
        boolean mine = isLocalVictim(mc);
        String kicker = penalty ? "ШТРАФ ЧАТА" : (mine ? "СМЕРТЬ" : trunc(victim) + " УМЕР");
        String badge = penalty ? "ШТРАФ" : "СМЕРТЬ";
        int badgeW = font.width(badge) + HudLayout.s(14, es);
        HudCard.drawBadge(g, font, px + panelW - badgeW - HudLayout.s(10, es), py + HudLayout.s(8, es),
                badge, accent, alpha);
        String title = "§c§l☠  " + kicker;
        int tw = font.width(TwitchUi.stripFormat(title));
        g.drawString(font, title, px + (panelW - tw) / 2, py + HudLayout.s(8, es),
                (aByte << 24) | 0xFF_66_66, false);

        String killLine = !killer.isBlank()
                ? "§cУБИЛ: §f§l" + trunc(killer)
                : "§cУМЕР: §f§l" + trunc(cause.isBlank() ? "неизвестно" : cause);
        int kw = font.width(TwitchUi.stripFormat(killLine));
        g.drawString(font, killLine, px + (panelW - kw) / 2, py + HudLayout.s(24, es),
                (aByte << 24) | 0xFF_EE_EE, false);

        String second = !killer.isBlank() && !cause.isBlank()
                ? "§7из-за: §f" + cause + " §8· §7#" + totalDeaths
                : "§7Всего: §f" + totalDeaths + " §8| §7окно: §c" + windowDeaths;
        int sw2 = font.width(TwitchUi.stripFormat(second));
        g.drawString(font, second, px + (panelW - sw2) / 2, py + HudLayout.s(38, es),
                (aByte << 24) | 0xCCCCCC, false);

        String coords = "§eX:" + x + " §fY:" + y + " §bZ:" + z + dimLabel();
        int coordW = font.width(TwitchUi.stripFormat(coords));
        g.drawString(font, coords, px + (panelW - coordW) / 2, py + HudLayout.s(52, es),
                (aByte << 24) | 0xFF_EE_EE, false);

        if (penalty && evilBlockSec > 0) {
            String pen = "§cУрон заблокирован: §f" + formatTime(evilBlockSec);
            int pw = font.width(TwitchUi.stripFormat(pen));
            g.drawString(font, pen, px + (panelW - pw) / 2, py + HudLayout.s(66, es),
                    (aByte << 24) | 0xFF_AA_AA, false);
        }

        float progress = 1f - (float) (now - showStart) / (showUntil - showStart);
        HudCard.drawProgressBar(g, px + HudLayout.s(10, es), py + panelH - HudLayout.s(7, es),
                panelW - HudLayout.s(20, es), HudLayout.s(3, es), progress, accent, alpha);
    }

    private static boolean isLocalVictim(Minecraft mc) {
        if (mc.player == null || victim == null || victim.isBlank()) return true;
        return mc.player.getGameProfile().name().equalsIgnoreCase(victim);
    }

    private static String trunc(String s) {
        if (s == null) return "";
        return s.length() > 16 ? s.substring(0, 15) + "…" : s;
    }

    private static String dimLabel() {
        if (dimension == null || dimension.isBlank()) return "";
        String ru = switch (dimension) {
            case "overworld" -> "Овермир";
            case "the_nether" -> "Ад";
            case "the_end" -> "Край";
            default -> dimension;
        };
        return " §8(" + ru + ")";
    }

    private static String formatTime(int sec) {
        int m = sec / 60;
        int s = sec % 60;
        return m > 0 ? m + "м " + s + "с" : s + "с";
    }
}
