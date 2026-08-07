package julianh06.wynnextras.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.CurrentVersionData;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.features.aspects.LocalAspectStorage;
import julianh06.wynnextras.features.guildviewer.data.GuildData;
import julianh06.wynnextras.features.profileviewer.data.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@WEModule
public class WynncraftApiHandler {
    public static WynncraftApiHandler INSTANCE = new WynncraftApiHandler();

    public transient final AtomicInteger aspectFetchGeneration = new AtomicInteger(0);
    public transient final AtomicBoolean isFetchingAspects = new AtomicBoolean(false);
    private static final long ASPECT_FETCH_RETRY_DELAY_MS = 60_000;

    // Reuse HttpClient instance instead of creating new ones
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // Use synchronized list to prevent concurrent modification
    public transient List<ApiAspect> aspectList = java.util.Collections.synchronizedList(new ArrayList<>());
    public transient boolean[] waitingForAspectResponse = new boolean[5];
    private transient long lastAspectFetchFailureMillis = 0;
    // Lock object for synchronizing array access
    public transient final Object aspectLock = new Object();

    private static Map<String, JsonObject> cachedItemDatabase;
    private static CompletableFuture<List<ApiLootPool>> officialLootPoolsFuture;
    private static List<ApiLootPool> officialLootPoolsCache;

    public static Map<String, JsonObject> getCachedItemDatabase() {
        return cachedItemDatabase;
    }

    public static void setCachedItemDatabase(Map<String, JsonObject> itemDatabase) {
        cachedItemDatabase = itemDatabase;
    }

    public static class ApiLootPool {
        public String name;
        public String internalName;
        public String type;
        public List<ApiLootPoolReward> rewards = new ArrayList<>();
    }

    public static class ApiLootPoolReward {
        public String name;
        public String type;
        public int amount;
        public boolean always;
        public String tier;
        public boolean shiny;
        public String tooltip;
    }

    public static synchronized CompletableFuture<List<ApiLootPool>> fetchOfficialLootPools(boolean forceRefresh) {
        if (!forceRefresh && officialLootPoolsCache != null) {
            return CompletableFuture.completedFuture(officialLootPoolsCache);
        }

        if (!forceRefresh && officialLootPoolsFuture != null) {
            return officialLootPoolsFuture;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.wynncraft.com/v3/map/loot-pools"))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        officialLootPoolsFuture = HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        WynnExtras.LOGGER.error("Failed to fetch official loot pools: " + response.statusCode());
                        return null;
                    }

                    try {
                        JsonArray pools = JsonParser.parseString(response.body()).getAsJsonArray();
                        List<ApiLootPool> result = new ArrayList<>();

                        for (JsonElement poolElement : pools) {
                            JsonObject poolJson = poolElement.getAsJsonObject();
                            ApiLootPool pool = new ApiLootPool();
                            pool.name = jsonString(poolJson, "name");
                            pool.internalName = jsonString(poolJson, "internalName");
                            pool.type = jsonString(poolJson, "type");

                            JsonArray rewards = poolJson.getAsJsonArray("rewards");
                            if (rewards != null) {
                                for (JsonElement rewardElement : rewards) {
                                    JsonObject rewardJson = rewardElement.getAsJsonObject();
                                    ApiLootPoolReward reward = new ApiLootPoolReward();
                                    reward.name = jsonString(rewardJson, "name");
                                    reward.type = jsonString(rewardJson, "type");
                                    reward.amount = rewardJson.has("amount") && !rewardJson.get("amount").isJsonNull()
                                            ? rewardJson.get("amount").getAsInt() : 1;
                                    reward.always = rewardJson.has("always") && !rewardJson.get("always").isJsonNull()
                                            && rewardJson.get("always").getAsBoolean();
                                    reward.tier = rewardJson.has("tier") && !rewardJson.get("tier").isJsonNull()
                                            ? rewardJson.get("tier").getAsString() : "";
                                    reward.shiny = rewardJson.has("shiny") && !rewardJson.get("shiny").isJsonNull()
                                            && rewardJson.get("shiny").getAsBoolean();
                                    reward.tooltip = jsonTextBlock(rewardJson, "lore");
                                    if (reward.tooltip.isEmpty()) {
                                        reward.tooltip = jsonTextBlock(rewardJson, "tooltip");
                                    }
                                    pool.rewards.add(reward);
                                }
                            }

                            result.add(pool);
                        }

                        officialLootPoolsCache = result;
                        return result;
                    } catch (Exception e) {
                        WynnExtras.LOGGER.error("Error parsing official loot pools: " + e.getMessage());
                        return null;
                    }
                })
                .exceptionally(ex -> {
                    WynnExtras.LOGGER.error("Failed to fetch official loot pools: " + ex.getMessage());
                    return null;
                });

        return officialLootPoolsFuture;
    }

    public static synchronized void clearOfficialLootPoolsCache() {
        officialLootPoolsCache = null;
        officialLootPoolsFuture = null;
    }

    private static String jsonString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }

    private static String jsonTextBlock(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) return "";

        JsonElement value = json.get(key);
        if (value.isJsonArray()) {
            List<String> lines = new ArrayList<>();
            for (JsonElement line : value.getAsJsonArray()) {
                if (!line.isJsonNull()) lines.add(line.getAsString());
            }
            return String.join("\n", lines);
        }

        return value.getAsString();
    }

    private static Command apiKeyCmd = new Command(
            "apikey",
            "",
            context -> {
                String key = StringArgumentType.getString(context, "key");
                if(key.equals("clear")) {
                    WynncraftAuthManager.clearApiKey();
                    MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("You have successfully cleared your api key.")));
                    return 1;
                }

                WynncraftAuthManager.setApiKey(key);
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("You have successfully set your api key." +
                        " It has been saved in your config. Don't share it publicly.")));
                return 1;
            },
            null,
            List.of(ClientCommandManager.argument("key", StringArgumentType.word()))
    );

    private static Command apiKeyCmdNoArgs = new Command(
            "apikey",
            "",
            context -> {
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("""
                        You can use either OAuth or an api key to authorize. OAuth is easier to setup but an api key
                        might give you more access (like for the semi-private stats of friends or guild members)
                        You can add an API key like this: "/WynnExtras apikey <your key>".
                        If you use API keys on multiple accounts you can either:
                           1. Add your alt(s) to your existing Wynncraft account so they can share the same API key
                           2. Create a separate Wynncraft account for each Minecraft account, and generate an API key for each
                        You can find a tutorial on how to get your api key in #infos on our discord. \
                        Run "/WynnExtras discord" to join. Run "/WynnExtras apikey clear" to clear your api key.""")));
                return 1;
            },
            null,
            null
    );

    private static final String BASE_URL = "https://api.wynncraft.com/v3/player/";
    private static final String BASE_URL_GUILD = "https://api.wynncraft.com/v3/guild/";

    public String API_KEY;

    public static CompletableFuture<String> fetchUUID(String playerName) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + playerName))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int status = response.statusCode();
                    if (status == 404) return null;
                    if (status == 429) {
                        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                Text.of("§cMojang API rate limit reached. Please wait a moment.")));
                        return null;
                    }
                    if (status != 200) {
                        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                Text.of("§cMojang API error (" + status + "). Please try again.")));
                        return null;
                    }
                    try {
                        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                        return json.get("id").getAsString();
                    } catch (Exception e) {
                        return null;
                    }
                });
    }

    public static String formatUUID(String rawUUID) {
        return rawUUID.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
        );
    }

    public static CompletableFuture<GuildData> fetchGuildData(String prefix) {
        HttpRequest request = WynncraftAuthManager.applyWynncraftAuth(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL_GUILD + "prefix/" + prefix + "?identifier=uuid"))
                .GET())
                .build();

        return WynncraftAuthManager.sendWynncraftRequest(request)
                .thenApply(HttpResponse::body)
                .thenApply(WynncraftApiHandler::parseGuildData);
    }

    public static CompletableFuture<List<ApiAspect>> fetchAspectList(String className) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.wynncraft.com/v3/aspects/" + className))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    WynnExtras.LOGGER.error("Aspect API returned status " + response.statusCode() + " for " + className);
                    return null;
                }
                String body = response.body();
                if (body == null || !body.trim().startsWith("[{")) {
                    WynnExtras.LOGGER.error("Invalid API response for " + className + ": " + body);
                    return null;
                }
                return body;
            })
            .thenApply(body -> {
                if (body == null) return null;
                return parseAspectData(body);
            });
    }

    public static synchronized List<ApiAspect> fetchAllAspects() {
        List<String> classes = List.of("warrior", "shaman", "mage", "archer", "assassin");
        List<ApiAspect> aspectList = WynncraftApiHandler.INSTANCE.aspectList;

        if (hasAllAspectClasses(aspectList, classes)) return aspectList;
        if (INSTANCE.isFetchingAspects.get()) return aspectList;

        long now = System.currentTimeMillis();
        if (INSTANCE.lastAspectFetchFailureMillis > 0
                && now - INSTANCE.lastAspectFetchFailureMillis < ASPECT_FETCH_RETRY_DELAY_MS) {
            return aspectList;
        }

        if (!INSTANCE.isFetchingAspects.compareAndSet(false, true)) {
            return aspectList;
        }

        final int generation = INSTANCE.aspectFetchGeneration.get();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        int i = 0;
        for (String className : classes) {
            CompletableFuture<Void> future = WynncraftApiHandler.fetchAspectList(className)
                    .thenAccept(result -> {
                        if (INSTANCE.aspectFetchGeneration.get() != generation) return;
                        if (result == null || result.isEmpty()) return;

                        synchronized (aspectList) {
                            for (ApiAspect aspect : result) {
                                boolean alreadyExists = aspectList.stream()
                                        .anyMatch(existing -> Objects.equals(existing.getName(), aspect.getName()));
                                if (!alreadyExists) aspectList.add(aspect);
                            }
                        }
                    })
                    .exceptionally(ex -> {
                        WynnExtras.LOGGER.error("Error fetching aspects for " + className + ": " + ex.getMessage());
                        return null;
                    });

            futures.add(future);
            i++;
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, ex) -> {
                    synchronized (WynncraftApiHandler.class) {
                        if (!hasAllAspectClasses(aspectList, classes)) {
                            INSTANCE.lastAspectFetchFailureMillis = System.currentTimeMillis();
                        } else {
                            INSTANCE.lastAspectFetchFailureMillis = 0;
                        }

                        INSTANCE.isFetchingAspects.set(false);
                    }
                });

        return aspectList;
    }

    private static boolean hasAllAspectClasses(List<ApiAspect> aspectList, List<String> classes) {
        if (aspectList == null || aspectList.isEmpty()) return false;

        Set<String> loadedClasses = aspectList.stream()
                .map(ApiAspect::getRequiredClass)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        return loadedClasses.containsAll(classes);
    }

    /** Default: silent on lookup failure. Auto-fetchers (GV/ProfessionOverlay/CharacterModelMixin etc.)
     *  fire constantly with possibly-missing names — they shouldn't spam chat. Explicit user
     *  commands like /pv should call {@link #fetchPlayerData(String, boolean)} with verbose=true. */
    public static CompletableFuture<PlayerData> fetchPlayerData(String playerName) {
        return fetchPlayerData(playerName, false);
    }

    public static CompletableFuture<PlayerData> fetchPlayerData(String playerName, boolean verbose) {
        return fetchUUID(playerName).thenCompose(rawUUID -> {
            if (rawUUID == null) {
                if (verbose) {
                    MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("§cPlayername is incorrect or unknown.")));
                }
                return CompletableFuture.completedFuture(null);
            }

            String formattedUUID = formatUUID(rawUUID);
            if (verbose) {
                logPlayerFetchDebug("Starting fetch for " + playerName + " (" + formattedUUID + "), auth=" + playerFetchAuthSummary());
            }

            if (WynncraftAuthManager.hasApiKey()) {
                return fetchPlayerDataWithApiKey(formattedUUID, verbose)
                        .thenCompose(apiKeyResult -> {
                            if (!WynncraftAuthManager.hasOAuthToken()) {
                                if (verbose) {
                                    logPlayerFetchDebug("Skipping OAuth comparison, auth=" + playerFetchAuthSummary());
                                }
                                return CompletableFuture.completedFuture(apiKeyResult.playerData());
                            }

                            return fetchPlayerDataWithCurrentAuth(formattedUUID, verbose)
                                    .thenApply(currentResult -> selectBestPlayerData(currentResult, apiKeyResult, verbose));
                        });
            }

            return fetchPlayerDataWithCurrentAuth(formattedUUID, verbose)
                    .thenApply(PlayerDataFetchResult::playerData);
        });
    }

    private static CompletableFuture<PlayerDataFetchResult> fetchPlayerDataWithCurrentAuth(String formattedUUID, boolean verbose) {
        HttpRequest request = WynncraftAuthManager.applyWynncraftAuth(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + formattedUUID + "?fullResult"))
                .GET())
                .build();

        return WynncraftAuthManager.sendWynncraftRequest(request)
                .thenApply(response -> parsePlayerFetchResponse("current auth", response, verbose));
    }

    private static CompletableFuture<PlayerDataFetchResult> fetchPlayerDataWithApiKey(String formattedUUID, boolean verbose) {
        HttpRequest request = WynncraftAuthManager.applyWynncraftApiKeyAuth(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + formattedUUID + "?fullResult"))
                .GET())
                .build();

        return WynncraftAuthManager.httpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parsePlayerFetchResponse("API key", response, verbose))
                .exceptionally(ex -> {
                    if (verbose) {
                        logPlayerFetchDebug("API key request failed: " + ex.getMessage());
                    }
                    return new PlayerDataFetchResult(-1, null);
                });
    }

    private static PlayerData selectBestPlayerData(PlayerDataFetchResult currentResult, PlayerDataFetchResult apiKeyResult, boolean verbose) {
        PlayerData currentData = currentResult.playerData();
        PlayerData apiKeyData = apiKeyResult.playerData();
        boolean useApiKeyResult = hasMorePrivateData(apiKeyData, currentData);
        if (verbose) {
            logPlayerFetchDebug("Selected " + (useApiKeyResult ? "API key" : "OAuth/current auth")
                    + " result, currentScore=" + privateDataScore(currentData)
                    + ", apiKeyScore=" + privateDataScore(apiKeyData));
        }
        return useApiKeyResult ? apiKeyData : currentData;
    }

    private static PlayerDataFetchResult parsePlayerFetchResponse(String authName, HttpResponse<String> response, boolean verbose) {
        PlayerData playerData = null;
        if (response.statusCode() == 200) {
            playerData = parsePlayerData(response.body());
        }

        if (verbose) {
            logPlayerFetchDebug(authName + " response status=" + response.statusCode()
                    + ", score=" + privateDataScore(playerData)
                    + ", data=" + privateDataSummary(playerData)
                    + (response.statusCode() == 200 ? "" : ", body=" + debugBodySnippet(response.body())));
        }

        return new PlayerDataFetchResult(response.statusCode(), playerData);
    }

    private static boolean hasMorePrivateData(PlayerData candidate, PlayerData current) {
        return privateDataScore(candidate) > privateDataScore(current);
    }

    private static int privateDataScore(PlayerData playerData) {
        if (playerData == null) return 0;

        int score = 1;
        if (playerData.getRanking() != null && !playerData.getRanking().isEmpty()) score += 5;

        Global globalData = playerData.getGlobalData();
        if (globalData != null) {
            score += 5;
            if (globalData.getDungeons() != null) score += 3;
            if (globalData.getRaids() != null) score += 3;
            if (globalData.getGuildRaids() != null) score += 2;
            if (globalData.getPvp() != null) score += 1;
        }

        Map<String, CharacterData> characters = playerData.getCharacters();
        if (characters != null && !characters.isEmpty()) {
            score += 5 + characters.size();
            for (CharacterData character : characters.values()) {
                if (character.getSkillPoints() != null) score += 3;
                if (character.getProfessions() != null && !character.getProfessions().isEmpty()) score += 3;
                if (character.getDungeons() != null) score += 2;
                if (character.getRaids() != null) score += 2;
                if (character.getQuests() != null && !character.getQuests().isEmpty()) score += 2;
            }
        }

        return score;
    }

    private static String privateDataSummary(PlayerData playerData) {
        if (playerData == null) return "null";

        Global globalData = playerData.getGlobalData();
        Map<String, CharacterData> characters = playerData.getCharacters();

        int characterCount = characters == null ? 0 : characters.size();
        int skillPointCount = 0;
        int professionCount = 0;
        int dungeonCount = 0;
        int raidCount = 0;
        int questCount = 0;

        if (characters != null) {
            for (CharacterData character : characters.values()) {
                if (character.getSkillPoints() != null) skillPointCount++;
                if (character.getProfessions() != null && !character.getProfessions().isEmpty()) professionCount++;
                if (character.getDungeons() != null) dungeonCount++;
                if (character.getRaids() != null) raidCount++;
                if (character.getQuests() != null && !character.getQuests().isEmpty()) questCount++;
            }
        }

        return "username=" + playerData.getUsername()
                + ", global=" + (globalData != null)
                + ", globalDungeons=" + (globalData != null && globalData.getDungeons() != null)
                + ", globalRaids=" + (globalData != null && globalData.getRaids() != null)
                + ", globalGuildRaids=" + (globalData != null && globalData.getGuildRaids() != null)
                + ", ranking=" + (playerData.getRanking() == null ? "null" : playerData.getRanking().size())
                + ", characters=" + (characters == null ? "null" : characterCount)
                + ", charsWithSkillPoints=" + skillPointCount
                + ", charsWithProfessions=" + professionCount
                + ", charsWithDungeons=" + dungeonCount
                + ", charsWithRaids=" + raidCount
                + ", charsWithQuests=" + questCount;
    }

    private static String playerFetchAuthSummary() {
        return "oauth=" + WynncraftAuthManager.hasOAuthToken() + ", apiKey=" + WynncraftAuthManager.hasApiKey();
    }

    private static void logPlayerFetchDebug(String message) {
        WynnExtras.LOGGER.info("[PV Fetch Debug] " + message);
    }

    private static String debugBodySnippet(String body) {
        if (body == null) return "null";
        String normalized = body.replace('\n', ' ').replace('\r', ' ');
        return normalized.substring(0, Math.min(200, normalized.length()));
    }

    private record PlayerDataFetchResult(int statusCode, PlayerData playerData) {}

    public static CompletableFuture<FetchResult> fetchPlayerAspectData(String playerUUID) {
        if (playerUUID == null) {
            MinecraftUtils.sendMessageToClient(Text.of("§cUUID is null!"));
            return CompletableFuture.completedFuture(null);
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://wynnextras.com/aspects?playerUuid=" + playerUUID))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .handle((response, ex) -> {

                        if (ex != null) {
                            WynnExtras.LOGGER.error("Server unreachable: " + ex.getMessage());
                            return new FetchResult(FetchStatus.SERVER_UNREACHABLE, null);
                        }

                        int code = response.statusCode();

                        if (code == 403) {
                            return new FetchResult(FetchStatus.FORBIDDEN, null);
                        }

                        if (code == 401) {
                            return new FetchResult(FetchStatus.UNAUTHORIZED, null);
                        }

                        if (code == 400) {
                            WynnExtras.LOGGER.error("GET ERROR 400: " + response.body());
                            return new FetchResult(FetchStatus.UNKNOWN_ERROR, null);
                        }

                        if (code >= 500) {
                            WynnExtras.LOGGER.error("GET SERVER ERROR: " + code + " → " + response.body());
                            return new FetchResult(FetchStatus.SERVER_ERROR, null);
                        }

                        if (code != 200) {
                            WynnExtras.LOGGER.error("GET ERROR: " + code + " → " + response.body());
                            return new FetchResult(FetchStatus.UNKNOWN_ERROR, null);
                        }

                        User user = parsePlayerAspectData(response.body());
                        return new FetchResult(FetchStatus.OK, user);
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(new FetchResult(FetchStatus.UNKNOWN_ERROR, null));
        }
    }

    public static void processAspects(Map<String, Pair<String, String>> map) {
        if (MinecraftUtils.player() == null) {
            WynnExtras.LOGGER.error("Cannot upload aspects - player not loaded");
            return;
        }

        LocalAspectStorage.save(map);


        if(WynnExtras.isOnBeta()) {
            return;
        }


        // Authenticate with Mojang first
        MojangAuth.getWEToken().thenAccept(wynnextrasToken -> {
            if (wynnextrasToken == null) {
                WynnExtras.LOGGER.error("Failed to authenticate with Mojang for aspect upload");
                // Don't show duplicate error - MojangAuth already showed the error
                return;
            }

            try {
                // Build JSON payload
                JsonObject payload = new JsonObject();
                payload.addProperty("modVersion", CurrentVersionData.INSTANCE.version);

                JsonArray aspectsArray = new JsonArray();
                int processedCount = 0;
                int skippedCount = 0;
                for (String entry : map.keySet()) {
                    try {
                        Pair<String, String> aspectData = map.get(entry);
                        if (aspectData == null) {
                            WynnExtras.LOGGER.error("DEBUG: Null aspect data for: " + entry);
                            skippedCount++;
                            continue;
                        }

                        int amount = parseAspectAmount(aspectData);
                        JsonObject aspectJson = new JsonObject();
                        aspectJson.addProperty("name", entry);
                        aspectJson.addProperty("rarity", aspectData.getRight());
                        aspectJson.addProperty("amount", amount);
                        aspectsArray.add(aspectJson);
                        processedCount++;
                    } catch (Exception e) {
                        WynnExtras.LOGGER.error("DEBUG: Error processing aspect " + entry + ": " + e.getMessage());
                        e.printStackTrace();
                        skippedCount++;
                    }
                }
                payload.add("aspects", aspectsArray);

                WynnExtras.LOGGER.info("DEBUG: Loop stats - Processed: " + processedCount + ", Skipped: " + skippedCount + ", Total map size: " + map.size());

                WynnExtras.LOGGER.info("DEBUG: Built payload with " + aspectsArray.size() + " aspects");
                String payloadString = payload.toString();
                WynnExtras.LOGGER.info("DEBUG: Payload size: " + payloadString.length() + " characters");
                WynnExtras.LOGGER.info("DEBUG: First 500 chars of payload: " + payloadString.substring(0, Math.min(500, payloadString.length())));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://wynnextras.com/aspects"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", wynnextrasToken)
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .timeout(Duration.ofSeconds(8))
                        .build();

                ApiRequestHelper.sendWithAuthRetry(request, payload)
                        .thenAccept(response -> {
                            int code = response.statusCode();
                            if (code == 401) {
                                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cAuthentication failed"));
                                WynnExtras.LOGGER.error("Personal aspects upload auth error: " + response.body());
                            } else if (code >= 500) {
                                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cServer error - try again later"));
                                WynnExtras.LOGGER.error("Personal aspects upload error: " + code + " → " + response.body());
                            } else if(code != 200) {
                                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUpload failed (error " + code + ")"));
                                WynnExtras.LOGGER.error("Personal aspects upload error: " + code + " → " + response.body());
                            }
                        })
                        .exceptionally(ex -> {
                            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUpload failed - check your connection"));
                            WynnExtras.LOGGER.error("Failed to upload personal aspects: " + ex.getMessage());
                            return null;
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static int parseAspectAmount(Pair<String, String> aspect) {
        String tierText = aspect.getLeft();
        String rarity = aspect.getRight();

        int[] tier1 = {1, 1, 1};
        int[] tier2 = {4, 14, 4};
        int[] tier3 = {10, 60, 25};
        int[] tier4 = {0, 0, 120};

        int rarityIndex;
        switch (rarity) {
            case "Mythic" -> rarityIndex = 0;
            case "Fabled" -> rarityIndex = 1;
            case "Legendary" -> rarityIndex = 2;
            default -> rarityIndex = -1;
        }
        if (rarityIndex == -1) return 0;

        if (tierText.contains("[MAX]")) {
            return tier1[rarityIndex] + tier2[rarityIndex] + tier3[rarityIndex] + tier4[rarityIndex];
        }

        String currentTier = "";
        if (tierText.contains("Tier I ")) currentTier = "Tier I";
        else if (tierText.contains("Tier II ")) currentTier = "Tier II";
        else if (tierText.contains("Tier III ")) currentTier = "Tier III";

        int sum = 0;

        switch (currentTier) {
            case "Tier I" -> sum += tier1[rarityIndex];
            case "Tier II" -> sum += tier1[rarityIndex] + tier2[rarityIndex];
            case "Tier III" -> sum += tier1[rarityIndex] + tier2[rarityIndex] + tier3[rarityIndex];
        }

        if (tierText.matches(".*\\[(\\d+)/(\\d+)\\].*")) {
            String bracketContent = tierText.replaceAll(".*\\[(\\d+)/(\\d+)\\].*", "$1");
            int currentProgress = Integer.parseInt(bracketContent);
            sum += currentProgress;
        }

        return sum;
    }

    /**
     * Upload gambits to crowdsourcing API
     * Uses Mojang sessionserver authentication - secure and automatic
     * @param gambits List of gambits with name and description
     */
    public static void uploadGambits(List<julianh06.wynnextras.features.aspects.GambitData.GambitEntry> gambits) {
        if (MinecraftUtils.player() == null) {
            WynnExtras.LOGGER.error("Cannot upload gambits - player not loaded");
            return;
        }

        if(WynnExtras.isOnBeta()) {
            return;
        }

        if(!WynnExtrasConfig.INSTANCE.crowdSourceGambits) return;

        // Authenticate with Mojang first
        julianh06.wynnextras.utils.MojangAuth.getWEToken().thenAccept(wynnextrasToken -> {
            if (wynnextrasToken == null) {
                WynnExtras.LOGGER.error("Failed to authenticate with Mojang");
                return;
            }

            try {
                // Build JSON payload
                JsonObject payload = new JsonObject();
                JsonArray gambitsArray = new JsonArray();

                for (julianh06.wynnextras.features.aspects.GambitData.GambitEntry gambit : gambits) {
                    JsonObject gambitJson = new JsonObject();
                    gambitJson.addProperty("name", gambit.name);
                    gambitJson.addProperty("description", gambit.description);
                    gambitsArray.add(gambitJson);
                }

                payload.add("gambits", gambitsArray);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://wynnextras.com/gambit"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", wynnextrasToken)
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .timeout(Duration.ofSeconds(8))
                        .build();

                ApiRequestHelper.sendWithAuthRetry(request, payload)
                        .thenAccept(response -> {
                            int code = response.statusCode();
                            if(code == 401) {
                                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cAuthentication failed"));
                            } else if(code != 200) {
                                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cError uploading gambits: " + code));
                            }
                        })
                        .exceptionally(ex -> {
                            WynnExtras.LOGGER.error("Failed to upload gambits: " + ex.getMessage());
                            return null;
                        });

            } catch (Exception e) {
                e.printStackTrace();
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cError preparing gambits upload"));
            }
        });
    }

    /**
     * Fetch leaderboard of players with most maxed aspects
     * @param limit Number of entries (default 15, max 100)
     * @return CompletableFuture with list of leaderboard entries
     */
    public static CompletableFuture<List<LeaderboardEntry>> fetchLeaderboard(int limit) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://wynnextras.com/aspects/leaderboard?limit=" + limit))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            WynnExtras.LOGGER.info("[WynnExtras] Fetching leaderboard from: http://wynnextras.com/aspects/leaderboard?limit=" + limit);

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        WynnExtras.LOGGER.info("[WynnExtras] Leaderboard response code: " + response.statusCode());
                        WynnExtras.LOGGER.info("[WynnExtras] Leaderboard response body: " + response.body().substring(0, Math.min(500, response.body().length())));

                        if (response.statusCode() != 200) {
                            WynnExtras.LOGGER.info("Failed to fetch leaderboard: " + response.statusCode());
                            return new ArrayList<LeaderboardEntry>();
                        }

                        try {
                            JsonArray json = JsonParser.parseString(response.body()).getAsJsonArray();
                            List<LeaderboardEntry> result = new ArrayList<>();

                            for (int i = 0; i < json.size(); i++) {
                                JsonObject entry = json.get(i).getAsJsonObject();
                                LeaderboardEntry player = gson.fromJson(entry, LeaderboardEntry.class);
                                result.add(player);
                                WynnExtras.LOGGER.info("[WynnExtras] Parsed leaderboard entry: " + player.getPlayerName() + " - " + player.getMaxAspectCount() + " maxed");
                            }

                            WynnExtras.LOGGER.info("[WynnExtras] Fetched " + result.size() + " leaderboard entries");
                            return result;
                        } catch (Exception e) {
                            WynnExtras.LOGGER.error("Error parsing leaderboard: " + e.getMessage());
                            e.printStackTrace();
                            return new ArrayList<LeaderboardEntry>();
                        }
                    })
                    .exceptionally(ex -> {
                        WynnExtras.LOGGER.error("Failed to fetch leaderboard: " + ex.getMessage());
                        ex.printStackTrace();
                        return new ArrayList<LeaderboardEntry>();
                    });

        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(new ArrayList<LeaderboardEntry>());
        }
    }

    /**
     * Fetch crowdsourced gambits from API
     * @return CompletableFuture with list of gambits or null if not available
     */
    public static CompletableFuture<List<julianh06.wynnextras.features.aspects.GambitData.GambitEntry>> fetchCrowdsourcedGambits() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://wynnextras.com/gambit"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            WynnExtras.LOGGER.info("No crowdsourced gambits: " + response.statusCode());
                            return null;
                        }

                        try {
                            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                            JsonArray gambits = json.getAsJsonArray("gambits");

                            List<julianh06.wynnextras.features.aspects.GambitData.GambitEntry> result = new ArrayList<>();
                            for (int i = 0; i < gambits.size(); i++) {
                                JsonObject gambit = gambits.get(i).getAsJsonObject();
                                String name = gambit.get("name").getAsString();
                                String description = gambit.get("description").getAsString();

                                result.add(new julianh06.wynnextras.features.aspects.GambitData.GambitEntry(name, description));
                            }

                            WynnExtras.LOGGER.info("Fetched " + result.size() + " gambits from crowdsourced data");
                            return result;
                        } catch (Exception e) {
                            WynnExtras.LOGGER.error("Error parsing crowdsourced gambits: " + e.getMessage());
                            return null;
                        }
                    })
                    .exceptionally(ex -> {
                        WynnExtras.LOGGER.error("Failed to fetch crowdsourced gambits: " + ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }

    public static CompletableFuture<Map<String, JsonObject>> fetchItemDatabase() {
        HttpRequest request = WynncraftAuthManager.applyWynncraftAuth(HttpRequest.newBuilder()
                .uri(URI.create("https://api.wynncraft.com/v3/item/database?fullResult"))
                .GET())
                .build();

        return WynncraftAuthManager.sendWynncraftRequest(request)
                .thenApply(HttpResponse::body)
                .thenApply(WynncraftApiHandler::parseItemDatabase);
    }

    private static Map<String, JsonObject> parseItemDatabase(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (root.isJsonArray()) {
            Map<String, JsonObject> result = new HashMap<>();
            for (JsonElement el : root.getAsJsonArray()) {
                JsonObject obj = el.getAsJsonObject();
                String name = obj.has("internalName") ? obj.get("internalName").getAsString()
                            : obj.has("displayName")  ? obj.get("displayName").getAsString()
                            : null;
                if (name != null) result.put(name, obj);
            }
            return result;
        }
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, JsonObject>>() {}.getType();
        return gson.fromJson(root, mapType);
    }

    public static CompletableFuture<AbilityMapData> fetchPlayerAbilityMap(String playerUUID, String characterUUUID) {
        HttpRequest request = WynncraftAuthManager.applyWynncraftAuth(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + playerUUID + "/characters/" + characterUUUID + "/abilities"))
                .GET())
                .build();

        return WynncraftAuthManager.sendWynncraftRequest(request)
                .thenApply(HttpResponse::body)
                .thenApply(WynncraftApiHandler::parseAbilityMapData);
    }

    public static CompletableFuture<AbilityMapData> fetchClassAbilityMap(String className) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.wynncraft.com/v3/ability/map/" + className))
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(WynncraftApiHandler::parseAbilityMapData);
    }

    public static CompletableFuture<AbilityTreeData> fetchClassAbilityTree(String className) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.wynncraft.com/v3/ability/tree/" + className))
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(WynncraftApiHandler::parseAbilityTreeData);
    }

    private static PlayerData parsePlayerData(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
                .create();

        return gson.fromJson(json, PlayerData.class);
    }

    private static GuildData parseGuildData(String json) {
        Gson gson = new Gson();

        return gson.fromJson(json, GuildData.class);
    }

    private static User parsePlayerAspectData(String json) {
        Gson gson = new Gson();

        return gson.fromJson(json, User.class);
    }

    private static List<ApiAspect> parseAspectData(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(ApiAspect.Icon.class, new ApiAspect.IconDeserializer())
                .create();

        Type mapType = new TypeToken<List<ApiAspect>>() {}.getType();
        List<ApiAspect> aspectList = gson.fromJson(json, mapType);

        return aspectList;
    }

    private static AbilityMapData parseAbilityMapData(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(AbilityMapData.class, new AbilityMapDataDeserializer())
                .registerTypeAdapter(AbilityMapData.Node.class, new NodeDeserializer())
                .registerTypeAdapter(AbilityMapData.Icon.class, new IconDeserializer())
                .create();

        return gson.fromJson(json, AbilityMapData.class);
    }

    private static AbilityTreeData parseAbilityTreeData(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(AbilityTreeData.class, new AbilityTreeDataDeserializer())
                .registerTypeAdapter(AbilityMapData.Node.class, new NodeDeserializer())
                .registerTypeAdapter(AbilityMapData.Icon.class, new IconDeserializer())
                .create();

        return gson.fromJson(json, AbilityTreeData.class);
    }

    static Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();




    public static void load() {
        WynncraftAuthManager.load();
    }

    public static void save() {
        WynncraftAuthManager.save();
    }

    public static List<Text> parseStyledHtml(List<String> htmlLines) {
        return htmlLines.stream()
                .map(line -> parseSpan(line, Style.EMPTY))
                .collect(Collectors.toList());
    }

    public static String sanitizeHtmlString(String s) {
        if (s == null) return "";
        return s.replaceAll("", "");
    }


    private static Text parseSpan(String html, Style inheritedStyle) {
        MutableText result = null;

        int index = 0;
        while (index < html.length()) {
            int spanStart = html.indexOf("<span", index);
            if (spanStart == -1) {
                String remaining = stripHtml(html.substring(index));
                if (!remaining.isEmpty()) {
                    MutableText piece = Text.literal(remaining).setStyle(inheritedStyle);
                    result = (result == null) ? piece : result.append(piece);
                }
                break;
            }

            String before = stripHtml(html.substring(index, spanStart));
            if (!before.isEmpty()) {
                MutableText piece = Text.literal(before).setStyle(inheritedStyle);
                result = (result == null) ? piece : result.append(piece);
            }

            int tagEnd = html.indexOf(">", spanStart);
            if (tagEnd == -1) break;
            String tag = html.substring(spanStart, tagEnd + 1);

            String style = "";
            Matcher styleMatcher = Pattern.compile("style\\s*=\\s*(['\"])(.*?)\\1").matcher(tag);
            if (styleMatcher.find()) style = styleMatcher.group(2);

            String classAttr = "";
            Matcher classMatcher = Pattern.compile("class\\s*=\\s*(['\"])(.*?)\\1").matcher(tag);
            if (classMatcher.find()) classAttr = classMatcher.group(2);

            int contentStart = tagEnd + 1;
            int spanEnd = findMatchingSpanEnd(html, contentStart);
            if (spanEnd == -1) break;

            String inner = html.substring(contentStart, spanEnd);
            Style newStyle = inheritedStyle.withItalic(false);

            Matcher colorMatch = Pattern.compile("color:\\s*#([0-9a-fA-F]{6})").matcher(style);
            if (colorMatch.find()) {
                int rgb = Integer.parseInt(colorMatch.group(1), 16);
                newStyle = newStyle.withColor(TextColor.fromRgb(rgb));
            }

            if (style.contains("font-weight:bold") || style.contains("font-weight:bolder")) {
                newStyle = newStyle.withBold(true);
            }

            if (classAttr != null && !classAttr.isEmpty()) {
                if (classAttr.contains("font-common")) {
                    newStyle = newStyle.withFont(new StyleSpriteSource.Font(Identifier.of("minecraft", "common")));
                } else if (classAttr.contains("font-ascii")) {
                    newStyle = newStyle.withFont(new StyleSpriteSource.Font(Identifier.of("minecraft", "default")));
                }
            }

            Text parsedInner = parseSpan(inner, newStyle);
            MutableText piece = parsedInner instanceof MutableText ? (MutableText) parsedInner : Text.literal(parsedInner.getString()).setStyle(newStyle);
            result = (result == null) ? piece : result.append(piece);

            index = spanEnd + "</span>".length();
        }

        return (result == null) ? Text.empty() : result;
    }

    public static String stripHtml(String input) {
        return input.replaceAll("<[^>]*>", "");
    }

    private static int findMatchingSpanEnd(String html, int contentStart) {
        int index = contentStart;
        int depth = 0;
        while (index < html.length()) {
            int nextOpen = html.indexOf("<span", index);
            int nextClose = html.indexOf("</span>", index);
            if (nextClose == -1) return -1; // kein schließendes Tag mehr

            if (nextOpen != -1 && nextOpen < nextClose) {
                depth++; // inneres <span> beginnt
                index = nextOpen + 5;
            } else {
                if (depth == 0) {
                    return nextClose; // passendes schließendes Tag für die aktuelle Ebene
                } else {
                    depth--; // schließt eine verschachtelte Ebene
                    index = nextClose + 7;
                }
            }
        }
        return -1;
    }

    public enum FetchStatus {
        OK,
        NOT_FOUND,
        FORBIDDEN,
        SERVER_UNREACHABLE,
        SERVER_ERROR,
        UNKNOWN_ERROR,
        UNAUTHORIZED,
        NOKEYSET
    }

    public record FetchResult(FetchStatus status, User user) {}
}
