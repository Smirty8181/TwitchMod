package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.donation.DonationWheelPool;
import net.twitchcraft.donation.DonationWheelService;
import net.twitchcraft.donation.WheelTimeFreeze;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.DonationWheelPayload;

import java.util.List;

/** Полоска хаоса заполнена → колесо наказания. */
public final class ChaosWheel {
    private ChaosWheel() {}

    public static boolean start(MinecraftServer server, ServerPlayer target) {
        if (server == null || target == null) return false;
        if (DonationWheelService.isBusy()) return false;
        DonationWheelService.markBusy();
        DonationWheelPool.SpinResult spin = ChaosWheelPool.roll(ConfigManager.get());
        DonationWheelPool.Segment winner = spin.winner();
        ChatGameCommand cmd = ChatGameCommand.fromId(winner.commandId()).orElse(ChatGameCommand.TRAP);

        List<String> labels = spin.segments().stream().map(DonationWheelPool.Segment::label).toList();
        List<String> ids = spin.segments().stream().map(DonationWheelPool.Segment::commandId).toList();
        List<Integer> colors = spin.segments().stream().map(DonationWheelPool.Segment::color).toList();

        EffectRunner.pause(DonationWheelService.WHEEL_DURATION_MS / 1000L + 1);
        WheelTimeFreeze.freeze(server);
        AnnouncementService.announceCustom(target, "§c§lХАОС МАКС §7— колесо наказания");
        TwitchCraftMod.getConnectionManager().sendChatMessage("ХАОС МАКС! Крутим колесо наказания.");

        DonationWheelPayload.send(target, "ХАОС", "MAX", "", 0xFF_E7_4C_3C,
                labels, ids, colors, spin.winnerIndex(), winner.label());

        int spinTicks = DonationWheelService.SPIN_MS / 50;
        int totalTicks = DonationWheelService.WHEEL_DURATION_MS / 50;
        ServerTaskQueue.schedule(server, spinTicks, () -> {
            if (!target.isAlive()) return;
            AnnouncementService.announceCustom(target, "§c§lНАКАЗАНИЕ §f" + winner.label());
            TwitchCraftMod.getConnectionManager().sendChatMessage("Хаос выбрал: " + winner.label());
        });
        ServerTaskQueue.schedule(server, totalTicks, () -> {
            try {
                if (!TargetPlayerUtil.isOnline(server, target)) return;
                CommandContext ctx = new CommandContext(server, target, "хаос",
                        "!" + cmd.getId(), cmd, true);
                cmd.execute(ctx);
            } finally {
                WheelTimeFreeze.unfreeze(server);
            }
        });
        return true;
    }
}
