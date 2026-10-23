package net.twitchcraft.donation;

import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.config.TwitchCraftConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Пул приколов для донат-рулетки 500₽+. */
public final class DonationWheelPool {
    public static final int WHEEL_SLOTS = 5;

    public record Segment(String commandId, String label, int color) {}

    public record SpinResult(List<Segment> segments, int winnerIndex) {
        public Segment winner() {
            return segments.get(winnerIndex);
        }
    }

    private static final List<Segment> POOL = List.of(
            new Segment("meteor", "🌋 Метеоритный дождь", 0xFF_E7_4C_3C),
            new Segment("wither", "💀 Появление Визера", 0xFF_2C_3E_50),
            new Segment("army", "⚔ Легион хаоса", 0xFF_8E_44_AD),
            new Segment("tntrain", "💥 TNT-апокалипсис", 0xFF_E6_7E_22),
            new Segment("boss", "👹 Босс-рейд", 0xFF_C0_39_2B),
            new Segment("dragon", "🐉 Дракон над головой", 0xFF_9B_59_B6),
            new Segment("prison", "⛓ Железная тюрьма", 0xFF_7F_8C_8D),
            new Segment("creeperstorm", "💣 Крипер-шторм", 0xFF_27_AE_60),
            new Segment("event", "🎪 Тройное событие", 0xFF_F3_9C_12),
            new Segment("spin", "🌀 Вихрь безумия", 0xFF_34_98_DB),
            new Segment("curse", "☠ Адское проклятие", 0xFF_1A_BC_9C),
            new Segment("clear", "🗑 Полная очистка", 0xFF_95_A5_A6),
            new Segment("launch", "🚀 Космический полёт", 0xFF_48_C9_B0),
            new Segment("trap", "⚠ Смертельная ловушка", 0xFF_D3_54_00),
            new Segment("chicken", "🐔 Куриный ад", 0xFF_F1_C4_0F),
            new Segment("anvil", "🔨 Дождь наковален", 0xFF_BDC_3C_7),
            new Segment("storm", "⚡ Громовой шторм", 0xFF_29_80_B9),
            new Segment("hype", "🔥 Мега-хайп", 0xFF_E9_1E_63),
            new Segment("dome", "🔮 Кристальный купол", 0xFF_91_46_FF),
            new Segment("lava", "🌋 Лава под ногами", 0xFF_E6_7E_22),
            new Segment("pit", "🕳 Яма", 0xFF_5D_4E_37),
            new Segment("drop", "🎒 Выронил всё", 0xFF_B2_BE_B5),
            new Segment("titan", "🏔 Титан", 0xFF_E7_4C_3C),
            new Segment("ant", "🐜 Муравей", 0xFF_7F_8C_8D),
            new Segment("shuffle", "🔀 Хотбар в кашу", 0xFF_9B_59_B6)
    );

    private DonationWheelPool() {}

    /** Случайные 5 сегментов из пула + случайный победитель. */
    public static SpinResult roll(TwitchCraftConfig config) {
        List<Segment> available = new ArrayList<>();
        for (Segment seg : POOL) {
            if (isEnabled(config, seg.commandId())) {
                available.add(seg);
            }
        }
        if (available.size() < WHEEL_SLOTS) {
            available = new ArrayList<>(POOL);
        }

        Collections.shuffle(available, ThreadLocalRandom.current());
        List<Segment> picked = new ArrayList<>(available.subList(0, Math.min(WHEEL_SLOTS, available.size())));

        int attempts = 0;
        while (picked.size() < WHEEL_SLOTS && attempts++ < 64) {
            Segment extra = POOL.get(ThreadLocalRandom.current().nextInt(POOL.size()));
            if (picked.stream().noneMatch(s -> s.commandId().equals(extra.commandId()))) {
                picked.add(extra);
            }
        }
        while (picked.size() < WHEEL_SLOTS) {
            picked.add(POOL.get(picked.size() % POOL.size()));
        }

        int winnerIndex = ThreadLocalRandom.current().nextInt(WHEEL_SLOTS);
        return new SpinResult(List.copyOf(picked), winnerIndex);
    }

    public static int poolSize() {
        return POOL.size();
    }

    private static boolean isEnabled(TwitchCraftConfig config, String commandId) {
        return ChatGameCommand.fromId(commandId)
                .map(cmd -> config.getCommandSettings(cmd.getId(), cmd.getDefaultCooldown()).enabled)
                .orElse(true);
    }
}
