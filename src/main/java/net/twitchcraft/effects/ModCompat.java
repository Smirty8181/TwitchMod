package net.twitchcraft.effects;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.twitchcraft.feedback.AnnouncementService;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/** Soft-зависимости на моды из stream-профиля (без compile-зависимостей). */
public final class ModCompat {
    private ModCompat() {}

    public static boolean isLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static Optional<Item> item(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null) return Optional.empty();
        Item item = BuiltInRegistries.ITEM.getValue(loc);
        if (item == null || item == Items.AIR) return Optional.empty();
        return Optional.of(item);
    }

    public static Optional<EntityType<?>> entity(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null) return Optional.empty();
        return BuiltInRegistries.ENTITY_TYPE.getOptional(loc);
    }

    public static Optional<Block> block(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null) return Optional.empty();
        return BuiltInRegistries.BLOCK.getOptional(loc)
                .filter(b -> b != Blocks.AIR);
    }

    public static Optional<SoundEvent> sound(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null) return Optional.empty();
        return BuiltInRegistries.SOUND_EVENT.getOptional(loc);
    }

    public static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public static boolean placeNear(ServerPlayer player, Block block, int dx, int dy, int dz) {
        ServerLevel level = player.level();
        BlockPos pos = player.blockPosition().offset(dx, dy, dz);
        BlockState state = block.defaultBlockState();
        if (!level.getBlockState(pos).canBeReplaced()) {
            pos = player.blockPosition().above();
        }
        if (!level.getBlockState(pos).canBeReplaced()
                && !level.getBlockState(pos).isAir()) {
            return false;
        }
        return level.setBlock(pos, state, 3);
    }

    public static void playOptional(ServerLevel level, BlockPos pos, String soundId,
                                    SoundEvent fallback, float volume, float pitch) {
        SoundEvent event = sound(soundId).orElse(fallback);
        level.playSound(null, pos, event, SoundSource.PLAYERS, volume, pitch);
    }

    /** Chaos Chickens: назначает трейт ванильной курице через reflection. */
    public static boolean assignChaosTrait(Chicken chicken, String traitKey) {
        if (!isLoaded("chaoschickens") || chicken == null) return false;
        try {
            Class<?> traitTypeCls = Class.forName("com.chaoschickens.common.trait.TraitType");
            Method fromKey = traitTypeCls.getMethod("fromKey", String.class);
            Object trait = fromKey.invoke(null, traitKey);
            if (trait == null) return false;

            Class<?> fabricCls = Class.forName("com.chaoschickens.fabric.ChaosChickensFabric");
            Method assign = fabricCls.getMethod("assignTrait", Chicken.class, traitTypeCls);
            assign.invoke(null, chicken, trait);
            return true;
        } catch (Throwable t) {
            try {
                Class<?> util = Class.forName("com.chaoschickens.fabric.util.ChickenDataUtil");
                Class<?> traitTypeCls = Class.forName("com.chaoschickens.common.trait.TraitType");
                Method fromKey = traitTypeCls.getMethod("fromKey", String.class);
                Object trait = fromKey.invoke(null, traitKey);
                Method setTrait = util.getMethod("setTrait", Chicken.class, traitTypeCls);
                setTrait.invoke(null, chicken, trait);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    public static String randomChaosTrait() {
        String[] keys = {
                "explosive", "speed", "fire", "magnet", "golden",
                "disco", "zombie", "teleport", "ice", "cursed", "boss"
        };
        return keys[ThreadLocalRandom.current().nextInt(keys.length)];
    }

    /** Pehkui: настоящий scale, если мод стоит. Иначе молча пропускаем — ванильный Attributes.SCALE уже сработал. */
    public static void setPehkuiScale(Entity entity, float scale) {
        if (!isLoaded("pehkui") || entity == null) return;
        try {
            Class<?> types = Class.forName("virtuoel.pehkui.api.ScaleTypes");
            Object base = types.getField("BASE").get(null);
            Object data = base.getClass().getMethod("getScaleData", Entity.class).invoke(base, entity);
            data.getClass().getMethod("setScale", float.class).invoke(data, scale);
        } catch (Throwable ignored) {
        }
    }

    public static void missingMod(net.twitchcraft.commands.CommandContext ctx, String modName) {
        if (ctx != null) ctx.fail();
        missingMod(ctx != null ? ctx.target : null, modName);
    }

    public static void missingMod(ServerPlayer player, String modName) {
        AnnouncementService.announceCustom(player,
                "§cНужен мод §f" + modName + "§c — сейчас его нет в профиле");
    }

    /** Carry On: попытаться поднять сущность в руки. */
    public static boolean tryCarryOnPickup(ServerPlayer player, Entity entity) {
        if (!isLoaded("carryon") || player == null || entity == null) return false;
        try {
            Class<?> handler = Class.forName("tschipp.carryon.common.carry.PickupHandler");
            java.lang.reflect.Method m = handler.getMethod(
                    "tryPickupEntity",
                    ServerPlayer.class,
                    Entity.class,
                    java.util.function.Function.class);
            Object result = m.invoke(null, player, entity, (java.util.function.Function<Entity, Boolean>) e -> true);
            return result instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }

    private static final String[] RANDOM_SKIN_NAMES = {
            "Notch", "jeb_", "Dinnerbone", "Grumm", "Steve", "Alex",
            "Dream", "Technoblade", "TommyInnit", "Sapnap", "GeorgeNotFound",
            "Ph1LzA", "WilburSoot", "Ranboo", "Fundy", "Quackity",
            "CaptainSparklez", "DanTDM", "PewDiePie", "Grian", "MumboJumbo",
            "SolidarityGaming", "SmallishBeans", "PearlescentMoon", "InTheLittleWood"
    };

    public static String randomSkinName(String prefer) {
        if (prefer != null && prefer.length() >= 3 && prefer.length() <= 16
                && prefer.matches("[A-Za-z0-9_]+")) {
            if (ThreadLocalRandom.current().nextBoolean()) return prefer;
        }
        return RANDOM_SKIN_NAMES[ThreadLocalRandom.current().nextInt(RANDOM_SKIN_NAMES.length)];
    }

    /** Easy NPC: выставить player-скин на humanoid NPC. */
    public static boolean applyEasyNpcPlayerSkin(Entity npc, String playerName) {
        if (!isLoaded("easy_npc") || npc == null || playerName == null || playerName.isBlank()) return false;
        try {
            Class<?> entryCls = Class.forName("de.markusbordihn.easynpc.data.skin.SkinDataEntry");
            Method create = entryCls.getMethod("createPlayerSkin", String.class, java.util.UUID.class);
            Object entry = create.invoke(null, playerName, java.util.UUID.randomUUID());

            Class<?> easyNpc = Class.forName("de.markusbordihn.easynpc.entity.easynpc.EasyNPC");
            if (!easyNpc.isInstance(npc)) return false;

            Class<?> handler = Class.forName("de.markusbordihn.easynpc.handler.SkinHandler");
            Method setSkin = handler.getMethod("setSkin", easyNpc, entryCls);
            Object ok = setSkin.invoke(null, npc, entry);
            return !(ok instanceof Boolean b) || b;
        } catch (Throwable t) {
            try {
                // fallback: дефолтный вариант Steve/Alex/...
                Class<?> entryCls = Class.forName("de.markusbordihn.easynpc.data.skin.SkinDataEntry");
                Method createDefault = entryCls.getMethod("createDefaultSkin", String.class);
                String[] variants = {"steve", "alex", "ari", "efe", "kai", "makena", "noor", "sunny", "zuri"};
                Object entry = createDefault.invoke(null,
                        variants[ThreadLocalRandom.current().nextInt(variants.length)]);
                Class<?> easyNpc = Class.forName("de.markusbordihn.easynpc.entity.easynpc.EasyNPC");
                Class<?> handler = Class.forName("de.markusbordihn.easynpc.handler.SkinHandler");
                Method setSkin = handler.getMethod("setSkin", easyNpc, entryCls);
                setSkin.invoke(null, npc, entry);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }
}
