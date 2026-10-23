package net.twitchcraft.donation;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.stream.StreamTargets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Донаты, которые нельзя было сразу применить (стример оффлайн).
 * Повторяем на серверном тике, чтобы не терять платежи после проставления lastDonationId.
 */
public final class PendingDonationQueue {
    private static final int MAX_PENDING = 64;
    private static final int RETRY_INTERVAL_TICKS = 40;
    private static final long MAX_AGE_MS = 30 * 60 * 1000L;

    private record Pending(String preferredTarget, String donor, double amount, String currency,
                           String message, long enqueuedAtMs) {}

    private static final List<Pending> QUEUE = new CopyOnWriteArrayList<>();
    private static int tickCounter;

    private PendingDonationQueue() {}

    public static void enqueue(String preferredTarget, String donor, double amount,
                               String currency, String message) {
        while (QUEUE.size() >= MAX_PENDING) {
            Pending dropped = QUEUE.removeFirst();
            TwitchCraftMod.LOGGER.warn("Pending donations overflow — dropped {} {} from {}",
                    dropped.amount, dropped.currency, dropped.donor);
        }
        QUEUE.add(new Pending(
                preferredTarget != null ? preferredTarget : "",
                donor,
                amount,
                currency != null ? currency : "RUB",
                message != null ? message : "",
                System.currentTimeMillis()));
        TwitchCraftMod.LOGGER.info("Donation queued (offline): {} {} from {} → '{}'",
                amount, currency, donor, preferredTarget);
    }

    public static int size() {
        return QUEUE.size();
    }

    public static void clear() {
        QUEUE.clear();
        tickCounter = 0;
    }

    public static void tick(MinecraftServer server, Consumer<PendingDonation> deliver) {
        if (QUEUE.isEmpty()) return;
        tickCounter++;
        if (tickCounter % RETRY_INTERVAL_TICKS != 0) return;

        long now = System.currentTimeMillis();
        List<Pending> ready = new ArrayList<>();
        Iterator<Pending> it = QUEUE.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (now - p.enqueuedAtMs > MAX_AGE_MS) {
                TwitchCraftMod.LOGGER.warn("Pending donation expired: {} {} from {}",
                        p.amount, p.currency, p.donor);
                QUEUE.remove(p);
                continue;
            }
            if (resolveOnline(server, p.preferredTarget).isPresent()) {
                ready.add(p);
                QUEUE.remove(p);
            }
        }
        for (Pending p : ready) {
            deliver.accept(new PendingDonation(p.preferredTarget, p.donor, p.amount, p.currency, p.message));
        }
    }

    private static java.util.Optional<ServerPlayer> resolveOnline(MinecraftServer server, String preferred) {
        if (preferred != null && !preferred.isBlank()) {
            var opt = StreamTargets.resolve(server, preferred);
            if (opt.isPresent()) return opt;
        }
        return StreamTargets.resolve(server);
    }

    public record PendingDonation(String preferredTarget, String donor, double amount,
                                  String currency, String message) {}
}
