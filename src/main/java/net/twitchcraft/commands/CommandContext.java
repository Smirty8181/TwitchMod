package net.twitchcraft.commands;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CommandContext {
    public final MinecraftServer server;
    public final ServerPlayer target;
    public final String twitchUser;
    public final String rawMessage;
    public final ChatGameCommand command;
    public final boolean moderatorTriggered;
    private boolean failed;

    public CommandContext(MinecraftServer server, ServerPlayer target, String twitchUser,
                          String rawMessage, ChatGameCommand command, boolean moderatorTriggered) {
        this.server = server;
        this.target = target;
        this.twitchUser = twitchUser;
        this.rawMessage = rawMessage;
        this.command = command;
        this.moderatorTriggered = moderatorTriggered;
    }

    public String viewer() {
        return twitchUser == null || twitchUser.isBlank() ? "чат" : twitchUser;
    }

    public void fail() {
        this.failed = true;
    }

    public boolean failed() {
        return failed;
    }
}
