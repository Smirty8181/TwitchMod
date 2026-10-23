package net.twitchcraft.stream;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.twitchcraft.feedback.AnnouncementService;

/**
 * Один стример лег спать — утро для обоих, даже если второй в шахте.
 */
public final class DuoSleep {
    private DuoSleep() {}

    public static void tick(MinecraftServer server) {
        if (server == null) return;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        ServerPlayer sleeper = null;
        for (ServerPlayer p : StreamTargets.onlineStreamers(server)) {
            if (p.level() != overworld) continue;
            if (p.isSleepingLongEnough()) {
                sleeper = p;
                break;
            }
        }
        if (sleeper == null) return;

        long time = overworld.getDayTime();
        overworld.setDayTime(time + 24000L - time % 24000L);
        overworld.setWeatherParameters(0, 6000, false, false);
        overworld.resetWeatherCycle();

        for (ServerPlayer p : overworld.players()) {
            if (p.isSleeping()) p.stopSleepInBed(false, false);
        }

        overworld.playSound(null, sleeper.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.5f, 1.6f);
        String name = sleeper.getGameProfile().name();
        for (ServerPlayer p : StreamTargets.onlineStreamers(server)) {
            p.displayClientMessage(Component.literal(
                    "§e" + name + " §7лег спать — §fутро для обоих"), true);
        }
        AnnouncementService.announceCustom(sleeper, "§eСон за двоих §7— ночь пропущена");
    }
}
