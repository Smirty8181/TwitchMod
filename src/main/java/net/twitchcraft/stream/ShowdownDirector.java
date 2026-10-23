package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.twitchcraft.network.ShowdownPayload;

/** Один оверлей на экране: бомба > охота > ставка > король. */
public final class ShowdownDirector {
    private static boolean wasShowing;

    private ShowdownDirector() {}

    public static void tick(MinecraftServer server) {
        refresh(server);
    }

    public static void refresh(MinecraftServer server) {
        if (BombManager.get().isActive()) {
            BombManager.get().pushHud(server);
            wasShowing = true;
            return;
        }
        if (DuoTether.isActive()) {
            DuoTether.pushHud(server);
            wasShowing = true;
            return;
        }
        if (PartnerHold.isActive()) {
            PartnerHold.pushHud(server);
            wasShowing = true;
            return;
        }
        if (HuntManager.get().isActive()) {
            HuntManager.get().pushHud(server);
            wasShowing = true;
            return;
        }
        if (SizePrank.isActive()) {
            SizePrank.pushHud(server);
            wasShowing = true;
            return;
        }
        if (!SixSevenRitual.statusLine().isEmpty()) {
            SixSevenRitual.pushHud(server);
            wasShowing = true;
            return;
        }
        if (MorphManager.get().isActive()) {
            MorphManager.get().pushHud(server);
            wasShowing = true;
            return;
        }
        if (StreamerTaunt.get().isActive()) {
            StreamerTaunt.get().pushHud(server);
            wasShowing = true;
            return;
        }
        if (StreamerContract.get().isActive()) {
            StreamerContract.get().pushHud(server);
            wasShowing = true;
            return;
        }
        if (StreamerChallenge.get().isActive()) {
            StreamerChallenge.get().pushHud(server);
            wasShowing = true;
            return;
        }
        if (BetManager.get().isActive()) {
            BetManager.get().pushHud(server);
            wasShowing = true;
            return;
        }
        if (VoteManager.get().isActive()) {
            VoteManager.get().pushHud(server);
            wasShowing = true;
            return;
        }
        if (StreamerShield.isActive()) {
            StreamerShield.pushHud(server);
            wasShowing = true;
            return;
        }
        if (ChatKingManager.get().isActive()) {
            ChatKingManager.get().pushHud(server);
            wasShowing = true;
            return;
        }
        if (CipherRiddle.isActive()) {
            CipherRiddle.pushHud(server);
            wasShowing = true;
            return;
        }
        if (wasShowing) {
            ShowdownPayload.hide(server);
            wasShowing = false;
        }
    }
}
