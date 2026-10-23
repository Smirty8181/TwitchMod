package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.EvilCommands;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.feedback.AnnouncementService;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomEventScheduler {
    private static long nextEventAt;

    private static final ChatGameCommand[] SAFE_POOL = {
            ChatGameCommand.HYPE,
            ChatGameCommand.CHICKEN,
            ChatGameCommand.FLASH,
            ChatGameCommand.JUMPSCARE,
            ChatGameCommand.DRUNK,
            ChatGameCommand.GLOW,
            ChatGameCommand.FISH,
            ChatGameCommand.DANCE,
            ChatGameCommand.CLOWN,
            ChatGameCommand.SLAP,
            ChatGameCommand.FIREWORK,
            ChatGameCommand.COIN,
            ChatGameCommand.KVA
    };

    private RandomEventScheduler() {}

    public static void init() {
        scheduleNext();
    }

    public static void tick(MinecraftServer server) {
        if (!ConfigManager.get().stream.randomEventsEnabled) return;
        if (StreamerShield.isActive()) return;
        if (nextEventAt == 0) scheduleNext();
        if (System.currentTimeMillis() >= nextEventAt) {
            fire(server, "§d§lСлучайное событие!");
            scheduleNext();
        }
    }

    public static void fire(MinecraftServer server, String announcePrefix) {
        if (!ConfigManager.get().stream.randomEventsEnabled) return;
        if (StreamerShield.isActive()) return;

        StreamTargets.resolve(server).ifPresent(target -> {
            ChatGameCommand cmd = pickCommand();
            if (cmd == null) return;
            String label = cmd.getRussianName();

            AnnouncementService.announceCustom(target, announcePrefix + " §e!" + label);
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    "Событие: !" + label + " на " + target.getGameProfile().name());
            TwitchCraftMod.getConnectionManager().executeById(
                    server, "событие", cmd.getId(), true, target.getGameProfile().name());
        });
    }

    private static ChatGameCommand pickCommand() {
        for (int attempts = 0; attempts < 20; attempts++) {
            ChatGameCommand cmd = SAFE_POOL[ThreadLocalRandom.current().nextInt(SAFE_POOL.length)];
            if (EvilCommands.isEvil(cmd)) continue;
            var settings = ConfigManager.get().getCommandSettings(cmd.getId(), cmd.getDefaultCooldown());
            if (settings.enabled) return cmd;
        }
        return ChatGameCommand.HYPE;
    }

    private static void scheduleNext() {
        var s = ConfigManager.get().stream;
        int min = Math.max(1, s.randomEventMinMinutes);
        int max = Math.max(min, s.randomEventMaxMinutes);
        int minutes = min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
        nextEventAt = System.currentTimeMillis() + minutes * 60_000L;
    }
}
