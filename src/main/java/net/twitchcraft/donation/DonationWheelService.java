package net.twitchcraft.donation;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.DonationWheelPayload;
import net.twitchcraft.stream.ServerTaskQueue;
import net.twitchcraft.stream.StreamStats;
import net.twitchcraft.stream.TargetPlayerUtil;
import net.twitchcraft.twitch.ChatCommandProcessor;

import java.util.List;

/** Рулетка доната 500₽+ — спин на клиенте, эффект и анонс победителя после остановки. */
public final class DonationWheelService {
    public static final int SPIN_MS = 3200;
    public static final int HOLD_MS = 2600;
    public static final int WHEEL_DURATION_MS = SPIN_MS + HOLD_MS;
    private static final int SPIN_TICKS = SPIN_MS / 50;
    private static final int TOTAL_TICKS = WHEEL_DURATION_MS / 50;
    private static long busyUntil;

    private DonationWheelService() {}

    public static boolean isBusy() {
        return System.currentTimeMillis() < busyUntil;
    }

    public static void markBusy() {
        busyUntil = System.currentTimeMillis() + WHEEL_DURATION_MS + 400L;
    }

    public static void start(MinecraftServer server, ServerPlayer target, String username,
                             String amountStr, String currency, TwitchCraftConfig.DonationTier tier,
                             String donorMessage, ChatCommandProcessor processor) {
        TwitchCraftConfig config = ConfigManager.get();
        DonationWheelPool.SpinResult spin = DonationWheelPool.roll(config);
        DonationWheelPool.Segment winner = spin.winner();

        int tierColor = DonationTierStyle.colorForAmount(tier.minAmount);
        List<String> labels = spin.segments().stream().map(DonationWheelPool.Segment::label).toList();
        List<String> ids = spin.segments().stream().map(DonationWheelPool.Segment::commandId).toList();
        List<Integer> colors = spin.segments().stream().map(DonationWheelPool.Segment::color).toList();

        ChatGameCommand cmd = ChatGameCommand.fromId(winner.commandId()).orElse(ChatGameCommand.METEOR);

        DonationState.record(username, amountStr, currency, "🎰 " + winner.label(), target.getGameProfile().name());
        StreamStats.forPlayer(target.getGameProfile().name()).recordDonation(username, winner.commandId());

        markBusy();
        EffectRunner.pause(WHEEL_DURATION_MS / 1000L + 1);
        WheelTimeFreeze.freeze(server);

        // Сразу: только «крутится», без спойлера приза
        AnnouncementService.announceDonationWheelSpin(target, username, amountStr, currency, tier.minAmount);

        DonationWheelPayload.send(target, username, amountStr, currency, tierColor,
                labels, ids, colors, spin.winnerIndex(), winner.label());

        // После остановки барабана — анонс победителя (совпадает с HUD)
        ServerTaskQueue.schedule(server, SPIN_TICKS, () -> {
            if (!TargetPlayerUtil.isOnline(server, target)) return;
            AnnouncementService.announceDonationWheelResult(target, username, amountStr, currency,
                    winner.label(), cmd, donorMessage, tier.minAmount);
        });

        ServerTaskQueue.schedule(server, TOTAL_TICKS, () -> {
            try {
                if (!TargetPlayerUtil.isOnline(server, target)) return;
                TwitchCraftMod.LOGGER.info("Donation wheel: {} won {} (idx={})",
                        username, winner.commandId(), spin.winnerIndex());
                var winTier = new TwitchCraftConfig.DonationTier(tier.minAmount, winner.commandId(), winner.label());
                winTier.wheel = false;
                DonationEffectService.executeWheelWinner(server, target, username, winTier, donorMessage, processor);
            } finally {
                WheelTimeFreeze.unfreeze(server);
            }
        });
    }
}
