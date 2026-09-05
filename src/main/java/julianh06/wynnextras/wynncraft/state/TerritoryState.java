package julianh06.wynnextras.wynncraft.state;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import julianh06.wynnextras.core.WynnExtras;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TerritoryState {
    private static final String TERRITORY_LIST_URL = "https://api.wynncraft.com/v3/guild/list/territory";
    private static final long FETCH_RETRY_MS = 60_000L;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final Map<String, String> DEFENSES = new ConcurrentHashMap<>();
    private static final AtomicBoolean FETCHING = new AtomicBoolean(false);
    private static volatile Map<String, TerritoryBounds> territories = Map.of();
    private static volatile long lastFetchAttempt;
    private static boolean loggedFailure;
    private static Map<String, TerritoryBounds> cachedTerritories = Map.of();
    private static double cachedX = Double.NaN;
    private static double cachedZ = Double.NaN;
    private static String cachedCurrentTerritory;

    private TerritoryState() {}

    public static void initialize() {
        fetchIfNeeded();
    }

    public static Optional<String> currentTerritory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return Optional.empty();
        if (territories.isEmpty()) fetchIfNeeded();

        double x = client.player.getX();
        double z = client.player.getZ();
        Map<String, TerritoryBounds> currentTerritories = territories;
        if (currentTerritories != cachedTerritories || x != cachedX || z != cachedZ) {
            cachedTerritories = currentTerritories;
            cachedX = x;
            cachedZ = z;
            cachedCurrentTerritory = null;
            for (Map.Entry<String, TerritoryBounds> entry : currentTerritories.entrySet()) {
                if (!entry.getValue().contains(x, z)) continue;
                cachedCurrentTerritory = entry.getKey();
                break;
            }
        }
        return Optional.ofNullable(cachedCurrentTerritory).or(WarState::territory);
    }

    public static Optional<TerritoryCenter> center(String territory) {
        TerritoryBounds bounds = territories.get(territory);
        if (bounds == null) {
            fetchIfNeeded();
            return Optional.empty();
        }
        return Optional.of(new TerritoryCenter((bounds.startX + bounds.endX) / 2.0,
                (bounds.startZ + bounds.endZ) / 2.0));
    }

    public static Optional<String> defense(String territory) {
        return territory == null ? Optional.empty() : Optional.ofNullable(DEFENSES.get(territory));
    }

    public static void cacheDefense(String territory, String defense) {
        if (territory != null && !territory.isBlank() && defense != null && !defense.isBlank()) {
            DEFENSES.put(territory, defense);
        }
    }

    private static void fetchIfNeeded() {
        if (!territories.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now - lastFetchAttempt < FETCH_RETRY_MS || !FETCHING.compareAndSet(false, true)) return;
        lastFetchAttempt = now;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TERRITORY_LIST_URL))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("Territory API returned " + response.statusCode());
                    }
                    return parseTerritories(response.body());
                })
                .thenAccept(parsed -> {
                    if (parsed.isEmpty()) return;
                    territories = parsed;
                    loggedFailure = false;
                })
                .exceptionally(exception -> {
                    if (!loggedFailure) {
                        loggedFailure = true;
                        WynnExtras.LOGGER.warn("Failed to load territory data", exception);
                    }
                    return null;
                })
                .whenComplete((ignored, exception) -> FETCHING.set(false));
    }

    private static Map<String, TerritoryBounds> parseTerritories(String body) {
        Map<String, TerritoryBounds> parsed = new HashMap<>();
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            try {
                JsonObject data = entry.getValue().getAsJsonObject();
                JsonObject location = data.getAsJsonObject("location");
                if (location == null) continue;
                JsonArray start = location.getAsJsonArray("start");
                JsonArray end = location.getAsJsonArray("end");
                if (start == null || end == null || start.size() < 2 || end.size() < 2) continue;

                int firstX = start.get(0).getAsInt();
                int firstZ = start.get(1).getAsInt();
                int secondX = end.get(0).getAsInt();
                int secondZ = end.get(1).getAsInt();
                parsed.put(entry.getKey(), new TerritoryBounds(
                        Math.min(firstX, secondX), Math.min(firstZ, secondZ),
                        Math.max(firstX, secondX), Math.max(firstZ, secondZ)));
            } catch (RuntimeException ignored) {}
        }
        return parsed;
    }

    public record TerritoryCenter(double x, double z) {}

    private record TerritoryBounds(int startX, int startZ, int endX, int endZ) {
        private boolean contains(double x, double z) {
            return x >= startX && x <= endX && z >= startZ && z <= endZ;
        }
    }
}
