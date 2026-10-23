package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

/** Щит стримера: чат не может писать команды с уроном. */
public final class StreamerShield {
    private static long until;
    private static String reason = "";
    private static int durationSec;

    private StreamerShield() {}

    public static boolean isActive() {
        if (until <= 0) return false;
        if (System.currentTimeMillis() >= until) {
            until = 0;
            return false;
        }
        return true;
    }

    public static int secondsLeft() {
        if (!isActive()) return 0;
        return (int) Math.max(0, (until - System.currentTimeMillis() + 999) / 1000);
    }

    public static String statusLine() {
        if (!isActive()) return "";
        return "§6§l🛡 ЩИТ §e" + secondsLeft() + "s §7без урона";
    }

    public static void activate(MinecraftServer server, ServerPlayer target, int seconds, String why) {
        durationSec = Math.max(8, seconds);
        until = System.currentTimeMillis() + durationSec * 1000L;
        reason = why != null ? why : "щит";
        boolean afterDeath = why != null && why.contains("смерти");
        AnnouncementService.announceCustom(target, afterDeath
                ? "§6§l🛡 ПАУЗА " + durationSec + "с §7чат не бьёт — вставай"
                : "§6§l🛡 ЩИТ! §7Команды с уроном выключены на §e" + durationSec + "с");
        TwitchCraftMod.getConnectionManager().sendChatMessage(afterDeath
                ? "ПАУЗА " + durationSec + "с после смерти: урон из чата нельзя, пранки можно."
                : "ЩИТ СТРИМЕРА! " + durationSec + "с нельзя писать команды с уроном (!молния, мобы, TNT…). Пранки можно.");
        ShowdownDirector.refresh(server);
    }

    public static void clear(MinecraftServer server, ServerPlayer target, String announce) {
        boolean was = isActive();
        until = 0;
        if (was && target != null && announce != null) {
            AnnouncementService.announceCustom(target, announce);
        }
        ShowdownDirector.refresh(server);
    }

    public static void tick(MinecraftServer server) {
        if (until > 0 && System.currentTimeMillis() >= until) {
            until = 0;
            StreamTargets.resolve(server).ifPresent(p ->
                    AnnouncementService.announceCustom(p, "§7🛡 Щит закончился — урон снова можно"));
            TwitchCraftMod.getConnectionManager().sendChatMessage("Щит стримера спал. Команды с уроном снова можно.");
        }
    }

    public static void pushHud(MinecraftServer server) {
        if (!isActive()) return;
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "SHIELD",
                "ЩИТ",
                reason.isBlank() ? "урон запрещён" : reason,
                secondsLeft(),
                Math.max(1, durationSec),
                0, 0,
                "пранки ок",
                "урон блок",
                true));
    }
}
