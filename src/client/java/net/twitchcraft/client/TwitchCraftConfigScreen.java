package net.twitchcraft.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;

import java.util.ArrayList;
import java.util.List;

public final class TwitchCraftConfigScreen {
    private TwitchCraftConfigScreen() {}

    public static Screen create(Screen parent) {
        TwitchCraftConfig config = copy(ConfigManager.get());
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("twitchcraft.config.title"))
                .setSavingRunnable(() -> {
                    ConfigManager.set(config);
                });

        ConfigEntryBuilder entry = builder.entryBuilder();

        ConfigCategory twitch = builder.getOrCreateCategory(Component.translatable("twitchcraft.config.category.twitch"));
        twitch.addEntry(entry.startStrField(Component.translatable("twitchcraft.config.channel"), config.twitch.channel)
                .setDefaultValue("")
                .setSaveConsumer(v -> config.twitch.channel = v)
                .build());
        twitch.addEntry(entry.startStrField(Component.translatable("twitchcraft.config.oauth"), maskToken(config.twitch.oauthToken))
                .setDefaultValue("")
                .setTooltip(Component.translatable("twitchcraft.config.oauth.tooltip"))
                .setSaveConsumer(v -> {
                    if (!v.contains("•")) config.twitch.oauthToken = v;
                })
                .build());
        twitch.addEntry(entry.startBooleanToggle(Component.translatable("twitchcraft.config.autoconnect"), config.twitch.autoConnect)
                .setDefaultValue(false)
                .setSaveConsumer(v -> config.twitch.autoConnect = v)
                .build());
        twitch.addEntry(entry.startStrField(Component.translatable("twitchcraft.config.target"), config.targetPlayer)
                .setDefaultValue("")
                .setTooltip(Component.literal("Один ник или два через запятую: nick1,nick2"))
                .setSaveConsumer(v -> config.targetPlayer = v)
                .build());
        twitch.addEntry(entry.startStrList(Component.literal("Стримеры (список)"), new ArrayList<>(config.targetPlayers))
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Component.literal("Приоритет над targetPlayer. Для двух стримеров на сервере."))
                .setSaveConsumer(v -> config.targetPlayers = new ArrayList<>(v))
                .build());

        ConfigCategory commands = builder.getOrCreateCategory(Component.translatable("twitchcraft.config.category.commands"));
        for (ChatGameCommand.Category cat : ChatGameCommand.Category.values()) {
            if (cat == ChatGameCommand.Category.MODERATOR) continue;
            commands.addEntry(entry.startTextDescription(Component.literal("§6── " + categoryRu(cat) + " ──")).build());
            for (ChatGameCommand cmd : ChatGameCommand.values()) {
                if (cmd.getCategory() != cat) continue;
                TwitchCraftConfig.CommandSettings settings = config.getCommandSettings(cmd.getId(), cmd.getDefaultCooldown());
                String label = "!" + cmd.getRussianName();
                commands.addEntry(entry.startBooleanToggle(Component.literal(label), settings.enabled)
                        .setDefaultValue(true)
                        .setSaveConsumer(v -> settings.enabled = v)
                        .build());
                commands.addEntry(entry.startIntSlider(
                                Component.literal(label + " · КД (сек)"),
                                settings.cooldownSeconds, 0, 300)
                        .setDefaultValue(cmd.getDefaultCooldown())
                        .setSaveConsumer(v -> settings.cooldownSeconds = v)
                        .build());
                commands.addEntry(entry.startIntSlider(
                                Component.literal(label + " · макс/мин"),
                                settings.maxUsesPerMinute, 0, 30)
                        .setDefaultValue(0)
                        .setTooltip(Component.literal("0 = только лимит категории"))
                        .setSaveConsumer(v -> settings.maxUsesPerMinute = v)
                        .build());
            }
        }

        ConfigCategory balance = builder.getOrCreateCategory(Component.literal("Баланс (~100 чат)"));
        balance.addEntry(entry.startIntSlider(Component.translatable("twitchcraft.config.antispam"), config.security.antiSpamSeconds, 0, 60)
                .setDefaultValue(8)
                .setTooltip(Component.literal("Мин. пауза между командами одного зрителя"))
                .setSaveConsumer(v -> config.security.antiSpamSeconds = v)
                .build());
        balance.addEntry(entry.startIntSlider(Component.translatable("twitchcraft.config.moblimit"), config.security.mobSpawnRateLimitPerMinute, 1, 60)
                .setDefaultValue(12).setSaveConsumer(v -> config.security.mobSpawnRateLimitPerMinute = v).build());
        balance.addEntry(entry.startIntSlider(Component.translatable("twitchcraft.config.tntlimit"), config.security.tntRateLimitPerMinute, 1, 30)
                .setDefaultValue(6).setSaveConsumer(v -> config.security.tntRateLimitPerMinute = v).build());
        balance.addEntry(entry.startIntSlider(Component.literal("Лимит хаос/мин"), config.security.chaosRateLimitPerMinute, 1, 40)
                .setDefaultValue(10).setSaveConsumer(v -> config.security.chaosRateLimitPerMinute = v).build());
        balance.addEntry(entry.startIntSlider(Component.literal("Лимит эффекты/мин"), config.security.effectRateLimitPerMinute, 1, 40)
                .setDefaultValue(14).setSaveConsumer(v -> config.security.effectRateLimitPerMinute = v).build());
        balance.addEntry(entry.startIntSlider(Component.literal("Лимит движение/мин"), config.security.movementRateLimitPerMinute, 1, 40)
                .setDefaultValue(12).setSaveConsumer(v -> config.security.movementRateLimitPerMinute = v).build());
        balance.addEntry(entry.startIntSlider(Component.literal("Лимит инвент/мин"), config.security.inventoryRateLimitPerMinute, 1, 40)
                .setDefaultValue(10).setSaveConsumer(v -> config.security.inventoryRateLimitPerMinute = v).build());
        balance.addEntry(entry.startIntSlider(Component.literal("Лимит мир/мин"), config.security.worldRateLimitPerMinute, 1, 30)
                .setDefaultValue(8).setSaveConsumer(v -> config.security.worldRateLimitPerMinute = v).build());
        balance.addEntry(entry.startIntSlider(Component.literal("Лимит шоу/мин"), config.security.funRateLimitPerMinute, 1, 30)
                .setDefaultValue(10).setSaveConsumer(v -> config.security.funRateLimitPerMinute = v).build());

        ConfigCategory security = builder.getOrCreateCategory(Component.translatable("twitchcraft.config.category.security"));
        security.addEntry(entry.startBooleanToggle(Component.translatable("twitchcraft.config.whitelist"), config.security.whitelistEnabled)
                .setDefaultValue(false)
                .setSaveConsumer(v -> config.security.whitelistEnabled = v)
                .build());
        security.addEntry(entry.startStrList(Component.translatable("twitchcraft.config.whitelist.list"), new ArrayList<>(config.security.whitelist))
                .setDefaultValue(new ArrayList<>())
                .setSaveConsumer(v -> config.security.whitelist = new ArrayList<>(v))
                .build());
        security.addEntry(entry.startStrList(Component.translatable("twitchcraft.config.moderators"), new ArrayList<>(config.security.moderators))
                .setDefaultValue(new ArrayList<>())
                .setSaveConsumer(v -> config.security.moderators = new ArrayList<>(v))
                .build());
        security.addEntry(entry.startIntSlider(Component.translatable("twitchcraft.config.antispam"), config.security.antiSpamSeconds, 0, 60)
                .setDefaultValue(8)
                .setSaveConsumer(v -> config.security.antiSpamSeconds = v)
                .build());
        security.addEntry(entry.startBooleanToggle(Component.translatable("twitchcraft.config.hardcore"), config.security.disableInHardcore)
                .setDefaultValue(true)
                .setSaveConsumer(v -> config.security.disableInHardcore = v)
                .build());

        ConfigCategory feedback = builder.getOrCreateCategory(Component.translatable("twitchcraft.config.category.feedback"));
        feedback.addEntry(entry.startBooleanToggle(Component.translatable("twitchcraft.config.feedback.chat"), config.feedback.announceInTwitchChat)
                .setDefaultValue(false).setSaveConsumer(v -> config.feedback.announceInTwitchChat = v).build());
        feedback.addEntry(entry.startBooleanToggle(Component.translatable("twitchcraft.config.feedback.title"), config.feedback.showTitleInGame)
                .setDefaultValue(true).setSaveConsumer(v -> config.feedback.showTitleInGame = v).build());
        feedback.addEntry(entry.startBooleanToggle(Component.translatable("twitchcraft.config.feedback.actionbar"), config.feedback.showActionBar)
                .setDefaultValue(true).setSaveConsumer(v -> config.feedback.showActionBar = v).build());

        ConfigCategory hud = builder.getOrCreateCategory(Component.translatable("twitchcraft.config.category.hud"));
        hud.addEntry(entry.startBooleanToggle(Component.translatable("twitchcraft.config.scoreboard"), config.scoreboard.enabled)
                .setDefaultValue(true).setSaveConsumer(v -> config.scoreboard.enabled = v).build());
        hud.addEntry(entry.startBooleanToggle(Component.translatable("twitchcraft.config.scoreboard.commands"), config.scoreboard.showCommands)
                .setDefaultValue(true).setSaveConsumer(v -> config.scoreboard.showCommands = v).build());
        hud.addEntry(entry.startBooleanToggle(Component.translatable("twitchcraft.config.scoreboard.donations"), config.scoreboard.showDonations)
                .setDefaultValue(true).setSaveConsumer(v -> config.scoreboard.showDonations = v).build());

        ConfigCategory donations = builder.getOrCreateCategory(Component.translatable("twitchcraft.config.category.donations"));
        donations.addEntry(entry.startBooleanToggle(Component.translatable("twitchcraft.config.donations.enabled"), config.donations.enabled)
                .setDefaultValue(false).setSaveConsumer(v -> config.donations.enabled = v).build());
        donations.addEntry(entry.startStrField(Component.translatable("twitchcraft.config.donations.token"), maskToken(config.donations.accessToken))
                .setDefaultValue("").setSaveConsumer(v -> { if (!v.contains("•")) config.donations.accessToken = v; }).build());

        ConfigCategory stream = builder.getOrCreateCategory(Component.translatable("twitchcraft.config.category.stream"));
        stream.addEntry(entry.startBooleanToggle(Component.translatable("twitchcraft.config.stream.namedmobs"), config.stream.namedMobs)
                .setDefaultValue(true).setSaveConsumer(v -> config.stream.namedMobs = v).build());
        stream.addEntry(entry.startBooleanToggle(Component.translatable("twitchcraft.config.stream.revenge"), config.stream.revengeOnDeath)
                .setDefaultValue(true).setSaveConsumer(v -> config.stream.revengeOnDeath = v).build());
        stream.addEntry(entry.startBooleanToggle(Component.translatable("twitchcraft.config.stream.chaos"), config.stream.chaosMeterEnabled)
                .setDefaultValue(true).setSaveConsumer(v -> config.stream.chaosMeterEnabled = v).build());
        stream.addEntry(entry.startIntSlider(Component.translatable("twitchcraft.config.stream.chaosmax"), config.stream.chaosMeterMax, 10, 200)
                .setDefaultValue(50).setSaveConsumer(v -> config.stream.chaosMeterMax = v).build());
        stream.addEntry(entry.startBooleanToggle(Component.literal("Суд чата"), config.stream.courtEnabled)
                .setDefaultValue(true).setSaveConsumer(v -> config.stream.courtEnabled = v).build());
        stream.addEntry(entry.startIntSlider(Component.literal("Суд: секунды"), config.stream.courtDurationSeconds, 10, 120)
                .setDefaultValue(30).setSaveConsumer(v -> config.stream.courtDurationSeconds = v).build());
        stream.addEntry(entry.startBooleanToggle(Component.literal("Boss Bar: Чат vs Стример"), config.stream.battleBarEnabled)
                .setDefaultValue(false).setSaveConsumer(v -> config.stream.battleBarEnabled = v).build());
        stream.addEntry(entry.startIntSlider(Component.literal("HP битвы"), config.stream.battleMaxHp, 20, 200)
                .setDefaultValue(100).setSaveConsumer(v -> config.stream.battleMaxHp = v).build());
        stream.addEntry(entry.startBooleanToggle(Component.literal("Случайные события"), config.stream.randomEventsEnabled)
                .setDefaultValue(true).setSaveConsumer(v -> config.stream.randomEventsEnabled = v).build());
        stream.addEntry(entry.startIntSlider(Component.literal("События: мин (от)"), config.stream.randomEventMinMinutes, 1, 30)
                .setDefaultValue(3).setSaveConsumer(v -> config.stream.randomEventMinMinutes = v).build());
        stream.addEntry(entry.startIntSlider(Component.literal("События: мин (до)"), config.stream.randomEventMaxMinutes, 1, 60)
                .setDefaultValue(6).setSaveConsumer(v -> config.stream.randomEventMaxMinutes = v).build());
        stream.addEntry(entry.startBooleanToggle(Component.literal("§dХаусмод (без КД)"), config.stream.houseMode)
                .setDefaultValue(false)
                .setTooltip(Component.literal("Отключает все КД и лимиты. Только стример: /twitchcraft haus или K → Стрим"))
                .setSaveConsumer(v -> config.stream.houseMode = v)
                .build());

        return builder.build();
    }

    private static String categoryRu(ChatGameCommand.Category cat) {
        return switch (cat) {
            case MOB -> "Мобы";
            case TNT -> "TNT";
            case CHAOS -> "Хаос";
            case INVENTORY -> "Инвентарь";
            case MOVEMENT -> "Движение";
            case EFFECT -> "Эффекты";
            case WORLD -> "Мир";
            case FUN -> "Шоу";
            default -> cat.name();
        };
    }

    private static TwitchCraftConfig copy(TwitchCraftConfig src) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        return gson.fromJson(gson.toJson(src), TwitchCraftConfig.class);
    }

    private static String maskToken(String token) {
        if (token == null || token.isBlank()) return "";
        return "••••••••";
    }
}
