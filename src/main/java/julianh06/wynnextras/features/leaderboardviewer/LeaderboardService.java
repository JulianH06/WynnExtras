package julianh06.wynnextras.features.leaderboardviewer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import julianh06.wynnextras.core.WynnExtras;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;

public final class LeaderboardService {
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9]+");
    private static final long CACHE_DURATION_MS = 60 * 1000;
    private static final CompletableFuture<HttpClient> HTTP_CLIENT = CompletableFuture.supplyAsync(() ->
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build());
    private static final Map<RequestKey, CacheEntry> CACHE = new HashMap<>();
    private static final Map<RequestKey, CompletableFuture<List<LeaderboardEntry>>> PENDING_REQUESTS = new HashMap<>();

    private LeaderboardService() {
    }

    public static CompletableFuture<List<LeaderboardEntry>> fetchLeaderboard(String id) {
        return fetchLeaderboard(id, 100);
    }

    public static synchronized CompletableFuture<List<LeaderboardEntry>> fetchLeaderboard(String id, int limit) {
        if (id == null || !VALID_ID.matcher(id).matches()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid leaderboard id: " + id));
        }

        RequestKey key = new RequestKey(id, Math.clamp(limit, 1, 1000));
        CacheEntry cached = CACHE.get(key);
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt() < CACHE_DURATION_MS) {
            return CompletableFuture.completedFuture(cached.entries());
        }

        CompletableFuture<List<LeaderboardEntry>> pending = PENDING_REQUESTS.get(key);
        if (pending != null) return pending;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.wynncraft.com/v3/leaderboards/" + key.id() + "?resultLimit=" + key.limit()))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        CompletableFuture<List<LeaderboardEntry>> future = HTTP_CLIENT
                .thenCompose(client -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new CompletionException(new IOException(
                                "Leaderboard request failed with HTTP " + response.statusCode()));
                    }
                    return parseEntries(response.body());
                });

        PENDING_REQUESTS.put(key, future);
        future.whenComplete((entries, error) -> {
            synchronized (LeaderboardService.class) {
                PENDING_REQUESTS.remove(key, future);
                if (error == null) {
                    CACHE.put(key, new CacheEntry(entries, System.currentTimeMillis()));
                } else {
                    WynnExtras.LOGGER.error("Failed to fetch leaderboard {}", key.id(), error);
                }
            }
        });
        return future;
    }

    private static List<LeaderboardEntry> parseEntries(String responseBody) {
        JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
        List<LeaderboardEntry> entries = new ArrayList<>();

        for (Map.Entry<String, JsonElement> responseEntry : response.entrySet()) {
            int rank = Integer.parseInt(responseEntry.getKey());
            JsonObject entry = responseEntry.getValue().getAsJsonObject();
            JsonObject metadata = entry.has("metadata") && entry.get("metadata").isJsonObject()
                    ? entry.getAsJsonObject("metadata")
                    : null;

            entries.add(new LeaderboardEntry(
                    rank,
                    getNullableString(entry, "name"),
                    getNullableString(entry, "uuid"),
                    getNullableString(entry, "prefix"),
                    getNullableDouble(entry, "score"),
                    metadata,
                    entry
            ));
        }

        entries.sort(Comparator.comparingInt(LeaderboardEntry::rank));
        return List.copyOf(entries);
    }

    private static String getNullableString(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static Double getNullableDouble(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value == null || value.isJsonNull() ? null : value.getAsDouble();
    }

    private record RequestKey(String id, int limit) {
    }

    private record CacheEntry(List<LeaderboardEntry> entries, long fetchedAt) {
    }
}
