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
                var session = ((julianh06.wynnextras.mixin.Accessor.MinecraftClientAccessor) mc).getSession();
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
     * @return CompletableFuture with AuthData, or null if authentication failed
     */
    public static CompletableFuture<AuthData> getAuthData() {
        return authenticateForAPI().thenApply(serverId -> {
            if (serverId == null) {
                return null;
            }

            String username = McUtils.playerName();
            if (username == null) {
                return null;
            }

            return new AuthData(username, serverId);
        });
    }
}
