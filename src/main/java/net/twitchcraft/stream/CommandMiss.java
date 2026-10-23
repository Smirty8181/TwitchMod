package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.EvilCommands;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.CommandAlertPayload;

/**
 * Дуэт: раз в несколько минут злая команда промахивается и бьёт напарника.
 */
public final class CommandMiss {
    public static final int INTERVAL_MS = 8 * 60 * 1000;

    private static long lastAt = System.currentTimeMillis();
    private static boolean armed;

    private CommandMiss() {}

    public static void arm() {
        armed = true;
    }

    public static ServerPlayer maybeRedirect(MinecraftServer server, ServerPlayer intended,
                                             ChatGameCommand command, String twitchUser) {
        if (server == null || intended == null || command == null) return intended;
        if (!EvilCommands.isEvil(command)) return intended;
        if (HouseMode.isActive()) return intended;
        if (!ConfigManager.get().stream.duoStreamMode) return intended;
        ServerPlayer partner = StreamTargets.partner(server, intended).orElse(null);
        if (partner == null) return intended;
        if (StreamerShield.isActive()) return intended;
        if (PartnerHold.protects(partner)) return intended;
        if (DeathTracker.isEvilBlocked(partner.getGameProfile().name())) return intended;

        long now = System.currentTimeMillis();
        if (!armed && now - lastAt < INTERVAL_MS) return intended;
        armed = false;
        lastAt = now;

        String hit = partner.getGameProfile().name();
        String from = intended.getGameProfile().name();
        String cmd = command.getRussianName();
        AnnouncementService.announceCustom(intended,
                "§e§lПРОМАХ §7!" + cmd + " §8→ §f" + hit);
        AnnouncementService.announceCustom(partner,
                "§c§lПРОМАХ §7!" + cmd + " §8летело в §f" + from + " §8→ §cты");
        CommandAlertPayload.sendToStreamers(server, twitchUser == null ? "чат" : twitchUser,
                "ПРОМАХ → " + hit, "miss");
        StreamChronicle.note("промах !" + cmd + " " + from + " → " + hit);
        return partner;
    }
}
