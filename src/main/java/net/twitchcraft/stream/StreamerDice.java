package net.twitchcraft.stream;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.DicePayload;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Кубик кидает только мир в случайный момент.
 * Чат и стример не могут бросить и не влияют на грань.
 */
public final class StreamerDice {
    public enum Source { WORLD }

    private static final StreamerDice INSTANCE = new StreamerDice();
    private static final long ROLL_MS = 1800;

    private long rollingUntil;
    private boolean doubleNext;

    private StreamerDice() {}

    public static StreamerDice get() {
        return INSTANCE;
    }

    public boolean isRolling() {
        return System.currentTimeMillis() < rollingUntil;
    }

    public boolean consumeDouble() {
        if (!doubleNext) return false;
        doubleNext = false;
        return true;
    }

    public boolean refuseManual(ServerPlayer player) {
        if (player != null) {
            AnnouncementService.announceCustom(player, "§7Кубик кидает только мир — в случайный момент");
        }
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "Кубик кидает только мир. Ни чат, ни стример не могут повлиять.");
        return false;
    }

    public boolean rollFromWorld(MinecraftServer server, ServerPlayer player) {
        if (player == null) return false;
        if (isRolling()) return false;

        int face = 1 + ThreadLocalRandom.current().nextInt(6);
        rollingUntil = System.currentTimeMillis() + ROLL_MS;
        FaceInfo info = info(face);
        DicePayload.broadcast(server, face, info.title, info.subtitle, shieldSeconds(face));

        ServerLevel level = player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
                SoundSource.PLAYERS, 0.9f, 0.8f);
        level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.4, player.getZ(),
                16, 0.3, 0.3, 0.3, 0.08);

        TwitchCraftMod.getConnectionManager().sendChatMessage("МИР КИДАЕТ КУБИК…");
        ServerTaskQueue.schedule(server, 36, () -> apply(server, player, face));
        return true;
    }

    public void tryDouble(MinecraftServer server, ServerPlayer target, String user, ChatGameCommand command) {
        if (!consumeDouble() || command == null) return;
        if (command == ChatGameCommand.WITHER || command == ChatGameCommand.DRAGON
                || command == ChatGameCommand.CHAT_PHANTOM) {
            return;
        }
        CommandContext bonus = new CommandContext(server, target, user, "!" + command.getId(), command, true);
        try {
            command.execute(bonus);
        } catch (Exception e) {
            TwitchCraftMod.LOGGER.debug("Dice double failed: {}", command.getId(), e);
        }
        AnnouncementService.announceCustom(target,
                "§6§l🎲 ×2 §7кубик удвоил §e!" + command.getRussianName());
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "Кубик ×2: !" + command.getRussianName() + " от " + user);
    }

    private void apply(MinecraftServer server, ServerPlayer player, int face) {
        if (player == null || !player.isAlive()) {
            StreamTargets.resolve(server).ifPresent(p -> apply(server, p, face));
            return;
        }
        ServerLevel level = player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.8f, 1.1f);
        FaceInfo info = info(face);
        AnnouncementService.announceCustom(player, "§6§l🎲 " + face + " §7— " + info.title);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "Кубик мира: " + face + " — " + info.chat);

        switch (face) {
            case 1 -> RandomEventScheduler.fire(server, "§6§lКУБИК");
            case 2 -> {
                doubleNext = true;
                AnnouncementService.announceCustom(player, "§eСледующая команда чата удвоится");
            }
            case 3 -> {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 0));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0));
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        player.getX(), player.getY() + 1, player.getZ(), 20, 0.4, 0.6, 0.4, 0.1);
            }
            case 4 -> VoteManager.get().start(server);
            case 5 -> HuntManager.get().start(server, player, "мир");
            case 6 -> StreamerShield.activate(server, player, shieldSeconds(6), "кубик мира");
            default -> { }
        }
    }

    private static int shieldSeconds(int face) {
        if (face != 6) return 0;
        int cfg = net.twitchcraft.config.ConfigManager.get().stream.diceShieldMinutes * 60;
        return Math.max(35, Math.min(cfg, 90));
    }

    private static FaceInfo info(int face) {
        return switch (face) {
            case 1 -> new FaceInfo("ХАОС", "случайное событие", "хаос на стримера");
            case 2 -> new FaceInfo("×2", "команда чата удваивается", "×2 следующей команде чата");
            case 3 -> new FaceInfo("БАФФ", "хил + скорость", "стример получил бафф");
            case 4 -> new FaceInfo("ГОЛОС", "чат голосует !1 !2 !3", "голосование");
            case 5 -> new FaceInfo("ОХОТА", "чат охотится", "охота");
            default -> new FaceInfo("ЩИТ", "урон " + shieldSeconds(6) + "с", "щит от урона");
        };
    }

    private record FaceInfo(String title, String subtitle, String chat) {}
}
