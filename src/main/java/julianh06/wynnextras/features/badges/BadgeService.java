package julianh06.wynnextras.features.badges;

import julianh06.wynnextras.core.WynnExtras;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import julianh06.wynnextras.duck.EntityRenderStateAccess;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.CurrentVersionData;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.utils.ApiRequestHelper;
import julianh06.wynnextras.utils.MojangAuth;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String ACTIVE_DETAILS_URL = "http://wynnextras.com/wynnextras-users/active/details";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new GsonBuilder().create();
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private static final Map<String, BadgeProfile> profilesByUuid = new ConcurrentHashMap<>();
    private static final Map<String, BadgeProfile> profilesByUsername = new ConcurrentHashMap<>();
    private static long lastSyncTime = 0;
    private static final long SYNC_INTERVAL_MS = 1_200_000; // 20 minutes

    private static int tickCounter = 0;

    /**
     * Check if a player UUID is a WynnExtras user
     */
    public static boolean isWynnExtrasUser(String uuid) {
        if (!WynnExtrasConfig.INSTANCE.showWynnExtrasBadges) return false;
        String normalizedUuid = BadgeProfile.normalizeUuid(uuid);
        return normalizedUuid != null && profilesByUuid.containsKey(normalizedUuid);
    }

    public static Text appendBadge(EntityRenderState state, Text label) {
        if (!WynnExtrasConfig.INSTANCE.showWynnExtrasBadges || label == null || state == null) return label;
        if (hasKnownBadgeSuffix(label.getString())) return label;

        BadgeProfile profile = profileFor(state, label);
        if (profile == null) return label;

        MutableText result = label.copy();
        result.append(Text.literal(" "));
        result.append(BadgeCatalog.badgeText(profile.selectedIconId, profile.selectedColorId));
        return result;
    }

    public static void syncWithServerSoon() {
        lastSyncTime = 0;
        tickCounter = 199;
    }

    public static void reloadBadgeInfoFromServer() {
        getActiveUsers();
        getActiveUserDetails();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!MinecraftUtils.isOnWynncraft()) return;

        handleTick();
    }

    private static void handleTick() {
        tickCounter++;
        if (tickCounter % 200 != 0) return;
        if (lastSyncTime != 0 && System.currentTimeMillis() - lastSyncTime < SYNC_INTERVAL_MS) return;

        syncWithServer();
    }

    private static void syncWithServer() {
        lastSyncTime = System.currentTimeMillis();
        BadgeProfileData.load();

        MojangAuth.getWEToken().thenAccept(wynnextrasToken -> {
            if (wynnextrasToken == null) {
                WynnExtras.LOGGER.error("[WynnExtras] Failed to get auth data for badge sync");
                return;
            }

            sendHeartbeat(wynnextrasToken);
            getActiveUsers();
            getActiveUserDetails();
        }).exceptionally(e -> {
            WynnExtras.LOGGER.error("[WynnExtras] Error getting auth data: " + e.getMessage());
            return null;
        });
    }

    private static void getActiveUserDetails() {
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ACTIVE_DETAILS_URL))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    parseDetailsResponse(response.body());
                }
            } catch (Exception e) {
                WynnExtras.LOGGER.error("[WynnExtras] Badge details fetching error: " + e.getMessage());
            }
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
                    WynnExtras.LOGGER.error("[WynnExtras] Badge fetching failed: " + response.statusCode());
                }
            } catch (Exception e) {
                WynnExtras.LOGGER.error("[WynnExtras] Badge fetching error: " + e.getMessage());
            }
        });
    }

    private static void sendHeartbeat(String wynnextrasToken) {
        CompletableFuture.runAsync(() -> {
            try {
                BadgeProfile profile = BadgeProfileData.getLocalProfile();
                JsonObject body = new JsonObject();
                body.addProperty("modVersion", CurrentVersionData.INSTANCE.version);
                body.addProperty("badgeIconId", profile.selectedIconId);
                body.addProperty("badgeColorId", profile.selectedColorId);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(HEARTBEAT_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", wynnextrasToken)
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                        .build();

                ApiRequestHelper.sendWithAuthRetry(request, body).thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        WynnExtras.LOGGER.error("[WynnExtras] Badge heartbeat failed: " + response.statusCode());
                    }
                });
            } catch (Exception e) {
                WynnExtras.LOGGER.error("[WynnExtras] Badge heartbeat error: " + e.getMessage());
            }
        });
    }

    private static void parseResponse(String responseBody) {
        try {
            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);

            Map<String, BadgeProfile> nextByUuid = new ConcurrentHashMap<>();
            Map<String, BadgeProfile> nextByUsername = new ConcurrentHashMap<>();

            if (json.has("uuids")) {
                JsonArray uuids = json.getAsJsonArray("uuids");
                for (int i = 0; i < uuids.size(); i++) {
                    String uuid = BadgeProfile.normalizeUuid(uuids.get(i).getAsString());
                    if (uuid != null) {
                        BadgeProfile existing = profilesByUuid.get(uuid);
                        nextByUuid.put(uuid, existing != null
                                ? existing
                                : new BadgeProfile(uuid, null, BadgeCatalog.DEFAULT_ICON_ID, BadgeCatalog.DEFAULT_COLOR_ID));
                    }
                }
            }

            if (json.has("badges") && json.get("badges").isJsonArray()) {
                JsonArray badges = json.getAsJsonArray("badges");
                for (JsonElement element : badges) {
                    if (!element.isJsonObject()) continue;
                    BadgeProfile profile = parseProfile(element.getAsJsonObject());
                    if (profile == null || profile.uuid == null) continue;
                    nextByUuid.put(profile.uuid, profile);
                    if (profile.username != null && !profile.username.isBlank()) {
                        nextByUsername.put(profile.username.toLowerCase(), profile);
                    }
                }
            }

            profilesByUuid.clear();
            profilesByUuid.putAll(nextByUuid);
            profilesByUsername.clear();
            profilesByUsername.putAll(nextByUsername);
            for (BadgeProfile profile : profilesByUuid.values()) {
                if (profile.username != null && !profile.username.isBlank()) {
                    profilesByUsername.put(profile.username.toLowerCase(), profile);
                }
            }

            if (json.has("count")) {
                int count = json.get("count").getAsInt();
                WynnExtras.LOGGER.info("[WynnExtras] Server reports " + count + " active users");
            } else {
                WynnExtras.LOGGER.info("[WynnExtras] Synced " + profilesByUuid.size() + " active badge users");
            }
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error parsing badge response: " + e.getMessage());
        }
    }

    private static BadgeProfile parseProfile(JsonObject obj) {
        String uuid = jsonString(obj, "uuid");
        String username = jsonString(obj, "username");
        String iconId = jsonString(obj, "iconId");
        String colorId = jsonString(obj, "colorId");
        BadgeProfile profile = new BadgeProfile(uuid, username,
                iconId == null || iconId.isBlank() ? BadgeCatalog.DEFAULT_ICON_ID : iconId,
                colorId == null || colorId.isBlank() ? BadgeCatalog.DEFAULT_COLOR_ID : colorId);
        profile.sanitize(false);
        return profile.uuid == null ? null : profile;
    }

    private static void parseDetailsResponse(String responseBody) {
        try {
            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
            if (!json.has("users") || !json.get("users").isJsonArray()) return;

            for (JsonElement element : json.getAsJsonArray("users")) {
                if (!element.isJsonObject()) continue;
                JsonObject obj = element.getAsJsonObject();
                String uuid = BadgeProfile.normalizeUuid(jsonString(obj, "uuid"));
                String username = jsonString(obj, "username");
                if (uuid == null || username == null || username.isBlank()) continue;

                BadgeProfile profile = profilesByUuid.get(uuid);
                if (profile == null) {
                    profile = new BadgeProfile(uuid, username, BadgeCatalog.DEFAULT_ICON_ID, BadgeCatalog.DEFAULT_COLOR_ID);
                    profilesByUuid.put(uuid, profile);
                } else {
                    profile.username = username;
                }
                profilesByUsername.put(username.toLowerCase(), profile);
            }
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error parsing badge details response: " + e.getMessage());
        }
    }

    private static String jsonString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsString();
    }

    private static BadgeProfile profileFor(EntityRenderState state, Text label) {
        Entity entity = ((EntityRenderStateAccess) state).wynnExtras$getEntity();
        if (entity instanceof PlayerEntity player) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && player.getUuid().equals(mc.player.getUuid())) {
                return BadgeProfileData.getLocalProfile();
            }
            BadgeProfile profile = profilesByUuid.get(BadgeProfile.normalizeUuid(player.getUuidAsString()));
            if (profile != null) return profile;
        }

        BadgeProfile labelProfile = profileFromLabel(label);
        if (labelProfile == null) return null;
        return labelProfile;
    }

    private static BadgeProfile profileFromLabel(Text label) {
        String value = stripKnownBadge(label.getString()).trim();
        if (value.isEmpty()) return null;
        String[] parts = value.split("\\s+");
        if (parts.length == 0) return null;

        MinecraftClient mc = MinecraftClient.getInstance();
        String localName = mc.player == null ? null : mc.player.getGameProfile().name();
        if (localName != null) {
            for (String part : parts) {
                String candidate = cleanUsernameCandidate(part);
                if (candidate != null && candidate.equalsIgnoreCase(localName)) {
                    return BadgeProfileData.getLocalProfile();
                }
            }
        }

        for (int i = parts.length - 1; i >= 0; i--) {
            String candidate = cleanUsernameCandidate(parts[i]);
            if (candidate == null) continue;
            BadgeProfile profile = profilesByUsername.get(candidate.toLowerCase());
            if (profile != null) return profile;
        }
        return null;
    }

    private static String cleanUsernameCandidate(String value) {
        String candidate = value.replaceAll("^[^A-Za-z0-9_]+|[^A-Za-z0-9_]+$", "");
        Matcher matcher = USERNAME_PATTERN.matcher(candidate);
        return matcher.matches() ? candidate : null;
    }

    private static String stripKnownBadge(String value) {
        String stripped = value;
        for (BadgeCatalog.BadgeIcon icon : BadgeCatalog.icons()) {
            if (stripped.endsWith(icon.glyph())) {
                return stripped.substring(0, stripped.length() - icon.glyph().length()).trim();
            }
        }
        return stripped;
    }

    private static boolean hasKnownBadgeSuffix(String value) {
        String stripped = value == null ? "" : value.trim();
        for (BadgeCatalog.BadgeIcon icon : BadgeCatalog.icons()) {
            if (stripped.endsWith(icon.glyph())) return true;
        }
        return false;
    }
}
