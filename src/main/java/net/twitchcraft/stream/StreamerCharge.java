package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.commands.GoodCommands;
import net.twitchcraft.feedback.AnnouncementService;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Заряды стримера. Тратятся из меню I — надо заработать игрой.
 * Макс 3. КД на одну и ту же причину, чтобы не фармить.
 */
public final class StreamerCharge {
    public static final int MAX = 3;
    private static final long REASON_CD_MS = 50_000L;
    private static final long SURVIVE_MS = 8 * 60_000L;
    private static final int HEALS_FOR_CHARGE = 4;

    private static int charges;
    private static int heals;
    private static long aliveSince = System.currentTimeMillis();
    private static long nextWorldDiceAt;
    private static final Map<String, Long> reasonAt = new HashMap<>();

    private StreamerCharge() {}

    public static void reset() {
        charges = 0;
        heals = 0;
        aliveSince = System.currentTimeMillis();
        nextWorldDiceAt = 0;
        reasonAt.clear();
    }

    public static int get() {
        return charges;
    }

    public static String sidebarHint() {
        return "§6✦ " + charges + "/" + MAX;
    }

    public static boolean spend(ServerPlayer player, int amount, String what) {
        if (charges < amount) {
            if (player != null) {
                AnnouncementService.announceCustom(player,
                        "§cНет заряда §7(" + charges + "/" + MAX + ") — " + what
                                + " §8· убей моба чата, добудь алмаз, выживи охоту");
            }
            return false;
        }
        charges -= amount;
        if (player != null) {
            AnnouncementService.announceCustom(player,
                    "§6−" + amount + " заряд §7→ " + what + " §8(" + charges + "/" + MAX + ")");
        }
        return true;
    }

    public static void earn(MinecraftServer server, ServerPlayer player, String reason, int amount) {
        earn(server, player, reason, amount, false);
    }

    public static void earn(MinecraftServer server, ServerPlayer player, String reason, int amount, boolean ignoreCd) {
        if (amount <= 0) return;
        String key = reason == null ? "x" : reason.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        if (!ignoreCd) {
            Long last = reasonAt.get(key);
            if (last != null && now - last < REASON_CD_MS) return;
        }
        if (charges >= MAX) {
            if (player != null) {
                AnnouncementService.announceCustom(player, "§6Заряды полные §8(" + MAX + ")");
            }
            return;
        }
        reasonAt.put(key, now);
        charges = Math.min(MAX, charges + amount);
        if (player != null) {
            AnnouncementService.announceCustom(player,
                    "§6§l+ЗАРЯД §7" + reason + " §8(" + charges + "/" + MAX + ")");
        }
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "Стример получил заряд (" + charges + "/" + MAX + ") — " + reason);
    }

    public static void onGoodCommand(MinecraftServer server, ServerPlayer target, ChatGameCommand cmd) {
        if (!GoodCommands.isGood(cmd) || target == null) return;
        heals++;
        if (heals >= HEALS_FOR_CHARGE) {
            heals = 0;
            earn(server, target, "хилы чата", 1);
        }
    }

    public static void onStreamerDeath() {
        aliveSince = System.currentTimeMillis();
        heals = Math.max(0, heals - 1);
    }

    public static void onStreamerKill(MinecraftServer server, ServerPlayer killer, LivingEntity killed) {
        if (killer == null || killed == null || !StreamTargets.isTarget(killer)) return;
        var type = killed.getType();
        if (type == EntityType.WITHER || type == EntityType.ENDER_DRAGON
                || type == EntityType.WARDEN || type == EntityType.ELDER_GUARDIAN
                || type == EntityType.RAVAGER) {
            earn(server, killer, "босс", 1);
        }
    }

    public static void onGhostKilledByStreamer(MinecraftServer server, ServerPlayer killer, String viewer) {
        if (killer == null || !StreamTargets.isTarget(killer)) return;
        earn(server, killer, "моб чата", 1);
    }

    public static boolean sacrifice(MinecraftServer server, ServerPlayer player) {
        if (player == null) return false;
        ItemStack hand = player.getMainHandItem();
        if (!isValuable(hand)) {
            AnnouncementService.announceCustom(player,
                    "§7Жертва: возьми в руку §eалмаз / тотем / незерит / гапл");
            return false;
        }
        if (charges >= MAX) {
            AnnouncementService.announceCustom(player, "§6Заряды уже полные");
            return false;
        }
        String name = hand.getHoverName().getString();
        if (!consumeHand(player)) return false;
        earn(server, player, "жертва", 1, true);
        AnnouncementService.announceCustom(player, "§6Жертва: §e" + name);
        return true;
    }

    public static boolean consumeValuable(ServerPlayer player) {
        if (player == null) return false;
        if (!isValuable(player.getMainHandItem())) {
            AnnouncementService.announceCustom(player,
                    "§7Нужен §eалмаз / тотем / незерит / гапл §7в руке");
            return false;
        }
        return consumeHand(player);
    }

    private static boolean consumeHand(ServerPlayer player) {
        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty()) return false;
        hand.shrink(1);
        player.getInventory().setChanged();
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (nextWorldDiceAt == 0) scheduleWorldDice();
        long now = System.currentTimeMillis();
        if (now - aliveSince >= SURVIVE_MS) {
            aliveSince = now;
            StreamTargets.resolve(server).ifPresent(p -> earn(server, p, "8 мин жив", 1));
        }
        if (now >= nextWorldDiceAt) {
            scheduleWorldDice();
            StreamTargets.resolve(server).ifPresent(p -> {
                AnnouncementService.announceCustom(p, "§6§lМИР КИДАЕТ КУБИК");
                TwitchCraftMod.getConnectionManager().sendChatMessage("Мир кидает кубик стримеру!");
                StreamerDice.get().rollFromWorld(server, p);
            });
        }
    }

    private static void scheduleWorldDice() {
        int min = 8;
        int max = 14;
        int minutes = min + ThreadLocalRandom.current().nextInt(max - min + 1);
        nextWorldDiceAt = System.currentTimeMillis() + minutes * 60_000L;
    }

    private static boolean isValuable(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        return s.is(Items.DIAMOND) || s.is(Items.DIAMOND_BLOCK)
                || s.is(Items.NETHERITE_INGOT) || s.is(Items.NETHERITE_SCRAP)
                || s.is(Items.TOTEM_OF_UNDYING)
                || s.is(Items.GOLDEN_APPLE) || s.is(Items.ENCHANTED_GOLDEN_APPLE)
                || s.is(Items.EMERALD_BLOCK) || s.is(Items.GOLD_BLOCK)
                || s.is(Items.NETHERITE_SWORD) || s.is(Items.NETHERITE_CHESTPLATE)
                || s.is(Items.NETHERITE_HELMET) || s.is(Items.NETHERITE_LEGGINGS)
                || s.is(Items.NETHERITE_BOOTS) || s.is(Items.NETHERITE_PICKAXE);
    }
}
