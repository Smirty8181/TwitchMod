package net.twitchcraft.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.twitchcraft.TwitchCraftMod;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Кэш аватаров Twitch по логину.
 * Резолв URL через публичный API (ivr.fi → decapi), загрузка PNG/JPEG в DynamicTexture.
 */
public final class TwitchAvatarCache {
    private static final int MAX_CACHE = 64;
    private static final long RETRY_COOLDOWN_MS = 120_000;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    private static final ExecutorService POOL = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "TwitchCraft-Avatar");
        t.setDaemon(true);
        return t;
    });

    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();
    private static final Map<String, Long> FAILED_AT = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> CACHE = new LinkedHashMap<>(80, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ResourceLocation> eldest) {
            if (size() <= MAX_CACHE) return false;
            release(eldest.getValue());
            return true;
        }
    };
    private static final Map<ResourceLocation, DynamicTexture> DYN = new ConcurrentHashMap<>();

    private TwitchAvatarCache() {}

    /** @return текстура или null (пока stub / грузится) */
    public static synchronized ResourceLocation getIfReady(String login) {
        if (login == null || login.isBlank()) return null;
        String key = normalize(login);
        return CACHE.get(key);
    }

    /** Запросить загрузку (идемпотентно). */
    public static void request(String login) {
        if (login == null || login.isBlank()) return;
        String key = normalize(login);
        if (key.length() < 2 || key.length() > 25) return;
        synchronized (TwitchAvatarCache.class) {
            if (CACHE.containsKey(key)) return;
        }
        if (IN_FLIGHT.contains(key)) return;
        Long failed = FAILED_AT.get(key);
        if (failed != null && System.currentTimeMillis() - failed < RETRY_COOLDOWN_MS) return;
        if (!IN_FLIGHT.add(key)) return;

        POOL.execute(() -> {
            try {
                byte[] png = fetchAvatarBytes(key);
                if (png == null || png.length < 64) {
                    FAILED_AT.put(key, System.currentTimeMillis());
                    return;
                }
                Minecraft mc = Minecraft.getInstance();
                if (mc == null) return;
                mc.execute(() -> registerTexture(key, png));
            } catch (Exception e) {
                FAILED_AT.put(key, System.currentTimeMillis());
                TwitchCraftMod.LOGGER.debug("Avatar fetch failed for {}: {}", key, e.toString());
            } finally {
                IN_FLIGHT.remove(key);
            }
        });
    }

    private static void registerTexture(String key, byte[] bytes) {
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() < 1) return;

            // квадрат 64×64 для GUI
            NativeImage square = resizeSquare(image, 64);
            if (square != image) image.close();

            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    TwitchCraftMod.MOD_ID, "avatar/" + key);
            Minecraft mc = Minecraft.getInstance();
            var tm = mc.getTextureManager();

            DynamicTexture prev = DYN.remove(id);
            if (prev != null) {
                NativeImage px = prev.getPixels();
                if (px != null) px.close();
            }
            try {
                tm.release(id);
            } catch (Exception ignored) {
            }

            DynamicTexture dyn = new DynamicTexture(() -> "twitchcraft-avatar:" + key, square);
            dyn.setFilter(true, false);
            dyn.setClamp(true);
            tm.register(id, dyn);
            dyn.upload();
            DYN.put(id, dyn);
            synchronized (TwitchAvatarCache.class) {
                CACHE.put(key, id);
                // LinkedHashMap eldest cleanup
                while (CACHE.size() > MAX_CACHE) {
                    Iterator<Map.Entry<String, ResourceLocation>> it = CACHE.entrySet().iterator();
                    if (!it.hasNext()) break;
                    Map.Entry<String, ResourceLocation> e = it.next();
                    it.remove();
                    release(e.getValue());
                }
            }
        } catch (Exception e) {
            FAILED_AT.put(key, System.currentTimeMillis());
            TwitchCraftMod.LOGGER.debug("Avatar register failed for {}: {}", key, e.toString());
        }
    }

    private static byte[] fetchAvatarBytes(String login) throws Exception {
        String imageUrl = resolveViaIvr(login);
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = "https://decapi.me/twitch/avatar/" + URLEncoder.encode(login, StandardCharsets.UTF_8);
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "TwitchCraft/1.16 (Minecraft mod; +https://modrinth.com/mod/twitchcraft)")
                .GET()
                .build();
        HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300 && resp.body() != null && resp.body().length > 64) {
            // decapi иногда отдаёт текстовый URL
            if (looksLikeTextUrl(resp.body())) {
                String url = new String(resp.body(), StandardCharsets.UTF_8).trim();
                if (url.startsWith("http")) {
                    return downloadBytes(url);
                }
                return null;
            }
            return resp.body();
        }
        return null;
    }

    private static String resolveViaIvr(String login) {
        try {
            String url = "https://api.ivr.fi/v2/twitch/user?login="
                    + URLEncoder.encode(login, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "TwitchCraft/1.16")
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200 || resp.body() == null) return null;
            JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
            if (obj.has("logo") && !obj.get("logo").isJsonNull()) {
                return obj.get("logo").getAsString();
            }
            if (obj.has("profilePictureUrl") && !obj.get("profilePictureUrl").isJsonNull()) {
                return obj.get("profilePictureUrl").getAsString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static byte[] downloadBytes(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "TwitchCraft/1.16")
                .GET()
                .build();
        HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) return resp.body();
        return null;
    }

    private static boolean looksLikeTextUrl(byte[] body) {
        if (body.length > 400) return false;
        String s = new String(body, StandardCharsets.UTF_8).trim();
        return s.startsWith("http://") || s.startsWith("https://");
    }

    private static NativeImage resizeSquare(NativeImage src, int size) {
        if (src.getWidth() == size && src.getHeight() == size) return src;
        NativeImage out = new NativeImage(size, size, true);
        int sw = src.getWidth();
        int sh = src.getHeight();
        int side = Math.min(sw, sh);
        int ox = (sw - side) / 2;
        int oy = (sh - side) / 2;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int sx = ox + x * side / size;
                int sy = oy + y * side / size;
                out.setPixel(x, y, src.getPixel(sx, sy));
            }
        }
        return out;
    }

    private static void release(ResourceLocation id) {
        if (id == null) return;
        DynamicTexture dyn = DYN.remove(id);
        if (dyn != null) {
            NativeImage px = dyn.getPixels();
            if (px != null) px.close();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            try {
                mc.getTextureManager().release(id);
            } catch (Exception ignored) {
            }
        }
    }

    private static String normalize(String login) {
        return login.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
    }
}
