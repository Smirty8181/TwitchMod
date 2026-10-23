package net.twitchcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Переходы между экранами меню с возвратом назад. */
public final class MenuNavigation {
    private MenuNavigation() {}

    public static void openRules(Screen returnTo) {
        Minecraft.getInstance().setScreen(new RulesScreen(returnTo));
    }

    public static void openCommandCreator(Screen returnTo) {
        Minecraft.getInstance().setScreen(new CustomCommandCreatorScreen(returnTo));
    }

    public static void openSettings() {
        Minecraft.getInstance().setScreen(new SettingsScreen());
    }

    public static void openSetupWizard(Screen returnTo) {
        Minecraft.getInstance().setScreen(new SetupWizardScreen(returnTo));
    }

    public static void openSetupWizard() {
        openSetupWizard(null);
    }

    public static void confirmDiscard(Screen screen, Runnable onDiscard) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        onDiscard.run();
                    } else {
                        mc.setScreen(screen);
                    }
                },
                Component.literal("Выйти без сохранения?"),
                Component.literal("Несохранённые изменения будут потеряны. Нажмите «Сохранить» внизу справа.")
        ));
    }
}
