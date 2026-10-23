package net.twitchcraft.donation;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/** Последние донаты для HUD (не сохраняется в JSON). */
public final class DonationState {
    private static volatile String lastDonor = "-";
    private static volatile String lastAmount = "";
    private static volatile String lastLabel = "";
    private static volatile String lastTarget = "";
    private static volatile boolean connected;
    private static volatile String lastError = "";

    private static final ConcurrentHashMap<String, TargetSnapshot> BY_TARGET = new ConcurrentHashMap<>();

    private record TargetSnapshot(String donor, String amount, String label) {}

    private DonationState() {}

    public static void record(String donor, String amount, String currency, String label, String targetPlayer) {
        lastDonor = donor;
        lastAmount = amount + currency;
        lastLabel = label;
        lastTarget = targetPlayer;
        if (targetPlayer != null && !targetPlayer.isBlank()) {
            BY_TARGET.put(targetPlayer.toLowerCase(Locale.ROOT),
                    new TargetSnapshot(donor, amount + currency, label));
        }
    }

    public static void setConnected(boolean value) {
        connected = value;
        if (value) lastError = "";
    }

    public static void setLastError(String message) {
        lastError = message != null ? message : "";
    }

    public static boolean isConnected() {
        return connected;
    }

    public static String lastError() {
        return lastError;
    }

    public static String lastDonor() { return lastDonor; }

    public static String lastAmount() { return lastAmount; }

    public static String lastLabel() { return lastLabel; }

    public static String lastTarget() { return lastTarget; }

    public static String lastDonorFor(String minecraftName) {
        TargetSnapshot snap = snapshotFor(minecraftName);
        return snap != null ? snap.donor : lastDonor;
    }

    public static String lastAmountFor(String minecraftName) {
        TargetSnapshot snap = snapshotFor(minecraftName);
        return snap != null ? snap.amount : lastAmount;
    }

    private static TargetSnapshot snapshotFor(String minecraftName) {
        if (minecraftName == null || minecraftName.isBlank()) return null;
        return BY_TARGET.get(minecraftName.toLowerCase(Locale.ROOT));
    }
}
