package net.twitchcraft.content;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.Vec3;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.stream.GhostMobRegistry;

import java.util.List;

public final class ViewerEggService {
    private ViewerEggService() {}

    public static ItemLore lore(String viewer) {
        return new ItemLore(List.of(
                Component.literal("§7Зритель §d" + viewer),
                Component.literal("§8ПКМ — призвать ghost-моба")));
    }

    public static String viewerFromStack(ItemStack stack) {
        if (stack.isEmpty()) return "";
        var name = stack.get(DataComponents.CUSTOM_NAME);
        if (name == null) return "";
        String raw = name.getString();
        int open = raw.indexOf('[');
        int close = raw.indexOf(']');
        if (open >= 0 && close > open) {
            return raw.substring(open + 1, close).trim();
        }
        return raw.replace("§", "").trim();
    }

    public static void spawnFromEgg(ServerLevel level, ServerPlayer target, String viewer) {
        if (!ConfigManager.get().stream.ghostMobsEnabled) return;
        Vec3 pos = target.position().add(
                level.random.nextDouble() * 4 - 2,
                0,
                level.random.nextDouble() * 4 - 2);
        var zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        if (zombie != null) {
            zombie.setPos(pos.x, pos.y, pos.z);
            zombie.setCustomName(Component.literal("§d[" + viewer + "]"));
            zombie.setCustomNameVisible(true);
            GhostMobRegistry.register(zombie, viewer, "зомби", target.getGameProfile().name());
            level.addFreshEntity(zombie);
        }
    }

    public static void giveEgg(ServerPlayer player, String viewer) {
        ItemStack egg = net.twitchcraft.item.ViewerEggItem.create(viewer);
        if (!player.getInventory().add(egg)) {
            player.drop(egg, false);
        }
    }
}
