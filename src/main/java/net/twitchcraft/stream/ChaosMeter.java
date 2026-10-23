package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.donation.DonationWheelService;

public final class ChaosMeter {
    private ChaosMeter() {}

    public static void onCommandExecuted(MinecraftServer server, ServerPlayer target, ChatGameCommand command) {
        var stream = ConfigManager.get().stream;
        if (!stream.chaosMeterEnabled) return;

        int current = StreamStats.forPlayer(target.getGameProfile().name()).addChaos(1, stream.chaosMeterMax);
        if (current < stream.chaosMeterMax) return;
        if (DonationWheelService.isBusy()) return;

        StreamStats.forPlayer(target.getGameProfile().name()).resetChaos();
        ChaosWheel.start(server, target);
    }

    public static void addBonus(MinecraftServer server, ServerPlayer target, int amount) {
        var stream = ConfigManager.get().stream;
        if (!stream.chaosMeterEnabled || amount <= 0) return;
        int current = StreamStats.forPlayer(target.getGameProfile().name()).addChaos(amount, stream.chaosMeterMax);
        if (current < stream.chaosMeterMax) return;
        if (DonationWheelService.isBusy()) return;
        StreamStats.forPlayer(target.getGameProfile().name()).resetChaos();
        ChaosWheel.start(server, target);
    }
}
