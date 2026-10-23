package net.twitchcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.AudiencePresets;
import net.twitchcraft.config.ConfigApply;
import net.twitchcraft.config.ConfigManager;
import java.util.List;
import net.twitchcraft.donation.DonationAlertsPoller;
import net.twitchcraft.donation.DonationState;
import net.twitchcraft.network.RulesNetworking;
import net.twitchcraft.stream.HouseMode;
import net.twitchcraft.stream.StreamStateStore;
import net.twitchcraft.stream.StreamTargets;
import net.twitchcraft.twitch.TwitchConnectionManager;

public final class TwitchCraftCommands {
    private TwitchCraftCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                net.minecraft.commands.CommandBuildContext registryAccess,
                                Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("twitchcraft")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("connect")
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            TwitchCraftMod.getConnectionManager().connect(server);
                            ctx.getSource().sendSuccess(() -> Component.literal("TwitchCraft: connecting..."), true);
                            return 1;
                        }))
                .then(Commands.literal("disconnect")
                        .executes(ctx -> {
                            TwitchCraftMod.getConnectionManager().disconnect();
                            ctx.getSource().sendSuccess(() -> Component.literal("TwitchCraft: disconnected"), true);
                            return 1;
                        }))
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            ConfigManager.load();
                            ConfigApply.restartServices(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal("TwitchCraft: config reloaded"), true);
                            return 1;
                        }))
                .then(Commands.literal("status")
                        .executes(ctx -> {
                            TwitchConnectionManager mgr = TwitchCraftMod.getConnectionManager();
                            String status = mgr.isConnected()
                                    ? "Connected to #" + mgr.getChannel()
                                    : "Disconnected" + (mgr.getLastError().isEmpty() ? "" : " (" + mgr.getLastError() + ")");
                            ctx.getSource().sendSuccess(() -> Component.literal("TwitchCraft: " + status), false);
                            return 1;
                        }))
                .then(Commands.literal("test")
                        .then(Commands.argument("command", StringArgumentType.word())
                                .executes(ctx -> {
                                    String cmd = StringArgumentType.getString(ctx, "command");
                                    boolean ok = TwitchCraftMod.getConnectionManager()
                                            .testCommand(ctx.getSource().getServer(), cmd);
                                    if (!ok) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "§cTwitchCraft: неизвестная или недоступная команда: §f" + cmd));
                                        return 0;
                                    }
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("§aTwitchCraft: §f!" + cmd + " §aвыполнена"), true);
                                    return 1;
                                })))
                .then(Commands.literal("testsub")
                        .executes(ctx -> runTestSub(ctx, "Сабчик"))
                        .then(Commands.argument("nick", StringArgumentType.word())
                                .executes(ctx -> runTestSub(ctx, StringArgumentType.getString(ctx, "nick")))))
                .then(Commands.literal("testraid")
                        .executes(ctx -> runTestRaid(ctx, 5, "Raider"))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 8))
                                .executes(ctx -> runTestRaid(ctx, IntegerArgumentType.getInteger(ctx, "count"), "Raider"))
                                .then(Commands.argument("raider", StringArgumentType.word())
                                        .executes(ctx -> runTestRaid(ctx,
                                                IntegerArgumentType.getInteger(ctx, "count"),
                                                StringArgumentType.getString(ctx, "raider"))))))
                .then(Commands.literal("первое")
                        .executes(ctx -> runTestNewbie(ctx, "зритель"))
                        .then(Commands.argument("nick", StringArgumentType.word())
                                .executes(ctx -> runTestNewbie(ctx, StringArgumentType.getString(ctx, "nick")))))
                .then(Commands.literal("фоллоу")
                        .executes(ctx -> runTestFollow(ctx, "зритель"))
                        .then(Commands.argument("nick", StringArgumentType.word())
                                .executes(ctx -> runTestFollow(ctx, StringArgumentType.getString(ctx, "nick")))))
                .then(Commands.literal("testnewbie")
                        .executes(ctx -> runTestNewbie(ctx, "зритель"))
                        .then(Commands.argument("nick", StringArgumentType.word())
                                .executes(ctx -> runTestNewbie(ctx, StringArgumentType.getString(ctx, "nick")))))
                .then(Commands.literal("testfollow")
                        .executes(ctx -> runTestFollow(ctx, "зритель"))
                        .then(Commands.argument("nick", StringArgumentType.word())
                                .executes(ctx -> runTestFollow(ctx, StringArgumentType.getString(ctx, "nick")))))
                .then(Commands.literal("держись")
                        .executes(TwitchCraftCommands::runPartnerHold))
                .then(Commands.literal("промах")
                        .executes(TwitchCraftCommands::runMiss))
                .then(Commands.literal("обменинв")
                        .executes(TwitchCraftCommands::runSoloSwap))
                .then(Commands.literal("testdonate")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                .executes(ctx -> runTestDonate(ctx, IntegerArgumentType.getInteger(ctx, "amount"),
                                        "TestDonate", null))
                                .then(Commands.argument("donor", StringArgumentType.word())
                                        .executes(ctx -> runTestDonate(ctx, IntegerArgumentType.getInteger(ctx, "amount"),
                                                StringArgumentType.getString(ctx, "donor"), null))
                                        .then(Commands.argument("target", StringArgumentType.word())
                                                .executes(ctx -> runTestDonate(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "amount"),
                                                        StringArgumentType.getString(ctx, "donor"),
                                                        StringArgumentType.getString(ctx, "target")))))))
                .then(Commands.literal("donations")
                        .executes(ctx -> {
                            var ds = ConfigManager.get().donations;
                            int profiles = TwitchCraftMod.getDonationPoller().profileCount();
                            String status;
                            if (!ds.enabled) {
                                status = "§7выключено";
                            } else if (DonationState.isConnected()) {
                                status = "§aON §7(" + profiles + " акк.)";
                            } else if (profiles > 0) {
                                String err = DonationState.lastError();
                                status = "§cOFF §7— токен не принят"
                                        + (err.isBlank() ? "" : " §8(" + err + ")");
                            } else {
                                status = "§cOFF §7— нет токена";
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("DonateX: " + status), false);
                            int pending = TwitchCraftMod.getDonationPoller().pendingCount();
                            if (pending > 0) {
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        "§eОчередь: §f" + pending + " §7донат(ов) ждут онлайн"), false);
                            }
                            if (DonationState.isConnected()) {
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        "§7Последний: §e" + DonationState.lastDonor()
                                                + " §6" + DonationState.lastAmount()
                                                + " §7→ §c" + DonationState.lastLabel()), false);
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§7Тест: §f/twitchcraft testdonate <сумма> [донатер] [стример]"), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§7Tiers: §8" + DonationAlertsPoller.tiersHint()), false);
                            return 1;
                        })
                        .then(Commands.literal("test")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                        .executes(ctx -> runTestDonate(ctx, IntegerArgumentType.getInteger(ctx, "amount"),
                                                "TestDonate", null)))))
                .then(Commands.literal("preset")
                        .then(Commands.argument("size", StringArgumentType.word())
                                .executes(ctx -> {
                                    String size = StringArgumentType.getString(ctx, "size").toLowerCase();
                                    if (!List.of(AudiencePresets.SMALL, AudiencePresets.MEDIUM,
                                            AudiencePresets.LARGE, AudiencePresets.MEGA, AudiencePresets.DUO).contains(size)) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "§cПресеты: small, medium, large, mega, duo"));
                                        return 0;
                                    }
                                    var cfg = ConfigManager.get();
                                    AudiencePresets.apply(cfg, size);
                                    ConfigManager.set(cfg);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§aПресет: §f" + AudiencePresets.label(size)), true);
                                    return 1;
                                })))
                .then(Commands.literal("dice")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendFailure(Component.literal("Только из игры"));
                                return 0;
                            }
                            boolean ok = net.twitchcraft.stream.StreamerDice.get()
                                    .refuseManual(player);
                            return ok ? 1 : 0;
                        }))
                .then(Commands.literal("testdice")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendFailure(Component.literal("Только из игры"));
                                return 0;
                            }
                            boolean ok = net.twitchcraft.stream.StreamerDice.get()
                                    .rollFromWorld(ctx.getSource().getServer(), player);
                            if (!ok) {
                                ctx.getSource().sendFailure(Component.literal("§7Кубик уже крутится"));
                                return 0;
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("§aКубик мира — тест"), true);
                            return 1;
                        }))
                .then(Commands.literal("testdrop")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendFailure(Component.literal("Только из игры"));
                                return 0;
                            }
                            boolean ok = net.twitchcraft.stream.WorldEncounter.forceSpawn(
                                    ctx.getSource().getServer(), player);
                            if (!ok) {
                                ctx.getSource().sendFailure(Component.literal(
                                        "§7Находка не влезла — мало места или не Оверворлд"));
                                return 0;
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("§aНаходка мира — тест"), true);
                            return 1;
                        }))
                .then(Commands.literal("testchaos")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            if (player == null) {
                                ctx.getSource().sendFailure(Component.literal("Только из игры"));
                                return 0;
                            }
                            boolean ok = net.twitchcraft.stream.ChaosWheel.start(
                                    ctx.getSource().getServer(), player);
                            if (!ok) {
                                ctx.getSource().sendFailure(Component.literal("§7Колесо уже крутится"));
                                return 0;
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("§cКолесо хаоса — тест"), true);
                            return 1;
                        }))
                .then(Commands.literal("mercy")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            if (player == null) return 0;
                            net.twitchcraft.network.StreamerActionNetworking.handle(player, "mercy");
                            return 1;
                        }))
                .then(Commands.literal("challenge")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            if (player == null) return 0;
                            boolean ok = net.twitchcraft.stream.StreamerChallenge.get()
                                    .startFromStreamer(ctx.getSource().getServer(), player);
                            return ok ? 1 : 0;
                        }))
                .then(Commands.literal("contract")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            if (player == null) return 0;
                            boolean ok = net.twitchcraft.stream.StreamerContract.get()
                                    .start(ctx.getSource().getServer(), player);
                            return ok ? 1 : 0;
                        }))
                .then(Commands.literal("sacrifice")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            if (player == null) return 0;
                            boolean ok = net.twitchcraft.stream.StreamerCharge.sacrifice(
                                    ctx.getSource().getServer(), player);
                            return ok ? 1 : 0;
                        }))
                .then(Commands.literal("taunt")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            if (player == null) return 0;
                            boolean ok = net.twitchcraft.stream.StreamerTaunt.get()
                                    .startFromStreamer(ctx.getSource().getServer(), player);
                            return ok ? 1 : 0;
                        }))
                .then(Commands.literal("bargain")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            if (player == null) return 0;
                            boolean ok = net.twitchcraft.stream.StreamerBargain.tryBuyout(
                                    ctx.getSource().getServer(), player);
                            return ok ? 1 : 0;
                        }))
                .then(Commands.literal("resetstats")
                        .executes(ctx -> {
                            StreamStateStore.resetAndSave(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§aTwitchCraft: статистика мира сброшена"), true);
                            return 1;
                        }))
                .then(Commands.literal("logs")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§dTwitchCraft: §fлоги стрима → "
                                            + net.twitchcraft.stream.StreamChronicle.pathHint()), false);
                            return 1;
                        }))
                .then(Commands.literal("haus")
                        .executes(ctx -> {
                            HouseMode.toggle(ctx.getSource().getServer());
                            boolean on = ConfigManager.get().stream.houseMode;
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    on ? "§dХаусмод §aВКЛ §7— все КД отключены" : "§7Хаусмод выключен"), true);
                            return 1;
                        })));

        dispatcher.register(Commands.literal("rules")
                .executes(ctx -> {
                    var player = ctx.getSource().getPlayer();
                    if (player == null) {
                        ctx.getSource().sendFailure(Component.literal("Только для игроков"));
                        return 0;
                    }
                    RulesNetworking.openFor(player);
                    return 1;
                }));
    }

    private static int runTestDonate(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                                     int amount, String donor, String targetPlayer) {
        var source = ctx.getSource();
        var server = source.getServer();

        String target = targetPlayer;
        if (target == null || target.isBlank()) {
            ServerPlayer player = source.getPlayer();
            if (player != null && StreamTargets.isTarget(player)) {
                target = player.getGameProfile().name();
            } else {
                target = StreamTargets.resolve(server)
                        .map(p -> p.getGameProfile().name())
                        .orElse("");
            }
        }

        if (target.isBlank()) {
            source.sendFailure(Component.literal("§cНет стримера онлайн. Укажи: /twitchcraft testdonate " + amount + " " + donor + " <ник>"));
            return 0;
        }

        String currency = ConfigManager.get().donations.tiers.stream()
                .map(t -> t.currency)
                .filter(c -> c != null && !c.isBlank())
                .findFirst()
                .orElse("RUB");

        boolean ok = TwitchCraftMod.getDonationPoller().testDonation(
                server, target, donor, amount, currency, "тест доната");
        if (!ok) {
            source.sendFailure(Component.literal("§cСумма " + amount + currency + " не попала ни в один tier"));
            source.sendFailure(Component.literal("§7Сумма округляется вниз: 599₽ = tier 500₽, 1000₽ = phantom"));
            source.sendFailure(Component.literal("§7Tiers: " + DonationAlertsPoller.tiersHint()));
            return 0;
        }

        var resolved = net.twitchcraft.donation.DonationTierResolver
                .resolve(amount, currency, ConfigManager.get().donations.tiers).orElse(null);
        String tierInfo = resolved != null
                ? " §7→ §f" + resolved.minAmount + "₽ " + (resolved.label != null ? resolved.label : resolved.command)
                : "";
        String targetFinal = target;
        source.sendSuccess(() -> Component.literal("§aТест доната: §e" + donor + " §6" + amount + currency
                + tierInfo + " §7→ §f" + targetFinal), false);
        return 1;
    }

    private static int runTestSub(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String nick) {
        var source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Только из игры"));
            return 0;
        }
        net.twitchcraft.stream.TwitchHypeEffects.onSub(source.getServer(), player.getGameProfile().name(), nick, false, "");
        source.sendSuccess(() -> Component.literal("§dТест саба: §f" + nick + " §7→ свинья"), true);
        return 1;
    }

    private static int runTestRaid(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                                   int count, String raider) {
        var source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Только из игры"));
            return 0;
        }
        net.twitchcraft.stream.TwitchHypeEffects.onRaid(source.getServer(), player.getGameProfile().name(), raider, count);
        source.sendSuccess(() -> Component.literal("§cТест рейда: §f" + raider + " §c×" + count), true);
        return 1;
    }

    private static int runTestNewbie(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String nick) {
        var source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Только из игры"));
            return 0;
        }
        net.twitchcraft.stream.TwitchHypeEffects.onNewbie(source.getServer(), player.getGameProfile().name(), nick);
        source.sendSuccess(() -> Component.literal("§eТест: §f" + nick + " §7впервые на канале"), true);
        return 1;
    }

    private static int runPartnerHold(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Только из игры"));
            return 0;
        }
        ServerPlayer partner = StreamTargets.partner(source.getServer(), player).orElse(player);
        String fallen = partner == player ? "тест" : player.getGameProfile().name();
        net.twitchcraft.stream.PartnerHold.activate(source.getServer(), partner, fallen);
        source.sendSuccess(() -> Component.literal(
                "§eДЕРЖИСЬ §7→ §f" + partner.getGameProfile().name() + " §75с"), true);
        return 1;
    }

    private static int runSoloSwap(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Только из игры"));
            return 0;
        }
        net.twitchcraft.effects.EffectRunner.swapInventoriesSoloTest(source.getServer(), player);
        source.sendSuccess(() -> Component.literal("§dСоло-тест обмена хотбара"), true);
        return 1;
    }

    private static int runMiss(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Только из игры"));
            return 0;
        }
        net.twitchcraft.stream.CommandMiss.arm();
        ServerPlayer partner = StreamTargets.partner(source.getServer(), player).orElse(null);
        String hit = partner != null ? partner.getGameProfile().name() : player.getGameProfile().name();
        net.twitchcraft.network.CommandAlertPayload.sendToStreamers(
                source.getServer(), "чат", "ПРОМАХ → " + hit, "miss");
        source.sendSuccess(() -> Component.literal(
                "§eПРОМАХ §7вооружён — следующая злая команда летит в напарника"), true);
        return 1;
    }

    private static int runTestFollow(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String nick) {
        var source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Только из игры"));
            return 0;
        }
        net.twitchcraft.stream.TwitchHypeEffects.onFollow(source.getServer(), player.getGameProfile().name(), nick);
        source.sendSuccess(() -> Component.literal("§dТест: §f" + nick + " §7зафоловил"), true);
        return 1;
    }
}
