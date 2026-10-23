package net.twitchcraft.client;

import net.fabricmc.loader.api.FabricLoader;
import net.twitchcraft.config.PresetIO;
import net.twitchcraft.config.TwitchCraftConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/** Папка «/twitchcraft-для-напарника — скинуть архивом, без токенов. */
public final class PartnerPack {
    private PartnerPack() {}

    public static Path export(TwitchCraftConfig config) throws IOException {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("twitchcraft-для-напарника");
        Files.createDirectories(dir);

        Files.writeString(dir.resolve("ПРОЧТИ.txt"), readme(), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("preset.json"), PresetIO.toJson(config), StandardCharsets.UTF_8);

        Path mods = FabricLoader.getInstance().getGameDir().resolve("mods");
        copyMatching(mods, dir, "twitchcraft-", ".jar");
        copyMatching(mods, dir, "physics-mod-", ".jar");
        copyMatching(mods, dir, "ragdolls-", ".jar");

        return dir;
    }

    private static void copyMatching(Path mods, Path dest, String prefix, String suffix) throws IOException {
        if (!Files.isDirectory(mods)) return;
        Path newest = null;
        long newestTime = Long.MIN_VALUE;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(mods)) {
            for (Path file : stream) {
                String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!name.startsWith(prefix) || !name.endsWith(suffix) || !Files.isRegularFile(file)) continue;
                long time = Files.getLastModifiedTime(file).toMillis();
                if (newest == null || time >= newestTime) {
                    newest = file;
                    newestTime = time;
                }
            }
        }
        if (newest != null) {
            Files.copy(newest, dest.resolve(newest.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static String readme() {
        return """
                TwitchCraft — для напарника (2 минуты)

                ЧТО ПОЛОЖИТЬ В mods
                -------------------
                Те же jar, что в этой папке:
                  twitchcraft-….jar     — чат, HUD, команды
                  physics-mod-….jar     — физика взрывов (нужен ОБОИМ)
                  ragdolls-….jar        — тряпичные трупы (клиент, нужен ОБОИМ)

                Профиль: Fabric 1.21.10, как у напарника. Не клади второй twitchcraft-*.jar.

                ПЕРВЫЙ ЗАХОД
                ------------
                1. Зайди в мир (LAN к хосту).
                2. Нажми K.
                3. Twitch: СВОЙ канал и СВОЙ OAuth (не чужой).
                   OAuth: twitchapps.com/tmi
                4. Баланс → «Как у напарника»  ИЛИ  импорт preset.json из этой папки.
                5. Справа внизу — Сохранить.

                КЛАВИШИ
                -------
                K     настройки TwitchCraft
                R     правила (!команды чата)
                I     лаборатория тестов (только стример)
                U     свои команды
                P     статистика чата
                Средняя кнопка мыши — пинг напарнику (куда смотришь)

                PHYSICS
                -------
                Esc → Настройки → Physics Settings...
                Для стрима хватит как есть. Не жми GET PRO.
                Лаги: GENERAL → Physics Max Objects убавить (например 200).

                ЧАТ (примеры)
                -------------
                !элитры   крылья + ракеты
                !тряпка   стример падает ragdoll на 5 сек
                !лаки     lucky-блок
                !хил      лечение
                !бомба    горячая картошка

                Токены DonateX / OAuth в эту папку не кладутся. Каждый пишет свои в K → Twitch.
                """;
    }
}
