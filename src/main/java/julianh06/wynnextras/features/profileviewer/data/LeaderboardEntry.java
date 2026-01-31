package julianh06.wynnextras.features.profileviewer.data;

/**
 * Represents a leaderboard entry from /user/leaderboard endpoint
 */
public class LeaderboardEntry {
    private String playerUuid;
    public String playerName;
    public int maxAspectCount;

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getMaxAspectCount() {
        return maxAspectCount;
    }
}
