package julianh06.wynnextras.features.buildplanner.data;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public record WynnAspect(
        AbilityTreeClass abilityClass,
        String displayName,
        int id,
        String rarity,
        List<String> aliases,
        List<Tier> tiers
) {
    public WynnAspect {
        aliases = List.copyOf(aliases == null ? List.of() : aliases);
        tiers = List.copyOf(tiers == null ? List.of() : tiers);
    }

    public Tier tier(int number) {
        if (tiers.isEmpty()) {
            throw new IllegalStateException(displayName + " has no tiers");
        }
        return tiers.get(Math.max(1, Math.min(tiers.size(), number)) - 1);
    }

    public record Tier(int threshold, String description, List<JsonObject> abilities) {
        public Tier {
            List<JsonObject> copied = new ArrayList<>();
            if (abilities != null) {
                abilities.forEach(ability -> copied.add(ability.deepCopy()));
            }
            abilities = List.copyOf(copied);
        }
    }
}
