package net.twitchcraft.twitch;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.CategoryCooldownResolver;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.commands.CustomCommandRegistry;
import net.twitchcraft.commands.EvilCommands;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.CustomCommandDefinition;
import net.twitchcraft.config.TwitchCraftConfig;
import net.twitchcraft.effects.CustomCommandExecutor;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.feedback.CommandDeny;
import net.twitchcraft.security.HardcoreGuard;
import net.twitchcraft.security.RateLimiter;
import net.twitchcraft.stream.CourtManager;
import net.twitchcraft.stream.HouseMode;
import net.twitchcraft.stream.StreamStats;
import net.twitchcraft.stream.StreamTargets;
import net.twitchcraft.stream.StreamTickHandler;
import net.twitchcraft.stream.VoteManager;
import net.twitchcraft.stream.ViewerKarma;
import net.twitchcraft.network.CommandAlertPayload;
import net.twitchcraft.stream.ChaosMeter;
import net.twitchcraft.stream.CommandChainTracker;
import net.twitchcraft.stream.DeathTracker;
import net.twitchcraft.stream.LightningComboTracker;
import net.twitchcraft.stream.ChatActivity;
import net.twitchcraft.stream.CipherRiddle;
import net.twitchcraft.stream.UnnamedPatron;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ChatCommandProcessor {
    private final RateLimiter rateLimiter = new RateLimiter();
    private final Map<String, Long> lastUserCommand = new HashMap<>();

    public void handle(MinecraftServer server, String twitchUser, String message) {
        handle(server, twitchUser, message, null);
    }

    public void handle(MinecraftServer server, String twitchUser, String message, String targetPlayerName) {
        if (message == null || !message.startsWith("!")) return;
        if (EffectRunner.isPaused()) {
            deny(server, targetPlayerName, twitchUser, "чат", CommandDeny.PAUSE, EffectRunner.pauseSecondsLeft());
            return;
        }

        String body = message.substring(1).trim().toLowerCase(Locale.ROOT);

        if (VoteManager.get().isActive() && body.matches("[123]")) {
            if (VoteManager.get().castVote(twitchUser, Integer.parseInt(body))) {
                TwitchCraftMod.LOGGER.debug("Vote cast by {} for {}", twitchUser, body);
            }
            return;
        }
        if (CourtManager.isCourtVote(body)) {
            if (CourtManager.get().castVote(twitchUser, CourtManager.parseGuilty(body))) {
                TwitchCraftMod.LOGGER.debug("Court vote by {}: {}", twitchUser, body);
            }
            return;
        }

        String first = body.contains(" ") ? body.substring(0, body.indexOf(' ')) : body;
        if (net.twitchcraft.stream.BetManager.isBetAlias(first)
                && net.twitchcraft.stream.BetManager.get().isActive()) {
            net.twitchcraft.stream.BetManager.get().handle(server, twitchUser, message);
            return;
        }
        if (net.twitchcraft.stream.BombManager.get().isActive()
                && net.twitchcraft.stream.BombManager.isDefuse(first)) {
            net.twitchcraft.stream.BombManager.get().defuse(server, twitchUser);
            return;
        }
        if (first.equals("кубик") || first.equals("dice") || first.equals("ролл")) {
            net.twitchcraft.stream.StreamTargets.resolve(server).ifPresent(p ->
                    net.twitchcraft.stream.StreamerDice.get().refuseManual(p));
            return;
        }
        if (first.equals("тесткубик") || first.equals("testdice")) {
            boolean mod = ConfigManager.get().security.moderators.stream()
                    .anyMatch(m -> m.equalsIgnoreCase(twitchUser));
            if (mod) {
                net.twitchcraft.stream.StreamTargets.resolve(server).ifPresent(p ->
                        net.twitchcraft.stream.StreamerDice.get().rollFromWorld(server, p));
            }
            return;
        }
        if (first.equals("тестхаос") || first.equals("testchaos")) {
            boolean mod = ConfigManager.get().security.moderators.stream()
                    .anyMatch(m -> m.equalsIgnoreCase(twitchUser));
            if (mod) {
                net.twitchcraft.stream.StreamTargets.resolve(server).ifPresent(p ->
                        net.twitchcraft.stream.ChaosWheel.start(server, p));
            }
            return;
        }
        if (first.equals("вызов") || first.equals("challenge")) {
            net.twitchcraft.stream.StreamTargets.resolve(server).ifPresent(p ->
                    net.twitchcraft.stream.StreamerChallenge.get().startFromChat(server, p, twitchUser));
            return;
        }
        if (first.startsWith("превратить") || first.equals("морф") || first.equals("morph")) {
            net.twitchcraft.stream.CipherRiddle.tryGuess(server, twitchUser, ChatGameCommand.MORPH);
            execute(server, twitchUser, message, ChatGameCommand.MORPH, false, false, targetPlayerName);
            return;
        }
        if (first.equals("насмешка") || first.equals("taunt") || first.equals("провокация")) {
            net.twitchcraft.stream.StreamTargets.resolve(server).ifPresent(p ->
                    net.twitchcraft.stream.StreamerTaunt.get().startFromChat(server, p, twitchUser));
            return;
        }

        Optional<ChatGameCommand> cmdOpt = ChatGameCommand.fromMessage(message);
        if (cmdOpt.isPresent()) {
            net.twitchcraft.stream.CipherRiddle.tryGuess(server, twitchUser, cmdOpt.get());
            execute(server, twitchUser, message, cmdOpt.get(), false, false, targetPlayerName);
            return;
        }

        Optional<CustomCommandDefinition> customOpt = CustomCommandRegistry.fromMessage(message, ConfigManager.get());
        if (customOpt.isPresent()) {
            executeCustom(server, twitchUser, message, customOpt.get(), false, false, targetPlayerName);
            return;
        }
        if (!first.isBlank() && first.length() <= 24) {
            net.twitchcraft.stream.StreamChronicle.unknown(twitchUser, first, targetPlayerName);
        }
    }

    public void executeById(MinecraftServer server, String user, String commandId, boolean bypassCooldown) {
        executeById(server, user, commandId, bypassCooldown, null);
    }

    public boolean executeById(MinecraftServer server, String user, String commandId,
                            boolean bypassCooldown, String targetPlayerName) {
        return ChatGameCommand.fromId(commandId)
                .map(cmd -> execute(server, user, "!" + commandId, cmd, bypassCooldown, false, targetPlayerName))
                .orElse(false);
    }

    private boolean execute(MinecraftServer server, String twitchUser, String message, ChatGameCommand command,
                         boolean bypassCooldown, boolean skipAnnounce, String targetPlayerName) {
        TwitchCraftConfig config = ConfigManager.get();
        TwitchCraftConfig.SecuritySettings security = config.security;

        boolean isModerator = security.moderators.stream()
                .anyMatch(m -> m.equalsIgnoreCase(twitchUser));

        if (command.isModeratorOnly() && !isModerator) return false;

        if (security.whitelistEnabled && !isWhitelisted(twitchUser, security) && !skipAnnounce) {
            deny(server, targetPlayerName, twitchUser, command.getRussianName(), CommandDeny.WHITELIST, 0);
            return false;
        }

        boolean crowdGame = command == ChatGameCommand.SIX_SEVEN;

        if (!isModerator && !bypassCooldown && !HouseMode.isActive() && !crowdGame
                && !checkAntiSpam(twitchUser, security.antiSpamSeconds)) {
            deny(server, targetPlayerName, twitchUser, command.getRussianName(), CommandDeny.SPAM,
                    antiSpamLeft(twitchUser, security.antiSpamSeconds));
            return false;
        }

        TwitchCraftConfig.CommandSettings settings = config.getCommandSettings(command.getId(), command.getDefaultCooldown());

        if (!settings.enabled && !isModerator && !bypassCooldown) {
            deny(server, targetPlayerName, twitchUser, command.getRussianName(), CommandDeny.DISABLED, 0);
            return false;
        }

        Optional<ServerPlayer> targetOpt = resolveTarget(server, targetPlayerName);
        if (targetOpt.isEmpty()) {
            deny(server, targetPlayerName, twitchUser, command.getRussianName(), CommandDeny.OFFLINE, 0);
            if (!StreamTargets.hasConfiguredTargets()) {
                TwitchCraftMod.LOGGER.debug("No streamer configured for command {}", command.getId());
            } else {
                TwitchCraftMod.LOGGER.debug("No target player online for command {}", command.getId());
            }
            return false;
        }

        ServerPlayer target = targetOpt.get();
        String streamer = target.getGameProfile().name();
        ServerLevel world = target.level();

        boolean tauntRush = net.twitchcraft.stream.StreamerTaunt.get().isActive()
                && EvilCommands.isEvil(command);

        int resolvedCd = resolveCommandCooldown(target, twitchUser, command, config, settings.cooldownSeconds);
        if (!bypassCooldown && !HouseMode.isActive() && !tauntRush && !crowdGame) {
            String cdKey = CategoryCooldownResolver.scopedKey(streamer,
                    CategoryCooldownResolver.cooldownKey(command, config));
            if (!EffectRunner.cooldowns().isReady(cdKey, resolvedCd) && !isModerator) {
                int left = (int) EffectRunner.cooldowns().secondsRemaining(cdKey, resolvedCd);
                deny(server, streamer, twitchUser, command.getRussianName(), CommandDeny.COOLDOWN, left);
                return false;
            }
        }

        boolean skipSafety = isModerator || HouseMode.isActive();

        if (!skipSafety
                && EvilCommands.isEvil(command)
                && net.twitchcraft.stream.PartnerHold.protects(target)) {
            int left = net.twitchcraft.stream.PartnerHold.secondsLeft();
            AnnouncementService.announceCustom(target,
                    "§eДЕРЖИСЬ §7— урон ещё " + left + "с");
            deny(server, streamer, twitchUser, command.getRussianName(), CommandDeny.HOLD, left);
            return false;
        }

        if (!skipSafety
                && EvilCommands.isEvil(command)
                && net.twitchcraft.stream.StreamerShield.isActive()) {
            int left = net.twitchcraft.stream.StreamerShield.secondsLeft();
            AnnouncementService.announceCustom(target,
                    "§6🛡 Щит кубика §7— урон ещё " + left + "с");
            deny(server, streamer, twitchUser, command.getRussianName(), CommandDeny.SHIELD, left);
            return false;
        }

        if (!skipSafety
                && DeathTracker.isEvilBlocked(target.getGameProfile().name())
                && EvilCommands.isEvil(command)) {
            int left = DeathTracker.evilBlockSecondsLeft(target.getGameProfile().name());
            AnnouncementService.announceCustom(target,
                    "§4☠ Команды с уроном заблокированы ещё " + left + "с");
            deny(server, streamer, twitchUser, command.getRussianName(), CommandDeny.EVIL_BLOCK, left);
            return false;
        }

        if (!skipSafety && !tauntRush
                && EvilCommands.isEvil(command)) {
            int pace = Math.max(4, config.death != null ? config.death.evilPaceSeconds : 12);
            String paceKey = CategoryCooldownResolver.scopedKey(streamer, "evil-pace");
            if (!EffectRunner.cooldowns().isReady(paceKey, pace)) {
                int left = (int) EffectRunner.cooldowns().secondsRemaining(paceKey, pace);
                AnnouncementService.announceCustom(target,
                        "§cУрон чата ещё " + left + "с §8· пусть стример оклемается");
                deny(server, streamer, twitchUser, command.getRussianName(), CommandDeny.EVIL_PACE, left);
                return false;
            }
        }

        if (HardcoreGuard.isBlocked(world, config.security)) {
            TwitchCraftMod.LOGGER.info("Command {} blocked in Hardcore", command.getId());
            deny(server, streamer, twitchUser, command.getRussianName(), CommandDeny.HARDCORE, 0);
            return false;
        }

        if (!bypassCooldown && !HouseMode.isActive() && !tauntRush
                && !checkRateLimit(streamer, command, settings, config.security)) {
            TwitchCraftMod.LOGGER.debug("Rate limit hit for {}", command.getId());
            deny(server, streamer, twitchUser, command.getRussianName(), CommandDeny.RATE, 0);
            return false;
        }

        target = net.twitchcraft.stream.CommandMiss.maybeRedirect(server, target, command, twitchUser);
        streamer = target.getGameProfile().name();

        CommandContext ctx = new CommandContext(server, target, twitchUser, message, command, isModerator);
        try {
            command.execute(ctx);
            if (ctx.failed()) {
                return true;
            }
            if (!crowdGame) {
                ChatActivity.onCommand(command, twitchUser);
            }
            net.twitchcraft.stream.UnnamedPatron.tryAmplify(server, target, twitchUser, command);
            net.twitchcraft.stream.ChatKingManager.get()
                    .tryAmplify(server, target, twitchUser, command);
            net.twitchcraft.stream.StreamerDice.get()
                    .tryDouble(server, target, twitchUser, command);
            net.twitchcraft.stream.StreamerChallenge.get().onCommand(command);
            net.twitchcraft.stream.StreamerCharge.onGoodCommand(server, target, command);
            net.twitchcraft.stream.ViewerKarma.onCommand(server, target, twitchUser, command);

            if (!bypassCooldown && !HouseMode.isActive() && !tauntRush && !crowdGame) {
                String cdKey = CategoryCooldownResolver.scopedKey(streamer,
                        CategoryCooldownResolver.cooldownKey(command, config));
                EffectRunner.cooldowns().markUsed(cdKey);
                if (EvilCommands.isEvil(command)) {
                    EffectRunner.cooldowns().markUsed(
                            CategoryCooldownResolver.scopedKey(streamer, "evil-pace"));
                }
            }

            if (!HouseMode.isActive() && !crowdGame) {
                lastUserCommand.put(twitchUser.toLowerCase(Locale.ROOT), System.currentTimeMillis());
            }

            int baseCd = CategoryCooldownResolver.cooldownSeconds(command, config, settings.cooldownSeconds);
            double cdMult = baseCd > 0 ? resolvedCd / (double) baseCd : 1.0;
            net.twitchcraft.stream.StreamChronicle.command(twitchUser, command, streamer,
                    HouseMode.isActive() ? 0 : resolvedCd, cdMult);

            if (!skipAnnounce) {
                StreamStats.forPlayer(target.getGameProfile().name()).recordCommand(twitchUser, command);
                int cd = HouseMode.isActive() ? 0 : EffectRunner.cooldowns().effectiveCooldown(
                        CategoryCooldownResolver.scopedKey(streamer,
                                CategoryCooldownResolver.cooldownKey(command, config)),
                        resolvedCd);
                if (AnnouncementService.isRareCinematic(command)) {
                    AnnouncementService.announceRareCinematic(target, twitchUser, command);
                } else {
                    AnnouncementService.announceCommand(target, twitchUser, command, cd);
                }
                ChaosMeter.onCommandExecuted(server, target, command);
                StreamTickHandler.battleBar().onCommand(server, target, command);
                if (command == ChatGameCommand.LIGHTNING) {
                    LightningComboTracker.onLightning(server, target, twitchUser);
                }
                CommandChainTracker.onCommand(server, target, twitchUser, command);
                // лента всегда — клиент рисует cinematic + аватар
                if (ConfigManager.get().scoreboard.showCommandFeed
                        || AnnouncementService.isRareCinematic(command)) {
                    CommandAlertPayload.send(target, twitchUser, command.getRussianName(), command.getId(), cd);
                }
            }

            TwitchCraftMod.LOGGER.debug("Executed {} from {} on {}", command.getId(), twitchUser, target.getGameProfile().name());
            return true;
        } catch (Exception e) {
            TwitchCraftMod.LOGGER.error("Failed to execute command {}", command.getId(), e);
            return false;
        }
    }

    private boolean executeCustom(MinecraftServer server, String twitchUser, String message,
                                  CustomCommandDefinition def, boolean bypassCooldown,
                                  boolean skipAnnounce, String targetPlayerName) {
        TwitchCraftConfig config = ConfigManager.get();
        TwitchCraftConfig.SecuritySettings security = config.security;

        boolean isModerator = security.moderators.stream()
                .anyMatch(m -> m.equalsIgnoreCase(twitchUser));

        if (security.whitelistEnabled && !isWhitelisted(twitchUser, security) && !skipAnnounce) {
            deny(server, targetPlayerName, twitchUser, def.displayName(), CommandDeny.WHITELIST, 0);
            return false;
        }

        if (!isModerator && !bypassCooldown && !HouseMode.isActive() && !checkAntiSpam(twitchUser, security.antiSpamSeconds)) {
            deny(server, targetPlayerName, twitchUser, def.displayName(), CommandDeny.SPAM,
                    antiSpamLeft(twitchUser, security.antiSpamSeconds));
            return false;
        }

        if (!def.enabled && !isModerator && !bypassCooldown) {
            deny(server, targetPlayerName, twitchUser, def.displayName(), CommandDeny.DISABLED, 0);
            return false;
        }

        Optional<ServerPlayer> targetOpt = resolveTarget(server, targetPlayerName);
        if (targetOpt.isEmpty()) {
            deny(server, targetPlayerName, twitchUser, def.displayName(), CommandDeny.OFFLINE, 0);
            return false;
        }

        ServerPlayer target = targetOpt.get();
        String streamer = target.getGameProfile().name();

        if (!isModerator && !bypassCooldown && !HouseMode.isActive()
                && EvilCommands.isEvilCustom(def)
                && net.twitchcraft.stream.PartnerHold.protects(target)) {
            int left = net.twitchcraft.stream.PartnerHold.secondsLeft();
            AnnouncementService.announceCustom(target,
                    "§eДЕРЖИСЬ §7— урон ещё " + left + "с");
            deny(server, streamer, twitchUser, def.displayName(), CommandDeny.HOLD, left);
            return false;
        }

        if (!isModerator && !bypassCooldown && !HouseMode.isActive()
                && EvilCommands.isEvilCustom(def)
                && net.twitchcraft.stream.StreamerShield.isActive()) {
            int left = net.twitchcraft.stream.StreamerShield.secondsLeft();
            AnnouncementService.announceCustom(target,
                    "§6🛡 Щит §7— урон ещё " + left + "с");
            deny(server, streamer, twitchUser, def.displayName(), CommandDeny.SHIELD, left);
            return false;
        }

        if (!isModerator && !bypassCooldown && !HouseMode.isActive()
                && DeathTracker.isEvilBlocked(target.getGameProfile().name())
                && EvilCommands.isEvilCustom(def)) {
            int left = DeathTracker.evilBlockSecondsLeft(target.getGameProfile().name());
            AnnouncementService.announceCustom(target,
                    "§4☠ Команды с уроном заблокированы ещё " + left + "с");
            deny(server, streamer, twitchUser, def.displayName(), CommandDeny.EVIL_BLOCK, left);
            return false;
        }

        if (!isModerator && !bypassCooldown && !HouseMode.isActive()
                && EvilCommands.isEvilCustom(def)) {
            int pace = Math.max(4, config.death != null ? config.death.evilPaceSeconds : 12);
            String paceKey = CategoryCooldownResolver.scopedKey(streamer, "evil-pace");
            if (!EffectRunner.cooldowns().isReady(paceKey, pace)) {
                int left = (int) EffectRunner.cooldowns().secondsRemaining(paceKey, pace);
                deny(server, streamer, twitchUser, def.displayName(), CommandDeny.EVIL_PACE, left);
                return false;
            }
        }

        String cdKey = CategoryCooldownResolver.scopedKey(streamer,
                CategoryCooldownResolver.cooldownKey(def, config));

        int cdSec = ChatActivity.scaleCooldown(CategoryCooldownResolver.cooldownSeconds(def, config),
                def, target);
        cdSec = UnnamedPatron.applyCooldown(twitchUser, cdSec);
        cdSec = CipherRiddle.applyCooldown(twitchUser, cdSec);
        if (!bypassCooldown && !HouseMode.isActive()
                && !EffectRunner.cooldowns().isReady(cdKey, cdSec) && !isModerator) {
            int left = (int) EffectRunner.cooldowns().secondsRemaining(cdKey, cdSec);
            deny(server, streamer, twitchUser, def.displayName(), CommandDeny.COOLDOWN, left);
            return false;
        }

        ServerLevel world = target.level();

        if (HardcoreGuard.isBlocked(world, config.security)) {
            deny(server, streamer, twitchUser, def.displayName(), CommandDeny.HARDCORE, 0);
            return false;
        }

        if (!bypassCooldown && !HouseMode.isActive() && !checkCustomRateLimit(streamer, def, security)) {
            deny(server, streamer, twitchUser, def.displayName(), CommandDeny.RATE, 0);
            return false;
        }

        try {
            CustomCommandExecutor.execute(target, twitchUser, def);
            ChatActivity.onCommand(def, twitchUser);

            if (!bypassCooldown && !HouseMode.isActive()) {
                EffectRunner.cooldowns().markUsed(CategoryCooldownResolver.scopedKey(streamer,
                        CategoryCooldownResolver.cooldownKey(def, config)));
                if (EvilCommands.isEvilCustom(def)) {
                    EffectRunner.cooldowns().markUsed(
                            CategoryCooldownResolver.scopedKey(streamer, "evil-pace"));
                }
            }

            if (!HouseMode.isActive()) {
                lastUserCommand.put(twitchUser.toLowerCase(Locale.ROOT), System.currentTimeMillis());
            }

            int baseCd = CategoryCooldownResolver.cooldownSeconds(def, config);
            double cdMult = baseCd > 0 ? cdSec / (double) baseCd : 1.0;
            net.twitchcraft.stream.StreamChronicle.command(twitchUser, def, streamer,
                    HouseMode.isActive() ? 0 : cdSec, cdMult);

            if (!skipAnnounce) {
                int cd = HouseMode.isActive() ? 0 : EffectRunner.cooldowns().effectiveCooldown(
                        CategoryCooldownResolver.scopedKey(streamer,
                                CategoryCooldownResolver.cooldownKey(def, config)),
                        cdSec);
                AnnouncementService.announceCustom(target,
                        twitchUser + " → §e!" + def.displayName() + " §8· §7КД " + cd + "с");
                if (config.scoreboard.showCommandFeed) {
                    CommandAlertPayload.send(target, twitchUser, def.displayName(), def.cooldownKey(), cd);
                }
            }

            TwitchCraftMod.LOGGER.debug("Executed custom {} from {} on {}", def.displayName(), twitchUser,
                    target.getGameProfile().name());
            return true;
        } catch (Exception e) {
            TwitchCraftMod.LOGGER.error("Failed to execute custom command {}", def.displayName(), e);
            return false;
        }
    }

    private boolean checkCustomRateLimit(String playerName, CustomCommandDefinition def,
                                         TwitchCraftConfig.SecuritySettings security) {
        if (!rateLimiter.tryCommand(playerName, def.cooldownKey(), def.maxUsesPerMinute)) return false;
        String type = def.actionType == null ? "effect" : def.actionType.toLowerCase(Locale.ROOT);
        return switch (type) {
            case "mob" -> rateLimiter.tryMobSpawn(playerName, security.mobSpawnRateLimitPerMinute);
            case "item" -> rateLimiter.tryInventory(playerName, security.inventoryRateLimitPerMinute);
            default -> rateLimiter.tryEffect(playerName, security.effectRateLimitPerMinute);
        };
    }

    private int resolveCommandCooldown(ServerPlayer target, String twitchUser, ChatGameCommand command,
                                      TwitchCraftConfig config, int perCommand) {
        int s = CategoryCooldownResolver.cooldownSeconds(command, config, perCommand);
        s = ViewerKarma.applyCooldown(target, command, s);
        s = ChatActivity.scaleCooldown(s, command, target);
        s = UnnamedPatron.applyCooldown(twitchUser, s);
        return CipherRiddle.applyCooldown(twitchUser, s);
    }

    private static Optional<ServerPlayer> resolveTarget(MinecraftServer server, String targetPlayerName) {
        if (targetPlayerName != null && !targetPlayerName.isBlank()) {
            return StreamTargets.resolve(server, targetPlayerName);
        }
        return EffectRunner.resolveTarget(server);
    }

    private boolean checkRateLimit(String playerName, ChatGameCommand command,
                                   TwitchCraftConfig.CommandSettings settings,
                                   TwitchCraftConfig.SecuritySettings security) {
        if (!rateLimiter.tryCommand(playerName, command.getId(), settings.maxUsesPerMinute)) return false;
        return rateLimiter.tryCategory(playerName, command, security);
    }

    private boolean isWhitelisted(String user, TwitchCraftConfig.SecuritySettings security) {
        return security.whitelist.stream().anyMatch(w -> w.equalsIgnoreCase(user));
    }

    private boolean checkAntiSpam(String user, int seconds) {
        if (seconds <= 0) return true;
        Long last = lastUserCommand.get(user.toLowerCase(Locale.ROOT));
        if (last == null) return true;
        return System.currentTimeMillis() - last >= seconds * 1000L;
    }

    private int antiSpamLeft(String user, int seconds) {
        if (seconds <= 0) return 0;
        Long last = lastUserCommand.get(user.toLowerCase(Locale.ROOT));
        if (last == null) return seconds;
        long left = seconds * 1000L - (System.currentTimeMillis() - last);
        return left <= 0 ? 0 : (int) ((left + 999) / 1000);
    }

    private void deny(MinecraftServer server, String targetPlayerName,
                      String user, String commandRu, String reasonId, int seconds) {
        CommandDeny.send(server, targetPlayerName, user, commandRu, reasonId, seconds);
        net.twitchcraft.stream.StreamChronicle.deny(user, commandRu, reasonId, seconds, targetPlayerName);
    }
}
