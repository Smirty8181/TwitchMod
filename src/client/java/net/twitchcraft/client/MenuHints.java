package net.twitchcraft.client;

/** Подсказки навигации и тексты для меню TwitchCraft. */
public final class MenuHints {
    public static final String SETTINGS_FOOTER =
            "§8ESC §7выход · §aСохранить §7справа · §8R §7правила · §8U §7команды · §8Shift §7±10";
    public static final String RULES_FOOTER =
            "§8ESC §7закрыть · §8K §7настройки · §8R §7правила · §8Ctrl+колёсико §7масштаб";
    public static final String HUD_FOOTER =
            "§8ESC §7назад · §8Shift §7±10 · §8колёсико §7прокрутка";
    public static final String COMMANDS_FOOTER =
            "§8ESC §7назад · §8U §7конструктор · §8Shift §7±10";

    public static String tabSubtitle(int tab) {
        return switch (tab) {
            case 0 -> "Мастер, шпаргалка и папка для напарника";
            case 1 -> "Канал Twitch, OAuth и DonateX";
            case 2 -> "Хаос, суд, смерти и штрафы чата";
            case 3 -> "Тема интерфейса и конструктор HUD";
            case 4 -> "Профиль стрима, аудитория, «как у напарника»";
            case 5 -> "Белый список, хардкор и модераторы";
            case 6 -> "Куда показывать результат команд зрителей";
            case 7 -> "Суммы донатов, уровни и рулетка";
            case 8 -> "Включение и КД · поиск сверху: бомба";
            default -> "";
        };
    }

    public static String guideWelcome() {
        return "§7K §8настройки · §7R §8правила · §7I §8тесты · §7U §8команды · §7P §8статистика · §aСохранить §8справа";
    }

    public static String step(boolean done, int num, String text) {
        return (done ? "§a✓ §f" : "§7○ §8") + num + ". §7" + text;
    }

    private MenuHints() {}
}
