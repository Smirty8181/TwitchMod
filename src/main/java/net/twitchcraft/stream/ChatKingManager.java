package net.twitchcraft.stream;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

/** !король — зритель коронуется. Его следующая моб-команда удваивается. */
public final class ChatKingManager {
    private static final ChatKingManager INSTANCE = new ChatKingManager();

    private String king = "";
    private long until;
    private boolean amplified;

    private ChatKingManager() {}

    public static ChatKingManager get() {
        return INSTANCE;
    }

    public boolean isActive() {
        return king != null && !king.isBlank() && System.currentTimeMillis() < until;
    }

    public int secondsLeft() {
        if (!isActive()) return 0;
        return (int) Math.max(0, (until - System.currentTimeMillis() + 999) / 1000);
    }

    public String king() {
        return king;
    }

    public String statusLine() {
        if (!isActive()) return "";
        return "§6§l👑 " + trunc(king, 10) + " §f" + secondsLeft() + "с";
    }

    public void crown(MinecraftServer server, ServerPlayer target, String twitchUser) {
        String name = twitchUser != null && !twitchUser.isBlank() ? twitchUser : "чат";
        int sec = Math.max(20, ConfigManager.get().stream.kingSeconds);
        king = name;
        until = System.currentTimeMillis() + sec * 1000L;
        amplified = false;

        ServerLevel level = target.level();
        Allay allay = EntityType.ALLAY.create(level, EntitySpawnReason.COMMAND);
        if (allay != null) {
            allay.setPos(target.getX() + 1, target.getY() + 1, target.getZ());
            allay.setCustomName(Component.literal("§6👑 " + trunc(name, 14)));
            allay.setCustomNameVisible(true);
            allay.addEffect(new MobEffectInstance(MobEffects.GLOWING, sec * 20, 0));
            level.addFreshEntity(allay);
        }
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.9f, 0.6f);
        level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 2, target.getZ(),
                18, 0.4, 0.5, 0.4, 0.02);

        AnnouncementService.announceCustom(target,
                "§6§l👑 КОРОЛЬ ЧАТА: §f" + name + " §7— мобы ×2 на " + sec + "с");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "Король чата: " + name + "! Его моб-команды удваиваются " + sec + "с.");
        GuideNpcManager.get().spawn(server, target, name, "король", false);
        pushHud(server);
    }

    public void tryAmplify(MinecraftServer server, ServerPlayer target, String twitchUser, ChatGameCommand command) {
        if (!isActive() || amplified || command == null || twitchUser == null) return;
        if (!king.equalsIgnoreCase(twitchUser)) return;
        if (command.getCategory() != ChatGameCommand.Category.MOB) return;
        if (command == ChatGameCommand.WITHER || command == ChatGameCommand.DRAGON
                || command == ChatGameCommand.CHAT_PHANTOM) {
            return;
        }
        amplified = true;
        CommandContext bonus = new CommandContext(server, target, twitchUser, "!" + command.getId(), command, true);
        try {
            command.execute(bonus);
        } catch (Exception e) {
            TwitchCraftMod.LOGGER.debug("King amplify failed: {}", command.getId(), e);
        }
        AnnouncementService.announceCustom(target, "§6§l👑 " + king + " §7удвоил §e!" + command.getRussianName());
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "Король " + king + " удвоил !" + command.getRussianName() + "!");
    }

    public void tick(MinecraftServer server) {
        if (king != null && !king.isBlank() && System.currentTimeMillis() >= until) {
            king = "";
            until = 0;
        }
    }

    public void pushHud(MinecraftServer server) {
        if (!isActive()) return;
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "KING",
                "КОРОЛЬ ЧАТА",
                king,
                secondsLeft(),
                Math.max(20, ConfigManager.get().stream.kingSeconds),
                0, 0,
                "мобы ×2",
                amplified ? "уже удвоил" : "ждёт команду",
                true));
    }

    private static String trunc(String name, int max) {
        if (name == null || name.isBlank()) return "?";
        return name.length() > max ? name.substring(0, max - 1) + "…" : name;
    }
}
