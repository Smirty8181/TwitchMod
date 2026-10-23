package net.twitchcraft.stream;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.InvertControlsPayload;

/** !инверсия — WASD наоборот 10 секунд. */
public final class InvertControls {
    private InvertControls() {}

    public static void start(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        int ticks = 20 * 10;
        InvertControlsPayload.send(p, ticks);
        p.level().playSound(null, p.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.PLAYERS, 0.7f, 0.8f);
        AnnouncementService.announceCustom(p,
                "§5§lИнверсия WASD §710 сек §8· от §f" + ctx.twitchUser);
    }
}
