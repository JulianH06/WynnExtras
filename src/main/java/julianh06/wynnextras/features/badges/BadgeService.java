package julianh06.wynnextras.features.badges;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wynntils.core.components.Models;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.CurrentVersionData;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.event.WorldChangeEvent;
import julianh06.wynnextras.utils.MojangAuth;
import net.minecraft.client.MinecraftClient;
import net.neoforged.bus.api.SubscribeEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WynnExtras user badges (star indicators).
 *
 * On world join and every 600 seconds:
 * - Sends heartbeat to wynnextras.com with UUID and mod version
 * - Receives list of active WynnExtras users (last 7 days)
 * - Caches UUIDs in a HashSet for O(1) lookup
 */
@WEModule
public class BadgeService {
    private static final String HEARTBEAT_URL = "http://wynnextras.com/wynnextras-users/heartbeat";
    private static final String ACTIVE_URL = "http://wynnextras.com/wynnextras-users/active";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new GsonBuilder().create();

    private static final Set<String> wynnextrasUsers = ConcurrentHashMap.newKeySet();
    private static long lastSyncTime = 0;
    private static final long SYNC_INTERVAL_MS = 600_000; // 10 minutes

    private static int tickCounter = 0;
    private static boolean initialSyncDone = false;

    /**
     * Check if a player UUID is a WynnExtras user
     */
    public static boolean isWynnExtrasUser(String uuid) {
        if (!WynnExtrasConfig.INSTANCE.badgesEnabled) return false;
        // Normalize UUID format (remove dashes if present)
        String normalizedUuid = uuid.replace("-", "").toLowerCase();
        return wynnextrasUsers.contains(normalizedUuid);
    }

    @SubscribeEvent
    public void onWorldChange(WorldChangeEvent event) {
        // Sync on world change
        if (Models.WorldState.onWorld()) {
            syncWithServer();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!Models.WorldState.onWorld()) return;

        tickCounter++;
        if (tickCounter % 400 != 0) return; // Check every 20 seconds

        // Initial sync
        if (!initialSyncDone) {
            initialSyncDone = true;
            syncWithServer();
            return;
        }

        // Periodic sync
        long now = System.currentTimeMillis();
        if (now - lastSyncTime >= SYNC_INTERVAL_MS) {
            syncWithServer();
        }
    }

    private static void syncWithServer() {
        if (!WynnExtrasConfig.INSTANCE.badgesEnabled) return;

        lastSyncTime = System.currentTimeMillis();

        // Get authentication data
        MojangAuth.getAuthData().thenAccept(authData -> {
            if (authData == null) {
                System.err.println("[WynnExtras] Failed to get auth data for badge sync");
                return;
            }

            sendHeartbeat(authData.username, authData.serverId);
            getActiveUsers();
        }).exceptionally(e -> {
            System.err.println("[WynnExtras] Error getting auth data: " + e.getMessage());
            return null;
        });
    }

    private static void getActiveUsers() {
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ACTIVE_URL))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    parseResponse(response.body());
                } else {
                    System.err.println("[WynnExtras] Badge fetching failed: " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("[WynnExtras] Badge fetching error: " + e.getMessage());
            }
        });
    }

    private static void sendHeartbeat(String username, String serverId) {
        CompletableFuture.runAsync(() -> {
            try {
                // Build request body
                JsonObject body = new JsonObject();
                body.addProperty("modVersion", CurrentVersionData.INSTANCE.version);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(HEARTBEAT_URL))
                        .header("Content-Type", "application/json")
                        .header("Username", username)
                        .header("Server-ID", serverId)
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    System.err.println("[WynnExtras] Badge heartbeat failed: " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("[WynnExtras] Badge heartbeat error: " + e.getMessage());
            }
        });
    }

    private static void parseResponse(String responseBody) {
        try {
            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);

            if (json.has("uuids")) {
                JsonArray uuids = json.getAsJsonArray("uuids");
                wynnextrasUsers.clear();
                for (int i = 0; i < uuids.size(); i++) {
                    String uuid = uuids.get(i).getAsString().replace("-", "").toLowerCase();
                    wynnextrasUsers.add(uuid);
                }
                System.out.println("[WynnExtras] Synced " + wynnextrasUsers.size() + " active badge users");
            }

            if (json.has("count")) {
                int count = json.get("count").getAsInt();
                System.out.println("[WynnExtras] Server reports " + count + " active users");
            }
        } catch (Exception e) {
            System.err.println("[WynnExtras] Error parsing badge response: " + e.getMessage());
        }
    }

    /**
     * Force a sync with the server (useful for testing)
     */
    public static void forceSync() {
        syncWithServer();
    }
}
