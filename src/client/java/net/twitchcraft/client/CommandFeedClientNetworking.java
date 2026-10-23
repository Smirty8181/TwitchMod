package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.EvilCommands;
import net.twitchcraft.commands.GoodCommands;
import net.twitchcraft.network.CommandAlertPayload;

public final class CommandFeedClientNetworking {
    private static boolean registered;

    private CommandFeedClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(CommandAlertPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> {
                    TwitchAvatarCache.request(payload.twitchUser());
                    if (HypeTickerOverlay.isHype(payload.commandId())) {
                        HypeTickerOverlay.push(payload.twitchUser(), payload.commandRu(),
                                payload.commandId(), payload.nickColor());
                    } else {
                        CommandFeedOverlay.push(payload.twitchUser(), payload.commandRu(),
                                payload.commandId(), payload.nickColor(), payload.cooldownSec());
                    }
                    triggerFx(payload.commandId(), payload.twitchUser(), payload.commandRu());
                }));
        registered = true;
    }

    private static long lastFxMs;

    private static void triggerFx(String commandId, String twitchUser, String commandRu) {
        long now = System.currentTimeMillis();
        boolean flood = now - lastFxMs < 420;
        lastFxMs = now;
        if (!flood) {
            CommandScreenFx.triggerVignette(vignetteColor(commandId), 420);
        }
        if (CinematicBossOverlay.isRare(commandId)) {
            CinematicBossOverlay.trigger(commandId, twitchUser);
            return;
        }
        if ("jumpscare".equals(commandId)) {
            CommandScreenFx.triggerJumpscare();
            return;
        }
        if (flood) return;
        if ("raid".equals(commandId)) {
            CommandScreenFx.triggerEvil(2, 900);
            return;
        }
        if ("firstmsg".equals(commandId) || "follow".equals(commandId) || "sub".equals(commandId)) {
            CommandScreenFx.triggerGood(1100);
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getWindow() != null) {
                ConfettiFx.burstGood(HudRails.left() + HudLayout.s(48),
                        mc.getWindow().getGuiScaledHeight() - HudRails.hotbar() - HudLayout.s(18), 16);
            }
            return;
        }
        if (!HudResolver.enabled(HudResolver.Element.COMMAND_FX)) return;
        var feed = HudResolver.feedSections();
        if (!feed.screenFx) return;
        var cmd = ChatGameCommand.fromId(commandId);
        if ("morph".equals(commandId) || (commandRu != null && commandRu.contains("преврат"))) {
            CommandScreenFx.triggerMorph();
            return;
        }
        if ("ant".equals(commandId) || "titan".equals(commandId) || "sixseven".equals(commandId)) {
            CommandScreenFx.triggerMorph();
            return;
        }
        if (cmd.isPresent() && GoodCommands.isGood(cmd.get())) {
            CommandScreenFx.triggerGood(1100);
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getWindow() != null) {
                ConfettiFx.burstGood(HudRails.left() + HudLayout.s(48),
                        mc.getWindow().getGuiScaledHeight() - HudRails.hotbar() - HudLayout.s(18), 16);
            }
            return;
        }
        if (cmd.isPresent() && EvilCommands.isEvil(cmd.get())) {
            CommandScreenFx.triggerEvil(1, 620);
            return;
        }
        if (cmd.isEmpty()) {
            CommandScreenFx.triggerEvil(1, 420);
        }
    }

    private static int vignetteColor(String commandId) {
        if ("follow".equals(commandId) || "sub".equals(commandId) || "firstmsg".equals(commandId)) {
            return 0xE87AD0;
        }
        if ("raid".equals(commandId) || "miss".equals(commandId)) {
            return 0xE74C3C;
        }
        return ChatGameCommand.fromId(commandId)
                .map(cmd -> switch (EvilCommands.harm(cmd)) {
                    case GOOD -> 0x2ECC71;
                    case DAMAGE -> 0xE74C3C;
                    case PRANK -> 0xF1C40F;
                    case MOD -> 0x9B59B6;
                })
                .orElseGet(() -> GoodCommands.isGood(ChatGameCommand.fromId(commandId).orElse(null))
                        ? 0x2ECC71 : 0x9B59B6);
    }
}
