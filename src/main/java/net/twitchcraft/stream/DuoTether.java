package net.twitchcraft.stream;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.CommandContext;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

/** !связь — верёвка между двумя стримерами: урон и расстояние общие. */
public final class DuoTether {
    private static final int DURATION_SEC = 45;
    private static final double SOFT = 10;
    private static final double HARD = 18;

    private static boolean active;
    private static long endAt;
    private static boolean echoingDamage;

    private DuoTether() {}

    public static boolean isActive() {
        return active && System.currentTimeMillis() < endAt;
    }

    public static int secondsLeft() {
        if (!isActive()) return 0;
        return (int) Math.max(0, (endAt - System.currentTimeMillis() + 999) / 1000);
    }

    public static void fromCommand(CommandContext ctx) {
        start(ctx.server, ctx.target, ctx.twitchUser);
    }

    public static boolean start(MinecraftServer server, ServerPlayer who, String by) {
        if (server == null) return false;
        var pair = pair(server, who);
        if (pair == null) {
            if (who != null) {
                AnnouncementService.announceCustom(who, "§7Связь нужна двоим на сервере");
            }
            return false;
        }
        if (isActive()) {
            AnnouncementService.announceCustom(pair[0], "§dСвязь уже держит §f" + secondsLeft() + "с");
            return false;
        }
        active = true;
        endAt = System.currentTimeMillis() + DURATION_SEC * 1000L;
        String from = by == null || by.isBlank() ? "чат" : by;
        for (ServerPlayer p : pair) {
            p.addEffect(new MobEffectInstance(MobEffects.GLOWING, DURATION_SEC * 20, 0, false, false));
            title(p, "§d§lСВЯЗЬ", "§f" + DURATION_SEC + "с §8· §7урон и шаг общие");
            p.level().playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.PLAYERS, 0.9f, 0.6f);
        }
        AnnouncementService.announceCustom(pair[0],
                "§d§lСВЯЗЬ! §7" + from + " §8связал вас на §f" + DURATION_SEC + "с");
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "СВЯЗЬ! " + from + " связал стримеров на " + DURATION_SEC + "с. Урон и расстояние общие.");
        ShowdownDirector.refresh(server);
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (!active) return;
        if (!isActive()) {
            finish(server, "§7Связь распалась");
            return;
        }
        ServerPlayer[] pair = pair(server, null);
        if (pair == null) {
            finish(server, "§7Связь оборвалась — нет двоих");
            return;
        }
        leash(pair[0], pair[1]);
        if (server.getTickCount() % 4 == 0) {
            particles(pair[0], pair[1]);
        }
        if (server.getTickCount() % 20 == 0) {
            pushHud(server);
        }
    }

    public static void forceEnd(MinecraftServer server, String msg) {
        if (!active) return;
        finish(server, msg == null || msg.isBlank() ? "§7Связь распалась" : msg);
    }

    public static void onDamage(ServerPlayer victim, float amount, DamageSource source) {
        if (!isActive() || echoingDamage || victim == null || amount <= 0) return;
        MinecraftServer server = victim.level().getServer();
        ServerPlayer other = StreamTargets.partner(server, victim).orElse(null);
        if (other == null || !other.isAlive()) return;
        if (source.getEntity() == other) return;
        if (!(other.level() instanceof ServerLevel level)) return;
        echoingDamage = true;
        try {
            other.hurtServer(level, other.damageSources().generic(), Math.max(1f, amount * 0.45f));
        } finally {
            echoingDamage = false;
        }
    }

    public static void onStreamerDeath(MinecraftServer server, ServerPlayer dead) {
        if (!isActive() || dead == null) return;
        ServerPlayer other = StreamTargets.partner(server, dead).orElse(null);
        finish(server, "§cСвязь оборвалась смертью");
        if (other == null || !other.isAlive()) return;
        other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
        other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 1));
        other.setDeltaMovement(0, 1.15, 0);
        other.hurtMarked = true;
        AnnouncementService.announceCustom(other, "§cНапарник погиб — связь ударила и тебя");
    }

    public static void pushHud(MinecraftServer server) {
        if (!isActive()) return;
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "TETHER", "СВЯЗЬ", "урон общий",
                secondsLeft(), DURATION_SEC, 0, 0, "ты", "он", true));
    }

    private static void finish(MinecraftServer server, String msg) {
        active = false;
        endAt = 0;
        if (server != null) {
            for (ServerPlayer p : StreamTargets.onlineStreamers(server)) {
                AnnouncementService.announceCustom(p, msg);
            }
            ShowdownDirector.refresh(server);
        }
    }

    private static void leash(ServerPlayer a, ServerPlayer b) {
        if (a.level() != b.level()) return;
        double dist = a.distanceTo(b);
        if (dist <= SOFT) return;
        Vec3 mid = a.position().add(b.position()).scale(0.5);
        double pull = dist > HARD ? 0.55 : 0.22;
        pullToward(a, mid, pull);
        pullToward(b, mid, pull);
        if (dist > HARD + 6) {
            a.teleportTo(a.level(), mid.x, mid.y, mid.z, java.util.Set.of(), a.getYRot(), a.getXRot(), true);
            b.teleportTo(b.level(), mid.x, mid.y, mid.z, java.util.Set.of(), b.getYRot(), b.getXRot(), true);
        }
    }

    private static void pullToward(ServerPlayer p, Vec3 mid, double str) {
        Vec3 d = mid.subtract(p.position());
        if (d.lengthSqr() < 0.04) return;
        Vec3 n = d.normalize().scale(str);
        p.setDeltaMovement(p.getDeltaMovement().add(n.x, Math.min(0.35, n.y + 0.08), n.z));
        p.hurtMarked = true;
    }

    private static void particles(ServerPlayer a, ServerPlayer b) {
        if (!(a.level() instanceof ServerLevel level) || a.level() != b.level()) return;
        Vec3 from = a.position().add(0, 1.2, 0);
        Vec3 to = b.position().add(0, 1.2, 0);
        Vec3 step = to.subtract(from);
        int n = 4;
        for (int i = 1; i < n; i++) {
            double t = i / (double) n;
            level.sendParticles(ParticleTypes.WITCH,
                    from.x + step.x * t, from.y + step.y * t, from.z + step.z * t,
                    1, 0.02, 0.02, 0.02, 0);
        }
    }

    private static ServerPlayer[] pair(MinecraftServer server, ServerPlayer prefer) {
        var list = StreamTargets.onlineStreamers(server);
        if (list.size() < 2) return null;
        ServerPlayer a = prefer != null && StreamTargets.isTarget(prefer) ? prefer : list.getFirst();
        ServerPlayer b = StreamTargets.partner(server, a).orElse(null);
        if (b == null) return null;
        return new ServerPlayer[]{a, b};
    }

    private static void title(ServerPlayer p, String t, String s) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 45, 10));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(t)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(s)));
    }
}
