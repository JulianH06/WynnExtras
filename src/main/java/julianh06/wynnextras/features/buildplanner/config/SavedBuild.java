package julianh06.wynnextras.features.buildplanner.config;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SavedBuild(
        String name,
        String characterClass,
        int level,
        Map<String, String> equipment,
        Map<String, String> powders,
        List<SavedOffhand> offhands,
        int[] assignedSkills,
        Map<String, String> tomes,
        Map<String, SavedAspect> aspects,
        Map<String, Set<String>> abilityTrees,
        long savedAt
) {
    public SavedBuild {
        name = name == null ? "" : name.trim();
        characterClass = characterClass == null ? "" : characterClass.trim();
        level = Math.clamp(level, 1, 121);
        equipment = Map.copyOf(new LinkedHashMap<>(equipment == null ? Map.of() : equipment));
        powders = Map.copyOf(new LinkedHashMap<>(powders == null ? Map.of() : powders));
        offhands = List.copyOf(offhands == null ? List.of() : offhands.stream()
                .filter(java.util.Objects::nonNull).limit(3).toList());
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

    public record SavedOffhand(String item, String powders) {
        public SavedOffhand {
            item = item == null ? "" : item.trim();
            powders = powders == null ? "" : powders.trim().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
