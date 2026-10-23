package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.feedback.AnnouncementService;

/**
 * Выкуп: только под бомбой или охотой. Платишь ценный предмет.
 * Бомба гаснет, но тыква. Охота срывается, без награды + слабость.
 */
public final class StreamerBargain {
    private StreamerBargain() {}

    public static boolean tryBuyout(MinecraftServer server, ServerPlayer player) {
        if (player == null) return false;
        boolean bomb = BombManager.get().isActive();
        boolean hunt = HuntManager.get().isActive();
        if (!bomb && !hunt) {
            AnnouncementService.announceCustom(player,
                    "§7Выкуп только во время §cбомбы §7или §cохоты");
            return false;
        }
        if (!StreamerCharge.consumeValuable(player)) return false;

        if (bomb) {
            BombManager.get().defuse(server, "выкуп");
            CommandContext ctx = new CommandContext(server, player, "выкуп", "!clown",
                    ChatGameCommand.CLOWN, true);
            EffectRunner.clown(ctx);
            AnnouncementService.announceCustom(player, "§eВыкуп: бомба снята, ты в тыкве");
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    "Стример выкупил бомбу предметом — но получил тыкву.");
            return true;
        }

        HuntManager.get().buyout(server, player);
        return true;
    }
}
