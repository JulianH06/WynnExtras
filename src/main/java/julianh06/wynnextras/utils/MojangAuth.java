package julianh06.wynnextras.utils;

import com.google.gson.JsonObject;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.core.WynnExtras;
import net.minecraft.text.Text;

import java.math.BigInteger;
import java.net.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;

/**
 * Handles Mojang sessionserver authentication for secure API calls
 * Implements the standard Minecraft server authentication flow
 */
public class MojangAuth {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String MOJANG_JOIN_SERVER = "https://sessionserver.mojang.com/session/minecraft/join";

    // Cache authentication to avoid rapid-fire Mojang API calls
    // 5 minutes is safe - Mojang session tokens last for hours
    private static AuthData cachedAuthData = null;
    private static long cacheTimestamp = 0;
    private static final long CACHE_DURATION_MS = 300000; // 5 minutes
    private static final Object cacheLock = new Object();

    // Track in-progress authentication to prevent multiple simultaneous auths
    private static CompletableFuture<AuthData> pendingAuth = null;

    /**
     * Authenticate with Mojang and get a server ID for API calls
     * This performs the standard Minecraft server join flow
     *
     * @return CompletableFuture with serverId on success, or null on failure
     */
    public static CompletableFuture<String> authenticateForAPI() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var mc = McUtils.mc();
                if (mc == null || mc.player == null) {
                    return null;
                }

                // Get session via accessor
                var session = mc.getSession();
                if (session == null) {
                    WynnExtras.LOGGER.error("Session is null");
                    return null;
                }

                // Create session service (Yggdrasil auth service)
                YggdrasilAuthenticationService authService = new YggdrasilAuthenticationService(
                    Proxy.NO_PROXY
                );
                MinecraftSessionService sessionService = authService.createMinecraftSessionService();

                // Generate a random server ID (shared secret)
                String serverId = generateServerId();

                // Get player UUID
                java.util.UUID playerUuid = mc.player.getUuid();

                // Join server via Mojang (this validates our session)
                // This call internally sends accessToken to Mojang with the serverId
                try {
                    // joinServer takes UUID, accessToken, and serverId
                    sessionService.joinServer(playerUuid, session.getAccessToken(), serverId);

                    // Small delay to let Mojang propagate the session before we use it
                    // Without this, the first request often fails because hasJoined check
                    // happens before Mojang has fully processed the joinServer call
                    Thread.sleep(200);

                    WynnExtras.LOGGER.info("Successfully authenticated with Mojang sessionserver");
                    return serverId;
                } catch (AuthenticationException e) {
                    WynnExtras.LOGGER.error("Failed to join server via Mojang sessionserver", e);
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                        Text.literal("§cFailed to authenticate with Mojang. Please restart your game.")
                    ));
                    return null;
                }

            } catch (Exception e) {
                WynnExtras.LOGGER.error("Error during Mojang authentication", e);
                return null;
            }
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
     * Result of authentication containing username and serverId
     */
    public static class AuthData {
        public final String username;
        public final String serverId;

        public AuthData(String username, String serverId) {
            this.username = username;
            this.serverId = serverId;
        }
    }

    /**
     * Get authentication data for API calls
     * Call this before making authenticated API requests
     *
     * Caches authentication for 5 minutes to prevent rapid-fire Mojang API calls.
     * Also ensures only one authentication happens at a time - other callers wait
     * for the in-progress auth to complete.
     *
     * @return CompletableFuture with AuthData, or null if authentication failed
     */
    public static CompletableFuture<AuthData> getAuthData() {
        synchronized (cacheLock) {
            // Check if we have a valid cached auth
            long now = System.currentTimeMillis();
            if (cachedAuthData != null && (now - cacheTimestamp) < CACHE_DURATION_MS) {
                WynnExtras.LOGGER.info("Reusing cached Mojang authentication (age: {}ms)", now - cacheTimestamp);
                return CompletableFuture.completedFuture(cachedAuthData);
            }

            // Check if auth is already in progress - wait for it instead of starting another
            if (pendingAuth != null) {
                WynnExtras.LOGGER.info("Waiting for in-progress Mojang authentication");
                return pendingAuth;
            }

            // Start new authentication and track it
            WynnExtras.LOGGER.info("Starting fresh Mojang authentication");
            pendingAuth = authenticateForAPI().thenApply(serverId -> {
                if (serverId == null) {
                    synchronized (cacheLock) {
                        pendingAuth = null;
                    }
                    return null;
                }

                String username = McUtils.playerName();
                if (username == null) {
                    synchronized (cacheLock) {
                        pendingAuth = null;
                    }
                    return null;
                }

                AuthData authData = new AuthData(username, serverId);

                // Cache the result and clear pending flag
                synchronized (cacheLock) {
                    cachedAuthData = authData;
                    cacheTimestamp = System.currentTimeMillis();
                    pendingAuth = null;
                }

                return authData;
            });

            return pendingAuth;
        }
    }

    /**
     * Manually invalidate the authentication cache
     * Useful if an API request fails with auth error
     */
    public static void invalidateCache() {
        synchronized (cacheLock) {
            cachedAuthData = null;
            cacheTimestamp = 0;
            WynnExtras.LOGGER.info("Mojang auth cache invalidated");
        }
    }
}
