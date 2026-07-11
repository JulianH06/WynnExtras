package julianh06.wynnextras.features.debug;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Records raw game data (titles, scoreboard lines, chat, action bar, boss bars,
 * container titles) into a JSONL file while enabled. Toggled with /we record.
 *
 * Purpose: collect real Wynncraft protocol samples so detection patterns
 * (raids, world state, chat types, ...) can be built and tested against
 * actual captured data instead of guesswork.
 */
public class GameDataRecorder {
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static boolean enabled = false;
    private static BufferedWriter writer = null;
    private static Path currentFile = null;
    private static int lineCount = 0;

    public static void register() {
        // Chat + action bar arrive through Fabric's message events; everything
        // packet-shaped (titles, scoreboard, boss bars, container titles) is fed
        // by GameDataRecorderMixin. Recording never alters the message flow.
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            record(overlay ? "actionbar" : "chat", message);
            return true;
        });
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static String toggle() {
        if (enabled) {
            close();
            return "§cRecording stopped§7 — " + lineCount + " entries in " + currentFile.getFileName();
        }
        try {
            Path dir = FabricLoader.getInstance().getGameDir().resolve("wynnextras").resolve("capture");
            Files.createDirectories(dir);
            currentFile = dir.resolve("capture-" + LocalDateTime.now().format(FILE_STAMP) + ".jsonl");
            writer = Files.newBufferedWriter(currentFile, StandardCharsets.UTF_8);
            lineCount = 0;
            enabled = true;
            return "§aRecording game data§7 to " + currentFile.getFileName() + " — run §f/we record§7 again to stop.";
        } catch (IOException e) {
            return "§cCouldn't start recording: " + e.getMessage();
        }
    }

    public static void record(String type, Text text, String... extraKeyValues) {
        if (!enabled || text == null) return;

        JsonObject entry = new JsonObject();
        entry.addProperty("t", System.currentTimeMillis());
        entry.addProperty("type", type);
        entry.addProperty("plain", text.getString());
        // Full component JSON keeps colors/fonts/hover data that getString() drops —
        // Wynncraft encodes a lot of meaning in styling, so patterns need it.
        try {
            TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, text)
                    .result()
                    .ifPresent(json -> entry.add("json", json));
        } catch (Exception ignored) {} // some hover payloads need registry context; plain text is still recorded
        addExtras(entry, extraKeyValues);
        write(entry);
    }

    public static void recordRaw(String type, String value, String... extraKeyValues) {
        if (!enabled || value == null) return;

        JsonObject entry = new JsonObject();
        entry.addProperty("t", System.currentTimeMillis());
        entry.addProperty("type", type);
        entry.addProperty("plain", value);
        addExtras(entry, extraKeyValues);
        write(entry);
    }

    private static void addExtras(JsonObject entry, String... extraKeyValues) {
        for (int i = 0; i + 1 < extraKeyValues.length; i += 2) {
            entry.addProperty(extraKeyValues[i], extraKeyValues[i + 1]);
        }
    }

    private static synchronized void write(JsonObject entry) {
        if (writer == null) return;
        try {
            writer.write(entry.toString());
            writer.newLine();
            writer.flush();
            lineCount++;
        } catch (IOException e) {
            close();
        }
    }

    private static synchronized void close() {
        enabled = false;
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {}
            writer = null;
        }
    }
}
