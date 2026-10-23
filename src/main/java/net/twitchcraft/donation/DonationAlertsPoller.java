package net.twitchcraft.donation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.config.ConfigManager;
import net.twitchcraft.config.StreamerProfile;
import net.twitchcraft.config.TwitchCraftConfig;
import net.twitchcraft.feedback.AnnouncementService;
import net.twitchcraft.network.DonationAlertPayload;
import net.twitchcraft.stream.StreamStats;
import net.twitchcraft.stream.StreamTargets;
import net.twitchcraft.stream.StreamerProfiles;
import net.twitchcraft.twitch.ChatCommandProcessor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class DonationAlertsPoller {
    private static final String API_URL = "https://donatex.gg/api/v1/donations";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ChatCommandProcessor processor = new ChatCommandProcessor();
    private final Object pollLock = new Object();
    private volatile List<TokenGroup> tokenGroups = List.of();
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> task;
    private volatile MinecraftServer server;
    private volatile boolean running;
    private volatile boolean warnedChat;
    private volatile long lastAuthWarnAt;
    private final AtomicInteger targetRoundRobin = new AtomicInteger();

    private static final class TokenGroup {
        final String token;
        final List<String> targetPlayers = new ArrayList<>();
        final java.util.LinkedHashSet<String> seenIds = new java.util.LinkedHashSet<>();
        boolean initialized;
        boolean apiOk;
        volatile long skipUntil;

        TokenGroup(String token) {
            this.token = token;
        }
    }

    public void start(MinecraftServer server) {
        this.server = server;
        TwitchCraftConfig.DonationSettings ds = ConfigManager.get().donations;
        if (!ds.enabled) {
            DonationState.setConnected(false);
            return;
        }

        Map<String, TokenGroup> grouped = new LinkedHashMap<>();
        for (StreamerProfile profile : StreamerProfiles.active()) {
            String token = profile.hasDonations() ? profile.donationToken.trim() : "";
            if (token.isBlank() && ds.accessToken != null && !ds.accessToken.isBlank()) {
                token = ds.accessToken.trim();
            }
            if (!token.isBlank()) {
                grouped.computeIfAbsent(token, TokenGroup::new).targetPlayers.add(profile.minecraftName);
            }
        }
        if (grouped.isEmpty() && ds.accessToken != null && !ds.accessToken.isBlank()) {
            String target = StreamerProfiles.active().stream().map(p -> p.minecraftName).findFirst().orElse("");
            TokenGroup g = new TokenGroup(ds.accessToken.trim());
            if (!target.isBlank()) g.targetPlayers.add(target);
            grouped.put(g.token, g);
        }
        if (grouped.isEmpty()) {
            DonationState.setConnected(false);
            TwitchCraftMod.LOGGER.warn("DonateX: no tokens configured");
            return;
        }

        stop();
        synchronized (pollLock) {
            tokenGroups = new CopyOnWriteArrayList<>(grouped.values());
        }
        running = true;
        warnedChat = false;
        lastAuthWarnAt = 0;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TwitchCraft-Donations");
            t.setDaemon(true);
            return t;
        });
        int interval = Math.max(5, ds.pollIntervalSeconds);
        task = executor.scheduleAtFixedRate(this::pollSafe, 2, interval, TimeUnit.SECONDS);
        TwitchCraftMod.LOGGER.info("DonateX: polling {} token(s) for {} target(s) every {}s",
                grouped.size(), grouped.values().stream().mapToInt(g -> g.targetPlayers.size()).sum(), interval);
    }

    public void stop() {
        running = false;
        ScheduledFuture<?> t = task;
        ScheduledExecutorService e = executor;
        task = null;
        executor = null;
        if (t != null) t.cancel(false);
        if (e != null) {
            e.shutdown();
            try {
                if (!e.awaitTermination(5, TimeUnit.SECONDS)) {
                    e.shutdownNow();
                }
            } catch (InterruptedException ex) {
                e.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        synchronized (pollLock) {
            tokenGroups = List.of();
        }
        DonationState.setConnected(false);
    }

    public int profileCount() {
        synchronized (pollLock) {
            return tokenGroups.stream().mapToInt(g -> g.targetPlayers.size()).sum();
        }
    }

    public int pendingCount() {
        return PendingDonationQueue.size();
    }

    /** Вызывается с серверного тика — доставляет отложенные донаты. */
    public void tickPending(MinecraftServer server) {
        if (!running) return;
        this.server = server;
        PendingDonationQueue.tick(server, pending ->
                processDonation(pending.preferredTarget(), pending.donor(),
                        pending.amount(), pending.currency(), pending.message()));
    }

    public boolean testDonation(MinecraftServer server, String targetPlayer, String donor,
                                double amount, String currency, String message) {
        this.server = server;
        TwitchCraftConfig config = ConfigManager.get();
        var tier = DonationTierResolver.resolve(amount, currency, config.donations.tiers).orElse(null);
        if (tier == null) return false;
        processDonation(targetPlayer, donor, amount, currency, message);
        return true;
    }

    public static String tiersHint() {
        return DonationTierResolver.hint(ConfigManager.get().donations.tiers);
    }

    private void pollSafe() {
        if (!running) return;
        try {
            List<TokenGroup> snapshot;
            synchronized (pollLock) {
                snapshot = List.copyOf(tokenGroups);
            }
            boolean anyOk = false;
            for (TokenGroup group : snapshot) {
                if (pollGroup(group)) anyOk = true;
            }
            DonationState.setConnected(anyOk);
        } catch (Exception e) {
            TwitchCraftMod.LOGGER.warn("Donation poll error", e);
            DonationState.setConnected(false);
        }
    }

    private boolean pollGroup(TokenGroup group) throws Exception {
        if (server == null || !server.isRunning() || !running) return false;
        long now = System.currentTimeMillis();
        if (now < group.skipUntil) return false;

        HttpResponse<String> response = http.send(buildRequest(group.token), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            group.apiOk = false;
            String hint = tokenHint(group.token, response.statusCode());
            DonationState.setLastError(hint);
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                group.skipUntil = now + 15 * 60_000L;
            }
            if (now - lastAuthWarnAt > 600_000L) {
                lastAuthWarnAt = now;
                TwitchCraftMod.LOGGER.warn("DonateX API {} for token group ({} targets) — {}",
                        response.statusCode(), group.targetPlayers.size(), hint);
            }
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                notifyAuthFailure(hint);
            }
            return false;
        }
        group.apiOk = true;
        DonationState.setLastError("");
        warnedChat = false;

        JsonArray data = readDonationArray(response.body());
        if (data == null || data.isEmpty()) return true;

        List<JsonObject> donations = new ArrayList<>();
        for (JsonElement el : data) {
            if (el != null && el.isJsonObject()) donations.add(el.getAsJsonObject());
        }

        if (!group.initialized) {
            for (JsonObject d : donations) remember(group, readId(d));
            group.initialized = true;
            TwitchCraftMod.LOGGER.info("DonateX synced token group, cached {} donations", group.seenIds.size());
            return true;
        }

        for (JsonObject d : donations) {
            String id = readId(d);
            if (id.isBlank() || group.seenIds.contains(id)) continue;
            remember(group, id);

            String username = readUsername(d);
            if (username == null || username.isBlank()) {
                username = "Аноним";
            }
            double amount = readRubAmount(d);
            if (amount <= 0) continue;
            String currency = "RUB";
            String message = d.has("message") && !d.get("message").isJsonNull()
                    ? d.get("message").getAsString() : "";

            String target = pickTarget(group);
            if (target.isBlank()) {
                target = firstOnlineStreamer();
            }
            if (target.isBlank()) {
                // Не теряем донат: ставим в очередь до входа стримера
                String preferred = group.targetPlayers.isEmpty() ? "" : group.targetPlayers.getFirst();
                PendingDonationQueue.enqueue(preferred, username, amount, currency, message);
                continue;
            }
            processDonation(target, username, amount, currency, message);
        }
        return true;
    }

    private HttpRequest buildRequest(String token) {
        String encoded = java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "?skip=0&take=25&sortOrder=NewestFirst&token=" + encoded
                        + "&access_token=" + encoded))
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(12));
        if (looksLikeJwt(token) && token.chars().filter(c -> c == '.').count() >= 2) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.build();
    }

    private static boolean looksLikeJwt(String token) {
        return token != null && token.startsWith("eyJ");
    }

    private static String tokenHint(String token, int status) {
        if (token == null || token.isBlank()) {
            return "токен пустой";
        }
        if (looksLikeJwt(token)) {
            long dots = token.chars().filter(c -> c == '.').count();
            if (dots < 2 || token.length() <= 256) {
                return "обрезанный JWT — вставь external token из кабинета, не сессию браузера";
            }
            return "это OAuth JWT (живёт час) — нужен бессрочный external token";
        }
        if (status == 401 || status == 403) {
            return "DonateX отклонил токен — кабинет → Настройки → Api → external token";
        }
        return "DonateX ответил " + status;
    }

    private void notifyAuthFailure(String hint) {
        MinecraftServer srv = server;
        if (srv == null || warnedChat) return;
        warnedChat = true;
        srv.execute(() -> {
            for (String name : StreamTargets.names()) {
                ServerPlayer player = srv.getPlayerList().getPlayerByName(name);
                if (player == null) continue;
                player.sendSystemMessage(Component.literal("§cDonateX: токен не принят"));
                player.sendSystemMessage(Component.literal("§7" + hint));
                player.sendSystemMessage(Component.literal(
                        "§7donatex.gg → Настройки → Api → скопируй §fexternal token §7→ K → вставь → Сохранить"));
            }
        });
    }

    private String pickTarget(TokenGroup group) {
        if (group.targetPlayers.isEmpty()) return "";
        if (group.targetPlayers.size() == 1) {
            String only = group.targetPlayers.getFirst();
            if (server != null && server.getPlayerList().getPlayerByName(only) != null) return only;
            return "";
        }
        if (server == null) return "";

        List<String> online = new ArrayList<>();
        for (String name : group.targetPlayers) {
            ServerPlayer p = server.getPlayerList().getPlayerByName(name);
            if (p != null) online.add(name);
        }
        if (online.isEmpty()) return "";
        if (online.size() == 1) return online.getFirst();
        int idx = Math.floorMod(targetRoundRobin.getAndIncrement(), online.size());
        return online.get(idx);
    }

    private String firstOnlineStreamer() {
        if (server == null) return "";
        for (String name : StreamTargets.names()) {
            if (server.getPlayerList().getPlayerByName(name) != null) return name;
        }
        if (!server.getPlayerList().getPlayers().isEmpty()) {
            return server.getPlayerList().getPlayers().getFirst().getGameProfile().name();
        }
        return "";
    }

    private static JsonArray readDonationArray(String body) {
        JsonElement root = JsonParser.parseString(body);
        if (root.isJsonArray()) return root.getAsJsonArray();
        if (root.isJsonObject()) {
            JsonObject obj = root.getAsJsonObject();
            if (obj.has("data") && obj.get("data").isJsonArray()) return obj.getAsJsonArray("data");
            if (obj.has("items") && obj.get("items").isJsonArray()) return obj.getAsJsonArray("items");
        }
        return new JsonArray();
    }

    private static void remember(TokenGroup group, String id) {
        if (id == null || id.isBlank()) return;
        group.seenIds.add(id);
        while (group.seenIds.size() > 300) {
            var it = group.seenIds.iterator();
            it.next();
            it.remove();
        }
    }

    private static String readId(JsonObject d) {
        if (d.has("id") && !d.get("id").isJsonNull()) {
            return d.get("id").getAsString().trim();
        }
        return "";
    }

    private static double readRubAmount(JsonObject d) {
        double rub = readNumber(d, "amountInRub");
        if (rub > 0) return rub;
        return readAmount(d);
    }

    private static double readNumber(JsonObject d, String key) {
        if (!d.has(key) || d.get(key).isJsonNull()) return -1;
        var el = d.get(key);
        try {
            if (el.isJsonPrimitive()) {
                var prim = el.getAsJsonPrimitive();
                if (prim.isNumber()) return prim.getAsDouble();
                if (prim.isString()) {
                    return Double.parseDouble(prim.getAsString().trim().replace(",", "."));
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private static double readAmount(JsonObject d) {
        return readNumber(d, "amount");
    }

    private static String readUsername(JsonObject d) {
        if (d.has("username") && !d.get("username").isJsonNull()) {
            String u = d.get("username").getAsString();
            if (u != null && !u.isBlank()) return u.trim();
        }
        if (d.has("name") && !d.get("name").isJsonNull()) {
            String u = d.get("name").getAsString();
            if (u != null && !u.isBlank()) return u.trim();
        }
        return null;
    }

    private void processDonation(String targetPlayer, String username, double amount, String currency, String donorMessage) {
        TwitchCraftConfig config = ConfigManager.get();
        var tier = DonationTierResolver.resolve(amount, currency, config.donations.tiers).orElse(null);
        if (tier == null) {
            TwitchCraftMod.LOGGER.warn("Donation ignored: {} {} from {} — no matching tier",
                    amount, currency, username);
            return;
        }

        MinecraftServer srv = server;
        if (srv == null) {
            PendingDonationQueue.enqueue(targetPlayer, username, amount, currency, donorMessage);
            return;
        }

        srv.execute(() -> {
            var targetOpt = resolveDonationTarget(targetPlayer);
            if (targetOpt.isEmpty()) {
                PendingDonationQueue.enqueue(targetPlayer, username, amount, currency, donorMessage);
                return;
            }

            ServerPlayer target = targetOpt.get();
            String amountStr = formatAmount(amount);
            int tierColor = DonationTierStyle.colorForAmount(tier.minAmount);
            String effectId = tier.command != null ? tier.command.toLowerCase(java.util.Locale.ROOT) : "";

            TwitchCraftMod.LOGGER.info("Donation {} {} from {} → tier {}₽ ({}, wheel={})",
                    amount, currency, username, tier.minAmount, effectId, tier.wheel);
            net.twitchcraft.stream.StreamChronicle.donation(username, amount, currency,
                    target.getGameProfile().name(), effectId, tier.wheel);

            if (DonationTierResolver.isPhantomTier(tier)) {
                runExclusiveDonation(target, username, amountStr, currency, tier, donorMessage, tierColor, effectId);
                DonationEffectService.execute(server, target, username, tier, donorMessage);
                return;
            }

            if (DonationTierResolver.usesWheel(tier)) {
                DonationWheelService.start(server, target, username, amountStr, currency, tier, donorMessage, processor);
                return;
            }

            runExclusiveDonation(target, username, amountStr, currency, tier, donorMessage, tierColor, effectId);
            DonationEffectService.execute(server, target, username, tier, donorMessage);
        });
    }

    private static String formatAmount(double amount) {
        if (amount == Math.rint(amount)) {
            return String.valueOf((long) amount);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", amount);
    }

    private java.util.Optional<ServerPlayer> resolveDonationTarget(String preferred) {
        if (preferred != null && !preferred.isBlank()) {
            var opt = StreamTargets.resolve(server, preferred);
            if (opt.isPresent()) return opt;
        }
        var next = StreamTargets.resolve(server);
        if (next.isPresent()) return next;
        if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
            return java.util.Optional.of(server.getPlayerList().getPlayers().getFirst());
        }
        return java.util.Optional.empty();
    }

    private void runExclusiveDonation(ServerPlayer target, String username, String amountStr, String currency,
                                      TwitchCraftConfig.DonationTier tier, String donorMessage,
                                      int tierColor, String effectId) {
        DonationState.record(username, amountStr, currency, tier.label, target.getGameProfile().name());
        StreamStats.forPlayer(target.getGameProfile().name()).recordDonation(username, tier.command);

        String displayLabel = DonationEffectCatalog.resolveLabel(effectId, tier.label)
                .orElse(tier.label != null ? tier.label : effectId);

        AnnouncementService.announceDonation(target, username, amountStr, currency, displayLabel,
                null, donorMessage, tier.minAmount);
        DonationAlertPayload.send(target, username, amountStr, currency, displayLabel, tierColor,
                tier.minAmount, effectId, donorMessage, DonationEffectCatalog.screenFxLevel(tier.minAmount));
    }
}
