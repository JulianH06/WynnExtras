package julianh06.wynnextras.features.buildplanner.config;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public record SavedBuild(
        String name,
        String characterClass,
        int level,
        Map<String, String> equipment,
        Map<String, String> powders,
        int[] assignedSkills,
        Map<String, String> tomes,
        Map<String, SavedAspect> aspects,
        Map<String, Set<String>> abilityTrees,
        long savedAt
) {
    public SavedBuild {
        name = name == null ? "" : name.trim();
        characterClass = characterClass == null ? "" : characterClass.trim();
        level = Math.max(1, Math.min(121, level));
        equipment = Map.copyOf(new LinkedHashMap<>(equipment == null ? Map.of() : equipment));
        powders = Map.copyOf(new LinkedHashMap<>(powders == null ? Map.of() : powders));
        assignedSkills = Arrays.copyOf(
                assignedSkills == null ? new int[5] : assignedSkills, 5);
        tomes = Map.copyOf(new LinkedHashMap<>(tomes == null ? Map.of() : tomes));
        aspects = Map.copyOf(new LinkedHashMap<>(aspects == null ? Map.of() : aspects));
        Map<String, Set<String>> trees = new LinkedHashMap<>();
        if (abilityTrees != null) {
            abilityTrees.forEach((key, value) ->
                    trees.put(key, Set.copyOf(new LinkedHashSet<>(value))));
        }
        abilityTrees = Map.copyOf(trees);
    }

    @Override
    public int[] assignedSkills() {
        return Arrays.copyOf(assignedSkills, assignedSkills.length);
    }

    public record SavedAspect(String name, int tier) {
        public SavedAspect {
            name = name == null ? "" : name.trim();
            tier = Math.max(1, tier);
        }
    }
}
