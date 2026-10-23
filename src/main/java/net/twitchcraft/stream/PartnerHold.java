package net.twitchcraft.stream;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

/**
 * Дуэт: один умер — живому 5 сек щита и плашка «держись».
 * Не суд и не реванш: чат не бьёт напарника, мобы бьют слабее.
 */
public final class PartnerHold {
    public static final int SECONDS = 5;

    private static long until;
    private static String partnerName = "";
    private static String fallenName = "";

    private PartnerHold() {}

    public static boolean isActive() {
        return until > 0 && System.currentTimeMillis() < until;
    }

    public static int secondsLeft() {
        if (!isActive()) return 0;
        return (int) Math.max(0, (until - System.currentTimeMillis() + 999) / 1000);
    }

    public static boolean protects(ServerPlayer target) {
        if (!isActive() || target == null || partnerName.isBlank()) return false;
        return target.getGameProfile().name().equalsIgnoreCase(partnerName);
    }

    public static void onStreamerDeath(MinecraftServer server, ServerPlayer fallen) {
        if (server == null || fallen == null) return;
        if (HouseMode.isActive()) return;
        if (!ConfigManager.get().stream.duoStreamMode) return;
        StreamTargets.partner(server, fallen).ifPresent(partner -> activate(server, partner, fallen.getGameProfile().name()));
    }

    public static boolean activate(MinecraftServer server, ServerPlayer partner, String fallen) {
        if (partner == null) return false;
        partnerName = partner.getGameProfile().name();
        fallenName = fallen == null || fallen.isBlank() ? "напарник" : fallen.trim();
        until = System.currentTimeMillis() + SECONDS * 1000L;

        int ticks = SECONDS * 20;
        partner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 3, true, false, true));
        partner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 1, true, false, true));
        partner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, 0, true, false, true));

        if (partner.level() instanceof ServerLevel level) {
            level.playSound(null, partner.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.85f, 1.35f);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    partner.getX(), partner.getY() + 1.1, partner.getZ(),
                    18, 0.4, 0.6, 0.4, 0.12);
            level.sendParticles(ParticleTypes.END_ROD,
                    partner.getX(), partner.getY() + 1.0, partner.getZ(),
                    10, 0.25, 0.4, 0.25, 0.02);
        }

        AnnouncementService.announceCustom(partner,
                "§e§lДЕРЖИСЬ §f" + SECONDS + "с §7чат не бьёт · " + trunc(fallenName) + " пал");
        StreamTargets.partner(server, partner).ifPresent(dead ->
                AnnouncementService.announceCustom(dead,
                        "§7" + trunc(partnerName) + " §eдержится §7" + SECONDS + "с"));
        StreamChronicle.note("держись " + partnerName + " ← " + fallenName);
        ShowdownDirector.refresh(server);
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (until <= 0) return;
        if (System.currentTimeMillis() < until) return;
        until = 0;
        String who = partnerName;
        partnerName = "";
        StreamTargets.resolve(server, who).ifPresent(p ->
                AnnouncementService.announceCustom(p, "§7щит «держись» спал"));
        ShowdownDirector.refresh(server);
    }

    public static void pushHud(MinecraftServer server) {
        if (!isActive()) return;
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "HOLD",
                "ДЕРЖИСЬ",
                trunc(fallenName) + " пал · чат не бьёт",
                secondsLeft(),
                SECONDS,
                0, 0,
                trunc(partnerName),
                "щит",
                true));
    }

    private static String trunc(String name) {
        if (name == null) return "";
        String t = name.trim();
        return t.length() > 14 ? t.substring(0, 14) : t;
    }
}
