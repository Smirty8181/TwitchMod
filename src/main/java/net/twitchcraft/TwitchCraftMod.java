package net.twitchcraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.twitchcraft.registry.ModEntities;
import net.twitchcraft.registry.ModItems;
import net.twitchcraft.command.TwitchCraftCommands;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.donation.DonationAlertsPoller;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.stream.BetManager;
import net.twitchcraft.stream.CourtManager;
import net.twitchcraft.stream.HuntManager;
import net.twitchcraft.stream.ChatMobBudget;
import net.twitchcraft.stream.GhostMobRegistry;
import net.twitchcraft.stream.Graveyard;
import net.twitchcraft.stream.StreamStats;
import net.twitchcraft.stream.StreamStateStore;
import net.twitchcraft.stream.StreamTargets;
import net.twitchcraft.stream.CommandChainTracker;
import net.twitchcraft.stream.DeathTracker;
import net.twitchcraft.stream.DeathGrace;
import net.twitchcraft.stream.LightningComboTracker;
import net.twitchcraft.stream.StreamTickHandler;
import net.twitchcraft.network.ConfigNetworking;
import net.twitchcraft.network.RulesNetworking;
import net.twitchcraft.network.ViewerStatsNetworking;
import net.twitchcraft.twitch.TwitchConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwitchCraftMod implements ModInitializer {
    public static final String MOD_ID = "twitchcraft";
    public static final String AUTHOR = "Smirty";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static TwitchConnectionManager connectionManager;
    private static DonationAlertsPoller donationPoller;

    @Override
    public void onInitialize() {
        ConfigManager.load();
        ModItems.register();
        ModEntities.register();
        ConfigNetworking.registerServer();
        RulesNetworking.registerServer();
        ViewerStatsNetworking.registerServer();
        net.twitchcraft.network.StreamerActionNetworking.registerServer();
        connectionManager = new TwitchConnectionManager();
        donationPoller = new DonationAlertsPoller();

        CommandRegistrationCallback.EVENT.register(TwitchCraftCommands::register);

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender == null || !StreamTargets.isTarget(sender)) return;
            var server = sender.level().getServer();
            if (server == null) return;
            String text = message.signedContent();
            if (text == null || text.isBlank()) {
                text = message.decoratedContent().getString();
            }
            if (text == null || text.isBlank()) return;
            net.twitchcraft.stream.UnnamedPatron.onStreamerText(server, text);
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
            if (!net.twitchcraft.stream.WorldEncounter.allowBreak(world, pos)) return false;
            return net.twitchcraft.stream.SpawnGuard.allowBreak(world, player, pos);
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            return net.twitchcraft.stream.WorldEncounter.onUseBlock(sp, hit.getBlockPos());
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            return net.twitchcraft.stream.WorldEncounter.onUseEntity(sp, entity);
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (!(player instanceof ServerPlayer sp) || !StreamTargets.isTarget(sp)) return;
            var block = state.getBlock();
            if (block == net.minecraft.world.level.block.Blocks.DIAMOND_ORE
                    || block == net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE
                    || block == net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS) {
                net.twitchcraft.stream.StreamerCharge.earn(sp.level().getServer(), sp, "находка", 1);
            }
            net.twitchcraft.stream.MilestoneNarrator.onBlockBreak(sp.level().getServer(), sp, block);
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer victim && StreamTargets.isTarget(victim)) {
                net.twitchcraft.stream.DuoTether.onDamage(victim, amount, source);
            }
            return true;
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TwitchCraftMod.LOGGER.info("TwitchCraft server started");
            StreamStateStore.load(server);
            net.twitchcraft.stream.StreamChronicle.open(server);
            StreamTickHandler.initBattle();
            if (ConfigManager.get().twitch.autoConnect) {
                connectionManager.connect(server);
            }
            if (ConfigManager.get().donations.enabled) {
                donationPoller.start(server);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            net.twitchcraft.stream.StreamChronicle.close();
            StreamStateStore.save(server);
            connectionManager.disconnect();
            donationPoller.stop();
            StreamTickHandler.onServerStop(server);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            StreamTickHandler.tick(server);
            StreamStateStore.tickAutosave();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> StreamTickHandler.hud().refreshPlayer(server, handler.getPlayer())));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                StreamTickHandler.hud().onPlayerDisconnect(handler.getPlayer()));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (handler.getPlayer() != null) {
                String name = handler.getPlayer().getGameProfile().name();
                LightningComboTracker.onPlayerDisconnect(name);
                CommandChainTracker.onPlayerDisconnect(name);
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            MinecraftServer server = entity.level().getServer();
            if (server == null) return;

            if (entity instanceof ServerPlayer player) {
                if (!StreamTargets.isTarget(player)) return;

                net.twitchcraft.stream.DeathCause cause = net.twitchcraft.stream.DeathCause.resolve(player, source);
                Entity killer = source.getEntity();
                String killerViewer = cause.hasKiller() ? cause.killer() : null;
                String killerMobRu = cause.detail();
                if (killer != null) {
                    GhostMobRegistry.GhostInfo info = GhostMobRegistry.get(killer.getUUID());
                    if (info != null) {
                        killerViewer = info.viewer();
                        killerMobRu = info.mobRu();
                        Graveyard.get().add(killerViewer, killerMobRu);
                        String msg = killerViewer + " победил стримера!";
                        StreamStats.forPlayer(player.getGameProfile().name()).addEvil();
                        AnnouncementService.announceCustom(player, "§c§l" + msg);
                    }
                }

                StreamTickHandler.battleBar().onStreamerDeath(player);
                net.twitchcraft.stream.DuoTether.onStreamerDeath(server, player);
                HuntManager.get().onStreamerDeath(server, player);
                BetManager.get().onStreamerDeath(server, player);
                net.twitchcraft.stream.StreamerContract.get().onStreamerDeath(server, player);
                net.twitchcraft.stream.StreamerTaunt.get().onStreamerDeath(server, player);
                net.twitchcraft.stream.StreamerCharge.onStreamerDeath();
                if (cause.fromChat()) {
                    net.twitchcraft.stream.MilestoneNarrator.onDeath(server, player, cause.killer(), cause.detail());
                } else {
                    net.twitchcraft.stream.MilestoneNarrator.onFirstDeath(server, player);
                }
                DeathTracker.onDeath(player, cause);
                DeathGrace.onDeath(server, player);
                net.twitchcraft.stream.PartnerHold.onStreamerDeath(server, player);
                if (ConfigManager.get().stream.revengeOnDeath) {
                    CourtManager.get().start(server, player, killerViewer, killerMobRu);
                }
            } else if (entity instanceof LivingEntity living && entity.level() instanceof ServerLevel level) {
                net.twitchcraft.stream.WorldEncounter.onGuestDeath(living);
                ChatMobBudget.forget(living.getUUID());
                GhostMobRegistry.onMobDeath(living, source, level);
                if (source.getEntity() instanceof ServerPlayer killer) {
                    StreamTickHandler.battleBar().onGhostKilled(killer);
                    net.twitchcraft.stream.StreamerCharge.onStreamerKill(server, killer, living);
                    net.twitchcraft.stream.CampaignJourney.onBossKill(server, killer, living);
                }
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) return;
            DeathTracker.onRespawnIfMissed(newPlayer);
            DeathGrace.onRespawn(newPlayer);
        });
    }

    public static TwitchConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public static DonationAlertsPoller getDonationPoller() {
        return donationPoller;
    }
}
