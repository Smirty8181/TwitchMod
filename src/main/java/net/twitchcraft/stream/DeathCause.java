package net.twitchcraft.stream;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

/** Кто и чем убил стримера — для экрана и чата. */
public record DeathCause(String killer, String detail, boolean fromChat) {
    public static final DeathCause UNKNOWN = new DeathCause("", "неизвестно", false);

    public DeathCause(String killer, String detail) {
        this(killer, detail, false);
    }

    public static DeathCause resolve(ServerPlayer victim, DamageSource source) {
        if (victim == null || source == null) return UNKNOWN;

        Entity attacker = source.getEntity();
        if (attacker != null) {
            GhostMobRegistry.GhostInfo ghost = GhostMobRegistry.get(attacker.getUUID());
            if (ghost != null && ghost.viewer() != null && !ghost.viewer().isBlank()) {
                String mob = ghost.mobRu() == null || ghost.mobRu().isBlank() ? entityRu(attacker) : ghost.mobRu();
                return new DeathCause(plain(ghost.viewer()), mob, true);
            }
            if (attacker instanceof ServerPlayer p) {
                String name = p.getGameProfile().name();
                if (StreamTargets.isTarget(p) && !p.getUUID().equals(victim.getUUID())) {
                    return new DeathCause(name, "напарник");
                }
                return new DeathCause(name, "игрок");
            }
            String named = customNick(attacker);
            String type = entityRu(attacker);
            if (!named.isBlank()) return new DeathCause(named, type, true);
            if (!type.isBlank()) return new DeathCause(type, worldRu(source));
        }

        if (HuntManager.get().isActive()) {
            String hunter = HuntManager.get().hunter();
            if (hunter != null && !hunter.isBlank()) {
                return new DeathCause(hunter, "охота", true);
            }
        }

        String world = worldRu(source);
        Entity direct = source.getDirectEntity();
        if (direct != null && direct != attacker) {
            String nick = customNick(direct);
            if (!nick.isBlank()) return new DeathCause(nick, world, true);
        }
        return new DeathCause("", world);
    }

    public boolean hasKiller() {
        return killer != null && !killer.isBlank();
    }

    public String screenKiller() {
        return hasKiller() ? killer : detail;
    }

    public String screenDetail() {
        if (!hasKiller()) return "";
        return detail == null ? "" : detail;
    }

    public String twitchLine(String victim) {
        String who = victim == null || victim.isBlank() ? "стример" : victim;
        if (hasKiller()) {
            String extra = detail == null || detail.isBlank() ? "" : " (" + detail + ")";
            return who + " умер. Убил: " + killer + extra;
        }
        return who + " умер: " + (detail == null || detail.isBlank() ? "неизвестно" : detail);
    }

    private static String customNick(Entity entity) {
        if (entity == null || !entity.hasCustomName() || entity.getCustomName() == null) return "";
        String raw = entity.getCustomName().getString();
        return plain(raw);
    }

    private static String plain(String raw) {
        if (raw == null) return "";
        String s = raw.replaceAll("§.", "").replace('\n', ' ').trim();
        s = s.replaceAll("^[\\[\\(❤♥⚔💀👻🐔]+\\s*", "");
        s = s.replaceAll("[\\]\\)]+$", "").trim();
        if (s.length() > 18) s = s.substring(0, 17) + "…";
        return s;
    }

    private static String entityRu(Entity entity) {
        if (entity == null) return "моб";
        EntityType<?> type = entity.getType();
        if (type == EntityType.PLAYER || entity instanceof Player) return "игрок";
        if (type == EntityType.CREEPER) return "крипер";
        if (type == EntityType.ZOMBIE) return "зомби";
        if (type == EntityType.SKELETON) return "скелет";
        if (type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER) return "паук";
        if (type == EntityType.ENDERMAN) return "эндермен";
        if (type == EntityType.WITCH) return "ведьма";
        if (type == EntityType.SLIME) return "слайм";
        if (type == EntityType.GHAST) return "гаст";
        if (type == EntityType.PHANTOM) return "фантом";
        if (type == EntityType.WITHER) return "визер";
        if (type == EntityType.ENDER_DRAGON) return "дракон";
        if (type == EntityType.WOLF) return "волк";
        if (type == EntityType.IRON_GOLEM) return "голем";
        if (type == EntityType.WARDEN) return "варден";
        if (type == EntityType.BLAZE) return "ифрит";
        if (type == EntityType.PIGLIN || type == EntityType.ZOMBIFIED_PIGLIN) return "пиглин";
        if (type == EntityType.HOGLIN || type == EntityType.ZOGLIN) return "хоглин";
        if (type == EntityType.PILLAGER) return "разбойник";
        if (type == EntityType.VINDICATOR) return "поборник";
        if (type == EntityType.EVOKER) return "заклинатель";
        if (type == EntityType.RAVAGER) return "равагер";
        if (type == EntityType.DROWNED) return "утопленник";
        if (type == EntityType.HUSK) return "кадавр";
        if (type == EntityType.STRAY) return "строг";
        if (type == EntityType.ENDERMITE) return "эндермит";
        if (type == EntityType.SILVERFISH) return "чешуйница";
        if (type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN) return "страж";
        if (type == EntityType.SHULKER) return "шалкер";
        if (type == EntityType.VEX) return "вредина";
        if (type == EntityType.BREEZE) return "breeze";
        if (type == EntityType.BOGGED) return "болотный";
        if (type == EntityType.CREAKING) return "скрипун";
        if (type == EntityType.CHICKEN) return "курица";
        if (type == EntityType.BEE) return "пчела";
        if (type == EntityType.LIGHTNING_BOLT) return "молния";
        if (type == EntityType.TNT || type == EntityType.TNT_MINECART) return "TNT";
        if (type == EntityType.FALLING_BLOCK) return "падение блока";
        String key = type.toShortString();
        return key == null || key.isBlank() ? "моб" : key;
    }

    private static String worldRu(DamageSource source) {
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.STALAGMITE)) return "падение";
        if (source.is(DamageTypes.LAVA)) return "лава";
        if (source.is(DamageTypes.DROWN)) return "вода";
        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.CAMPFIRE) || source.is(DamageTypes.HOT_FLOOR)) return "огонь";
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) return "пустота";
        if (source.is(DamageTypes.STARVE)) return "голод";
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) return "магия";
        if (source.is(DamageTypes.LIGHTNING_BOLT)) return "молния";
        if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) return "взрыв";
        if (source.is(DamageTypes.WITHER) || source.is(DamageTypes.WITHER_SKULL)) return "визер";
        if (source.is(DamageTypes.FREEZE)) return "холод";
        if (source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH)
                || source.is(DamageTypes.STING)) return "шипы";
        if (source.is(DamageTypes.FLY_INTO_WALL)) return "удар об стену";
        if (source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.IN_WALL)) return "задавило";
        if (source.is(DamageTypes.SONIC_BOOM)) return "варден";
        if (source.is(DamageTypes.THORNS)) return "шипы брони";
        if (source.is(DamageTypes.DRAGON_BREATH)) return "дракон";
        if (source.is(DamageTypes.BAD_RESPAWN_POINT)) return "кровать";
        String id = source.getMsgId();
        if (id == null || id.isBlank()) return "урон";
        return switch (id) {
            case "fall" -> "падение";
            case "lava" -> "лава";
            case "drown" -> "вода";
            case "inFire", "onFire" -> "огонь";
            case "outOfWorld" -> "пустота";
            case "starve" -> "голод";
            case "explosion", "explosion.player" -> "взрыв";
            case "lightningBolt" -> "молния";
            default -> "урон";
        };
    }
}
