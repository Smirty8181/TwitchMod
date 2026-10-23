package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.twitchcraft.config.ConfigManager;

/** Лёгкие UI-звуки HUD (опционально). */
public final class HudUiSounds {
    private static long lastFeedAt;
    private static long lastDonationAt;

    private HudUiSounds() {}

    public static boolean enabled() {
        var ui = ConfigManager.get().ui;
        return ui == null || ui.hudSounds;
    }

    public static void feedTick() {
        if (!enabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastFeedAt < 80) return;
        lastFeedAt = now;
        play(SoundEvents.UI_BUTTON_CLICK.value(), 0.18f, 1.55f);
    }

    public static void denyTick() {
        if (!enabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastFeedAt < 90) return;
        lastFeedAt = now;
        play(SoundEvents.UI_TOAST_OUT, 0.22f, 0.92f);
    }

    public static void donation() {
        if (!enabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastDonationAt < 200) return;
        lastDonationAt = now;
        play(SoundEvents.UI_TOAST_IN, 0.45f, 1.05f);
    }

    public static void wheelStop() {
        if (!enabled()) return;
        play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.15f);
    }

    public static void diceRoll() {
        if (!enabled()) return;
        play(SoundEvents.UI_BUTTON_CLICK.value(), 0.35f, 0.7f);
    }

    public static void diceLand() {
        if (!enabled()) return;
        play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.55f, 1.25f);
    }

    public static void death() {
        if (!enabled()) return;
        play(SoundEvents.UI_TOAST_OUT, 0.35f, 0.7f);
    }

    private static void play(SoundEvent sound, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || sound == null) return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
