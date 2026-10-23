package net.twitchcraft.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.twitchcraft.content.ViewerEggService;

/** Яйцо зрителя — спавн ghost-моба с именем на стикере. */
public class ViewerEggItem extends Item {
    public ViewerEggItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        String viewer = ViewerEggService.viewerFromStack(stack);
        if (viewer.isBlank()) viewer = "Viewer";

        if (player instanceof ServerPlayer sp) {
            ViewerEggService.spawnFromEgg((ServerLevel) level, sp, viewer);
            level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON,
                    SoundSource.PLAYERS, 1f, 1.1f);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.CONSUME;
    }

    public static ItemStack create(String viewerName) {
        ItemStack stack = new ItemStack(net.twitchcraft.registry.ModItems.VIEWER_EGG);
        String name = viewerName != null ? viewerName.trim() : "Viewer";
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§d§l[" + name + "] §5Яйцо зрителя"));
        stack.set(DataComponents.LORE, ViewerEggService.lore(name));
        return stack;
    }
}
