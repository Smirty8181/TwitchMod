package net.twitchcraft.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.feedback.AnnouncementService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** !паркур — летающая дорожка, лут за финиш, хаос если упал. */
public final class ParkourChallenge {
    private static final Map<UUID, Run> ACTIVE = new ConcurrentHashMap<>();
    private static final int VANISH_EVERY = 8;
    private static final int MAX_TICKS = 20 * 16;

    private record Step(BlockPos pos, BlockState previous) {}

    private static final class Run {
        final ServerLevel level;
        final List<Step> remaining;
        final BlockPos end;
        final int startY;
        final String viewer;
        int ticks;
        int sinceVanish;

        Run(ServerLevel level, List<Step> steps, BlockPos end, int startY, String viewer) {
            this.level = level;
            this.remaining = steps;
            this.end = end;
            this.startY = startY;
            this.viewer = viewer;
        }
    }

    private ParkourChallenge() {}

    public static void start(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        Run old = ACTIVE.remove(p.getUUID());
        if (old != null) restoreAll(old);

        ServerLevel level = p.level();
        Direction dir = p.getDirection();
        BlockPos cursor = p.blockPosition().relative(dir, 2).above(2);
        List<Step> steps = new ArrayList<>();
        BlockState path = Blocks.LIME_CONCRETE.defaultBlockState();
        BlockState endBlock = Blocks.GOLD_BLOCK.defaultBlockState();
        for (int i = 0; i < 10; i++) {
            cursor = cursor.relative(dir, 2);
            if (i % 3 == 2) cursor = cursor.above();
            tryPlace(level, cursor, path, steps);
        }
        BlockPos end = cursor.relative(dir, 2);
        tryPlace(level, end, endBlock, steps);
        tryPlace(level, end.relative(dir.getClockWise()), endBlock, steps);
        tryPlace(level, end.relative(dir.getCounterClockWise()), endBlock, steps);

        if (steps.size() < 6) {
            restoreList(level, steps);
            AnnouncementService.announceCustom(p, "§eПаркур §7не влез — мало воздуха");
            ctx.fail();
            return;
        }
        ACTIVE.put(p.getUUID(), new Run(level, steps, end, cursor.getY(), ctx.twitchUser));
        level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(),
                SoundSource.PLAYERS, 1f, 1.4f);
        AnnouncementService.announceCustom(p,
                "§b§lПаркур §7беги по дорожке · лут на финише · от §f" + ctx.twitchUser);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "ПАРКУР от " + ctx.twitchUser + "! Если стример упадёт — чат ржёт.");
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Run>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Run> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            Run run = e.getValue();
            run.ticks++;
            run.sinceVanish++;
            if (p == null || !p.isAlive() || p.level() != run.level) {
                restoreAll(run);
                it.remove();
                continue;
            }
            if (reachedEnd(p, run)) {
                restoreAll(run);
                it.remove();
                reward(p, run);
                continue;
            }
            if (p.getY() < run.startY - 4 || run.ticks >= MAX_TICKS || run.remaining.isEmpty()) {
                restoreAll(run);
                it.remove();
                fail(server, p, run);
                continue;
            }
            if (run.sinceVanish >= VANISH_EVERY && run.remaining.size() > 3) {
                run.sinceVanish = 0;
                Step step = run.remaining.removeFirst();
                restoreOne(run.level, step);
                run.level.sendParticles(ParticleTypes.CLOUD,
                        step.pos().getX() + 0.5, step.pos().getY() + 0.5, step.pos().getZ() + 0.5,
                        6, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }

    public static String statusLine(ServerPlayer player) {
        Run r = ACTIVE.get(player.getUUID());
        if (r == null) return "";
        return "§b✦ ПАРКУР §f" + Math.max(1, (MAX_TICKS - r.ticks + 19) / 20) + "с";
    }

    public static void clear() {
        for (Run r : ACTIVE.values()) restoreAll(r);
        ACTIVE.clear();
    }

    private static void tryPlace(ServerLevel level, BlockPos pos, BlockState state, List<Step> out) {
        if (SpawnGuard.isProtected(level, pos)) return;
        BlockState prev = level.getBlockState(pos);
        if (!prev.isAir() && !prev.canBeReplaced()) return;
        if (!SpawnGuard.replace(level, pos, state)) return;
        out.add(new Step(pos.immutable(), prev));
    }

    private static boolean reachedEnd(ServerPlayer p, Run run) {
        BlockPos feet = p.blockPosition().below();
        return feet.distManhattan(run.end) <= 2 && p.onGround();
    }

    private static void reward(ServerPlayer p, Run run) {
        Direction face = p.getDirection().getOpposite();
        BlockPos chestPos = run.end.above();
        if (!run.level.getBlockState(chestPos).isAir()) {
            chestPos = p.blockPosition().relative(p.getDirection());
        }
        BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, face);
        if (SpawnGuard.replace(run.level, chestPos, chest)
                && run.level.getBlockEntity(chestPos) instanceof ChestBlockEntity box) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            box.setItem(0, new ItemStack(Items.IRON_INGOT, 8 + r.nextInt(8)));
            box.setItem(1, new ItemStack(Items.COAL, 16));
            box.setItem(2, new ItemStack(Items.BREAD, 8));
            box.setItem(3, new ItemStack(Items.GOLD_INGOT, 2 + r.nextInt(3)));
            box.setItem(4, new ItemStack(Items.IRON_PICKAXE));
            box.setChanged();
        } else {
            p.getInventory().add(new ItemStack(Items.IRON_INGOT, 12));
        }
        run.level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.PLAYERS, 0.9f, 1.2f);
        AnnouncementService.announceCustom(p, "§a§lПаркур пройден §7сундук на финише");
        TwitchCraftMod.getConnectionManager().sendChatMessage("Паркур пройден! Лут у стримера.");
    }

    private static void fail(MinecraftServer server, ServerPlayer p, Run run) {
        AnnouncementService.announceCustom(p, "§c§lУпал с паркура §7чат ржёт");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "ХАХА " + run.viewer + " уронил стримера с паркура. +хаос");
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_NO,
                SoundSource.PLAYERS, 1f, 0.7f);
        ChaosMeter.addBonus(server, p, 12);
    }

    private static void restoreAll(Run run) {
        restoreList(run.level, run.remaining);
        run.remaining.clear();
    }

    private static void restoreList(ServerLevel level, List<Step> steps) {
        for (Step step : steps) restoreOne(level, step);
    }

    private static void restoreOne(ServerLevel level, Step step) {
        BlockState now = level.getBlockState(step.pos());
        if (now.is(Blocks.LIME_CONCRETE) || now.is(Blocks.GOLD_BLOCK)) {
            SpawnGuard.replace(level, step.pos(), step.previous());
        }
    }
}
