package julianh06.wynnextras.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@WEModule
public class WynncraftAuthManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String DEFAULT_OAUTH_CLIENT_ID = "w5NyhNaX9-ZTgCM_uaJgdSGITT_VfGbI";
    private static final String DEFAULT_AUTHORIZE_URL = "https://wynncraft.com/authorize";
    private static final String TOKEN_URL = "https://api.wynncraft.com/v3/oauth/token";
    private static final String ME_URL = "https://api.wynncraft.com/v3/oauth/me";
    private static final String REDIRECT_PATH = "/wynnextras/oauth/callback";
    private static final List<Integer> CALLBACK_PORTS = List.of(42871, 42872, 42873);

    private static final List<String> DEFAULT_SCOPES = List.of(
            "identify",
            "main_access",
            "character_list_access",
            "character_data_access",
            "character_build_access",
            "online_status",
            "hunted_characters_access"
    );

    private static AuthData authData = new AuthData();
    private static PendingLogin pendingLogin;

    private static final SubCommand oauthLoginCmd = new SubCommand(
            "login",
            "",
            context -> {
                startOAuthLogin();
                return 1;
            },
            null,
            null
    );

    private static final SubCommand oauthStatusCmd = new SubCommand(
            "status",
            "",
            context -> {
                printOAuthStatus();
                return 1;
            },
            null,
            null
    );

    private static final SubCommand oauthClearCmd = new SubCommand(
            "clear",
            "",
            context -> {
                clearOAuth();
                sendPrefixedMessage(Text.of("OAuth2 authorization cleared."));
                return 1;
            },
            null,
            null
    );

    private static final Command oauthCmd = new Command(
            "oauth",
            "",
            context -> {
                sendPrefixedMessage(Text.of("""
                        You can use either OAuth or an api key to authorize. OAuth is easier to setup but an api key
                        might give you more access (like for the stats of friends or guild members)
                        Wynncraft OAuth2 commands:
                        /we oauth login - authorize WynnExtras in your browser
                        /we oauth status - show current authorization
                        /we oauth clear - remove OAuth2 authorization
                        /we apikey - more info on api keys
                    """));
                return 1;
            },
            List.of(oauthLoginCmd, oauthStatusCmd, oauthClearCmd),
            null
    );

    public static void load() {
        if (McUtils.player() == null) {
            WynnExtras.LOGGER.error("[WynnExtras] Cannot load Wynncraft auth - player not loaded");
            return;
        }

        Path path = authPath();
        if (!Files.exists(path)) return;

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            AuthData loaded = GSON.fromJson(json, AuthData.class);
            if (loaded != null) authData = loaded;

            if (json.has("API_KEY") && !json.get("API_KEY").isJsonNull()) {
                authData.apiKey = json.get("API_KEY").getAsString();
            }

            WynncraftApiHandler.INSTANCE.API_KEY = emptyToNull(authData.apiKey);
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Couldn't read Wynncraft auth file:");
            e.printStackTrace();
        }
    }

    public static void save() {
        if (McUtils.player() == null) {
            WynnExtras.LOGGER.error("[WynnExtras] Cannot save Wynncraft auth - player not loaded");
            return;
        }

        authData.apiKey = emptyToNull(WynncraftApiHandler.INSTANCE.API_KEY);
        authData.API_KEY = authData.apiKey;

        Path path = authPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(authData, writer);
            }
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Couldn't write Wynncraft auth file:");
            e.printStackTrace();
        }
    }

    public static void setApiKey(String apiKey) {
        authData.apiKey = emptyToNull(apiKey);
        WynncraftApiHandler.INSTANCE.API_KEY = authData.apiKey;
        save();
    }

    public static void clearApiKey() {
        setApiKey(null);
    }

    public static boolean hasAuthentication() {
        return getBearerToken() != null;
    }

    public static boolean hasOAuthToken() {
        return authData.oauthAccessToken != null && !authData.oauthAccessToken.isBlank();
    }

    public static boolean hasApiKey() {
        return emptyToNull(WynncraftApiHandler.INSTANCE.API_KEY) != null || emptyToNull(authData.apiKey) != null;
    }

    public static String getApiKeyAuthorizationHeaderValue() {
        String apiKey = emptyToNull(WynncraftApiHandler.INSTANCE.API_KEY);
        if (apiKey == null) apiKey = emptyToNull(authData.apiKey);
        return apiKey == null ? null : "Bearer " + apiKey;
    }

    public static HttpRequest.Builder applyWynncraftApiKeyAuth(HttpRequest.Builder builder) {
        String apiKeyAuth = getApiKeyAuthorizationHeaderValue();
        if (apiKeyAuth != null) builder.header("Authorization", apiKeyAuth);
        return builder;
    }

    public static HttpRequest.Builder applyWynncraftAuth(HttpRequest.Builder builder) {
        String token = getBearerToken();
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return builder;
    }

    public static String getAuthorizationHeaderValue() {
        String token = getBearerToken();
        return token == null ? null : "Bearer " + token;
    }

    public static void handleWynncraftUnauthorized(int statusCode) {
        if (statusCode != 401 || !hasOAuthToken()) return;

        authData.oauthAccessToken = null;
        authData.oauthScopes = null;
        authData.oauthProfiles = null;
        save();

        sendPrefixedMessage(Text.of("§cWynncraft OAuth2 authorization expired or was revoked. Run /we oauth login to reconnect."));
    }

    public static CompletableFuture<HttpResponse<String>> sendWynncraftRequest(HttpRequest request) {
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    handleWynncraftUnauthorized(response.statusCode());
                    return response;
                });
    }

    public static HttpClient httpClient() {
        return HTTP_CLIENT;
    }

    private static String getBearerToken() {
        if (hasOAuthToken()) return authData.oauthAccessToken;

        String apiKey = emptyToNull(WynncraftApiHandler.INSTANCE.API_KEY);
        if (apiKey != null) return apiKey;

        return emptyToNull(authData.apiKey);
    }

    private static void startOAuthLogin() {
        String clientId = getOAuthClientId();
        if (clientId == null) {
            sendPrefixedMessage(Text.of("§cWynncraft OAuth2 client id is not configured."));
            return;
        }

        try {
            if (pendingLogin != null) pendingLogin.close();

            HttpServer server = createCallbackServer();
            int port = server.getAddress().getPort();
            String redirectUri = "http://127.0.0.1:" + port + REDIRECT_PATH;
            String state = randomBase64Url(32);
            String codeVerifier = randomBase64Url(64);
            String codeChallenge = codeChallenge(codeVerifier);

            pendingLogin = new PendingLogin(server, state, codeVerifier, redirectUri);

            server.createContext(REDIRECT_PATH, exchange -> handleOAuthCallback(exchange, pendingLogin));
            server.start();

            String authorizeUrl = buildAuthorizeUrl(clientId, redirectUri, state, codeChallenge);
            LinkUtils.openLink(authorizeUrl);

            CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES).execute(() -> {
                if (pendingLogin != null && pendingLogin.state.equals(state)) {
                    pendingLogin.close();
                    pendingLogin = null;
                }
            });

            sendPrefixedMessage(Text.of("Opened Wynncraft OAuth2 authorization in your browser."));
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to start OAuth2 login", e);
            sendPrefixedMessage(Text.of("§cFailed to start OAuth2 login. Check the log for details."));
        }
    }

    private static HttpServer createCallbackServer() throws IOException {
        IOException lastException = null;
        for (int port : CALLBACK_PORTS) {
            try {
                return HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            } catch (IOException e) {
                lastException = e;
            }
        }
        throw lastException == null ? new IOException("No OAuth2 callback port configured") : lastException;
    }

    private static void handleOAuthCallback(HttpExchange exchange, PendingLogin login) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String state = query.get("state");
        String code = query.get("code");
        String error = query.get("error");
        String errorDescription = query.get("error_description");

        if (login == null || !login.state.equals(state)) {
            writeCallbackResponse(exchange, "WynnExtras OAuth2 login failed: invalid state. You can close this tab.");
            sendPrefixedMessage(Text.of("§cOAuth2 login failed: invalid state."));
            return;
        }

        if (error != null) {
            writeCallbackResponse(exchange, "WynnExtras OAuth2 login failed: " + error + ". You can close this tab.");
            sendPrefixedMessage(Text.of("§cOAuth2 login failed: " + error + (errorDescription == null ? "" : " - " + errorDescription)));
            login.close();
            pendingLogin = null;
            return;
        }

        if (code == null || code.isBlank()) {
            writeCallbackResponse(exchange, "WynnExtras OAuth2 login failed: no code was returned. You can close this tab.");
            sendPrefixedMessage(Text.of("§cOAuth2 login failed: no code returned."));
            return;
        }

        writeCallbackResponse(exchange, "WynnExtras OAuth2 authorization succeeded. You can close this tab.");

        exchangeOAuthCode(code, login)
                .thenCompose(success -> success ? fetchOAuthMe() : CompletableFuture.completedFuture(false))
                .thenAccept(success -> {
                    if (success) {
                        sendPrefixedMessage(Text.of("§aWynncraft OAuth2 authorization saved."));
                    }
                })
                .exceptionally(ex -> {
                    WynnExtras.LOGGER.error("[WynnExtras] OAuth2 login failed", ex);
                    sendPrefixedMessage(Text.of("§cOAuth2 login failed. Check the log for details."));
                    return null;
                });

        login.close();
        pendingLogin = null;
    }

    private static CompletableFuture<Boolean> exchangeOAuthCode(String code, PendingLogin login) {
        String clientId = getOAuthClientId();
        if (clientId == null) return CompletableFuture.completedFuture(false);

        String body = formBody(Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", login.redirectUri,
                "client_id", clientId,
                "code_verifier", login.codeVerifier
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        WynnExtras.LOGGER.error("[WynnExtras] OAuth2 token exchange failed: " + response.statusCode() + " " + response.body());
                        String detail = parseOAuthErrorDetail(response.body());
                        if (detail.toLowerCase().contains("client secret")) {
                            sendPrefixedMessage(Text.of("§cOAuth2 token exchange failed: the Wynncraft app is private. If you see this Julian probably fucked something up."));
                        } else {
                            sendPrefixedMessage(Text.of("§cOAuth2 token exchange failed: " + response.statusCode() + (detail.isEmpty() ? "" : " - " + detail)));
                        }
                        return false;
                    }

                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    authData.oauthAccessToken = json.get("access_token").getAsString();
                    authData.oauthScopes = json.has("scope") && !json.get("scope").isJsonNull()
                            ? List.of(json.get("scope").getAsString().split(" "))
                            : List.of();
                    save();
                    return true;
                });
    }

    private static CompletableFuture<Boolean> fetchOAuthMe() {
        if (!hasOAuthToken()) return CompletableFuture.completedFuture(false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ME_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + authData.oauthAccessToken)
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        WynnExtras.LOGGER.error("[WynnExtras] OAuth2 identity fetch failed: " + response.statusCode() + " " + response.body());
                        return false;
                    }

                    authData.oauthMeJson = response.body();
                    updateOAuthIdentityCache(response.body());
                    save();
                    return true;
                });
    }

    private static void updateOAuthIdentityCache(String body) {
        List<String> profiles = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonObject application = root.getAsJsonObject("application");
            if (application != null && application.has("scopes") && application.get("scopes").isJsonArray()) {
                List<String> scopes = new ArrayList<>();
                for (JsonElement element : application.getAsJsonArray("scopes")) {
                    scopes.add(element.getAsString());
                }
                authData.oauthScopes = scopes;
            }

            JsonObject profileJson = root.getAsJsonObject("profiles");
            if (profileJson != null) {
                for (Map.Entry<String, JsonElement> entry : profileJson.entrySet()) {
                    JsonObject profile = entry.getValue().getAsJsonObject();
                    String username = profile.has("username") && !profile.get("username").isJsonNull()
                            ? profile.get("username").getAsString()
                            : entry.getKey();
                    boolean primary = profile.has("primary") && !profile.get("primary").isJsonNull()
                            && profile.get("primary").getAsBoolean();
                    profiles.add(primary ? username + " (primary)" : username);
                }
            }
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to parse OAuth2 identity", e);
        }
        authData.oauthProfiles = profiles;
    }

    private static String parseOAuthErrorDetail(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("detail") && !json.get("detail").isJsonNull()) return json.get("detail").getAsString();
            if (json.has("error") && !json.get("error").isJsonNull()) return json.get("error").getAsString();
        } catch (Exception ignored) {
        }
        return "";
    }

    private static void printOAuthStatus() {
        if (!hasOAuthToken()) {
            String apiKey = emptyToNull(WynncraftApiHandler.INSTANCE.API_KEY);
            if (apiKey == null) apiKey = emptyToNull(authData.apiKey);
            sendPrefixedMessage(Text.of(
                    apiKey == null
                            ? "No Wynncraft OAuth2 authorization or API key is set."
                            : "No OAuth2 authorization is set. Classic API key fallback is available."));
            return;
        }

        fetchOAuthMe().thenAccept(success -> {
            String profiles = authData.oauthProfiles == null || authData.oauthProfiles.isEmpty()
                    ? "none"
                    : String.join(", ", authData.oauthProfiles);
            String scopes = authData.oauthScopes == null || authData.oauthScopes.isEmpty()
                    ? "none"
                    : String.join(", ", authData.oauthScopes);

            sendPrefixedMessage(Text.of(
                    (success ? "OAuth2 authorization is active." : "OAuth2 token is saved, but identity check failed.")
                            + "\nProfiles: " + profiles
                            + "\nScopes: " + scopes));
        });
    }

    private static void sendPrefixedMessage(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        Runnable send = () -> WynnExtras.sendMessageToClient(message);
        if (client.isOnThread()) {
            send.run();
        } else {
            client.execute(send);
        }
    }

    private static void clearOAuth() {
        authData.oauthAccessToken = null;
        authData.oauthScopes = null;
        authData.oauthProfiles = null;
        authData.oauthMeJson = null;
        save();
    }

    private static Path authPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("wynnextras/" + McUtils.player().getUuid() + "/apikeyDoNotShare.json");
    }

    private static String getOAuthClientId() {
        return emptyToNull(DEFAULT_OAUTH_CLIENT_ID);
    }

    private static String buildAuthorizeUrl(String clientId, String redirectUri, String state, String codeChallenge) {
        String scope = String.join(" ", DEFAULT_SCOPES);
        return getAuthorizeUrl()
                + "?response_type=code"
                + "&client_id=" + urlEncode(clientId)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&scope=" + urlEncode(scope)
                + "&state=" + urlEncode(state)
                + "&code_challenge=" + urlEncode(codeChallenge)
                + "&code_challenge_method=S256"
                + "&code_challenge_type=S256";
    }

    private static String getAuthorizeUrl() {
        String configured = emptyToNull(authData.oauthAuthorizeUrl);
        return configured == null ? DEFAULT_AUTHORIZE_URL : configured;
    }

    private static String formBody(Map<String, String> values) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            joiner.add(urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()));
        }
        return joiner.toString();
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isBlank()) return result;

        for (String part : query.split("&")) {
            int separator = part.indexOf('=');
            if (separator == -1) {
                result.put(urlDecode(part), "");
            } else {
                result.put(urlDecode(part.substring(0, separator)), urlDecode(part.substring(separator + 1)));
            }
        }

        return result;
    }

    private static void writeCallbackResponse(HttpExchange exchange, String message) throws IOException {
        byte[] bytes = ("<!doctype html><html><body><p>" + escapeHtml(message) + "</p></body></html>")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String randomBase64Url(int bytes) {
        byte[] data = new byte[bytes];
        RANDOM.nextBytes(data);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static String codeChallenge(String verifier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static class PendingLogin {
        private final HttpServer server;
        private final String state;
        private final String codeVerifier;
        private final String redirectUri;

        private PendingLogin(HttpServer server, String state, String codeVerifier, String redirectUri) {
            this.server = server;
            this.state = state;
            this.codeVerifier = codeVerifier;
            this.redirectUri = redirectUri;
        }

        private void close() {
            server.stop(0);
        }
    }

    private static class AuthData {
        public String apiKey;
        public String API_KEY;
        public String oauthAccessToken;
        public String oauthAuthorizeUrl;
        public List<String> oauthScopes;
        public List<String> oauthProfiles;
        public String oauthMeJson;
    }
}