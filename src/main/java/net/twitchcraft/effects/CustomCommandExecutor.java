package net.twitchcraft.effects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.twitchcraft.config.CustomCommandDefinition;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.stream.ChatMobBudget;
import net.twitchcraft.feedback.AnnouncementService;

public final class CustomCommandExecutor {
    private CustomCommandExecutor() {}

    public static void execute(ServerPlayer target, String twitchUser, CustomCommandDefinition def) {
        if (def == null || target == null) return;
        String type = def.actionType == null ? "effect" : def.actionType.toLowerCase();
        switch (type) {
            case "item" -> giveItem(target, twitchUser, def);
            case "mob" -> spawnMobs(target, twitchUser, def);
            default -> applyEffect(target, twitchUser, def);
        }
    }

    private static void applyEffect(ServerPlayer target, String twitchUser, CustomCommandDefinition def) {
        Holder<MobEffect> effect = resolveEffect(def.effectId);
        if (effect == null) return;
        int ticks = Math.max(20, def.durationSeconds * 20);
        int amp = Math.max(0, Math.min(def.amplifier, 255));
        target.addEffect(new MobEffectInstance(effect, ticks, amp));
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.BREWING_STAND_BREW,
                SoundSource.PLAYERS, 0.7f, 1.2f);
        level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + 1, target.getZ(),
                12, 0.4, 0.5, 0.4, 0.05);
        AnnouncementService.announceCustom(target,
                twitchUser + " → §e!" + def.displayName() + " §7(эффект)");
    }

    private static void giveItem(ServerPlayer target, String twitchUser, CustomCommandDefinition def) {
        Item item = resolveItem(def.itemId);
        if (item == null) return;
        int count = Math.max(1, Math.min(def.itemCount, 64));
        ItemStack stack = new ItemStack(item, count);
        if (!target.getInventory().add(stack)) {
            target.drop(stack, false);
        }
        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS, 0.8f, 1f);
        AnnouncementService.announceCustom(target,
                twitchUser + " → §e!" + def.displayName() + " §7(предмет ×" + count + ")");
    }

    private static void spawnMobs(ServerPlayer target, String twitchUser, CustomCommandDefinition def) {
        EntityType<?> type = resolveEntity(def.entityId);
        if (type == null) return;
        int count = Math.max(1, Math.min(def.mobCount, 10));
        ServerLevel level = target.level();
        for (int i = 0; i < count; i++) {
            Entity entity = type.create(level, EntitySpawnReason.COMMAND);
            if (entity == null) continue;
            double x = target.getX() + level.random.nextInt(5) - 2;
            double z = target.getZ() + level.random.nextInt(5) - 2;
            entity.setPos(x, target.getY(), z);
            nameEntity(entity, twitchUser, def.displayName());
            level.addFreshEntity(entity);
            ChatMobBudget.track(entity);
            if (ConfigManager.get().stream.ghostMobsEnabled && twitchUser != null && !twitchUser.isBlank()) {
                net.twitchcraft.stream.GhostMobRegistry.register(entity, twitchUser, def.displayName(),
                        target.getGameProfile().name());
            }
        }
        level.playSound(null, target.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON,
                SoundSource.PLAYERS, 0.9f, 0.9f);
    }

    private static void nameEntity(Entity entity, String twitchUser, String mobRu) {
        if (!ConfigManager.get().stream.namedMobs || twitchUser == null || twitchUser.isBlank()) return;
        String name = twitchUser.length() > 16 ? twitchUser.substring(0, 16) : twitchUser;
        entity.setCustomName(Component.literal("§c[" + name + "]"));
        entity.setCustomNameVisible(true);
    }

    private static Holder<MobEffect> resolveEffect(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null) return null;
        return BuiltInRegistries.MOB_EFFECT.get(loc).orElse(null);
    }

    private static Item resolveItem(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null) return null;
        Item item = BuiltInRegistries.ITEM.getValue(loc);
        return item == null || item == net.minecraft.world.item.Items.AIR ? null : item;
    }

    private static EntityType<?> resolveEntity(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.getValue(loc);
    }
}
