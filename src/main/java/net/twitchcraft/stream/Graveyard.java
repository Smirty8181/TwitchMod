package net.twitchcraft.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Graveyard {
    public record Entry(String viewer, String mobRu, long timeMs) {}

    private static final Graveyard INSTANCE = new Graveyard();
    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    private Graveyard() {}

    public static Graveyard get() {
        return INSTANCE;
    }

    public void add(String viewer, String mobRu) {
        entries.add(0, new Entry(viewer, mobRu, System.currentTimeMillis()));
        while (entries.size() > 10) entries.removeLast();
    }

    public List<Entry> recent(int count) {
        if (entries.isEmpty()) return List.of();
        return entries.subList(0, Math.min(count, entries.size()));
    }

    public List<Entry> exportEntries() {
        return new ArrayList<>(entries);
    }

    public void clear() {
        entries.clear();
    }

    public void restore(List<Entry> loaded) {
        entries.clear();
        if (loaded == null || loaded.isEmpty()) return;
        int n = Math.min(10, loaded.size());
        for (int i = 0; i < n; i++) {
            Entry e = loaded.get(i);
            if (e != null && e.viewer() != null) {
                entries.add(e);
            }
        }
    }
}
