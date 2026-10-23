package net.twitchcraft.client;

import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.TwitchCraftConfig;

/** Цветовые темы — минималистичные градиенты, аниме-дефолт. */
public final class UiTheme {
    public enum Id {
        ANIME("anime", "Аниме"),
        DARK("dark", "Тёмная"),
        PURPLE("purple", "Фиолетовая"),
        RED("red", "Красная"),
        WHITE("white", "Белая"),
        GREEN("green", "Зелёная"),
        NEON("neon", "Неон");

        private final String key;
        private final String label;

        Id(String key, String label) {
            this.key = key;
            this.label = label;
        }

        public String key() { return key; }
        public String label() { return label; }

        public static Id fromKey(String key) {
            if (key == null || key.isBlank()) return ANIME;
            for (Id id : values()) {
                if (id.key.equalsIgnoreCase(key)) return id;
            }
            return ANIME;
        }
    }

    public record Palette(
            int accent,
            int accentDark,
            int accentLight,
            int accentAlt,
            int bgPanel,
            int bgCard,
            int bgTab,
            int bgTabActive,
            int border,
            int text,
            int textDim,
            int hudEdge,
            int saveBtn,
            int saveBtnHover,
            String titleFormat
    ) {}

    private static Palette current = palette(Id.ANIME);

    private UiTheme() {}

    public static Palette get() {
        return current;
    }

    public static void applyFromConfig() {
        apply(ConfigManager.get());
    }

    public static void apply(TwitchCraftConfig config) {
        String key = config != null && config.ui != null ? config.ui.theme : "anime";
        apply(Id.fromKey(key));
    }

    public static void apply(Id id) {
        current = palette(id);
        TwitchUi.syncTheme(current);
    }

    public static Palette palette(Id id) {
        return switch (id) {
            // Сакура + чернила + мягкий циан — чистый аниме HUD
            case ANIME -> new Palette(
                    0xFF_FF_8E_B4, 0xFF_C4_38_78, 0xFF_FF_D2_E6, 0xFF_6E_D8_FF,
                    0xF8_06_07_0E, 0xEE_0C_0E_18, 0xD8_09_0B_14, 0xF4_16_12_24,
                    0xFF_34_2E_42, 0xFF_FB_F7_FC, 0xFF_A8_A0_B4,
                    0x70_FF_8E_B4, 0xFF_E0_52_8C, 0xFF_F4_72_A4,
                    "§d§lTwitch§b§lCraft");
            case DARK -> new Palette(
                    0xFF_C8_C4_D0, 0xFF_4A_48_55, 0xFF_EE_EC_F4, 0xFF_88_B8_D8,
                    0xF6_0A_0A_0E, 0xEA_14_14_18, 0xD8_10_10_14, 0xEC_1C_1C_22,
                    0xFF_3A_3A_42, 0xFF_F2_F0_F6, 0xFF_92_90_A0,
                    0x55_A0_A0_B0, 0xFF_3A_3A_44, 0xFF_5A_5A_66,
                    "§7§lTwitch§f§lCraft");
            case RED -> new Palette(
                    0xFF_FF_5A_6E, 0xFF_A0_28_38, 0xFF_FF_A0_B0, 0xFF_FF_B0_70,
                    0xF6_10_08_0C, 0xEA_1C_10_14, 0xD8_16_0C_10, 0xEC_2A_14_1A,
                    0xFF_4A_2A_32, 0xFF_F8_F2_F4, 0xFF_B8_90_98,
                    0x55_FF_5A_6E, 0xFF_B0_30_40, 0xFF_D0_48_58,
                    "§c§lTwitch§f§lCraft");
            case WHITE -> new Palette(
                    0xFF_FF_6B_9D, 0xFF_E0_A0_B8, 0xFF_FF_D0_E0, 0xFF_7A_C8_E8,
                    0xF4_F4_F0_F6, 0xEC_EE_EA_F2, 0xE4_E4_E0_EA, 0xFF_FF_FF_FF,
                    0xFF_D8_D0_DC, 0xFF_22_20_28, 0xFF_6A_64_74,
                    0x44_FF_6B_9D, 0xFF_E8_88_A8, 0xFF_FF_A0_BC,
                    "§d§lTwitch§0§lCraft");
            case GREEN -> new Palette(
                    0xFF_4A_E8_A0, 0xFF_1E_8A_58, 0xFF_A0_F0_C8, 0xFF_7A_D8_FF,
                    0xF6_08_10_0E, 0xEA_0E_18_14, 0xD8_0A_14_10, 0xEC_14_28_1C,
                    0xFF_2A_4A_3A, 0xFF_F2_F8_F4, 0xFF_88_B8_A0,
                    0x55_4A_E8_A0, 0xFF_24_9A_62, 0xFF_3A_C0_7A,
                    "§a§lTwitch§f§lCraft");
            case PURPLE -> new Palette(
                    0xFF_B4_7A_FF, 0xFF_6A_2A_B0, 0xFF_D8_B8_FF, 0xFF_7A_D4_FF,
                    0xF6_0C_0A_14, 0xEA_14_12_1E, 0xD8_10_0E_18, 0xEC_24_18_38,
                    0xFF_3A_32_4A, 0xFF_F6_F2_FA, 0xFF_A8_9E_C0,
                    0x55_B4_7A_FF, 0xFF_7A_3A_C0, 0xFF_9A_5A_E0,
                    "§d§lTwitch§f§lCraft");
            case NEON -> new Palette(
                    0xFF_00_F0_D0, 0xFF_00_88_90, 0xFF_80_FF_EE, 0xFF_FF_60_C8,
                    0xF6_04_0A_10, 0xEA_08_12_18, 0xD8_06_0E_14, 0xEC_0C_1E_28,
                    0xFF_1A_3A_44, 0xFF_E8_FF_FC, 0xFF_78_B0_B8,
                    0x55_00_F0_D0, 0xFF_00_88_98, 0xFF_00_C0_C8,
                    "§b§lTwitch§d§lCraft");
        };
    }
}
