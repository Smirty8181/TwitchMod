package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.feedback.AnnouncementService;

public final class RevengeHandler {
    private RevengeHandler() {}

    public static void executeArmy(MinecraftServer server) {
        var stream = ConfigManager.get().stream;
        StreamTargets.resolve(server).ifPresent(player -> AnnouncementService.announceRevenge(player));
        server.schedule(new net.minecraft.server.TickTask(server.getTickCount() + stream.revengeDelaySeconds * 20, () -> {
            if (!server.isRunning()) return;
            EffectRunner.resolveTarget(server).ifPresent(target -> {
                CommandContext ctx = new CommandContext(server, target, "revenge", "!revenge",
                        net.twitchcraft.commands.ChatGameCommand.ARMY, true);
                net.twitchcraft.commands.ChatGameCommand.ARMY.execute(ctx);
            });
        }));
    }
}
