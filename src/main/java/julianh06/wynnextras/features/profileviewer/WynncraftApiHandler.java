package julianh06.wynnextras.features.profileviewer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.features.profileviewer.data.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@WEModule
public class WynncraftApiHandler {
    public static WynncraftApiHandler INSTANCE = new WynncraftApiHandler();

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
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Add an API key like this: \"/WynnExtras apikey <your key>\". " +
                        "You can find a tutorial on how to get your api key in #infos on our discord. " +
                        "Run \"/WynnExtras discord\" to join.")));
                return 1;
            },
            null,
            null
    );

    private static final String BASE_URL = "https://api.wynncraft.com/v3/player/";

    public String API_KEY;

    public static CompletableFuture<String> fetchUUID(String playerName) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + playerName))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
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

    public static CompletableFuture<PlayerData> fetchPlayerData(String playerName) {
        return fetchUUID(playerName).thenCompose(rawUUID -> {
            if (rawUUID == null) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("§cPlayername is incorrect or unknown.")));
                return CompletableFuture.completedFuture(null);
            }

            String formattedUUID = formatUUID(rawUUID);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request;

            if (INSTANCE.API_KEY == null) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("§4You currently don't have an api key set, some stats may be hidden to you." +
                        " Run \"/WynnExtras apikey\" to learn more.")));


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

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenApply(WynncraftApiHandler::parsePlayerData);
        });
    }

    public static CompletableFuture<AbilityMapData> fetchPlayerAbilityMap(String playerUUID, String characterUUUID) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request;

        if (INSTANCE.API_KEY == null) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("§4You currently don't have an api key set, some stats may be hidden to you." +
                    " Run \"/WynnExtras apikey\" to learn more.")));


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

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(WynncraftApiHandler::parseAbilityMapData);
    }

    public static CompletableFuture<AbilityMapData> fetchClassAbilityMap(String className) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request;

        request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.wynncraft.com/v3/ability/map/" + className))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(WynncraftApiHandler::parseAbilityMapData);
    }

    public static CompletableFuture<AbilityTreeData> fetchClassAbilityTree(String className) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request;

        request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.wynncraft.com/v3/ability/tree/" + className))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(WynncraftApiHandler::parseAbilityTreeData);
    }

    private static PlayerData parsePlayerData(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
                .create();

        return gson.fromJson(json, PlayerData.class);
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


    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras/apikeyDoNotShare.json");

    public static void load() {
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
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            gson.toJson(INSTANCE, writer);
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't write the apikey file:");
            e.printStackTrace();
        }
    }

    public static List<Text> parseStyledHtml(List<String> htmlLines) {
        return htmlLines.stream()
                .map(line -> parseSpan(sanitizeHtmlString(line), Style.EMPTY))
                .collect(Collectors.toList());
    }

    public static String sanitizeHtmlString(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\u0000-\\u001F\\u007F-\\u009F\\uE000-\\uF8FF]", "");
    }


    private static Text parseSpan(String html, Style inheritedStyle) {
        MutableText result = null; // kein leeres Literal als Root

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

            int contentStart = tagEnd + 1;
            int spanEnd = findMatchingSpanEnd(html, contentStart);
            if (spanEnd == -1) break;

            String inner = html.substring(contentStart, spanEnd);
            Style newStyle = inheritedStyle;

            Matcher colorMatch = Pattern.compile("color:\\s*#([0-9a-fA-F]{6})").matcher(style);
            if (colorMatch.find()) {
                int rgb = Integer.parseInt(colorMatch.group(1), 16);
                newStyle = newStyle.withColor(TextColor.fromRgb(rgb));
            }
            if (style.contains("font-weight:bold") || style.contains("font-weight:bolder")) {
                newStyle = newStyle.withBold(true);
            }

            Text parsedInner = parseSpan(inner, newStyle);
            MutableText piece = parsedInner instanceof MutableText ? (MutableText) parsedInner : Text.literal(parsedInner.getString()).setStyle(newStyle);
            result = (result == null) ? piece : result.append(piece);

            index = spanEnd + "</span>".length();
        }

        return (result == null) ? Text.empty() : result;
    }

    public static String stripHtml(String input) {
        return input.replaceAll("<[^>]*>", "").replaceAll("&nbsp;", " ");
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

}
