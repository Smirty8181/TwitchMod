package net.twitchcraft.hud;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.GoodCommands;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.stream.RandomEventScheduler;

public final class ChatVsStreamerBar {
    private ServerBossEvent bossEvent;
    private int chatHp;
    private int streamerHp;
    private int maxHp;
    private boolean lowHpTriggered;

    public void reset() {
        maxHp = Math.max(20, ConfigManager.get().stream.battleMaxHp);
        chatHp = maxHp;
        streamerHp = maxHp;
        lowHpTriggered = false;
    }

    public int chatHp() { return chatHp; }
    public int streamerHp() { return streamerHp; }
    public int maxHp() { return maxHp; }

    public void onStreamerDeath() {
        if (!enabled()) return;
        chatHp = Math.min(maxHp, chatHp + 15);
    }

    public void onGhostKilled() {
        if (!enabled()) return;
        streamerHp = Math.min(maxHp, streamerHp + 5);
        chatHp = Math.max(0, chatHp - 3);
    }

    public void onCommand(MinecraftServer server, ServerPlayer target, ChatGameCommand command) {
        if (!enabled()) return;
        if (command.getCategory() == ChatGameCommand.Category.MODERATOR) return;

        int damage = switch (command.getCategory()) {
            case MOB -> 5;
            case TNT -> 8;
            case CHAOS -> 7;
            case EFFECT -> 4;
            case MOVEMENT -> 3;
            case INVENTORY -> 4;
            default -> 2;
        };

        if (GoodCommands.isGood(command)) {
            streamerHp = Math.min(maxHp, streamerHp + boostForGood(command));
            chatHp = Math.max(0, chatHp - 5);
        } else {
            streamerHp = Math.max(0, streamerHp - damage);
            chatHp = Math.min(maxHp, chatHp + 2);
        }

        if (streamerHp <= 20 && !lowHpTriggered) {
            lowHpTriggered = true;
            RandomEventScheduler.fire(server, "§cЧат давит!");
        }

        if (streamerHp <= 0) {
            RandomEventScheduler.fire(server, "§c§lЧат победил!");
            resetRound(server, target, false);
        } else if (chatHp <= 0) {
            EffectRunner.pause(120);
            AnnouncementService.announceCustom(target, "§a§lСтример победил чат!");
            resetRound(server, target, true);
        }
    }

    private void resetRound(MinecraftServer server, ServerPlayer target, boolean streamerWon) {
        if (streamerWon) {
            chatHp = maxHp / 2;
            streamerHp = maxHp;
        } else {
            streamerHp = maxHp;
            chatHp = Math.max(maxHp / 3, chatHp - 20);
        }
        lowHpTriggered = false;
    }

    public void update(ServerPlayer player) {
        if (!enabled()) {
            remove(player);
            return;
        }

        if (maxHp <= 0) reset();

        if (bossEvent == null) {
            bossEvent = new ServerBossEvent(
                    Component.empty(),
                    BossEvent.BossBarColor.PURPLE,
                    BossEvent.BossBarOverlay.PROGRESS
            );
            bossEvent.setDarkenScreen(false);
            bossEvent.setPlayBossMusic(false);
            bossEvent.setCreateWorldFog(false);
        }

        float progress = maxHp <= 0 ? 0 : (float) streamerHp / maxHp;
        bossEvent.setProgress(Math.max(0f, Math.min(1f, progress)));
        bossEvent.setColor(streamerHp <= 25 ? BossEvent.BossBarColor.RED
                : streamerHp <= 50 ? BossEvent.BossBarColor.YELLOW
                : BossEvent.BossBarColor.GREEN);

        // Компактная подпись
        bossEvent.setName(Component.literal(
                "§d⚔ §c" + chatHp + " §8| §a" + streamerHp + "§7/§f" + maxHp));

        if (!bossEvent.getPlayers().contains(player)) {
            bossEvent.addPlayer(player);
        }
    }

    public void remove(ServerPlayer player) {
        if (bossEvent != null) {
            bossEvent.removePlayer(player);
        }
    }

    public void clear() {
        if (bossEvent != null) {
            bossEvent.removeAllPlayers();
            bossEvent = null;
        }
    }

    private int boostForGood(ChatGameCommand command) {
        return switch (command) {
            case SAVE -> 14;
            case HEAL, HELP -> 10;
            case REGEN, FEED -> 8;
            default -> 6;
        };
    }

    private boolean enabled() {
        return ConfigManager.get().stream.battleBarEnabled;
    }
}
