# TwitchCraft

**TwitchCraft** — Fabric-мод для Minecraft 1.21.10 на Java 21, который подключает Twitch-чат к игровому миру. Зрители управляют игрой через команды с префиксом `!` в чате Twitch. Идеально для стримеров.

[![Modrinth](https://img.shields.io/modrinth/bt/twitchcraft?color=0076ff&label=Modrinth)](https://modrinth.com/mod/twitchcraft)
[![GitHub](https://img.shields.io/github/v/release/Smirty/TwitchCraft?include_prereleases&color=0076ff)](https://github.com/Smirty/TwitchCraft/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Minecraft 1.21.10](https://img.shields.io/badge/Minecraft-1.21.10-0076ff?logo=minecraft)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.3-0076ff)](https://fabricmc.net/)
[![Java 21](https://img.shields.io/badge/Java-21-0076ff)](https://adoptium.net/)

## Возможности

- **50+ команд** чата: `!крипер`, `!тнт`, `!армия`, `!метеор`, `!молния` и многие другие
- **Суд чата при смерти**: `!виноват` / `!невиновен`
- **Мобы-призраки зрителей** с ником `[Viewer]`
- **Boss Bar «Чат vs Стример»**
- **Scoreboard** с отображением КД, кладбища и статистики
- **Хаусмод** — все КД отключены (`/twitchcraft haus` или клавиша `K` → Стрим)
- **DonateX** (опциональная интеграция с донатами)
- **`/rules`** — справочник команд прямо в игре
- **Конструктор команд** — создание кастомных команд через GUI
- Локализация: **русский** и **английский**

## Архитектура

Мод состоит из следующих основных модулей:

| Модуль | Назначение |
|--------|------------|
| `twitchcraft.twitch` | Подключение к Twitch IRC через twitch4j, обработка сообщений чата |
| `twitchcraft.commands` | Регистрация команд, коoldown-менеджер, категории команд |
| `twitchcraft.effects` / `stream` | Выполнение игровых эффектов (только на server thread) |
| `twitchcraft.config` | Загрузка/сохранение JSON-конфига, интеграция с Cloth Config + Mod Menu |
| `twitchcraft.security` | Whitelist, антиспам, защита от хардкора, лимиты мобов/TNT |
| `twitchcraft.client` | Клиентский интерфейс, горячие клавиши, рендер, HUD |
| `twitchcraft.donation` | Интеграция с DonateX, колесо донатов, очереди |
| `twitchcraft.network` | Сетевые пакеты между клиентом и сервером |
| `twitchcraft.mixin` | Mixin для модификации игрового поведения |

## Быстрый старт

### Установка для игроков

1. Установите [Fabric Loader](https://fabricmc.net/install/) (версии 0.19.3+)
2. Установите [Fabric API](https://modrinth.com/mod/fabric-api) (версии 0.138.4+1.21.10)
3. Установите [Cloth Config](https://modrinth.com/mod/cloth-config) и [Mod Menu](https://modrinth.com/mod/modmenu)
4. Скачайте последнюю версию TwitchCraft с [Modrinth](https://modrinth.com/mod/twitchcraft) или [GitHub Releases](https://github.com/Smirty/TwitchCraft/releases)
5. Положите JAR-файл в папку `mods/` вашего Minecraft-клиента и сервера
6. Запустите игру, настройте мод через Mod Menu (клавиша `K`) или команду `/twitchcraft`

### Настройка Twitch

1. Получите OAuth-токен на [twitchapps.com/tmi](https://twitchapps.com/tmi)
2. В настройках мода укажите имя канала и OAuth-токен (токен маскируется в интерфейсе)
3. Включите автоподключение или используйте `/twitchcraft connect` в игре
4. Зрители могут теперь писать `!команда` в чате Twitch

## Сборка из исходного кода

### Требования

- JDK 21+
- Gradle 8+

### Команды

```bash
# Клонировать репозиторий
git clone https://github.com/Smirty/TwitchCraft.git
cd TwitchCraft

# Собрать мод
./gradlew build

# Готовый JAR: build/libs/twitchcraft-1.31.60.jar

# Установить в профиль Modrinth 'stream' (если настроен)
./gradlew deployStream
```

## Конфигурация

Конфиг хранится в `config/twitchcraft.json`:

```json
{
  "twitch": {
    "channel": "your_channel",
    "oauthToken": "",
    "autoConnect": false
  },
  "targetPlayer": "",
  "security": {
    "whitelistEnabled": false,
    "whitelist": [],
    "moderators": [],
    "antiSpamSeconds": 3,
    "disableInHardcore": true,
    "mobSpawnRateLimitPerMinute": 10,
    "tntRateLimitPerMinute": 5
  },
  "commands": {
    "lightning": { "enabled": true, "cooldownSeconds": 30 },
    "creeper": { "enabled": true, "cooldownSeconds": 15 }
  },
  "keybind": "key.keyboard.k"
}
```

Команды настраиваются individual: включение/выключение и cooldown для каждой.

## Команды сервера

| Команда | Описание | Права |
|---------|----------|-------|
| `/twitchcraft connect` | Подключиться к Twitch IRC | OP 2 |
| `/twitchcraft disconnect` | Отключиться от Twitch | OP 2 |
| `/twitchcraft reload` | Перезагрузить конфиг | OP 2 |
| `/twitchcraft test <команда>` | Выполнить команду локально | OP 2 |
| `/twitchcraft status` | Статус подключения | OP 2 |
| `/twitchcraft haus` | Хаусмод (все КД выкл) | OP 2 |
| `/rules` | Справочник команд | Все |

## Горячие клавиши

| Клавиша | Действие |
|---------|----------|
| `K` | Открыть настройки TwitchCraft |

## Безопасность

- OAuth-токен не отображается в интерфейсе (поле ввода пароля)
- Все изменения мира выполняются только на server thread
- В режиме Hardcore команды отключаются (настраивается)
- Белый список ограничивает доступ к командам
- Антиспам ограничивает частоту команд от одного пользователя
- Лимиты на спавн мобов и взрывы TNT

## Вклад в проект

Приветствуются отчёты об ошибках, предложения и pull-запросы. Перед отправкой PR:

1. Создайте issue с описанием проблемы или предложения
2. Сделайте fork репозитория
3. Создайте ветку для ваших изменений
4. Убедитесь, что код компилируется (`./gradlew build`)
5. Отправьте pull-запрос с описанием изменений

## Лицензия

Распространяется под лицензией [MIT](LICENSE).

## Ссылки

- [Modrinth проект](https://modrinth.com/mod/twitchcraft)
- [GitHub репозиторий](https://github.com/Smirty/TwitchCraft)
- [ТЗ (Техническое задание)](TZ.md)
- [Автор: Smirty](https://github.com/Smirty)

---

*TwitchCraft — управляйте миром через Twitch-чат.*