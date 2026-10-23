package net.twitchcraft.stream;

import net.minecraft.server.MinecraftServer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.commands.ChatGameCommand;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.effects.EffectRunner;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.ShowdownPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class VoteManager {
    private static final VoteManager INSTANCE = new VoteManager();

    private boolean active;
    private long endTime;
    private int durationSec = 45;
    private ChatGameCommand[] options = new ChatGameCommand[3];
    private final int[] voteCounts = new int[3];
    private final Map<String, Integer> userVotes = new HashMap<>();

    private VoteManager() {}

    public static VoteManager get() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active && System.currentTimeMillis() < endTime;
    }

    public int secondsLeft() {
        if (!active) return 0;
        return (int) Math.max(0, (endTime - System.currentTimeMillis() + 999) / 1000);
    }

    public String optionLabelRu(int index) {
        if (index < 0 || index >= 3 || options[index] == null) return "?";
        return "!" + options[index].getRussianName();
    }

    public int votes(int index) {
        if (index < 0 || index >= 3) return 0;
        return voteCounts[index];
    }

    public boolean start(MinecraftServer server) {
        if (isActive()) {
            EffectRunner.resolveTarget(server).ifPresent(p ->
                    AnnouncementService.announceCustom(p,
                            "§dГолосование уже идёт §f" + secondsLeft() + "с"));
            return false;
        }
        ChatGameCommand[] pool = ChatGameCommand.funPool(ConfigManager.get());
        if (pool.length == 0) pool = ChatGameCommand.funPool();
        java.util.List<ChatGameCommand> shuffled = new java.util.ArrayList<>(java.util.Arrays.asList(pool));
        java.util.Collections.shuffle(shuffled);
        options = new ChatGameCommand[3];
        for (int i = 0; i < 3; i++) {
            options[i] = shuffled.get(i % shuffled.size());
            if (i > 0 && options[i] == options[i - 1] && shuffled.size() > i) {
                options[i] = shuffled.get(Math.min(i, shuffled.size() - 1));
            }
        }
        voteCounts[0] = voteCounts[1] = voteCounts[2] = 0;
        userVotes.clear();
        durationSec = Math.max(8, ConfigManager.get().stream.voteDurationSeconds);
        active = true;
        endTime = System.currentTimeMillis() + durationSec * 1000L;

        String line = "§d§lГОЛОС! §e!1 §f" + ru(0)
                + "  §e!2 §f" + ru(1)
                + "  §e!3 §f" + ru(2)
                + " §8· §7" + durationSec + "с";
        EffectRunner.resolveTarget(server).ifPresent(p -> AnnouncementService.announceCustom(p, line));
        TwitchCraftMod.getConnectionManager().sendChatMessage(
                "ГОЛОСОВАНИЕ " + durationSec + "с!  !1 " + ru(0)
                        + "  |  !2 " + ru(1)
                        + "  |  !3 " + ru(2));
        pushHud(server);
        return true;
    }

    public boolean castVote(String user, int option) {
        if (!isActive() || option < 1 || option > 3) return false;
        if (userVotes.containsKey(user.toLowerCase())) return false;
        userVotes.put(user.toLowerCase(), option);
        voteCounts[option - 1]++;
        return true;
    }

    public void tick(MinecraftServer server) {
        if (!active) return;
        if (System.currentTimeMillis() >= endTime) {
            finish(server);
        }
    }

    public void pushHud(MinecraftServer server) {
        if (!isActive()) return;
        ShowdownPayload.broadcast(server, new ShowdownPayload(
                "VOTE",
                "ГОЛОС",
                packedSubtitle(),
                secondsLeft(),
                Math.max(1, durationSec),
                voteCounts[0],
                voteCounts[1],
                ru(0),
                ru(2),
                true));
    }

    private void finish(MinecraftServer server) {
        active = false;
        int maxVotes = voteCounts[0];
        for (int i = 1; i < 3; i++) {
            if (voteCounts[i] > maxVotes) maxVotes = voteCounts[i];
        }
        java.util.List<Integer> tied = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (voteCounts[i] == maxVotes) tied.add(i);
        }
        int bestIndex = tied.get(ThreadLocalRandom.current().nextInt(tied.size()));
        ChatGameCommand winner = options[bestIndex];
        int winningVotes = voteCounts[bestIndex];
        EffectRunner.resolveTarget(server).ifPresent(target -> {
            AnnouncementService.announceVoteWinner(target, winner, winningVotes);
            TwitchCraftMod.getConnectionManager().executeById(
                    server, "голос", winner.getId(), true, target.getGameProfile().name());
        });
        ShowdownDirector.refresh(server);
    }

    private String ru(int i) {
        if (i < 0 || i >= 3 || options[i] == null) return "?";
        return sanitize(options[i].getRussianName());
    }

    private String packedSubtitle() {
        return ru(0) + ":" + voteCounts[0]
                + "|" + ru(1) + ":" + voteCounts[1]
                + "|" + ru(2) + ":" + voteCounts[2];
    }

    private static String sanitize(String s) {
        if (s == null || s.isBlank()) return "?";
        return s.replace("|", "/").replace(":", " ");
    }
}
