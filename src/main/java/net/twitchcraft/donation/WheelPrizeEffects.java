package net.twitchcraft.donation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.stream.ChatMobBudget;
import net.twitchcraft.stream.ServerTaskQueue;
import net.twitchcraft.stream.SpawnGuard;
import net.twitchcraft.twitch.ChatCommandProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Мега-эффекты призов донат-рулетки 500₽ — усиленные версии !команд. */
public final class WheelPrizeEffects {
    private WheelPrizeEffects() {}

    public static void execute(MinecraftServer server, ServerPlayer target, String donor,
                               String commandId, ChatCommandProcessor processor) {
        String cmd = commandId != null ? commandId.toLowerCase() : "";
        showWheelCinematic(target, donor, cmd);

        ServerLevel level = target.level();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY() + 1, target.getZ(),
                16, 1.5, 1.2, 1.5, 0.15);
        level.playSound(null, target.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.PLAYERS, 1.4f, 0.9f);

        switch (cmd) {
            case "launch" -> megaLaunch(server, target, donor);
            case "curse" -> megaCurse(server, target, donor);
            case "meteor" -> megaMeteor(server, target, donor);
            case "wither" -> megaWither(server, target, donor);
            case "army" -> megaArmy(server, target, donor);
            case "tntrain" -> megaTntRain(server, target, donor);
            case "boss" -> megaBoss(server, target, donor);
            case "dragon" -> megaDragon(server, target, donor);
            case "prison" -> megaPrison(server, target, donor);
            case "creeperstorm" -> megaCreeperStorm(server, target, donor);
            case "event" -> megaEvent(server, target, donor);
            case "spin" -> megaSpin(server, target, donor);
            case "clear" -> megaClear(server, target, donor);
            case "trap" -> megaTrap(server, target, donor);
            case "chicken" -> megaChicken(server, target, donor);
            case "anvil" -> megaAnvil(server, target, donor);
            case "storm" -> megaStorm(server, target, donor);
            case "hype" -> megaHype(server, target, donor);
            case "dome" -> megaDome(server, target, donor);
            default -> {
                if (!cmd.isBlank()) {
                    processor.executeById(server, donor, cmd,
                            ConfigManager.get().donations.bypassCooldown, target.getGameProfile().name());
                }
                finaleBurst(level, target);
            }
        }
        twitchMsg(donor, wheelAction(cmd));
    }

    private static void megaLaunch(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS, 1.5f, 0.6f);

        double vy = Math.max(7.5, (320.0 - target.getY()) / 10.0);
        target.setDeltaMovement(0, vy, 0);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80, 3, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, true));

        for (int wave = 0; wave < 12; wave++) {
            final int w = wave;
            int tick = wave * 5;
            ServerTaskQueue.schedule(server, tick, () -> {
                double x = target.getX();
                double y = target.getY();
                double z = target.getZ();
                level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 6, 0.4, 0.2, 0.4, 0.06);
                level.sendParticles(ParticleTypes.END_ROD, x, y - 0.5, z, 4, 0.3, 0.5, 0.3, 0.04);
                if (w % 3 == 0) {
                    level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.FIREWORK_ROCKET_BLAST,
                            SoundSource.PLAYERS, 0.8f, 1.2f + w * 0.05f);
                }
            });
        }

        ServerTaskQueue.schedule(server, 55, () -> {
            for (int i = 0; i < 5; i++) {
                spawnFirework(level, target.getX() + i - 2, target.getY() + 2, target.getZ());
            }
            level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(), target.getZ(),
                    30, 1.5, 0.5, 1.5, 0.08);
        });
    }

    private static void megaCurse(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.HOSTILE, 1.5f, 0.5f);

        var lookup = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> binding = lookup.getOrThrow(Enchantments.BINDING_CURSE);
        Holder<Enchantment> vanishing = lookup.getOrThrow(Enchantments.VANISHING_CURSE);
        String donorShort = truncate(donor, 14);

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        List<EquipmentSlot> worn = new ArrayList<>();
        for (EquipmentSlot slot : slots) {
            if (!target.getItemBySlot(slot).isEmpty()) worn.add(slot);
        }

        int cursed = 0;
        if (!worn.isEmpty()) {
            List<EquipmentSlot> picks = new ArrayList<>(worn);
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            int count = Math.min(3, picks.size());
            for (int i = 0; i < count; i++) {
                EquipmentSlot slot = picks.remove(rnd.nextInt(picks.size()));
                ItemStack stack = target.getItemBySlot(slot).copy();
                ItemEnchantments.Mutable ench = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                ench.set(binding, 1);
                ench.set(vanishing, 1);
                stack.set(DataComponents.ENCHANTMENTS, ench.toImmutable());
                stack.set(DataComponents.CUSTOM_NAME,
                        Component.literal("§4§l☠ Проклятие §7· §c" + donorShort));
                target.setItemSlot(slot, stack);
                cursed++;
            }
        } else {
            ItemStack stack = new ItemStack(Items.LEATHER_BOOTS);
            ItemEnchantments.Mutable ench = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            ench.set(binding, 1);
            ench.set(vanishing, 1);
            stack.set(DataComponents.ENCHANTMENTS, ench.toImmutable());
            stack.set(DataComponents.CUSTOM_NAME,
                    Component.literal("§4§l☠ Проклятие §7· §c" + donorShort));
            target.getInventory().add(stack);
            cursed = 1;
        }

        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));

        for (int t = 0; t < 6; t++) {
            int tick = t * 8;
            ServerTaskQueue.schedule(server, tick, () -> {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1, target.getZ(),
                        25, 1.2, 0.8, 1.2, 0.03);
                level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY(), target.getZ(),
                        15, 0.8, 0.5, 0.8, 0.02);
                level.playSound(null, target.blockPosition(), SoundEvents.WITHER_AMBIENT,
                        SoundSource.HOSTILE, 0.6f, 0.7f);
            });
        }

        TwitchCraftMod.LOGGER.info("Wheel curse: {} cursed {} slots for {}", donor, cursed, target.getName().getString());
    }

    private static void megaMeteor(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 1.2f, 0.6f);

        double cx = target.getX();
        double cy = target.getY();
        double cz = target.getZ();
        int[] delays = {0, 18, 36};
        for (int i = 0; i < delays.length; i++) {
            final int idx = i;
            int d = delays[i];
            ServerTaskQueue.schedule(server, d, () -> {
                BlockPos pos = BlockPos.containing(cx + ThreadLocalRandom.current().nextInt(-4, 5),
                        cy + 18 + idx * 4, cz + ThreadLocalRandom.current().nextInt(-4, 5));
                BlockState state = Blocks.MAGMA_BLOCK.defaultBlockState();
                FallingBlockEntity meteor = FallingBlockEntity.fall(level, pos, state);
                if (meteor != null) {
                    meteor.time = 1;
                    level.addFreshEntity(meteor);
                }
                level.sendParticles(ParticleTypes.LAVA, cx, cy + 2, cz, 20, 2, 1, 2, 0.02);
                level.explode(null, cx, cy, cz, 2.2f + idx * 0.4f, Level.ExplosionInteraction.NONE);
            });
        }
    }

    private static void megaWither(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 1f, 0.85f);

        WitherBoss wither = EntityType.WITHER.create(level, EntitySpawnReason.COMMAND);
        if (wither != null) {
            wither.setPos(target.getX() + 3, target.getY() + 2, target.getZ() + 3);
            wither.setCustomName(Component.literal("§8§l💀 " + truncate(donor, 12)));
            wither.setCustomNameVisible(true);
            level.addFreshEntity(wither);
            ChatMobBudget.track(wither);
        }

        ringParticles(level, target, ParticleTypes.SOUL, 3.5, 50);
        ServerTaskQueue.schedule(server, 20, () ->
                level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 2, target.getZ(),
                        60, 2, 1, 2, 0.05));
    }

    private static void megaArmy(MinecraftServer server, ServerPlayer target, String donor) {
        DonationEffectService.runDonorLegion(server, target, donor);
        CommandContext ctx = new CommandContext(server, target, donor, "!army",
                ChatGameCommand.ARMY, true);
        for (int i = 0; i < 4; i++) EffectRunner.spawnZombie(ctx);
        for (int i = 0; i < 3; i++) EffectRunner.spawnSkeleton(ctx);
        EffectRunner.spawnGhast(ctx);
    }

    private static void megaTntRain(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.TNT_PRIMED,
                SoundSource.HOSTILE, 1.5f, 0.7f);

        for (int wave = 0; wave < 4; wave++) {
            int w = wave;
            ServerTaskQueue.schedule(server, wave * 14, () -> spawnTntWave(level, target, 5 + w));
        }

        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        ItemParticleOption goldParticle = new ItemParticleOption(ParticleTypes.ITEM, gold);
        for (int t = 0; t < 10; t++) {
            ServerTaskQueue.schedule(server, t * 4, () ->
                    level.sendParticles(goldParticle, target.getX(), target.getY() + 12, target.getZ(),
                            20, 2.5, 0.3, 2.5, 0.1));
        }
    }

    private static void megaBoss(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 1f, 0.8f);

        CommandContext ctx = new CommandContext(server, target, donor, "!boss", ChatGameCommand.BOSS, true);
        EffectRunner.boss(ctx);
        ServerTaskQueue.schedule(server, 30, () -> {
            EffectRunner.boss(ctx);
            level.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1, target.getZ(),
                    3, 0, 0, 0, 0);
        });
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false, true));
    }

    private static void megaDragon(MinecraftServer server, ServerPlayer target, String donor) {
        if (target.level().dimension() != net.minecraft.world.level.Level.END) {
            megaMeteor(server, target, donor);
            return;
        }
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE, 1.2f, 0.7f);

        EnderDragon dragon = EntityType.ENDER_DRAGON.create(level, EntitySpawnReason.COMMAND);
        if (dragon != null) {
            dragon.setPos(target.getX(), target.getY() + 12, target.getZ());
            dragon.setCustomName(Component.literal("§5§l🐉 " + truncate(donor, 12)));
            dragon.setCustomNameVisible(true);
            level.addFreshEntity(dragon);
            ChatMobBudget.track(dragon);
        }
        ringParticles(level, target, ParticleTypes.PORTAL, 4, 40);
        level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 2, target.getZ(),
                80, 2, 1, 2, 0.5);
    }

    private static void megaPrison(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS, 1.5f, 0.5f);

        BlockPos center = target.blockPosition();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2 || dy == 3;
                    if (wall) {
                        var block = dy == 3 ? Blocks.IRON_BARS : Blocks.IRON_BARS;
                        if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && dy < 3) {
                            block = Blocks.OBSIDIAN;
                        }
                        SpawnGuard.replace(level, center.offset(dx, dy, dz), block.defaultBlockState());
                    }
                }
            }
        }
        target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 400, 1, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 0, false, false, true));
        level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + 1, target.getZ(),
                40, 1, 0.5, 1, 0.1);
    }

    private static void megaCreeperStorm(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.CREEPER_PRIMED,
                SoundSource.HOSTILE, 1.5f, 0.6f);

        for (int i = 0; i < 6; i++) {
            int delay = i * 6;
            ServerTaskQueue.schedule(server, delay, () -> {
                Entity entity = EntityType.CREEPER.create(level, EntitySpawnReason.COMMAND);
                if (entity instanceof Creeper creeper) {
                    double x = target.getX() + level.random.nextInt(7) - 3;
                    double z = target.getZ() + level.random.nextInt(7) - 3;
                    creeper.setPos(x, target.getY(), z);
                    creeper.setCustomName(Component.literal("§a[" + truncate(donor, 10) + "]"));
                    creeper.setCustomNameVisible(true);
                    creeper.setGlowingTag(true);
                    level.addFreshEntity(creeper);
                    ChatMobBudget.track(creeper);
                }
            });
        }
        ringParticles(level, target, ParticleTypes.EXPLOSION, 3, 15);
    }

    private static void megaEvent(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.RAID_HORN.value(),
                SoundSource.HOSTILE, 1.2f, 0.9f);

        ServerTaskQueue.schedule(server, 0, () -> megaMeteor(server, target, donor));
        ServerTaskQueue.schedule(server, 45, () -> megaStorm(server, target, donor));
        ServerTaskQueue.schedule(server, 90, () -> megaTntRain(server, target, donor));
    }

    private static void megaSpin(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.2f, 0.4f);

        target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 2, false, false, true));
        float startYaw = target.getYRot();
        float startPitch = target.getXRot();

        for (int t = 1; t <= 160; t++) {
            int tick = t;
            ServerTaskQueue.schedule(server, t, () -> {
                if (!target.isAlive()) return;
                target.setYRot(startYaw + tick * 12f);
                target.setXRot(Mth.clamp(startPitch + 35f * Mth.sin(tick * 0.12f), -85f, 85f));
                if (tick % 8 == 0) {
                    level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1, target.getZ(),
                            8, 0.5, 0.5, 0.5, 0.3);
                }
            });
        }

        ServerTaskQueue.schedule(server, 80, () -> {
            double dx = level.random.nextDouble() * 2 - 1;
            double dz = level.random.nextDouble() * 2 - 1;
            target.setDeltaMovement(dx * 1.5, 1.2, dz * 1.5);
            target.hurtMarked = true;
        });
    }

    private static void megaClear(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 1.5f, 0.5f);

        for (int i = 0; i < target.getInventory().getContainerSize(); i++) {
            ItemStack stack = target.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, stack.copy());
                level.sendParticles(particle, target.getX(), target.getY() + 1.5, target.getZ(),
                        3, 0.8, 0.5, 0.8, 0.15);
            }
        }
        target.getInventory().clearContent();
        level.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + 1, target.getZ(),
                50, 1, 0.8, 1, 0.1);
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, true));
    }

    private static void megaTrap(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON,
                SoundSource.BLOCKS, 1.5f, 0.6f);

        BlockPos below = target.blockPosition().below();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                SpawnGuard.replace(level, below.offset(dx, 0, dz), Blocks.TNT.defaultBlockState());
            }
        }

        for (int i = 0; i < 4; i++) {
            int fuse = 40 + i * 15;
            ServerTaskQueue.schedule(server, 0, () -> {
                PrimedTnt tnt = new PrimedTnt(level, target.getX(), target.getY(), target.getZ(), null);
                tnt.setFuse(fuse);
                level.addFreshEntity(tnt);
            });
        }
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, true));
    }

    private static void megaChicken(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.CHICKEN_AMBIENT,
                SoundSource.NEUTRAL, 1.5f, 0.7f);

        CommandContext ctx = new CommandContext(server, target, donor, "!chicken", ChatGameCommand.CHICKEN, true);
        for (int wave = 0; wave < 3; wave++) {
            int w = wave;
            ServerTaskQueue.schedule(server, wave * 10, () -> {
                for (int i = 0; i < 6; i++) {
                    Entity chicken = EntityType.CHICKEN.create(level, EntitySpawnReason.COMMAND);
                    if (chicken != null) {
                        chicken.setPos(target.getX() + level.random.nextInt(9) - 4,
                                target.getY() + 8 + w * 2, target.getZ() + level.random.nextInt(9) - 4);
                        chicken.setCustomName(Component.literal("§e🐔 " + truncate(donor, 8)));
                        chicken.setCustomNameVisible(true);
                        level.addFreshEntity(chicken);
                        ChatMobBudget.track(chicken);
                    }
                }
            });
        }
    }

    private static void megaAnvil(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.BLOCKS, 1.2f, 0.6f);

        int[] heights = {10, 14, 18};
        for (int h : heights) {
            ServerTaskQueue.schedule(server, h == 10 ? 0 : h == 14 ? 20 : 40, () -> {
                BlockPos pos = BlockPos.containing(target.getX(), target.getY() + h, target.getZ());
                FallingBlockEntity anvil = FallingBlockEntity.fall(level, pos, Blocks.ANVIL.defaultBlockState());
                if (anvil != null) level.addFreshEntity(anvil);
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 2, target.getZ(),
                        15, 0.5, 0.5, 0.5, 0.1);
            });
        }
    }

    private static void megaStorm(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER, 2f, 0.75f);
        level.setWeatherParameters(0, 400, true, true);

        double cx = target.getX();
        double cy = target.getY();
        double cz = target.getZ();
        int[] delays = {0, 10, 20, 30, 40, 50};
        for (int i = 0; i < delays.length; i++) {
            int idx = i;
            ServerTaskQueue.schedule(server, delays[i], () -> {
                double angle = idx * Math.PI * 2 / 6 + ThreadLocalRandom.current().nextDouble() * 0.3;
                double r = 2 + ThreadLocalRandom.current().nextDouble() * 5;
                strikeLightning(level, cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r);
                if (idx == 3) strikeLightning(level, cx, cy, cz);
            });
        }
        ServerTaskQueue.schedule(server, 55, () ->
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy + 1, cz, 16, 2, 1, 2, 0.2));
    }

    private static void megaHype(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 1.5f, 1.2f);

        target.addEffect(new MobEffectInstance(MobEffects.SPEED, 400, 3, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 300, 1, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 300, 2, false, false, true));

        for (int i = 0; i < 8; i++) {
            final int fi = i;
            int tick = i * 8;
            ServerTaskQueue.schedule(server, tick, () -> {
                spawnFirework(level, target.getX(), target.getY() + 10 + fi, target.getZ());
                level.sendParticles(ParticleTypes.FIREWORK, target.getX(), target.getY() + 1, target.getZ(),
                        20, 1, 0.5, 1, 0.08);
            });
        }
    }

    private static void megaDome(MinecraftServer server, ServerPlayer target, String donor) {
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.2f, 1.1f);

        BlockPos center = target.blockPosition();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 0; dy <= 4; dy++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    boolean wall = dist >= 2.8 || dy == 4;
                    if (wall) {
                        var block = (Math.abs(dx) == 3 && Math.abs(dz) == 3) ? Blocks.OBSIDIAN : Blocks.GLASS;
                        SpawnGuard.replace(level, center.offset(dx, dy, dz), block.defaultBlockState());
                    }
                }
            }
        }
        target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 300, 0, false, false, true));
        ringParticles(level, target, ParticleTypes.END_ROD, 3.5, 30);
    }

    private static void finaleBurst(ServerLevel level, ServerPlayer target) {
        level.sendParticles(ParticleTypes.FIREWORK, target.getX(), target.getY() + 2, target.getZ(),
                30, 1.5, 1, 1.5, 0.1);
    }

    private static void ringParticles(ServerLevel level, ServerPlayer target,
                                      net.minecraft.core.particles.ParticleOptions type,
                                      double radius, int count) {
        double cx = target.getX();
        double cy = target.getY() + 1;
        double cz = target.getZ();
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2 / count;
            level.sendParticles(type, cx + Math.cos(angle) * radius, cy, cz + Math.sin(angle) * radius,
                    1, 0, 0.2, 0, 0.01);
        }
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
            double x = target.getX() + rnd.nextDouble(-5, 5);
            double z = target.getZ() + rnd.nextDouble(-5, 5);
            double y = target.getY() + 16 + rnd.nextInt(10);
            PrimedTnt tnt = new PrimedTnt(level, x, y, z, null);
            tnt.setFuse(30 + rnd.nextInt(30));
            level.addFreshEntity(tnt);
        }
        level.playSound(null, target.blockPosition(), SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1f, 0.8f);
    }

    private static void spawnFirework(ServerLevel level, double x, double y, double z) {
        ItemStack stack = Items.FIREWORK_ROCKET.getDefaultInstance();
        FireworkRocketEntity fw = new FireworkRocketEntity(level, stack, x, y, z, true);
        level.addFreshEntity(fw);
    }

    private static void showWheelCinematic(ServerPlayer target, String donor, String cmd) {
        var fb = ConfigManager.get().feedback;
        String title = wheelTitle(cmd);
        String subtitle = "§6" + truncate(donor, 18) + " §7· §d§l500₽ МЕГА-ПРИЗ";
        if (fb.showTitleInGame) {
            target.connection.send(new ClientboundSetTitlesAnimationPacket(6, 55, 14));
            target.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
            target.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
        }
        if (fb.showActionBar) {
            target.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(
                    "§d§l🎰 РУЛЕТКА §7— " + wheelSubtitle(cmd))));
        }
    }

    private static String wheelTitle(String cmd) {
        return switch (cmd) {
            case "launch" -> "§b§l🚀 КОСМИЧЕСКИЙ ПОЛЁТ";
            case "curse" -> "§2§l☠ АДСКОЕ ПРОКЛЯТИЕ";
            case "meteor" -> "§c§l🌋 МЕТЕОРИТНЫЙ ДОЖД";
            case "wither" -> "§8§l💀 ПОЯВЛЕНИЕ ВИЗЕРА";
            case "army" -> "§5§l⚔ ЛЕГИОН ХАОСА";
            case "tntrain" -> "§6§l💥 АПОКАЛИПСИС TNT";
            case "boss" -> "§4§l👹 БОСС-РЕЙД";
            case "dragon" -> "§5§l🐉 ДРАКОН НАД ГОЛОВОЙ";
            case "prison" -> "§7§l⛓ ЖЕЛЕЗНАЯ ТЮРЬМА";
            case "creeperstorm" -> "§a§l💣 КРИПЕР-ШТОРМ";
            case "event" -> "§e§l🎪 ТРОЙНОЕ СОБЫТИЕ";
            case "spin" -> "§9§l🌀 ВИХРЬ БЕЗУМИЯ";
            case "clear" -> "§8§l🗑 ПОЛНАЯ ОЧИСТКА";
            case "trap" -> "§6§l⚠ СМЕРТЕЛЬНАЯ ЛОВУШКА";
            case "chicken" -> "§e§l🐔 КУРИНЫЙ АД";
            case "anvil" -> "§7§l🔨 ДОЖДЬ НАКОВАЛЕН";
            case "storm" -> "§9§l⚡ ГРОМОВОЙ ШТОРМ";
            case "hype" -> "§d§l🔥 МЕГА-ХАЙП";
            case "dome" -> "§5§l🔮 КРИСТАЛЬНЫЙ КУПОЛ";
            default -> "§d§l🎰 МЕГА-ПРИЗ";
        };
    }

    private static String wheelSubtitle(String cmd) {
        return switch (cmd) {
            case "launch" -> "Стример улетает в стратосферу!";
            case "curse" -> "Проклятие на 3 слота брони + тьма";
            case "meteor" -> "Три метеора с взрывами";
            case "wither" -> "Настоящий Визер с именем донатера";
            case "army" -> "Армия + гаст + легион";
            case "tntrain" -> "4 волны TNT + золотой дождь";
            case "boss" -> "Двойной босс-спавн";
            case "dragon" -> "Эндер-дракон над стримером";
            case "prison" -> "Обсидиановая клетка 5×5";
            case "creeperstorm" -> "6 светящихся криперов";
            case "event" -> "Метеор → гроза → TNT";
            case "spin" -> "160 тиков вращения + подброс";
            case "clear" -> "Инвентарь стёрт в пыль";
            case "trap" -> "Сетка TNT под ногами";
            case "chicken" -> "18 кур с неба";
            case "anvil" -> "Три наковальни подряд";
            case "storm" -> "6 молний + гроза";
            case "hype" -> "Скорость III + 8 фейерверков";
            case "dome" -> "Стеклянный купол 7×7";
            default -> "Эксклюзив доната 500₽";
        };
    }

    private static String wheelAction(String cmd) {
        return switch (cmd) {
            case "launch" -> "запустил стримера в космос! 🚀";
            case "curse" -> "проклял стримера по полной! ☠";
            case "meteor" -> "обрушил метеоритный дождь! 🌋";
            case "wither" -> "призвал Визера! 💀";
            case "army" -> "развязал мега-армию! ⚔";
            case "tntrain" -> "устроил TNT-апокалипсис! 💥";
            case "boss" -> "призвал босс-рейд! 👹";
            case "dragon" -> "выпустил дракона! 🐉";
            case "prison" -> "запер стримера в тюрьме! ⛓";
            case "creeperstorm" -> "запустил крипер-шторм! 💣";
            case "event" -> "запустил тройное событие! 🎪";
            case "spin" -> "крутанул стримера в вихре! 🌀";
            case "clear" -> "стёр инвентарь стримера! 🗑";
            case "trap" -> "поставил смертельную ловушку! ⚠";
            case "chicken" -> "устроил куриный ад! 🐔";
            case "anvil" -> "обрушил наковальни! 🔨";
            case "storm" -> "разразил громовой шторм! ⚡";
            case "hype" -> "включил мега-хайп! 🔥";
            case "dome" -> "закрыл стримера в куполе! 🔮";
            default -> "выиграл мега-приз рулетки! 🎰";
        };
    }

    private static void twitchMsg(String donor, String action) {
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "🎰 " + donor + " (500₽) " + action);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
