package net.twitchcraft.stream;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.effects.ModCompat;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

/**
 * Настоящий размер (ванильный scale 1.21 + Pehkui, если стоит).
 * !муравей 30с · !титан 15с.
 */
public final class SizePrank {
    private static final ResourceLocation SCALE_ID =
            ResourceLocation.fromNamespaceAndPath(TwitchCraftMod.MOD_ID, "chat_scale");

    private static boolean active;
    private static long endTime;
    private static String label = "";
    private static String viewer = "";
    private static int durationSec;
    private static double targetScale = 1.0;

    private SizePrank() {}

    public static boolean isActive() {
        return active && System.currentTimeMillis() < endTime;
    }

    public static int secondsLeft() {
        if (!isActive()) return 0;
        return (int) Math.max(0, (endTime - System.currentTimeMillis() + 999) / 1000);
    }

    public static String statusLine() {
        if (!isActive()) return "";
        return "§e§l" + label + " §f" + secondsLeft() + "с";
    }

    public static void ant(CommandContext ctx) {
        int sec = Math.max(8, ConfigManager.get().stream.antSeconds);
        apply(ctx, 0.16, sec, "МУРАВЕЙ", true);
    }

    public static void titan(CommandContext ctx) {
        int sec = Math.max(6, ConfigManager.get().stream.titanSeconds);
        apply(ctx, 3.5, sec, "ТИТАН", false);
    }

    public static void apply(CommandContext ctx, double scale, int seconds, String name, boolean tiny) {
        if (ctx == null || ctx.target == null) return;
        if (!ConfigManager.get().stream.sizePranksEnabled) {
            AnnouncementService.announceCustom(ctx.target, "§7Размер выключен в настройках");
            net.twitchcraft.feedback.CommandDeny.send(ctx.target, ctx.twitchUser, name,
                    net.twitchcraft.feedback.CommandDeny.DISABLED, 0);
            ctx.fail();
            return;
        }
        ServerPlayer p = ctx.target;
        setScale(p, scale);
        active = true;
        durationSec = seconds;
        endTime = System.currentTimeMillis() + seconds * 1000L;
        label = name;
        viewer = ctx.twitchUser == null ? "чат" : ctx.twitchUser;
        targetScale = scale;

        ServerLevel level = p.level();
        level.playSound(null, p.blockPosition(),
                tiny ? SoundEvents.SILVERFISH_AMBIENT : SoundEvents.RAVAGER_ROAR,
                SoundSource.PLAYERS, tiny ? 1.2f : 1.0f, tiny ? 1.6f : 0.55f);
        level.sendParticles(tiny ? ParticleTypes.CLOUD : ParticleTypes.EXPLOSION,
                p.getX(), p.getY() + 1, p.getZ(), tiny ? 20 : 8, 0.4, 0.5, 0.4, 0.02);
        level.sendParticles(ParticleTypes.WITCH,
                p.getX(), p.getY() + 1, p.getZ(), 16, 0.3, 0.6, 0.3, 0.04);
        AnnouncementService.announceCustom(p,
                (tiny ? "§7§l" : "§6§l") + name + " §f" + seconds + "с §7от §e" + viewer);
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                viewer + " сделал стримера: " + name + " на " + seconds + "с. Настоящий размер.");
        ShowdownPayload.broadcast(ctx.server, new ShowdownPayload(
                "SIZE", name, viewer, seconds, seconds, 0, 0,
                tiny ? "влезет в дырку" : "дом больше не дом",
                "настоящий scale", true));
    }

    public static void tick(MinecraftServer server) {
        if (!active) return;
        StreamTargets.resolve(server).ifPresent(p -> {
            if (System.currentTimeMillis() >= endTime) {
                clear(server, p, true);
                return;
            }
            if (p.tickCount % 12 == 0) {
                p.level().sendParticles(ParticleTypes.MYCELIUM,
                        p.getX(), p.getY() + 0.2, p.getZ(), 2, 0.15, 0.1, 0.15, 0.0);
            }
        });
    }

    public static void pushHud(MinecraftServer server) {
        if (!isActive()) return;
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "SIZE", label, viewer, secondsLeft(), Math.max(1, durationSec),
                0, 0, "scale " + targetScale, "чат", true));
    }

    public static void clear(MinecraftServer server, ServerPlayer p, boolean announce) {
        active = false;
        if (p != null) {
            setScale(p, 1.0);
            if (announce) {
                AnnouncementService.announceCustom(p, "§7Размер снова обычный");
                TwitchCraftMod.getConnectionManager().sendChatMessage("Стример снова нормального роста.");
            }
        }
        ShowdownDirector.refresh(server);
    }

    private static void setScale(ServerPlayer p, double scale) {
        AttributeInstance inst = p.getAttribute(Attributes.SCALE);
        if (inst != null) {
            inst.removeModifier(SCALE_ID);
            if (Math.abs(scale - 1.0) > 0.01) {
                inst.addTransientModifier(new AttributeModifier(
                        SCALE_ID, scale - 1.0, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        p.refreshDimensions();
        ModCompat.setPehkuiScale(p, (float) scale);
    }
}
