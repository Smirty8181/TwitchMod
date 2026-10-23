package net.twitchcraft.stream;

import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.config.TwitchCraftConfig;
import net.twitchcraft.donation.DonationWheelPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Наказания, когда полоска хаоса заполнена. */
public final class ChaosWheelPool {
    private static final List<DonationWheelPool.Segment> PUNISH = List.of(
            new DonationWheelPool.Segment("trap", "Смертельная ловушка", 0xFF_D3_54_00),
            new DonationWheelPool.Segment("launch", "В космос", 0xFF_48_C9_B0),
            new DonationWheelPool.Segment("anvil", "Дождь наковален", 0xFF_95_A5_A6),
            new DonationWheelPool.Segment("pit", "Яма под ногами", 0xFF_5D_4E_37),
            new DonationWheelPool.Segment("lava", "Лава", 0xFF_E6_7E_22),
            new DonationWheelPool.Segment("spin", "Крутилка", 0xFF_34_98_DB),
            new DonationWheelPool.Segment("chicken", "Куриный ад", 0xFF_F1_C4_0F),
            new DonationWheelPool.Segment("prison", "Тюрьма", 0xFF_7F_8C_8D),
            new DonationWheelPool.Segment("creeperstorm", "Крипер-шторм", 0xFF_27_AE_60),
            new DonationWheelPool.Segment("ant", "Муравей", 0xFF_7F_8C_8D),
            new DonationWheelPool.Segment("titan", "Титан", 0xFF_E7_4C_3C),
            new DonationWheelPool.Segment("drop", "Выронил всё", 0xFF_B2_BE_B5),
            new DonationWheelPool.Segment("shuffle", "Хотбар в кашу", 0xFF_9B_59_B6),
            new DonationWheelPool.Segment("blind", "Слепота", 0xFF_2C_3E_50),
            new DonationWheelPool.Segment("meteor", "Метеор", 0xFF_C0_39_2B),
            new DonationWheelPool.Segment("tnt", "TNT на голову", 0xFF_E7_4C_3C)
    );

    private ChaosWheelPool() {}

    public static DonationWheelPool.SpinResult roll(TwitchCraftConfig config) {
        List<DonationWheelPool.Segment> available = new ArrayList<>();
        for (DonationWheelPool.Segment seg : PUNISH) {
            if (ChatGameCommand.fromId(seg.commandId())
                    .map(cmd -> config.getCommandSettings(cmd.getId(), cmd.getDefaultCooldown()).enabled)
                    .orElse(true)) {
                available.add(seg);
            }
        }
        if (available.size() < DonationWheelPool.WHEEL_SLOTS) {
            available = new ArrayList<>(PUNISH);
        }
        Collections.shuffle(available, ThreadLocalRandom.current());
        List<DonationWheelPool.Segment> picked = new ArrayList<>(
                available.subList(0, Math.min(DonationWheelPool.WHEEL_SLOTS, available.size())));
        while (picked.size() < DonationWheelPool.WHEEL_SLOTS) {
            picked.add(PUNISH.get(picked.size() % PUNISH.size()));
        }
        int winner = ThreadLocalRandom.current().nextInt(DonationWheelPool.WHEEL_SLOTS);
        return new DonationWheelPool.SpinResult(List.copyOf(picked), winner);
    }
}
