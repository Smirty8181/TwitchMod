package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.commands.EvilCommands;
import net.twitchcraft.commands.GoodCommands;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

/** Вызов: добро vs урон. Если начал стример — чат в форе, ничья = проигрыш стримера. */
public final class StreamerChallenge {
    private static final StreamerChallenge INSTANCE = new StreamerChallenge();
    private static final long CHAT_CD_MS = 90_000;

    private boolean active;
    private boolean byChat;
    private long endTime;
    private int durationSec;
    private int good;
    private int damage;
    private long nextChatAt;

    private StreamerChallenge() {}

    public static StreamerChallenge get() {
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
        return "§d§lВЫЗОВ §f" + secondsLeft() + "s §a+" + good + " §c-" + damage;
    }

    public boolean startFromStreamer(MinecraftServer server, ServerPlayer player) {
        if (isActive()) {
            AnnouncementService.announceCustom(player, "§7Вызов уже идёт");
            return false;
        }
        if (!StreamerCharge.spend(player, 1, "вызов")) return false;
        return start(server, player, false);
    }

    public boolean startFromChat(MinecraftServer server, ServerPlayer player, String viewer) {
        long now = System.currentTimeMillis();
        if (now < nextChatAt) {
            int left = (int) ((nextChatAt - now + 999) / 1000);
            if (player != null) {
                AnnouncementService.announceCustom(player, "§7!вызов через §f" + left + "с");
            }
            return false;
        }
        nextChatAt = now + CHAT_CD_MS;
        TwitchCraftMod.getConnectionManager().sendChatMessage(viewer + " бросил вызов!");
        return start(server, player, true);
    }

    public boolean start(MinecraftServer server, ServerPlayer player, boolean chatStarted) {
        if (isActive()) {
            AnnouncementService.announceCustom(player, "§7Вызов уже идёт");
            return false;
        }
        durationSec = Math.max(12, ConfigManager.get().stream.challengeSeconds);
        endTime = System.currentTimeMillis() + durationSec * 1000L;
        good = 0;
        damage = 0;
        active = true;
        byChat = chatStarted;
        String rule = chatStarted
                ? "честно: больше добра или урона"
                : "чат в форе: добро должно обогнать урон на 2";
        AnnouncementService.announceCustom(player,
                "§d§lВЫЗОВ! §7" + rule + " §8· §f" + durationSec + "с");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "ВЫЗОВ " + durationSec + "с! !хил против урона. " + rule);
        pushHud(server);
        return true;
    }

    public void onCommand(ChatGameCommand command) {
        if (!isActive() || command == null) return;
        if (GoodCommands.isGood(command)) good++;
        else if (EvilCommands.isEvil(command)) damage++;
    }

    public void tick(MinecraftServer server) {
        if (!active) return;
        if (System.currentTimeMillis() >= endTime) {
            finish(server);
        }
    }

    public void pushHud(MinecraftServer server) {
        if (!isActive()) return;
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "CHALLENGE",
                "ВЫЗОВ",
                byChat ? "честный" : "чат +2 фора",
                secondsLeft(),
                durationSec,
                damage,
                good,
                "⚔ урон",
                "♥ добро",
                true));
    }

    private void finish(MinecraftServer server) {
        active = false;
        boolean streamerWins;
        if (good + damage == 0) {
            streamerWins = false;
        } else if (byChat) {
            streamerWins = good > damage;
        } else {
            streamerWins = good >= damage + 2;
        }

        StreamTargets.resolve(server).ifPresent(p -> {
            if (streamerWins) {
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
                if (byChat) {
                    p.getInventory().add(new ItemStack(Items.GOLDEN_APPLE));
                }
                AnnouncementService.announceCustom(p,
                        "§a§lВЫЗОВ: ДОБРО §a" + good + " §7vs §c" + damage);
                TwitchCraftMod.getConnectionManager().sendChatMessage(
                        "Вызов: добро " + good + ":" + damage);
            } else {
                CommandContext ctx = new CommandContext(server, p, "вызов", "!clown",
                        ChatGameCommand.CLOWN, true);
                EffectRunner.clown(ctx);
                if (!byChat) {
                    EffectRunner.startBomb(ctx);
                } else {
                    EffectRunner.junk(ctx);
                }
                AnnouncementService.announceCustom(p,
                        "§c§lВЫЗОВ: УРОН §c" + damage + " §7vs §a" + good
                                + (good + damage == 0 ? " §8(никто не писал)" : ""));
                TwitchCraftMod.getConnectionManager().sendChatMessage(
                        "Вызов: урон " + damage + ":" + good
                                + (byChat ? "" : " — бомба, стример начал сам"));
            }
        });
        ShowdownDirector.refresh(server);
    }
}
