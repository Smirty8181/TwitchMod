package net.twitchcraft.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.entity.ChatPhantomEntity;

public final class ModEntities {
    public static final ResourceLocation CHAT_PHANTOM_ID =
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "chat_phantom");
    private static final ResourceKey<EntityType<?>> CHAT_PHANTOM_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, CHAT_PHANTOM_ID);

    public static final EntityType<ChatPhantomEntity> CHAT_PHANTOM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CHAT_PHANTOM_ID,
            EntityType.Builder.of(ChatPhantomEntity::new, MobCategory.MONSTER)
                    .sized(1.2f, 0.6f)
                    .fireImmune()
                    .build(CHAT_PHANTOM_KEY));

    private ModEntities() {}

    public static void register() {
        FabricDefaultAttributeRegistry.register(CHAT_PHANTOM, ChatPhantomEntity.createAttributes());
        TwitchCraftMod.LOGGER.info("Registered TwitchCraft entities");
    }
}
