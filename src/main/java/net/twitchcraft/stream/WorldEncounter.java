package net.twitchcraft.stream;

import com.mojang.math.Transformation;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.WorldDropPayload;
import net.twitchcraft.TwitchCraftMod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Контент в мире, не команда чата: груз, тотем, гость-свинья.
 * Раз в 8–14 мин рядом со стримером. Дуо бегут к метке.
 */
public final class WorldEncounter {
    public enum Kind { CRATE, SHRINE, GUEST }

    private record Placed(BlockPos pos, BlockState previous, BlockState placed) {}

    private static final class Run {
        final Kind kind;
        final ServerLevel level;
        final BlockPos origin;
        final String viewer;
        final List<Placed> blocks = new ArrayList<>();
        UUID guestId;
        UUID labelId;
        boolean trap;
        boolean claimed;
        int ticksLeft;
        int lingerTicks;
        int durationSec;

        Run(Kind kind, ServerLevel level, BlockPos origin, String viewer, int durationSec) {
            this.kind = kind;
            this.level = level;
            this.origin = origin.immutable();
            this.viewer = viewer;
            this.durationSec = durationSec;
            this.ticksLeft = durationSec * 20;
        }
    }

    private static Run active;
    private static long nextAt;
    private static Kind lastKind;

    private WorldEncounter() {}

    public static void init() {
        nextAt = System.currentTimeMillis() + 90_000L;
    }

    public static boolean isActive() {
        return active != null;
    }

    public static void tick(MinecraftServer server) {
        if (active != null) {
            tickActive(server, active);
            return;
        }
        if (!enabled()) return;
        if (HouseMode.isActive()) return;
        if (StreamerShield.isActive()) return;
        if (nextAt == 0) init();
        if (System.currentTimeMillis() < nextAt) return;
        if (!trySpawn(server, null)) {
            nextAt = System.currentTimeMillis() + 90_000L;
        }
    }

    public static boolean forceSpawn(MinecraftServer server, ServerPlayer who) {
        if (!enabled()) {
            if (who != null) {
                AnnouncementService.announceHud(who, "§7Находки мира выключены в K → Стрим");
            }
            return false;
        }
        if (active != null) finish(server, active);
        return trySpawn(server, who);
    }

    public static String statusLine(ServerPlayer player) {
        Run run = active;
        if (run == null || player == null) return "";
        if (player.level() != run.level) return prefix(run) + " §8другой мир";
        int sec = secondsLeft(run);
        int dist = (int) Math.sqrt(player.blockPosition().distSqr(run.origin));
        return prefix(run) + " §f" + dist + "м " + compass(player, run.origin) + " §e" + sec + "с";
    }

    public static void pushHud(MinecraftServer server) {
        Run run = active;
        if (run == null) {
            WorldDropPayload.hide(server);
            return;
        }
        String dim = run.level.dimension().location().getPath();
        WorldDropPayload.broadcast(server, new WorldDropPayload(
                run.kind.name(),
                trunc(run.viewer, 16),
                dim,
                run.origin.getX(), run.origin.getY(), run.origin.getZ(),
                secondsLeft(run),
                Math.max(1, run.durationSec),
                run.claimed,
                true));
    }

    public static boolean allowBreak(Level world, BlockPos pos) {
        Run run = active;
        if (run == null || world != run.level) return true;
        for (Placed p : run.blocks) {
            if (p.pos().equals(pos)) return false;
        }
        return true;
    }

    public static InteractionResult onUseBlock(ServerPlayer player, BlockPos pos) {
        Run run = active;
        if (run == null || player.level() != run.level) return InteractionResult.PASS;
        if (!owns(run, pos)) return InteractionResult.PASS;
        if (run.kind == Kind.CRATE) {
            if (!run.claimed) claimCrate(player, run);
            return InteractionResult.PASS;
        }
        if (run.kind == Kind.SHRINE) {
            if (!run.claimed) claimShrine(player, run);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public static InteractionResult onUseEntity(ServerPlayer player, Entity entity) {
        Run run = active;
        if (run == null || run.kind != Kind.GUEST || run.guestId == null) return InteractionResult.PASS;
        if (!entity.getUUID().equals(run.guestId)) return InteractionResult.PASS;
        if (!run.claimed) claimGuest(player, run, false);
        return InteractionResult.SUCCESS;
    }

    public static void onGuestDeath(Entity entity) {
        Run run = active;
        if (run == null || run.kind != Kind.GUEST || run.guestId == null) return;
        if (!entity.getUUID().equals(run.guestId)) return;
        ServerPlayer claimer = nearestStreamer(run);
        if (claimer != null && !run.claimed) claimGuest(claimer, run, true);
    }

    public static void clear() {
        if (active != null) {
            MinecraftServer server = active.level.getServer();
            restore(active);
            despawnGuest(active);
            despawnLabel(active);
            active = null;
            if (server != null) WorldDropPayload.hide(server);
        }
    }

    private static boolean trySpawn(MinecraftServer server, ServerPlayer prefer) {
        ServerPlayer host = prefer;
        if (host == null || host.level().dimension() != Level.OVERWORLD) {
            host = pickOverworldHost(server);
        }
        if (host == null) return false;
        ServerLevel level = host.level();
        BlockPos site = findSite(level, host);
        if (site == null) return false;
        Kind kind = nextKind();
        String viewer = pickViewer(host.getGameProfile().name());
        int duration = switch (kind) {
            case CRATE -> 45;
            case SHRINE -> 35;
            case GUEST -> 40;
        };
        Run run = new Run(kind, level, site, viewer, duration);
        boolean ok = switch (kind) {
            case CRATE -> placeCrate(run);
            case SHRINE -> placeShrine(run);
            case GUEST -> placeGuest(run);
        };
        if (!ok) {
            restore(run);
            despawnGuest(run);
            despawnLabel(run);
            return false;
        }
        spawnLabel(run);
        active = run;
        lastKind = kind;
        scheduleNext();
        burst(run);
        announce(server, run, host);
        pushHud(server);
        return true;
    }

    private static boolean placeCrate(Run run) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        run.trap = r.nextFloat() < 0.18f;
        BlockPos floor = run.origin.below();
        if (!put(run, floor, Blocks.WAXED_COPPER_BLOCK.defaultBlockState())) {
            if (!put(run, floor, Blocks.CUT_COPPER.defaultBlockState())) return false;
        }
        BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
        if (!put(run, run.origin, barrel)) return false;
        if (run.level.getBlockEntity(run.origin) instanceof BarrelBlockEntity box) {
            fillCrate(box, run.viewer, run.trap, r);
            box.setChanged();
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (tryPut(run, run.origin.relative(dir), Blocks.LIGHTNING_ROD.defaultBlockState())) break;
        }
        return true;
    }

    private static boolean placeShrine(Run run) {
        BlockPos base = run.origin.below();
        if (!put(run, base, Blocks.CRYING_OBSIDIAN.defaultBlockState())) return false;
        if (!put(run, run.origin, Blocks.AMETHYST_BLOCK.defaultBlockState())) return false;
        tryPut(run, run.origin.above(), Blocks.SOUL_LANTERN.defaultBlockState());
        BlockState candles = Blocks.PURPLE_CANDLE.defaultBlockState()
                .setValue(CandleBlock.CANDLES, 3)
                .setValue(CandleBlock.LIT, true);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            tryPut(run, run.origin.relative(dir), candles);
        }
        return true;
    }

    private static boolean placeGuest(Run run) {
        tryPut(run, run.origin, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState());
        Pig pig = EntityType.PIG.create(run.level, EntitySpawnReason.EVENT);
        if (pig == null) return false;
        pig.setPos(run.origin.getX() + 0.5, run.origin.getY(), run.origin.getZ() + 0.5);
        pig.setCustomName(Component.literal(trunc(run.viewer, 12)).withStyle(ChatFormatting.LIGHT_PURPLE));
        pig.setCustomNameVisible(true);
        pig.setPersistenceRequired();
        pig.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        pig.addEffect(new MobEffectInstance(MobEffects.GLOWING, run.durationSec * 20, 0, false, false));
        pig.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, run.durationSec * 20, 0, false, false));
        pig.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, false));
        run.level.addFreshEntity(pig);
        run.guestId = pig.getUUID();
        ChatMobBudget.track(pig);
        return true;
    }

    private static boolean put(Run run, BlockPos pos, BlockState state) {
        if (SpawnGuard.isProtected(run.level, pos)) return false;
        BlockState prev = run.level.getBlockState(pos);
        if (!prev.getFluidState().isEmpty()) return false;
        boolean air = prev.isAir() || prev.canBeReplaced();
        boolean pad = isPad(state.getBlock());
        if (!air && !pad) return false;
        if (!air && pad && !prev.isFaceSturdy(run.level, pos, Direction.UP)) return false;
        if (!SpawnGuard.replace(run.level, pos, state)) return false;
        run.blocks.add(new Placed(pos.immutable(), prev, state));
        return true;
    }

    private static boolean tryPut(Run run, BlockPos pos, BlockState state) {
        BlockState prev = run.level.getBlockState(pos);
        if (!prev.isAir() && !prev.canBeReplaced()) return false;
        return put(run, pos, state);
    }

    private static boolean isPad(net.minecraft.world.level.block.Block block) {
        return block == Blocks.WAXED_COPPER_BLOCK
                || block == Blocks.CUT_COPPER
                || block == Blocks.CRYING_OBSIDIAN
                || block == Blocks.AMETHYST_BLOCK;
    }

    private static void fillCrate(BarrelBlockEntity box, String viewer, boolean trap, ThreadLocalRandom r) {
        box.setItem(0, named(new ItemStack(Items.BREAD, 6 + r.nextInt(5)), viewer));
        box.setItem(1, new ItemStack(Items.COOKED_PORKCHOP, 3 + r.nextInt(3)));
        box.setItem(2, new ItemStack(Items.TORCH, 12 + r.nextInt(8)));
        box.setItem(3, new ItemStack(Items.IRON_INGOT, 2 + r.nextInt(6)));
        box.setItem(4, new ItemStack(Items.COAL, 8));
        if (r.nextFloat() < 0.45f) box.setItem(5, new ItemStack(Items.IRON_PICKAXE));
        if (r.nextFloat() < 0.28f) box.setItem(6, new ItemStack(Items.GOLDEN_APPLE));
        if (r.nextFloat() < 0.40f) box.setItem(7, new ItemStack(Items.ARROW, 12 + r.nextInt(12)));
        if (trap) box.setItem(8, named(new ItemStack(Items.TNT, 1), "сюрприз " + viewer));
    }

    private static void tickActive(MinecraftServer server, Run run) {
        if (run.claimed) {
            run.lingerTicks--;
            followLabel(run);
            if (run.lingerTicks % 10 == 0) pushHud(server);
            if (run.lingerTicks <= 0) {
                finish(server, run);
            }
            return;
        }
        run.ticksLeft--;
        particles(run);
        followLabel(run);
        if (run.ticksLeft % 20 == 0) {
            pushHud(server);
        }
        if (run.kind == Kind.SHRINE && !run.claimed) {
            for (ServerPlayer p : StreamTargets.onlineStreamers(server)) {
                if (p.level() == run.level && p.blockPosition().distManhattan(run.origin) <= 1) {
                    claimShrine(p, run);
                    break;
                }
            }
        }
        if (run.ticksLeft <= 0) {
            miss(server, run);
        }
    }

    private static void claimCrate(ServerPlayer player, Run run) {
        run.claimed = true;
        run.lingerTicks = 20 * 25;
        run.level.playSound(null, run.origin, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 1.1f, 1.0f);
        sparkBurst(run, 0xF1C40F);
        if (run.trap) {
            PrimedTnt tnt = new PrimedTnt(run.level,
                    run.origin.getX() + 0.5, run.origin.getY() + 1.1, run.origin.getZ() + 0.5, player);
            tnt.setFuse(55);
            run.level.addFreshEntity(tnt);
            burstChickens(run, 5);
            hudAll(player.level().getServer(), "§c§lГРУЗ-ЛОВУШКА §7взрыв через 3с · §f" + run.viewer);
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    "Груз от " + run.viewer + " оказался ловушкой! Взрыв через 3 секунды.");
        } else {
            hudAll(player.level().getServer(),
                    "§6§lГРУЗ §7забрал §f" + player.getGameProfile().name() + " §8· §f" + run.viewer);
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    player.getGameProfile().name() + " забрал груз чата от " + run.viewer + "!");
        }
        refreshLabel(run);
        pushHud(player.level().getServer());
    }

    private static void claimShrine(ServerPlayer player, Run run) {
        run.claimed = true;
        run.lingerTicks = 20 * 8;
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 300, 0));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 300, 0));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
        giveOrDrop(player, new ItemStack(Items.GOLD_NUGGET, 4));
        giveOrDrop(player, new ItemStack(Items.COOKED_BEEF, 2));
        run.level.playSound(null, run.origin, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.95f, 1.35f);
        run.level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, true, true,
                run.origin.getX() + 0.5, run.origin.getY() + 1.4, run.origin.getZ() + 0.5,
                28, 0.45, 0.7, 0.45, 0.12);
        firework(run, 0x9B59B6, FireworkExplosion.Shape.STAR);
        hudAll(player.level().getServer(),
                "§5§lТОТЕМ §7активировал §f" + player.getGameProfile().name() + " §8· §f" + run.viewer);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                player.getGameProfile().name() + " тронул тотем чата от " + run.viewer + " — бафф 15с.");
        refreshLabel(run);
        pushHud(player.level().getServer());
    }

    private static void claimGuest(ServerPlayer player, Run run, boolean killed) {
        run.claimed = true;
        run.lingerTicks = 20 * 6;
        ItemStack gift = named(new ItemStack(Items.GOLDEN_APPLE), "посылка " + run.viewer);
        giveOrDrop(player, gift);
        if (killed) {
            giveOrDrop(player, new ItemStack(Items.COOKED_PORKCHOP, 3));
        }
        run.level.playSound(null, run.origin, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);
        firework(run, 0xFF7AA8, FireworkExplosion.Shape.BURST);
        if (killed) {
            ChatMobBudget.forget(run.guestId);
            run.guestId = null;
        } else {
            despawnGuest(run);
        }
        String verb = killed ? "вскрыл посылку" : "принял посылку";
        hudAll(player.level().getServer(),
                "§d§lГОСТЬ §7" + verb + " §f" + player.getGameProfile().name() + " §8· §f" + run.viewer);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                player.getGameProfile().name() + " " + verb + " от " + run.viewer + "!");
        refreshLabel(run);
        pushHud(player.level().getServer());
    }

    private static void miss(MinecraftServer server, Run run) {
        if (run.kind == Kind.CRATE) dumpCrate(run);
        firework(run, colorOf(run.kind), FireworkExplosion.Shape.SMALL_BALL);
        burstChickens(run, run.kind == Kind.GUEST ? 3 : 4);
        if (run.kind == Kind.GUEST) {
            dropAt(run, named(new ItemStack(Items.GOLDEN_APPLE), "посылка " + run.viewer));
        }
        hudAll(server, "§8" + titleOf(run.kind) + " от " + run.viewer + " исчез — не успели");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                titleOf(run.kind) + " от " + run.viewer + " исчез. Не успели.");
        finish(server, run);
    }

    private static void finish(MinecraftServer server, Run run) {
        restore(run);
        despawnGuest(run);
        despawnLabel(run);
        if (active == run) active = null;
        if (server != null) WorldDropPayload.hide(server);
    }

    private static void restore(Run run) {
        for (int i = run.blocks.size() - 1; i >= 0; i--) {
            Placed p = run.blocks.get(i);
            BlockState now = run.level.getBlockState(p.pos());
            if (now.equals(p.placed()) || now.getBlock() == p.placed().getBlock()) {
                if (run.level.getBlockEntity(p.pos()) instanceof BarrelBlockEntity box) {
                    dumpBox(run, box);
                }
                SpawnGuard.replace(run.level, p.pos(), p.previous());
            }
        }
        run.blocks.clear();
    }

    private static void dumpCrate(Run run) {
        if (run.level.getBlockEntity(run.origin) instanceof BarrelBlockEntity box) {
            dumpBox(run, box);
        }
    }

    private static void dumpBox(Run run, BarrelBlockEntity box) {
        for (int i = 0; i < box.getContainerSize(); i++) {
            ItemStack stack = box.getItem(i);
            if (stack.isEmpty()) continue;
            Containers.dropItemStack(run.level,
                    run.origin.getX() + 0.5, run.origin.getY() + 0.6, run.origin.getZ() + 0.5,
                    stack.copy());
            box.setItem(i, ItemStack.EMPTY);
        }
        box.setChanged();
    }

    private static void despawnGuest(Run run) {
        if (run.guestId == null) return;
        Entity e = run.level.getEntity(run.guestId);
        if (e != null && e.isAlive()) {
            run.level.sendParticles(ParticleTypes.CLOUD, true, true,
                    e.getX(), e.getY() + 0.6, e.getZ(),
                    16, 0.35, 0.35, 0.35, 0.03);
            e.discard();
        }
        ChatMobBudget.forget(run.guestId);
        run.guestId = null;
    }

    private static void spawnLabel(Run run) {
        if (run.kind == Kind.GUEST) return;
        Display.TextDisplay text = EntityType.TEXT_DISPLAY.create(run.level, EntitySpawnReason.COMMAND);
        if (text == null) return;
        text.setPos(run.origin.getX() + 0.5, run.origin.getY() + 1.85, run.origin.getZ() + 0.5);
        text.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        text.setViewRange(1.8f);
        text.setLineWidth(80);
        text.setBackgroundColor(0x990C0812);
        text.setTextOpacity((byte) -1);
        text.setFlags((byte) (Display.TextDisplay.FLAG_SHADOW | Display.TextDisplay.FLAG_SEE_THROUGH));
        text.setTransformation(new Transformation(null, null, new Vector3f(1.05f, 1.05f, 1.05f), null));
        text.setInvulnerable(true);
        text.setNoGravity(true);
        text.setText(labelComponent(run));
        run.level.addFreshEntity(text);
        run.labelId = text.getUUID();
    }

    private static void refreshLabel(Run run) {
        if (run.labelId == null) return;
        Entity e = run.level.getEntity(run.labelId);
        if (e instanceof Display.TextDisplay text) {
            text.setText(labelComponent(run));
        }
    }

    private static void followLabel(Run run) {
        if (run.labelId == null || run.kind != Kind.GUEST) return;
        Entity label = run.level.getEntity(run.labelId);
        Entity guest = run.guestId == null ? null : run.level.getEntity(run.guestId);
        if (label == null || guest == null || !guest.isAlive()) return;
        label.setPos(guest.getX(), guest.getY() + 1.85, guest.getZ());
    }

    private static void despawnLabel(Run run) {
        if (run.labelId == null) return;
        Entity e = run.level.getEntity(run.labelId);
        if (e != null) e.discard();
        run.labelId = null;
    }

    private static Component labelComponent(Run run) {
        return switch (run.kind) {
            case CRATE -> Component.literal("Груз").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            case SHRINE -> Component.literal("Тотем").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
            case GUEST -> Component.literal("Гость").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
        };
    }

    private static void particles(Run run) {
        double x = run.origin.getX() + 0.5;
        double z = run.origin.getZ() + 0.5;
        double baseY = run.origin.getY() + 0.9;
        int color = colorOf(run.kind);
        DustParticleOptions dust = new DustParticleOptions(color, 1.35f);
        DustParticleOptions dustSoft = new DustParticleOptions(0xFFFFFF, 0.7f);
        double spin = (run.durationSec * 20 - run.ticksLeft) * 0.18;
        if (run.kind == Kind.CRATE) {
            int h = 18;
            for (int i = 0; i < h; i++) {
                if ((run.ticksLeft + i) % 2 != 0) continue;
                double y = baseY + i * 0.72;
                double ang = spin + i * 0.38;
                double r = 0.28 + (i % 4) * 0.04;
                boom(run.level, dust, x + Math.cos(ang) * r, y, z + Math.sin(ang) * r, 1);
                if (i % 3 == 0) boom(run.level, dustSoft, x, y, z, 1);
            }
            boom(run.level, ParticleTypes.ELECTRIC_SPARK, x, baseY + 10, z, 3);
            boom(run.level, ParticleTypes.END_ROD, x, baseY + 14, z, 2);
            if (run.ticksLeft % 8 == 0) {
                boom(run.level, ParticleTypes.FIREWORK, x, baseY + 12, z, 6);
            }
            if (run.ticksLeft % 50 == 0) {
                run.level.playSound(null, run.origin, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.28f, 1.55f);
            }
        } else if (run.kind == Kind.SHRINE) {
            for (int i = 0; i < 8; i++) {
                double ang = spin + i * (Math.PI / 4);
                double r = 0.85;
                boom(run.level, dust, x + Math.cos(ang) * r, baseY + 0.35, z + Math.sin(ang) * r, 1);
            }
            boom(run.level, ParticleTypes.WITCH, x, baseY + 0.8, z, 3);
            boom(run.level, ParticleTypes.PORTAL, x, baseY + 0.6, z, 5);
            boom(run.level, ParticleTypes.SOUL_FIRE_FLAME, x, baseY + 1.2, z, 1);
        } else {
            Entity e = run.guestId == null ? null : run.level.getEntity(run.guestId);
            double gx = e != null ? e.getX() : x;
            double gy = e != null ? e.getY() + 0.7 : baseY;
            double gz = e != null ? e.getZ() : z;
            boom(run.level, ParticleTypes.HEART, gx, gy + 0.5, gz, 1);
            boom(run.level, ParticleTypes.NOTE, gx, gy + 0.3, gz, 1);
            boom(run.level, dust, gx, gy, gz, 2);
            boom(run.level, ParticleTypes.END_ROD, gx, gy + 0.8, gz, 1);
        }
    }

    private static void boom(ServerLevel level, net.minecraft.core.particles.ParticleOptions opts,
                             double x, double y, double z, int count) {
        level.sendParticles(opts, true, true, x, y, z, count, 0.02, 0.04, 0.02, 0.0);
    }

    private static void burst(Run run) {
        double x = run.origin.getX() + 0.5;
        double y = run.origin.getY();
        double z = run.origin.getZ() + 0.5;
        run.level.playSound(null, run.origin, SoundEvents.ENDER_EYE_LAUNCH, SoundSource.BLOCKS, 1.05f, 0.65f);
        run.level.playSound(null, run.origin, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.35f, 1.2f);
        sparkBurst(run, colorOf(run.kind));
        firework(run, colorOf(run.kind), FireworkExplosion.Shape.LARGE_BALL);
        if (run.kind == Kind.CRATE) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(run.level, EntitySpawnReason.EVENT);
            if (bolt != null) {
                bolt.setPos(x, y, z);
                bolt.setVisualOnly(true);
                run.level.addFreshEntity(bolt);
            }
        }
        run.level.sendParticles(ParticleTypes.FIREWORK, true, true, x, y + 10, z, 28, 0.5, 2.2, 0.5, 0.08);
        run.level.sendParticles(ParticleTypes.GLOW, true, true, x, y + 2, z, 18, 0.6, 1.2, 0.6, 0.02);
    }

    private static void sparkBurst(Run run, int color) {
        DustParticleOptions dust = new DustParticleOptions(color, 1.6f);
        double x = run.origin.getX() + 0.5;
        double y = run.origin.getY() + 1.2;
        double z = run.origin.getZ() + 0.5;
        run.level.sendParticles(dust, true, true, x, y, z, 24, 0.7, 0.8, 0.7, 0.02);
        run.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, true, x, y, z, 16, 0.5, 0.6, 0.5, 0.08);
    }

    private static void firework(Run run, int color, FireworkExplosion.Shape shape) {
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        FireworkExplosion boom = new FireworkExplosion(
                shape,
                IntList.of(color, 0xFFFFFF),
                IntList.of(0xFFD700),
                true, true);
        stack.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(boom)));
        FireworkRocketEntity fw = new FireworkRocketEntity(run.level, stack,
                run.origin.getX() + 0.5, run.origin.getY() + 8, run.origin.getZ() + 0.5, true);
        run.level.addFreshEntity(fw);
    }

    private static void burstChickens(Run run, int count) {
        for (int i = 0; i < count; i++) {
            Chicken chicken = EntityType.CHICKEN.create(run.level, EntitySpawnReason.EVENT);
            if (chicken == null) continue;
            chicken.setPos(run.origin.getX() + 0.5 + (i % 3 - 1) * 0.6,
                    run.origin.getY() + 0.4,
                    run.origin.getZ() + 0.5 + (i / 3) * 0.5);
            chicken.setCustomName(Component.literal("§e" + trunc(run.viewer, 12)));
            chicken.setCustomNameVisible(true);
            run.level.addFreshEntity(chicken);
            ChatMobBudget.track(chicken);
        }
    }

    private static void announce(MinecraftServer server, Run run, ServerPlayer host) {
        int dist = (int) Math.sqrt(host.blockPosition().distSqr(run.origin));
        String dir = compass(host, run.origin);
        String title = switch (run.kind) {
            case CRATE -> "§6§lГРУЗ";
            case SHRINE -> "§5§lТОТЕМ";
            case GUEST -> "§d§lГОСТЬ";
        };
        String sub = "§f" + run.viewer + "  §e" + dist + "м " + dir;
        for (ServerPlayer p : StreamTargets.onlineStreamers(server)) {
            AnnouncementService.flashTitle(p, title, sub);
        }
        String chat = switch (run.kind) {
            case CRATE -> "Груз чата от " + run.viewer + "! " + dist + "м на " + dir
                    + ". " + run.durationSec + " секунд.";
            case SHRINE -> "Тотем чата от " + run.viewer + "! " + dist + "м на " + dir
                    + ". Тронь — усиление.";
            case GUEST -> "Гость " + run.viewer + "! " + dist + "м на " + dir
                    + ". Нажми на свинью — посылка.";
        };
        TwitchCraftMod.getConnectionManager().sendChatMessage(chat);
    }

    private static void hudAll(MinecraftServer server, String msg) {
        if (server == null) return;
        for (ServerPlayer p : StreamTargets.onlineStreamers(server)) {
            AnnouncementService.announceHud(p, msg);
        }
    }

    private static BlockPos findSite(ServerLevel level, ServerPlayer p) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 18; attempt++) {
            int dist = 8 + r.nextInt(7);
            double ang = r.nextDouble() * Math.PI * 2;
            int dx = (int) Math.round(Math.cos(ang) * dist);
            int dz = (int) Math.round(Math.sin(ang) * dist);
            BlockPos probe = p.blockPosition().offset(dx, 0, dz);
            BlockPos floor = findFloor(level, probe, p.getBlockY());
            if (floor == null) continue;
            BlockPos air = floor.above();
            if (SpawnGuard.isProtected(level, floor) || SpawnGuard.isProtected(level, air)) continue;
            if (SpawnGuard.isProtected(level, air.above()) || SpawnGuard.isProtected(level, air.above(2))) continue;
            if (!free(level, air) || !free(level, air.above()) || !free(level, air.above(2))) continue;
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) continue;
            if (!level.getFluidState(air).isEmpty()) continue;
            return air;
        }
        return null;
    }

    private static BlockPos findFloor(ServerLevel level, BlockPos start, int playerY) {
        BlockPos pos = new BlockPos(start.getX(), Math.min(level.getMaxY() - 4, playerY + 6), start.getZ());
        for (int i = 0; i < 18; i++) {
            BlockState here = level.getBlockState(pos);
            BlockState below = level.getBlockState(pos.below());
            if ((here.isAir() || here.canBeReplaced())
                    && below.isFaceSturdy(level, pos.below(), Direction.UP)
                    && below.getFluidState().isEmpty()) {
                int y = pos.below().getY();
                if (Math.abs(y - playerY) > 8) return null;
                return pos.below();
            }
            pos = pos.below();
        }
        return null;
    }

    private static boolean free(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.isAir() || s.canBeReplaced();
    }

    private static ServerPlayer pickOverworldHost(MinecraftServer server) {
        List<ServerPlayer> overworld = new ArrayList<>();
        for (ServerPlayer p : StreamTargets.onlineStreamers(server)) {
            if (p.level().dimension() == Level.OVERWORLD && p.isAlive()) overworld.add(p);
        }
        if (overworld.isEmpty()) return null;
        return overworld.get(ThreadLocalRandom.current().nextInt(overworld.size()));
    }

    private static ServerPlayer nearestStreamer(Run run) {
        MinecraftServer server = run.level.getServer();
        if (server == null) return null;
        ServerPlayer best = null;
        double bestD = Double.MAX_VALUE;
        for (ServerPlayer p : StreamTargets.onlineStreamers(server)) {
            if (p.level() != run.level) continue;
            double d = p.blockPosition().distSqr(run.origin);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private static Kind nextKind() {
        Kind[] all = Kind.values();
        Kind pick = all[ThreadLocalRandom.current().nextInt(all.length)];
        if (pick == lastKind) {
            pick = all[(pick.ordinal() + 1) % all.length];
        }
        return pick;
    }

    private static String pickViewer(String fallback) {
        List<String> names = ChatViewerRegistry.recentDisplayNames();
        if (!names.isEmpty()) {
            return names.get(ThreadLocalRandom.current().nextInt(names.size()));
        }
        return fallback != null && !fallback.isBlank() ? fallback : "чат";
    }

    private static void scheduleNext() {
        var s = ConfigManager.get().stream;
        int min = Math.max(6, s.worldEncounterMinMinutes);
        int max = Math.max(min, s.worldEncounterMaxMinutes);
        int minutes = min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
        nextAt = System.currentTimeMillis() + minutes * 60_000L;
    }

    private static boolean enabled() {
        return ConfigManager.get().stream.worldEncountersEnabled;
    }

    private static boolean owns(Run run, BlockPos pos) {
        for (Placed p : run.blocks) {
            if (p.pos().equals(pos)) return true;
        }
        return false;
    }

    private static int secondsLeft(Run run) {
        if (run.claimed) return Math.max(1, (run.lingerTicks + 19) / 20);
        return Math.max(1, (run.ticksLeft + 19) / 20);
    }

    private static String prefix(Run run) {
        return switch (run.kind) {
            case CRATE -> "§6ГРУЗ";
            case SHRINE -> "§5ТОТЕМ";
            case GUEST -> "§dГОСТЬ";
        };
    }

    private static String titleOf(Kind kind) {
        return switch (kind) {
            case CRATE -> "ГРУЗ";
            case SHRINE -> "ТОТЕМ";
            case GUEST -> "ГОСТЬ";
        };
    }

    private static int colorOf(Kind kind) {
        return switch (kind) {
            case CRATE -> 0xF1C40F;
            case SHRINE -> 0x9B59B6;
            case GUEST -> 0xFF7AA8;
        };
    }

    private static String compass(ServerPlayer p, BlockPos target) {
        int dx = target.getX() - p.blockPosition().getX();
        int dz = target.getZ() - p.blockPosition().getZ();
        if (Math.abs(dx) < 4 && Math.abs(dz) < 4) return "здесь";
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        if (angle < 0) angle += 360;
        if (angle < 22.5 || angle >= 337.5) return "север";
        if (angle < 67.5) return "северо-восток";
        if (angle < 112.5) return "восток";
        if (angle < 157.5) return "юго-восток";
        if (angle < 202.5) return "юг";
        if (angle < 247.5) return "юго-запад";
        if (angle < 292.5) return "запад";
        return "северо-запад";
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§e" + trunc(name, 18)));
        return stack;
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void dropAt(Run run, ItemStack stack) {
        run.level.addFreshEntity(new ItemEntity(run.level,
                run.origin.getX() + 0.5, run.origin.getY() + 0.5, run.origin.getZ() + 0.5, stack));
    }

    private static String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
