package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

/**
 * Насмешка: стример дразнит чат. Урон без КД 25с.
 * Смерть = армия. Выжил = заряд. Нельзя под щитом.
 */
public final class StreamerTaunt {
    private static final StreamerTaunt INSTANCE = new StreamerTaunt();
    private static final int DURATION_SEC = 25;
    private static final long COOLDOWN_MS = 3 * 60_000L;

    private boolean active;
    private long endTime;
    private long nextAt;
    private String targetName = "";

    private StreamerTaunt() {}

    public static StreamerTaunt get() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active && System.currentTimeMillis() < endTime;
    }

    public int secondsLeft() {
        if (!isActive()) return 0;
        return (int) Math.max(0, (endTime - System.currentTimeMillis() + 999) / 1000);
    }

    public String statusLine() {
        if (!isActive()) return "";
        return "§c§lНАСМЕШКА §f" + secondsLeft() + "s §7урон без КД";
    }

    public boolean startFromStreamer(MinecraftServer server, ServerPlayer player) {
        return start(server, player, false);
    }

    public boolean startFromChat(MinecraftServer server, ServerPlayer player, String viewer) {
        return start(server, player, true);
    }

    private boolean start(MinecraftServer server, ServerPlayer player, boolean byChat) {
        if (player == null) return false;
        if (isActive()) {
            AnnouncementService.announceCustom(player, "§7Насмешка уже идёт");
            return false;
        }
        if (StreamerShield.isActive()) {
            AnnouncementService.announceCustom(player, "§7Нельзя насмехаться под щитом");
            return false;
        }
        long now = System.currentTimeMillis();
        if (now < nextAt) {
            int left = (int) ((nextAt - now + 999) / 1000);
            AnnouncementService.announceCustom(player, "§7Насмешка через §f" + left + "с");
            return false;
        }
        active = true;
        endTime = now + DURATION_SEC * 1000L;
        nextAt = now + COOLDOWN_MS;
        targetName = player.getGameProfile().name();
        String who = byChat ? "чат начал" : "стример дразнит чат";
        AnnouncementService.announceCustom(player,
                "§c§lНАСМЕШКА! §7Урон без КД §e" + DURATION_SEC + "с §8· " + who);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "НАСМЕШКА! " + DURATION_SEC + "с пишите урон без КД. Смерть = армия.");
        pushHud(server);
        return true;
    }

    public void onStreamerDeath(MinecraftServer server, ServerPlayer player) {
        if (!isActive()) return;
        if (targetName != null && !targetName.equalsIgnoreCase(player.getGameProfile().name())) return;
        active = false;
        AnnouncementService.announceCustom(player, "§c§lНАСМЕШКА ПРОВАЛЕНА — армия!");
        TwitchCraftMod.getConnectionManager().sendChatMessage("Насмешка провалена! Армия чата.");
        CommandContext ctx = new CommandContext(server, player, "насмешка", "!army",
                ChatGameCommand.ARMY, true);
        EffectRunner.spawnArmy(ctx);
        ShowdownDirector.refresh(server);
    }

    public void tick(MinecraftServer server) {
        if (!active) return;
        if (System.currentTimeMillis() >= endTime) {
            active = false;
            StreamTargets.resolve(server).ifPresent(p -> {
                StreamerCharge.earn(server, p, "насмешка", 1, true);
                AnnouncementService.announceCustom(p, "§a§lНАСМЕШКА ВЫДЕРЖАНА §7+1 заряд");
            });
            TwitchCraftMod.getConnectionManager().sendChatMessage("Стример пережил насмешку.");
            ShowdownDirector.refresh(server);
        }
    }

    public void pushHud(MinecraftServer server) {
        if (!isActive()) return;
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "TAUNT",
                "НАСМЕШКА",
                "урон без КД",
                secondsLeft(),
                DURATION_SEC,
                0, 0,
                "смерть = армия",
                "выжить = заряд",
                true));
    }
}
