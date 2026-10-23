package net.twitchcraft.client;

/** Подсказки для вкладки Twitch в настройках. */
public final class TwitchSetupHints {
    public static final String INTRO_1 = "§7Каждый стример вводит §dсвои§7 данные на §dсвоём ПК§7. Начни с вкладки §f🚀 Старт§7.";
    public static final String INTRO_2 = "§7После заполнения нажми §aСохранить §7(справа внизу) и §f/twitchcraft connect";

    public static final String MC_LABEL = "§fВаш ник Minecraft";
    public static final String MC_HINT = "§8Определяется автоматически из сессии";

    public static final String CH_LABEL = "§fTwitch-канал";
    public static final String CH_HINT = "§8Ник канала §7без §8#  §7(напр. §dsmirty§7)";
    public static final String CH_PLACEHOLDER = "ник_канала";

    public static final String OAUTH_URL = "https://twitchapps.com/tmi";
    public static final String DA_URL = "https://donatex.gg/";

    public static final String OAUTH_LABEL = "§fOAuth-токен Twitch (IRC)";
    public static final HintWithLink OAUTH_HINT = new HintWithLink(
            "§7Получить: ", "§n§btwitchapps.com/tmi", "", OAUTH_URL);
    public static final String OAUTH_HINT2 = "§7Войти через Twitch → скопируй §foauth:...";
    public static final String OAUTH_PLACEHOLDER = "oauth:xxxxxxxxxxxxxxxxxxxx";

    public static final String EVENTS_LABEL = "§fBits / Subs / Raids";
    public static final String EVENTS_HINT = "§8IRC: bits→яйцо · sub→tnt · raid→армия · §7нужен OAuth выше";

    public static final String DA_LABEL = "§fТокен DonateX";
    public static final HintWithLink DA_HINT = new HintWithLink(
            "§7Кабинет: ", "§n§bdonatex.gg", " §7→ Настройки → Api", DA_URL);
    public static final String DA_HINT2 = "§7Скопируй §fexternal token§7, не JWT из браузера";
    public static final String DA_PLACEHOLDER = "external token DonateX...";

    public record HintWithLink(String before, String linkText, String after, String url) {}

    private TwitchSetupHints() {}
}
