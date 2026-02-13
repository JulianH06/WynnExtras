package julianh06.wynnextras.features.badges;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches username to UUID mappings from the player list.
 * Used to look up UUIDs for chat badge rendering.
 */
public class PlayerUuidCache {

    private static final Map<String, String> usernameToUuid = new ConcurrentHashMap<>();

    /**
     * Update the cache from the current player list.
     * Should be called periodically or on player list changes.
     */
    public static void updateFromPlayerList() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return;

        Collection<PlayerListEntry> playerList = mc.getNetworkHandler().getPlayerList();
        for (PlayerListEntry entry : playerList) {
            if (entry.getProfile() != null) {
                // Use standard GameProfile accessors
                String username = entry.getProfile().getName().toLowerCase();
                String uuid = entry.getProfile().getId().toString();
                usernameToUuid.put(username, uuid);
            }
        }
    }

    /**
     * Get the UUID for a username.
     * @param username The player's username (case-insensitive)
     * @return The UUID string, or null if not found
     */
    public static String getUuid(String username) {
        if (username == null || username.isEmpty()) return null;
        return usernameToUuid.get(username.toLowerCase());
    }

    /**
     * Check if a username belongs to a WynnExtras user.
     * @param username The player's username
     * @return true if they are a WynnExtras user
     */
    public static boolean isWynnExtrasUser(String username) {
        String uuid = getUuid(username);
        if (uuid == null) return false;
        return BadgeService.isWynnExtrasUser(uuid);
    }

    /**
     * Clear the cache.
     */
    public static void clear() {
        usernameToUuid.clear();
    }
}
