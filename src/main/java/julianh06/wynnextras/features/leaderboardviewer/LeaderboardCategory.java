package julianh06.wynnextras.features.leaderboardviewer;

import java.util.List;

public record LeaderboardCategory(String displayName, List<LeaderboardDefinition> leaderboards) {
    public LeaderboardCategory {
        leaderboards = List.copyOf(leaderboards);
    }
}