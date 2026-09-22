package julianh06.wynnextras.features.leaderboardviewer;

import com.google.gson.JsonArray;
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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class GuildSeasonService {
    private static final long CACHE_DURATION_MS = 10 * 60 * 1000;
    private static final CompletableFuture<HttpClient> HTTP_CLIENT = CompletableFuture.supplyAsync(() ->
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    private static Map<String, GuildSeason> cachedSeasons;
    private static long fetchedAt;
    private static CompletableFuture<Map<String, GuildSeason>> pendingRequest;

    private GuildSeasonService() {
    }

    public static synchronized CompletableFuture<Map<String, GuildSeason>> fetchSeasons() {
        if (cachedSeasons != null && System.currentTimeMillis() - fetchedAt < CACHE_DURATION_MS) {
            return CompletableFuture.completedFuture(cachedSeasons);
        }
        if (pendingRequest != null) return pendingRequest;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.wynncraft.com/v3/guild/seasons"))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        pendingRequest = HTTP_CLIENT
                .thenCompose(client -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new CompletionException(new IOException(
                                "Guild seasons request failed with HTTP " + response.statusCode()));
                    }
                    return parseSeasons(response.body());
                });
        pendingRequest.whenComplete((seasons, error) -> {
            synchronized (GuildSeasonService.class) {
                pendingRequest = null;
                if (error == null) {
                    cachedSeasons = seasons;
                    fetchedAt = System.currentTimeMillis();
                } else {
                    WynnExtras.LOGGER.error("Failed to fetch guild seasons", error);
                }
            }
        });
        return pendingRequest;
    }

    private static Map<String, GuildSeason> parseSeasons(String body) {
        JsonObject response = JsonParser.parseString(body).getAsJsonObject();
        Map<String, GuildSeason> seasons = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : response.entrySet()) {
            JsonObject season = entry.getValue().getAsJsonObject();
            int number = Integer.parseInt(entry.getKey());
            seasons.put("guildSeason" + number, new GuildSeason(
                    number,
                    parseDate(season, "startDate", "initDate"),
                    parseDate(season, "endDate", null),
                    parseRewards(season.getAsJsonArray("ratingRewards")),
                    parseRewards(season.getAsJsonArray("leaderboardRewards"))));
        }
        return Map.copyOf(seasons);
    }

    private static Instant parseDate(JsonObject object, String field, String fallbackField) {
        JsonElement value = object.get(field);
        if ((value == null || value.isJsonNull()) && fallbackField != null) value = object.get(fallbackField);
        return Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(value.getAsString()));
    }

    private static List<GuildSeason.Reward> parseRewards(JsonArray rewards) {
        List<GuildSeason.Reward> result = new ArrayList<>();
        for (JsonElement element : rewards) {
            JsonObject reward = element.getAsJsonObject();
            JsonObject condition = reward.getAsJsonObject("condition");
            JsonElement value = reward.get("value");
            JsonElement expires = reward.get("expires");
            result.add(new GuildSeason.Reward(
                    reward.get("type").getAsString(),
                    value == null || value.isJsonNull() ? null : value.getAsString(),
                    expires == null || expires.isJsonNull()
                            ? null : Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(expires.getAsString())),
                    condition.get("type").getAsString(),
                    condition.get("value").getAsInt()));
        }
        return List.copyOf(result);
    }
}
