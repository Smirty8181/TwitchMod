package net.twitchcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.twitchcraft.TwitchCraftMod;
import net.twitchcraft.network.TtsSpeakPayload;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/** Озвучка !озвучка на клиенте стримера (Windows SAPI / macOS say / Linux espeak). */
public final class TtsClientNetworking {
    private static boolean registered;
    private static final ExecutorService TTS_POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "TwitchCraft-TTS");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicReference<String> ENGINE = new AtomicReference<>();
    private static volatile boolean warnedNoEngine;

    private TtsClientNetworking() {}

    public static void register() {
        if (registered) return;
        ClientPlayNetworking.registerGlobalReceiver(TtsSpeakPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> speak(payload.speaker(), payload.text())));
        registered = true;
        TTS_POOL.execute(TtsClientNetworking::detectEngine);
    }

    private static void speak(String speaker, String text) {
        if (text == null || text.isBlank()) return;
        String clean = sanitize(text);
        if (clean.isBlank()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.literal("§d§l🔊 §f" + trunc(speaker, 12) + "§8: §e" + trunc(clean, 80)),
                    false);
        }

        String engine = ENGINE.get();
        if (engine == null) {
            detectEngine();
            engine = ENGINE.get();
        }
        if (engine == null || "none".equals(engine)) {
            if (!warnedNoEngine && mc.player != null) {
                warnedNoEngine = true;
                mc.player.displayClientMessage(Component.literal(
                        "§8[TwitchCraft] §7TTS: движок не найден. Текст в чате — ок. "
                                + "§8Windows: SAPI · macOS: say · Linux: espeak-ng / espeak / spd-say"),
                        false);
            }
            TwitchCraftMod.LOGGER.info("TTS (text-only fallback): {} — {}", speaker, clean);
            return;
        }

        final String eng = engine;
        TTS_POOL.execute(() -> speakWithEngine(eng, clean));
    }

    private static void detectEngine() {
        if (ENGINE.get() != null) return;
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            ENGINE.set("windows");
            return;
        }
        if (os.contains("mac") || os.contains("darwin")) {
            if (commandExists("say")) {
                ENGINE.set("macos");
                return;
            }
            ENGINE.set("none");
            return;
        }
        // Linux / other Unix
        if (commandExists("espeak-ng")) {
            ENGINE.set("espeak-ng");
        } else if (commandExists("espeak")) {
            ENGINE.set("espeak");
        } else if (commandExists("spd-say")) {
            ENGINE.set("spd-say");
        } else {
            ENGINE.set("none");
            TwitchCraftMod.LOGGER.info("TTS: no engine found (install espeak-ng, espeak, or spd-say)");
        }
    }

    private static void speakWithEngine(String engine, String text) {
        try {
            switch (engine) {
                case "windows" -> speakWindows(text);
                case "macos" -> speakMac(text);
                case "espeak-ng" -> speakEspeak("espeak-ng", text);
                case "espeak" -> speakEspeak("espeak", text);
                case "spd-say" -> speakSpdSay(text);
                default -> TwitchCraftMod.LOGGER.info("TTS fallback: {}", text);
            }
        } catch (Exception e) {
            TwitchCraftMod.LOGGER.warn("TTS failed ({}): {}", engine, e.toString());
        }
    }

    private static void speakWindows(String text) throws Exception {
        String escaped = shellSafe(text);
        if (escaped.isBlank()) return;
        String ps = "Add-Type -AssemblyName System.Speech; "
                + "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$s.Rate = 0; $s.Volume = 100; "
                + "try { $s.SelectVoiceByHints([System.Speech.Synthesis.VoiceGender]::NotSet, "
                + "[System.Speech.Synthesis.VoiceAge]::NotSet, 0, "
                + "[System.Globalization.CultureInfo]::GetCultureInfo('ru-RU')) } catch {}; "
                + "$s.Speak('" + escaped + "')";
        new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", ps)
                .redirectErrorStream(true)
                .start()
                .waitFor();
    }

    private static void speakMac(String text) throws Exception {
        String escaped = shellSafe(text);
        if (escaped.isBlank()) return;
        // Prefer Russian voice if available; -v falls back silently on unknown voice names via try
        ProcessBuilder pb = new ProcessBuilder("say", "-v", "Milena", escaped);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        int code = p.waitFor();
        if (code != 0) {
            new ProcessBuilder("say", escaped).redirectErrorStream(true).start().waitFor();
        }
    }

    private static void speakEspeak(String bin, String text) throws Exception {
        String escaped = shellSafe(text);
        if (escaped.isBlank()) return;
        new ProcessBuilder(bin, "-v", "ru", escaped)
                .redirectErrorStream(true)
                .start()
                .waitFor();
    }

    private static void speakSpdSay(String text) throws Exception {
        String escaped = shellSafe(text);
        if (escaped.isBlank()) return;
        new ProcessBuilder("spd-say", "-l", "ru", escaped)
                .redirectErrorStream(true)
                .start()
                .waitFor();
    }

    private static boolean commandExists(String name) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            ProcessBuilder pb = os.contains("win")
                    ? new ProcessBuilder("where", name)
                    : new ProcessBuilder("sh", "-c", "command -v " + name);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = br.readLine();
                int code = p.waitFor();
                return code == 0 && line != null && !line.isBlank();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static String shellSafe(String text) {
        return text.replace("'", "").replace("\"", "").replace("`", "")
                .replace("$", "").replace(";", "").replace("|", "").replace("&", "")
                .replace("(", "").replace(")", "").replace("\\", "")
                .replace("\n", " ").replace("\r", " ").trim();
    }

    private static String sanitize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
                continue;
            }
            if (Character.isISOControl(c)) continue;
            sb.append(c);
        }
        return sb.toString()
                .replace('×', ' ')
                .replaceAll("(?i)[xх]\\s*\\d+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
