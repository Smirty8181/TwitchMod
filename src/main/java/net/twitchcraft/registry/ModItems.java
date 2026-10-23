package net.twitchcraft.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.item.ViewerEggItem;

import java.util.function.Function;

public final class ModItems {
    public static final Item VIEWER_EGG = register("viewer_egg",
            ViewerEggItem::new,
            new Item.Properties().stacksTo(16));

    private ModItems() {}

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties settings) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, name));
        T item = factory.apply(settings.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void register() {
        TwitchCraftMod.LOGGER.info("Registered TwitchCraft items");
    }
}
