package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.feedback.AnnouncementService;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class CourtManager {
    private static final CourtManager INSTANCE = new CourtManager();

    private boolean active;
    private long endTime;
    private int guiltyVotes;
    private int innocentVotes;
    private final Map<String, Boolean> userVotes = new HashMap<>();
    private ServerPlayer deadPlayer;
    private String killerViewer;
    private String killerMobRu;

    private CourtManager() {}

    public static CourtManager get() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active && System.currentTimeMillis() < endTime;
    }

    public int secondsLeft() {
        if (!active) return 0;
        return (int) Math.max(0, (endTime - System.currentTimeMillis() + 999) / 1000);
    }

    public int guiltyVotes() { return guiltyVotes; }
    public int innocentVotes() { return innocentVotes; }

    public void start(MinecraftServer server, ServerPlayer player, String killerViewer, String killerMobRu) {
        if (!ConfigManager.get().stream.courtEnabled) {
            RevengeHandler.executeArmy(server);
            return;
        }

        this.deadPlayer = player;
        this.killerViewer = killerViewer != null ? killerViewer : "?";
        this.killerMobRu = killerMobRu != null ? killerMobRu : "моб";
        guiltyVotes = 0;
        innocentVotes = 0;
        userVotes.clear();
        active = true;
        endTime = System.currentTimeMillis() + ConfigManager.get().stream.courtDurationSeconds * 1000L;

        String msg = "§e§lСУД ЧАТА! §7" + player.getGameProfile().name() + " погиб! "
                + "§c!виноват §7/ §a!невиновен §7(" + ConfigManager.get().stream.courtDurationSeconds + "с)";
        AnnouncementService.announceCustom(player, msg);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "СУД ЧАТА! " + player.getGameProfile().name() + " погиб! Голосуй: !виноват или !невиновен (" + secondsLeft() + "с)");
    }

    public boolean castVote(String user, boolean guilty) {
        if (!isActive()) return false;
        if (userVotes.containsKey(user.toLowerCase(Locale.ROOT))) return false;
        userVotes.put(user.toLowerCase(Locale.ROOT), guilty);
        if (guilty) guiltyVotes++;
        else innocentVotes++;
        return true;
    }

    public void tick(MinecraftServer server) {
        if (!active) return;
        if (System.currentTimeMillis() >= endTime) {
            finish(server);
        }
    }

    private void finish(MinecraftServer server) {
        active = false;
        boolean chatGuilty = guiltyVotes >= innocentVotes;

        ServerPlayer target = deadPlayer != null ? deadPlayer : StreamTargets.resolve(server).orElse(null);
        if (target == null) return;

        if (chatGuilty) {
            AnnouncementService.announceCustom(target, "§c§lВИНОВАТЫ! §7(" + guiltyVotes + " vs " + innocentVotes + ") → армия!");
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    "Суд: ЧАТ ВИНОВАТ (" + guiltyVotes + " vs " + innocentVotes + ")! Реванш!");
            RevengeHandler.executeArmy(server);
        } else {
            var s = ConfigManager.get().stream;
            EffectRunner.cooldowns().applyPenalty(target.getGameProfile().name(),
                    s.courtPenaltyMultiplier,
                    s.courtPenaltyMinutes * 60_000L);
            AnnouncementService.announceCustom(target,
                    "§a§lНЕВИНОВЕН! §7КД чата x" + (int) s.courtPenaltyMultiplier + " на " + s.courtPenaltyMinutes + " мин");
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    "Суд: чат НЕВИНОВЕН! Штраф: КД x" + (int) s.courtPenaltyMultiplier + " на " + s.courtPenaltyMinutes + " мин");
        }
        deadPlayer = null;
    }

    public static boolean isCourtVote(String body) {
        return body.equals("виноват") || body.equals("guilty")
                || body.equals("невиновен") || body.equals("innocent");
    }

    public static boolean parseGuilty(String body) {
        return body.equals("виноват") || body.equals("guilty");
    }
}
