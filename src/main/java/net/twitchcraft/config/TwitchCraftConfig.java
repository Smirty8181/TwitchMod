package net.twitchcraft.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TwitchCraftConfig {
    public TwitchSettings twitch = new TwitchSettings();
    public String targetPlayer = "";
    /** Несколько стримеров — приоритет над targetPlayer (или "nick1,nick2" в targetPlayer). */
    public List<String> targetPlayers = new ArrayList<>();
    /** Профили стримеров (дуо): ник MC + Twitch + DonateX. */
    public List<StreamerProfile> streamers = new ArrayList<>();
    public SecuritySettings security = new SecuritySettings();
    public FeedbackSettings feedback = new FeedbackSettings();
    public ScoreboardSettings scoreboard = new ScoreboardSettings();
    public UiSettings ui = new UiSettings();
    public DonationSettings donations = new DonationSettings();
    public StreamSettings stream = new StreamSettings();
    public Map<String, CommandSettings> commands = new HashMap<>();
    /** Команды, созданные в игре (клавиша U). */
    public List<CustomCommandDefinition> customCommands = new ArrayList<>();
    /** Общие кулдауны по пакам категорий. */
    public CooldownPackSettings cooldownPacks = new CooldownPackSettings();
    /** Смерти стримера, координаты, штраф чата. */
    public DeathSettings death = new DeathSettings();
    /** Полная настройка HUD: позиции, цвета, секции. */
    public HudSettings hud = new HudSettings();
    /** Однократно применить пресет баланса (~100 зрителей). Сбрось false для повторного применения. */
    public boolean balancePresetApplied = false;
    /** Баланс КД под хаос. Поднимай, чтобы перезалить таблицу кулдаунов. */
    public int commandBalanceVersion = 0;
    /** small | medium | large | mega | duo */
    public String audiencePreset = "medium";
    /** vanilla | hardcore | modpack — быстрый профиль стрима */
    public String streamGenre = "vanilla";

    public static class TwitchSettings {
        public String channel = "";
        public String oauthToken = "";
        public boolean autoConnect = true;
        /** Bits / subs / raids через IRC. */
        public boolean eventSubEnabled = true;
        public int bitsMinAmount = 100;
    }

    public static class SecuritySettings {
        public boolean whitelistEnabled = false;
        public List<String> whitelist = new ArrayList<>();
        public List<String> moderators = new ArrayList<>();
        public int antiSpamSeconds = 20;
        public boolean disableInHardcore = true;
        public int mobSpawnRateLimitPerMinute = 18;
        public int tntRateLimitPerMinute = 8;
        public int chaosRateLimitPerMinute = 12;
        public int effectRateLimitPerMinute = 16;
        public int movementRateLimitPerMinute = 14;
        public int inventoryRateLimitPerMinute = 12;
        public int worldRateLimitPerMinute = 8;
        public int funRateLimitPerMinute = 16;
    }

    public static class FeedbackSettings {
        public boolean announceInTwitchChat = false;
        public boolean showTitleInGame = true;
        public boolean showActionBar = true;
        public String chatMessageFormat = "⚡ {user} вызвал {command}! (КД {cooldown}с)";
        public String donationMessageFormat = "🔥 {user} задонатил {amount}{currency} → {command}!";
    }

    public static class ScoreboardSettings {
        public boolean enabled = true;
        public boolean showCommands = true;
        public boolean showDonations = true;
        public boolean showStats = true;
        public boolean showGraveyard = true;
        public int maxCommandLines = 32;
        public int pageRotateSeconds = 7;
        /** Всегда показывать на sidebar (не листается с категориями). */
        public boolean showFeaturedCommand = true;
        /** id команды, напр. lightning → !молния */
        public String featuredCommandId = "lightning";
        /** Лента «зритель → команда» внизу экрана. */
        public boolean showCommandFeed = true;
    }

    public static class UiSettings {
        /** anime | dark | purple | red | white | green | neon */
        public String theme = "anime";
        /** Масштаб HUD-оверлеев (0.75–1.35). */
        public float hudScale = 1.0f;
        /** top | bottom */
        public String donationPosition = "top";
        /** bottom | left */
        public String feedPosition = "bottom";
        /** Компактный sidebar — только ключевые строки. */
        public boolean sidebarCompact = false;
        /** Лёгкие UI-звуки: лента / донат / рулетка / смерть. */
        public boolean hudSounds = true;
    }

    public static class DonationSettings {
        public boolean enabled = false;
        public String accessToken = "";
        public int pollIntervalSeconds = 6;
        public boolean bypassCooldown = true;
        public List<DonationTier> tiers = new ArrayList<>();
    }

    public static class DonationTier {
        public int minAmount = 100;
        public String currency = "RUB";
        public String command = "tntrain";
        public String label = "TNT-дождь";
        /** Рулетка на экране вместо мгновенной команды. */
        public boolean wheel = false;

        public DonationTier() {}

        public DonationTier(int minAmount, String command, String label) {
            this.minAmount = minAmount;
            this.command = command;
            this.label = label;
        }
    }

    public static class StreamSettings {
        public boolean namedMobs = true;
        public boolean ghostMobsEnabled = true;
        public boolean revengeOnDeath = true;
        public int revengeDelaySeconds = 5;
        public boolean courtEnabled = true;
        public int courtDurationSeconds = 30;
        public int courtPenaltyMultiplier = 2;
        public int courtPenaltyMinutes = 2;
        public boolean chaosMeterEnabled = true;
        public int chaosMeterMax = 50;
        public String chaosMeterCommand = "army";
        public int voteDurationSeconds = 45;
        public boolean chaosBossBar = false;
        public boolean battleBarEnabled = false;
        public int battleMaxHp = 100;
        public boolean randomEventsEnabled = true;
        public int randomEventMinMinutes = 3;
        public int randomEventMaxMinutes = 6;
        /** Режим «2 стримера на локальном сервере». */
        public boolean duoStreamMode = false;
        /** Хаусмод — все КД и лимиты отключены. */
        public boolean houseMode = false;
        /** Мини-игры чата. */
        public int huntSeconds = 60;
        public int betSeconds = 90;
        public int bombSeconds = 12;
        public int kingSeconds = 90;
        /** Кубик стримера: щит на N минут при 6. */
        public int diceShieldMinutes = 1;
        public int challengeSeconds = 25;
        public boolean milestoneNarratorEnabled = true;
        public boolean sizePranksEnabled = true;
        public int antSeconds = 30;
        public int titanSeconds = 15;
        public boolean sixSevenEnabled = true;
        /** КД команд плывут от активности чата (EMA), а не от скачков онлайна. */
        public boolean chatActivityCooldowns = true;
        /** Писать дневник команд/смертей/донатов в logs/twitchcraft. */
        public boolean sessionLogEnabled = true;
        public boolean unnamedPatronEnabled = true;
        public int unnamedPatronMinutes = 12;
        public boolean cipherRiddleEnabled = true;
        public int cipherRiddleSeconds = 120;
        /** Груз / тотем / гость в мире, не команда чата. */
        public boolean worldEncountersEnabled = true;
        public int worldEncounterMinMinutes = 8;
        public int worldEncounterMaxMinutes = 14;
    }

    public static class CommandSettings {
        public boolean enabled = true;
        public int cooldownSeconds = 15;
        /** Макс. использований в минуту (глобально). 0 = без отдельного лимита. */
        public int maxUsesPerMinute = 0;

        public CommandSettings() {}

        public CommandSettings(boolean enabled, int cooldownSeconds) {
            this.enabled = enabled;
            this.cooldownSeconds = cooldownSeconds;
        }
    }

    public CommandSettings getCommandSettings(String id, int defaultCooldown) {
        return commands.computeIfAbsent(id, k -> new CommandSettings(true, defaultCooldown));
    }

    public static class CooldownPackSettings {
        /** Общий КД на весь пак (хаос, добро, мобы…). */
        public boolean sharedPackCooldowns = false;
        public int goodPackSeconds = 12;
        public int chaosPackSeconds = 16;
        public int mobPackSeconds = 10;
        public int tntPackSeconds = 18;
        public int effectPackSeconds = 8;
        public int movementPackSeconds = 10;
        public int inventoryPackSeconds = 12;
        public int worldPackSeconds = 18;
        public int funPackSeconds = 8;
    }

    public static class DeathSettings {
        public boolean enabled = true;
        /** Смертей за окно → штраф. */
        public int deathsForPenalty = 5;
        public int deathWindowMinutes = 10;
        public int evilBlockMinutes = 5;
        public boolean blockEvilCommands = true;
        public boolean showDeathCounter = true;
        /** overlay | actionbar | title | sidebar | all */
        public String coordsDisplay = "overlay";
        public boolean showCoordinates = true;
        public boolean showDimension = true;
        public boolean showDeathToast = true;
        /** Закрепить последние координаты смерти на экране постоянно. */
        public boolean pinCoordinates = false;
        /** Стрелка на HUD в сторону точки смерти. */
        public boolean showDeathArrow = true;
        /** Секунд без урона чата после смерти (респавн). */
        public int respawnGraceSeconds = 20;
        /** Мин. пауза между любыми командами с уроном на одного стримера. */
        public int evilPaceSeconds = 12;
    }

    /** Настройки одного HUD-элемента: позиция, масштаб, цвет. */
    public static class HudElementConfig {
        public boolean enabled = true;
        /** TOP_LEFT | TOP_CENTER | TOP_RIGHT | CENTER_LEFT | CENTER | CENTER_RIGHT | BOTTOM_LEFT | BOTTOM_CENTER | BOTTOM_RIGHT */
        public String anchor = "TOP_LEFT";
        public int offsetX = 6;
        public int offsetY = 8;
        /** Множитель поверх globalScale (0.5–2.0). */
        public float scale = 1.0f;
        public float opacity = 1.0f;
        /** 0 = цвет темы. */
        public int accentColor = 0;
        public int goodColor = 0;
        public int evilColor = 0;
    }

    /** Что показывать в Sidebar. */
    public static class HudSidebarSections {
        public boolean title = true;
        public boolean lastCommand = true;
        public boolean stats = true;
        public boolean chaos = true;
        public boolean penalty = true;
        public boolean deaths = true;
        public boolean evilBlock = true;
        public boolean deathCoords = true;
        public boolean donations = true;
        public boolean commands = true;
        public boolean graveyard = true;
        public boolean statusLine = true;
        public boolean houseMode = true;
        public boolean compact = false;
        public boolean featuredCommand = true;
        public boolean showTopViewers = true;
        public boolean showStreaks = true;
        public int maxCommandLines = 32;
        public int pageRotateSeconds = 7;
        public String featuredCommandId = "lightning";
    }

    /** Что показывать в ленте команд. */
    public static class HudCommandFeedSections {
        public boolean avatars = true;
        public boolean badges = true;
        public boolean progressBar = true;
        public boolean screenFx = true;
        public boolean showStreaks = true;
        public int maxEntries = 3;
    }

    public static class HudSettings {
        public float globalScale = 1.0f;
        public HudElementConfig sidebar = HudDefaults.sidebar();
        public HudElementConfig commandFeed = HudDefaults.commandFeed();
        public HudElementConfig commandDeny = HudDefaults.commandDeny();
        public HudElementConfig deathToast = HudDefaults.deathToast();
        public HudElementConfig pinnedDeathCoords = HudDefaults.pinnedDeathCoords();
        public HudElementConfig donationToast = HudDefaults.donationToast();
        public HudElementConfig comboChain = HudDefaults.comboChain();
        public HudElementConfig donationWheel = HudDefaults.donationWheel();
        public HudElementConfig commandFx = HudDefaults.commandFx();
        public HudElementConfig donationFx = HudDefaults.donationFx();
        public HudElementConfig duoScore = HudDefaults.duoScore();
        public HudSidebarSections sidebarContent = new HudSidebarSections();
        public HudCommandFeedSections commandFeedContent = new HudCommandFeedSections();
    }
}
