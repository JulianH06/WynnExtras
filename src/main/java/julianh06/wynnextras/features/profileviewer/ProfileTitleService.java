package julianh06.wynnextras.features.profileviewer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.utils.BackendErrorLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ProfileTitleService {
    private static final String TITLES_URL = "https://wynnextras.com/api/profile-titles";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private static final AtomicBoolean FETCH_STARTED = new AtomicBoolean();

    private static volatile HashMap<String, String> titlesByUsername = new HashMap<>();

    private ProfileTitleService() {}

    public static void fetch() {
        if (!FETCH_STARTED.compareAndSet(false, true)) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TITLES_URL))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        BackendErrorLogger.error("profile-titles",
                                "Failed to fetch profile titles, invalid status: " + response.statusCode());
                        return;
                    }

                    try {
                        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                        HashMap<String, String> fetchedTitles = new HashMap<>();

                        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                            if (!entry.getValue().isJsonPrimitive()
                                    || !entry.getValue().getAsJsonPrimitive().isString()) continue;

                            String username = entry.getKey().trim();
                            String title = entry.getValue().getAsString().trim();
                            if (username.isEmpty() || title.isEmpty()) continue;

                            fetchedTitles.put(username.toLowerCase(Locale.ROOT), title);
                        }

                        titlesByUsername = fetchedTitles;
                        WynnExtras.LOGGER.info("[WynnExtras] Successfully fetched {} profile titles", fetchedTitles.size());
                    } catch (Exception e) {
                        BackendErrorLogger.error("profile-titles",
                                "Failed to parse profile titles: " + e.getMessage());
                    }
                })
                .exceptionally(error -> {
                    BackendErrorLogger.error("profile-titles",
                            "Failed to fetch profile titles: " + error.getMessage());
                    return null;
                });
    }

    public static String getTitle(String username) {
        if (username == null) return null;
        return titlesByUsername.get(username.toLowerCase(Locale.ROOT));
    }
}