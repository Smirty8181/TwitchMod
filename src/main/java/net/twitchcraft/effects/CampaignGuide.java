package net.twitchcraft.effects;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.feedback.AnnouncementService;

/**
 * Чат — навигатор прохождения: деревня, крепость, бастион, стронгхолд, портал.
 */
public final class CampaignGuide {
    private CampaignGuide() {}

    public static void village(CommandContext ctx) {
        locateTag(ctx, StructureTags.VILLAGE, "деревня", ctx.server.overworld(), false);
    }

    public static void fortress(CommandContext ctx) {
        ServerLevel nether = ctx.server.getLevel(Level.NETHER);
        if (nether == null) {
            AnnouncementService.announceCustom(ctx.target, "§7Ад ещё не сгенерирован");
            ctx.fail();
            return;
        }
        locateKey(ctx, BuiltinStructures.FORTRESS, "крепость Незера", nether, true);
    }

    public static void bastion(CommandContext ctx) {
        ServerLevel nether = ctx.server.getLevel(Level.NETHER);
        if (nether == null) {
            AnnouncementService.announceCustom(ctx.target, "§7Ад ещё не сгенерирован");
            ctx.fail();
            return;
        }
        locateKey(ctx, BuiltinStructures.BASTION_REMNANT, "бастион", nether, true);
    }

    public static void stronghold(CommandContext ctx) {
        locateKey(ctx, BuiltinStructures.STRONGHOLD, "крепость Края", ctx.server.overworld(), false);
    }

    public static void portalKit(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ModCompat.giveOrDrop(p, new ItemStack(Items.OBSIDIAN, 8));
        ModCompat.giveOrDrop(p, new ItemStack(Items.FLINT_AND_STEEL, 1));
        AnnouncementService.announceCustom(p, "§6" + ctx.twitchUser + " §7дал набор портала §8(8 обсидиана)");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                ctx.twitchUser + " дал стримеру набор портала.");
    }

    public static void fireRes(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        if (p.level().dimension() != Level.NETHER) {
            AnnouncementService.announceCustom(p, "§7Огнестойкость только в Аду");
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    "Огнестойкость работает только когда стример в Аду.");
            ctx.fail();
            return;
        }
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600, 0));
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOTTLE_EMPTY,
                SoundSource.PLAYERS, 0.9f, 1.15f);
        p.level().sendParticles(ParticleTypes.FLAME,
                p.getX(), p.getY() + 1, p.getZ(), 18, 0.4, 0.5, 0.4, 0.02);
        p.level().sendParticles(ParticleTypes.LAVA,
                p.getX(), p.getY() + 0.2, p.getZ(), 6, 0.3, 0.1, 0.3, 0.0);
        AnnouncementService.announceCustom(p, "§c§lОГНЕСТОЙКОСТЬ §e3 мин §7от §f" + ctx.twitchUser);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                ctx.twitchUser + " дал огнестойкость на 3 минуты в Аду.");
    }

    public static void enderEye(CommandContext ctx) {
        ServerPlayer p = ctx.target;
        ModCompat.giveOrDrop(p, new ItemStack(Items.ENDER_EYE, 1));
        AnnouncementService.announceCustom(p, "§5" + ctx.twitchUser + " §7дал око Края");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                ctx.twitchUser + " дал око Края. Кидай и ищи крепость!");
    }

    private static void locateKey(CommandContext ctx, ResourceKey<Structure> key, String ru,
                                  ServerLevel search, boolean netherScale) {
        var lookup = search.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var holder = lookup.get(key);
        if (holder.isEmpty()) {
            AnnouncementService.announceCustom(ctx.target, "§7" + ru + " нет в этом мире");
            ctx.fail();
            return;
        }
        finishLocate(ctx, HolderSet.direct(holder.get()), ru, search, netherScale);
    }

    private static void locateTag(CommandContext ctx, TagKey<Structure> tag, String ru,
                                  ServerLevel search, boolean netherScale) {
        var lookup = search.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var holders = lookup.get(tag);
        if (holders.isEmpty()) {
            AnnouncementService.announceCustom(ctx.target, "§7" + ru + " нет в этом мире");
            ctx.fail();
            return;
        }
        finishLocate(ctx, holders.get(), ru, search, netherScale);
    }

    private static void finishLocate(CommandContext ctx, HolderSet<Structure> set, String ru,
                                     ServerLevel search, boolean netherScale) {
        ServerPlayer p = ctx.target;
        BlockPos origin;
        if (p.level() == search) {
            origin = p.blockPosition();
        } else if (netherScale && p.level().dimension() == Level.OVERWORLD) {
            origin = new BlockPos(p.getBlockX() / 8, 64, p.getBlockZ() / 8);
        } else {
            origin = new BlockPos(0, 64, 0);
        }

        Pair<BlockPos, Holder<Structure>> pair = search.getChunkSource().getGenerator()
                .findNearestMapStructure(search, set, origin, 120, false);
        if (pair == null) {
            AnnouncementService.announceCustom(p, "§7" + ru + " не найдена рядом");
            TwitchCraftMod.getConnectionManager().sendChatMessage(ru + " не найдена рядом.");
            ctx.fail();
            return;
        }

        BlockPos found = pair.getFirst();
        BlockPos show = found;
        String extra = "";
        if (netherScale && p.level().dimension() == Level.OVERWORLD) {
            show = new BlockPos(found.getX() * 8, p.getBlockY(), found.getZ() * 8);
            extra = " §8· портал около §f" + show.getX() + " " + show.getZ();
        }

        int dx = show.getX() - p.getBlockX();
        int dz = show.getZ() - p.getBlockZ();
        int dist = (int) Math.round(Math.sqrt((double) dx * dx + (double) dz * dz));
        String dir = compass(dx, dz);
        String line = ru + ": " + dist + "м на " + dir + "  (" + show.getX() + " / " + show.getZ() + ")";

        p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        trail(p, dx, dz);
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENDER_EYE_LAUNCH,
                SoundSource.PLAYERS, 0.7f, 1.1f);
        AnnouncementService.announceCustom(p, "§6§l" + ctx.twitchUser + " §7→ §e" + line + extra);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                ctx.twitchUser + " нашёл " + line + extra.replace("§8· портал около §f", " | портал надземья: "));
    }

    private static void trail(ServerPlayer p, int dx, int dz) {
        double len = Math.sqrt((double) dx * dx + (double) dz * dz);
        if (len < 1) return;
        ServerLevel level = p.level();
        for (int i = 1; i <= 10; i++) {
            double t = i * 1.6;
            level.sendParticles(ParticleTypes.END_ROD,
                    p.getX() + (dx / len) * t,
                    p.getY() + 1.2,
                    p.getZ() + (dz / len) * t,
                    2, 0.05, 0.1, 0.05, 0.0);
        }
    }

    private static String compass(int dx, int dz) {
        if (Math.abs(dx) < 8 && Math.abs(dz) < 8) return "здесь";
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
}
