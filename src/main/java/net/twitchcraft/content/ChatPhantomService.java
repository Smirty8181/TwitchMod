package net.twitchcraft.content;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.entity.ChatPhantomEntity;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.registry.ModEntities;
import net.twitchcraft.stream.ChatMobBudget;

/** Спавн босса «Призрак чата» за донат 1000₽+. */
public final class ChatPhantomService {
    private ChatPhantomService() {}

    public static boolean spawn(ServerLevel level, ServerPlayer target, String viewer) {
        if (level == null || target == null) return false;

        Vec3 base = target.position();
        double x = base.x + (level.random.nextDouble() - 0.5) * 6;
        double z = base.z + (level.random.nextDouble() - 0.5) * 6;
        double y = base.y + 5 + level.random.nextInt(3);

        ChatPhantomEntity phantom = ModEntities.CHAT_PHANTOM.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (phantom == null) {
            TwitchCraftMod.LOGGER.error("Chat Phantom: entity create failed");
            return false;
        }

        phantom.setPos(x, y, z);
        phantom.setYRot(level.random.nextFloat() * 360f);
        phantom.setTarget(target);

        if (!level.addFreshEntity(phantom)) {
            TwitchCraftMod.LOGGER.error("Chat Phantom: addFreshEntity failed at {}, {}, {}", x, y, z);
            return false;
        }

        phantom.initBoss(viewer, target);
        ChatMobBudget.track(phantom);

        level.playSound(null, target.blockPosition(), SoundEvents.PHANTOM_AMBIENT,
                SoundSource.HOSTILE, 1.5f, 0.45f);
        level.playSound(null, target.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK,
                SoundSource.HOSTILE, 0.9f, 0.6f);

        AnnouncementService.announceCustom(target,
                "§5§l☠ ПРИЗРАК ЧАТА §7— §d" + viewer + " §c§l120 HP");
        TwitchCraftMod.LOGGER.info("Chat Phantom spawned for donor {} targeting {}", viewer, target.getGameProfile().name());
        return true;
    }
}
