package net.twitchcraft.feedback;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;
import net.twitchcraft.twitch.TwitchConnectionManager;

public final class AnnouncementService {
    private AnnouncementService() {}

    public static void announceCommand(ServerPlayer target, String twitchUser, ChatGameCommand command, int cooldownSec) {
        TwitchCraftConfig.FeedbackSettings fb = ConfigManager.get().feedback;

        String ruName = command.getRussianName();
        if (fb.announceInTwitchChat) {
            String msg = fb.chatMessageFormat
                    .replace("{user}", twitchUser)
                    .replace("{command}", ruName.toUpperCase())
                    .replace("{cooldown}", String.valueOf(cooldownSec));
            TwitchCraftMod.getConnectionManager().sendChatMessage(msg);
        }

        if (fb.showActionBar) {
            boolean good = net.twitchcraft.commands.GoodCommands.isGood(command);
            target.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.literal((good ? "§a♥ " : "§6[Twitch] §f") + twitchUser
                            + " §7→ " + (good ? "§a!" : "§e!") + ruName
                            + " §8· §7КД " + cooldownSec + "с")));
        }
        if (fb.showTitleInGame && net.twitchcraft.commands.GoodCommands.isGood(command)) {
            showTitle(target,
                    Component.literal("§a§l♥ " + ruName.toUpperCase()),
                    Component.literal("§f" + twitchUser + " §7помог"),
                    3, 22, 8);
        }
    }

    public static void announceDonation(ServerPlayer target, String donor, String amount, String currency,
                                        String commandLabel, ChatGameCommand command, String donorMessage,
                                        int tierMinAmount) {
        TwitchCraftConfig.FeedbackSettings fb = ConfigManager.get().feedback;

        if (fb.announceInTwitchChat) {
            String msg = fb.donationMessageFormat
                    .replace("{user}", donor)
                    .replace("{amount}", amount)
                    .replace("{currency}", currency)
                    .replace("{command}", commandLabel);
            TwitchCraftMod.getConnectionManager().sendChatMessage(msg);
        }
        // HUD — только компактный DonationOverlay на клиенте (без Title/ActionBar)
    }

    public static void announceDonation(ServerPlayer target, String donor, String amount, String currency,
                                        String commandLabel, ChatGameCommand command, String donorMessage) {
        announceDonation(target, donor, amount, currency, commandLabel, command, donorMessage, 50);
    }

    public static void announceDonationWheel(ServerPlayer target, String donor, String amount, String currency,
                                             String winnerLabel, ChatGameCommand command, String donorMessage,
                                             int tierMinAmount) {
        announceDonationWheelResult(target, donor, amount, currency, winnerLabel, command, donorMessage, tierMinAmount);
    }

    /** Старт спина — без названия приза. */
    public static void announceDonationWheelSpin(ServerPlayer target, String donor, String amount, String currency,
                                                 int tierMinAmount) {
        TwitchCraftConfig.FeedbackSettings fb = ConfigManager.get().feedback;
        if (fb.announceInTwitchChat) {
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    "🎰 " + donor + " " + amount + currency + " крутит рулетку…");
        }
        if (fb.showActionBar) {
            target.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.literal("§6§l🎰 РУЛЕТКА §7— §f" + donor + " §8· §6" + amount + currency)));
        }
    }

    /** Результат после остановки — совпадает с тем, что на экране. */
    public static void announceDonationWheelResult(ServerPlayer target, String donor, String amount, String currency,
                                                   String winnerLabel, ChatGameCommand command, String donorMessage,
                                                   int tierMinAmount) {
        TwitchCraftConfig.FeedbackSettings fb = ConfigManager.get().feedback;
        if (fb.announceInTwitchChat) {
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    "🎰 " + donor + " " + amount + currency + " → " + winnerLabel);
        }
        if (fb.showTitleInGame) {
            showTitle(target,
                    Component.literal("§6§l🎰 ВЫПАЛО"),
                    Component.literal("§f" + winnerLabel),
                    5, 45, 12);
        }
        if (fb.showActionBar) {
            target.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.literal("§6§lВЫПАЛО §7— §f" + winnerLabel)));
        }
    }

    public static void announceComboStorm(ServerPlayer target, String twitchUser) {
        if (ConfigManager.get().feedback.announceInTwitchChat) {
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    "⚡ КОМБО! " + twitchUser + " ×3 !молния → ГРОЗА!");
        }
        if (ConfigManager.get().feedback.showTitleInGame) {
            showTitle(target,
                    Component.literal("§b§l⛈ ГРОЗА!"),
                    Component.literal("§7" + twitchUser + " §8→ §e5 молний"),
                    5, 60, 15);
        }
        if (ConfigManager.get().feedback.showActionBar) {
            target.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.literal("§b§l⛈ ГРОЗА §7— §e5 молний §8(комбо ×3 !молния)")));
        }
    }

    public static void announceCustom(ServerPlayer target, String message) {
        if (ConfigManager.get().feedback.showActionBar) {
            target.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("§6[Twitch] §f" + message)));
        }
        TwitchConnectionManager mgr = TwitchCraftMod.getConnectionManager();
        if (ConfigManager.get().feedback.announceInTwitchChat) {
            mgr.sendChatMessage(stripColors(message));
        }
    }

    public static void announceHud(ServerPlayer target, String message) {
        if (target == null) return;
        if (ConfigManager.get().feedback.showActionBar) {
            target.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(message)));
        }
    }

    public static void flashTitle(ServerPlayer target, String title, String subtitle) {
        if (target == null || !ConfigManager.get().feedback.showTitleInGame) return;
        showTitle(target, Component.literal(title), Component.literal(subtitle), 4, 36, 10);
    }

    public static void announceChaos(ServerPlayer target, ChatGameCommand command) {
        announceCustom(target, "ХАОС МАКС! → !" + command.getRussianName());
        if (ConfigManager.get().feedback.showTitleInGame) {
            showTitle(target, Component.literal("§4§lХАОС ЧАТА!"), Component.literal("§c!" + command.getRussianName()));
        }
    }

    public static void announceVoteWinner(ServerPlayer target, ChatGameCommand winner, int votes) {
        String msg = "Голосование: победил !" + winner.getRussianName() + " (" + votes + " гол.)";
        announceCustom(target, msg);
        if (ConfigManager.get().feedback.showTitleInGame) {
            showTitle(target, Component.literal("§d§lГОЛОСОВАНИЕ"), Component.literal("§e!" + winner.getRussianName()));
        }
    }

    public static void announceRevenge(ServerPlayer target) {
        announceCustom(target, "ЧАТ ПОБЕДИЛ! Стример погиб — реванш!");
        if (ConfigManager.get().feedback.showTitleInGame) {
            showTitle(target, Component.literal("§4§lЧАТ ПОБЕДИЛ!"), Component.literal("§cРеванш!"));
        }
    }

    /** Кинематографичный title для редких команд (визер / дракон / фантом). */
    public static void announceRareCinematic(ServerPlayer target, String twitchUser, ChatGameCommand command) {
        if (command == null || target == null) return;
        TwitchCraftConfig.FeedbackSettings fb = ConfigManager.get().feedback;
        String user = twitchUser != null ? twitchUser : "чат";
        String title;
        String subtitle;
        switch (command) {
            case WITHER -> {
                title = "§0§l☠ ВИЗЕР";
                subtitle = "§7" + user + " §8призвал §cсмерть";
            }
            case DRAGON -> {
                title = "§5§l🐉 ДРАКОН КРАЯ";
                subtitle = "§d" + user + " §8открыл §5портал хаоса";
            }
            case CHAT_PHANTOM -> {
                title = "§5§l👻 ПРИЗРАК ЧАТА";
                subtitle = "§d" + user + " §8натравил фантома чата";
            }
            default -> {
                return;
            }
        }
        if (fb.announceInTwitchChat) {
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    "⚠ РЕДКОЕ: " + user + " → !" + command.getRussianName());
        }
        if (fb.showTitleInGame) {
            showTitle(target, Component.literal(title), Component.literal(subtitle), 8, 70, 20);
        }
        if (fb.showActionBar) {
            target.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.literal("§5§l★ РЕДКОЕ §7— §f" + user + " §8→ §e!" + command.getRussianName())));
        }
    }

    public static boolean isRareCinematic(ChatGameCommand command) {
        return command == ChatGameCommand.WITHER
                || command == ChatGameCommand.DRAGON
                || command == ChatGameCommand.CHAT_PHANTOM;
    }

    private static void showTitle(ServerPlayer player, Component title, Component subtitle) {
        showTitle(player, title, subtitle, 5, 50, 10);
    }

    private static void showTitle(ServerPlayer player, Component title, Component subtitle,
                                  int fadeIn, int stay, int fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }

    private static String stripColors(String s) {
        return s.replaceAll("§.", "");
    }
}
