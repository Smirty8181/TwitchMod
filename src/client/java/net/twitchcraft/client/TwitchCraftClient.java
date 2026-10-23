package net.twitchcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.server.packs.PackType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.twitchcraft.client.render.ChatPhantomRenderer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.registry.ModEntities;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.LocalProfileStore;
import net.twitchcraft.config.StreamerProfile;
import net.twitchcraft.network.NetworkRegistration;
import org.lwjgl.glfw.GLFW;

public class TwitchCraftClient implements ClientModInitializer {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "main"));
    public static KeyMapping OPEN_CONFIG_KEY;
    public static KeyMapping OPEN_RULES_KEY;
    public static KeyMapping OPEN_COMMAND_CREATOR_KEY;
    public static KeyMapping OPEN_VIEWER_STATS_KEY;
    public static KeyMapping OPEN_TEST_LAB_KEY;
    public static KeyMapping PARTNER_PING_KEY;
    private static boolean wizardPromptedThisSession;

    @Override
    public void onInitializeClient() {
        NetworkRegistration.registerPlayS2C();
        NetworkRegistration.registerPlayC2S();
        ConfigManager.load();
        HudClientStore.load();
        TwitchMenuScale.load();
        UiTheme.applyFromConfig();
        migrateWizardFlagIfNeeded();
        DonationClientNetworking.register();
        ComboChainClientNetworking.register();
        DeathClientNetworking.register();
        CommandFeedClientNetworking.register();
        CommandDenyClientNetworking.register();
        SidebarClientNetworking.register();
        RulesClientNetworking.register();
        TtsClientNetworking.register();
        ViewerStatsClient.register();
        ShowdownClientNetworking.register();
        DiceClientNetworking.register();
        DuoScoreClientNetworking.register();
        PartnerPingClientNetworking.register();
        WorldDropClientNetworking.register();
        PartnerPresetClientNetworking.register();
        RagdollClientNetworking.register();
        InvertControlsClientNetworking.register();
        UpgradeClientNetworking.register();
        HotbarShuffleClientNetworking.register();
        EntityRendererRegistry.register(ModEntities.CHAT_PHANTOM, ChatPhantomRenderer::new);

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> TwitchTextureLoader.ensureAll());
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "textures");
            }

            @Override
            public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
                TwitchTextureLoader.reload();
            }
        });

        ResourceLocation diceHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "dice");
        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR, diceHud,
                (context, tickCounter) -> DiceOverlay.render(context, tickCounter.getGameTimeDeltaPartialTick(true)));

        ResourceLocation showdownHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "showdown");
        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR, showdownHud,
                (context, tickCounter) -> ShowdownOverlay.render(context, tickCounter.getGameTimeDeltaPartialTick(true)));

        ResourceLocation comboHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "combo_chain");
        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR, comboHud,
                (context, tickCounter) -> ComboChainOverlay.render(context, tickCounter.getGameTimeDeltaPartialTick(true)));

        ResourceLocation screenFxHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "screen_fx");
        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR, screenFxHud,
                (context, tickCounter) -> CommandScreenFx.render(context));

        ResourceLocation cinematicHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "cinematic_boss");
        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR, cinematicHud,
                (context, tickCounter) -> CinematicBossOverlay.render(context, tickCounter.getGameTimeDeltaPartialTick(true)));

        ResourceLocation deathHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "death_alert");
        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR, deathHud,
                (context, tickCounter) -> DeathOverlay.render(context, tickCounter.getGameTimeDeltaPartialTick(true)));

        ResourceLocation pinnedDeathHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "death_coords_pin");
        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR, pinnedDeathHud,
                (context, tickCounter) -> DeathCoordsPinOverlay.render(context, tickCounter.getGameTimeDeltaPartialTick(true)));

        ResourceLocation commandFeedHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "command_feed");
        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR, commandFeedHud,
                (context, tickCounter) -> CommandFeedOverlay.render(context, tickCounter.getGameTimeDeltaPartialTick(true)));

        ResourceLocation hypeTickerHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "hype_ticker");
        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR, hypeTickerHud,
                (context, tickCounter) -> HypeTickerOverlay.render(context, tickCounter.getGameTimeDeltaPartialTick(true)));

        ResourceLocation commandDenyHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "command_deny");
        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR, commandDenyHud,
                (context, tickCounter) -> CommandDenyOverlay.render(context, tickCounter.getGameTimeDeltaPartialTick(true)));

        ResourceLocation duoScoreHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "duo_score");
        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR, duoScoreHud,
                (context, tickCounter) -> DuoScoreOverlay.render(context, tickCounter.getGameTimeDeltaPartialTick(true)));

        ResourceLocation partnerPingHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "partner_ping");
        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR, partnerPingHud,
                (context, tickCounter) -> PartnerPingOverlay.render(context, tickCounter.getGameTimeDeltaPartialTick(true)));

        ResourceLocation worldDropHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "world_drop");
        HudElementRegistry.attachElementBefore(VanillaHudElements.BOSS_BAR, worldDropHud,
                (context, tickCounter) -> WorldDropOverlay.render(context, tickCounter.getGameTimeDeltaPartialTick(true)));

        ResourceLocation donationFx = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "donation_fx");
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, donationFx,
                (context, tickCounter) -> DonationScreenFx.render(context));

        ResourceLocation donationHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "donation_toast");
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, donationHud,
                (context, tickCounter) -> DonationOverlay.render(context, tickCounter.getGameTimeDeltaPartialTick(true)));

        ResourceLocation wheelHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "donation_wheel");
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, wheelHud,
                (context, tickCounter) -> DonationWheelOverlay.render(context, tickCounter.getGameTimeDeltaPartialTick(true)));

        ResourceLocation upgradeHud = ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "upgrade");
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, upgradeHud,
                (context, tickCounter) -> UpgradeOverlay.renderHud(context, tickCounter.getGameTimeDeltaPartialTick(true)));

        OPEN_CONFIG_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.twitchcraft.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                CATEGORY
        ));

        OPEN_RULES_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.twitchcraft.rules",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                CATEGORY
        ));

        OPEN_COMMAND_CREATOR_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.twitchcraft.command_creator",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                CATEGORY
        ));

        OPEN_VIEWER_STATS_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.twitchcraft.viewer_stats",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                CATEGORY
        ));

        OPEN_TEST_LAB_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.twitchcraft.test_lab",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                CATEGORY
        ));

        PARTNER_PING_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.twitchcraft.partner_ping",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            RagdollClient.tick();
            if (client.player != null && client.level != null) {
                SidebarOverlay.clientTick();
                maybeOpenSetupWizard(client);
            }
            while (OPEN_CONFIG_KEY.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new SettingsScreen());
                }
            }
            while (OPEN_RULES_KEY.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new RulesScreen());
                }
            }
            while (OPEN_COMMAND_CREATOR_KEY.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new CustomCommandCreatorScreen());
                }
            }
            while (OPEN_VIEWER_STATS_KEY.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new ViewerStatsScreen());
                } else if (client.screen instanceof ViewerStatsScreen) {
                    client.setScreen(null);
                }
            }
            while (OPEN_TEST_LAB_KEY.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new TestLabScreen());
                } else if (client.screen instanceof TestLabScreen) {
                    client.setScreen(null);
                }
            }
            while (PARTNER_PING_KEY.consumeClick()) {
                if (client.screen == null && client.player != null && client.getConnection() != null) {
                    StreamerActionClient.send("ping");
                }
            }
        });
    }

    /** Уже настроенные профили не гоняем через мастер повторно. */
    private static void migrateWizardFlagIfNeeded() {
        StreamerProfile p = LocalProfileStore.load();
        if (!p.setupWizardCompleted && p.hasTwitch()) {
            p.setupWizardCompleted = true;
            LocalProfileStore.save(p);
        }
    }

    private static void maybeOpenSetupWizard(net.minecraft.client.Minecraft client) {
        if (wizardPromptedThisSession || client.screen != null) return;
        StreamerProfile p = LocalProfileStore.load();
        if (p.setupWizardCompleted || p.hasTwitch()) {
            wizardPromptedThisSession = true;
            return;
        }
        wizardPromptedThisSession = true;
        client.setScreen(new SetupWizardScreen());
    }
}
