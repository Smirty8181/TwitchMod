package net.twitchcraft.stream;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Своя шутка: чат спамит !сикссевен / !67.
 * Семь раз за 45с — случайный муравей или титан.
 */
public final class SixSevenRitual {
    private static final int NEED = 7;
    private static final long WINDOW_MS = 45_000L;

    private static int count;
    private static long windowUntil;
    private static String lastUser = "";
    private static final Set<String> users = new HashSet<>();

    private SixSevenRitual() {}

    public static String statusLine() {
        if (count <= 0 || System.currentTimeMillis() >= windowUntil) return "";
        return "§e§l67 §f" + count + "/" + NEED;
    }

    public static void cast(CommandContext ctx) {
        if (ctx == null || ctx.target == null) return;
        if (!ConfigManager.get().stream.sixSevenEnabled) {
            AnnouncementService.announceCustom(ctx.target, "§767 выключен в настройках");
            return;
        }
        long now = System.currentTimeMillis();
        if (now >= windowUntil) {
            count = 0;
            users.clear();
            windowUntil = now + WINDOW_MS;
        }
        String who = ctx.twitchUser == null ? "чат" : ctx.twitchUser;
        String key = who.toLowerCase(Locale.ROOT);
        if (!users.add(key)) {
            AnnouncementService.announceCustom(ctx.target, "§7" + who + " уже в этом 67");
            return;
        }
        count = users.size();
        lastUser = who;
        ServerPlayer p = ctx.target;
        ServerLevel level = p.level();
        level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_COW_BELL.value(),
                SoundSource.PLAYERS, 1.0f, 0.7f + count * 0.08f);
        level.sendParticles(ParticleTypes.NOTE,
                p.getX(), p.getY() + 2.2, p.getZ(), 10 + count, 0.6, 0.4, 0.6, 0.3);
        p.setYRot(p.getYRot() + 35);
        p.hurtMarked = true;

        String line = count >= NEED ? "СИКС СЕВЕН" : "Сикс севен";
        GuideNpcManager.get().comment(ctx.server, p, line);
        AnnouncementService.announceCustom(p, "§e§l67 §f" + count + "/" + NEED + " §8· " + lastUser);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                lastUser + " 67! (" + count + "/" + NEED + ")");

        if (count >= NEED) {
            count = 0;
            users.clear();
            windowUntil = 0;
            finale(ctx);
        } else {
            ShowdownPayload.broadcast(ctx.server, new ShowdownPayload(
                    "SIXSEVEN", "67", lastUser, (int) ((windowUntil - now) / 1000),
                    45, count, NEED, "ников", "нужно", true));
        }
    }

    public static void tick(MinecraftServer server) {
        if (count > 0 && System.currentTimeMillis() >= windowUntil) {
            count = 0;
            users.clear();
            ShowdownDirector.refresh(server);
        }
    }

    public static void pushHud(MinecraftServer server) {
        if (count <= 0 || System.currentTimeMillis() >= windowUntil) return;
        int left = (int) Math.max(0, (windowUntil - System.currentTimeMillis() + 999) / 1000);
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "SIXSEVEN", "67", lastUser, left, 45, count, NEED,
                "ников", "нужно", true));
    }

    private static void finale(CommandContext ctx) {
        boolean ant = ThreadLocalRandom.current().nextBoolean();
        CommandContext inner = new CommandContext(ctx.server, ctx.target, lastUser,
                ant ? "!муравей" : "!титан",
                ant ? ChatGameCommand.ANT : ChatGameCommand.TITAN, true);
        if (ant) SizePrank.ant(inner);
        else SizePrank.titan(inner);
        ctx.target.level().playSound(null, ctx.target.blockPosition(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 0.8f);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "67 СОБРАН! Стример теперь " + (ant ? "муравей" : "титан") + ".");
    }
}
