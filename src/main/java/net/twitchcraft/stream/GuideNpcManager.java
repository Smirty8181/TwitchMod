package net.twitchcraft.stream;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.TtsSpeakPayload;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Реплики короля / ангела / вех: только чат и озвучка, без мобов в мире.
 */
public final class GuideNpcManager {
    private static final GuideNpcManager INSTANCE = new GuideNpcManager();
    private static final int DURATION_SEC = 10;
    private static final String[] LINES = {
            "Сикс севен",
            "Охаешечки Датабаёшечки",
            "Скибиди туалет",
            "Камдаун бро",
            "Это Ohio",
            "Я в деле",
            "Погнали нафиг",
            "Чат в здании",
            "Рил ток",
            "База",
            "Не база",
            "Эщкере",
            "67",
            "Six seven",
            "Охаё",
            "Датабаёшечки",
            "Братан держись",
            "Копай сюда",
            "Там алмазы блин",
            "Не тудааа",
            "АД АД АД",
            "Я GPS",
            "Следуй за мной (лол)",
            "Ну такое",
            "Заточка плюс вайб",
            "Грифер момент",
            "Чат сказал копать",
            "Дракона зови",
            "Охаешечки",
            "Сикс. Севен."
    };

    private boolean active;
    private long endTime;
    private long nextLineAt;
    private String viewer = "";
    private String role = "король";

    private GuideNpcManager() {}

    public static GuideNpcManager get() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active && System.currentTimeMillis() < endTime;
    }

    public void comment(MinecraftServer server, ServerPlayer player, String line) {
        comment(server, player, line, null);
    }

    public void comment(MinecraftServer server, ServerPlayer player, String line, String viewerName) {
        if (player == null || line == null || line.isBlank()) return;
        if (viewerName != null && !viewerName.isBlank()) {
            this.viewer = viewerName;
            this.role = "убийца";
        } else if (this.viewer == null || this.viewer.isBlank()) {
            this.viewer = "диктор";
            this.role = "диктор";
        }
        this.active = true;
        this.endTime = System.currentTimeMillis() + DURATION_SEC * 1000L;
        this.nextLineAt = Long.MAX_VALUE;
        shoutLine(player, line);
    }

    public void spawn(MinecraftServer server, ServerPlayer player, String viewer, String role) {
        spawn(server, player, viewer, role, true);
    }

    public void spawn(MinecraftServer server, ServerPlayer player, String viewer, String role, boolean announce) {
        if (player == null) return;
        this.viewer = viewer == null || viewer.isBlank() ? "чат" : viewer;
        this.role = role == null ? "король" : role;
        this.active = true;
        this.endTime = System.currentTimeMillis() + DURATION_SEC * 1000L;
        this.nextLineAt = System.currentTimeMillis() + 2500L;
        player.playNotifySound(SoundEvents.VILLAGER_CELEBRATE, SoundSource.PLAYERS, 1.3f, 1.1f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_CELEBRATE,
                SoundSource.PLAYERS, 1.6f, 1.1f);
        player.level().sendParticles(ParticleTypes.NOTE,
                player.getX(), player.getY() + 2, player.getZ(), 12, 0.5, 0.4, 0.5, 0.2);
        if (announce) {
            AnnouncementService.announceCustom(player,
                    "§6" + this.viewer + " §7— " + this.role + " орёт мемы 10с");
            TwitchCraftMod.getConnectionManager().sendChatMessage(
                    this.viewer + " орёт мемы. Готовьте уши.");
        }
    }

    public void tick(MinecraftServer server) {
        if (!active) return;
        StreamTargets.resolve(server).ifPresent(p -> {
            if (System.currentTimeMillis() >= endTime) {
                active = false;
                return;
            }
            if (System.currentTimeMillis() >= nextLineAt) {
                shout(p);
                nextLineAt = System.currentTimeMillis() + 7000L + ThreadLocalRandom.current().nextInt(4000);
            }
        });
    }

    private void shout(ServerPlayer p) {
        shoutLine(p, LINES[ThreadLocalRandom.current().nextInt(LINES.length)]);
    }

    private void shoutLine(ServerPlayer p, String line) {
        ServerLevel level = p.level();
        p.playNotifySound(SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 1.4f,
                0.9f + level.random.nextFloat() * 0.25f);
        p.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.2f, 1.15f);
        p.playNotifySound(SoundEvents.WANDERING_TRADER_YES, SoundSource.PLAYERS, 1.3f, 1.05f);
        level.playSound(null, p.blockPosition(), SoundEvents.VILLAGER_AMBIENT,
                SoundSource.PLAYERS, 2.0f, 0.95f);
        TtsSpeakPayload.send(p, speaker(), speakable(line));
        level.sendParticles(ParticleTypes.NOTE,
                p.getX(), p.getY() + 2.1, p.getZ(), 8, 0.3, 0.2, 0.3, 0.4);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                p.getX(), p.getY() + 1.4, p.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        AnnouncementService.announceCustom(p, "§6" + speaker() + "§7: §e" + line);
        TwitchCraftMod.getConnectionManager().sendChatMessage(speaker() + ": " + line);
        purgeLegacyNarrators(level, p);
    }

    /** TTS не должен читать × как «умножение». */
    private static String speakable(String line) {
        if (line == null) return "";
        return line.replace('×', ' ')
                .replaceAll("(?i)[xх]\\s*\\d+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String speaker() {
        return viewer == null || viewer.isBlank() ? "диктор" : viewer;
    }

    private static boolean purgedLegacy;

    /** Старые Easy NPC / аллаи-дикторы с прошлых версий — сразу убрать. */
    private static void purgeLegacyNarrators(ServerLevel level, ServerPlayer p) {
        if (purgedLegacy) return;
        purgedLegacy = true;
        for (Entity e : level.getEntities(p, p.getBoundingBox().inflate(32),
                other -> other != p && other.hasCustomName())) {
            String n = e.getCustomName() == null ? "" : e.getCustomName().getString();
            if (n.contains("🎤") || n.contains("диктор") || n.contains("☠")) {
                e.discard();
            }
        }
    }
}
