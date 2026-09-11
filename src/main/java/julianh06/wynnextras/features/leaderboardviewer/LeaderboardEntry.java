package julianh06.wynnextras.features.leaderboardviewer;

import com.google.gson.JsonObject;

public record LeaderboardEntry(
        int rank,
        String name,
        String uuid,
        String prefix,
        Double score,
        JsonObject metadata,
        JsonObject rawData
) { }