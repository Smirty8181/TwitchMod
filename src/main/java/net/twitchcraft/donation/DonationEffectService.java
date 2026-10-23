package net.twitchcraft.donation;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;
import net.twitchcraft.content.ChatPhantomService;
import net.twitchcraft.stream.ChatMobBudget;
import net.twitchcraft.stream.ServerTaskQueue;
import net.minecraft.world.level.Level;
import net.twitchcraft.twitch.ChatCommandProcessor;

import java.util.concurrent.ThreadLocalRandom;

/** Эксклюзивные эффекты доната — усиленные и уникальные, не копии !команд. */
public final class DonationEffectService {
    private DonationEffectService() {}

    public static void execute(MinecraftServer server, ServerPlayer target, String donor,
                               TwitchCraftConfig.DonationTier tier, String donorMessage) {
        String cmd = tier.command != null ? tier.command.toLowerCase() : "";
        if ("wheel".equals(cmd)) return;
        if (DonationEffectCatalog.isExclusive(cmd)) {
            switch (cmd) {
                case "lightning" -> sponsorStorm(server, target, donor, donorMessage, tier.minAmount);
                case "goldrush" -> goldRush(server, target, donor, donorMessage, tier.minAmount);
                case "tntrain" -> moneyApocalypse(server, target, donor, donorMessage, tier.minAmount);
                case "disco" -> discoChaos(server, target, donor, donorMessage, tier.minAmount);
                case "army" -> donorLegion(server, target, donor, donorMessage, tier.minAmount);
                case "treasure" -> donorTreasure(server, target, donor, donorMessage, tier.minAmount);
                case "raid" -> donorRaid(server, target, donor, donorMessage, tier.minAmount);
                case "phantom" -> {
                    showCinematic(target, "§5§l☠ ПРИЗРАК ЧАТА",
                            "§d" + donor + " §7· §f" + DonationEffectCatalog.exclusiveSubtitle("phantom"), tier.minAmount);
                    ChatPhantomService.spawn(target.level(), target, donor);
                }
                default -> fallback(server, target, donor, cmd);
            }
            return;
        }
        fallback(server, target, donor, cmd);
    }

    public static void executeWheelWinner(MinecraftServer server, ServerPlayer target, String donor,
                                          TwitchCraftConfig.DonationTier tier, String donorMessage,
                                          ChatCommandProcessor processor) {
        String cmd = tier.command != null ? tier.command.toLowerCase() : "";
        WheelPrizeEffects.execute(server, target, donor, cmd, processor);
    }

    private static void fallback(MinecraftServer server, ServerPlayer target, String donor, String cmd) {
        if (cmd == null || cmd.isBlank()) return;
        new ChatCommandProcessor().executeById(server, donor, cmd,
                ConfigManager.get().donations.bypassCooldown, target.getGameProfile().name());
    }

    private static void sponsorStorm(MinecraftServer server, ServerPlayer target, String donor,
                                     String message, int tierMin) {
        ServerLevel level = target.level();
        showCinematic(target, "§e§l⚡ СПОНСОРСКАЯ ГРОЗА",
                "§6" + donor + " §7· §f" + DonationEffectCatalog.exclusiveSubtitle("lightning"), tierMin);

        level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER, 2f, 0.8f);

        double cx = target.getX();
        double cy = target.getY();
        double cz = target.getZ();
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI * 2 / 12;
            double px = cx + Math.cos(angle) * 5;
            double pz = cz + Math.sin(angle) * 5;
            level.sendParticles(ParticleTypes.END_ROD, px, cy + 0.5, pz, 1, 0, 0.4, 0, 0.02);
        }

        int[] delays = {0, 8, 16, 24, 32};
        for (int i = 0; i < delays.length; i++) {
            int idx = i;
            ServerTaskQueue.schedule(server, delays[i], () -> {
                double angle = idx * Math.PI * 2 / 5 + ThreadLocalRandom.current().nextDouble() * 0.4;
                double r = 2 + ThreadLocalRandom.current().nextDouble() * 4;
                strikeLightning(level, cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r);
                if (idx == 2) {
                    strikeLightning(level, cx, cy, cz);
                }
            });
        }

        ServerTaskQueue.schedule(server, 40, () ->
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy + 1, cz, 14, 2, 1, 2, 0.15));
        twitchMsg(donor, tierMin, "подарил спонсорскую грозу стримеру!");
    }

    private static void moneyApocalypse(MinecraftServer server, ServerPlayer target, String donor,
                                        String message, int tierMin) {
        ServerLevel level = target.level();
        showCinematic(target, "§6§l💰 ДЕНЕЖНЫЙ ДОЖД",
                "§e" + donor + " §7· §f12 TNT + золотой ливень", tierMin);

        level.playSound(null, target.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.AMBIENT, 1.5f, 0.7f);

        for (int wave = 0; wave < 3; wave++) {
            int w = wave;
            ServerTaskQueue.schedule(server, wave * 12, () -> spawnTntWave(level, target, 4 + w));
        }

        ItemStack goldStack = new ItemStack(Items.GOLD_INGOT);
        ItemParticleOption gold = new ItemParticleOption(ParticleTypes.ITEM, goldStack);
        for (int t = 0; t < 8; t++) {
            int tick = t * 5;
            ServerTaskQueue.schedule(server, tick, () -> {
                double x = target.getX();
                double z = target.getZ();
                level.sendParticles(gold, x, target.getY() + 10, z, 25, 3, 0.2, 3, 0.08);
                level.sendParticles(ParticleTypes.FIREWORK, x, target.getY() + 8, z, 8, 2, 1, 2, 0.05);
            });
        }

        ServerTaskQueue.schedule(server, 20, () -> spawnFirework(level, target));
        twitchMsg(donor, tierMin, "устроил денежный апокалипс!");
    }

    public static void runDonorLegion(MinecraftServer server, ServerPlayer target, String donor) {
        donorLegion(server, target, donor, "", 500);
    }

    private static void donorLegion(MinecraftServer server, ServerPlayer target, String donor,
                                    String message, int tierMin) {
        ServerLevel level = target.level();
        String shortDonor = donor.length() > 12 ? donor.substring(0, 11) + "…" : donor;
        showCinematic(target, "§c§l⚔ ЛЕГИОН ДОНАТЕРА",
                "§6" + donor + " §7· §fбронированный отряд", tierMin);

        level.playSound(null, target.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK,
                SoundSource.HOSTILE, 1.2f, 0.7f);

        spawnLegionMob(level, target, EntityType.ZOMBIE, donor, shortDonor, true);
        spawnLegionMob(level, target, EntityType.ZOMBIE, donor, shortDonor, true);
        spawnLegionMob(level, target, EntityType.ZOMBIE, donor, shortDonor, false);
        spawnLegionMob(level, target, EntityType.ZOMBIE, donor, shortDonor, false);
        spawnLegionMob(level, target, EntityType.SKELETON, donor, shortDonor, true);
        spawnLegionMob(level, target, EntityType.SKELETON, donor, shortDonor, false);
        spawnLegionMob(level, target, EntityType.SKELETON, donor, shortDonor, false);

        Husk leader = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
        if (leader != null) {
            leader.setPos(target.getX(), target.getY(), target.getZ());
            leader.setCustomName(Component.literal("§6§l⚔ " + shortDonor));
            leader.setCustomNameVisible(true);
            leader.setGlowingTag(true);
            leader.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            leader.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            leader.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            leader.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 120, 1));
            leader.setTarget(target);
            level.addFreshEntity(leader);
            ChatMobBudget.track(leader);
        }

        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(), target.getY() + 1, target.getZ(), 30, 2, 0.5, 2, 0.03);
        twitchMsg(donor, tierMin, "призвал легион донатера!");
    }

    private static void goldRush(MinecraftServer server, ServerPlayer target, String donor,
                                 String message, int tierMin) {
        ServerLevel level = target.level();
        showCinematic(target, "§6§l✦ ЗОЛОТАЯ ЛИХОРАДКА",
                "§e" + donor + " §7· §fзолото, изумруды, бафф", tierMin);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 1.1f, 1.3f);
        target.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 18, 1));
        target.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 25, 0));
        target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 12, 0));

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            dropLoot(level, target, new ItemStack(Items.GOLD_INGOT), 1.8);
        }
        for (int i = 0; i < 4; i++) {
            dropLoot(level, target, new ItemStack(Items.EMERALD), 2.2);
        }
        for (int i = 0; i < 6; i++) {
            dropLoot(level, target, new ItemStack(Items.GOLD_NUGGET, 3 + rnd.nextInt(4)), 2.4);
        }
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                target.getX(), target.getY() + 1, target.getZ(), 12, 1.2, 0.8, 1.2, 0.12);
        twitchMsg(donor, tierMin, "устроил золотую лихорадку!");
    }

    private static void discoChaos(MinecraftServer server, ServerPlayer target, String donor,
                                   String message, int tierMin) {
        ServerLevel level = target.level();
        showCinematic(target, "§d§l✦ ДИСКО-ХАОС",
                "§d" + donor + " §7· §fфейерверки и крутилка", tierMin);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 16, 0));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20 * 10, 1));
        target.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 12, 0));

        for (int i = 0; i < 8; i++) {
            int tick = i * 4;
            ServerTaskQueue.schedule(server, tick, () -> {
                if (!target.isAlive()) return;
                target.setYRot(target.getYRot() + 50);
                target.setYHeadRot(target.getYRot());
                level.sendParticles(ParticleTypes.NOTE,
                        target.getX(), target.getY() + 1.4, target.getZ(), 10, 1.2, 0.6, 1.2, 0.2);
                level.playSound(null, target.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(),
                        SoundSource.PLAYERS, 0.9f, 0.8f + ThreadLocalRandom.current().nextFloat() * 0.8f);
                if (tick % 8 == 0) spawnFirework(level, target);
            });
        }
        twitchMsg(donor, tierMin, "включил диско-хаос!");
    }

    private static void donorTreasure(MinecraftServer server, ServerPlayer target, String donor,
                                      String message, int tierMin) {
        ServerLevel level = target.level();
        showCinematic(target, "§b§l✦ СУНДУК ДОНАТЕРА",
                "§b" + donor + " §7· §fалмазы и шанс тотема", tierMin);
        level.playSound(null, target.blockPosition(), SoundEvents.CHEST_OPEN,
                SoundSource.PLAYERS, 1.1f, 0.9f);

        dropLoot(level, target, new ItemStack(Items.DIAMOND, 2), 1.4);
        dropLoot(level, target, new ItemStack(Items.GOLDEN_APPLE, 2), 1.6);
        dropLoot(level, target, new ItemStack(Items.GOLD_INGOT, 6), 1.8);
        dropLoot(level, target, new ItemStack(Items.EXPERIENCE_BOTTLE, 4), 2.0);
        if (ThreadLocalRandom.current().nextInt(100) < 22) {
            dropLoot(level, target, new ItemStack(Items.TOTEM_OF_UNDYING), 1.2);
        }
        ExperienceOrb.award(level, target.position(), 28);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                target.getX(), target.getY() + 1, target.getZ(), 24, 1.4, 0.6, 1.4, 0.05);
        twitchMsg(donor, tierMin, "бросил сундук донатера!");
    }

    private static void donorRaid(MinecraftServer server, ServerPlayer target, String donor,
                                  String message, int tierMin) {
        ServerLevel level = target.level();
        String shortDonor = donor.length() > 12 ? donor.substring(0, 11) + "…" : donor;
        showCinematic(target, "§c§l✦ РЕЙД ДОНАТЕРА",
                "§c" + donor + " §7· §fравагер и пиллагеры", tierMin);
        level.playSound(null, target.blockPosition(), SoundEvents.RAID_HORN.value(),
                SoundSource.HOSTILE, 1.4f, 0.85f);

        spawnRaider(level, target, EntityType.PILLAGER, shortDonor, Items.CROSSBOW);
        spawnRaider(level, target, EntityType.PILLAGER, shortDonor, Items.CROSSBOW);
        spawnRaider(level, target, EntityType.VINDICATOR, shortDonor, Items.IRON_AXE);
        spawnRaider(level, target, EntityType.VINDICATOR, shortDonor, Items.IRON_AXE);
        spawnRaider(level, target, EntityType.EVOKER, shortDonor, Items.TOTEM_OF_UNDYING);

        Ravager ravager = EntityType.RAVAGER.create(level, EntitySpawnReason.COMMAND);
        if (ravager != null) {
            ravager.setPos(target.getX() + 2, target.getY(), target.getZ() + 2);
            ravager.setCustomName(Component.literal("§c§l⚔ " + shortDonor));
            ravager.setCustomNameVisible(true);
            ravager.setGlowingTag(true);
            ravager.setTarget(target);
            level.addFreshEntity(ravager);
            ChatMobBudget.track(ravager);
        }
        twitchMsg(donor, tierMin, "вызвал рейд донатера!");
    }

    private static void spawnRaider(ServerLevel level, ServerPlayer target, EntityType<?> type,
                                    String shortDonor, net.minecraft.world.item.Item weapon) {
        Entity entity = type.create(level, EntitySpawnReason.COMMAND);
        if (entity == null) return;
        entity.setPos(target.getX() + level.random.nextInt(7) - 3,
                target.getY(), target.getZ() + level.random.nextInt(7) - 3);
        entity.setCustomName(Component.literal("§7[" + shortDonor + "]"));
        entity.setCustomNameVisible(true);
        if (entity instanceof AbstractIllager illager) {
            illager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
            illager.setTarget(target);
        }
        level.addFreshEntity(entity);
        ChatMobBudget.track(entity);
    }

    private static void dropLoot(ServerLevel level, ServerPlayer target, ItemStack stack, double spread) {
        double x = target.getX() + (level.random.nextDouble() - 0.5) * spread * 2;
        double z = target.getZ() + (level.random.nextDouble() - 0.5) * spread * 2;
        ItemEntity item = new ItemEntity(level, x, target.getY() + 0.6, z, stack);
        item.setPickUpDelay(12);
        level.addFreshEntity(item);
    }

    private static void spawnLegionMob(ServerLevel level, ServerPlayer target, EntityType<?> type,
                                       String donor, String shortDonor, boolean heavy) {
        Entity entity = type.create(level, EntitySpawnReason.COMMAND);
        if (entity == null) return;
        double x = target.getX() + level.random.nextInt(7) - 3;
        double z = target.getZ() + level.random.nextInt(7) - 3;
        entity.setPos(x, target.getY(), z);
        entity.setCustomName(Component.literal("§7[" + shortDonor + "]"));
        entity.setCustomNameVisible(true);
        if (entity instanceof Zombie zombie) {
            zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(heavy ? Items.IRON_HELMET : Items.LEATHER_HELMET));
            zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(heavy ? Items.IRON_CHESTPLATE : Items.LEATHER_CHESTPLATE));
            zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(heavy ? Items.IRON_SWORD : Items.STONE_SWORD));
            if (heavy) zombie.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 90, 0));
            zombie.setTarget(target);
        } else if (entity instanceof Skeleton sk) {
            sk.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
            sk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            if (heavy) sk.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 90, 0));
            sk.setTarget(target);
        }
        level.addFreshEntity(entity);
        ChatMobBudget.track(entity);
    }

    private static void strikeLightning(ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.COMMAND);
        if (bolt != null) {
            bolt.setPos(x, y, z);
            level.addFreshEntity(bolt);
        }
    }

    private static void spawnTntWave(ServerLevel level, ServerPlayer target, int count) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            double x = target.getX() + rnd.nextDouble(-4, 4);
            double z = target.getZ() + rnd.nextDouble(-4, 4);
            double y = target.getY() + 14 + rnd.nextInt(8);
            PrimedTnt tnt = new PrimedTnt(level, x, y, z, null);
            tnt.setFuse(35 + rnd.nextInt(25));
            level.addFreshEntity(tnt);
        }
        level.playSound(null, target.blockPosition(), SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1f, 0.8f);
    }

    private static void spawnFirework(ServerLevel level, ServerPlayer target) {
        ItemStack stack = Items.FIREWORK_ROCKET.getDefaultInstance();
        FireworkRocketEntity fw = new FireworkRocketEntity(level, stack,
                target.getX(), target.getY() + 12, target.getZ(), true);
        level.addFreshEntity(fw);
    }

    private static void showCinematic(ServerPlayer target, String title, String subtitle, int tierMin) {
        var fb = ConfigManager.get().feedback;
        if (fb.showTitleInGame && tierMin >= 50) {
            int stay = tierMin >= 200 ? 50 : 35;
            target.connection.send(new ClientboundSetTitlesAnimationPacket(8, stay, 12));
            target.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
            target.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
        }
        if (fb.showActionBar) {
            target.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(
                    "§6§l💰 ДОНАТ §7— " + subtitle)));
        }
    }

    private static void twitchMsg(String donor, int tierMin, String action) {
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "💰 " + donor + " (" + tierMin + "₽) " + action);
    }
}
