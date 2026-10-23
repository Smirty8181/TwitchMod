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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

import java.util.Locale;

/** !охота — чат ставит таймер. Смерть стримера = победа охотника. */
public final class HuntManager {
    private static final HuntManager INSTANCE = new HuntManager();

    private boolean active;
    private String hunter = "";
    private String targetName = "";
    private long endTime;
    private int durationSec;

    private HuntManager() {}

    public static HuntManager get() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active && System.currentTimeMillis() < endTime;
    }

    public int secondsLeft() {
        if (!active) return 0;
        return (int) Math.max(0, (endTime - System.currentTimeMillis() + 999) / 1000);
    }

    public String hunter() {
        return hunter;
    }

    public String statusLine() {
        if (!isActive()) return "";
        return "§c§lОХОТА §f" + secondsLeft() + "s §7" + trunc(hunter, 10);
    }

    public boolean start(MinecraftServer server, ServerPlayer target, String twitchUser) {
        if (isActive()) {
            AnnouncementService.announceCustom(target,
                    "§cОхота уже идёт §7— §f" + hunter + " §8· §f" + secondsLeft() + "с");
            return false;
        }
        hunter = twitchUser != null && !twitchUser.isBlank() ? twitchUser : "чат";
        targetName = target.getGameProfile().name();
        durationSec = Math.max(20, ConfigManager.get().stream.huntSeconds);
        endTime = System.currentTimeMillis() + durationSec * 1000L;
        active = true;

        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS, 0.7f, 0.7f);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationSec * 20, 0));

        AnnouncementService.announceCustom(target,
                "§c§lОХОТА! §f" + hunter + " §7— выживи §e" + durationSec + "с");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "ОХОТА! " + hunter + " охотится " + durationSec + "с. Если стример умрёт — победа за " + hunter + "!");
        pushHud(server);
        return true;
    }

    public void buyout(MinecraftServer server, ServerPlayer player) {
        if (!isActive()) return;
        String who = hunter;
        finish();
        if (player != null) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 0));
            AnnouncementService.announceCustom(player,
                    "§eВыкуп: охота §f" + who + " §7сорвана, без награды");
        }
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "Стример выкупил охоту предметом. " + who + " не получил победу.");
        ShowdownDirector.refresh(server);
    }

    public void onStreamerDeath(MinecraftServer server, ServerPlayer player) {
        if (!isActive()) return;
        if (targetName != null && !targetName.equalsIgnoreCase(player.getGameProfile().name())) return;
        String winner = hunter;
        finish();
        Graveyard.get().add(winner, "охота");
        AnnouncementService.announceCustom(player,
                "§c§lОХОТНИК ПОБЕДИЛ! §f" + winner + " §7убил стримера");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "ОХОТА: " + winner + " ПОБЕДИЛ! Стример не выжил.");
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "HUNT", "ОХОТНИК ПОБЕДИЛ", winner, 3, 3, 1, 0, "чат", "стример", true));
        ServerTaskQueue.schedule(server, 60, () -> ShowdownDirector.refresh(server));
    }

    public void tick(MinecraftServer server) {
        if (!active) return;
        if (System.currentTimeMillis() >= endTime) {
            survive(server);
        }
    }

    public void pushHud(MinecraftServer server) {
        if (!isActive()) return;
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "HUNT",
                "ОХОТА",
                hunter + " vs стример",
                secondsLeft(),
                durationSec,
                0, 0,
                "смерть = победа чата",
                "выжить " + durationSec + "с",
                true));
    }

    private void survive(MinecraftServer server) {
        String loser = hunter;
        String savedTarget = targetName;
        finish();
        StreamTargets.resolve(server).ifPresent(p -> {
            if (savedTarget != null && !savedTarget.equalsIgnoreCase(p.getGameProfile().name())) return;
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            ServerLevel level = p.level();
            level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0f, 1.2f);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(),
                    24, 0.4, 0.6, 0.4, 0.08);
            for (int i = 0; i < 3; i++) {
                Sheep sheep = EntityType.SHEEP.create(level, EntitySpawnReason.COMMAND);
                if (sheep == null) continue;
                sheep.setPos(p.getX() + i - 1, p.getY(), p.getZ() + 1);
                sheep.setCustomName(Component.literal("§7" + trunc(loser, 12)));
                sheep.setCustomNameVisible(true);
                level.addFreshEntity(sheep);
            }
            StreamerCharge.earn(server, p, "охота", 1);
            AnnouncementService.announceCustom(p,
                    "§a§lСТРИМЕР ВЫЖИЛ! §7охота §f" + loser + " §7провалена §8+заряд");
        });
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "ОХОТА: стример ВЫЖИЛ! " + loser + " не успел.");
        ShowdownDirector.refresh(server);
    }

    private void finish() {
        active = false;
        endTime = 0;
        hunter = "";
        targetName = "";
    }

    private static String trunc(String name, int max) {
        if (name == null || name.isBlank()) return "?";
        return name.length() > max ? name.substring(0, max - 1) + "…" : name;
    }
}
