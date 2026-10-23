package net.twitchcraft.stream;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.UpgradePayload;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * !апрейд — как upgrader.vip: вложил предмет, крутится шанс, апгрейд или сгорел.
 */
public final class ItemUpgrade {
    public static final int PICK_SEC = 12;
    public static final int SPIN_MS = 2800;
    public static final int HOLD_MS = 2600;

    private static UUID targetId;
    private static String viewer = "";
    private static String phase = "";
    private static int selectedSlot = -1;
    private static long pickUntil;
    private static long applyAt;
    private static long hideAt;
    private static boolean won;
    private static int landAngle;
    private static int chance;
    private static ItemStack stake = ItemStack.EMPTY;
    private static ItemStack preview = ItemStack.EMPTY;

    private ItemUpgrade() {}

    public static boolean isActive() {
        return targetId != null && !phase.isBlank() && !"hide".equals(phase);
    }

    public static String statusLine() {
        if (!isActive()) return "";
        if ("pick".equals(phase)) {
            int left = (int) Math.max(0, (pickUntil - System.currentTimeMillis() + 999) / 1000);
            return "§b✦ АПРГЕЙД §f" + left + "с §7вложи предмет";
        }
        if ("spin".equals(phase)) return "§b✦ АПРГЕЙД §eкрутится…";
        return won ? "§a✦ АПРГЕЙД!" : "§c✦ ПРОВАЛ";
    }

    public static boolean start(MinecraftServer server, ServerPlayer player, String twitchUser) {
        if (player == null) return false;
        if (isActive()) {
            AnnouncementService.announceCustom(player, "§7Апгрейд уже идёт");
            return false;
        }
        targetId = player.getUUID();
        viewer = twitchUser == null || twitchUser.isBlank() ? "чат" : twitchUser;
        phase = "pick";
        selectedSlot = player.getInventory().getSelectedSlot();
        pickUntil = System.currentTimeMillis() + PICK_SEC * 1000L;
        applyAt = 0;
        hideAt = 0;
        won = false;
        landAngle = 0;
        chance = 0;
        refreshPreview(player);
        push(server);
        AnnouncementService.announceCustom(player,
                "§b§lАПРГЕЙД! §7Кликни предмет в меню · " + PICK_SEC + "с · от §f" + viewer);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                viewer + " открыл АПРГЕЙД! Стример вкладывает предмет — как на upgrader.");
        return true;
    }

    public static void pickSlot(ServerPlayer player, int slot) {
        if (!isActive() || !"pick".equals(phase) || player == null) return;
        if (!player.getUUID().equals(targetId)) return;
        if (slot < 0 || slot > 40) return;
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.isEmpty()) {
            AnnouncementService.announceCustom(player, "§7Слот пустой");
            return;
        }
        selectedSlot = slot;
        refreshPreview(player);
        push(player.level().getServer());
    }

    public static void confirm(ServerPlayer player) {
        if (!isActive() || !"pick".equals(phase) || player == null) return;
        if (!player.getUUID().equals(targetId)) return;
        beginSpin(player.level().getServer(), player);
    }

    public static void tick(MinecraftServer server) {
        if (!isActive()) return;
        ServerPlayer player = server.getPlayerList().getPlayer(targetId);
        if (player == null) {
            stop(server);
            return;
        }
        long now = System.currentTimeMillis();
        if ("pick".equals(phase) && now >= pickUntil) {
            beginSpin(server, player);
            return;
        }
        if ("spin".equals(phase) && now >= applyAt) {
            applyResult(server, player);
            return;
        }
        if (("win".equals(phase) || "lose".equals(phase)) && now >= hideAt) {
            stop(server);
        }
    }

    private static void beginSpin(MinecraftServer server, ServerPlayer player) {
        refreshPreview(player);
        if (stake.isEmpty() || preview.isEmpty()) {
            AnnouncementService.announceCustom(player, "§7Нечего апгрейдить — вложи предмет");
            TwitchCraftMod.getConnectionManager().sendChatMessage("Апгрейд отменён: слот пустой.");
            stop(server);
            return;
        }
        var step = ItemUpgradeLadder.of(stake);
        boolean jackpot = step == null && ItemUpgradeLadder.neverDestroy(stake);
        if (step == null && !jackpot) {
            AnnouncementService.announceCustom(player, "§7Этот предмет нельзя апгрейднуть");
            stop(server);
            return;
        }
        chance = jackpot ? 18 : step.chance();
        won = ThreadLocalRandom.current().nextInt(100) < chance;
        if (won) {
            landAngle = ThreadLocalRandom.current().nextInt(Math.max(1, Math.round(360f * chance / 100f)));
        } else {
            int loseArc = Math.max(1, 360 - Math.round(360f * chance / 100f));
            landAngle = Math.round(360f * chance / 100f) + ThreadLocalRandom.current().nextInt(loseArc);
        }
        landAngle = landAngle % 360;
        phase = "spin";
        applyAt = System.currentTimeMillis() + SPIN_MS;
        hideAt = applyAt + HOLD_MS;
        push(server);
        ServerLevel level = player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
                SoundSource.PLAYERS, 0.9f, 0.8f);
    }

    private static void applyResult(MinecraftServer server, ServerPlayer player) {
        ItemStack slotStack = selectedSlot >= 0 ? player.getInventory().getItem(selectedSlot) : ItemStack.EMPTY;
        if (slotStack.isEmpty() || !ItemStack.isSameItem(slotStack, stake)) {
            // предмет успели выкинуть — ищем копию
            selectedSlot = findSimilar(player, stake);
            slotStack = selectedSlot >= 0 ? player.getInventory().getItem(selectedSlot) : ItemStack.EMPTY;
        }
        ServerLevel level = player.level();
        if (won) {
            ItemStack result = preview.copy();
            if (ItemUpgradeLadder.neverDestroy(stake) && ItemUpgradeLadder.of(stake) == null) {
                result = ItemUpgradeLadder.jackpotKeep(stake);
            }
            consumeOne(player, selectedSlot, slotStack);
            give(player, result);
            phase = "win";
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1f, 1.2f);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.2, player.getZ(), 28, 0.4, 0.6, 0.4, 0.12);
            AnnouncementService.announceCustom(player,
                    "§a§lАПРГЕЙД! §f" + ItemUpgradeLadder.nameOf(stake)
                            + " §7→ §b" + ItemUpgradeLadder.nameOf(result));
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    "АПРГЕЙД! " + ItemUpgradeLadder.nameOf(stake) + " → " + ItemUpgradeLadder.nameOf(result)
                            + " (" + chance + "%) от " + viewer);
        } else {
            boolean keep = ItemUpgradeLadder.neverDestroy(stake)
                    || (ItemUpgradeLadder.of(stake) != null && ItemUpgradeLadder.of(stake).protectedOnFail());
            if (!keep) consumeOne(player, selectedSlot, slotStack);
            phase = "lose";
            level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND,
                    SoundSource.PLAYERS, 0.8f, 0.55f);
            level.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 1, player.getZ(), 18, 0.3, 0.4, 0.3, 0.02);
            String msg = keep
                    ? "§cПровалил апгрейд §7— предмет на месте"
                    : "§c§lСГОРЕЛ! §7" + ItemUpgradeLadder.nameOf(stake) + " §8уничтожен";
            AnnouncementService.announceCustom(player, msg);
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    keep
                            ? viewer + " не прокнул апгрейд (" + chance + "%), предмет цел."
                            : "ПРОВАЛ! " + ItemUpgradeLadder.nameOf(stake) + " сгорел. Шанс был " + chance + "%.");
        }
        push(server);
    }

    private static void consumeOne(ServerPlayer player, int slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (slot >= 0 && slot <= 40 && ItemStack.isSameItem(player.getInventory().getItem(slot), stack)) {
            player.getInventory().getItem(slot).shrink(1);
        } else {
            stack.shrink(1);
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (selectedSlot >= 0 && selectedSlot <= 40 && player.getInventory().getItem(selectedSlot).isEmpty()) {
            player.getInventory().setItem(selectedSlot, stack);
        } else if (!player.getInventory().add(stack)) {
            ItemEntity drop = player.drop(stack, false);
            if (drop != null) drop.setNoPickUpDelay();
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static int findSimilar(ServerPlayer player, ItemStack sample) {
        if (sample == null || sample.isEmpty()) return -1;
        var inv = player.getInventory();
        for (int i = 0; i < 41; i++) {
            if (ItemStack.isSameItem(inv.getItem(i), sample)) return i;
        }
        return -1;
    }

    private static void refreshPreview(ServerPlayer player) {
        if (selectedSlot < 0 || selectedSlot > 40) selectedSlot = player.getInventory().getSelectedSlot();
        ItemStack stack = player.getInventory().getItem(selectedSlot);
        if (stack.isEmpty()) {
            int hand = player.getInventory().getSelectedSlot();
            stack = player.getInventory().getItem(hand);
            if (!stack.isEmpty()) selectedSlot = hand;
        }
        stake = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (!stake.isEmpty()) stake.setCount(1);
        var step = ItemUpgradeLadder.of(stake);
        if (step == null && ItemUpgradeLadder.neverDestroy(stake)) {
            preview = ItemUpgradeLadder.jackpotKeep(stake);
            chance = 18;
            return;
        }
        if (step == null) {
            preview = ItemStack.EMPTY;
            chance = 0;
        } else {
            preview = ItemUpgradeLadder.preview(stake);
            chance = step.chance();
        }
    }

    private static void stop(MinecraftServer server) {
        phase = "hide";
        push(server);
        targetId = null;
        viewer = "";
        phase = "";
        selectedSlot = -1;
        stake = ItemStack.EMPTY;
        preview = ItemStack.EMPTY;
    }

    private static void push(MinecraftServer server) {
        if (server == null) return;
        String fromId = ItemUpgradeLadder.idOf(stake);
        String toId = ItemUpgradeLadder.idOf(preview);
        String fromName = ItemUpgradeLadder.nameOf(stake);
        String toName = preview.isEmpty() ? "—" : ItemUpgradeLadder.nameOf(preview);
        int left = "pick".equals(phase)
                ? (int) Math.max(0, (pickUntil - System.currentTimeMillis() + 999) / 1000)
                : 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            boolean canPick = targetId != null && p.getUUID().equals(targetId) && "pick".equals(phase);
            UpgradePayload.send(p, new UpgradePayload(
                    phase == null || phase.isBlank() ? "hide" : phase,
                    viewer,
                    left,
                    chance,
                    fromId,
                    fromName,
                    toId,
                    toName,
                    selectedSlot,
                    canPick,
                    landAngle,
                    won));
        }
    }
}
