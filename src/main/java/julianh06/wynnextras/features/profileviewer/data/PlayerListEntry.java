package julianh06.wynnextras.features.profileviewer.data;

/**
 * Represents a player entry from /user/list endpoint
 */
public class PlayerListEntry {
    private String playerUuid;
    private String playerName;
    private String modVersion;
    private long lastUpdated;
    private int aspectCount;

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getModVersion() {
        return modVersion;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public int getAspectCount() {
        return aspectCount;
    }
}
