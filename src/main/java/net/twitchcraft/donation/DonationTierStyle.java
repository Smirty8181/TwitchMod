package net.twitchcraft.donation;

/** Цвета алертов по tier доната. */
public final class DonationTierStyle {
    private DonationTierStyle() {}

    public static int colorForAmount(int minAmount) {
        if (minAmount >= 1000) return 0xFF_5B_2C_6F;
        if (minAmount >= 500) return 0xFF_FF_44_44;
        if (minAmount >= 200) return 0xFF_BB_44_FF;
        if (minAmount >= 100) return 0xFF_FF_88_44;
        return 0xFF_FF_D7_00;
    }

    public static String colorCodeForAmount(int minAmount) {
        if (minAmount >= 1000) return "§5";
        if (minAmount >= 500) return "§c";
        if (minAmount >= 200) return "§d";
        if (minAmount >= 100) return "§6";
        return "§e";
    }

    /** Tier от 500₽ — рулетка на экране вместо мгновенного эффекта. */
    public static boolean usesWheel(int minAmount) {
        return minAmount >= 500;
    }
}
