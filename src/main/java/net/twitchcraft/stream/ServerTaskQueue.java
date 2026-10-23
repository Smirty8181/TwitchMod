package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Отложенные задачи на серверном тике. */
public final class ServerTaskQueue {
    private static final List<Pending> PENDING = new ArrayList<>();

    private record Pending(long runAtTick, Runnable task) {}

    private static long serverTick;

    private ServerTaskQueue() {}

    public static void schedule(MinecraftServer server, int delayTicks, Runnable task) {
        synchronized (PENDING) {
            PENDING.add(new Pending(serverTick + delayTicks, () -> server.execute(task)));
        }
    }

    public static void clear() {
        synchronized (PENDING) {
            PENDING.clear();
        }
        serverTick = 0;
    }

    public static void tick() {
        serverTick++;
        List<Runnable> due = new ArrayList<>();
        synchronized (PENDING) {
            Iterator<Pending> it = PENDING.iterator();
            while (it.hasNext()) {
                Pending p = it.next();
                if (serverTick >= p.runAtTick) {
                    due.add(p.task);
                    it.remove();
                }
            }
        }
        for (Runnable task : due) {
            task.run();
        }
    }
}
