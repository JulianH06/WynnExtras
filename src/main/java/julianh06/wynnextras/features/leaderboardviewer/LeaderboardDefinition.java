package julianh06.wynnextras.features.leaderboardviewer;

import java.util.List;

public record LeaderboardDefinition(String id, String displayName, List<LeaderboardDefinition> variants) {
    public LeaderboardDefinition(String id, String displayName) {
        this(id, displayName, List.of());
    }

    public boolean hasVariants() {
        return !variants.isEmpty();
    }
}
