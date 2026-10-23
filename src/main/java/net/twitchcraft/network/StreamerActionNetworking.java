package net.twitchcraft.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.config.ConfigApply;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.PresetIO;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.stream.DeathTracker;
import net.twitchcraft.stream.StreamerChallenge;
import net.twitchcraft.stream.StreamerBargain;
import net.twitchcraft.stream.StreamerCharge;
import net.twitchcraft.stream.StreamerContract;
import net.twitchcraft.stream.ChaosWheel;
import net.twitchcraft.stream.StreamerDice;
import net.twitchcraft.stream.StreamerTaunt;
import net.twitchcraft.stream.StreamerShield;
import net.twitchcraft.stream.StreamStateStore;
import net.twitchcraft.stream.StreamTargets;
import net.twitchcraft.stream.HouseMode;
import net.twitchcraft.TwitchCraftMod;

public final class StreamerActionNetworking {
    private static long nextMercyAt;

    private StreamerActionNetworking() {}

    public static void registerServer() {
        NetworkRegistration.registerPlayC2S();
        ServerPlayNetworking.registerGlobalReceiver(StreamerActionPayload.TYPE, (payload, ctx) -> {
            var server = ctx.player().level().getServer();
            if (server == null) return;
            server.execute(() -> handle(ctx.player(), payload.action()));
        });
    }

    public static void handle(ServerPlayer player, String action) {
        if (player == null || action == null || action.length() > 80) return;
        if (!ConfigApply.canEdit(player) && !StreamTargets.isTarget(player)) {
            player.sendSystemMessage(Component.literal("§cТолько стример может это делать"));
            return;
        }
        var server = player.level().getServer();
        if (server == null) return;
        String raw = action.trim();
        String key = raw.toLowerCase();
        if (key.startsWith("testdonate:")) {
            testDonate(player, raw.substring("testdonate:".length()));
            return;
        }
        if (key.startsWith("testcmd:")) {
            testCommand(player, raw.substring("testcmd:".length()));
            return;
        }
        switch (key) {
            case "dice" -> StreamerDice.get().refuseManual(player);
            case "testdice" -> {
                boolean ok = StreamerDice.get().rollFromWorld(server, player);
                if (!ok) player.sendSystemMessage(Component.literal("§7Кубик уже крутится"));
            }
            case "testdrop" -> {
                boolean ok = net.twitchcraft.stream.WorldEncounter.forceSpawn(server, player);
                if (!ok) player.sendSystemMessage(Component.literal("§7Находка не влезла — мало места или не Оверворлд"));
            }
            case "testchaos" -> {
                if (!ChaosWheel.start(server, player)) {
                    player.sendSystemMessage(Component.literal("§7Колесо уже крутится"));
                }
            }
            case "testhaus" -> HouseMode.toggle(server);
            case "testtether" -> {
                if (!net.twitchcraft.stream.DuoTether.start(server, player, "тест")) {
                    player.sendSystemMessage(Component.literal("§7Связь не стартовала — нужны двое"));
                }
            }
            case "challenge" -> StreamerChallenge.get().startFromStreamer(server, player);
            case "mercy" -> mercy(server, player);
            case "contract" -> StreamerContract.get().start(server, player);
            case "sacrifice" -> StreamerCharge.sacrifice(server, player);
            case "taunt" -> StreamerTaunt.get().startFromStreamer(server, player);
            case "bargain" -> StreamerBargain.tryBuyout(server, player);
            case "saidname" -> net.twitchcraft.stream.UnnamedPatron.confess(server, player);
            case "ping" -> {
                if (!StreamTargets.isTarget(player)) {
                    player.sendSystemMessage(Component.literal("§cПинг только для стримеров"));
                    break;
                }
                net.twitchcraft.stream.PartnerPing.ping(player);
            }
            case "copypartner" -> {
                String json = PresetIO.toJson(ConfigManager.get());
                if (json == null || json.isBlank()) {
                    player.sendSystemMessage(Component.literal("§cПресет мира пустой"));
                    break;
                }
                PartnerPresetPayload.send(player, json);
                player.sendSystemMessage(Component.literal(
                        "§dПресет мира §7(без токенов и ников) — нажми §aСохранить §7в K"));
            }
            case "resetstats" -> {
                StreamStateStore.resetAndSave(server);
                player.sendSystemMessage(Component.literal("§aСтатистика этого мира сброшена"));
            }
            default -> {
                if (action != null && action.startsWith("upgrade:slot:")) {
                    try {
                        int slot = Integer.parseInt(action.substring("upgrade:slot:".length()));
                        net.twitchcraft.stream.ItemUpgrade.pickSlot(player, slot);
                    } catch (NumberFormatException ignored) {
                    }
                } else if ("upgrade:go".equals(action)) {
                    net.twitchcraft.stream.ItemUpgrade.confirm(player);
                } else {
                    player.sendSystemMessage(Component.literal("§cНеизвестное действие"));
                }
            }
        }
    }

    /** Милость помогает чату, не стримеру: снимает щит, но тыква + КД. */
    private static void mercy(net.minecraft.server.MinecraftServer server, ServerPlayer player) {
        boolean blocked = DeathTracker.isEvilBlocked(player.getGameProfile().name())
                || StreamerShield.isActive();
        if (!blocked) {
            AnnouncementService.announceCustom(player, "§7Снимать нечего — блока урона нет");
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextMercyAt) {
            int left = (int) ((nextMercyAt - now + 999) / 1000);
            AnnouncementService.announceCustom(player, "§7Милость через §f" + left + "с");
            return;
        }
        nextMercyAt = now + 90_000L;
        CommandContext ctx = new CommandContext(server, player, "милость", "!clown",
                ChatGameCommand.CLOWN, true);
        EffectRunner.clown(ctx);
        DeathTracker.clearEvilBlock(player.getGameProfile().name());
        StreamerShield.clear(server, player, "§aМилость: урон снова можно §8(тыква — цена)");
        AnnouncementService.announceCustom(player, "§eМилость: чат снова бьёт, ты в тыкве");
    }

    private static void testDonate(ServerPlayer player, String amountRaw) {
        int amount;
        try {
            amount = Integer.parseInt(amountRaw.trim());
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§cСумма не число"));
            return;
        }
        if (amount < 1 || amount > 1_000_000) {
            player.sendSystemMessage(Component.literal("§cСумма вне диапазона"));
            return;
        }
        var server = player.level().getServer();
        if (server == null) return;
        String target = player.getGameProfile().name();
        boolean ok = TwitchCraftMod.getDonationPoller().testDonation(
                server, target, "Тест", amount, "RUB", "тест доната");
        if (!ok) {
            player.sendSystemMessage(Component.literal("§cНет тира на §f" + amount + "₽"));
        }
    }

    private static void testCommand(ServerPlayer player, String commandId) {
        if (commandId == null || commandId.isBlank()) return;
        var server = player.level().getServer();
        if (server == null) return;
        boolean ok = TwitchCraftMod.getConnectionManager().testCommand(server, commandId.trim());
        if (!ok) {
            player.sendSystemMessage(Component.literal("§cНеизвестная команда: §f" + commandId));
        }
    }
}
