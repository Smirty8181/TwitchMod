package net.twitchcraft.stream;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.feedback.AnnouncementService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Саб → именная свинья. Рейд → армия по числу зрителей, ники с чата рейда. */
public final class TwitchHypeEffects {
    private static final int RAID_CAP = 8;

    private TwitchHypeEffects() {}

    public static void onSub(MinecraftServer server, String targetPlayer, String subscriber,
                             boolean gifted, String gifter) {
        if (server == null) return;
        ServerPlayer target = resolve(server, targetPlayer);
        if (target == null) return;
        String nick = trunc(subscriber);
        if (nick.isBlank()) nick = "саб";
        spawnPig(target, nick);
        String line = gifted && gifter != null && !gifter.isBlank()
                ? "§d❤ САБ §f" + nick + " §8← §d" + trunc(gifter)
                : "§d❤ САБ §f" + nick + " §7→ свинья";
        AnnouncementService.announceCustom(target, line);
        net.twitchcraft.network.CommandAlertPayload.sendToStreamers(server, nick, gifted ? "гифт-саб" : "саб", "sub");
        StreamChronicle.note("саб " + nick + (gifted ? " (гифт)" : ""));
        TwitchCraftMod.LOGGER.info("TwitchCraft sub pig {}", nick);
    }

    public static void onRaid(MinecraftServer server, String targetPlayer, String raider, int viewers) {
        if (server == null) return;
        ServerPlayer target = resolve(server, targetPlayer);
        if (target == null) return;
        int n = Math.max(1, Math.min(RAID_CAP, viewers <= 0 ? 1 : viewers));
        String leader = trunc(raider);
        if (leader.isBlank()) leader = "рейд";
        Set<String> already = ChatViewerRegistry.snapshotKeys();
        List<RaidNameBinder.SlotLike> unnamed = new ArrayList<>();
        ServerLevel level = target.level();
        for (int i = 0; i < n; i++) {
            Entity mob = spawnRaidMob(target, i, n);
            if (mob == null) continue;
            if (i == 0) {
                nameHostile(mob, leader);
                GhostMobRegistry.register(mob, leader, "рейд", target.getGameProfile().name());
            } else {
                mob.setCustomName(Component.literal("§8рейд"));
                mob.setCustomNameVisible(true);
                unnamed.add(RaidNameBinder.slot(mob));
                GhostMobRegistry.register(mob, "рейд", "рейд", target.getGameProfile().name());
            }
        }
        RaidNameBinder.begin(unnamed, already, leader);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.COMMAND);
        if (bolt != null) {
            bolt.setPos(target.getX(), target.getY(), target.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, target.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.9f, 1.05f);
        AnnouncementService.announceCustom(target,
                "§c🚀 РЕЙД §f" + leader + " §c×" + n + (unnamed.isEmpty() ? "" : " §8· ники из чата"));
        net.twitchcraft.network.CommandAlertPayload.sendToStreamers(server, leader, "рейд ×" + n, "raid");
        StreamChronicle.note("рейд " + leader + " ×" + n);
        TwitchCraftMod.LOGGER.info("TwitchCraft raid {} ×{}", leader, n);
    }

    public static void onNewbie(MinecraftServer server, String targetPlayer, String user) {
        if (server == null) return;
        ServerPlayer target = resolve(server, targetPlayer);
        if (target == null) return;
        String nick = trunc(user);
        if (nick.isBlank()) return;
        spawnChicken(target, nick);
        AnnouncementService.announceCustom(target, "§e★ " + nick + " §7впервые на канале");
        net.twitchcraft.network.CommandAlertPayload.sendToStreamers(server, nick, "впервые на канале", "firstmsg");
        StreamChronicle.note("first-msg " + nick);
        TwitchCraftMod.LOGGER.info("TwitchCraft first-msg {}", nick);
    }

    public static void onFollow(MinecraftServer server, String targetPlayer, String user) {
        if (server == null) return;
        ServerPlayer target = resolve(server, targetPlayer);
        if (target == null) return;
        String nick = trunc(user);
        if (nick.isBlank()) nick = "кто-то";
        AnnouncementService.announceCustom(target, "§d+ " + nick + " §7зафоловил");
        net.twitchcraft.network.CommandAlertPayload.sendToStreamers(server, nick, "фоллоу", "follow");
        StreamChronicle.note("фоллоу " + nick);
        TwitchCraftMod.LOGGER.info("TwitchCraft follow {}", nick);
    }

    private static Entity spawnRaidMob(ServerPlayer target, int index, int total) {
        ServerLevel level = target.level();
        EntityType<?> type = index == 0
                ? EntityType.ZOMBIE
                : ((index % 3 == 1) ? EntityType.SKELETON : EntityType.ZOMBIE);
        Entity entity = type.create(level, EntitySpawnReason.COMMAND);
        if (entity == null) return null;
        double angle = (Math.PI * 2.0 * index) / Math.max(1, total);
        double dist = 5.5 + ThreadLocalRandom.current().nextDouble();
        entity.setPos(
                target.getX() + Math.cos(angle) * dist,
                target.getY(),
                target.getZ() + Math.sin(angle) * dist);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.setTarget(target);
        }
        if (entity instanceof Zombie z) z.setBaby(false);
        if (index == 0 && entity instanceof net.minecraft.world.entity.LivingEntity living) {
            buffRaidBoss(living);
        }
        level.addFreshEntity(entity);
        ChatMobBudget.track(entity);
        level.sendParticles(index == 0 ? ParticleTypes.FLAME : ParticleTypes.SMOKE,
                entity.getX(), entity.getY() + 1, entity.getZ(),
                index == 0 ? 18 : 8, 0.3, 0.4, 0.3, 0.02);
        return entity;
    }

    private static void buffRaidBoss(net.minecraft.world.entity.LivingEntity living) {
        var hp = living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(Math.max(40.0, hp.getBaseValue() * 2.6));
            living.setHealth(living.getMaxHealth());
        }
        var scale = living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(1.55);
        }
        var dmg = living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (dmg != null) {
            dmg.setBaseValue(dmg.getBaseValue() + 3.0);
        }
        living.setGlowingTag(true);
        if (living instanceof Mob mob) {
            mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_HELMET));
            mob.setDropChance(net.minecraft.world.entity.EquipmentSlot.HEAD, 0.12f);
        }
        living.refreshDimensions();
    }

    private static void spawnPig(ServerPlayer target, String nick) {
        ServerLevel level = target.level();
        Pig pig = EntityType.PIG.create(level, EntitySpawnReason.COMMAND);
        if (pig == null) return;
        double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
        pig.setPos(target.getX() + Math.cos(angle) * 2.2, target.getY(), target.getZ() + Math.sin(angle) * 2.2);
        pig.setCustomName(Component.literal("§d❤ " + nick));
        pig.setCustomNameVisible(true);
        pig.setPersistenceRequired();
        if (ThreadLocalRandom.current().nextFloat() < 0.22f) pig.setBaby(true);
        level.addFreshEntity(pig);
        ChatMobBudget.track(pig);
        GhostMobRegistry.register(pig, nick, "свинья", target.getGameProfile().name());
        level.playSound(null, pig.blockPosition(), SoundEvents.PIG_AMBIENT, SoundSource.NEUTRAL, 1.1f, 1.15f);
        level.sendParticles(ParticleTypes.HEART, pig.getX(), pig.getY() + 0.8, pig.getZ(),
                7, 0.3, 0.3, 0.3, 0.02);
    }

    private static void spawnChicken(ServerPlayer target, String nick) {
        ServerLevel level = target.level();
        Chicken chicken = EntityType.CHICKEN.create(level, EntitySpawnReason.COMMAND);
        if (chicken == null) return;
        double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
        chicken.setPos(target.getX() + Math.cos(angle) * 2.4, target.getY(), target.getZ() + Math.sin(angle) * 2.4);
        chicken.setCustomName(Component.literal("§e★ " + nick));
        chicken.setCustomNameVisible(true);
        chicken.setPersistenceRequired();
        if (ThreadLocalRandom.current().nextFloat() < 0.35f) chicken.setBaby(true);
        level.addFreshEntity(chicken);
        ChatMobBudget.track(chicken);
        GhostMobRegistry.register(chicken, nick, "цыплёнок", target.getGameProfile().name());
        level.playSound(null, chicken.blockPosition(), SoundEvents.CHICKEN_AMBIENT, SoundSource.NEUTRAL, 1.15f, 1.2f);
        level.sendParticles(ParticleTypes.END_ROD, chicken.getX(), chicken.getY() + 0.6, chicken.getZ(),
                10, 0.25, 0.4, 0.25, 0.01);
    }

    private static void nameHostile(Entity entity, String nick) {
        entity.setCustomName(Component.literal("§c§l⚔ " + nick));
        entity.setCustomNameVisible(true);
    }

    private static ServerPlayer resolve(MinecraftServer server, String targetPlayer) {
        if (targetPlayer != null && !targetPlayer.isBlank()) {
            var hit = StreamTargets.resolve(server, targetPlayer);
            if (hit.isPresent()) return hit.get();
        }
        return StreamTargets.resolve(server).orElse(null);
    }

    private static String trunc(String name) {
        if (name == null) return "";
        String t = name.trim();
        if (t.length() > 16) t = t.substring(0, 16);
        return t;
    }
}
