package julianh06.wynnextras.features.badges;

import julianh06.wynnextras.core.WynnExtras;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynntils.mc.extension.EntityRenderStateExtension;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.CurrentVersionData;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.event.WorldChangeEvent;
import julianh06.wynnextras.features.achievements.Achievement;
import julianh06.wynnextras.features.achievements.AchievementId;
import julianh06.wynnextras.features.achievements.AchievementTracking;
import julianh06.wynnextras.features.achievements.Achievements;
import julianh06.wynnextras.features.achievements.ProgressAchievement;
import julianh06.wynnextras.features.achievements.TieredAchievement;
import julianh06.wynnextras.utils.ApiRequestHelper;
import julianh06.wynnextras.utils.BackendErrorLogger;
import julianh06.wynnextras.utils.MojangAuth;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.neoforged.bus.api.SubscribeEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private static final String ACHIEVEMENTS_URL = "https://wynnextras.com/achievements?playerUuid=";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new GsonBuilder().create();
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private static final long ACHIEVEMENT_RETRY_DELAY_MS = 60_000;

    private static final Map<String, BadgeProfile> profilesByUuid = new ConcurrentHashMap<>();
    private static final Map<String, BadgeProfile> profilesByUsername = new ConcurrentHashMap<>();
    private static final Map<String, Achievements> achievementsByUuid = new ConcurrentHashMap<>();
    private static final Map<String, Long> achievementRetryAtByUuid = new ConcurrentHashMap<>();
    private static final Map<String, AchievementLoadStatus> achievementLoadStatusByUuid = new ConcurrentHashMap<>();
    private static final Achievements achievementDefinitions = Achievements.createDefaultAchievementSet();
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
        if (!WynnExtrasConfig.INSTANCE.showWynnExtrasBadges || label == null) return label;

        BadgeProfile profile = profileFor(state, label);
        if (profile == null) return label;

        return appendBadge(label, profile);
    }

    public static Text appendBadge(UUID uuid, String username, Text label) {
        if (!WynnExtrasConfig.INSTANCE.showWynnExtrasBadges || label == null) return label;

        BadgeProfile profile = getBadgeProfile(uuid, username);
        return profile == null ? label : appendBadge(label, profile);
    }

    public static List<Text> getBadgeTooltip(UUID uuid, String username) {
        if (!WynnExtrasConfig.INSTANCE.showWynnExtrasBadges) return List.of();

        BadgeProfile profile = getBadgeProfile(uuid, username);
        if (profile == null) return List.of();

        BadgeCatalog.BadgeIcon icon = BadgeCatalog.icon(profile.selectedIconId);
        BadgeCatalog.BadgeColor color = BadgeCatalog.color(profile.selectedColorId);
        Achievements achievements = achievementsForTooltip(uuid);
        AchievementLoadStatus loadStatus = achievementLoadStatus(uuid);
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal("WynnExtras Badge").formatted(Formatting.GOLD));
        tooltip.add(Text.literal("Icon: ").append(BadgeCatalog.badgeText(icon.id(), color.id())).append(" " + icon.displayName()));
        tooltip.addAll(badgeRequirementTooltip(icon.achievement(), icon.minTier(), achievements, loadStatus));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("Color: ").append(BadgeCatalog.colorPreviewText(color.id())).append(" " + color.displayName()));
        tooltip.addAll(badgeRequirementTooltip(color.achievement(), color.minTier(), achievements, loadStatus));
        return tooltip;
    }

    private static Text appendBadge(Text label, BadgeProfile profile) {
        if (hasKnownBadgeSuffix(label.getString())) return label;

        MutableText result = label.copy();
        result.append(Text.literal(" "));
        result.append(BadgeCatalog.badgeText(profile.selectedIconId, profile.selectedColorId));
        return result;
    }

    private static BadgeProfile getBadgeProfile(UUID uuid, String username) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (uuid != null && mc.player != null && uuid.equals(mc.player.getUuid())) {
            return BadgeProfileData.getLocalProfile();
        }

        if (uuid != null) {
            BadgeProfile profile = profilesByUuid.get(BadgeProfile.normalizeUuid(uuid.toString()));
            if (profile != null) return profile;
        }

        if (username == null || username.isBlank()) return null;
        return profilesByUsername.get(username.toLowerCase());
    }

    private static List<Text> badgeRequirementTooltip(AchievementId achievementId, Integer minTier, Achievements achievements, AchievementLoadStatus loadStatus) {
        if (achievementId == null) {
            return List.of(Text.of("§aUnlocked by default"));
        }

        Achievement achievement = (achievements == null ? achievementDefinitions : achievements).getById(achievementId.id());
        if (achievement == null) {
            return List.of(Text.of(BadgeCatalog.requirement(achievementId, minTier)));
        }

        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.of("§6" + achievement.getTitle() + (minTier == null ? "" : " (Tier " + minTier + ")")));
        tooltip.add(Text.of("§7" + achievement.getDescription()));
        if (achievement instanceof TieredAchievement tiered) {
            tooltip.add(Text.of("§8Tiers: " + tierMilestones(tiered)));
        }
        tooltip.add(Text.of("§8Progress: " + (achievements == null ? progressLoadText(loadStatus) : progressText(achievement))));
        return tooltip;
    }

    private static String progressText(Achievement achievement) {
        if (achievement instanceof TieredAchievement tiered) {
            List<Integer> targets = tiered.getLevelTargets();
            int maxTarget = targets.isEmpty() ? 0 : targets.getLast();
            int level = tiered.getCurrentLevel();
            if (achievement.isUnlocked() || level >= targets.size()) {
                return tiered.getCurrent() + "/" + maxTarget + " Tier " + tiered.getCurrentLevel() + " (MAX)";
            }
            int maxTier = Math.max(1, targets.size());
            return tiered.getCurrent() + "/" + targets.get(level) + " Tier " + tiered.getCurrentLevel() + "/" + maxTier;
        }
        if (achievement.isUnlocked()) return "Unlocked";
        if (achievement instanceof ProgressAchievement progress) {
            return progress.getCurrent() + "/" + progress.getTarget();
        }
        return "Locked";
    }

    private static String progressLoadText(AchievementLoadStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Not uploaded";
            case FAILED -> "Unavailable";
            case LOADING -> "Loading...";
        };
    }

    private static Achievements achievementsForTooltip(UUID uuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (uuid != null && mc.player != null && uuid.equals(mc.player.getUuid())) {
            return AchievementTracking.achievements;
        }

        String normalizedUuid = uuid == null ? null : BadgeProfile.normalizeUuid(uuid.toString());
        if (normalizedUuid == null) return null;

        Achievements achievements = achievementsByUuid.get(normalizedUuid);
        if (achievements == null) fetchAchievements(normalizedUuid);
        return achievements;
    }

    private static AchievementLoadStatus achievementLoadStatus(UUID uuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (uuid != null && mc.player != null && uuid.equals(mc.player.getUuid())) return AchievementLoadStatus.LOADING;

        String normalizedUuid = uuid == null ? null : BadgeProfile.normalizeUuid(uuid.toString());
        if (normalizedUuid == null) return AchievementLoadStatus.FAILED;
        return achievementLoadStatusByUuid.getOrDefault(normalizedUuid, AchievementLoadStatus.LOADING);
    }

    private static void fetchAchievements(String playerUuid) {
        long now = System.currentTimeMillis();
        if (achievementsByUuid.containsKey(playerUuid) || now < achievementRetryAtByUuid.getOrDefault(playerUuid, 0L)) return;

        achievementRetryAtByUuid.put(playerUuid, now + ACHIEVEMENT_RETRY_DELAY_MS);
        achievementLoadStatusByUuid.put(playerUuid, AchievementLoadStatus.LOADING);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ACHIEVEMENTS_URL + playerUuid))
                .GET()
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        Achievements achievements = parseAchievements(response.body());
                        if (achievements != null) {
                            achievementsByUuid.put(playerUuid, achievements);
                            achievementLoadStatusByUuid.remove(playerUuid);
                            return;
                        }
                    }

                    achievementLoadStatusByUuid.put(playerUuid,
                            response.statusCode() == 404 ? AchievementLoadStatus.NOT_FOUND : AchievementLoadStatus.FAILED);
                    if (response.statusCode() != 404) {
                        BackendErrorLogger.error("achievement-fetch", "Achievement fetch failed: " + response.statusCode());
                    }
                })
                .exceptionally(ex -> {
                    achievementLoadStatusByUuid.put(playerUuid, AchievementLoadStatus.FAILED);
                    BackendErrorLogger.error("achievement-fetch", "Failed to fetch achievements: " + ex.getMessage());
                    return null;
                });
    }

    private static Achievements parseAchievements(String responseBody) {
        try {
            JsonObject response = GSON.fromJson(responseBody, JsonObject.class);
            if (response == null || !response.has("achievements") || !response.get("achievements").isJsonArray()) return null;

            Achievements achievements = Achievements.createDefaultAchievementSet();
            for (JsonElement element : response.getAsJsonArray("achievements")) {
                if (!element.isJsonObject()) continue;

                JsonObject state = element.getAsJsonObject();
                String id = jsonString(state, "id");
                if (id == null) continue;

                Integer current = state.has("current") && state.get("current").isJsonPrimitive()
                        ? state.get("current").getAsInt() : null;
                boolean unlocked = state.has("unlocked") && state.get("unlocked").getAsBoolean();
                achievements.applyRemoteState(id, unlocked, current);
            }
            return achievements;
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Error parsing achievement response", e);
            return null;
        }
    }

    private enum AchievementLoadStatus {
        LOADING,
        NOT_FOUND,
        FAILED
    }

    private static String tierMilestones(TieredAchievement achievement) {
        List<Integer> targets = achievement.getLevelTargets();
        if (targets.isEmpty()) return "";

        StringBuilder tiers = new StringBuilder();
        for (int i = 0; i < targets.size(); i++) {
            if (i > 0) tiers.append("/");
            tiers.append(targets.get(i));
        }
        return tiers.toString();
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
        handleTick();
    }

    @SubscribeEvent
    public void onWorldChange(WorldChangeEvent event) {
        syncWithServerSoon();
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

        getActiveUsers();
        getActiveUserDetails();

        MojangAuth.getWEToken().thenAccept(wynnextrasToken -> {
            if (wynnextrasToken != null) sendHeartbeat(wynnextrasToken);
        }).exceptionally(e -> {
            BackendErrorLogger.error("badge-auth", "Error getting auth data for badge sync: " + e.getMessage());
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
                } else {
                    BackendErrorLogger.error("badge-details", "Badge details fetching failed: " + response.statusCode());
                }
            } catch (Exception e) {
                BackendErrorLogger.error("badge-details", "Badge details fetching error: " + e.getMessage());
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
                    BackendErrorLogger.error("badge-active-users", "Badge fetching failed: " + response.statusCode());
                }
            } catch (Exception e) {
                BackendErrorLogger.error("badge-active-users", "Badge fetching error: " + e.getMessage());
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
                        BackendErrorLogger.error("badge-heartbeat", "Badge heartbeat failed: " + response.statusCode());
                    }
                });
            } catch (Exception e) {
                BackendErrorLogger.error("badge-heartbeat", "Badge heartbeat error: " + e.getMessage());
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
        Entity entity = state instanceof EntityRenderStateExtension extension ? extension.getEntity() : null;
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
