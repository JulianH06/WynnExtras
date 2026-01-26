package julianh06.wynnextras.features.profileviewer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Time;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.CurrentVersionData;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.features.guildviewer.data.GuildData;
import julianh06.wynnextras.features.profileviewer.data.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@WEModule
public class WynncraftApiHandler {
    public static WynncraftApiHandler INSTANCE = new WynncraftApiHandler();

    // Reuse HttpClient instance instead of creating new ones
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    // Use synchronized list to prevent concurrent modification
    public List<ApiAspect> aspectList = java.util.Collections.synchronizedList(new ArrayList<>());
    public boolean[] waitingForAspectResponse = new boolean[5];
    // Lock object for synchronizing array access
    private final Object aspectLock = new Object();

    private static Command apiKeyCmd = new Command(
            "apikey",
            "",
            context -> {
                String arg = StringArgumentType.getString(context, "key");
                INSTANCE.API_KEY = arg;
                save();
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("You have successfully set your api key." +
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
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("""
                        Add an API key like this: "/WynnExtras apikey <your key>". \
                        If you play on multiple accounts you can either:
                           1. Add your alt(s) to your existing Wynncraft account so they can share the same API key
                           2. Create a separate Wynncraft account for each Minecraft account, and generate an API key for each
                        You can find a tutorial on how to get your api key in #infos on our discord. \
                        Run "/WynnExtras discord" to join.""")));
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
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                        return json.get("id").getAsString(); // UUID ohne Bindestriche
                    } catch (Exception e) {
                        return null; // Spieler existiert nicht
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
        HttpRequest request;

        if (INSTANCE.API_KEY == null) {
//            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("§4You currently don't have an api key set, some stats may be hidden to you." +
//                    " Run \"/WynnExtras apikey\" to learn more.")));


            request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL_GUILD + "prefix/" + prefix + "?identifier=uuid"))
                    .GET()
                    .build();
        } else {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL_GUILD + "prefix/" + prefix + "?identifier=uuid"))
                    .header("Authorization", "Bearer " + INSTANCE.API_KEY)
                    .GET()
                    .build();
        }

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(WynncraftApiHandler::parseGuildData);
    }

    public static CompletableFuture<List<ApiAspect>> fetchAspectList(String className) {
        HttpRequest request;

        if(INSTANCE.API_KEY == null) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("You need to set your api-key to upload your aspects. For more info run \"/WynnExtras apikey\""));
            return CompletableFuture.completedFuture(null);
        } else {
            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.wynncraft.com/v3/aspects/" + className))
                    .header("Authorization", "Bearer " + INSTANCE.API_KEY)
                    .GET()
                    .build();
        }

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(WynncraftApiHandler::parseAspectData);
    }

    public static List<ApiAspect> fetchAllAspects() {
        List<String> classes = List.of("warrior", "shaman", "mage", "archer", "assassin");
        List<ApiAspect> aspectList = WynncraftApiHandler.INSTANCE.aspectList;

        if(!aspectList.isEmpty()) {
            return aspectList;
        }

        // Check API key before attempting fetch
        if (INSTANCE.API_KEY == null) {
            return aspectList; // Return empty list, message already shown by fetchAspectList
        }

        // Synchronize access to waitingForAspectResponse array
        synchronized (INSTANCE.aspectLock) {
            // Check if ALL classes are marked as waiting but list is still empty - means previous fetch failed
            boolean allWaiting = true;
            for (int j = 0; j < 5; j++) {
                if (!WynncraftApiHandler.INSTANCE.waitingForAspectResponse[j]) {
                    allWaiting = false;
                    break;
                }
            }
            if (allWaiting && aspectList.isEmpty()) {
                // Reset all flags - previous fetch must have failed
                for (int j = 0; j < 5; j++) {
                    WynncraftApiHandler.INSTANCE.waitingForAspectResponse[j] = false;
                }
            }

            int i = 0;
            for(String className : classes) {
                if(WynncraftApiHandler.INSTANCE.waitingForAspectResponse[i]) {
                    i++;
                    continue;
                }

                WynncraftApiHandler.INSTANCE.waitingForAspectResponse[i] = true;

                int finalI = i;
                CompletableFuture<List<ApiAspect>> future = WynncraftApiHandler.fetchAspectList(className);
                if (future != null) {
                    future.thenAccept(result -> {
                                if(result == null) return;
                                if(result.isEmpty()) return;

                                synchronized (INSTANCE.aspectLock) {
                                    WynncraftApiHandler.INSTANCE.waitingForAspectResponse[finalI] = false;
                                }
                                // Only add aspects that aren't already in the list (prevent duplicates)
                                // aspectList is already synchronized, but we need to check-then-add atomically
                                synchronized (aspectList) {
                                    for (ApiAspect aspect : result) {
                                        boolean alreadyExists = aspectList.stream()
                                            .anyMatch(existing -> existing.getName().equals(aspect.getName()));
                                        if (!alreadyExists) {
                                            aspectList.add(aspect);
                                        }
                                    }
                                }
                            })
                            .exceptionally(ex -> {
                                System.err.println("Unexpected error fetching aspects: " + ex.getMessage());
                                synchronized (INSTANCE.aspectLock) {
                                    WynncraftApiHandler.INSTANCE.waitingForAspectResponse[finalI] = false;
                                }
                                return null;
                            });
                }
                i++;
            }
        }

        return aspectList;
    }

    public static CompletableFuture<PlayerData> fetchPlayerData(String playerName) {
        return fetchUUID(playerName).thenCompose(rawUUID -> {
            if (rawUUID == null) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("§cPlayername is incorrect or unknown.")));
                return CompletableFuture.completedFuture(null);
            }

            String formattedUUID = formatUUID(rawUUID);
            HttpRequest request;

            if (INSTANCE.API_KEY == null) {
//                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("§4You currently don't have an api key set, some stats may be hidden to you." +
//                        " Run \"/WynnExtras apikey\" to learn more.")));


                request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + formattedUUID + "?fullResult"))
                        .GET()
                        .build();
            } else {
                request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + formattedUUID + "?fullResult"))
                        .header("Authorization", "Bearer " + INSTANCE.API_KEY)
                        .GET()
                        .build();
            }

            return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenApply(WynncraftApiHandler::parsePlayerData);
        });
    }

    public static CompletableFuture<FetchResult> fetchPlayerAspectData(String playerUUID, String requestingUUID) {
        if (playerUUID == null) {
            McUtils.sendMessageToClient(Text.of("§cUUID is null!"));
            return CompletableFuture.completedFuture(null);
        }

        if(INSTANCE.API_KEY == null) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("You need to set your api-key to upload your aspects. For more info run \"/we apikey\""));
            return CompletableFuture.completedFuture(new FetchResult(FetchStatus.NOKEYSET, null));
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://wynnextras.com/user"))
                    .header("playerUUID", playerUUID)
                    .header("Wynncraft-Api-Key", INSTANCE.API_KEY)
                    .header("RequestingUUID", requestingUUID)
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .handle((response, ex) -> {

                        if (ex != null) {
                            System.err.println("Server unreachable: " + ex.getMessage());
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
                            return new FetchResult(FetchStatus.UNKNOWN_ERROR, null);
                        }

                        if (code >= 500) {
                            return new FetchResult(FetchStatus.SERVER_ERROR, null);
                        }

                        if (code != 200) {
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
        if(INSTANCE.API_KEY == null) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("You need to set your api-key to upload your aspects. For more info run \"/WynnExtras apikey\""));
            return;
        }

        List<Aspect> aspects = new ArrayList<>();
        for(String entry : map.keySet()) {
            int amount = parseAspectAmount(map.get(entry));
            Aspect aspect = new Aspect();
            aspect.setAmount(amount);
            aspect.setName(entry);
            aspect.setRarity(map.get(entry).getRight());
            aspects.add(aspect);
        }

        if (McUtils.player() == null) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cCannot upload aspects - player not loaded"));
            return;
        }

        User user = new User();
        user.setUuid(McUtils.player().getUuidAsString());
        user.setPlayerName(McUtils.playerName());
        user.setAspects(aspects);
        user.setModVersion(CurrentVersionData.INSTANCE.version);
        user.setUpdatedAt(Time.now().timestamp());

        postPlayerAspectData(user).thenAccept(result -> {
            if (result.status() == FetchStatus.OK) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aSuccessfully uploaded your aspects!"));
            } else if (result.status() == FetchStatus.NOKEYSET) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§You need to set your api-key to use this feature. Run \"/we apikey\" for more information."));
            } else if (result.status() == FetchStatus.FORBIDDEN) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§4You need to upload your own aspects first to view other peoples aspects. Run \"/we apikey\" for more information."));
            } else if (result.status() == FetchStatus.UNAUTHORIZED) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§4The api-key you have set is not connected to your minecraft account. Run \"/we apikey\" for more information."));
            } else {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§4Error: " + result.status()));
            }
        });
    }

    public static CompletableFuture<FetchResult> postPlayerAspectData(User user) {
        if (user == null) {
            McUtils.sendMessageToClient(Text.of("§cUser object is null!"));
            return CompletableFuture.completedFuture(new FetchResult(FetchStatus.UNKNOWN_ERROR, null));
        }

        try {
            String json = gson.toJson(user);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://wynnextras.com/user"))
                    .header("Content-Type", "application/json")
                    .header("Wynncraft-Api-Key", INSTANCE.API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(8))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .handle((response, ex) -> {

                        if (ex != null) {
                            System.err.println("Server unreachable: " + ex.getMessage());
                            return new FetchResult(FetchStatus.SERVER_UNREACHABLE, null);
                        }

                        int code = response.statusCode();

                        if (code == 401) {
                            return new FetchResult(FetchStatus.UNAUTHORIZED, null);
                        }

                        if (code == 400) {
                            return new FetchResult(FetchStatus.UNKNOWN_ERROR, null);
                        }

                        if (code >= 500) {
                            return new FetchResult(FetchStatus.SERVER_ERROR, null);
                        }

                        if (code != 200) {
                            System.err.println("POST ERROR: " + code + " → " + response.body());
                            return new FetchResult(FetchStatus.UNKNOWN_ERROR, null);
                        }

                        User savedUser = parsePlayerAspectData(response.body());
                        return new FetchResult(FetchStatus.OK, savedUser);
                    });

        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(new FetchResult(FetchStatus.UNKNOWN_ERROR, null));
        }
    }


    private static int parseAspectAmount(Pair<String, String> aspect) {
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




    public static CompletableFuture<AbilityMapData> fetchPlayerAbilityMap(String playerUUID, String characterUUUID) {
        HttpRequest request;

        if (INSTANCE.API_KEY == null) {
//            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("§4You currently don't have an api key set, some stats may be hidden to you." +
//                    " Run \"/WynnExtras apikey\" to learn more.")));


            request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + playerUUID + "/characters/" + characterUUUID + "/abilities"))
                    .GET()
                    .build();
        } else {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + playerUUID + "/characters/" + characterUUUID + "/abilities"))
                    .header("Authorization", "Bearer " + INSTANCE.API_KEY)
                    .GET()
                    .build();
        }

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
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

        Type mapType = new TypeToken<Map<String, ApiAspect>>() {}.getType();
        Map<String, ApiAspect> aspectMap = gson.fromJson(json, mapType);

        return new ArrayList<>(aspectMap.values());
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
        if (McUtils.player() == null) {
            System.err.println("[WynnExtras] Cannot load API key - player not loaded");
            return;
        }

        Path CONFIG_PATH = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("wynnextras/" + McUtils.player().getUuid().toString() + "/apikeyDoNotShare.json");
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                WynncraftApiHandler loaded = gson.fromJson(reader, WynncraftApiHandler.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                } else {
                    System.err.println("[WynnExtras] Deserialized data was null, keeping default INSTANCE.");
                }
            } catch (IOException e) {
                System.err.println("[WynnExtras] Couldn't read the apikey file:");
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        if (McUtils.player() == null) {
            System.err.println("[WynnExtras] Cannot save API key - player not loaded");
            return;
        }

        Path CONFIG_PATH = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("wynnextras/" + McUtils.player().getUuid().toString() + "/apikeyDoNotShare.json");
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            gson.toJson(INSTANCE, writer);
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't write the apikey file:");
            e.printStackTrace();
        }
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
