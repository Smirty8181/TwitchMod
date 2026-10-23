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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * !превратитьлетучаямышь — стример на 60с становится мобом.
 * Раз в 10 минут. Нужно хотя бы одно добро от этого зрителя.
 */
public final class MorphManager {
    private static final MorphManager INSTANCE = new MorphManager();
    private static final int DURATION_SEC = 60;
    private static final long COOLDOWN_MS = 10 * 60_000L;
    private static final Map<String, EntityType<?>> MOBS = new LinkedHashMap<>();

    static {
        put(EntityType.BAT, "летучаямышь", "мышь", "bat", "летучая");
        put(EntityType.CHICKEN, "курица", "chicken", "курочка");
        put(EntityType.COW, "корова", "cow");
        put(EntityType.PIG, "свинья", "pig", "хрюшка");
        put(EntityType.SHEEP, "овца", "sheep");
        put(EntityType.WOLF, "волк", "wolf", "пёс", "пес", "собака");
        put(EntityType.CAT, "кот", "кошка", "cat", "котик");
        put(EntityType.FOX, "лиса", "fox");
        put(EntityType.FROG, "лягушка", "frog", "ква");
        put(EntityType.BEE, "пчела", "bee");
        put(EntityType.ALLAY, "алай", "allay");
        put(EntityType.VILLAGER, "житель", "villager");
        put(EntityType.ZOMBIE, "зомби", "zombie");
        put(EntityType.SKELETON, "скелет", "skeleton");
        put(EntityType.SPIDER, "паук", "spider");
        put(EntityType.CAVE_SPIDER, "пещерныйпаук", "cavespider");
        put(EntityType.CREEPER, "крипер", "creeper");
        put(EntityType.ENDERMAN, "эндермен", "enderman", "ендермен");
        put(EntityType.WITCH, "ведьма", "witch");
        put(EntityType.SLIME, "слизень", "slime", "слайм");
        put(EntityType.MAGMA_CUBE, "магмакуб", "magmacube", "магма");
        put(EntityType.BLAZE, "ифрит", "blaze");
        put(EntityType.GHAST, "гаст", "ghast");
        put(EntityType.HOGLIN, "хоглин", "hoglin");
        put(EntityType.PIGLIN, "пиглин", "piglin");
        put(EntityType.STRIDER, "страйдер", "strider");
        put(EntityType.CAMEL, "верблюд", "camel");
        put(EntityType.HORSE, "лошадь", "horse", "конь");
        put(EntityType.DONKEY, "осёл", "осел", "donkey");
        put(EntityType.LLAMA, "лама", "llama");
        put(EntityType.PANDA, "панда", "panda");
        put(EntityType.POLAR_BEAR, "медведь", "bear", "polarbear");
        put(EntityType.GOAT, "коза", "goat");
        put(EntityType.SNIFFER, "нюхач", "sniffer");
        put(EntityType.ARMADILLO, "броненосец", "armadillo");
        put(EntityType.BREEZE, "бриз", "breeze");
        put(EntityType.BOGGED, "болотник", "bogged");
        put(EntityType.RABBIT, "кролик", "rabbit", "заяц");
        put(EntityType.PARROT, "попугай", "parrot");
        put(EntityType.SQUID, "кальмар", "squid");
        put(EntityType.GLOW_SQUID, "светящийсякальмар", "glowsquid");
        put(EntityType.DOLPHIN, "дельфин", "dolphin");
        put(EntityType.TURTLE, "черепаха", "turtle");
        put(EntityType.IRON_GOLEM, "голем", "irongolem", "големжелезо");
        put(EntityType.SNOW_GOLEM, "снежныйголем", "snowgolem");
        put(EntityType.PHANTOM, "фантом", "phantom");
        put(EntityType.VEX, "векс", "vex");
        put(EntityType.WARDEN, "варден", "warden");
    }

    private boolean active;
    private long endTime;
    private long nextAt;
    private int mobId = -1;
    private String mobRu = "";
    private String viewer = "";
    private EntityType<?> morphType;

    private MorphManager() {}

    public static MorphManager get() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active && System.currentTimeMillis() < endTime;
    }

    public int secondsLeft() {
        if (!isActive()) return 0;
        return (int) Math.max(0, (endTime - System.currentTimeMillis() + 999) / 1000);
    }

    public String statusLine() {
        if (!isActive()) return "";
        return "§d§lМОРФ §f" + mobRu + " §e" + secondsLeft() + "с";
    }

    public static void fromCommand(CommandContext ctx) {
        if (!get().tryStart(ctx.server, ctx.target, ctx.twitchUser, ctx.rawMessage)) {
            ctx.fail();
        }
    }

    public boolean tryStart(MinecraftServer server, ServerPlayer player, String viewer, String raw) {
        if (player == null) return false;
        if (isActive()) {
            AnnouncementService.announceCustom(player, "§7Уже моб ещё §f" + secondsLeft() + "с");
            net.twitchcraft.feedback.CommandDeny.send(player, viewer, "превратить",
                    net.twitchcraft.feedback.CommandDeny.ACTIVE, secondsLeft());
            return false;
        }
        long now = System.currentTimeMillis();
        if (now < nextAt) {
            int left = (int) ((nextAt - now + 999) / 1000);
            AnnouncementService.announceCustom(player, "§7Морф через §f" + left + "с");
            TwitchCraftMod.getConnectionManager().sendChatMessage("Превращение через " + left + "с.");
            net.twitchcraft.feedback.CommandDeny.send(player, viewer, "превратить",
                    net.twitchcraft.feedback.CommandDeny.COOLDOWN, left);
            return false;
        }
        if (viewer != null && !viewer.isBlank() && !ViewerKarma.hasHelped(viewer)) {
            AnnouncementService.announceCustom(player,
                    "§7" + viewer + " ещё не помогал — морф закрыт");
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    viewer + ", сначала !хил / !деревня / !портал — потом !превратить");
            net.twitchcraft.feedback.CommandDeny.send(player, viewer, "превратить",
                    net.twitchcraft.feedback.CommandDeny.HELP, 0);
            return false;
        }

        String spec = extractSpec(raw);
        EntityType<?> type = MOBS.get(spec);
        if (type == null) {
            AnnouncementService.announceCustom(player,
                    "§7Моб? §e!превратитьлетучаямышь §8· курица, крипер, гаст, кот…");
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    "Пиши !превратитьлетучаямышь или !превратить крипер");
            return false;
        }
        if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER) {
            return false;
        }

        ServerLevel level = player.level();
        Entity mob = type.create(level, EntitySpawnReason.COMMAND);
        if (mob == null) {
            AnnouncementService.announceCustom(player, "§7Этот моб не заспавнился");
            return false;
        }
        mob.setPos(player.getX(), player.getY(), player.getZ());
        mob.setCustomName(Component.literal("§d" + (viewer == null ? "чат" : viewer)));
        mob.setCustomNameVisible(true);
        mob.setInvulnerable(true);
        mob.setSilent(true);
        if (mob instanceof Mob m) {
            m.setNoAi(true);
            m.setPersistenceRequired();
            m.setNoGravity(false);
        }
        level.addFreshEntity(mob);

        this.active = true;
        this.endTime = now + DURATION_SEC * 1000L;
        this.nextAt = now + COOLDOWN_MS;
        this.mobId = mob.getId();
        this.mobRu = ruName(spec, type);
        this.viewer = viewer == null ? "чат" : viewer;
        this.morphType = type;

        cloak(player);
        burst(level, player, true);
        AnnouncementService.announceCustom(player,
                "§d§lМОРФ! §f" + mobRu + " §7на 60с §8· " + this.viewer);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                this.viewer + " превратил стримера в " + mobRu + " на 60 секунд!");
        net.twitchcraft.network.CommandAlertPayload.send(player, this.viewer, "превратить " + mobRu, "morph");
        pushHud(server);
        return true;
    }

    public void tick(MinecraftServer server) {
        if (!active) return;
        StreamTargets.resolve(server).ifPresent(p -> {
            if (System.currentTimeMillis() >= endTime) {
                finish(server, p);
                return;
            }
            follow(p);
            if (p.tickCount % 30 == 0) {
                cloak(p);
            }
            if (p.tickCount % 10 == 0) {
                p.level().sendParticles(ParticleTypes.WITCH,
                        p.getX(), p.getY() + 0.4, p.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
            }
        });
    }

    public void pushHud(MinecraftServer server) {
        if (!isActive()) return;
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "MORPH",
                "МОРФ",
                mobRu + " · " + viewer,
                secondsLeft(),
                DURATION_SEC,
                0, 0,
                "1 минута",
                "раз в 10 мин",
                true));
    }

    private void follow(ServerPlayer p) {
        Entity mob = p.level().getEntity(mobId);
        if (mob == null || !mob.isAlive()) return;
        mob.setPos(p.getX(), p.getY(), p.getZ());
        mob.setYRot(p.getYRot());
        mob.setXRot(p.getXRot());
        mob.setDeltaMovement(p.getDeltaMovement());
        mob.hurtMarked = true;
    }

    private void finish(MinecraftServer server, ServerPlayer p) {
        active = false;
        Entity mob = p.level().getEntity(mobId);
        if (mob != null) mob.discard();
        mobId = -1;
        p.removeEffect(MobEffects.INVISIBILITY);
        p.setInvisible(false);
        burst(p.level(), p, false);
        AnnouncementService.announceCustom(p, "§7Морф кончился — снова человек");
        TwitchCraftMod.getConnectionManager().sendChatMessage("Стример снова человек.");
        ShowdownDirector.refresh(server);
    }

    private void cloak(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false));
        p.setInvisible(true);
        if (morphType == EntityType.BAT || morphType == EntityType.PHANTOM
                || morphType == EntityType.GHAST || morphType == EntityType.ALLAY
                || morphType == EntityType.BEE || morphType == EntityType.VEX) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false));
        }
        if (morphType == EntityType.RABBIT || morphType == EntityType.FROG
                || morphType == EntityType.GOAT) {
            p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 40, 1, true, false));
        }
    }

    private static void burst(ServerLevel level, ServerPlayer p, boolean into) {
        level.playSound(null, p.blockPosition(),
                into ? SoundEvents.EVOKER_CAST_SPELL : SoundEvents.ILLUSIONER_MIRROR_MOVE,
                SoundSource.PLAYERS, 1.0f, into ? 0.8f : 1.3f);
        level.sendParticles(into ? ParticleTypes.WITCH : ParticleTypes.CLOUD,
                p.getX(), p.getY() + 1, p.getZ(), 28, 0.5, 0.7, 0.5, 0.08);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                p.getX(), p.getY() + 1, p.getZ(), 16, 0.3, 0.5, 0.3, 0.04);
    }

    private static String extractSpec(String raw) {
        if (raw == null) return "";
        String body = raw.startsWith("!") ? raw.substring(1) : raw;
        body = body.trim().toLowerCase(Locale.ROOT);
        if (body.startsWith("превратить")) body = body.substring("превратить".length());
        else if (body.startsWith("morph")) body = body.substring("morph".length());
        else if (body.startsWith("морф")) body = body.substring("морф".length());
        return body.replace(" ", "").replace("_", "").replace("-", "");
    }

    private static String ruName(String spec, EntityType<?> type) {
        return switch (spec) {
            case "летучаямышь", "мышь", "bat", "летучая" -> "летучая мышь";
            case "creeper", "крипер" -> "крипер";
            case "ghast", "гаст" -> "гаст";
            default -> {
                String d = type.getDescription().getString();
                yield d == null || d.isBlank() ? spec : d.toLowerCase(Locale.ROOT);
            }
        };
    }

    private static void put(EntityType<?> type, String... aliases) {
        for (String a : aliases) {
            MOBS.put(a.toLowerCase(Locale.ROOT), type);
        }
    }
}
