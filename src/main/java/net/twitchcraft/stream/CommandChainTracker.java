package net.twitchcraft.stream;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ComboChainPayload;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Цепочка от 3 разных зрителей: !молния → !крипер → !tnt за 10 минут. */
public final class CommandChainTracker {
    private static final long WINDOW_MS = 10 * 60 * 1000L;
    private static final ChatGameCommand[] STEPS = {
            ChatGameCommand.LIGHTNING,
            ChatGameCommand.CREEPER,
            ChatGameCommand.TNT
    };

    private static final class State {
        long startedAt;
        int step;
        final String[] users = new String[3];
    }

    private static final Map<String, State> BY_TARGET = new ConcurrentHashMap<>();

    private CommandChainTracker() {}

    public static void onPlayerDisconnect(String playerName) {
        if (playerName != null) BY_TARGET.remove(playerName);
    }

    public static void onCommand(MinecraftServer server, ServerPlayer target, String twitchUser,
                                 ChatGameCommand command) {
        if (target == null || twitchUser == null || twitchUser.isBlank()) return;

        String key = target.getGameProfile().name();
        long now = System.currentTimeMillis();
        State state = BY_TARGET.get(key);

        if (state == null) {
            if (command != STEPS[0]) return;
            state = new State();
            state.startedAt = now;
            state.step = 0;
            state.users[0] = twitchUser.toLowerCase(Locale.ROOT);
            BY_TARGET.put(key, state);
            AnnouncementService.announceCustom(target,
                    "§e" + twitchUser + " §7начал цепочку! §8(1/3 §7!молния§8)");
            return;
        }

        if (now - state.startedAt > WINDOW_MS) {
            BY_TARGET.remove(key);
            if (command != STEPS[0]) return;
            onCommand(server, target, twitchUser, command);
            return;
        }

        int expected = state.step + 1;
        if (expected >= STEPS.length) {
            BY_TARGET.remove(key);
            return;
        }

        if (command != STEPS[expected]) {
            BY_TARGET.remove(key);
            return;
        }

        String userKey = twitchUser.toLowerCase(Locale.ROOT);
        for (int i = 0; i < expected; i++) {
            if (userKey.equals(state.users[i])) {
                return;
            }
        }

        state.users[expected] = userKey;
        state.step = expected;

        if (expected < STEPS.length - 1) {
            AnnouncementService.announceCustom(target,
                    "§e" + twitchUser + " §7→ §f!" + command.getRussianName()
                            + " §8(" + (expected + 1) + "/3)");
            return;
        }

        BY_TARGET.remove(key);
        triggerCombo(server, target, state.users);
    }

    private static void triggerCombo(MinecraftServer server, ServerPlayer target, String[] users) {
        String u1 = users[0] != null ? users[0] : "?";
        String u2 = users[1] != null ? users[1] : "?";
        String u3 = users[2] != null ? users[2] : "?";

        ComboChainPayload.send(target, u1, u2, u3);

        target.connection.send(new ClientboundSetTitlesAnimationPacket(5, 70, 15));
        target.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("§4§l⚡ КОМБО ЦЕПОЧКА!")));
        target.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("§e!" + u1 + " §7→ §a!" + u2 + " §7→ §c!" + u3)));
        target.connection.send(new ClientboundSetActionBarTextPacket(
                Component.literal("§6§lКОМБО §7— молния → крипер → TNT!")));

        AnnouncementService.announceCustom(target, "§4§lКОМБО! §e3 зрителя собрали цепочку!");

        CommandContext ctx1 = new CommandContext(server, target, u1, "!молния", ChatGameCommand.LIGHTNING, false);
        EffectRunner.lightning(ctx1);

        ServerTaskQueue.schedule(server, 18, () -> {
            if (!TargetPlayerUtil.isOnline(server, target)) return;
            CommandContext ctx2 = new CommandContext(server, target, u2, "!крипер", ChatGameCommand.CREEPER, false);
            EffectRunner.spawnCreeper(ctx2);
        });

        ServerTaskQueue.schedule(server, 36, () -> {
            if (!TargetPlayerUtil.isOnline(server, target)) return;
            CommandContext ctx3 = new CommandContext(server, target, u3, "!tnt", ChatGameCommand.TNT, false);
            EffectRunner.spawnTnt(ctx3);
        });
    }
}
