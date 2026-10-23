package net.twitchcraft.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Каталог эффектов, предметов и мобов для конструктора команд. */
public final class RegistryCatalog {
    public record Entry(String id, String label, ItemStack icon) {}

    private static List<Entry> effects;
    private static List<Entry> items;
    private static List<Entry> mobs;

    private RegistryCatalog() {}

    public static List<Entry> effects() {
        if (effects == null) effects = buildEffects();
        return effects;
    }

    public static List<Entry> items() {
        if (items == null) items = buildItems();
        return items;
    }

    public static List<Entry> mobs() {
        if (mobs == null) mobs = buildMobs();
        return mobs;
    }

    public static ItemStack iconFor(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(loc);
        if (item != null && item != Items.AIR) return new ItemStack(item);
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(loc);
        if (effect != null) return new ItemStack(Items.POTION);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(loc);
        if (type != null) return mobIcon(type);
        return ItemStack.EMPTY;
    }

    public static List<Entry> filter(List<Entry> source, String query) {
        if (query == null || query.isBlank()) return source;
        String q = query.toLowerCase(Locale.ROOT);
        List<Entry> out = new ArrayList<>();
        for (Entry e : source) {
            if (e.id().toLowerCase(Locale.ROOT).contains(q)
                    || e.label().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(e);
            }
        }
        return out;
    }

    private static List<Entry> buildEffects() {
        List<Entry> list = new ArrayList<>();
        for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
            ResourceLocation loc = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            if (loc == null) continue;
            String label = Component.translatable(effect.getDescriptionId()).getString();
            list.add(new Entry(loc.toString(), label, new ItemStack(Items.POTION)));
        }
        list.sort(Comparator.comparing(Entry::label));
        return list;
    }

    private static List<Entry> buildItems() {
        List<Entry> list = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ResourceLocation loc = BuiltInRegistries.ITEM.getKey(item);
            if (loc == null) continue;
            String label = Component.translatable(item.getDescriptionId()).getString();
            list.add(new Entry(loc.toString(), label, new ItemStack(item)));
        }
        list.sort(Comparator.comparing(Entry::label));
        return list;
    }

    private static List<Entry> buildMobs() {
        List<Entry> list = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type.getCategory() == MobCategory.MISC && type != EntityType.LIGHTNING_BOLT) continue;
            ResourceLocation loc = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (loc == null) continue;
            String label = Component.translatable(type.getDescriptionId()).getString();
            list.add(new Entry(loc.toString(), label, mobIcon(type)));
        }
        list.sort(Comparator.comparing(Entry::label));
        return list;
    }

    private static ItemStack mobIcon(EntityType<?> type) {
        Item egg = SpawnEggItem.byId(type);
        if (egg != null && egg != Items.AIR) return new ItemStack(egg);
        return new ItemStack(Items.SPAWNER);
    }
}
