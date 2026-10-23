# TwitchCraft — Техническое задание

**Minecraft 1.21 · Fabric · Java 21**

Документ предназначен для передачи разработчику или AI-ассистенту. Содержит полное описание мода интеграции Twitch-чата с игровыми событиями.

---

## 1. Общее описание

**TwitchCraft** — серверный Fabric-мод для Minecraft 1.21, который подключается к Twitch IRC и выполняет игровые эффекты по команам зрителей в чате. Мод рассчитан на стримеров: зрители влияют на игру через префикс `!`.

### Цели

- Подключение к Twitch-каналу через IRC (библиотека **twitch4j**).
- Парсинг сообщений чата и выполнение зарегистрированных команд.
- Настройка через **Cloth Config** + **Mod Menu**, горячая клавиша **K**, команды `/twitchcraft`.
- Локализация **ru** / **en**.
- Безопасность: OAuth не отображается в UI, rate limit, whitelist, антиспам, отключение в Hardcore.

### Стек

| Компонент | Версия / библиотека |
|-----------|---------------------|
| Minecraft | 1.21 |
| Loader | Fabric |
| Java | 21 |
| Twitch | twitch4j (IRC) |
| Конфиг UI | Cloth Config API |
| Mod Menu | modmenu |
| Конфиг данные | JSON (файл на диске) |
| Локализация | `assets/twitchcraft/lang/en_us.json`, `ru_ru.json` |

---

## 2. Архитектура

```
twitchcraft/
├── TwitchCraftMod.java          # Entrypoint
├── twitch/
│   ├── TwitchConnection.java    # IRC connect/disconnect/reconnect
│   ├── ChatMessageHandler.java  # Парсинг !команд
│   └── OAuthManager.java        # Хранение токена (не в plain text в UI)
├── commands/
│   ├── CommandRegistry.java     # Регистрация всех команд
│   ├── CommandContext.java      # Player, sender, args, cooldown state
│   ├── CooldownManager.java     # Per-command CD
│   └── impl/                    # Реализации по категориям
├── effects/
│   └── ServerThreadExecutor.java # Все эффекты только на server thread
├── config/
│   ├── TwitchCraftConfig.java   # Dataclass / record
│   ├── ConfigManager.java       # Load/save JSON
│   └── ModMenuIntegration.java  # Cloth Config screen
├── client/
│   ├── KeybindHandler.java      # K — открыть экран
│   └── ConfigScreen.java        # Вкладки: Twitch, Команды, Безопасность
├── security/
│   ├── WhitelistFilter.java
│   ├── RateLimiter.java         # Мобы, TNT и т.д.
│   └── HardcoreGuard.java       # Блокировка в Hardcore
└── command/
    └── TwitchCraftCommands.java # /twitchcraft connect|reload|test|status
```

### Поток данных

1. IRC-сообщение → `ChatMessageHandler`.
2. Проверки: whitelist, антиспам, Hardcore, включена ли команда, cooldown.
3. Постановка задачи в очередь **server thread** (`ServerThreadExecutor`).
4. Выполнение эффекта на целевом игроке (стример или случайный — по команде).

---

## 3. Конфигурация

### 3.1 Файл JSON

Путь: `config/twitchcraft.json`

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

- `oauthToken` — хранится в файле; в UI показывается как `••••••••` (маскирование).
- `targetPlayer` — ник стримера; если пусто — оператор сервера / первый онлайн.
- Каждая команда имеет `enabled` и `cooldownSeconds` (переопределяют дефолты).

### 3.2 Mod Menu → Config

Экран на **Cloth Config** с вкладками:

| Вкладка | Поля |
|---------|------|
| **Twitch** | Канал, OAuth (скрытый ввод), автоподключение, целевой игрок |
| **Команды** | Список всех команд: вкл/выкл, cooldown (сек), группировка по категориям |
| **Безопасность** | Whitelist (список ников), антиспам (сек), Hardcore off, лимиты мобов/TNT |

### 3.3 Горячая клавиша

- По умолчанию: **K** — открывает тот же экран конфигурации (client-side).
- Регистрация через Fabric Key Binding API.

### 3.4 Команды сервера

| Команда | Описание |
|---------|----------|
| `/twitchcraft connect` | Подключиться к Twitch IRC |
| `/twitchcraft disconnect` | Отключиться |
| `/twitchcraft reload` | Перезагрузить config JSON |
| `/twitchcraft test <command>` | Выполнить команду локально (OP) |
| `/twitchcraft status` | Статус: connected, channel, uptime, последняя ошибка |

Требуют OP level 2 (настраиваемо).

---

## 4. Безопасность

1. **OAuth** — не логировать, не показывать в чате/консоли; в UI только password field.
2. **Server thread** — любые изменения мира (`World`, entities, blocks) только через `server.execute()`.
3. **Hardcore** — если `world.getLevelProperties().isHardcore()` и `disableInHardcore=true`, команды не выполняются.
4. **Whitelist** — если включён, только ники из списка могут триггерить команды.
5. **Антиспам** — один пользователь не чаще N секунд между любыми командами.
6. **Rate limit мобов** — глобальный счётчик спавнов в минуту (config).
7. **Rate limit TNT/взрывов** — отдельный лимит.
8. **Cooldown per command** — индивидуальный КД на команду (глобальный, не per-user, если не указано иное).

---

## 5. Команды чата

Формат: `!команда` или `!команда аргумент` (регистронезависимо для лatin; кириллица как в таблице).

Алиасы: каждая команда может иметь en-алиас (например `!lightning` = `!молния`).

### 5.1 Базовые (из описания)

| Команда | Алиас EN | Эффект | КД (сек) |
|---------|----------|--------|----------|
| `!молния` | `!lightning` | Молния в случайного онлайн-игрока | 30 |
| `!крипер` | `!creeper` | Спавн крипера рядом с целевым игроком | 15 |
| `!зомби` | `!zombie` | Спавн зомби | 15 |
| `!полет` | `!launch` | Подброс игрока на Y=255 (velocity вверх) | 20 |

### 5.2 Мобы

| Команда | Алиас EN | Эффект | КД |
|---------|----------|--------|-----|
| `!скелет` | `!skeleton` | Спавн скелета | 15 |
| `!паук` | `!spider` | Спавн паука | 15 |
| `!эндермен` | `!enderman` | Спавн эндермена | 20 |
| `!ведьма` | `!witch` | Спавн ведьмы | 20 |
| `!слайм` | `!slime` | Спавн большого слайма | 15 |
| `!гаст` | `!ghast` | Спавн гаста (если в Nether — иначе рядом в воздухе) | 30 |
| `!армия` | `!army` | 5 зомби + 3 скелета вокруг игрока | 60 |
| `!визер` | `!wither` | Спавн Wither (OP-dangerous; default enabled=false) | 120 |
| `!дракон` | `!dragon` | Спавн Ender Dragon (default enabled=false) | 300 |
| `!волк` | `!wolf` | Спавн агрессивного волка | 15 |
| `!железный_golem` | `!irongolem` | Спавн iron golem | 30 |

### 5.3 Хаос

| Команда | Алиас EN | Эффект | КД |
|---------|----------|--------|-----|
| `!tnt` | `!тнт` | Primed TNT x1 рядом с игроком | 20 |
| `!tnt_дождь` | `!tntrain` | 3–5 TNT сверху | 45 |
| `!лава` | `!lava` | Ведро лавы под ногами / блок лавы | 25 |
| `!вода` | `!water` | Залить водой область 3×3 | 20 |
| `!яма` | `!pit` | Вырыть яму 3×3×5 под игроком | 30 |
| `!метеор` | `!meteor` | Падающий анимированный блок (магма/obsidian) + взрыв без разрушения | 45 |
| `!наковальня` | `!anvil` | Падающая наковальня над игроком | 25 |
| `!молнии_везде` | `!storm` | 3 молнии в радиусе 10 блоков | 40 |
| `!огонь` | `!fire` | Поджечь игрока | 15 |
| `!крипер_шторм` | `!creeperstorm` | 3 charged creeper (или обычных) | 60 |

### 5.4 Инвентарь

| Команда | Алиас EN | Эффект | КД |
|---------|----------|--------|-----|
| `!дроп` | `!drop` | Выбросить случайный предмет из инвентаря | 20 |
| `!очистка` | `!clear` | Очистить весь инвентарь | 60 |
| `!алмаз` | `!diamond` | Дать 1 алмаз | 30 |
| `!мусор` | `!junk` | Дать случайный «мусорный» предмет (гниль, кость…) | 15 |
| `!проклятие` | `!curse` | Надеть предмет с Curse of Binding / Vanishing (если есть слот) | 45 |
| `!shuffle` | `!перемешать` | Перемешать слоты hotbar | 25 |
| `!яйцо` | `!egg` | 16 яиц в инвентарь | 10 |
| `!лук` | `!bow` | Лук + 32 стрелы | 20 |

### 5.5 Движение

| Команда | Алиас EN | Эффект | КД |
|---------|----------|--------|-----|
| `!телепорт` | `!tp` | Случайная телепортация в радиусе 50 блоков (безопасная Y) | 30 |
| `!крутилка` | `!spin` | Вращение камеры / velocity по кругу 5 сек | 25 |
| `!назад` | `!backward` | Инвертировать управление (эффект confusion + slowness) 10 сек | 30 |
| `!shuffle_pos` | `!swap` | Поменять местами с случайным онлайн-игроком | 45 |
| `!вверх` | `!up` | Телепорт +10Y | 15 |
| `!вниз` | `!down` | Телепорт −10Y (min Y=−64) | 15 |
| `!отброс` | `!knockback` | Сильный knockback в случайном направлении | 15 |

### 5.6 Эффекты (зелья)

| Команда | Алиас EN | Эффект | КД |
|---------|----------|--------|-----|
| `!лечение` | `!heal` | Полное HP + снять fire | 20 |
| `!урон` | `!hurt` | −4 HP | 15 |
| `!слепота` | `!blind` | Blindness 15 сек | 20 |
| `!скорость` | `!speed` | Speed II 20 сек | 15 |
| `!медлительность` | `!slow` | Slowness III 15 сек | 15 |
| `!голод` | `!hunger` | Hunger + exhaustion | 15 |
| `!реген` | `!regen` | Regeneration 10 сек | 20 |
| `!отравление` | `!poison` | Poison 8 сек | 20 |
| `!сила` | `!strength` | Strength II 15 сек | 20 |
| `!слабость` | `!weakness` | Weakness 15 сек | 15 |
| `!свечение` | `!glow` | Glowing 30 сек | 10 |

### 5.7 Погода / время

| Команда | Алиас EN | Эффект | КД |
|---------|----------|--------|-----|
| `!день` | `!day` | Время 6000 | 30 |
| `!ночь` | `!night` | Время 18000 | 30 |
| `!дождь` | `!rain` | Дождь вкл | 30 |
| `!ясно` | `!clear` | Дождь/гроза выкл | 30 |
| `!гром` | `!thunder` | Гроза | 45 |
| `!полдень` | `!noon` | Время 6000 + clear weather | 30 |

### 5.8 Fun

| Команда | Алиас EN | Эффект | КД |
|---------|----------|--------|-----|
| `!рандом` | `!random` | Случайная команда из включённых (кроме служебных) | 30 |
| `!монетка` | `!coin` | 50% heal / 50% hurt | 10 |
| `!босс` | `!boss` | Спавн Warden или Elder Guardian (config) | 90 |
| `!фейерверк` | `!firework` | Запуск фейерверка над игроком | 15 |
| `!овца` | `!sheep` | Разноцветные овцы x5 | 15 |
| `!гигант` | `!giant` | Slowness VI + Strength + увеличение hitbox (visual via particles) | 30 |

### 5.9 Служебные (модератор / стример)

| Команда | Алиас EN | Эффект | КД |
|---------|----------|--------|-----|
| `!стоп` | `!stop` | Пауза выполнения команд на 60 сек (только whitelist moderators) | — |
| `!статус` | `!status` | Сообщение в чат Twitch: mod online, queue length | 10 |
| `!skip` | `!скип` | Пропустить текущий cooldown (moderator) | — |

Модераторы — отдельный список в config `security.moderators[]`.

---

## 6. Локализация

Ключи в формате `twitchcraft.<category>.<id>`.

Примеры:

```json
{
  "twitchcraft.command.lightning": "Lightning strikes a random player!",
  "twitchcraft.config.title": "TwitchCraft Settings",
  "twitchcraft.status.connected": "Connected to %s",
  "twitchcraft.error.hardcore": "Commands disabled in Hardcore mode"
}
```

`ru_ru.json` — полный перевод всех ключей UI, сообщений команд и ошибок.

---

## 7. Зависимости (gradle)

```gradle
dependencies {
    modImplementation "net.fabricmc:fabric-loader:..."
    modImplementation "net.fabricmc.fabric-api:fabric-api:..."
    modImplementation "com.github.twitch4j:twitch4j:1.21.0" // актуальная версия
    modImplementation "com.terraformersmc:modmenu:..."
    modImplementation "me.shedaniel.cloth:cloth-config-fabric:..."
}
```

- **fabric-api**: networking (optional), command API, lifecycle events.
- **twitch4j** — IRC client; OAuth: `oauth:xxxxxxxx` из https://twitchapps.com/tmi/

---

## 8. Сборка и запуск

```bash
./gradlew build
# JAR: build/libs/twitchcraft-1.0.0.jar
```

1. Положить JAR в `mods/` сервера (и клиента, если нужен экран K).
2. Запустить сервер, заполнить `config/twitchcraft.json` или Mod Menu.
3. `/twitchcraft connect` или `autoConnect: true`.

---

## 9. Тестирование

| Сценарий | Ожидание |
|----------|----------|
| IRC connect | status connected, сообщения в лог без OAuth |
| `!молния` | Молния, повтор через 30 сек игнорируется |
| Cooldown off in config | Команда срабатывает каждый раз |
| Hardcore world | Команды не выполняются, warning в лог |
| Whitelist | Чужой ник — игнор |
| `/twitchcraft test lightning` | Эффект без IRC |
| Disconnect/reconnect | Автопереподключение (optional, config) |

---

## 10. Нефункциональные требования

- **Производительность**: обработка чата асинхронно; тяжёлые эффекты — server thread, без блокировки IRC.
- **Совместимость**: dedicated server + integrated server (singleplayer стрим).
- **Логирование**: SLF4J; уровень INFO для connect/command, DEBUG опционально.
- **Версионирование**: semver, mod id `twitchcraft`.

---

## 11. Структура репозитория (рекомендуемая)

```
TwitchMod/
├── build.gradle
├── gradle.properties          # minecraft_version=1.21, yarn_mappings, loader
├── settings.gradle
├── src/main/java/...          # пакет net.twitchcraft или com.<author>.twitchcraft
├── src/main/resources/
│   ├── fabric.mod.json
│   └── assets/twitchcraft/lang/
└── config/                    # пример twitchcraft.json (optional template)
```

---

## 12. Чеклист реализации

- [ ] Gradle + Fabric 1.21 skeleton
- [ ] twitch4j IRC + OAuth
- [ ] CommandRegistry + ~45 команд
- [ ] CooldownManager + RateLimiter
- [ ] ServerThreadExecutor
- [ ] JSON config load/save
- [ ] Cloth Config + Mod Menu
- [ ] Keybind K
- [ ] /twitchcraft commands
- [ ] ru/en lang files
- [ ] Whitelist, antispam, Hardcore guard
- [ ] README для стримеров (OAuth, setup)

---

*Конец документа. Версия ТЗ: 1.0 · Дата: 2026-06-16*
