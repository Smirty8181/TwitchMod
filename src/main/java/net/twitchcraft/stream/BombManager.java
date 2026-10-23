package net.twitchcraft.stream;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

import java.util.List;
import java.util.Locale;

/**
 * !бомба — горячая картошка. У кого в инвентаре, когда таймер кончится — взрыв.
 * Выбросить не спасает, пока напарник не подберёт. Чат может !разминируй.
 */
public final class BombManager {
    private static final BombManager INSTANCE = new BombManager();
    private static final String TAG = "TwitchCraftPotato";

    private boolean active;
    private String planter = "";
    private String holderName = "";
    private long explodeAt;
    private int durationSec;
    private int lastAnnounced;
    private Vec3 lastPos = Vec3.ZERO;
    private ServerLevel lastLevel;

    private BombManager() {}

    public static BombManager get() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active && System.currentTimeMillis() < explodeAt;
    }

    public int secondsLeft() {
        if (!active) return 0;
        return (int) Math.max(0, (explodeAt - System.currentTimeMillis() + 999) / 1000);
    }

    public String statusLine() {
        if (!isActive()) return "";
        String who = holderName.isBlank() ? "?" : holderName;
        return "§4§lБОМБА §c" + secondsLeft() + "s §f" + trunc(who, 10);
    }

    public static boolean isDefuse(String body) {
        if (body == null) return false;
        String first = body.contains(" ") ? body.substring(0, body.indexOf(' ')) : body;
        first = first.toLowerCase(Locale.ROOT);
        return first.equals("разминируй") || first.equals("defuse")
                || first.equals("сапёр") || first.equals("сапер")
                || first.equals("обезвредь") || first.equals("минусбомба");
    }

    public boolean start(MinecraftServer server, ServerPlayer target, String twitchUser) {
        if (isActive()) {
            AnnouncementService.announceCustom(target,
                    "§4Картошка уже у §f" + holderName + " §c" + secondsLeft() + "с");
            return false;
        }
        planter = twitchUser != null && !twitchUser.isBlank() ? twitchUser : "чат";
        durationSec = Math.max(8, ConfigManager.get().stream.bombSeconds);
        explodeAt = System.currentTimeMillis() + durationSec * 1000L;
        active = true;
        lastAnnounced = durationSec + 1;
        holderName = target.getGameProfile().name();
        lastLevel = target.level();
        lastPos = target.position();

        givePotato(target);

        ServerLevel level = target.level();
        level.playSound(null, target.blockPosition(), SoundEvents.TNT_PRIMED,
                SoundSource.BLOCKS, 1.2f, 1.0f);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationSec * 20, 0, false, false, true));

        AnnouncementService.announceCustom(target,
                "§4§lГОРЯЧАЯ КАРТОШКА! §f" + durationSec + "с §7— кинь напарнику");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "ГОРЯЧАЯ КАРТОШКА от " + planter + "! " + durationSec
                        +                 "с. Кинь напарнику. Чат: !разминируй");
        pushHud(server);
        return true;
    }

    public void defuse(MinecraftServer server, String twitchUser) {
        if (!isActive()) return;
        String hero = twitchUser != null && !twitchUser.isBlank() ? twitchUser : "чат";
        active = false;
        explodeAt = 0;
        stripPotatoes(server);

        ServerPlayer p = findHolder(server);
        if (p == null) p = StreamTargets.resolve(server).orElse(null);
        if (p != null) {
            ServerLevel level = p.level();
            p.removeEffect(MobEffects.GLOWING);
            level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0f, 1.4f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(),
                    20, 0.5, 0.4, 0.5, 0.02);
            Cat cat = EntityType.CAT.create(level, EntitySpawnReason.COMMAND);
            if (cat != null) {
                cat.setPos(p.getX() + 1, p.getY(), p.getZ());
                cat.setCustomName(Component.literal("§a сапёр " + trunc(hero, 10)));
                cat.setCustomNameVisible(true);
                cat.setTame(true, false);
                level.addFreshEntity(cat);
            }
            AnnouncementService.announceCustom(p,
                    "§a§lРАЗМИНИРОВАНО! §f" + hero + " §7спас §f" + holderName);
        }
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "Картошку разминировал " + hero + "!");
        holderName = "";
        ShowdownDirector.refresh(server);
    }

    public void tick(MinecraftServer server) {
        if (!active) return;
        refreshHolder(server);

        ServerPlayer holder = findHolder(server);
        if (holder != null) {
            lastLevel = holder.level();
            lastPos = holder.position();
            if (server.getTickCount() % 10 == 0) {
                holder.level().sendParticles(ParticleTypes.SMOKE,
                        holder.getX(), holder.getY() + 1.1, holder.getZ(),
                        4, 0.15, 0.2, 0.15, 0.01);
                holder.level().sendParticles(ParticleTypes.FLAME,
                        holder.getX(), holder.getY() + 1.0, holder.getZ(),
                        2, 0.12, 0.15, 0.12, 0.01);
            }
            if (server.getTickCount() % 20 == 0) {
                holder.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, true));
            }
        } else {
            glowGroundPotato(server);
        }

        int left = secondsLeft();
        if (left <= 5 && left > 0 && left != lastAnnounced) {
            lastAnnounced = left;
            ServerPlayer p = holder != null ? holder : StreamTargets.resolve(server).orElse(null);
            if (p != null) {
                p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(),
                        SoundSource.BLOCKS, 1.0f, 0.5f + (5 - left) * 0.15f);
                AnnouncementService.announceCustom(p, "§4§l💣 " + left + "… §7у §f" + holderName);
            }
        }
        if (System.currentTimeMillis() >= explodeAt) {
            explode(server);
        }
    }

    public void pushHud(MinecraftServer server) {
        if (!isActive()) return;
        String who = holderName.isBlank() ? "?" : holderName;
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "BOMB",
                "БОМБА",
                planter + " · у " + who,
                secondsLeft(),
                durationSec,
                secondsLeft(),
                0,
                who,
                "кинь!",
                true));
    }

    private void explode(MinecraftServer server) {
        active = false;
        explodeAt = 0;
        refreshHolder(server);
        ServerPlayer victim = findHolder(server);
        if (victim == null && holderName != null && !holderName.isBlank()) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.getGameProfile().name().equalsIgnoreCase(holderName)) {
                    victim = p;
                    break;
                }
            }
        }
        stripPotatoes(server);

        ServerLevel level;
        double x;
        double y;
        double z;
        String victimName;
        if (victim != null) {
            victim.removeEffect(MobEffects.GLOWING);
            level = victim.level();
            x = victim.getX();
            y = victim.getY();
            z = victim.getZ();
            victimName = victim.getGameProfile().name();
        } else if (lastLevel != null) {
            level = lastLevel;
            x = lastPos.x;
            y = lastPos.y;
            z = lastPos.z;
            victimName = holderName.isBlank() ? "?" : holderName;
        } else {
            TwitchCraftMod.getConnectionManager().sendChatMessage("Картошка взорвалась.");
            ShowdownDirector.refresh(server);
            return;
        }

        for (int i = 0; i < 2; i++) {
            PrimedTnt tnt = new PrimedTnt(level, x + (i == 0 ? 0.35 : -0.35), y + 0.2, z, victim);
            tnt.setFuse(12);
            level.addFreshEntity(tnt);
        }
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS, 1.2f, 0.8f);
        String boom = "§4§l💥 КАРТОШКА! §7у §f" + victimName + " §8· от §f" + planter;
        if (victim != null) {
            AnnouncementService.announceCustom(victim, boom);
        } else {
            StreamTargets.resolve(server).ifPresent(p -> AnnouncementService.announceCustom(p, boom));
        }
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "Картошка взорвалась у " + victimName + "! От " + planter);
        holderName = "";
        ShowdownDirector.refresh(server);
    }

    private void givePotato(ServerPlayer target) {
        ItemStack potato = makePotato();
        ItemStack main = target.getMainHandItem();
        target.setItemInHand(InteractionHand.MAIN_HAND, potato);
        if (!main.isEmpty()) {
            if (!target.getInventory().add(main)) {
                target.drop(main, false);
            }
        }
    }

    private ItemStack makePotato() {
        ItemStack stack = new ItemStack(Items.POTATO);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§c§lГОРЯЧАЯ КАРТОШКА"));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§cОт §f" + planter),
                Component.literal("§7Кинь напарнику, пока не бахнуло"))));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    static boolean isPotato(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return false;
        return data.copyTag().getBooleanOr(TAG, false);
    }

    private void refreshHolder(MinecraftServer server) {
        ServerPlayer found = null;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (inventoryHasPotato(p)) {
                found = p;
                break;
            }
        }
        if (found != null) {
            String name = found.getGameProfile().name();
            if (!name.equalsIgnoreCase(holderName)) {
                ServerPlayer prev = null;
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (p.getGameProfile().name().equalsIgnoreCase(holderName)) {
                        prev = p;
                        break;
                    }
                }
                if (prev != null) prev.removeEffect(MobEffects.GLOWING);
                holderName = name;
                found.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, true));
                found.level().playSound(null, found.blockPosition(), SoundEvents.ITEM_PICKUP,
                        SoundSource.PLAYERS, 1f, 0.6f);
                AnnouncementService.announceCustom(found, "§4§lКАРТОШКА У ТЕБЯ! §c" + secondsLeft() + "с");
                ServerPlayer partner = StreamTargets.partner(server, found).orElse(null);
                if (partner != null) {
                    AnnouncementService.announceCustom(partner,
                            "§e" + name + " §7поймал картошку · §c" + secondsLeft() + "с");
                }
            }
            return;
        }
        // На земле — всё ещё на последнем, кто держал.
    }

    private ServerPlayer findHolder(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (inventoryHasPotato(p)) return p;
        }
        if (holderName == null || holderName.isBlank()) return null;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getGameProfile().name().equalsIgnoreCase(holderName)) return p;
        }
        return null;
    }

    private static boolean inventoryHasPotato(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isPotato(inv.getItem(i))) return true;
        }
        return false;
    }

    private void stripPotatoes(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            var inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (isPotato(inv.getItem(i))) inv.setItem(i, ItemStack.EMPTY);
            }
            p.containerMenu.broadcastChanges();
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            AABB box = p.getBoundingBox().inflate(64);
            for (ItemEntity item : p.level().getEntitiesOfClass(ItemEntity.class, box)) {
                if (isPotato(item.getItem())) item.discard();
            }
        }
    }

    private void glowGroundPotato(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            AABB box = p.getBoundingBox().inflate(48);
            for (ItemEntity item : p.level().getEntitiesOfClass(ItemEntity.class, box)) {
                if (isPotato(item.getItem())) {
                    item.setGlowingTag(true);
                    item.setCustomNameVisible(true);
                    lastLevel = p.level();
                    lastPos = item.position();
                }
            }
        }
    }

    private static String trunc(String name, int max) {
        if (name == null || name.isBlank()) return "?";
        return name.length() > max ? name.substring(0, max - 1) + "…" : name;
    }
}
