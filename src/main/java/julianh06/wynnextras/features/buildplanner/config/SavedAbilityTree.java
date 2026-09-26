package julianh06.wynnextras.features.buildplanner.config;

import java.util.LinkedHashSet;
import java.util.Set;

public record SavedAbilityTree(
        String name,
        String characterClass,
        Set<String> selectedNodes,
        long savedAt
) {
    public SavedAbilityTree {
        name = name == null ? "" : name.trim();
        characterClass = characterClass == null ? "" : characterClass.trim();
        selectedNodes = Set.copyOf(new LinkedHashSet<>(
                selectedNodes == null ? Set.of() : selectedNodes));
    }
}
