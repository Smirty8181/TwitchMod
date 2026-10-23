package net.twitchcraft.stream;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.EvilCommands;
import net.twitchcraft.commands.GoodCommands;
import net.twitchcraft.feedback.AnnouncementService;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Карма стримера −100…+100. Добро поднимает, зло опускает.
 * +25 реген и скидка КД добра. −25 сила и чаще молнии.
 */
public final class ViewerKarma {
    public static final int MIN = -100;
    public static final int MAX = 100;
    public static final int BLESS = 25;
    public static final int CURSE = -25;
    private static final int ANGEL_GOODS = 3;

    private static final Map<String, Integer> goods = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> angelDone = new ConcurrentHashMap<>();
    private static final Map<String, Integer> lastTier = new ConcurrentHashMap<>();

    private ViewerKarma() {}

    public static void reset() {
        goods.clear();
        angelDone.clear();
        lastTier.clear();
    }

    public static boolean hasHelped(String user) {
        return goods.getOrDefault(key(user), 0) > 0;
    }

    /** Больше не блокирует урон — шкала вместо запрета. */
    public static boolean allowEvil(String user) {
        return true;
    }

    public static String blockHint() {
        return "Карма: добро поднимает шкалу, зло опускает. 25+ бог, −25 демон.";
    }

    public static int score(ServerPlayer player) {
        return StreamStats.forPlayer(player.getGameProfile().name()).getKarma();
    }

    public static boolean isBlessed(ServerPlayer player) {
        return score(player) >= BLESS;
    }

    public static boolean isCursed(ServerPlayer player) {
        return score(player) <= CURSE;
    }

    public static int applyCooldown(ServerPlayer player, ChatGameCommand command, int seconds) {
        if (player == null || command == null) return seconds;
        if (isBlessed(player) && GoodCommands.isGood(command)) {
            return Math.max(4, (int) Math.round(seconds * 0.7));
        }
        if (isCursed(player) && command == ChatGameCommand.LIGHTNING) {
            return Math.max(4, seconds / 2);
        }
        return seconds;
    }

    public static void onCommand(MinecraftServer server, ServerPlayer target,
                                 String user, ChatGameCommand command) {
        if (user == null || user.isBlank() || command == null || target == null) return;
        String k = key(user);
        StreamStats stats = StreamStats.forPlayer(target.getGameProfile().name());
        int before = stats.getKarma();
        if (GoodCommands.isGood(command)) {
            int n = goods.merge(k, 1, Integer::sum);
            stats.addKarma(goodWeight(command));
            if (n >= ANGEL_GOODS && !angelDone.getOrDefault(k, false)) {
                angelDone.put(k, true);
                becomeAngel(server, target, user, n);
            }
        } else if (EvilCommands.isEvil(command)) {
            stats.addKarma(-evilWeight(command));
            if (isCursed(target) && command != ChatGameCommand.LIGHTNING
                    && ThreadLocalRandom.current().nextFloat() < 0.28f) {
                extraLightning(target);
            }
        }
        maybeAnnounce(target, before, stats.getKarma());
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer p : StreamTargets.onlineStreamers(server)) {
            if (isBlessed(p)) {
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false, true));
            }
            if (isCursed(p)) {
                p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 80, 0, true, false, true));
            }
        }
    }

    public static String sidebarLine(ServerPlayer player) {
        return formatLine(score(player));
    }

    public static String formatLine(int k) {
        String bar = k >= BLESS ? "§a§l♥ БОГ" : (k <= CURSE ? "§c§l♥ ДЕМОН" : "§e♥ КАРМА");
        String num = (k > 0 ? "+" : "") + k;
        return bar + " §f" + num;
    }

    public static String sidebarHint(ServerPlayer player) {
        return sidebarLine(player);
    }

    private static int goodWeight(ChatGameCommand command) {
        return switch (command) {
            case HEAL, SAVE, TOTEM, GAPPLE, REGEN, HELP -> 5;
            default -> 3;
        };
    }

    private static int evilWeight(ChatGameCommand command) {
        return switch (command) {
            case LIGHTNING, TNT, TNT_RAIN, BOMB, METEOR, LAVA, WITHER -> 5;
            default -> 3;
        };
    }

    private static void maybeAnnounce(ServerPlayer target, int before, int now) {
        int oldT = tier(before);
        int newT = tier(now);
        String name = target.getGameProfile().name().toLowerCase(Locale.ROOT);
        Integer prev = lastTier.put(name, newT);
        if (oldT != newT || prev == null) {
            if (newT == 1) {
                AnnouncementService.announceCustom(target,
                        "§a§lКАРМА +" + now + " §7реген I · добро быстрее");
                TwitchCraftMod.getConnectionManager().sendChatMessage(
                        "КАРМА +" + now + ": чат играет в ДОБРОГО бога. Реген и скидка на хил.");
                return;
            }
            if (newT == -1) {
                AnnouncementService.announceCustom(target,
                        "§c§lКАРМА " + now + " §7сила I · молнии чаще");
                TwitchCraftMod.getConnectionManager().sendChatMessage(
                        "КАРМА " + now + ": чат играет в ЗЛОГО бога. Сила, но молнии сыпятся.");
                return;
            }
            if (oldT != 0) {
                AnnouncementService.announceCustom(target, "§eКарма " + now + " §7нейтраль");
            }
        }
        if (Math.floorDiv(before, 10) != Math.floorDiv(now, 10) && now != 0 && oldT == newT) {
            AnnouncementService.announceCustom(target, "§e♥ Карма " + (now > 0 ? "+" : "") + now);
        }
    }

    private static int tier(int karma) {
        if (karma >= BLESS) return 1;
        if (karma <= CURSE) return -1;
        return 0;
    }

    private static void extraLightning(ServerPlayer p) {
        ServerLevel level = p.level();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.COMMAND);
        if (bolt != null) {
            bolt.setPos(p.getX(), p.getY(), p.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        if (p.isAlive()) {
            p.hurtServer(level, p.damageSources().lightningBolt(), 4.0f);
        }
    }

    private static void becomeAngel(MinecraftServer server, ServerPlayer target, String user, int n) {
        ServerLevel level = target.level();
        target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.PLAYERS, 0.9f, 1.3f);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                target.getX(), target.getY() + 1, target.getZ(), 36, 0.5, 0.8, 0.5, 0.15);
        level.sendParticles(ParticleTypes.HEART,
                target.getX(), target.getY() + 1.6, target.getZ(), 10, 0.4, 0.3, 0.4, 0.02);
        AnnouncementService.announceCustom(target, "§a§l♥ АНГЕЛ ЧАТА: §f" + user + " §7×" + n);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "АНГЕЛ: " + user + " помог " + n + " раз! Можно !превратить");
        GuideNpcManager.get().spawn(server, target, user, "ангел", false);
    }

    private static String key(String user) {
        return user == null ? "" : user.toLowerCase(Locale.ROOT);
    }
}
