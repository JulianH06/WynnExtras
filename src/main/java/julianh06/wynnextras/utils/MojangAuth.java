package julianh06.wynnextras.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.core.WynnExtras;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import javax.naming.AuthenticationException;
import java.math.BigInteger;
import java.net.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles authentication against your backend using the player's
 * Mojang session access token.
 *
 * This is ONLY used to obtain a session token.
 * After login, all API calls use the session token.
 */
public class MojangAuth {

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private static String wynnextrasToken = null;
    private static long expiryTime = 0;

    /**
     * Authenticate with backend and obtain wynnextras token
     */
    public static CompletableFuture<String> authenticateForAPI() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var mc = McUtils.mc();
                if (mc == null || mc.player == null) {
                    return null;
                }

                var session = mc.getSession();
                if (session == null) {
                    WynnExtras.LOGGER.error("Session is null");
                    return null;
                }

                YggdrasilAuthenticationService authService =
                        new YggdrasilAuthenticationService(Proxy.NO_PROXY);
                MinecraftSessionService sessionService =
                        authService.createMinecraftSessionService();

                String serverId = generateServerId();
                UUID playerUuid = mc.player.getUuid();

                try {
                    // Authenticate with Mojang
                    sessionService.joinServer(
                            playerUuid,
                            session.getAccessToken(),
                            serverId
                    );

                    // Give Mojang time to propagate session
                    Thread.sleep(250);

                    WynnExtras.LOGGER.info("Mojang authentication successful");
                    return serverId;
                } catch (Exception e) {
                    WynnExtras.LOGGER.error("Mojang authentication failed", e);

                    McUtils.sendMessageToClient(
                            WynnExtras.addWynnExtrasPrefix(
                                Text.literal("§cAuthentication failed. Please restart Minecraft.")
                            )
                    );

                    return null;
                }
            } catch (Exception e) {
                WynnExtras.LOGGER.error("Authentication error", e);
                return null;
            }
        });
    }

    public static CompletableFuture<String> login() {
        return authenticateForAPI().thenCompose(serverId -> {
            if (serverId == null) return CompletableFuture.completedFuture(null);

            String username = McUtils.playerName();
            if (username == null) return CompletableFuture.completedFuture(null);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    JsonObject body = new JsonObject();
                    body.addProperty("username", username);
                    body.addProperty("serverId", serverId);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://www.wynnextras.com/auth"))
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() != 200) return null;

                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    wynnextrasToken = json.get("token").getAsString();
                    expiryTime = json.get("expiresIn").getAsLong();

                    WynnExtras.LOGGER.info("Received WynnExtras token from backend");
                    return wynnextrasToken;
                } catch (Exception e) {
                    WynnExtras.LOGGER.error("Backend authentication failed", e);
                    return null;
                }
            });
        });
    }

    /**
     * Generate a random server ID (shared secret) for authentication
     * Uses a hash of random data to create a unique identifier
     */
    private static String generateServerId() {
        try {
            // Generate random bytes
            byte[] randomBytes = new byte[16];
            new java.security.SecureRandom().nextBytes(randomBytes);

            // Hash to create server ID
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(randomBytes);

            // Convert to hex string (Minecraft uses negative hex notation)
            BigInteger bigInt = new BigInteger(hash);
            return bigInt.toString(16);

        } catch (Exception e) {
            // Fallback: use UUID as server ID
            return java.util.UUID.randomUUID().toString().replace("-", "");
        }
    }

    /**
     * Get a valid WEToken
     */
    public static CompletableFuture<String> getWEToken() {
        long now = System.currentTimeMillis();

        if (wynnextrasToken != null && now < expiryTime) {
            return CompletableFuture.completedFuture(wynnextrasToken);
        }

        WynnExtras.LOGGER.info("Session expired or missing, logging in");
        return login();
    }

    /**
     * Force token refresh (used after 401)
     */
    public static CompletableFuture<String> refreshSession() {
        WynnExtras.LOGGER.info("Refreshing session token");

        wynnextrasToken = null;
        expiryTime = 0;

        return login();
    }
}
