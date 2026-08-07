package julianh06.wynnextras.wynncraft.state;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class TerritoryState {
    private static final Map<String, String> DEFENSES = new ConcurrentHashMap<>();

    private TerritoryState() {}

    public static Optional<String> currentTerritory() {
        return WarState.territory();
    }

    public static Optional<String> defense(String territory) {
        return territory == null ? Optional.empty() : Optional.ofNullable(DEFENSES.get(territory));
    }

    public static void cacheDefense(String territory, String defense) {
        if (territory != null && !territory.isBlank() && defense != null && !defense.isBlank()) {
            DEFENSES.put(territory, defense);
        }
    }
}
