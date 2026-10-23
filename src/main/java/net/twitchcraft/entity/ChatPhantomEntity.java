package net.twitchcraft.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.stream.StreamStats;

/** Босс «Призрак чата» — 120 HP, фиолетовое свечение, жёсткий донат-босс. */
public class ChatPhantomEntity extends Phantom {
    private static final String TEAM_ID = "twitchcraft_phantom";

    private String viewerName = "Chat";
    private String streamerName = "";
    private ServerBossEvent bossBar;
    private boolean enraged;

    public ChatPhantomEntity(EntityType<? extends Phantom> type, Level level) {
        super(type, level);
        this.setPhantomSize(2);
        this.setPersistenceRequired();
    }

    public void initBoss(String viewer, ServerPlayer streamer) {
        viewerName = viewer != null && !viewer.isBlank() ? viewer : "Chat";
        streamerName = streamer != null ? streamer.getGameProfile().name() : "";
        setCustomName(Component.literal("§5§l☠ " + viewerName + " §d§lPHANTOM"));
        setCustomNameVisible(true);
        setGlowingTag(true);
        applyPurpleGlow();

        if (level().isClientSide()) return;

        ensureBossBar();
        setHealth(getMaxHealth());
        addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 60, 0, false, false, true));
    }

    private void ensureBossBar() {
        if (bossBar != null) return;

        bossBar = new ServerBossEvent(
                Component.literal("§5§l☠ " + viewerName + " §d§lPHANTOM §7(120 HP)"),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.NOTCHED_10);
        bossBar.setDarkenScreen(false);
        bossBar.setPlayBossMusic(false);
        bossBar.setCreateWorldFog(false);
        bossBar.setVisible(true);
        bossBar.setProgress(1f);
        syncBossBarPlayers();
    }

    private void syncBossBarPlayers() {
        if (bossBar == null || !(level() instanceof ServerLevel sl)) return;
        for (ServerPlayer player : sl.players()) {
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120)
                .add(Attributes.ATTACK_DAMAGE, 14)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
                .add(Attributes.FOLLOW_RANGE, 56)
                .add(Attributes.FLYING_SPEED, 0.55);
    }

    @Override
    public void tick() {
        super.tick();
        spawnPurpleParticles();
        if (!level().isClientSide()) {
            updateBossBar();
            checkEnrage();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hit = super.hurtServer(level, source, amount);
        if (hit && !level().isClientSide()) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                    getX(), getY() + 0.6, getZ(), 10, 0.5, 0.4, 0.5, 0.04);
        }
        return hit;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            living.setRemainingFireTicks(80);
            if (enraged) {
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
            } else {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
            }
        }
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        clearBossBar();
        removePurpleGlow();
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            if (!streamerName.isBlank()) {
                StreamStats.forPlayer(streamerName).addGood();
            } else {
                StreamStats.global().addGood();
            }
            String msg = "§a§lЧат победил §5" + viewerName + "§a! +добро";
            if (!streamerName.isBlank()) {
                ServerPlayer streamer = sl.getServer().getPlayerList().getPlayerByName(streamerName);
                if (streamer != null) {
                    AnnouncementService.announceCustom(streamer, msg);
                }
            } else {
                sl.players().forEach(p -> AnnouncementService.announceCustom(p, msg));
            }
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    viewerName + " Phantom уничтожен чатом стримера!");
            level().playSound(null, blockPosition(), SoundEvents.PHANTOM_DEATH,
                    SoundSource.HOSTILE, 1.5f, 0.7f);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                    getX(), getY() + 1, getZ(), 30, 0.8, 0.8, 0.8, 0.15);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        clearBossBar();
        removePurpleGlow();
        super.remove(reason);
    }

    private void applyPurpleGlow() {
        if (level().isClientSide()) return;
        Scoreboard scoreboard = level().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_ID);
        if (team == null) {
            team = scoreboard.addPlayerTeam(TEAM_ID);
            team.setColor(ChatFormatting.DARK_PURPLE);
        }
        scoreboard.addPlayerToTeam(getScoreboardName(), team);
    }

    private void removePurpleGlow() {
        if (level().isClientSide()) return;
        Scoreboard scoreboard = level().getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(getScoreboardName());
        if (team != null) {
            scoreboard.removePlayerFromTeam(getScoreboardName(), team);
        }
    }

    private void spawnPurpleParticles() {
        if (tickCount % 6 != 0) return;
        double px = getX() + (random.nextDouble() - 0.5) * 1.2;
        double py = getY() + random.nextDouble() * 0.8;
        double pz = getZ() + (random.nextDouble() - 0.5) * 1.2;
        level().addParticle(net.minecraft.core.particles.ParticleTypes.WITCH, px, py, pz, 0, 0.04, 0);
        if (tickCount % 12 == 0) {
            level().addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL, px, py + 0.2, pz, 0, 0.06, 0);
        }
        if (enraged && tickCount % 8 == 0) {
            var breath = PowerParticleOption.create(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH, 1.0f);
            level().addParticle(breath, px, py, pz, 0, 0.02, 0);
        }
    }

    private void updateBossBar() {
        if (level().isClientSide()) return;
        if (bossBar == null) {
            ensureBossBar();
            if (bossBar == null) return;
        }
        syncBossBarPlayers();
        float max = getMaxHealth();
        if (max > 0f) {
            bossBar.setProgress(Math.max(0f, getHealth() / max));
        }
    }

    private void checkEnrage() {
        if (enraged || getHealth() > getMaxHealth() * 0.5f) return;
        enraged = true;
        setPhantomSize(3);

        setAttribute(Attributes.ATTACK_DAMAGE, 18);
        setAttribute(Attributes.FLYING_SPEED, 0.75);
        setAttribute(Attributes.MOVEMENT_SPEED, 0.42);

        if (bossBar != null) {
            bossBar.setColor(BossEvent.BossBarColor.RED);
            bossBar.setName(Component.literal("§4§l☠ ЯРОСТЬ — " + viewerName + " §dPHANTOM"));
        }

        level().playSound(null, blockPosition(), SoundEvents.PHANTOM_SWOOP,
                SoundSource.HOSTILE, 1.8f, 0.5f);

        LivingEntity target = getTarget();
        if (target instanceof ServerPlayer player) {
            AnnouncementService.announceCustom(player, "§4§l☠ PHANTOM В ЯРОСТИ! §7<50% HP");
        }

        if (level() instanceof ServerLevel sl) {
            Vec3 pos = position();
            var breath = PowerParticleOption.create(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH, 1.0f);
            sl.sendParticles(breath, pos.x, pos.y + 1, pos.z, 40, 1.2, 0.8, 1.2, 0.02);
        }
    }

    private void setAttribute(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double value) {
        AttributeInstance inst = getAttribute(attr);
        if (inst != null) {
            inst.setBaseValue(value);
        }
    }

    private void clearBossBar() {
        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar = null;
        }
    }
}
