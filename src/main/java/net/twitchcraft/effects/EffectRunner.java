package net.twitchcraft.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.commands.CooldownManager;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;
import net.twitchcraft.feedback.AnnouncementService;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.twitchcraft.stream.BetManager;
import net.twitchcraft.stream.BombManager;
import net.twitchcraft.stream.ChatKingManager;
import net.twitchcraft.stream.HuntManager;
import net.twitchcraft.stream.InvertControls;
import net.twitchcraft.stream.OreVeinEvent;
import net.twitchcraft.stream.ParkourChallenge;
import net.twitchcraft.stream.ServerTaskQueue;
import net.twitchcraft.stream.ChatMobBudget;
import net.twitchcraft.stream.SpawnGuard;
import net.twitchcraft.stream.StreamTargets;
import net.twitchcraft.stream.VoteManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class EffectRunner {
    private static final CooldownManager COOLDOWNS = new CooldownManager();
    private static volatile long pauseUntil = 0;

    private EffectRunner() {}

    public static CooldownManager cooldowns() {
        return COOLDOWNS;
    }

    public static boolean isPaused() {
        return System.currentTimeMillis() < pauseUntil;
    }

    public static void pause(long seconds) {
        pauseUntil = System.currentTimeMillis() + seconds * 1000L;
    }

    public static int pauseSecondsLeft() {
        if (!isPaused()) return 0;
        return (int) Math.max(1, (pauseUntil - System.currentTimeMillis() + 999) / 1000);
    }

    private static ServerLevel world(ServerPlayer player) {
        return player.level();
    }

    public static void lightning(CommandContext ctx) {
        strikePlayer(ctx.target, 6.0f);
    }

    public static void spawnCreeper(CommandContext ctx) {
        spawnNear(ctx, EntityType.CREEPER, e -> {});
    }

    public static void spawnZombie(CommandContext ctx) {
        spawnNear(ctx, EntityType.ZOMBIE, e -> {});
    }

    public static void launch(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        double vy = 1.55 + ThreadLocalRandom.current().nextDouble() * 0.35;
        p.setDeltaMovement(p.getDeltaMovement().x, vy, p.getDeltaMovement().z);
        p.hurtMarked = true;
    }

    public static void spawnSkeleton(CommandContext ctx) {
        spawnNear(ctx, EntityType.SKELETON, e -> {});
    }

    public static void spawnSpider(CommandContext ctx) {
        spawnNear(ctx, EntityType.SPIDER, e -> {});
    }

    public static void spawnEnderman(CommandContext ctx) {
        spawnNear(ctx, EntityType.ENDERMAN, e -> {});
    }

    public static void spawnWitch(CommandContext ctx) {
        spawnNear(ctx, EntityType.WITCH, e -> {});
    }

    public static void spawnSlime(CommandContext ctx) {
        spawnNear(ctx, EntityType.SLIME, e -> {
            if (e instanceof Slime slime) slime.setSize(3, true);
        });
    }

    public static void spawnGhast(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        Ghast ghast = EntityType.GHAST.create(level, EntitySpawnReason.COMMAND);
        if (ghast != null) {
            ghast.setPos(p.getX(), p.getY() + 8, p.getZ());
            nameEntity(ctx, ghast);
            level.addFreshEntity(ghast);
            ChatMobBudget.track(ghast);
            if (ConfigManager.get().stream.ghostMobsEnabled && ctx.twitchUser != null && !ctx.twitchUser.isBlank()) {
                net.twitchcraft.stream.GhostMobRegistry.register(ghast, ctx.twitchUser,
                        ctx.command != null ? ctx.command.getRussianName() : "гаст",
                        ctx.target.getGameProfile().name());
            }
        }
    }

    public static void spawnArmy(CommandContext ctx) {
        for (int i = 0; i < 3; i++) spawnNear(ctx, EntityType.ZOMBIE, e -> {});
        spawnNear(ctx, EntityType.SKELETON, e -> {});
    }

    public static void spawnWither(CommandContext ctx) {
        if (!net.twitchcraft.stream.CampaignJourney.hasSeenNether()) {
            AnnouncementService.announceCustom(ctx.target,
                    "§7Визер после первого захода в Ад");
            ctx.fail();
            return;
        }
        if (ctx.target.level().dimension() != net.minecraft.world.level.Level.NETHER) {
            AnnouncementService.announceCustom(ctx.target, "§7Визер только в Аду");
            ctx.fail();
            return;
        }
        spawnNear(ctx, EntityType.WITHER, e -> {});
    }

    public static void spawnDragon(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        if (p.level().dimension() != net.minecraft.world.level.Level.END) {
            AnnouncementService.announceCustom(p, "§7Дракон только в Крае");
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    "Дракона можно призвать только когда стример в Крае.");
            ctx.fail();
            return;
        }
        ServerLevel level = world(p);
        EnderDragon dragon = EntityType.ENDER_DRAGON.create(level, EntitySpawnReason.COMMAND);
        if (dragon != null) {
            dragon.setPos(p.getX(), p.getY() + 20, p.getZ());
            level.addFreshEntity(dragon);
            ChatMobBudget.track(dragon);
        }
    }

    public static void spawnWolf(CommandContext ctx) {
        spawnNear(ctx, EntityType.WOLF, e -> {
            if (e instanceof Wolf wolf) {
                wolf.setTame(false, false);
                wolf.setTarget(ctx.target);
            }
        });
    }

    public static void spawnIronGolem(CommandContext ctx) {
        spawnNear(ctx, EntityType.IRON_GOLEM, e -> {});
    }

    public static void spawnTnt(CommandContext ctx) {
        spawnTntAt(ctx.target, 1);
    }

    public static void tntRain(CommandContext ctx) {
        spawnTntAt(ctx.target, 2);
    }

    public static void lava(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        BlockPos pos = p.blockPosition().below();
        SpawnGuard.replace(world(p), pos, Blocks.LAVA.defaultBlockState());
    }

    public static void water(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        BlockPos center = p.blockPosition();
        ServerLevel level = world(p);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                SpawnGuard.replace(level, center.offset(dx, 0, dz), Blocks.WATER.defaultBlockState());
            }
        }
    }

    public static void pit(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        BlockPos center = p.blockPosition().below();
        ServerLevel level = world(p);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy < 5; dy++) {
                    SpawnGuard.replace(level, center.offset(dx, -dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    public static void meteor(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        MinecraftServer server = ctx.server;
        double hx = p.getX();
        double hy = p.getY();
        double hz = p.getZ();
        level.playSound(null, hx, hy, hz, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.1f, 0.55f);
        for (int t = 0; t <= 25; t += 5) {
            int age = t;
            net.twitchcraft.stream.ServerTaskQueue.schedule(server, t, () ->
                    meteorRing(level, hx, hy, hz, age));
        }
        net.twitchcraft.stream.ServerTaskQueue.schedule(server, 30, () -> meteorStrike(level, p, hx, hy, hz));
    }

    private static void meteorRing(ServerLevel level, double x, double y, double z, int age) {
        if (level == null) return;
        double r = 2.35;
        int n = 20;
        for (int i = 0; i < n; i++) {
            double a = (Math.PI * 2.0 * i) / n;
            double px = x + Math.cos(a) * r;
            double pz = z + Math.sin(a) * r;
            level.sendParticles(ParticleTypes.FLAME, px, y + 0.08, pz, 1, 0, 0, 0, 0.0);
            if (age >= 15) {
                level.sendParticles(ParticleTypes.LAVA, px, y + 0.12, pz, 1, 0, 0, 0, 0.0);
            }
        }
        level.sendParticles(ParticleTypes.SMOKE, x, y + 0.2, z, 6, 0.35, 0.05, 0.35, 0.01);
        if (age % 10 == 0) {
            level.playSound(null, x, y, z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.35f, 0.45f);
        }
    }

    private static void meteorStrike(ServerLevel level, ServerPlayer target, double x, double y, double z) {
        if (level == null) return;
        BlockPos pos = BlockPos.containing(x, y + 20, z);
        FallingBlockEntity meteor = FallingBlockEntity.fall(level, pos, Blocks.NETHERRACK.defaultBlockState());
        if (meteor != null) {
            meteor.time = 1;
            level.addFreshEntity(meteor);
        }
        level.explode(null, x, y, z, 0.6f, Level.ExplosionInteraction.TNT);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.9f, 0.8f);
        if (target != null && target.isAlive() && target.level() == level
                && target.distanceToSqr(x, y, z) <= 12.25) {
            target.hurtServer(level, target.damageSources().explosion(null, null), 3.0f);
        }
    }

    public static void anvil(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        BlockPos pos = BlockPos.containing(p.getX(), p.getY() + 8, p.getZ());
        FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, Blocks.ANVIL.defaultBlockState());
        if (falling != null) level.addFreshEntity(falling);
    }

    public static void storm(CommandContext ctx) {
        ServerPlayer center = ctx.target;
        ServerLevel level = world(center);
        RandomSource random = level.random;
        for (int i = 0; i < 3; i++) {
            double x = center.getX() + random.nextInt(21) - 10;
            double z = center.getZ() + random.nextInt(21) - 10;
            showLightning(level, x, center.getY(), z);
        }
        strikePlayer(center, 4.0f);
    }

    public static void fire(CommandContext ctx) {
        ctx.target.igniteForSeconds(8);
    }

    public static void creeperStorm(CommandContext ctx) {
        for (int i = 0; i < 2; i++) {
            spawnNear(ctx, EntityType.CREEPER, e -> {});
        }
    }

    public static void drop(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (!p.getInventory().getItem(i).isEmpty()) slots.add(i);
        }
        if (slots.isEmpty()) return;
        int slot = slots.get(ThreadLocalRandom.current().nextInt(slots.size()));
        p.drop(p.getInventory().removeItem(slot, 1), false);
    }

    public static void clearInventory(CommandContext ctx) {
        ctx.target.getInventory().clearContent();
    }

    public static void diamond(CommandContext ctx) {
        ctx.target.getInventory().add(new ItemStack(Items.DIAMOND));
    }

    public static void junk(CommandContext ctx) {
        ItemStack[] junk = {
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.BONE),
                new ItemStack(Items.POISONOUS_POTATO),
                new ItemStack(Items.DEAD_BUSH)
        };
        ctx.target.getInventory().add(junk[ThreadLocalRandom.current().nextInt(junk.length)].copy());
    }

    public static void curse(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        var lookup = ctx.server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> binding = lookup.getOrThrow(Enchantments.BINDING_CURSE);
        Holder<Enchantment> vanishing = lookup.getOrThrow(Enchantments.VANISHING_CURSE);

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        List<EquipmentSlot> worn = new ArrayList<>();
        for (EquipmentSlot slot : slots) {
            if (!p.getItemBySlot(slot).isEmpty()) worn.add(slot);
        }

        if (!worn.isEmpty()) {
            EquipmentSlot slot = worn.get(ThreadLocalRandom.current().nextInt(worn.size()));
            ItemStack stack = p.getItemBySlot(slot).copy();
            ItemEnchantments.Mutable ench = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            ench.set(binding, 1);
            if (ThreadLocalRandom.current().nextBoolean()) ench.set(vanishing, 1);
            stack.set(DataComponents.ENCHANTMENTS, ench.toImmutable());
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("§4Проклятие чата"));
            p.setItemSlot(slot, stack);
        } else {
            ItemStack stack = new ItemStack(Items.LEATHER_BOOTS);
            ItemEnchantments.Mutable ench = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            ench.set(binding, 1);
            ench.set(vanishing, 1);
            stack.set(DataComponents.ENCHANTMENTS, ench.toImmutable());
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("§4Проклятие чата"));
            if (!p.getInventory().add(stack)) {
                p.drop(stack, false);
            }
        }
    }

    public static void shuffleHotbar(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        int[] dest = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
        for (int i = 8; i > 0; i--) {
            int j = ThreadLocalRandom.current().nextInt(i + 1);
            int tmp = dest[i];
            dest[i] = dest[j];
            dest[j] = tmp;
        }
        boolean moved = false;
        for (int i = 0; i < 9; i++) {
            if (dest[i] != i) {
                moved = true;
                break;
            }
        }
        if (!moved) {
            dest[0] = 1;
            dest[1] = 0;
        }
        ItemStack[] original = new ItemStack[9];
        for (int i = 0; i < 9; i++) original[i] = p.getInventory().getItem(i).copy();
        net.twitchcraft.network.HotbarShufflePayload.sendShuffle(p, dest);
        ServerLevel level = world(p);
        level.playSound(null, p.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.9f, 1.35f);
        net.twitchcraft.stream.ServerTaskQueue.schedule(ctx.server, net.twitchcraft.network.HotbarShufflePayload.SHUFFLE_TICKS, () -> {
            if (p == null || !p.isAlive()) return;
            ItemStack[] placed = new ItemStack[9];
            for (int i = 0; i < 9; i++) placed[i] = ItemStack.EMPTY;
            for (int i = 0; i < 9; i++) placed[dest[i]] = original[i];
            for (int i = 0; i < 9; i++) p.getInventory().setItem(i, placed[i]);
            p.containerMenu.broadcastChanges();
        });
    }

    /** !обменинв — рюкзаки, броня и хотбар меняются с напарником. */
    public static void swapInventories(CommandContext ctx) {
        ServerPlayer a = ctx.target;
        ServerPlayer b = StreamTargets.partner(ctx.server, a).orElse(null);
        if (b == null) {
            AnnouncementService.announceCustom(a, "§7Обмен инвентарями — нужен напарник онлайн");
            return;
        }
        var invA = a.getInventory();
        var invB = b.getInventory();
        int size = Math.min(invA.getContainerSize(), invB.getContainerSize());
        ItemStack[] copyA = new ItemStack[size];
        ItemStack[] copyB = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            copyA[i] = invA.getItem(i).copy();
            copyB[i] = invB.getItem(i).copy();
        }
        ItemStack[] hotA = new ItemStack[9];
        ItemStack[] hotB = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            hotA[i] = invA.getItem(i).copy();
            hotB[i] = invB.getItem(i).copy();
        }
        int slotA = invA.getSelectedSlot();
        int slotB = invB.getSelectedSlot();
        ItemStack cursorA = a.containerMenu.getCarried().copy();
        ItemStack cursorB = b.containerMenu.getCarried().copy();
        net.twitchcraft.network.HotbarShufflePayload.sendSwap(a, hotB);
        net.twitchcraft.network.HotbarShufflePayload.sendSwap(b, hotA);
        fxSwap(a);
        fxSwap(b);
        String who = truncName(ctx.twitchUser);
        AnnouncementService.announceCustom(a,
                "§d§lОБМЕН ИНВ! §7с §f" + b.getGameProfile().name() + " §8· §7" + who);
        AnnouncementService.announceCustom(b,
                "§d§lОБМЕН ИНВ! §7с §f" + a.getGameProfile().name() + " §8· §7" + who);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                who + " поменял инвентари " + a.getGameProfile().name()
                        + " и " + b.getGameProfile().name() + "!");
        net.twitchcraft.stream.ServerTaskQueue.schedule(ctx.server, net.twitchcraft.network.HotbarShufflePayload.SWAP_TICKS, () -> {
            if (!a.isAlive() || !b.isAlive()) return;
            for (int i = 0; i < size; i++) {
                a.getInventory().setItem(i, copyB[i]);
                b.getInventory().setItem(i, copyA[i]);
            }
            a.getInventory().setSelectedSlot(slotB);
            b.getInventory().setSelectedSlot(slotA);
            a.containerMenu.setCarried(cursorB);
            b.containerMenu.setCarried(cursorA);
            a.containerMenu.broadcastChanges();
            b.containerMenu.broadcastChanges();
            fxSwap(a);
            fxSwap(b);
        });
    }

    /** Соло-тест анимации обмена: хотбар разворачивается (ещё раз — обратно). */
    public static void swapInventoriesSoloTest(MinecraftServer server, ServerPlayer p) {
        if (server == null || p == null) return;
        ItemStack[] incoming = new ItemStack[9];
        boolean any = false;
        for (int i = 0; i < 9; i++) {
            if (!p.getInventory().getItem(i).isEmpty()) any = true;
        }
        if (any) {
            for (int i = 0; i < 9; i++) {
                incoming[i] = p.getInventory().getItem(8 - i).copy();
            }
        } else {
            ItemStack[] demo = new ItemStack[]{
                    new ItemStack(Items.DIAMOND, 3),
                    new ItemStack(Items.TNT, 4),
                    new ItemStack(Items.ENDER_PEARL, 8),
                    new ItemStack(Items.GOLDEN_APPLE, 2),
                    new ItemStack(Items.TOTEM_OF_UNDYING),
                    new ItemStack(Items.WATER_BUCKET),
                    new ItemStack(Items.ENDER_CHEST),
                    new ItemStack(Items.FIREWORK_ROCKET, 16),
                    new ItemStack(Items.COOKED_BEEF, 16)
            };
            System.arraycopy(demo, 0, incoming, 0, 9);
        }
        net.twitchcraft.network.HotbarShufflePayload.sendSwap(p, incoming);
        fxSwap(p);
        AnnouncementService.announceCustom(p,
                any ? "§d§lОБМЕН ИНВ §7соло-тест · хотбар развернётся, ещё раз — обратно"
                        : "§d§lОБМЕН ИНВ §7соло-тест · на хотбар прилетит набор");
        net.twitchcraft.stream.ServerTaskQueue.schedule(server,
                net.twitchcraft.network.HotbarShufflePayload.SWAP_TICKS, () -> {
            if (!p.isAlive()) return;
            for (int i = 0; i < 9; i++) {
                p.getInventory().setItem(i, incoming[i]);
            }
            p.containerMenu.broadcastChanges();
            fxSwap(p);
        });
    }

    private static void fxSwap(ServerPlayer p) {
        ServerLevel level = world(p);
        level.playSound(null, p.blockPosition(), SoundEvents.ENDER_CHEST_OPEN,
                SoundSource.PLAYERS, 0.9f, 1.35f);
        level.sendParticles(ParticleTypes.WITCH, p.getX(), p.getY() + 1.0, p.getZ(),
                10, 0.4, 0.5, 0.4, 0.02);
    }

    public static void eggs(CommandContext ctx) {
        ctx.target.getInventory().add(new ItemStack(Items.EGG, 16));
    }

    public static void bow(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        p.getInventory().add(new ItemStack(Items.BOW));
        p.getInventory().add(new ItemStack(Items.ARROW, 32));
    }

    /** !элитры — крылья + ракеты, чтобы сразу лететь. */
    public static void elytra(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        ItemStack wings = new ItemStack(Items.ELYTRA);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty()) {
            p.setItemSlot(EquipmentSlot.CHEST, wings);
        } else if (!chest.is(Items.ELYTRA)) {
            if (!p.getInventory().add(wings)) {
                p.drop(wings, false);
            }
        }
        ItemStack rockets = new ItemStack(Items.FIREWORK_ROCKET, 16);
        rockets.set(DataComponents.FIREWORKS, new net.minecraft.world.item.component.Fireworks(2, List.of()));
        if (!p.getInventory().add(rockets)) {
            p.drop(rockets, false);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA.value(),
                SoundSource.PLAYERS, 1f, 1.15f);
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(),
                18, 0.5, 0.4, 0.5, 0.04);
        AnnouncementService.announceCustom(p, "§b§lЭлитры §7от §f" + truncName(ctx.twitchUser)
                + " §8· прыжок + ракеты в руках");
    }

    public static void randomTeleport(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        RandomSource random = level.random;
        for (int attempt = 0; attempt < 16; attempt++) {
            int dx = random.nextInt(101) - 50;
            int dz = random.nextInt(101) - 50;
            double x = p.getX() + dx;
            double z = p.getZ() + dz;
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    BlockPos.containing(x, 0, z));
            if (level.isEmptyBlock(surface.above()) && level.isEmptyBlock(surface.above(2))) {
                p.teleportTo(level, surface.getX() + 0.5, surface.getY() + 1, surface.getZ() + 0.5,
                        Set.of(), p.getYRot(), p.getXRot(), true);
                return;
            }
        }
    }

    public static void spin(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 1));
        float startYaw = p.getYRot();
        float startPitch = p.getXRot();
        for (int t = 1; t <= 100; t++) {
            int tick = t;
            ServerTaskQueue.schedule(ctx.server, t, () -> {
                if (!net.twitchcraft.stream.TargetPlayerUtil.isOnline(ctx.server, p)) return;
                p.setYRot(startYaw + tick * 10.8f);
                p.setXRot(Mth.clamp(startPitch + 25f * Mth.sin(tick * 0.15f), -80f, 80f));
            });
        }
    }

    public static void backward(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 1));
        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 1));
    }

    public static void swap(CommandContext ctx) {
        ServerPlayer other = StreamTargets.partner(ctx.server, ctx.target).orElse(null);
        if (other == null) {
            List<ServerPlayer> others = ctx.server.getPlayerList().getPlayers().stream()
                    .filter(pl -> pl != ctx.target)
                    .toList();
            if (others.isEmpty()) return;
            other = others.get(ThreadLocalRandom.current().nextInt(others.size()));
        }
        double tx = ctx.target.getX(), ty = ctx.target.getY(), tz = ctx.target.getZ();
        float tyaw = ctx.target.getYRot(), tpitch = ctx.target.getXRot();
        ServerLevel tw = world(ctx.target);
        double ox = other.getX(), oy = other.getY(), oz = other.getZ();
        float oyaw = other.getYRot(), opitch = other.getXRot();
        ServerLevel ow = world(other);
        ctx.target.teleportTo(ow, ox, oy, oz, Set.of(), oyaw, opitch, true);
        other.teleportTo(tw, tx, ty, tz, Set.of(), tyaw, tpitch, true);
    }

    public static void up(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        p.teleportTo(world(p), p.getX(), p.getY() + 10, p.getZ(), Set.of(), p.getYRot(), p.getXRot(), true);
    }

    public static void down(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        p.teleportTo(level, p.getX(), Math.max(level.getMinY() + 2, p.getY() - 10), p.getZ(),
                Set.of(), p.getYRot(), p.getXRot(), true);
    }

    public static void knockback(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        RandomSource r = world(p).random;
        double dx = r.nextDouble() * 2 - 1;
        double dz = r.nextDouble() * 2 - 1;
        p.setDeltaMovement(dx, 0.8, dz);
        p.hurtMarked = true;
    }

    public static void heal(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        p.setHealth(p.getMaxHealth());
        p.clearFire();
        clearHarmfulEffects(p);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.8f, 1.4f);
        level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.2, p.getZ(),
                8, 0.5, 0.4, 0.5, 0.02);
    }

    public static void feed(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        p.getFoodData().setFoodLevel(20);
        p.getFoodData().setSaturation(14f);
        level.playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 1f, 1.2f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(),
                6, 0.4, 0.3, 0.4, 0.02);
    }

    public static void save(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        heal(ctx);
        feed(ctx);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 30, 1));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1));
        p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 12, 0));
        level.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS, 0.9f, 1.1f);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(),
                24, 0.6, 0.8, 0.6, 0.12);
        AnnouncementService.announceCustom(p, "§a§l" + ctx.twitchUser + " §fспас стримера!");
    }

    public static void totem(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        ItemStack stack = new ItemStack(Items.TOTEM_OF_UNDYING);
        if (!p.getInventory().add(stack)) {
            p.drop(stack, false);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS, 0.9f, 1.1f);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(),
                32, 0.5, 0.7, 0.5, 0.15);
        AnnouncementService.announceCustom(p, "§6§l" + ctx.twitchUser + " §fдал тотем бессмертия!");
    }

    private static void clearHarmfulEffects(ServerPlayer p) {
        p.removeEffect(MobEffects.POISON);
        p.removeEffect(MobEffects.WITHER);
        p.removeEffect(MobEffects.BLINDNESS);
        p.removeEffect(MobEffects.DARKNESS);
        p.removeEffect(MobEffects.SLOWNESS);
        p.removeEffect(MobEffects.WEAKNESS);
        p.removeEffect(MobEffects.HUNGER);
        p.removeEffect(MobEffects.NAUSEA);
    }

    public static void hurt(CommandContext ctx) {
        ctx.target.hurtServer(world(ctx.target), ctx.target.damageSources().generic(), 4.0f);
    }

    public static void blind(CommandContext ctx) {
        ctx.target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 300, 0));
    }

    public static void speed(CommandContext ctx) {
        ctx.target.addEffect(new MobEffectInstance(MobEffects.SPEED, 400, 1));
    }

    public static void slow(CommandContext ctx) {
        ctx.target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 300, 2));
    }

    public static void hunger(CommandContext ctx) {
        ctx.target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 2));
        ctx.target.causeFoodExhaustion(4.0f);
    }

    public static void regen(CommandContext ctx) {
        ctx.target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
    }

    public static void poison(CommandContext ctx) {
        ctx.target.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 0));
    }

    public static void strength(CommandContext ctx) {
        ctx.target.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 300, 1));
    }

    public static void weakness(CommandContext ctx) {
        ctx.target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0));
    }

    public static void glow(CommandContext ctx) {
        ctx.target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0));
    }

    public static void day(CommandContext ctx) {
        world(ctx.target).setDayTime(6000);
    }

    public static void night(CommandContext ctx) {
        world(ctx.target).setDayTime(18000);
    }

    public static void rain(CommandContext ctx) {
        ServerLevel level = world(ctx.target);
        level.setWeatherParameters(0, 6000, true, false);
    }

    public static void clearWeather(CommandContext ctx) {
        ServerLevel level = world(ctx.target);
        level.setWeatherParameters(0, 6000, false, false);
    }

    public static void thunder(CommandContext ctx) {
        ServerLevel level = world(ctx.target);
        level.setWeatherParameters(0, 6000, true, true);
    }

    public static void noon(CommandContext ctx) {
        ServerLevel level = world(ctx.target);
        level.setDayTime(6000);
        level.setWeatherParameters(0, 6000, false, false);
    }

    public static void randomCommand(CommandContext ctx) {
        ChatGameCommand pick = pickEnabledFun();
        AnnouncementService.announceCustom(ctx.target, "§dРандом → §e!" + pick.getRussianName());
        TwitchCraftMod.getConnectionManager().executeById(ctx.server, ctx.viewer(), pick.getId(), true,
                ctx.target.getGameProfile().name());
    }

    public static void coin(CommandContext ctx) {
        if (ThreadLocalRandom.current().nextBoolean()) heal(ctx);
        else hurt(ctx);
    }

    public static void boss(CommandContext ctx) {
        if (ThreadLocalRandom.current().nextBoolean()) {
            spawnNear(ctx, EntityType.WARDEN, e -> {});
        } else {
            spawnNear(ctx, EntityType.ELDER_GUARDIAN, e -> {});
        }
    }

    public static void firework(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        FireworkRocketEntity rocket = new FireworkRocketEntity(level, stack, p.getX(), p.getY() + 1, p.getZ(), true);
        level.addFreshEntity(rocket);
    }

    public static void sheep(CommandContext ctx) {
        RandomSource r = world(ctx.target).random;
        for (int i = 0; i < 5; i++) {
            spawnNear(ctx, EntityType.SHEEP, e -> {
                if (e instanceof Sheep sheep) sheep.setColor(net.minecraft.world.item.DyeColor.byId(r.nextInt(16)));
            });
        }
    }

    public static void giant(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 1));
        p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 160, 1));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 160, 0));
        world(p).sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 1, 0.5, 0.1);
        AnnouncementService.announceCustom(p, "§6§lГИГАНТ §78 сек §8· от §f" + ctx.viewer());
    }

    public static void stop(CommandContext ctx) {
        pause(60);
        AnnouncementService.announceCustom(ctx.target, "§c§lСТОП §7чат на паузе 60с · от §f" + ctx.viewer());
        TwitchCraftMod.LOGGER.info("TwitchCraft paused for 60s by {}", ctx.twitchUser);
    }

    public static void help(CommandContext ctx) {
        heal(ctx);
        AnnouncementService.announceCustom(ctx.target, "§a" + ctx.twitchUser + " §fпомог стримеру!");
    }

    public static void roulette(CommandContext ctx) {
        ChatGameCommand pick = pickEnabledFun();
        ServerPlayer original = ctx.target;
        AnnouncementService.announceCustom(original, "§dРулетка: §e!" + pick.getRussianName() + " §7через 3 сек...");
        int tick = ctx.server.getTickCount() + 60;
        ctx.server.schedule(new net.minecraft.server.TickTask(tick, () -> {
            if (!ctx.server.isRunning() || original == null || !original.isAlive()) return;
            TwitchCraftMod.getConnectionManager().executeById(ctx.server, ctx.viewer(), pick.getId(), true,
                    original.getGameProfile().name());
            AnnouncementService.announceCustom(original, "§dРулетка выпала: §c!" + pick.getRussianName());
        }));
    }

    private static ChatGameCommand pickEnabledFun() {
        ChatGameCommand[] pool = ChatGameCommand.funPool(ConfigManager.get());
        if (pool.length == 0) return ChatGameCommand.FIREWORK;
        return pool[ThreadLocalRandom.current().nextInt(pool.length)];
    }

    public static void startVote(CommandContext ctx) {
        if (!VoteManager.get().start(ctx.server)) ctx.fail();
    }

    public static void xpRain(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 60, 1, 1, 1, 0.05);
        p.giveExperiencePoints(15 + level.random.nextInt(20));
    }

    public static void dome(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        BlockPos center = p.blockPosition();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2 || dy == 3;
                    if (wall) {
                        SpawnGuard.replace(level, center.offset(dx, dy, dz), Blocks.GLASS.defaultBlockState());
                    }
                }
            }
        }
    }

    public static void prison(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        BlockPos center = p.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    if (Math.abs(dx) == 1 || Math.abs(dz) == 1 || dy == 2) {
                        SpawnGuard.replace(level, center.offset(dx, dy, dz), Blocks.IRON_BARS.defaultBlockState());
                    }
                }
            }
        }
    }

    public static void hype(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 2));
        p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        firework(ctx);
        world(p).sendParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
    }

    public static void chaosEvent(CommandContext ctx) {
        ChatGameCommand[] show = {
                ChatGameCommand.HYPE, ChatGameCommand.CHICKEN, ChatGameCommand.FLASH,
                ChatGameCommand.JUMPSCARE, ChatGameCommand.DRUNK, ChatGameCommand.FISH,
                ChatGameCommand.DANCE, ChatGameCommand.CLOWN, ChatGameCommand.SLAP,
                ChatGameCommand.FIREWORK
        };
        ChatGameCommand pick;
        if (ctx.server.overworld().random.nextFloat() < 0.22f) {
            ChatGameCommand[] spicy = {
                    ChatGameCommand.METEOR, ChatGameCommand.ARMY,
                    ChatGameCommand.STORM, ChatGameCommand.TNT_RAIN
            };
            pick = spicy[ctx.server.overworld().random.nextInt(spicy.length)];
        } else {
            pick = show[ctx.server.overworld().random.nextInt(show.length)];
        }
        AnnouncementService.announceCustom(ctx.target, "§d§lСОБЫТИЕ §e!" + pick.getRussianName());
        TwitchCraftMod.getConnectionManager().executeById(ctx.server, ctx.viewer(), pick.getId(), true,
                ctx.target.getGameProfile().name());
    }

    public static void chickenRain(CommandContext ctx) {
        for (int i = 0; i < 8; i++) {
            spawnNear(ctx, EntityType.CHICKEN, e -> {});
        }
    }

    public static void flash(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));
        showLightning(level, p.getX(), p.getY() + 8, p.getZ());
    }

    public static void trap(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        PrimedTnt tnt = new PrimedTnt(level, p.getX(), p.getY() + 0.2, p.getZ(), null);
        tnt.setFuse(55);
        level.addFreshEntity(tnt);
    }

    public static void viewerEgg(CommandContext ctx) {
        net.twitchcraft.content.ViewerEggService.giveEgg(ctx.target, ctx.twitchUser);
    }

    public static void chatPhantom(CommandContext ctx) {
        net.twitchcraft.content.ChatPhantomService.spawn(world(ctx.target), ctx.target, ctx.twitchUser);
    }

    /** !фпв — курица-камикадзе падает сверху и взрывается. */
    public static void fpvKamikaze(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        Entity chicken = EntityType.CHICKEN.create(level, EntitySpawnReason.COMMAND);
        if (chicken == null) return;
        chicken.setPos(p.getX(), p.getY() + 14, p.getZ());
        chicken.setDeltaMovement(0, -1.2, 0);
        chicken.setCustomName(Component.literal("§c§l[ФПВ] " + truncName(ctx.twitchUser)));
        chicken.setCustomNameVisible(true);
        chicken.setGlowingTag(true);
        level.addFreshEntity(chicken);
        ChatMobBudget.track(chicken);
        level.playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.HOSTILE, 1.2f, 0.7f);
        AnnouncementService.announceCustom(p, "§c§lФПВ §7— курица-камикадзе!");

        final int entityId = chicken.getId();
        ServerTaskQueue.schedule(ctx.server, 18, () -> {
            Entity e = level.getEntity(entityId);
            if (e == null || !e.isAlive()) {
                level.explode(null, p.getX(), p.getY(), p.getZ(), 2.4f, Level.ExplosionInteraction.MOB);
                return;
            }
            double x = e.getX();
            double y = e.getY();
            double z = e.getZ();
            e.discard();
            level.explode(null, x, y, z, 2.6f, Level.ExplosionInteraction.MOB);
            level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 8, 0.4, 0.4, 0.4, 0.02);
            level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.HOSTILE, 1.4f, 1f);
        });
    }

    /** !тцк — 5 свинозомби окружают и уводят стримера ~на 10 блоков. */
    public static void spawnPigZombies(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        String[] names = {"Мыкола", "Алексей", "Богдан", "Владимир", "Иване"};
        java.util.ArrayList<Integer> escortIds = new java.util.ArrayList<>(names.length);

        for (int i = 0; i < names.length; i++) {
            Entity entity = EntityType.ZOMBIFIED_PIGLIN.create(level, EntitySpawnReason.COMMAND);
            if (entity == null) continue;
            double angle = i * (Math.PI * 2.0 / names.length);
            entity.setPos(p.getX() + Math.cos(angle) * 1.6, p.getY(), p.getZ() + Math.sin(angle) * 1.6);
            entity.setCustomName(Component.literal("§6" + names[i]));
            entity.setCustomNameVisible(true);
            entity.setGlowingTag(true);
            if (entity instanceof net.minecraft.world.entity.Mob mob) {
                mob.setPersistenceRequired();
                mob.setTarget(p);
            }
            level.addFreshEntity(entity);
            ChatMobBudget.track(entity);
            escortIds.add(entity.getId());
            if (ConfigManager.get().stream.ghostMobsEnabled && ctx.twitchUser != null && !ctx.twitchUser.isBlank()) {
                net.twitchcraft.stream.GhostMobRegistry.register(entity, ctx.twitchUser, "тцк",
                        p.getGameProfile().name());
            }
        }

        double yawRad = Math.toRadians(level.random.nextFloat() * 360f);
        double dirX = Math.cos(yawRad);
        double dirZ = Math.sin(yawRad);
        double startX = p.getX();
        double startY = p.getY();
        double startZ = p.getZ();
        double destX = startX + dirX * 10.0;
        double destZ = startZ + dirZ * 10.0;
        float faceYaw = (float) (Math.toDegrees(Math.atan2(-dirX, dirZ)));

        level.playSound(null, p.blockPosition(), SoundEvents.ZOMBIFIED_PIGLIN_ANGRY,
                SoundSource.HOSTILE, 1.2f, 0.85f);
        AnnouncementService.announceCustom(p, "§6§lТЦК §7увозит стримера на §f10§7 блоков!");

        final int steps = 20;
        for (int step = 1; step <= steps; step++) {
            final int s = step;
            ServerTaskQueue.schedule(ctx.server, s, () -> {
                if (!p.isAlive() || p.level() != level) return;
                float t = s / (float) steps;
                double nx = startX + (destX - startX) * t;
                double nz = startZ + (destZ - startZ) * t;
                double ny = findEscortY(level, nx, startY, nz);
                p.teleportTo(level, nx, ny, nz, Set.of(), faceYaw, p.getXRot(), true);
                p.hurtMarked = true;

                int i = 0;
                for (int id : escortIds) {
                    Entity e = level.getEntity(id);
                    if (e == null || !e.isAlive()) {
                        i++;
                        continue;
                    }
                    double a = i * (Math.PI * 2.0 / Math.max(1, escortIds.size()));
                    double ex = nx + Math.cos(a) * 1.5;
                    double ez = nz + Math.sin(a) * 1.5;
                    e.teleportTo(ex, ny, ez);
                    if (e instanceof net.minecraft.world.entity.Mob mob) {
                        mob.setTarget(p);
                    }
                    i++;
                }

                if (s % 4 == 0) {
                    level.sendParticles(ParticleTypes.SMOKE, nx, ny + 0.2, nz, 4, 0.4, 0.1, 0.4, 0.01);
                }
            });
        }
    }

    private static double findEscortY(ServerLevel level, double x, double preferY, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(preferY + 2), Mth.floor(z));
        for (int i = 0; i < 8; i++) {
            if (level.getBlockState(pos).isAir() && !level.getBlockState(pos.below()).isAir()) {
                return pos.getY();
            }
            pos.move(0, -1, 0);
        }
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
        return Math.max(level.getMinY() + 1, surface);
    }

    /** !антидот — снять вредные эффекты. */
    public static void antidote(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        clearHarmfulEffects(p);
        p.clearFire();
        level.playSound(null, p.blockPosition(), SoundEvents.BREWING_STAND_BREW,
                SoundSource.PLAYERS, 1f, 1.3f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(),
                16, 0.5, 0.5, 0.5, 0.02);
        AnnouncementService.announceCustom(p, "§a§lАнтидот §7от §f" + truncName(ctx.twitchUser));
    }

    /** !яблоко — золотое яблоко. */
    public static void goldenApple(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        ItemStack apple = new ItemStack(Items.GOLDEN_APPLE);
        if (!p.getInventory().add(apple)) {
            p.drop(apple, false);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS, 0.9f, 1.1f);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(),
                10, 0.3, 0.4, 0.3, 0.05);
        AnnouncementService.announceCustom(p, "§6§lЯблоко §7от §f" + truncName(ctx.twitchUser));
    }

    /** !факелы — стак факелов для прогресса. */
    public static void torches(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        ItemStack stack = new ItemStack(Items.TORCH, 64);
        if (!p.getInventory().add(stack)) {
            p.drop(stack, false);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS, 0.9f, 1.25f);
        level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(),
                14, 0.35, 0.4, 0.35, 0.02);
        AnnouncementService.announceCustom(p, "§6§lФакелы §7от §f" + truncName(ctx.twitchUser));
    }

    /** !хлеб — стак хлеба. */
    public static void bread(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        ItemStack stack = new ItemStack(Items.BREAD, 64);
        if (!p.getInventory().add(stack)) {
            p.drop(stack, false);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS, 0.9f, 1.05f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(),
                10, 0.35, 0.35, 0.35, 0.02);
        AnnouncementService.announceCustom(p, "§e§lХлеб §7от §f" + truncName(ctx.twitchUser));
    }

    /** !доски — стак дубовых досок. */
    public static void planks(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        ItemStack stack = new ItemStack(Items.OAK_PLANKS, 64);
        if (!p.getInventory().add(stack)) {
            p.drop(stack, false);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.WOOD_PLACE,
                SoundSource.BLOCKS, 0.8f, 1.2f);
        level.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1, p.getZ(),
                8, 0.3, 0.3, 0.3, 0.02);
        AnnouncementService.announceCustom(p, "§6§lДоски §7от §f" + truncName(ctx.twitchUser));
    }

    /** !зелье — случайное полезное зелье. */
    public static void randomPotion(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        RandomSource rng = level.random;
        PotionPick pick = switch (rng.nextInt(7)) {
            case 0 -> new PotionPick("Скорость", Potions.SWIFTNESS);
            case 1 -> new PotionPick("Ночное зрение", Potions.NIGHT_VISION);
            case 2 -> new PotionPick("Огнестойкость", Potions.FIRE_RESISTANCE);
            case 3 -> new PotionPick("Подводное дыхание", Potions.WATER_BREATHING);
            case 4 -> new PotionPick("Сила", Potions.STRENGTH);
            case 5 -> new PotionPick("Регенерация", Potions.REGENERATION);
            default -> new PotionPick("Прыгучесть", Potions.LEAPING);
        };
        ItemStack stack = PotionContents.createItemStack(Items.POTION, pick.potion());
        if (!p.getInventory().add(stack)) {
            p.drop(stack, false);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.BREWING_STAND_BREW,
                SoundSource.PLAYERS, 0.9f, 1.15f);
        level.sendParticles(ParticleTypes.WITCH, p.getX(), p.getY() + 1.2, p.getZ(),
                14, 0.35, 0.4, 0.35, 0.02);
        AnnouncementService.announceCustom(p,
                "§5§lЗелье §d" + pick.label() + " §7от §f" + truncName(ctx.twitchUser));
    }

    private record PotionPick(String label, net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {}

    /** !друг — приручённый волк зрителя. */
    public static void buddyWolf(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        Wolf wolf = EntityType.WOLF.create(level, EntitySpawnReason.COMMAND);
        if (wolf == null) return;
        wolf.setPos(p.getX() + 1.2, p.getY(), p.getZ() + 1.2);
        wolf.setTame(true, true);
        wolf.setOwner(p);
        String nick = truncName(ctx.twitchUser);
        wolf.setCustomName(Component.literal("§a♥ " + nick));
        wolf.setCustomNameVisible(true);
        wolf.setPersistenceRequired();
        level.addFreshEntity(wolf);
        level.playSound(null, p.blockPosition(), SoundEvents.WOLF_SHAKE,
                SoundSource.NEUTRAL, 1f, 1.2f);
        AnnouncementService.announceCustom(p, "§a§lДруг §f" + nick + " §7прибежал защищать!");
    }

    /** !респект — лёгкий бафф + уважуха. */
    public static void respect(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 20, 0));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 5, 0));
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.7f, 1.6f);
        level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.4, p.getZ(),
                10, 0.5, 0.3, 0.5, 0.02);
        AnnouncementService.announceCustom(p, "§d§lРеспект §7от §f" + truncName(ctx.twitchUser) + "§7!");
    }

    /** !тапок — шлёпнуть стримера. */
    public static void slap(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        float yaw = (float) Math.toRadians(p.getYRot() + 180f + (level.random.nextFloat() - 0.5f) * 40f);
        double dx = -Math.sin(yaw) * 0.85;
        double dz = Math.cos(yaw) * 0.85;
        p.setDeltaMovement(dx, 0.35, dz);
        p.hurtMarked = true;
        p.hurtServer(level, p.damageSources().generic(), 1f);
        level.playSound(null, p.blockPosition(), SoundEvents.SLIME_ATTACK,
                SoundSource.PLAYERS, 1f, 0.6f);
        level.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1.2, p.getZ(),
                12, 0.3, 0.3, 0.3, 0.08);
        AnnouncementService.announceCustom(p, "§e§lТапок §7от §f" + truncName(ctx.twitchUser) + "§7!");
    }

    /** !клоун — тыква на голову. */
    public static void clown(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        ItemStack old = p.getItemBySlot(EquipmentSlot.HEAD);
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        if (!old.isEmpty() && !p.getInventory().add(old)) {
            p.drop(old, false);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.VILLAGER_NO,
                SoundSource.PLAYERS, 1f, 0.8f);
        level.sendParticles(ParticleTypes.NOTE, p.getX(), p.getY() + 2, p.getZ(),
                8, 0.4, 0.2, 0.4, 0.1);
        AnnouncementService.announceCustom(p, "§6§lКлоун §7— тыква от §f" + truncName(ctx.twitchUser));
    }

    /** !рыба — дождь из рыбы. */
    public static void fishRain(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            ServerTaskQueue.schedule(ctx.server, i * 2, () -> {
                if (!p.isAlive()) return;
                ItemStack fish = new ItemStack(level.random.nextBoolean() ? Items.COD : Items.SALMON);
                var drop = new net.minecraft.world.entity.item.ItemEntity(
                        level,
                        p.getX() + (level.random.nextDouble() - 0.5) * 2.5,
                        p.getY() + 3 + level.random.nextDouble(),
                        p.getZ() + (level.random.nextDouble() - 0.5) * 2.5,
                        fish);
                drop.setDeltaMovement(0, -0.2, 0);
                level.addFreshEntity(drop);
                if (idx % 3 == 0) {
                    level.playSound(null, p.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH,
                            SoundSource.PLAYERS, 0.6f, 1.2f);
                }
            });
        }
        AnnouncementService.announceCustom(p, "§b§lРыба §7сыпется от §f" + truncName(ctx.twitchUser));
    }

    /** !танцы — диско-пати: ноты + лёгкая крутилка. */
    public static void danceParty(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20 * 8, 1));
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 8, 0));
        for (int t = 0; t < 16; t++) {
            final int tick = t;
            ServerTaskQueue.schedule(ctx.server, tick * 5, () -> {
                if (!p.isAlive()) return;
                p.setYRot(p.getYRot() + 45f);
                p.yRotO = p.getYRot();
                p.hurtMarked = true;
                level.sendParticles(ParticleTypes.NOTE,
                        p.getX(), p.getY() + 1.5, p.getZ(),
                        6, 0.6, 0.4, 0.6, 0.2);
                if (tick % 2 == 0) {
                    level.playSound(null, p.getX(), p.getY(), p.getZ(),
                            SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS,
                            0.5f, 0.8f + tick * 0.05f);
                }
            });
        }
        AnnouncementService.announceCustom(p, "§d§lТанцы §7от §f" + truncName(ctx.twitchUser) + "§7!");
    }

    /** !новальный — на 2 блока вниз. */
    public static void novalnyDrop(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        double ny = Math.max(level.getMinY() + 1, p.getY() - 2);
        p.teleportTo(level, p.getX(), ny, p.getZ(), Set.of(), p.getYRot(), p.getXRot(), true);
        level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.9f, 0.6f);
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.5, p.getZ(),
                12, 0.3, 0.2, 0.3, 0.02);
        AnnouncementService.announceCustom(p, "§7Новальный §8→ §f−2 блока");
    }

    /** !озвучка "текст" — произносится у стримера на клиенте. */
    public static void voiceSay(CommandContext ctx) {
        String text = extractVoiceText(ctx.rawMessage);
        if (text.isBlank()) {
            AnnouncementService.announceCustom(ctx.target,
                    "§c!озвучка §7нужен текст: §f!озвучка \"привет\"");
            ctx.fail();
            return;
        }
        if (text.length() > 120) text = text.substring(0, 120);
        net.twitchcraft.network.TtsSpeakPayload.send(ctx.target, ctx.twitchUser, text);
        AnnouncementService.announceCustom(ctx.target,
                "§d§l🔊 " + truncName(ctx.twitchUser) + "§7: §f" + text);
    }

    static String extractVoiceText(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.trim();
        int sp = s.indexOf(' ');
        if (sp < 0) return "";
        String rest = s.substring(sp + 1).trim();
        if (rest.length() >= 2) {
            char a = rest.charAt(0);
            if (a == '"' || a == '\'' || a == '«') {
                char end = a == '«' ? '»' : a;
                int close = rest.indexOf(end, 1);
                if (close > 1) return rest.substring(1, close).trim();
            }
        }
        return rest;
    }

    private static String truncName(String name) {
        if (name == null || name.isBlank()) return "?";
        return name.length() > 12 ? name.substring(0, 11) + "…" : name;
    }

    public static void startHunt(CommandContext ctx) {
        if (!HuntManager.get().start(ctx.server, ctx.target, ctx.twitchUser)) ctx.fail();
    }

    public static void oreVein(CommandContext ctx) {
        OreVeinEvent.start(ctx);
    }

    public static void parkour(CommandContext ctx) {
        ParkourChallenge.start(ctx);
    }

    public static void invertControls(CommandContext ctx) {
        InvertControls.start(ctx);
    }

    public static void upgradeItem(CommandContext ctx) {
        if (!net.twitchcraft.stream.ItemUpgrade.start(ctx.server, ctx.target, ctx.twitchUser)) {
            ctx.fail();
        }
    }

    public static void startBet(CommandContext ctx) {
        BetManager.get().handle(ctx.server, ctx.twitchUser, ctx.rawMessage);
    }

    public static void startBomb(CommandContext ctx) {
        if (!BombManager.get().start(ctx.server, ctx.target, ctx.twitchUser)) ctx.fail();
    }

    public static void defuseBomb(CommandContext ctx) {
        if (BombManager.get().isActive()) {
            BombManager.get().defuse(ctx.server, ctx.twitchUser);
        } else {
            AnnouncementService.announceCustom(ctx.target, "§7Бомбы нет — сначала §e!бомба");
            ctx.fail();
        }
    }

    public static void crownKing(CommandContext ctx) {
        ChatKingManager.get().crown(ctx.server, ctx.target, ctx.twitchUser);
    }

    /** !табличка текст — граффити чата рядом со стримером. */
    public static void placeSign(CommandContext ctx) {
        String text = extractVoiceText(ctx.rawMessage);
        if (text.isBlank()) {
            AnnouncementService.announceCustom(ctx.target,
                    "§c!табличка §7нужен текст: §f!табличка привет стрим");
            ctx.fail();
            return;
        }
        text = text.replaceAll("§.", "").replace('\n', ' ').trim();
        if (text.length() > 60) text = text.substring(0, 60);

        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        BlockPos pos = p.blockPosition().relative(p.getDirection());
        if (!level.getBlockState(pos).canBeReplaced()) pos = pos.above();
        if (!level.getBlockState(pos).canBeReplaced()) pos = p.blockPosition().above(2);

        if (SpawnGuard.isProtected(level, pos)) {
            AnnouncementService.announceCustom(p, "§7Табличка не ставится на спавне");
            return;
        }
        int rot = Mth.floor((p.getYRot() + 180.0F) * 16.0F / 360.0F + 0.5) & 15;
        level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rot), 3);
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            String nick = truncName(ctx.twitchUser);
            String rest = text;
            sign.updateText(old -> old
                    .setMessage(0, Component.literal(nick + ":"))
                    .setMessage(1, Component.literal(take(rest, 0, 15)))
                    .setMessage(2, Component.literal(take(rest, 15, 15)))
                    .setMessage(3, Component.literal(take(rest, 30, 15))), true);
        }
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.1f);
        AnnouncementService.announceCustom(p, "§e✉ §f" + truncName(ctx.twitchUser) + "§7: §f" + text);
    }

    /** !украсть — вырвать стак и утащить. */
    public static void stealItem(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (!p.getInventory().getItem(i).isEmpty()) slots.add(i);
        }
        if (slots.isEmpty()) {
            AnnouncementService.announceCustom(p, "§7Украсть нечего — инвентарь пуст");
            return;
        }
        int slot = slots.get(ThreadLocalRandom.current().nextInt(slots.size()));
        ItemStack taken = p.getInventory().getItem(slot).copy();
        p.getInventory().setItem(slot, ItemStack.EMPTY);
        String itemName = taken.getHoverName().getString();
        var dropped = p.drop(taken, true);
        if (dropped != null) {
            dropped.setDeltaMovement(
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.4,
                    0.45,
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.4);
            dropped.setPickUpDelay(40);
        }
        world(p).playSound(null, p.blockPosition(), SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS, 1.0f, 0.5f);
        AnnouncementService.announceCustom(p,
                "§c§lУКРАЛИ! §f" + truncName(ctx.twitchUser) + " §7 унёс §e" + itemName);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                ctx.twitchUser + " украл у стримера: " + itemName);
    }

    /** !скример — шипение крипера без урона. */
    public static void jumpscare(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ServerLevel level = world(p);
        level.playSound(null, p.blockPosition(), SoundEvents.CREEPER_PRIMED,
                SoundSource.HOSTILE, 1.5f, 1.0f);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 35, 0));
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 18, 0));
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getY() + 1, p.getZ(),
                30, 0.6, 0.5, 0.6, 0.02);
        level.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 1, p.getZ(),
                20, 0.4, 0.4, 0.4, 0.01);
        AnnouncementService.announceCustom(p, "§8§lБУ! §7от §f" + truncName(ctx.twitchUser));
    }

    /** !пьяный — тошнота + камера пляшет. */
    public static void drunk(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 1));
        for (int i = 0; i < 8; i++) {
            int delay = 8 + i * 8;
            ServerTaskQueue.schedule(ctx.server, delay, () -> {
                if (!p.isAlive()) return;
                float yaw = p.getYRot() + (ThreadLocalRandom.current().nextFloat() * 56 - 28);
                float pitch = Mth.clamp(p.getXRot() + (ThreadLocalRandom.current().nextFloat() * 24 - 12), -80, 80);
                p.teleportTo(p.level(), p.getX(), p.getY(), p.getZ(), Set.of(), yaw, pitch, true);
            });
        }
        AnnouncementService.announceCustom(p, "§6§lПьяный режим §7от §f" + truncName(ctx.twitchUser));
    }

    /** !гравитация — вверх, потом вниз. */
    public static void gravityFlip(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 50, 3));
        ServerTaskQueue.schedule(ctx.server, 52, () -> {
            if (!p.isAlive()) return;
            p.removeEffect(MobEffects.LEVITATION);
            p.setDeltaMovement(0, -2.2, 0);
            p.hurtMarked = true;
            world(p).playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 0.5f, 1.6f);
        });
        AnnouncementService.announceCustom(p, "§b§lГравитация §7от §f" + truncName(ctx.twitchUser));
    }

    /** !ограбление — 8 сек напряжения, потом лучший предмет улетает. */
    public static void heist(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        String thief = truncName(ctx.twitchUser);
        AnnouncementService.announceCustom(p, "§6§lОГРАБЛЕНИЕ! §f" + thief + " §78 сек…");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "ОГРАБЛЕНИЕ! " + ctx.twitchUser + " через 8с унесёт лучший предмет.");
        world(p).playSound(null, p.blockPosition(), SoundEvents.VILLAGER_NO,
                SoundSource.PLAYERS, 1.0f, 0.8f);
        ServerTaskQueue.schedule(ctx.server, 160, () -> {
            if (!p.isAlive()) return;
            int bestSlot = -1;
            int bestScore = 0;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                int score = lootScore(p.getInventory().getItem(i));
                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }
            if (bestSlot < 0) {
                AnnouncementService.announceCustom(p, "§7Ограбление провал — пусто");
                return;
            }
            ItemStack taken = p.getInventory().getItem(bestSlot).copy();
            p.getInventory().setItem(bestSlot, ItemStack.EMPTY);
            String itemName = taken.getHoverName().getString();
            var dropped = p.drop(taken, true);
            if (dropped != null) {
                dropped.setDeltaMovement(
                        (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.8,
                        0.55,
                        (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.8);
                dropped.setPickUpDelay(50);
            }
            world(p).playSound(null, p.blockPosition(), SoundEvents.WITHER_SHOOT,
                    SoundSource.PLAYERS, 0.6f, 1.4f);
            AnnouncementService.announceCustom(p,
                    "§6§lУНЕСЛИ §e" + itemName + " §7— §f" + thief);
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    thief + " унёс: " + itemName);
        });
    }

    private static int lootScore(ItemStack s) {
        if (s == null || s.isEmpty()) return 0;
        if (s.is(Items.TOTEM_OF_UNDYING)) return 100;
        if (s.is(Items.ENCHANTED_GOLDEN_APPLE)) return 90;
        if (s.is(Items.NETHERITE_SWORD) || s.is(Items.NETHERITE_PICKAXE) || s.is(Items.NETHERITE_AXE)
                || s.is(Items.NETHERITE_CHESTPLATE) || s.is(Items.NETHERITE_HELMET)
                || s.is(Items.NETHERITE_LEGGINGS) || s.is(Items.NETHERITE_BOOTS)
                || s.is(Items.NETHERITE_INGOT)) return 80;
        if (s.is(Items.DIAMOND) || s.is(Items.DIAMOND_SWORD) || s.is(Items.DIAMOND_PICKAXE)
                || s.is(Items.DIAMOND_CHESTPLATE) || s.is(Items.DIAMOND_HELMET)
                || s.is(Items.DIAMOND_LEGGINGS) || s.is(Items.DIAMOND_BOOTS)
                || s.is(Items.DIAMOND_BLOCK)) return 60;
        if (s.is(Items.GOLDEN_APPLE)) return 50;
        if (s.isEnchanted()) return 40;
        return 10;
    }

    private static String take(String s, int from, int len) {
        if (s == null || from >= s.length()) return "";
        return s.substring(from, Math.min(s.length(), from + len));
    }

    public static void status(CommandContext ctx) {
        var mgr = TwitchCraftMod.getConnectionManager();
        var stream = ConfigManager.get().stream;
        String house = stream.houseMode ? " | хаусмод" : "";
        String channel = mgr.isConnected() ? mgr.getChannel() : "";
        String msg = mgr.isConnected()
                ? "TwitchCraft онлайн | #" + channel + house
                : "TwitchCraft оффлайн" + house;
        mgr.sendChatMessage(msg);
    }

    public static void skip(CommandContext ctx) {
        for (ServerPlayer streamer : StreamTargets.onlineStreamers(ctx.server)) {
            COOLDOWNS.clearScope(streamer.getGameProfile().name());
        }
        if (ctx.target != null) {
            COOLDOWNS.clearScope(ctx.target.getGameProfile().name());
        }
    }

    private static ServerPlayer randomPlayer(CommandContext ctx) {
        List<ServerPlayer> players = ctx.server.getPlayerList().getPlayers();
        if (players.isEmpty()) return null;
        return players.get(ThreadLocalRandom.current().nextInt(players.size()));
    }

    private static void spawnNear(CommandContext ctx, EntityType<?> type, Consumer<Entity> modifier) {
        ServerPlayer player = ctx.target;
        ServerLevel level = world(player);
        Entity entity = type.create(level, EntitySpawnReason.COMMAND);
        if (entity == null) return;
        double dist = spawnDistance(type);
        double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
        double x = player.getX() + Math.cos(angle) * dist;
        double z = player.getZ() + Math.sin(angle) * dist;
        entity.setPos(x, player.getY(), z);
        nameEntity(ctx, entity);
        modifier.accept(entity);
        level.addFreshEntity(entity);
        ChatMobBudget.track(entity);
        if (ConfigManager.get().stream.ghostMobsEnabled && ctx.twitchUser != null && !ctx.twitchUser.isBlank()) {
            String mobRu = ctx.command != null ? ctx.command.getRussianName() : "моб";
            net.twitchcraft.stream.GhostMobRegistry.register(entity, ctx.twitchUser, mobRu,
                    ctx.target.getGameProfile().name());
        }
    }

    private static void nameEntity(CommandContext ctx, Entity entity) {
        if (!ConfigManager.get().stream.namedMobs || ctx.twitchUser == null || ctx.twitchUser.isBlank()) return;
        String name = ctx.twitchUser;
        if (name.length() > 16) name = name.substring(0, 16);
        entity.setCustomName(Component.literal("§c[" + name + "]"));
        entity.setCustomNameVisible(true);
    }

    private static double spawnDistance(EntityType<?> type) {
        if (type == EntityType.CREEPER || type == EntityType.ZOMBIE || type == EntityType.SKELETON
                || type == EntityType.SPIDER || type == EntityType.ENDERMAN || type == EntityType.WITCH
                || type == EntityType.SLIME || type == EntityType.WOLF || type == EntityType.IRON_GOLEM
                || type == EntityType.GHAST || type == EntityType.WITHER || type == EntityType.WARDEN
                || type == EntityType.ELDER_GUARDIAN) {
            return 7.0 + ThreadLocalRandom.current().nextDouble() * 3.0;
        }
        return 1.5 + ThreadLocalRandom.current().nextDouble() * 2.0;
    }

    private static void showLightning(ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.COMMAND);
        if (bolt == null) return;
        bolt.setPos(x, y, z);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    private static void strikePlayer(ServerPlayer player, float damage) {
        ServerLevel level = world(player);
        showLightning(level, player.getX(), player.getY(), player.getZ());
        if (damage > 0 && player.isAlive()) {
            player.hurtServer(level, player.damageSources().lightningBolt(), damage);
        }
    }

    private static void spawnTntAt(ServerPlayer player, int count) {
        ServerLevel level = world(player);
        int n = Math.min(2, Math.max(1, count));
        for (int i = 0; i < n; i++) {
            PrimedTnt tnt = new PrimedTnt(level,
                    player.getX() + (i - 0.5) * 2.4,
                    player.getY() + 4 + i,
                    player.getZ(),
                    null);
            tnt.setFuse(70 + level.random.nextInt(20));
            level.addFreshEntity(tnt);
        }
    }

    public static Optional<ServerPlayer> resolveTarget(net.minecraft.server.MinecraftServer server) {
        return net.twitchcraft.stream.StreamTargets.resolve(server);
    }
}
