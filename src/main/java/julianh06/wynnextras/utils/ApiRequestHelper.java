package julianh06.wynnextras.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class ApiRequestHelper {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson GSON = new GsonBuilder().create();

    public static CompletableFuture<HttpResponse<String>> sendWithAuthRetry(HttpRequest request, JsonObject oldBody) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenCompose(response -> {
                if (response.statusCode() == 401) {
                    // Token expired → refresh & retry once
                    return MojangAuth.refreshSession().thenCompose(newToken -> {
                        if(request.bodyPublisher().isEmpty()) return null;

                        HttpRequest retryRequest = HttpRequest.newBuilder()
                            .uri(request.uri())
                            .header("Content-Type", "application/json")
                            .header("Authorization", newToken)
                            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(oldBody)))
                            .build();

                        return httpClient.sendAsync(retryRequest, HttpResponse.BodyHandlers.ofString());
                    });
                }

                return CompletableFuture.completedFuture(response);
            });
    }
}