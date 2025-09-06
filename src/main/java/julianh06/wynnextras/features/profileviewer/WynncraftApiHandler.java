package julianh06.wynnextras.features.profileviewer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import julianh06.wynnextras.features.profileviewer.data.OffsetDateTimeAdapter;
import julianh06.wynnextras.features.profileviewer.data.PlayerData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

public class WynncraftApiHandler {
    private static final String BASE_URL = "https://api.wynncraft.com/v3/player/";

    public static CompletableFuture<PlayerData> fetchPlayerData(String playerName) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + playerName + "?fullResult"))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(WynncraftApiHandler::parsePlayerData);
    }

    private static PlayerData parsePlayerData(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
                .create();
        return gson.fromJson(json, PlayerData.class);
    }
}
