package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Регистрация payload-типов на обеих сторонах (Fabric 1.21). */
public final class NetworkRegistration {
    private static boolean s2cRegistered;
    private static boolean c2sRegistered;

    private NetworkRegistration() {}

    public static void registerPlayS2C() {
        if (s2cRegistered) return;
        PayloadTypeRegistry.playS2C().register(OpenRulesPayload.TYPE, OpenRulesPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DonationAlertPayload.TYPE, DonationAlertPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DonationWheelPayload.TYPE, DonationWheelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CommandAlertPayload.TYPE, CommandAlertPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CommandDenyPayload.TYPE, CommandDenyPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ComboChainPayload.TYPE, ComboChainPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DeathAlertPayload.TYPE, DeathAlertPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SidebarStatePayload.TYPE, SidebarStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TtsSpeakPayload.TYPE, TtsSpeakPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ViewerStatsPayload.TYPE, ViewerStatsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ShowdownPayload.TYPE, ShowdownPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DicePayload.TYPE, DicePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DuoScorePayload.TYPE, DuoScorePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PartnerPingPayload.TYPE, PartnerPingPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WorldDropPayload.TYPE, WorldDropPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PartnerPresetPayload.TYPE, PartnerPresetPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RagdollPayload.TYPE, RagdollPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(InvertControlsPayload.TYPE, InvertControlsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UpgradePayload.TYPE, UpgradePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HotbarShufflePayload.TYPE, HotbarShufflePayload.CODEC);
        s2cRegistered = true;
    }

    public static void registerPlayC2S() {
        if (c2sRegistered) return;
        PayloadTypeRegistry.playC2S().register(SaveConfigPayload.TYPE, SaveConfigPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestViewerStatsPayload.TYPE, RequestViewerStatsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StreamerActionPayload.TYPE, StreamerActionPayload.CODEC);
        c2sRegistered = true;
    }
}
