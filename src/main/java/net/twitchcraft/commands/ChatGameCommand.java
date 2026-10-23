package net.twitchcraft.commands;

import net.twitchcraft.effects.CampaignGuide;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.effects.ModPackEffects;
import net.twitchcraft.stream.DuoTether;
import net.twitchcraft.stream.MorphManager;
import net.twitchcraft.stream.SixSevenRitual;
import net.twitchcraft.stream.SizePrank;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public enum ChatGameCommand {
    // Basic
    LIGHTNING("lightning", 30, Category.EFFECT, EffectRunner::lightning, "молния", "lightning"),
    CREEPER("creeper", 15, Category.MOB, EffectRunner::spawnCreeper, "крипер", "creeper"),
    ZOMBIE("zombie", 15, Category.MOB, EffectRunner::spawnZombie, "зомби", "zombie"),
    LAUNCH("launch", 20, Category.MOVEMENT, EffectRunner::launch, "полет", "launch"),

    // Mobs
    SKELETON("skeleton", 15, Category.MOB, EffectRunner::spawnSkeleton, "скелет", "skeleton"),
    SPIDER("spider", 15, Category.MOB, EffectRunner::spawnSpider, "паук", "spider"),
    ENDERMAN("enderman", 20, Category.MOB, EffectRunner::spawnEnderman, "эндермен", "enderman"),
    WITCH("witch", 20, Category.MOB, EffectRunner::spawnWitch, "ведьма", "witch"),
    SLIME("slime", 15, Category.MOB, EffectRunner::spawnSlime, "слайм", "slime"),
    GHAST("ghast", 30, Category.MOB, EffectRunner::spawnGhast, "гаст", "ghast"),
    ARMY("army", 60, Category.MOB, EffectRunner::spawnArmy, "армия", "army"),
    WITHER("wither", 120, Category.MOB, EffectRunner::spawnWither, "визер", "wither"),
    DRAGON("dragon", 300, Category.MOB, EffectRunner::spawnDragon, "дракон", "dragon"),
    WOLF("wolf", 15, Category.MOB, EffectRunner::spawnWolf, "волк", "wolf"),
    IRON_GOLEM("irongolem", 30, Category.MOB, EffectRunner::spawnIronGolem, "голем", "irongolem"),

    // Chaos
    TNT("tnt", 20, Category.TNT, EffectRunner::spawnTnt, "tnt", "тнт"),
    TNT_RAIN("tntrain", 45, Category.TNT, EffectRunner::tntRain,
            "tnt_дождь", "tntrain", "tntдождь", "тнтдождь", "тнт_дождь"),
    LAVA("lava", 25, Category.CHAOS, EffectRunner::lava, "лава", "lava"),
    WATER("water", 20, Category.CHAOS, EffectRunner::water, "вода", "water"),
    PIT("pit", 30, Category.CHAOS, EffectRunner::pit, "яма", "pit"),
    METEOR("meteor", 45, Category.TNT, EffectRunner::meteor, "метеор", "meteor"),
    ANVIL("anvil", 25, Category.CHAOS, EffectRunner::anvil, "наковальня", "anvil"),
    STORM("storm", 40, Category.EFFECT, EffectRunner::storm,
            "молнии_везде", "storm", "молниивезде", "грозы"),
    FIRE("fire", 15, Category.CHAOS, EffectRunner::fire, "огонь", "fire"),
    CREEPER_STORM("creeperstorm", 60, Category.MOB, EffectRunner::creeperStorm,
            "крипер_шторм", "creeperstorm", "крипершторм"),

    // Inventory
    DROP("drop", 20, Category.INVENTORY, EffectRunner::drop, "дроп", "drop"),
    CLEAR("clear", 60, Category.INVENTORY, EffectRunner::clearInventory, "очистка", "clear"),
    DIAMOND("diamond", 30, Category.INVENTORY, EffectRunner::diamond, "алмаз", "diamond"),
    JUNK("junk", 15, Category.INVENTORY, EffectRunner::junk, "мусор", "junk"),
    CURSE("curse", 45, Category.INVENTORY, EffectRunner::curse, "проклятие", "curse"),
    SHUFFLE("shuffle", 25, Category.INVENTORY, EffectRunner::shuffleHotbar, "перемешать", "shuffle"),
    SWAP_INV("swapinv", 30, Category.INVENTORY, EffectRunner::swapInventories,
            "обменинв", "swapinv", "обменрюкзак", "инвентарьобмен"),
    EGG("egg", 10, Category.INVENTORY, EffectRunner::eggs, "яйца", "egg"),
    BOW("bow", 20, Category.INVENTORY, EffectRunner::bow, "лук", "bow"),
    ELYTRA("elytra", 40, Category.INVENTORY, EffectRunner::elytra,
            "элитры", "элитра", "elytra", "крылья"),

    // Movement
    TELEPORT("tp", 30, Category.MOVEMENT, EffectRunner::randomTeleport, "телепорт", "tp"),
    SPIN("spin", 25, Category.MOVEMENT, EffectRunner::spin, "крутилка", "spin"),
    BACKWARD("backward", 30, Category.MOVEMENT, EffectRunner::backward, "назад", "backward"),
    SWAP("swap", 45, Category.MOVEMENT, EffectRunner::swap, "обмен", "swap"),
    UP("up", 15, Category.MOVEMENT, EffectRunner::up, "вверх", "up"),
    DOWN("down", 15, Category.MOVEMENT, EffectRunner::down, "вниз", "down"),
    KNOCKBACK("knockback", 15, Category.MOVEMENT, EffectRunner::knockback, "отброс", "knockback"),

    // Effects
    HEAL("heal", 20, Category.EFFECT, EffectRunner::heal, "хил", "хилл", "лечение", "heal"),
    HURT("hurt", 15, Category.EFFECT, EffectRunner::hurt, "урон", "hurt"),
    BLIND("blind", 20, Category.EFFECT, EffectRunner::blind, "слепота", "blind"),
    SPEED("speed", 15, Category.EFFECT, EffectRunner::speed, "скорость", "speed"),
    SLOW("slow", 15, Category.EFFECT, EffectRunner::slow, "медлительность", "slow"),
    HUNGER("hunger", 15, Category.EFFECT, EffectRunner::hunger, "голод", "hunger"),
    REGEN("regen", 20, Category.EFFECT, EffectRunner::regen, "реген", "регенерация", "regen"),
    POISON("poison", 20, Category.EFFECT, EffectRunner::poison, "отравление", "poison"),
    STRENGTH("strength", 20, Category.EFFECT, EffectRunner::strength, "сила", "strength"),
    WEAKNESS("weakness", 15, Category.EFFECT, EffectRunner::weakness, "слабость", "weakness"),
    GLOW("glow", 10, Category.EFFECT, EffectRunner::glow, "свечение", "glow"),

    FEED("feed", 15, Category.EFFECT, EffectRunner::feed, "еда", "корм", "feed"),
    SAVE("save", 45, Category.EFFECT, EffectRunner::save, "спасение", "спаси", "save"),
    TOTEM("totem", 45, Category.INVENTORY, EffectRunner::totem, "тотем", "totem"),

    // Weather / time
    DAY("day", 30, Category.WORLD, EffectRunner::day, "день", "day"),
    NIGHT("night", 30, Category.WORLD, EffectRunner::night, "ночь", "night"),
    RAIN("rain", 30, Category.WORLD, EffectRunner::rain, "дождь", "rain"),
    CLEAR_WEATHER("clearweather", 30, Category.WORLD, EffectRunner::clearWeather, "ясно", "sunny"),
    THUNDER("thunder", 45, Category.WORLD, EffectRunner::thunder, "гром", "thunder"),
    NOON("noon", 30, Category.WORLD, EffectRunner::noon, "полдень", "noon"),

    // Fun
    RANDOM("random", 30, Category.FUN, EffectRunner::randomCommand, "рандом", "random"),
    COIN("coin", 10, Category.FUN, EffectRunner::coin, "монетка", "coin"),
    BOSS("boss", 90, Category.MOB, EffectRunner::boss, "босс", "boss"),
    FIREWORK("firework", 15, Category.FUN, EffectRunner::firework, "фейерверк", "firework"),
    SHEEP("sheep", 15, Category.MOB, EffectRunner::sheep, "овца", "sheep"),
    GIANT("giant", 30, Category.EFFECT, EffectRunner::giant, "гигант", "giant", "великан"),
    HELP("help", 15, Category.EFFECT, EffectRunner::help, "помощь", "хелп", "help"),
    ROULETTE("roulette", 45, Category.FUN, EffectRunner::roulette, "рулетка", "roulette"),
    VOTE("vote", 60, Category.FUN, EffectRunner::startVote, "голос", "vote"),

    // Новые фишки
    XP("xp", 20, Category.EFFECT, EffectRunner::xpRain, "опыт", "xp"),
    DOME("dome", 35, Category.CHAOS, EffectRunner::dome, "купол", "dome"),
    PRISON("prison", 40, Category.CHAOS, EffectRunner::prison, "тюрьма", "prison"),
    HYPE("hype", 25, Category.FUN, EffectRunner::hype, "хайп", "hype"),
    EVENT("event", 50, Category.FUN, EffectRunner::chaosEvent, "событие", "event"),
    CHICKEN("chicken", 15, Category.MOB, EffectRunner::chickenRain, "курица", "chicken"),
    FLASH("flash", 20, Category.EFFECT, EffectRunner::flash, "вспышка", "flash"),
    TRAP("trap", 30, Category.CHAOS, EffectRunner::trap, "ловушка", "trap"),
    VIEWER_EGG("vieweregg", 20, Category.FUN, EffectRunner::viewerEgg, "яйцо", "vieweregg"),
    CHAT_PHANTOM("phantom", 300, Category.MOB, EffectRunner::chatPhantom, "фантом", "phantom"),

    // Мемы / пранки
    FPV("fpv", 35, Category.MOB, EffectRunner::fpvKamikaze, "фпв", "fpv", "камикадзе"),
    TCK("tck", 40, Category.MOB, EffectRunner::spawnPigZombies, "тцк", "tck", "свинозомби"),
    NOVALNY("novalny", 25, Category.MOVEMENT, EffectRunner::novalnyDrop, "новальный", "novalny"),
    VOICE("voice", 20, Category.FUN, EffectRunner::voiceSay, "озвучка", "voice", "tts", "озвуч"),

    // Добро + смешное
    ANTIDOTE("antidote", 18, Category.EFFECT, EffectRunner::antidote, "антидот", "antidote", "дебафф"),
    GAPPLE("gapple", 25, Category.INVENTORY, EffectRunner::goldenApple, "яблоко", "gapple", "гапл"),
    TORCHES("torches", 20, Category.INVENTORY, EffectRunner::torches, "факелы", "torches", "факел", "torch"),
    BREAD("bread", 15, Category.INVENTORY, EffectRunner::bread, "хлеб", "bread"),
    PLANKS("planks", 15, Category.INVENTORY, EffectRunner::planks, "доски", "planks", "доска"),
    POTION("potion", 25, Category.INVENTORY, EffectRunner::randomPotion, "зелье", "potion", "зелья"),
    BUDDY("buddy", 30, Category.MOB, EffectRunner::buddyWolf, "друг", "buddy", "пёс", "пес"),
    RESPECT("respect", 15, Category.EFFECT, EffectRunner::respect, "респект", "respect", "уважение"),
    SLAP("slap", 12, Category.FUN, EffectRunner::slap, "тапок", "slap", "пощёчина", "пощечина"),
    CLOWN("clown", 20, Category.FUN, EffectRunner::clown, "клоун", "clown", "тыква"),
    FISH("fish", 15, Category.FUN, EffectRunner::fishRain, "рыба", "fish", "рыбка"),
    DANCE("dance", 20, Category.FUN, EffectRunner::danceParty, "танцы", "dance", "диско"),

    // Моды из stream-профиля (soft-dep)
    GOOSE("goose", 25, Category.MOB, ModPackEffects::goose, "гусь", "goose", "гуси", "honk"),
    CHAOS_CHICKENS("chaoschickens", 30, Category.MOB, ModPackEffects::chaosChickens,
            "хаоскуры", "chaoschickens", "курыхаос"),
    BRAINROT("brainrot", 18, Category.FUN, ModPackEffects::brainrot,
            "брейнрот", "brainrot", "доритос", "doritos"),
    PRIME("prime", 20, Category.FUN, ModPackEffects::prime, "прайм", "prime"),
    KVA("kva", 12, Category.FUN, ModPackEffects::kva, "ква", "kva", "croak", "кваква"),
    AMONGUS("amongus", 25, Category.MOB, ModPackEffects::amongUs, "амонг", "amongus", "sus"),
    OIA("oia", 25, Category.MOB, ModPackEffects::oiaCat, "оя", "oia", "oiacat"),
    VINE("vine", 20, Category.FUN, ModPackEffects::vineBoom, "вайн", "vine", "vineboom", "бум"),
    TROLL("troll", 20, Category.FUN, ModPackEffects::trollFace, "тролль", "troll", "trollface"),
    LUCKY("lucky", 35, Category.CHAOS, ModPackEffects::luckyBlock, "лаки", "lucky", "luckyblock"),
    UNLUCKY("unlucky", 35, Category.CHAOS, ModPackEffects::unluckyBlock,
            "неудача", "unlucky", "анлаки"),
    VERY_LUCKY("verylucky", 45, Category.CHAOS, ModPackEffects::veryLuckyBlock,
            "оченьлаки", "verylucky"),
    GOLDEN_HEN("goldenhen", 30, Category.MOB, ModPackEffects::goldenHen,
            "курочка", "goldenhen", "золотаякурица"),
    LUCKY_ROULETTE("luckyroulette", 40, Category.CHAOS, ModPackEffects::luckyRoulette,
            "лакирулетка", "luckyroulette", "лакиролл"),
    PRIME_RAVE("primerave", 35, Category.FUN, ModPackEffects::primeRave,
            "праймрейв", "primerave", "рейв"),
    PIGGYBACK("piggyback", 25, Category.FUN, ModPackEffects::piggyback,
            "наспину", "piggyback", "пиггибэк", "спина"),
    CARRY_ON("carryon", 25, Category.FUN, ModPackEffects::carryOn,
            "неси", "carryon", "carry", "несименя"),
    RAGDOLL("ragdoll", 14, Category.FUN, ModPackEffects::ragdoll,
            "тряпка", "рагдолл", "ragdoll", "труп", "flop"),
    VIEWER_ARMY("viewerarmy", 50, Category.MOB, ModPackEffects::viewerArmy,
            "армиязрителей", "viewerarmy", "армиячата", "чатarmy"),

    // Мини-игры чата (клипы)
    HUNT("hunt", 90, Category.FUN, EffectRunner::startHunt,
            "охота", "hunt", "хант"),
    BET("bet", 5, Category.FUN, EffectRunner::startBet,
            "ставка", "bet", "ставки", "прогноз"),
    BOMB("bomb", 90, Category.CHAOS, EffectRunner::startBomb,
            "бомба", "bomb", "мина"),
    DEFUSE("defuse", 8, Category.EFFECT, EffectRunner::defuseBomb,
            "разминируй", "defuse", "сапёр", "сапер", "обезвредь"),
    SIGN("sign", 12, Category.FUN, EffectRunner::placeSign,
            "табличка", "sign", "надпись"),
    STEAL("steal", 25, Category.INVENTORY, EffectRunner::stealItem,
            "украсть", "steal", "воровство"),
    JUMPSCARE("jumpscare", 20, Category.FUN, EffectRunner::jumpscare,
            "скример", "jumpscare", "бу"),
    KING("king", 40, Category.FUN, EffectRunner::crownKing,
            "король", "king", "царь"),
    DRUNK("drunk", 22, Category.EFFECT, EffectRunner::drunk,
            "пьяный", "drunk", "пьянь"),
    GRAVITY("gravity", 28, Category.MOVEMENT, EffectRunner::gravityFlip,
            "гравитация", "gravity"),
    HEIST("heist", 45, Category.INVENTORY, EffectRunner::heist,
            "ограбление", "heist"),

    LOCATE_VILLAGE("village", 75, Category.WORLD, CampaignGuide::village,
            "деревня", "village"),
    LOCATE_FORTRESS("fortress", 90, Category.WORLD, CampaignGuide::fortress,
            "крепость", "fortress", "адкрепость"),
    LOCATE_BASTION("bastion", 90, Category.WORLD, CampaignGuide::bastion,
            "бастион", "bastion", "цитадель"),
    LOCATE_STRONGHOLD("stronghold", 120, Category.WORLD, CampaignGuide::stronghold,
            "энд", "stronghold", "стронгхолд", "крепостькрая"),
    PORTAL_KIT("portalkit", 150, Category.INVENTORY, CampaignGuide::portalKit,
            "портал", "portalkit", "обсидиан"),
    FIRE_RES("fireres", 50, Category.EFFECT, CampaignGuide::fireRes,
            "огнестойкость", "fireres", "огнестой"),
    ENDER_EYE("endereye", 70, Category.INVENTORY, CampaignGuide::enderEye,
            "глаз", "endereye", "око"),
    MORPH("morph", 600, Category.FUN, MorphManager::fromCommand,
            "превратить", "morph", "морф"),
    ANT("ant", 90, Category.FUN, SizePrank::ant,
            "муравей", "ant", "кроха", "микро"),
    TITAN("titan", 150, Category.FUN, SizePrank::titan,
            "титан", "titan", "колосс"),
    SIX_SEVEN("sixseven", 8, Category.FUN, SixSevenRitual::cast,
            "сикссевен", "67", "sixseven", "six7"),
    TETHER("tether", 50, Category.FUN, DuoTether::fromCommand,
            "связь", "tether", "верёвка", "веревка"),
    ORE_VEIN("orevein", 60, Category.WORLD, EffectRunner::oreVein,
            "шахта", "жила", "руда", "orevein"),
    PARKOUR("parkour", 180, Category.FUN, EffectRunner::parkour,
            "паркур", "parkour", "дорожка"),
    INVERT("invert", 90, Category.MOVEMENT, EffectRunner::invertControls,
            "инверсия", "инверсия_управления", "инверсияуправления", "invert", "wasd", "реверс"),
    UPGRADE("upgrade", 40, Category.FUN, EffectRunner::upgradeItem,
            "апрейд", "апгрейд", "upgrade", "upgrader"),

    // Moderator
    STOP("stop", 0, Category.MODERATOR, EffectRunner::stop, "стоп", "stop"),
    STATUS("status", 10, Category.MODERATOR, EffectRunner::status, "статус", "status"),
    SKIP("skip", 0, Category.MODERATOR, EffectRunner::skip, "skip", "скип");

    public enum Category {
        MOB, TNT, CHAOS, INVENTORY, MOVEMENT, EFFECT, WORLD, FUN, MODERATOR;

        public int accent() {
            return switch (this) {
                case MOB -> 0xFF_FF_6B_6B;
                case TNT -> 0xFF_FF_8A_5B;
                case CHAOS -> 0xFF_FF_9F_43;
                case INVENTORY -> 0xFF_4E_CD_C4;
                case MOVEMENT -> 0xFF_74_C0_FC;
                case EFFECT -> 0xFF_FF_E0_66;
                case WORLD -> 0xFF_A8_D8_FF;
                case FUN -> 0xFF_FF_85_C0;
                case MODERATOR -> 0xFF_91_46_FF;
            };
        }
    }

    private static final Map<String, ChatGameCommand> ALIAS_MAP = new HashMap<>();

    static {
        for (ChatGameCommand cmd : values()) {
            for (String alias : cmd.aliases) {
                ALIAS_MAP.put(alias.toLowerCase(Locale.ROOT), cmd);
            }
            ALIAS_MAP.put(cmd.id.toLowerCase(Locale.ROOT), cmd);
        }
    }

    private final String id;
    private final int defaultCooldown;
    private final Category category;
    private final Consumer<CommandContext> action;
    private final String[] aliases;

    ChatGameCommand(String id, int defaultCooldown, Category category, Consumer<CommandContext> action, String... aliases) {
        this.id = id;
        this.defaultCooldown = defaultCooldown;
        this.category = category;
        this.action = action;
        this.aliases = aliases;
    }

    public String getId() {
        return id;
    }

    public int getDefaultCooldown() {
        return defaultCooldown;
    }

    public Category getCategory() {
        return category;
    }

    public void execute(CommandContext ctx) {
        action.accept(ctx);
    }

    public boolean isModeratorOnly() {
        return category == Category.MODERATOR && this != STATUS;
    }

    public static Optional<ChatGameCommand> fromMessage(String message) {
        if (message == null || !message.startsWith("!")) {
            return Optional.empty();
        }
        String body = message.substring(1).trim();
        int space = body.indexOf(' ');
        String name = (space >= 0 ? body.substring(0, space) : body).toLowerCase(Locale.ROOT);
        ChatGameCommand exact = ALIAS_MAP.get(name);
        if (exact != null) return Optional.of(exact);
        return Optional.ofNullable(CommandTypo.resolve(name, ALIAS_MAP));
    }

    public static Optional<ChatGameCommand> fromId(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(ALIAS_MAP.get(id.toLowerCase(Locale.ROOT)));
    }

    public static ChatGameCommand[] funPool() {
        return funPool(null);
    }

    public static ChatGameCommand[] funPool(net.twitchcraft.config.TwitchCraftConfig config) {
        return Arrays.stream(values())
                .filter(c -> c.category != Category.MODERATOR)
                .filter(c -> c != RANDOM && c != ROULETTE && c != VOTE && c != BET
                        && c != BOMB && c != DEFUSE && c != HUNT && c != MORPH
                        && c != SIGN && c != VOICE && c != WITHER && c != DRAGON
                        && c != CHAT_PHANTOM && c != BOSS && c != CLEAR
                        && c != PARKOUR && c != TETHER && c != KING && c != EVENT
                        && c != UPGRADE)
                .filter(c -> config == null
                        || config.getCommandSettings(c.getId(), c.getDefaultCooldown()).enabled)
                .toArray(ChatGameCommand[]::new);
    }

    public static ChatGameCommand[] riddlePool(net.twitchcraft.config.TwitchCraftConfig config) {
        return Arrays.stream(funPool(config))
                .filter(c -> c != TNT_RAIN && c != METEOR && c != ARMY && c != CREEPER_STORM
                        && c != LAVA && c != PIT && c != LAUNCH && c != FPV && c != TRAP
                        && c != STEAL && c != HEIST && c != SWAP && c != CLEAR
                        && c != RAGDOLL && c != INVERT && c != ANT && c != TITAN)
                .toArray(ChatGameCommand[]::new);
    }

    public boolean matchesQuery(String query) {
        if (query == null || query.isBlank()) return false;
        String q = query.toLowerCase(Locale.ROOT).replace("!", "").trim();
        if (q.isEmpty()) return false;
        if (id.toLowerCase(Locale.ROOT).contains(q)) return true;
        if (getRussianName().toLowerCase(Locale.ROOT).contains(q)) return true;
        for (String alias : aliases) {
            if (alias.toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }

    /** Первый кириллический алиас — для HUD и чата. */
    public String getRussianName() {
        for (String alias : aliases) {
            if (containsCyrillic(alias)) return alias;
        }
        return aliases.length > 0 ? aliases[0] : id;
    }

    public String getCategoryRu() {
        return switch (category) {
            case MOB -> "Мобы";
            case TNT -> "TNT";
            case CHAOS -> "Хаос";
            case INVENTORY -> "Инвентарь";
            case MOVEMENT -> "Движение";
            case EFFECT -> "Эффекты";
            case WORLD -> "Мир";
            case FUN -> "Шоу";
            case MODERATOR -> "Модер";
        };
    }

    public static ChatGameCommand[] byCategory(Category cat) {
        return Arrays.stream(values())
                .filter(c -> c.category == cat && c.category != Category.MODERATOR)
                .toArray(ChatGameCommand[]::new);
    }

    private static boolean containsCyrillic(String s) {
        for (char c : s.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC) return true;
        }
        return false;
    }
}
