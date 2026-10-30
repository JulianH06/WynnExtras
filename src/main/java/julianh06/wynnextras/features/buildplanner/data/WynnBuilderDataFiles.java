package julianh06.wynnextras.features.buildplanner.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import julianh06.wynnextras.features.buildplanner.BuildPlannerRuntime;
import julianh06.wynnextras.features.buildplanner.PlannerLog;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderVersions;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Runtime cache for WynnBuilder-only metadata that the official Wynncraft API does not expose. */
public final class WynnBuilderDataFiles {
    private static final String VERSION = System.getProperty(
            "wynnextras.wynnbuilder.version", WynnBuilderVersions.latest());
    private static final URI ROOT = URI.create("https://wynnbuilder.github.io/data/" + VERSION + "/");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final Map<File, JsonElement> DATA = new ConcurrentHashMap<>();
    private static final AtomicBoolean REFRESH_STARTED = new AtomicBoolean();

    private WynnBuilderDataFiles() {}

    public static JsonElement read(File file) {
        JsonElement loaded = DATA.get(file);
        if (loaded != null) return loaded.deepCopy();
        synchronized (DATA) {
            loaded = DATA.get(file);
            if (loaded == null) {
                loaded = load(file);
                DATA.put(file, loaded);
            }
        }
        return loaded.deepCopy();
    }

    public static JsonElement readCached(File file) {
        JsonElement loaded = DATA.get(file);
        if (loaded != null) return loaded.deepCopy();
        Path path = path(file);
        if (!Files.isRegularFile(path)) return null;
        synchronized (DATA) {
            loaded = DATA.get(file);
            if (loaded == null) {
                try {
                    loaded = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
                    DATA.put(file, loaded);
                } catch (IOException | RuntimeException exception) {
                    PlannerLog.LOGGER.warn("Ignoring invalid cached WynnBuilder data {}.", path, exception);
                    return null;
                }
            }
        }
        return loaded.deepCopy();
    }

    public static void refreshAll() {
        if (!REFRESH_STARTED.compareAndSet(false, true)) return;
        for (File file : File.values()) {
            try {
                JsonElement refreshed = fetch(file);
                write(file, refreshed);
                DATA.put(file, refreshed);
            } catch (RuntimeException exception) {
                PlannerLog.LOGGER.warn("Could not refresh WynnBuilder {} data; cached data remains available.",
                        file.fileName, exception);
            }
        }
    }

    private static JsonElement load(File file) {
        Path path = path(file);
        if (Files.isRegularFile(path)) {
            try {
                return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            } catch (IOException | RuntimeException exception) {
                PlannerLog.LOGGER.warn("Ignoring invalid cached WynnBuilder data {}.", path, exception);
            }
        }
        JsonElement fetched = fetch(file);
        write(file, fetched);
        return fetched;
    }

    private static JsonElement fetch(File file) {
        try {
            HttpRequest request = HttpRequest.newBuilder(ROOT.resolve(file.fileName))
                    .header("Accept", "application/json")
                    .header("User-Agent", "WynnExtras-BuildPlanner")
                    .timeout(Duration.ofSeconds(90)).GET().build();
            HttpResponse<String> response = CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("HTTP " + response.statusCode());
            }
            return JsonParser.parseString(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("WynnBuilder " + file.fileName
                    + " is unavailable and no cache exists", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading WynnBuilder " + file.fileName, exception);
        }
    }

    private static void write(File file, JsonElement json) {
        Path target = path(file);
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(temporary, json.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            PlannerLog.LOGGER.warn("Could not cache WynnBuilder data {}.", target, exception);
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
            }
        }
    }

    private static Path path(File file) {
        Path base;
        try {
            base = BuildPlannerRuntime.configDirectory();
        } catch (RuntimeException exception) {
            base = Path.of(System.getProperty("java.io.tmpdir"), "wynnextras-test-cache");
        }
        return base.resolve("wynnbuilder-data").resolve(VERSION).resolve(file.fileName);
    }

    public enum File {
        ITEMS("items.json"),
        ASPECTS("aspects.json"),
        ABILITY_TREE("atree.json"),
        INGREDIENTS("ingreds.json"),
        RECIPES("recipes.json"),
        TOMES("tomes.json");

        private final String fileName;

        File(String fileName) {
            this.fileName = fileName;
        }
    }
}
