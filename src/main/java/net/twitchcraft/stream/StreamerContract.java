package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

/** Контракт: стример спорит, что не умрёт. Бесплатно, но если сдох — чат получает армию. */
public final class StreamerContract {
    private static final StreamerContract INSTANCE = new StreamerContract();
    private static final int DURATION_SEC = 90;
    private static final long COOLDOWN_MS = 3 * 60_000L;

    private boolean active;
    private long endTime;
    private long nextAt;
    private String targetName = "";

    private StreamerContract() {}

    public static StreamerContract get() {
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
        return "§c§lКОНТРАКТ §f" + secondsLeft() + "s §7не умри";
    }

    public boolean start(MinecraftServer server, ServerPlayer player) {
        if (isActive()) {
            AnnouncementService.announceCustom(player, "§7Контракт уже идёт");
            return false;
        }
        long now = System.currentTimeMillis();
        if (now < nextAt) {
            int left = (int) ((nextAt - now + 999) / 1000);
            AnnouncementService.announceCustom(player, "§7Контракт через §f" + left + "с");
            return false;
        }
        active = true;
        endTime = now + DURATION_SEC * 1000L;
        nextAt = now + COOLDOWN_MS;
        targetName = player.getGameProfile().name();
        AnnouncementService.announceCustom(player,
                "§c§lКОНТРАКТ! §7Не умри §e" + DURATION_SEC + "с §7или чат получит армию");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "КОНТРАКТ! Стример спорит, что не умрёт " + DURATION_SEC + "с. Смерть = армия чата.");
        pushHud(server);
        return true;
    }

    public void onStreamerDeath(MinecraftServer server, ServerPlayer player) {
        if (!isActive()) return;
        if (targetName != null && !targetName.equalsIgnoreCase(player.getGameProfile().name())) return;
        active = false;
        AnnouncementService.announceCustom(player, "§c§lКОНТРАКТ ПРОВАЛЕН — армия чата!");
        TwitchCraftMod.getConnectionManager().sendChatMessage("Контракт провален! Армия чата.");
        CommandContext ctx = new CommandContext(server, player, "контракт", "!army",
                ChatGameCommand.ARMY, true);
        EffectRunner.spawnArmy(ctx);
        ShowdownDirector.refresh(server);
    }

    public void tick(MinecraftServer server) {
        if (!active) return;
        if (System.currentTimeMillis() >= endTime) {
            active = false;
            StreamTargets.resolve(server).ifPresent(p -> {
                StreamerCharge.earn(server, p, "контракт", 1);
                AnnouncementService.announceCustom(p, "§a§lКОНТРАКТ ВЫПОЛНЕН §7+1 заряд");
            });
            TwitchCraftMod.getConnectionManager().sendChatMessage("Контракт выполнен: стример выжил.");
            ShowdownDirector.refresh(server);
        }
    }

    public void pushHud(MinecraftServer server) {
        if (!isActive()) return;
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "CONTRACT",
                "КОНТРАКТ",
                "не умри или армия",
                secondsLeft(),
                DURATION_SEC,
                0, 0,
                "смерть = армия",
                "выжить = заряд",
                true));
    }
}
