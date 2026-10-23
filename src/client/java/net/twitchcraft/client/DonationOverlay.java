package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.twitchcraft.donation.DonationEffectCatalog;

/** Премиум-алерт доната. */
public final class DonationOverlay {
    private static String donor = "";
    private static String amount = "";
    private static String label = "";
    private static String message = "";
    private static String effectId = "";
    private static int tierColor = 0xFF_FF_D7_00;
    private static int tierMin = 50;
    private static long showUntil;
    private static long showStart;
    private static long durationMs = 5500;

    private DonationOverlay() {}

    public static void show(String donorName, String amountText, String currency, String commandLabel,
                            int color, int minAmount, String effectId, String donorMessage, int screenFxLevel) {
        donor = donorName;
        TwitchAvatarCache.request(donorName);
        String cur = currency != null ? currency.trim() : "";
        if ("RUB".equalsIgnoreCase(cur) || "RUR".equalsIgnoreCase(cur)) cur = "₽";
        amount = amountText + (cur.isEmpty() ? "" : " " + cur);
        label = commandLabel != null && !commandLabel.isBlank()
                ? commandLabel
                : DonationEffectCatalog.exclusiveLabel(effectId);
        message = donorMessage != null ? donorMessage.trim() : "";
        DonationOverlay.effectId = effectId != null ? effectId : "";
        tierColor = color != 0 ? color : 0xFF_FF_D7_00;
        tierMin = minAmount > 0 ? minAmount : 50;
        durationMs = DonationEffectCatalog.toastDurationMs(tierMin);
        showStart = System.currentTimeMillis();
        showUntil = showStart + durationMs;
        if (HudResolver.enabled(HudResolver.Element.DONATION_FX)) {
            DonationScreenFx.trigger(screenFxLevel > 0 ? screenFxLevel : DonationEffectCatalog.screenFxLevel(tierMin),
                    (int) (durationMs * 0.35), tierMin);
        }
        HudUiSounds.donation();
    }

    public static void render(GuiGraphics g, float delta) {
        if (System.currentTimeMillis() > showUntil) return;
        if (DonationWheelOverlay.isActive()) return;
        if (!HudResolver.enabled(HudResolver.Element.DONATION_TOAST)) return;

        Minecraft mc = Minecraft.getInstance();
        if (!TwitchUi.shouldDrawHud(mc)) return;

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        float fade = fadeFactor();
        long elapsed = System.currentTimeMillis() - showStart;
        float appear = HudAnim.easeOutBack(Mth.clamp(elapsed / 380f, 0f, 1f));

        boolean hasMsg = message != null && !message.isBlank();
        var el = HudResolver.el(HudResolver.Element.DONATION_TOAST);
        float es = HudResolver.elementScale(HudResolver.Element.DONATION_TOAST);
        float opacity = HudResolver.opacity(HudResolver.Element.DONATION_TOAST);
        int boxW = DonationHudTheme.panelWidth(w, tierMin);
        int boxH = DonationHudTheme.panelHeight(tierMin, hasMsg);
        HudPlacement.Rect pos = HudPlacement.place(el.anchor, el.offsetX, el.offsetY, w, h, boxW, boxH, es);
        int x = pos.x();
        int y = pos.y() + (int) ((1f - appear) * -HudLayout.s(22, es));

        int displayColor = el.accentColor != 0 ? (el.accentColor | 0xFF_00_00_00) : tierColor;
        DonationHudTheme.drawPremiumCard(g, mc.font, x, y, boxW, boxH, displayColor, fade * opacity, tierMin,
                amount, donor, label, effectId, message);

        float progress = 1f - (float) (System.currentTimeMillis() - showStart) / durationMs;
        DonationHudTheme.drawProgress(g, x, y, boxW, boxH, displayColor, fade * opacity, progress);
    }

    private static float fadeFactor() {
        long left = showUntil - System.currentTimeMillis();
        long elapsed = System.currentTimeMillis() - showStart;
        float in = HudAnim.easeOutCubic(Math.min(1f, elapsed / 420f));
        float out = left < 850 ? left / 850f : 1f;
        return in * out;
    }
}
