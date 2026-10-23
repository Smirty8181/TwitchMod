package net.twitchcraft.rules;

import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.GoodCommands;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;
import net.twitchcraft.stream.HouseMode;

import java.util.ArrayList;
import java.util.List;

public final class RulesContent {
    public sealed interface Block {
        record Header(String text) implements Block {}
        record Paragraph(String text) implements Block {}
        record Spacer(int px) implements Block {}
        record Divider() implements Block {}
        record CommandCard(String command, String description, int cooldownSec) implements Block {}
        record FeatureCard(String icon, String title, String description) implements Block {}
        record ServerCmd(String command, String description) implements Block {}
    }

    public record Page(String id, String title, String icon, String subtitle, int accentColor, List<Block> blocks) {}

    private RulesContent() {}

    public static List<Page> allPages() {
        List<Page> pages = new ArrayList<>();
        pages.add(overview());
        pages.add(goodCommandsPage());
        for (ChatGameCommand.Category cat : ChatGameCommand.Category.values()) {
            if (cat == ChatGameCommand.Category.MODERATOR) continue;
            pages.add(commandsPage(cat));
        }
        pages.add(mechanicsPage());
        pages.add(moderatorPage());
        return pages;
    }

    private static Page overview() {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block.Paragraph("§7TwitchCraft — зрители управляют вашим миром через Twitch-чат."));
        blocks.add(new Block.Paragraph("§7Создатель мода: §d§lSmirty"));
        blocks.add(new Block.Spacer(4));
        blocks.add(new Block.FeatureCard("🎮", "Для стримера",
                "§f/rules §7в чате Minecraft — эта справка. §7Зрители её не видят."));
        blocks.add(new Block.FeatureCard("💬", "Для зрителей",
                "Пишут в §dTwitch-чат§7: §e!команда §7→ эффект на стримера\n"
                        + "§a+ добро: §7" + GoodCommands.sidebarHint()));
        blocks.add(new Block.FeatureCard("🥚", "Яйцо зрителя",
                "§e!яйцо §7или bits → предмет §d[ник]§7. ПКМ — ghost-моб"));
        blocks.add(new Block.FeatureCard("💰", "Донаты — эксклюзив",
                "§650₽ §8→ §dСпонсорская гроза §7· §675₽ §8→ §dЗолотая лихорадка\n"
                        + "§6100₽ §8→ §dДенежный дождь §7· §6150₽ §8→ §dДиско-хаос\n"
                        + "§6200₽ §8→ §dЛегион §7· §6300₽ §8→ §dСундук §7· §6500₽ §8→ §dРулетка\n"
                        + "§6750₽ §8→ §dРейд §7· §61000₽ §8→ §dПризрак чата §7(босс 120 HP)"));
        blocks.add(new Block.FeatureCard("👻", "Призрак чата",
                "§61000₽ §7→ §dбосс-фантом §c120 HP§7, свечение, ярость <50%"));
        blocks.add(new Block.Spacer(4));
        blocks.add(new Block.Paragraph("§71. §6K §7→ Twitch: канал + OAuth → §aСохранить §7→ §f/twitchcraft connect"));
        blocks.add(new Block.Paragraph("§72. §6K §7→ HUD: настрой сайдбар и ленту команд"));
        blocks.add(new Block.Paragraph("§73. Зрители: §c!молния §7или §a!хил §7— зло и добро"));
        blocks.add(new Block.Paragraph("§74. Сайдбар — КД, статистика, топ зрителей"));
        blocks.add(new Block.FeatureCard("⚡", "Комбо «ГРОЗА»",
                "§e3× !молния §7за 15 сек → §b§lГРОЗА §7(5 молний по стримеру)"));
        blocks.add(new Block.FeatureCard("🎰", "Рулетка доната",
                "§c500₽+ §7— колесо на экране, затем мега-эффект (не как !команда)"));
        blocks.add(new Block.FeatureCard("⚡", "Любимая команда",
                "§e!молния §7— молния бьёт стримера\n§7Всегда на scoreboard со звёздочкой ★"));
        blocks.add(new Block.FeatureCard("🎉", "Хаусмод",
                "§7Только стример: §f/twitchcraft haus §7или §6K §7→ Стрим\n§7§dВесь чат §7без КД, антиспама и лимитов"));
        blocks.add(new Block.Spacer(4));
        blocks.add(new Block.FeatureCard("📊", "Сайдбар",
                "Команды, добро/зло, суд, кладбище ☠, битва чата"));
        blocks.add(new Block.FeatureCard("⚔", "Полоска босса",
                "§cЧат vs Стример §7— HP-битва от команд зрителей"));
        blocks.add(new Block.FeatureCard("🔥", "Хаос",
                "Полоска заполняется → автоматическое мега-событие"));
        blocks.add(new Block.FeatureCard("🎯", "Охота",
                "§e!охота §7— 60с. Смерть стримера = победа охотника"));
        blocks.add(new Block.FeatureCard("💰", "Ставки",
                "§e!ставка смерть §7/ §e!ставка жив §7— угадай, умрёт ли стример"));
        blocks.add(new Block.FeatureCard("💣", "Бомба",
                "§e!бомба §7— горячая картошка в руки. Кинь напарнику. Чат: §a!разминируй"));
        blocks.add(new Block.FeatureCard("✦", "Заряды стримера",
                "Тратятся из меню §6I§7. Заряд: моб чата, алмаз, охота, 8 мин жив, хилы, жертва"));
        blocks.add(new Block.FeatureCard("🎲", "Кубик мира",
                "Только мир, каждые 8–14 мин. Ни чат, ни стример не кидают и не влияют"));
        blocks.add(new Block.FeatureCard("✦", "Находки мира",
                "Сами: груз (бочка), тотем (ПКМ), гость-свинья. Не команда чата. Дуо бегут к метке"));
        blocks.add(new Block.FeatureCard("🗺", "Чат-навигатор",
                "§e!деревня !крепость !бастион !энд !портал !глаз §7· огнестойкость только в Аду"));
        blocks.add(new Block.FeatureCard("🦇", "Морф",
                "§e!превратитьлетучаямышь §7— 60с, раз в 10 мин. Сначала помоги стримеру"));
        blocks.add(new Block.FeatureCard("🐜", "Размер",
                "§e!муравей §730с · §e!титан §715с — настоящий scale, дом больше не дом"));
        blocks.add(new Block.FeatureCard("67", "Сикс севен",
                "§e!сикссевен §7или §e!67 §7— семь §eразных ников §7за 45с = муравей или титан"));
        blocks.add(new Block.FeatureCard("🔗", "Связь",
                "§e!связь §7— 45с. Урон и шаг общие. Смерть бьёт напарника"));
        blocks.add(new Block.FeatureCard("🎒", "Обмен инвентарями",
                "§e!обменинв §7— рюкзаки, броня и хотбар меняются с напарником"));
        blocks.add(new Block.FeatureCard("🛡", "Защита спавна",
                "Спавн мира и кровать со спавнпоинтом нельзя сломать, взорвать или снести чатом"));
        blocks.add(new Block.Spacer(4));
        blocks.add(new Block.Paragraph("§8Клавиши: §7§lK§r§8 настройки · §7§lR§r§8 правила · §7§lI§r§8 тесты · §7§lU§r§8 команды · §7§lP§r§8 статистика · §7колёсико§r§8 пинг"));
        return new Page("overview", "Обзор", "◆", "Справочник стримера", 0xFF_91_46_FF, blocks);
    }

    private static Page goodCommandsPage() {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block.Paragraph("§7Зрители могут §aпомогать §7стримеру — это даёт §a+добро §7на scoreboard."));
        blocks.add(new Block.Spacer(4));
        for (ChatGameCommand cmd : GoodCommands.all()) {
            blocks.add(new Block.CommandCard(
                    "!" + cmd.getRussianName(),
                    describe(cmd),
                    liveCooldown(cmd)));
        }
        blocks.add(new Block.Spacer(6));
        blocks.add(new Block.Paragraph("§8Совет: расскажи в чате, что §a!хил §7и §a!помощь §7— это поддержка стрима"));
        return new Page("good", "Добро", "♥", "Положительные команды", 0xFF_55_CC_55, blocks);
    }

    private static Page commandsPage(ChatGameCommand.Category category) {
        List<Block> blocks = new ArrayList<>();
        ChatGameCommand[] cmds = ChatGameCommand.byCategory(category);
        String title = cmds.length > 0 ? cmds[0].getCategoryRu() : category.name();
        blocks.add(new Block.Paragraph("§7Зрители пишут в Twitch-чат. КД — в §6K §7→ §fКоманды §7(или §6K §7→ §fБаланс§7)."));
        if (HouseMode.isActive()) {
            blocks.add(new Block.Paragraph("§d§lХаусмод активен §7— все КД отключены."));
        }
        blocks.add(new Block.Spacer(4));
        for (ChatGameCommand cmd : cmds) {
            blocks.add(new Block.CommandCard(
                    "!" + cmd.getRussianName(),
                    describe(cmd),
                    liveCooldown(cmd)));
        }
        return new Page(category.name().toLowerCase(), title, categoryIcon(category), title, category.accent(), blocks);
    }

    private static Page mechanicsPage() {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block.Header("§d§lИнтерактив для клипов"));
        blocks.add(new Block.Spacer(4));
        blocks.add(new Block.FeatureCard("⚖", "Суд чата",
                "Смерть стримера → 30 сек: §c!виноват §7/ §a!невиновен\n§7Виноват = армия · Невиновен = КД ×2 на 2 мин"));
        blocks.add(new Block.FeatureCard("👻", "Призраки зрителей",
                "Моб с ником §c[Viewer]§7. Убил своего → +1 добро\nУбил стримера → запись в кладбище на scoreboard"));
        blocks.add(new Block.FeatureCard("🎲", "Случайные события",
                "Каждые 3–6 мин: метеор, армия, TNT-дождь и др."));
        blocks.add(new Block.FeatureCard("🎯", "Охота",
                "§e!охота §7— 60 сек. Стример светится. Смерть = победа охотника,\nвыжил = овцы с ником лузера"));
        blocks.add(new Block.FeatureCard("💰", "Ставки на смерть",
                "§e!ставка смерть §7или §e!ставка жив §7— 90 сек, без КД.\nПобедители объявляются в чат и на экране"));
        blocks.add(new Block.FeatureCard("💣", "Бомба",
                "§c!бомба §7— картошка в руки, ~12с. У кого в инвентаре — взрыв.\n"
                        + "Выбросить не спасает, пока напарник не подберёт. Чат: §a!разминируй"));
        blocks.add(new Block.FeatureCard("👑", "Король чата",
                "§e!король §7— алай с короной. Следующая моб-команда короля ×2"));
        blocks.add(new Block.FeatureCard("✦", "Покровитель",
                "Тихо выбирается зритель. Его команды сильнее. Если стример напишет ник в чат майн/Twitch или нажмёт «я проговорился» — покровитель сгорает"));
        blocks.add(new Block.FeatureCard("?", "Загадка",
                "На экране §e!к?????§7. Напиши эту команду — получишь усиление на 90с"));
        blocks.add(new Block.FeatureCard("⬆", "Апгрейд",
                "§e!апрейд §7— меню как upgrader. Вложи кирку: железо → алмаз. "
                        + "Кольцо шанса крутится. Победа — новый предмет, провал — сгорел"));
        blocks.add(new Block.FeatureCard("✦", "Заряды",
                "Макс 3. Дают: убийство моба зрителя, алмаз/обломок, выжить охоту,\n"
                        + "4 хила чата, 8 мин без смерти, жертва и контракт — из меню I"));
        blocks.add(new Block.FeatureCard("🎲", "Кубик мира",
                "Каждые 8–14 мин сам. Честный 1–6. Никто не может бросить или подкрутить"));
        blocks.add(new Block.FeatureCard("✦", "Находки мира",
                "Каждые 8–14 мин рядом спавнится груз, тотем или гость-свинья с ником из чата.\n"
                        + "Не !команда. Бочка — лут (иногда ловушка). Тотем — ПКМ/встань = бафф. Свинья — ПКМ = посылка"));
        blocks.add(new Block.FeatureCard("🗺", "Прохождение с чатом",
                "§e!деревня !крепость !бастион !энд §7— направление и метры.\n"
                        + "§e!портал §78 обсидиана + огниво · §e!огнестойкость §7только в Аду · §e!глаз\n"
                        + "Первый Ад/Край — глава на экране. !дракон только в Крае"));
        blocks.add(new Block.FeatureCard("🦇", "Превращение",
                "§e!превратитьлетучаямышь §7/ §e!превратить крипер §7— 60с, КД 10 мин.\n"
                        + "Открывается после одного добра от этого зрителя"));
        blocks.add(new Block.FeatureCard("👑", "Ангел кармы",
                "3 добра или !король — ангел на сайдбаре. Только чат и озвучка, без NPC в мире"));
        blocks.add(new Block.FeatureCard("🎤", "Диктор вех",
                "Первый алмаз → «СИКС СЕВЕН АЛМАЗЫ». Ад → «Охаешечки Датабаёшечки».\n"
                        + "Железо, обломок, Край, дракон, смерть — текст в чат + TTS, без спавна"));
        blocks.add(new Block.FeatureCard("🐜", "Муравей / титан",
                "§e!муравей §7= рост 0.16 на 30с. §e!титан §7= 3.5× на 15с. Настоящий хитбокс"));
        blocks.add(new Block.FeatureCard("67", "Ритуал 67",
                "Семь §eразных ников §7пишут §e!67 §7за 45с. Финал — муравей или титан"));
        blocks.add(new Block.FeatureCard("🔗", "Связь",
                "§e!связь §7— 45с. Общий урон и верёвка по расстоянию. Смерть бьёт второго"));
        blocks.add(new Block.FeatureCard("🛡", "Защита спавна",
                "Радиус 8 у мирового спавна + кровать/якорь стримера. Ломать, TNT и !яма не берут"));
        blocks.add(new Block.FeatureCard("🧹", "Лимит мобов чата",
                "Макс 24 обычных моба чата. Через 90с рассеиваются. Свечение только у 8 ближайших с именем. Визер/дракон/фантом живут 8 мин"));
        blocks.add(new Block.FeatureCard("📍", "Пинг напарника",
                "Средняя кнопка мыши — метка 20с. Стрелка и столб частиц. Куда смотришь, туда пинг"));
        blocks.add(new Block.FeatureCard("🎒", "Обмен инвентарями",
                "§e!обменинв §7— рюкзаки, броня и хотбар с напарником. КД 30с. Не урон"));
        blocks.add(new Block.FeatureCard("🛏", "Сон за двоих",
                "Один стример лег спать — утро у обоих, даже если второй в шахте. Без команды"));
        blocks.add(new Block.FeatureCard("❤", "HP напарника",
                "Справа снизу над смертями — полоска HP и голода второго. Не надо писать «ты живой?»"));
        blocks.add(new Block.FeatureCard("🔍", "Поиск в K",
                "В шапке настроек поле поиска. «бомба» сразу даёт тумблер и КД"));
        blocks.add(new Block.FeatureCard("↔", "Как у напарника",
                "K → Баланс. Копирует КД, HUD и шоу с живого конфига мира. Без OAuth и DonateX"));
        blocks.add(new Block.FeatureCard("⌨", "Опечатки",
                "Одна буква не та, лишняя, пропущенная или две соседние. §e!малния §7= молния, §e!хилл §7= хил"));
        blocks.add(new Block.FeatureCard("✈", "Элитры",
                "§e!элитры §7— крылья и 16 ракет. Physics Mod: взрывы и мобы с физикой"));
        blocks.add(new Block.FeatureCard("☠", "Кто убил",
                "На экране: кто убил и из-за чего — зритель, напарник, моб, падение, лава"));
        blocks.add(new Block.FeatureCard("📊", "Статистика мира",
                "Смерти, команды, вехи, кладбище — только этот мир.\nНовый мир = всё с нуля. K → Стрим → сброс"));
        blocks.add(new Block.FeatureCard("♥", "Карма чата",
                "2 урона подряд — дальше только добро. Ангел на сайдбаре. Морф за помощь"));
        blocks.add(new Block.FeatureCard("⚔", "Вызов",
                "§7−1 заряд, чат +2 фора, ничья = бомба. §e!вызов §7от чата — честно. Из меню I"));
        blocks.add(new Block.FeatureCard("🕊", "Милость",
                "Снимает блок урона, но стример в тыкве. Не победа — услуга чату. Из меню I"));
        blocks.add(new Block.FeatureCard("📜", "Контракт",
                "Бесплатно. 90с не умри = заряд. Смерть = армия чата. Из меню I"));
        blocks.add(new Block.FeatureCard("🗯", "Насмешка §8(!насмешка)",
                "25с урон без КД. Смерть = армия. Выжил = заряд. Нельзя под щитом. Из меню I"));
        blocks.add(new Block.FeatureCard("💰", "Выкуп",
                "Только бомба/охота. Платишь алмаз/тотем. Бомба → тыква. Охота без награды. Из меню I"));
        blocks.add(new Block.FeatureCard("⚔", "Что считается уроном",
                "Мобы-враги, TNT, лава, яма, молния, яд, голод, боссы.\n§7Пранки (крутилка, табличка, тыква) — не блок"));
        blocks.add(new Block.Spacer(6));
        blocks.add(new Block.Header("§e§lГолосование"));
        blocks.add(new Block.CommandCard("!голос", "Любой зритель запускает голосование на 3 команды", 60));
        blocks.add(new Block.Paragraph("§7Зрители голосуют: §e!1 §7/ §e!2 §7/ §e!3"));
        blocks.add(new Block.Spacer(6));
        blocks.add(new Block.Header("§a§lДобро vs §c§lЗло"));
        blocks.add(new Block.Paragraph("§a+ §7хил, помощь, реген, еда, спасение, алмаз, опыт"));
        blocks.add(new Block.Paragraph("§c− §7мобы, TNT, хаос, смерть от призрака"));
        return new Page("mechanics", "Механики", "⚡", "Суд · призраки · события", 0xFF_BF_94_FF, blocks);
    }

    private static Page moderatorPage() {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block.Paragraph("§7Команды для модераторов Twitch-канала:"));
        blocks.add(new Block.Spacer(4));
        for (ChatGameCommand cmd : ChatGameCommand.values()) {
            if (cmd.getCategory() != ChatGameCommand.Category.MODERATOR) continue;
            blocks.add(new Block.CommandCard("!" + cmd.getRussianName(), describe(cmd), liveCooldown(cmd)));
        }
        blocks.add(new Block.Spacer(8));
        blocks.add(new Block.Header("§6§lСерверные (OP)"));
        blocks.add(new Block.ServerCmd("/twitchcraft connect", "Подключить Twitch IRC"));
        blocks.add(new Block.ServerCmd("/twitchcraft disconnect", "Отключить"));
        blocks.add(new Block.ServerCmd("/twitchcraft reload", "Перезагрузить config/twitchcraft.json"));
        blocks.add(new Block.ServerCmd("/twitchcraft status", "Статус подключения"));
        blocks.add(new Block.ServerCmd("/twitchcraft test <cmd>", "Тест команды без Twitch"));
        blocks.add(new Block.ServerCmd("/twitchcraft dice", "Кубик кидает только мир"));
        blocks.add(new Block.ServerCmd("/twitchcraft testdice", "Проверить кубик мира"));
        blocks.add(new Block.ServerCmd("/twitchcraft testchaos", "Проверить колесо хаоса"));
        blocks.add(new Block.Paragraph("§eКлавиша I §7— меню тестов: донаты, колесо, кубик, все команды"));
        blocks.add(new Block.ServerCmd("/twitchcraft challenge", "Вызов чату"));
        blocks.add(new Block.ServerCmd("/twitchcraft mercy", "Милость — снять блок урона"));
        blocks.add(new Block.ServerCmd("/twitchcraft haus", "Хаусмод — все КД off/on (только стример/OP)"));
        blocks.add(new Block.ServerCmd("/rules", "Открыть это меню (для стримера)"));
        return new Page("moderator", "Админ", "⚙", "Модерам и OP", 0xFF_FF_55_66, blocks);
    }

    private static String categoryIcon(ChatGameCommand.Category cat) {
        return switch (cat) {
            case MOB -> "☠";
            case TNT -> "💥";
            case CHAOS -> "🌋";
            case INVENTORY -> "🎒";
            case MOVEMENT -> "🏃";
            case EFFECT -> "✨";
            case WORLD -> "🌍";
            case FUN -> "🎪";
            default -> "•";
        };
    }

    private static String describe(ChatGameCommand cmd) {
        return switch (cmd) {
            case LIGHTNING -> "Молния бьёт стримера";
            case CREEPER -> "Крипер рядом со стримером";
            case ZOMBIE -> "Зомби";
            case LAUNCH -> "Подбросить в небо";
            case SKELETON -> "Скелет";
            case SPIDER -> "Паук";
            case ENDERMAN -> "Эндермен";
            case WITCH -> "Ведьма";
            case SLIME -> "Большой слайм";
            case GHAST -> "Гаст над головой";
            case ARMY -> "Армия зомби и скелетов";
            case WITHER -> "Визер";
            case DRAGON -> "Дракон Края";
            case WOLF -> "Сердитый волк";
            case IRON_GOLEM -> "Железный голем";
            case TNT -> "TNT рядом";
            case TNT_RAIN -> "Дождь из TNT";
            case LAVA -> "Лава под ногами";
            case WATER -> "Вода";
            case PIT -> "Яма под ногами";
            case METEOR -> "Метеор из блоков";
            case ANVIL -> "Наковальня с неба";
            case STORM -> "Молнии по всем игрокам";
            case FIRE -> "Поджечь стримера";
            case CREEPER_STORM -> "Шторм криперов";
            case DROP -> "Выбросить предмет из руки";
            case CLEAR -> "Очистить инвентарь";
            case DIAMOND -> "Алмаз";
            case JUNK -> "Мусор в инвентарь";
            case CURSE -> "Проклятая броня";
            case SHUFFLE -> "Перемешать хотбар";
            case SWAP_INV -> "Обменять инвентари с напарником";
            case EGG -> "Залп яиц";
            case BOW -> "Лук и стрелы";
            case ELYTRA -> "Элитры на грудь (если слот свободен) + 16 ракет";
            case TELEPORT -> "Случайный телепорт";
            case SPIN -> "Крутилка";
            case BACKWARD -> "Развернуть камеру";
            case SWAP -> "Поменяться местами с игроком";
            case UP -> "Толкнуть вверх";
            case DOWN -> "Толкнуть вниз";
            case KNOCKBACK -> "Сильный отброс";
            case HEAL -> "Полное лечение + снятие дебаффов";
            case FEED -> "Восстановить голод и насыщение";
            case SAVE -> "Хил + еда + поглощение + реген";
            case TOTEM -> "Тотем бессмертия в инвентарь";
            case HURT -> "Урон";
            case BLIND -> "Слепота";
            case SPEED -> "Скорость";
            case SLOW -> "Медлительность";
            case HUNGER -> "Голод";
            case REGEN -> "Регенерация";
            case POISON -> "Отравление";
            case STRENGTH -> "Сила";
            case WEAKNESS -> "Слабость";
            case GLOW -> "Свечение";
            case DAY -> "День";
            case NIGHT -> "Ночь";
            case RAIN -> "Дождь";
            case CLEAR_WEATHER -> "Ясная погода";
            case THUNDER -> "Гроза";
            case NOON -> "Полдень";
            case RANDOM -> "Случайная команда";
            case COIN -> "Монетка орёл/решка";
            case BOSS -> "Случайный босс";
            case FIREWORK -> "Фейерверк";
            case SHEEP -> "Радужные овцы";
            case GIANT -> "Эффект гиганта";
            case HELP -> "Хил + сообщение поддержки";
            case ROULETTE -> "Рулетка эффектов";
            case VOTE -> "Любой зритель запускает голосование";
            case XP -> "Дождь опыта";
            case DOME -> "Стеклянный купол";
            case PRISON -> "Тюрьма из бедрока";
            case HYPE -> "Хайп-шоу";
            case EVENT -> "Хаос-событие";
            case CHICKEN -> "Дождь кур";
            case FLASH -> "Вспышка на экране";
            case TRAP -> "Ловушка";
            case VIEWER_EGG -> "Яйцо зрителя в инвентарь";
            case CHAT_PHANTOM -> "Босс-фантом 120 HP (1000₽)";
            case FPV -> "Курица-камикадзе (ФПВ) падает и взрывается";
            case TCK -> "ТЦК: 5 свинозомби окружают и уводят на 10 блоков";
            case NOVALNY -> "Телепорт на 2 блока вниз";
            case VOICE -> "Озвучка текста у стримера (!озвучка \"текст\")";
            case ANTIDOTE -> "Снять вредные эффекты";
            case GAPPLE -> "Выдать золотое яблоко";
            case TORCHES -> "Выдать стак факелов (64)";
            case BREAD -> "Выдать стак хлеба (64)";
            case PLANKS -> "Выдать стак дубовых досок (64)";
            case POTION -> "Случайное полезное зелье";
            case BUDDY -> "Приручённый волк зрителя";
            case RESPECT -> "Респект: поглощение + реген";
            case SLAP -> "Шлёпнуть тапком";
            case CLOWN -> "Надеть тыкву на голову";
            case FISH -> "Дождь из рыбы";
            case DANCE -> "Диско-крутилка и прыжки";
            case GOOSE -> "Гуси из Silly Goose";
            case CHAOS_CHICKENS -> "Куры с хаос-трейтами (Chaos Chickens)";
            case BRAINROT -> "Случайная мем-еда (DoritosandPrime)";
            case PRIME -> "Бутылка Prime + скорость";
            case KVA -> "КВА-звук (KvaKva) + лягушки";
            case AMONGUS -> "Among Us из Meme Craft";
            case OIA -> "Oia-кот из Meme Craft";
            case VINE -> "Vine Boom + отброс";
            case TROLL -> "Маска Troll Face";
            case LUCKY -> "Lucky Block рядом";
            case UNLUCKY -> "Very Unlucky Block рядом";
            case VERY_LUCKY -> "Very Lucky Block рядом";
            case GOLDEN_HEN -> "Золотая курочка (Lucky Break)";
            case LUCKY_ROULETTE -> "Рандом: лаки / неудача / очень лаки";
            case PRIME_RAVE -> "Prime + танцы + КВА";
            case PIGGYBACK -> "NPC со скином на спине (Easy NPC / Piggyback)";
            case CARRY_ON -> "Курица в руках (Carry On)";
            case RAGDOLL -> "Стример падает тряпичной куклой на 5 сек (физика Ragdolls), без клона";
            case VIEWER_ARMY -> "Армия NPC с никами из чата";
            case HUNT -> "Охота 60с: смерть стримера = победа охотника";
            case BET -> "Ставка: !ставка смерть / !ставка жив";
            case BOMB -> "Горячая картошка: кинь напарнику, или взорвёшься";
            case DEFUSE -> "Разминировать горячую картошку";
            case SIGN -> "Табличка с текстом зрителя (!табличка привет)";
            case STEAL -> "Украсть случайный стак из инвентаря";
            case JUMPSCARE -> "Скример: шипение крипера без урона";
            case KING -> "Король чата: мобы ×2";
            case DRUNK -> "Пьяный режим: тошнота и камера";
            case GRAVITY -> "Полёт вверх, потом падение";
            case HEIST -> "Ограбление: через 8с уносят лучший предмет";
            case LOCATE_VILLAGE -> "Найти деревню: направление и метры";
            case LOCATE_FORTRESS -> "Найти крепость Незера";
            case LOCATE_BASTION -> "Найти бастион";
            case LOCATE_STRONGHOLD -> "Найти крепость Края";
            case PORTAL_KIT -> "8 обсидиана + огниво";
            case FIRE_RES -> "Огнестойкость 3 минуты";
            case ENDER_EYE -> "Око Края в инвентарь";
            case MORPH -> "Превратить стримера в моба на 60с";
            case ANT -> "Настоящий муравей на 30с";
            case TITAN -> "Настоящий титан на 15с";
            case SIX_SEVEN -> "67: семь раз за 45с = размер-хаос";
            case TETHER -> "Связь 45с: урон и расстояние общие";
            case ORE_VEIN -> "Жила угля/железа рядом на 22с — успей добыть";
            case PARKOUR -> "Летающая дорожка: финиш = сундук, падение = хаос чату";
            case INVERT -> "WASD наоборот 10 секунд";
            case UPGRADE -> "Меню апгрейда: вложи предмет, шанс прокачать или сжечь";
            case STOP -> "Пауза всех команд чата";
            case STATUS -> "Статус в Twitch-чат";
            case SKIP -> "Пропустить КД команды";
        };
    }

    private static int liveCooldown(ChatGameCommand cmd) {
        TwitchCraftConfig config = ConfigManager.get();
        TwitchCraftConfig.CommandSettings settings = config.getCommandSettings(cmd.getId(), cmd.getDefaultCooldown());
        return settings.cooldownSeconds;
    }
}
