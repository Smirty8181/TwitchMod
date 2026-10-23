package net.twitchcraft.stream;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Лестница апгрейда: дерево → камень → железо → алмаз → незерит. */
public final class ItemUpgradeLadder {
    public record Step(Item next, int chance, boolean protectedOnFail) {}

    private static final Map<Item, Step> MAP = new HashMap<>();
    private static final Set<Item> NEVER_DESTROY = Set.of(
            Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE,
            Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE, Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
            Items.NETHERITE_INGOT, Items.NETHERITE_BLOCK, Items.TOTEM_OF_UNDYING,
            Items.NETHER_STAR, Items.DRAGON_EGG, Items.ELYTRA, Items.BEACON,
            Items.SHULKER_BOX, Items.BUNDLE, Items.ENCHANTED_GOLDEN_APPLE
    );

    static {
        chain(Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
        chain(Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
        chain(Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);
        chain(Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL);
        chain(Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
        put(Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, 38);
        put(Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, 38);
        put(Items.GOLDEN_AXE, Items.DIAMOND_AXE, 38);
        put(Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL, 38);
        put(Items.GOLDEN_HOE, Items.DIAMOND_HOE, 38);

        chain(Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET);
        chain(Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE);
        chain(Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS);
        chain(Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS);
        put(Items.GOLDEN_HELMET, Items.IRON_HELMET, 48);
        put(Items.GOLDEN_CHESTPLATE, Items.IRON_CHESTPLATE, 48);
        put(Items.GOLDEN_LEGGINGS, Items.IRON_LEGGINGS, 48);
        put(Items.GOLDEN_BOOTS, Items.IRON_BOOTS, 48);
        put(Items.TURTLE_HELMET, Items.DIAMOND_HELMET, 28);
        put(Items.LEATHER_HORSE_ARMOR, Items.IRON_HORSE_ARMOR, 50);
        put(Items.IRON_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR, 42);
        put(Items.GOLDEN_HORSE_ARMOR, Items.DIAMOND_HORSE_ARMOR, 28);

        put(Items.BOW, Items.CROSSBOW, 36);
        put(Items.FISHING_ROD, Items.BOW, 40);
        put(Items.ARROW, Items.SPECTRAL_ARROW, 55);
        put(Items.SHIELD, Items.DIAMOND_CHESTPLATE, 12);

        put(Items.STICK, Items.WOODEN_SWORD, 62);
        put(Items.DIRT, Items.COBBLESTONE, 58);
        put(Items.COBBLESTONE, Items.IRON_INGOT, 40);
        put(Items.COAL, Items.IRON_INGOT, 52);
        put(Items.RAW_COPPER, Items.COPPER_INGOT, 70);
        put(Items.COPPER_INGOT, Items.IRON_INGOT, 48);
        put(Items.RAW_IRON, Items.IRON_INGOT, 70);
        put(Items.IRON_NUGGET, Items.IRON_INGOT, 60);
        put(Items.IRON_INGOT, Items.GOLD_INGOT, 42);
        put(Items.RAW_GOLD, Items.GOLD_INGOT, 70);
        put(Items.GOLD_NUGGET, Items.GOLD_INGOT, 60);
        put(Items.GOLD_INGOT, Items.DIAMOND, 32);
        put(Items.DIAMOND, Items.NETHERITE_INGOT, 18);
        put(Items.NETHERITE_INGOT, Items.NETHERITE_BLOCK, 16);
        put(Items.COAL_BLOCK, Items.IRON_BLOCK, 40);
        put(Items.IRON_BLOCK, Items.GOLD_BLOCK, 34);
        put(Items.GOLD_BLOCK, Items.DIAMOND_BLOCK, 26);
        put(Items.DIAMOND_BLOCK, Items.NETHERITE_BLOCK, 14);
        put(Items.EMERALD, Items.EMERALD_BLOCK, 30);
        put(Items.LAPIS_LAZULI, Items.DIAMOND, 22);
        put(Items.REDSTONE, Items.GOLD_INGOT, 35);
        put(Items.QUARTZ, Items.DIAMOND, 24);
        put(Items.AMETHYST_SHARD, Items.DIAMOND, 28);
        put(Items.LEATHER, Items.IRON_INGOT, 40);
        put(Items.FLINT, Items.IRON_INGOT, 44);

        put(Items.ROTTEN_FLESH, Items.COOKED_BEEF, 50);
        put(Items.BEEF, Items.COOKED_BEEF, 72);
        put(Items.PORKCHOP, Items.COOKED_PORKCHOP, 72);
        put(Items.CHICKEN, Items.COOKED_CHICKEN, 72);
        put(Items.COD, Items.COOKED_COD, 72);
        put(Items.SALMON, Items.COOKED_SALMON, 72);
        put(Items.POTATO, Items.BAKED_POTATO, 72);
        put(Items.BREAD, Items.GOLDEN_CARROT, 36);
        put(Items.CARROT, Items.GOLDEN_CARROT, 40);
        put(Items.APPLE, Items.GOLDEN_APPLE, 22);
        put(Items.GOLDEN_CARROT, Items.GOLDEN_APPLE, 24);
        put(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, 10);
        put(Items.MELON_SLICE, Items.GOLDEN_APPLE, 16);
    }

    private ItemUpgradeLadder() {}

    public static Step of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return MAP.get(stack.getItem());
    }

    public static boolean neverDestroy(ItemStack stack) {
        return stack != null && !stack.isEmpty() && NEVER_DESTROY.contains(stack.getItem());
    }

    public static ItemStack preview(ItemStack from) {
        Step step = of(from);
        if (step == null) return ItemStack.EMPTY;
        return copyStats(from, new ItemStack(step.next()));
    }

    public static ItemStack copyStats(ItemStack from, ItemStack to) {
        if (from == null || to == null || to.isEmpty()) return to;
        var ench = from.get(DataComponents.ENCHANTMENTS);
        if (ench != null && !ench.isEmpty()) to.set(DataComponents.ENCHANTMENTS, ench);
        if (from.has(DataComponents.CUSTOM_NAME)) {
            to.set(DataComponents.CUSTOM_NAME, from.get(DataComponents.CUSTOM_NAME));
        }
        int oldMax = from.getMaxDamage();
        int newMax = to.getMaxDamage();
        if (oldMax > 0 && newMax > 0 && from.isDamaged()) {
            float ratio = from.getDamageValue() / (float) oldMax;
            to.setDamageValue(Math.min(newMax - 1, Math.round(newMax * ratio)));
        }
        return to;
    }

    public static ItemStack jackpotKeep(ItemStack from) {
        ItemStack out = from.copy();
        out.setCount(1);
        out.setDamageValue(0);
        out.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        if (!out.has(DataComponents.CUSTOM_NAME)) {
            out.set(DataComponents.CUSTOM_NAME, Component.literal("§b§lАпгрейд чата"));
        }
        return out;
    }

    public static String idOf(Item item) {
        var loc = BuiltInRegistries.ITEM.getKey(item);
        return loc == null ? "minecraft:air" : loc.toString();
    }

    public static String idOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        return idOf(stack.getItem());
    }

    public static String nameOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "пусто";
        String n = stack.getHoverName().getString();
        return n.length() > 22 ? n.substring(0, 21) + "…" : n;
    }

    private static void chain(Item... items) {
        int[] chances = {68, 55, 42, 22};
        for (int i = 0; i < items.length - 1; i++) {
            int c = i < chances.length ? chances[i] : 18;
            boolean protect = items[i + 1] == Items.NETHERITE_SWORD
                    || items[i + 1] == Items.NETHERITE_PICKAXE
                    || items[i + 1] == Items.NETHERITE_AXE
                    || items[i + 1] == Items.NETHERITE_SHOVEL
                    || items[i + 1] == Items.NETHERITE_HOE
                    || items[i + 1] == Items.NETHERITE_HELMET
                    || items[i + 1] == Items.NETHERITE_CHESTPLATE
                    || items[i + 1] == Items.NETHERITE_LEGGINGS
                    || items[i + 1] == Items.NETHERITE_BOOTS
                    || items[i] == Items.NETHERITE_SWORD
                    || items[i] == Items.NETHERITE_PICKAXE
                    || items[i] == Items.NETHERITE_AXE
                    || items[i] == Items.NETHERITE_SHOVEL
                    || items[i] == Items.NETHERITE_HOE
                    || items[i] == Items.NETHERITE_HELMET
                    || items[i] == Items.NETHERITE_CHESTPLATE
                    || items[i] == Items.NETHERITE_LEGGINGS
                    || items[i] == Items.NETHERITE_BOOTS;
            MAP.put(items[i], new Step(items[i + 1], c, protect));
        }
    }

    private static void put(Item from, Item to, int chance) {
        MAP.put(from, new Step(to, chance, NEVER_DESTROY.contains(from)));
    }
}
