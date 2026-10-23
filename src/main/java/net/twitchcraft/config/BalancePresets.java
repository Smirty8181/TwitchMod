package net.twitchcraft.config;

import net.twitchcraft.commands.ChatGameCommand;

import java.util.HashMap;
import java.util.Map;

/** Баланс под дуо-стрим: часто, смешно, без вечного КД-пака. */
public final class BalancePresets {
    public static final int CHAOS_VERSION = 14;

    private static final Map<String, Integer> COOLDOWNS = new HashMap<>();
    private static final Map<String, Integer> MAX_PER_MINUTE = new HashMap<>();

    static {
        // Шоу / пранки — коротко, чтобы чат жил
        set("slap", 8, 8);
        set("kva", 8, 8);
        set("coin", 8, 10);
        set("glow", 8, 8);
        set("sign", 8, 8);
        set("sixseven", 2, 12);
        set("bet", 4, 12);
        set("defuse", 5, 8);
        set("voice", 10, 6);
        set("jumpscare", 12, 5);
        set("clown", 12, 5);
        set("fish", 12, 5);
        set("dance", 12, 5);
        set("vine", 12, 5);
        set("troll", 14, 5);
        set("brainrot", 12, 6);
        set("prime", 14, 5);
        set("firework", 10, 6);
        set("hype", 16, 4);
        set("piggyback", 16, 4);
        set("carryon", 16, 4);
        set("ragdoll", 45, 1);
        set("primerave", 22, 3);
        set("oia", 18, 4);
        set("amongus", 18, 3);
        set("vieweregg", 12, 5);
        set("king", 28, 2);
        set("hunt", 50, 1);
        set("vote", 40, 2);
        set("random", 18, 4);
        set("roulette", 28, 2);
        set("event", 28, 2);
        set("morph", 90, 1);
        set("ant", 36, 2);
        set("titan", 50, 1);
        set("tether", 28, 2);
        set("orevein", 50, 2);
        set("parkour", 40, 1);
        set("invert", 28, 1);
        set("upgrade", 40, 2);

        // Мобы
        set("creeper", 12, 5);
        set("zombie", 10, 6);
        set("skeleton", 12, 5);
        set("spider", 12, 5);
        set("slime", 12, 5);
        set("sheep", 10, 6);
        set("chicken", 10, 6);
        set("wolf", 14, 4);
        set("enderman", 18, 3);
        set("witch", 18, 3);
        set("ghast", 28, 2);
        set("irongolem", 24, 2);
        set("goose", 14, 4);
        set("chaoschickens", 18, 3);
        set("goldenhen", 20, 3);
        set("buddy", 18, 3);
        set("fpv", 32, 2);
        set("tck", 24, 2);
        set("army", 45, 2);
        set("viewerarmy", 32, 2);
        set("creeperstorm", 40, 1);
        set("boss", 55, 1);
        set("wither", 150, 1);
        set("dragon", 220, 1);
        set("phantom", 180, 1);

        // TNT / хаос
        set("tnt", 28, 3);
        set("fire", 16, 4);
        set("water", 14, 4);
        set("anvil", 22, 3);
        set("lava", 32, 2);
        set("pit", 32, 2);
        set("trap", 28, 2);
        set("dome", 28, 2);
        set("prison", 40, 1);
        set("tntrain", 50, 1);
        set("meteor", 50, 1);
        set("lucky", 24, 3);
        set("unlucky", 24, 3);
        set("verylucky", 32, 2);
        set("luckyroulette", 28, 2);
        set("bomb", 60, 1);

        // Движение
        set("knockback", 10, 6);
        set("up", 12, 5);
        set("down", 12, 5);
        set("spin", 14, 4);
        set("launch", 24, 3);
        set("novalny", 14, 4);
        set("drunk", 14, 4);
        set("gravity", 18, 3);
        set("tp", 22, 3);
        set("backward", 18, 3);
        set("swap", 32, 2);

        // Эффекты
        set("hurt", 12, 5);
        set("blind", 14, 4);
        set("slow", 12, 5);
        set("hunger", 12, 4);
        set("poison", 16, 4);
        set("weakness", 12, 5);
        set("flash", 12, 5);
        set("lightning", 22, 3);
        set("storm", 32, 2);
        set("giant", 22, 3);
        set("speed", 12, 5);
        set("heal", 16, 5);
        set("feed", 12, 6);
        set("regen", 16, 4);
        set("strength", 16, 4);
        set("help", 14, 5);
        set("antidote", 12, 5);
        set("respect", 12, 5);
        set("save", 24, 3);
        set("xp", 14, 5);
        set("fireres", 28, 2);

        // Инвентарь
        set("egg", 10, 6);
        set("junk", 12, 5);
        set("drop", 22, 3);
        set("shuffle", 16, 4);
        set("bow", 16, 4);
        set("steal", 28, 2);
        set("curse", 28, 2);
        set("clear", 55, 1);
        set("diamond", 28, 3);
        set("elytra", 24, 3);
        set("totem", 28, 2);
        set("gapple", 16, 4);
        set("torches", 12, 5);
        set("bread", 10, 6);
        set("planks", 10, 6);
        set("potion", 16, 4);
        set("heist", 28, 2);
        set("swapinv", 22, 3);
        set("portalkit", 70, 1);
        set("endereye", 40, 2);

        // Мир
        set("day", 22, 3);
        set("night", 22, 3);
        set("rain", 18, 3);
        set("clearweather", 18, 3);
        set("noon", 22, 3);
        set("thunder", 28, 2);
        set("village", 45, 2);
        set("fortress", 55, 1);
        set("bastion", 55, 1);
        set("stronghold", 70, 1);
    }

    private BalancePresets() {}

    private static void set(String id, int cd, int maxPerMin) {
        COOLDOWNS.put(id, cd);
        MAX_PER_MINUTE.put(id, maxPerMin);
    }

    public static void apply(TwitchCraftConfig config) {
        for (ChatGameCommand cmd : ChatGameCommand.values()) {
            if (cmd.getCategory() == ChatGameCommand.Category.MODERATOR) continue;
            TwitchCraftConfig.CommandSettings s = config.getCommandSettings(cmd.getId(), cmd.getDefaultCooldown());
            s.cooldownSeconds = cooldownFor(cmd);
            int max = maxPerMinuteFor(cmd);
            if (max > 0) s.maxUsesPerMinute = max;
        }
        applyChaosLimits(config);
    }

    private static void applyChaosLimits(TwitchCraftConfig config) {
        if (config.cooldownPacks == null) {
            config.cooldownPacks = new TwitchCraftConfig.CooldownPackSettings();
        }
        var packs = config.cooldownPacks;
        packs.sharedPackCooldowns = false;
        packs.goodPackSeconds = 12;
        packs.chaosPackSeconds = 16;
        packs.mobPackSeconds = 10;
        packs.tntPackSeconds = 18;
        packs.effectPackSeconds = 8;
        packs.movementPackSeconds = 10;
        packs.inventoryPackSeconds = 12;
        packs.worldPackSeconds = 18;
        packs.funPackSeconds = 8;

        config.security.antiSpamSeconds = 20;
        config.security.mobSpawnRateLimitPerMinute = 18;
        config.security.tntRateLimitPerMinute = 8;
        config.security.chaosRateLimitPerMinute = 12;
        config.security.effectRateLimitPerMinute = 16;
        config.security.movementRateLimitPerMinute = 14;
        config.security.inventoryRateLimitPerMinute = 12;
        config.security.worldRateLimitPerMinute = 8;
        config.security.funRateLimitPerMinute = 16;
    }

    private static int cooldownFor(ChatGameCommand cmd) {
        Integer exact = COOLDOWNS.get(cmd.getId());
        if (exact != null) return exact;
        return switch (cmd.getCategory()) {
            case FUN -> 12;
            case EFFECT -> 14;
            case MOB -> 14;
            case TNT -> 22;
            case CHAOS -> 20;
            case INVENTORY -> 16;
            case MOVEMENT -> 14;
            case WORLD -> 22;
            default -> Math.max(8, cmd.getDefaultCooldown());
        };
    }

    private static int maxPerMinuteFor(ChatGameCommand cmd) {
        Integer exact = MAX_PER_MINUTE.get(cmd.getId());
        if (exact != null) return exact;
        return switch (cmd.getCategory()) {
            case FUN, EFFECT -> 5;
            case MOB, MOVEMENT, INVENTORY -> 4;
            case CHAOS, TNT -> 3;
            case WORLD -> 2;
            default -> 3;
        };
    }

    public static int recommendedCooldown(String commandId, int fallback) {
        return COOLDOWNS.getOrDefault(commandId, fallback);
    }

    public static int recommendedMaxPerMinute(String commandId) {
        return MAX_PER_MINUTE.getOrDefault(commandId, 0);
    }

    /** Дуо на LAN: чуть свободнее лимиты, тот же хаос-КД. */
    public static void applyDuo(TwitchCraftConfig config) {
        apply(config);
        config.security.antiSpamSeconds = 20;
        config.security.mobSpawnRateLimitPerMinute = 20;
        config.security.tntRateLimitPerMinute = 8;
        config.security.chaosRateLimitPerMinute = 14;
        config.security.effectRateLimitPerMinute = 18;
        config.security.movementRateLimitPerMinute = 16;
        config.security.inventoryRateLimitPerMinute = 14;
        config.security.funRateLimitPerMinute = 18;
    }

    /**
     * Точечный баланс стрима: не сбрасывает всю таблицу КД.
     * Срезает завышенные КД (паркур/инверсия), чинит ритуал 67, держит пейсинг.
     */
    public static void applyStreamTune(TwitchCraftConfig config) {
        snapOutrageousCooldowns(config);

        patch(config, "bomb", 60, 1);
        patch(config, "tnt", 24, 3);
        patch(config, "tntrain", 50, 1);
        patch(config, "meteor", 50, 1);
        patch(config, "lightning", 22, 3);
        patch(config, "creeper", 12, 5);
        patch(config, "creeperstorm", 40, 1);
        patch(config, "army", 45, 2);
        patch(config, "launch", 24, 3);
        patch(config, "lava", 28, 2);
        patch(config, "pit", 28, 2);
        patch(config, "fire", 16, 4);
        patch(config, "steal", 24, 2);
        patch(config, "drop", 22, 3);
        patch(config, "trap", 28, 2);
        patch(config, "parkour", 40, 1);
        patch(config, "invert", 28, 1);
        patch(config, "morph", 90, 1);
        patch(config, "hunt", 50, 1);
        patch(config, "ragdoll", 45, 1);
        patch(config, "sixseven", 2, 12);
        patch(config, "prison", 32, 1);
        patch(config, "fpv", 28, 2);
        patch(config, "titan", 50, 1);
        patch(config, "ant", 36, 2);
        patch(config, "orevein", 50, 2);
        patch(config, "vote", 40, 2);
        patch(config, "event", 28, 2);

        if (config.security != null) {
            config.security.antiSpamSeconds = 20;
            config.security.tntRateLimitPerMinute = Math.min(config.security.tntRateLimitPerMinute, 5);
            config.security.chaosRateLimitPerMinute = Math.min(Math.max(config.security.chaosRateLimitPerMinute, 6), 10);
        }
        if (config.death != null) {
            config.death.enabled = true;
            config.death.blockEvilCommands = true;
            config.death.deathsForPenalty = 5;
            config.death.deathWindowMinutes = 10;
            config.death.evilBlockMinutes = 5;
            config.death.respawnGraceSeconds = 20;
            config.death.evilPaceSeconds = 12;
        }
        if (config.stream != null) {
            config.stream.chatActivityCooldowns = true;
            config.stream.sessionLogEnabled = true;
            if (config.stream.bombSeconds < 12) config.stream.bombSeconds = 12;
            config.stream.unnamedPatronEnabled = true;
            config.stream.cipherRiddleEnabled = true;
            if (config.stream.unnamedPatronMinutes < 6) config.stream.unnamedPatronMinutes = 12;
            if (config.stream.cipherRiddleSeconds < 60 || config.stream.cipherRiddleSeconds > 150) {
                config.stream.cipherRiddleSeconds = 120;
            }
            if (config.stream.randomEventMinMinutes >= 5) {
                config.stream.randomEventMinMinutes = 4;
                config.stream.randomEventMaxMinutes = Math.max(7, config.stream.randomEventMaxMinutes);
                if (config.stream.randomEventMaxMinutes > 8) config.stream.randomEventMaxMinutes = 7;
            }
            config.stream.worldEncountersEnabled = true;
            if (config.stream.worldEncounterMinMinutes < 6) config.stream.worldEncounterMinMinutes = 8;
            if (config.stream.worldEncounterMaxMinutes < 8) config.stream.worldEncounterMaxMinutes = 14;
            config.stream.battleBarEnabled = false;
            config.stream.chaosBossBar = false;
        }
    }

    /** Паркур 810с и подобные следы от повторных пресетов. */
    public static void snapOutrageousCooldowns(TwitchCraftConfig config) {
        if (config == null) return;
        for (ChatGameCommand cmd : ChatGameCommand.values()) {
            if (cmd.getCategory() == ChatGameCommand.Category.MODERATOR) continue;
            if (cmd == ChatGameCommand.WITHER || cmd == ChatGameCommand.DRAGON
                    || cmd == ChatGameCommand.CHAT_PHANTOM) {
                continue;
            }
            int rec = cooldownFor(cmd);
            TwitchCraftConfig.CommandSettings s = config.getCommandSettings(cmd.getId(), rec);
            int cap = Math.max(120, rec * 2);
            if (s.cooldownSeconds > cap) {
                s.cooldownSeconds = rec;
            }
        }
    }

    private static void patch(TwitchCraftConfig config, String id, int cd, int maxPerMin) {
        ChatGameCommand cmd = ChatGameCommand.fromId(id).orElse(null);
        int fallback = cmd != null ? cmd.getDefaultCooldown() : cd;
        TwitchCraftConfig.CommandSettings s = config.getCommandSettings(id, fallback);
        s.cooldownSeconds = cd;
        if (maxPerMin > 0) s.maxUsesPerMinute = maxPerMin;
    }
}
