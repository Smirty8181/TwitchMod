package net.twitchcraft.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.RagdollPayload;
import net.twitchcraft.stream.ChatMobBudget;
import net.twitchcraft.stream.ServerTaskQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Команды чата под моды из stream-профиля. */
public final class ModPackEffects {
    private static final String[] BRAINROT_FOODS = {
            "doritosandprime:dorito",
            "doritosandprime:prime",
            "doritosandprime:flamin_cheetos",
            "doritosandprime:grimace_shake",
            "doritosandprime:lunchly",
            "doritosandprime:ohio_burger",
            "doritosandprime:mountain_dew",
            "doritosandprime:monster_energy",
            "doritosandprime:feastables",
            "doritosandprime:mango",
            "doritosandprime:mango_smoothie"
    };

    private static final Map<UUID, Long> RAGDOLL_UNTIL = new ConcurrentHashMap<>();

    private ModPackEffects() {}

    /** !гусь — Silly Goose. */
    public static void goose(CommandContext ctx) {
        if (!ModCompat.isLoaded("silly-goose")) {
            ModCompat.missingMod(ctx, "Silly Goose");
            return;
        }
        var type = ModCompat.entity("silly-goose:goose");
        if (type.isEmpty()) {
            ModCompat.missingMod(ctx, "Silly Goose");
            return;
        }
        ServerPlayer p = ctx.target;
        ServerLevel level = p.level();
        for (int i = 0; i < 4; i++) {
            spawnNamed(ctx, type.get(), i);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.CHICKEN_AMBIENT,
                SoundSource.NEUTRAL, 1.2f, 0.5f);
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(),
                16, 0.8, 0.4, 0.8, 0.02);
        AnnouncementService.announceCustom(p, "§e§lГуси §7от §f" + nick(ctx) + "§7 — HONK!");
    }

    /** !хаоскуры — Chaos Chickens (трейты). */
    public static void chaosChickens(CommandContext ctx) {
        if (!ModCompat.isLoaded("chaoschickens")) {
            ModCompat.missingMod(ctx, "Chaos Chickens");
            return;
        }
        ServerPlayer p = ctx.target;
        ServerLevel level = p.level();
        String[] forced = {"explosive", "disco", "boss", "teleport", "cursed"};
        for (int i = 0; i < 5; i++) {
            Chicken chicken = EntityType.CHICKEN.create(level, EntitySpawnReason.COMMAND);
            if (chicken == null) continue;
            double x = p.getX() + level.random.nextInt(5) - 2;
            double z = p.getZ() + level.random.nextInt(5) - 2;
            chicken.setPos(x, p.getY(), z);
            nameEntity(ctx, chicken);
            String trait = i < forced.length ? forced[i] : ModCompat.randomChaosTrait();
            ModCompat.assignChaosTrait(chicken, trait);
            chicken.setCustomName(Component.literal("§c[" + nick(ctx) + "] §e" + trait));
            chicken.setCustomNameVisible(true);
            level.addFreshEntity(chicken);
            registerGhost(ctx, chicken, "хаоскуры");
        }
        level.playSound(null, p.blockPosition(), SoundEvents.CHICKEN_HURT,
                SoundSource.HOSTILE, 1.1f, 0.7f);
        AnnouncementService.announceCustom(p, "§6§lХаос-куры §7от §f" + nick(ctx) + "§7!");
    }

    /** !брейнрот — случайная мем-еда DoritosandPrime. */
    public static void brainrot(CommandContext ctx) {
        if (!ModCompat.isLoaded("doritosandprime")) {
            ModCompat.missingMod(ctx, "DoritosandPrime");
            return;
        }
        ServerPlayer p = ctx.target;
        ServerLevel level = p.level();
        int given = 0;
        for (int i = 0; i < 3; i++) {
            String id = BRAINROT_FOODS[ThreadLocalRandom.current().nextInt(BRAINROT_FOODS.length)];
            Item item = ModCompat.item(id).orElse(null);
            if (item == null) continue;
            ModCompat.giveOrDrop(p, new ItemStack(item, 1 + level.random.nextInt(2)));
            given++;
        }
        if (given == 0) {
            ModCompat.missingMod(ctx, "DoritosandPrime");
            return;
        }
        level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EAT.value(),
                SoundSource.PLAYERS, 1f, 1.2f);
        level.sendParticles(ParticleTypes.ITEM_SLIME, p.getX(), p.getY() + 1.2, p.getZ(),
                14, 0.4, 0.3, 0.4, 0.05);
        AnnouncementService.announceCustom(p, "§d§lБрейнрот §7еда от §f" + nick(ctx));
    }

    /** !прайм — бутылка Prime + скорость. */
    public static void prime(CommandContext ctx) {
        if (!ModCompat.isLoaded("doritosandprime")) {
            ModCompat.missingMod(ctx, "DoritosandPrime");
            return;
        }
        Item item = ModCompat.item("doritosandprime:prime").orElse(null);
        if (item == null) {
            ModCompat.missingMod(ctx, "DoritosandPrime");
            return;
        }
        ServerPlayer p = ctx.target;
        ServerLevel level = p.level();
        ModCompat.giveOrDrop(p, new ItemStack(item));
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 20, 1));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20 * 15, 0));
        level.playSound(null, p.blockPosition(), SoundEvents.BOTTLE_FILL,
                SoundSource.PLAYERS, 1f, 1.4f);
        AnnouncementService.announceCustom(p, "§a§lPrime §7от §f" + nick(ctx) + "§7 — гидратация!");
    }

    /** !ква — звук KvaKva + лягушки. */
    public static void kva(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = p.level();
        boolean hasMod = ModCompat.isLoaded("kvakva");
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            ServerTaskQueue.schedule(ctx.server, i * 4, () -> {
                if (!p.isAlive()) return;
                if (hasMod) {
                    ModCompat.playOptional(level, p.blockPosition(), "kvakva:kvakva",
                            SoundEvents.FROG_AMBIENT, 1.4f, 0.85f + idx * 0.05f);
                } else {
                    level.playSound(null, p.blockPosition(), SoundEvents.FROG_AMBIENT,
                            SoundSource.PLAYERS, 1.2f, 0.9f + idx * 0.05f);
                }
                level.sendParticles(ParticleTypes.NOTE, p.getX(), p.getY() + 1.5, p.getZ(),
                        4, 0.5, 0.3, 0.5, 0.1);
            });
        }
        for (int i = 0; i < 3; i++) {
            spawnNamed(ctx, EntityType.FROG, i);
        }
        AnnouncementService.announceCustom(p, "§2§lКВА §7от §f" + nick(ctx) + "§7!");
    }

    /** !амонг — Among Us из Meme Craft. */
    public static void amongUs(CommandContext ctx) {
        if (!ModCompat.isLoaded("meme_craft")) {
            ModCompat.missingMod(ctx, "Meme Craft");
            return;
        }
        var type = ModCompat.entity("meme_craft:among_us");
        if (type.isEmpty()) {
            ModCompat.missingMod(ctx, "Meme Craft");
            return;
        }
        ServerPlayer p = ctx.target;
        for (int i = 0; i < 2; i++) spawnNamed(ctx, type.get(), i);
        p.level().playSound(null, p.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.HOSTILE, 0.7f, 1.6f);
        AnnouncementService.announceCustom(p, "§c§lАмонг §7— sus от §f" + nick(ctx));
    }

    /** !оя — Oia Cat + звук. */
    public static void oiaCat(CommandContext ctx) {
        if (!ModCompat.isLoaded("meme_craft")) {
            ModCompat.missingMod(ctx, "Meme Craft");
            return;
        }
        var type = ModCompat.entity("meme_craft:oia_cat");
        if (type.isEmpty()) {
            ModCompat.missingMod(ctx, "Meme Craft");
            return;
        }
        ServerPlayer p = ctx.target;
        ServerLevel level = p.level();
        spawnNamed(ctx, type.get(), 0);
        ModCompat.playOptional(level, p.blockPosition(), "meme_craft:oia",
                SoundEvents.CAT_PURREOW, 1.2f, 1f);
        AnnouncementService.announceCustom(p, "§d§lОя-кот §7от §f" + nick(ctx) + "§7!");
    }

    /** !вайн — Vine Boom + отброс. */
    public static void vineBoom(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = p.level();
        boolean meme = ModCompat.isLoaded("meme_craft");
        if (meme) {
            ModCompat.playOptional(level, p.blockPosition(), "meme_craft:vineboom2",
                    SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 0.7f);
            ModCompat.item("meme_craft:vine_boomer").ifPresent(item ->
                    ModCompat.giveOrDrop(p, new ItemStack(item)));
        } else {
            level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 1.4f, 0.6f);
        }
        p.setDeltaMovement(p.getDeltaMovement().add(0, 0.55, 0)
                .add(level.random.nextGaussian() * 0.4, 0, level.random.nextGaussian() * 0.4));
        p.hurtMarked = true;
        level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 1, p.getZ(),
                3, 0.2, 0.2, 0.2, 0);
        AnnouncementService.announceCustom(p, "§6§lVINE BOOM §7от §f" + nick(ctx));
    }

    /** !тролль — маска Troll Face. */
    public static void trollFace(CommandContext ctx) {
        if (!ModCompat.isLoaded("meme_craft")) {
            ModCompat.missingMod(ctx, "Meme Craft");
            return;
        }
        Item helmet = ModCompat.item("meme_craft:troll_face_helmet").orElse(null);
        if (helmet == null) {
            ModCompat.missingMod(ctx, "Meme Craft");
            return;
        }
        ServerPlayer p = ctx.target;
        ServerLevel level = p.level();
        ItemStack old = p.getItemBySlot(EquipmentSlot.HEAD);
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(helmet));
        if (!old.isEmpty()) ModCompat.giveOrDrop(p, old);
        level.playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES,
                SoundSource.PLAYERS, 1f, 0.5f);
        AnnouncementService.announceCustom(p, "§e§lТролль §7маска от §f" + nick(ctx));
    }

    /** !лаки — Lucky Block перед стримером. */
    public static void luckyBlock(CommandContext ctx) {
        placeLuckyVariant(ctx, "luckybreak:lucky_block", "Лаки", "§6");
    }

    /** !неудача — Very Unlucky Block. */
    public static void unluckyBlock(CommandContext ctx) {
        placeLuckyVariant(ctx, "luckybreak:very_unlucky_block", "Неудача", "§c");
    }

    /** !оченьлаки — Very Lucky Block. */
    public static void veryLuckyBlock(CommandContext ctx) {
        placeLuckyVariant(ctx, "luckybreak:very_lucky_block", "Очень лаки", "§a");
    }

    /** !курочка — Golden Hen из Lucky Break. */
    public static void goldenHen(CommandContext ctx) {
        if (!ModCompat.isLoaded("luckybreak")) {
            ModCompat.missingMod(ctx, "Lucky Break");
            return;
        }
        var type = ModCompat.entity("luckybreak:golden_hen");
        if (type.isEmpty()) {
            ModCompat.missingMod(ctx, "Lucky Break");
            return;
        }
        ServerPlayer p = ctx.target;
        spawnNamed(ctx, type.get(), 0);
        p.level().playSound(null, p.blockPosition(), SoundEvents.CHICKEN_EGG,
                SoundSource.NEUTRAL, 1f, 1.3f);
        AnnouncementService.announceCustom(p, "§6§lЗолотая курочка §7от §f" + nick(ctx));
    }

    /** !лакирулетка — случайный lucky / unlucky / very lucky. */
    public static void luckyRoulette(CommandContext ctx) {
        int roll = ThreadLocalRandom.current().nextInt(3);
        String label;
        String color;
        String blockId;
        switch (roll) {
            case 0 -> {
                blockId = "luckybreak:lucky_block";
                label = "ЛАКИ";
                color = "§a";
            }
            case 1 -> {
                blockId = "luckybreak:very_unlucky_block";
                label = "НЕУДАЧА";
                color = "§c";
            }
            default -> {
                blockId = "luckybreak:very_lucky_block";
                label = "ОЧЕНЬ ЛАКИ";
                color = "§e";
            }
        }
        placeLuckyVariant(ctx, blockId, "Лаки-рулетка → " + label, color);
    }

    /** !праймрейв — Prime + танцы + КВА. */
    public static void primeRave(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        if (ModCompat.isLoaded("doritosandprime")) {
            Item item = ModCompat.item("doritosandprime:prime").orElse(null);
            if (item != null) {
                ModCompat.giveOrDrop(p, new ItemStack(item));
                p.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 25, 1));
                p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20 * 20, 1));
            }
        }
        EffectRunner.danceParty(ctx);
        kva(ctx);
        p.level().sendParticles(ParticleTypes.NOTE, p.getX(), p.getY() + 1.5, p.getZ(),
                20, 0.8, 0.5, 0.8, 0.2);
        AnnouncementService.announceCustom(p, "§d§l♪ Прайм-рейв §7от §f" + nick(ctx) + "§7!");
    }

    /** !наспину — Easy NPC humanoid со скином на спине (или стойка, если мода нет). */
    public static void piggyback(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = p.level();
        String label = nick(ctx);

        // Другой онлайн-игрок → настоящий Piggyback
        if (ModCompat.isLoaded("piggyback")) {
            for (ServerPlayer candidate : level.players()) {
                if (candidate == p || !candidate.isAlive()) continue;
                if (net.twitchcraft.stream.StreamTargets.isTarget(candidate)) continue;
                if (candidate.startRiding(p, true, true)) {
                    level.playSound(null, p.blockPosition(), SoundEvents.HORSE_SADDLE.value(),
                            SoundSource.PLAYERS, 1f, 1.2f);
                    AnnouncementService.announceCustom(p,
                            "§d§lНа спину §7— §f" + candidate.getGameProfile().name()
                                    + " §7от §f" + label);
                    return;
                }
            }
        }

        Entity rider = null;
        String skinUsed = null;
        if (ModCompat.isLoaded("easy_npc")) {
            boolean slim = level.random.nextBoolean();
            var type = ModCompat.entity(slim ? "easy_npc:humanoid_slim" : "easy_npc:humanoid");
            if (type.isPresent()) {
                rider = type.get().create(level, EntitySpawnReason.COMMAND);
                if (rider != null) {
                    skinUsed = ModCompat.randomSkinName(ctx.twitchUser);
                    ModCompat.applyEasyNpcPlayerSkin(rider, skinUsed);
                    if (rider instanceof net.minecraft.world.entity.Mob mob) {
                        mob.setNoAi(true);
                        mob.setPersistenceRequired();
                    }
                }
            }
        }

        if (rider == null) {
            rider = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
            if (rider instanceof net.minecraft.world.entity.decoration.ArmorStand stand) {
                stand.setNoBasePlate(true);
                stand.setShowArms(true);
            }
        }
        if (rider == null) return;

        rider.setPos(p.getX(), p.getY() + 1.2, p.getZ());
        rider.setCustomName(Component.literal("§d§l" + label));
        rider.setCustomNameVisible(true);
        rider.setNoGravity(true);
        rider.setSilent(true);
        rider.setInvulnerable(true);
        level.addFreshEntity(rider);
        registerGhost(ctx, rider, "наспину");

        attachToBack(ctx, p, level, rider.getId(), 20 * 14);

        level.playSound(null, p.blockPosition(), SoundEvents.HORSE_SADDLE.value(),
                SoundSource.PLAYERS, 1f, 1.2f);
        level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.6, p.getZ(),
                8, 0.3, 0.2, 0.3, 0.02);
        String skinHint = skinUsed != null ? " §8· скин §f" + skinUsed : "";
        AnnouncementService.announceCustom(p,
                "§d§lНа спину §7NPC §f" + label + skinHint + " §7(14 сек)");
    }

    private static void attachToBack(CommandContext ctx, ServerPlayer p, ServerLevel level,
                                     int riderId, int durationTicks) {
        for (int t = 0; t <= durationTicks; t++) {
            final int tick = t;
            ServerTaskQueue.schedule(ctx.server, tick, () -> {
                if (!p.isAlive() || p.level() != level) {
                    Entity e = level.getEntity(riderId);
                    if (e != null) e.discard();
                    return;
                }
                Entity e = level.getEntity(riderId);
                if (e == null || !e.isAlive()) return;
                if (tick >= durationTicks) {
                    e.discard();
                    return;
                }
                float yawRad = (float) Math.toRadians(p.getYRot());
                double bx = p.getX() + Math.sin(yawRad) * 0.32;
                double by = p.getY() + (p.isCrouching() ? 0.95 : 1.25);
                double bz = p.getZ() - Math.cos(yawRad) * 0.32;
                e.setPos(bx, by, bz);
                e.setYRot(p.getYRot());
                e.setXRot(0);
                e.setDeltaMovement(Vec3.ZERO);
                e.hurtMarked = true;
            });
        }
    }

    /** !армиязрителей — 6 Easy NPC с никами из недавнего чата. */
    public static void viewerArmy(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = p.level();
        List<String> names = net.twitchcraft.stream.ChatViewerRegistry.randomViewers(6, ctx.twitchUser);
        // всегда включаем автора команды
        if (ctx.twitchUser != null && !ctx.twitchUser.isBlank()) {
            String self = truncNick(ctx.twitchUser);
            if (!names.contains(self)) {
                names.set(0, self);
            }
        }

        boolean easy = ModCompat.isLoaded("easy_npc");
        int spawned = 0;
        for (int i = 0; i < names.size(); i++) {
            String name = truncNick(names.get(i));
            Entity npc = null;
            if (easy) {
                boolean slim = level.random.nextBoolean();
                var type = ModCompat.entity(slim ? "easy_npc:humanoid_slim" : "easy_npc:humanoid");
                if (type.isPresent()) {
                    npc = type.get().create(level, EntitySpawnReason.COMMAND);
                    if (npc != null) {
                        ModCompat.applyEasyNpcPlayerSkin(npc, ModCompat.randomSkinName(name));
                        if (npc instanceof net.minecraft.world.entity.Mob mob) {
                            mob.setNoAi(false);
                            mob.setPersistenceRequired();
                        }
                    }
                }
            }
            if (npc == null) {
                npc = EntityType.VILLAGER.create(level, EntitySpawnReason.COMMAND);
            }
            if (npc == null) continue;

            double angle = i * (Math.PI * 2.0 / names.size());
            double r = 2.2;
            npc.setPos(p.getX() + Math.cos(angle) * r, p.getY(), p.getZ() + Math.sin(angle) * r);
            npc.setCustomName(Component.literal("§d§l" + name));
            npc.setCustomNameVisible(true);
            npc.setInvulnerable(true);
            level.addFreshEntity(npc);
            registerGhost(ctx, npc, "армия");
            spawned++;

            // лёгкий «подход» к стримеру через 1 сек
            final int id = npc.getId();
            ServerTaskQueue.schedule(ctx.server, 20 + i * 3, () -> {
                Entity e = level.getEntity(id);
                if (e == null || !e.isAlive() || !p.isAlive()) return;
                double dx = p.getX() - e.getX();
                double dz = p.getZ() - e.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.1) {
                    e.setDeltaMovement(dx / len * 0.25, 0.15, dz / len * 0.25);
                    e.hurtMarked = true;
                }
            });
        }

        level.playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(),
                SoundSource.PLAYERS, 0.9f, 1.3f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(),
                24, 1.5, 0.5, 1.5, 0.05);
        int known = net.twitchcraft.stream.ChatViewerRegistry.size();
        AnnouncementService.announceCustom(p,
                "§d§lАрмия зрителей §7×" + spawned
                        + " §8· в чате известно §f" + known
                        + " §8· от §f" + nick(ctx));
    }

    private static String truncNick(String name) {
        if (name == null || name.isBlank()) return "viewer";
        String n = name.trim();
        return n.length() > 16 ? n.substring(0, 16) : n;
    }

    /** !неси — форс-поднять курицу в руки (Carry On). */
    public static void carryOn(CommandContext ctx) {
        if (!ModCompat.isLoaded("carryon")) {
            ModCompat.missingMod(ctx, "Carry On");
            return;
        }
        ServerPlayer p = ctx.target;
        ServerLevel level = p.level();
        Chicken chicken = EntityType.CHICKEN.create(level, EntitySpawnReason.COMMAND);
        if (chicken == null) return;
        chicken.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        nameEntity(ctx, chicken);
        level.addFreshEntity(chicken);

        ItemStack main = p.getMainHandItem();
        ItemStack off = p.getOffhandItem();
        boolean swapped = false;
        if (!main.isEmpty()) {
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            swapped = true;
        }

        boolean ok = ModCompat.tryCarryOnPickup(p, chicken);
        if (!ok) {
            if (swapped && !p.getInventory().add(main)) p.drop(main, false);
            chicken.setDeltaMovement(0, 0.4, 0);
            AnnouncementService.announceCustom(p,
                    "§e§lНеси §7— курица рядом (Carry On не взял, руки заняты?)");
            return;
        }
        if (swapped) {
            if (!off.isEmpty() || !p.getInventory().add(main)) {
                p.drop(main, false);
            }
        }
        level.playSound(null, p.blockPosition(), SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS, 1f, 0.8f);
        AnnouncementService.announceCustom(p, "§e§lНеси §7— курица в руках от §f" + nick(ctx));
    }

    private static final ResourceLocation RAGDOLL_MOVE_ID =
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "ragdoll_move");
    private static final ResourceLocation RAGDOLL_JUMP_ID =
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "ragdoll_jump");

    /** !тряпка — живой стример замирает, клиент рисует физический труп Ragdolls. */
    public static void ragdoll(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = p.level();
        int ticks = 20 * 5;
        if (System.currentTimeMillis() < ragdollUntil(p)) {
            AnnouncementService.announceCustom(p, "§7Тряпка ещё идёт");
            ctx.fail();
            return;
        }
        RAGDOLL_UNTIL.put(p.getUUID(), System.currentTimeMillis() + ticks * 50L);

        p.stopFallFlying();
        p.setSprinting(false);
        freezeForRagdoll(p, true);
        double yaw = Math.toRadians(p.getYRot());
        shove(p, -Math.sin(yaw) * 0.12, 0.14, Math.cos(yaw) * 0.12);
        RagdollPayload.sendToWorld(p, ticks);

        ServerTaskQueue.schedule(ctx.server, ticks, () -> freezeForRagdoll(p, false));

        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_HURT,
                SoundSource.PLAYERS, 1.1f, 0.55f);
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(),
                22, 0.5, 0.2, 0.5, 0.04);
        AnnouncementService.announceCustom(p, "§c§lТряпка §75 сек §8· от §f" + nick(ctx));
    }

    private static long ragdollUntil(ServerPlayer p) {
        return RAGDOLL_UNTIL.getOrDefault(p.getUUID(), 0L);
    }

    private static void freezeForRagdoll(ServerPlayer p, boolean on) {
        setMulTotal(p.getAttribute(Attributes.MOVEMENT_SPEED), RAGDOLL_MOVE_ID, on ? -1.0 : 0);
        setMulTotal(p.getAttribute(Attributes.JUMP_STRENGTH), RAGDOLL_JUMP_ID, on ? -1.0 : 0);
        if (on) {
            p.setSprinting(false);
            p.stopFallFlying();
        }
    }

    private static void setMulTotal(AttributeInstance inst, ResourceLocation id, double amount) {
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0) {
            inst.addTransientModifier(new AttributeModifier(
                    id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void shove(ServerPlayer p, double x, double y, double z) {
        Vec3 v = new Vec3(x, y, z);
        p.setDeltaMovement(v);
        p.hasImpulse = true;
        p.hurtMarked = true;
        var packet = new ClientboundSetEntityMotionPacket(p.getId(), v);
        p.connection.send(packet);
        for (ServerPlayer viewer : p.level().players()) {
            if (viewer != p) viewer.connection.send(packet);
        }
    }

    private static void placeLuckyVariant(CommandContext ctx, String blockId, String label, String color) {
        if (!ModCompat.isLoaded("luckybreak")) {
            ModCompat.missingMod(ctx, "Lucky Break");
            return;
        }
        Block block = ModCompat.block(blockId).orElse(null);
        if (block == null) {
            ModCompat.missingMod(ctx, "Lucky Break");
            return;
        }
        ServerPlayer p = ctx.target;
        ServerLevel level = p.level();
        float yaw = (float) Math.toRadians(p.getYRot());
        int dx = (int) Math.round(-Math.sin(yaw) * 2);
        int dz = (int) Math.round(Math.cos(yaw) * 2);
        boolean ok = ModCompat.placeNear(p, block, dx, 0, dz);
        if (!ok) {
            ModCompat.giveOrDrop(p, new ItemStack(block.asItem()));
        }
        level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(),
                SoundSource.BLOCKS, 1f, ok ? 1.2f : 0.8f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + dx, p.getY() + 1, p.getZ() + dz,
                12, 0.4, 0.4, 0.4, 0.02);
        AnnouncementService.announceCustom(p, color + "§l" + label + " §7от §f" + nick(ctx));
    }

    private static void spawnNamed(CommandContext ctx, EntityType<?> type, int salt) {
        ServerPlayer player = ctx.target;
        ServerLevel level = player.level();
        Entity entity = type.create(level, EntitySpawnReason.COMMAND);
        if (entity == null) return;
        double x = player.getX() + level.random.nextInt(5) - 2 + (salt % 3) * 0.1;
        double z = player.getZ() + level.random.nextInt(5) - 2;
        entity.setPos(x, player.getY(), z);
        nameEntity(ctx, entity);
        level.addFreshEntity(entity);
        ChatMobBudget.track(entity);
        String mobRu = ctx.command != null ? ctx.command.getRussianName() : "моб";
        registerGhost(ctx, entity, mobRu);
    }

    private static void nameEntity(CommandContext ctx, Entity entity) {
        if (!ConfigManager.get().stream.namedMobs || ctx.twitchUser == null || ctx.twitchUser.isBlank()) return;
        String name = ctx.twitchUser;
        if (name.length() > 16) name = name.substring(0, 16);
        entity.setCustomName(Component.literal("§c[" + name + "]"));
        entity.setCustomNameVisible(true);
    }

    private static void registerGhost(CommandContext ctx, Entity entity, String mobRu) {
        if (ConfigManager.get().stream.ghostMobsEnabled && ctx.twitchUser != null && !ctx.twitchUser.isBlank()) {
            net.twitchcraft.stream.GhostMobRegistry.register(entity, ctx.twitchUser, mobRu,
                    ctx.target.getGameProfile().name());
        }
    }

    private static String nick(CommandContext ctx) {
        String name = ctx.twitchUser;
        if (name == null || name.isBlank()) return "?";
        return name.length() > 12 ? name.substring(0, 11) + "…" : name;
    }
}
