package julianh06.wynnextras.features.buildplanner.data;

import julianh06.wynnextras.wynncraft.state.CharacterClass;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class AbilityTreeState {
    public static final int MAX_ABILITY_POINTS = 50;
    private static final int[] ABILITY_POINT_LEVELS = {
            1, 2, 4, 6, 8, 10, 12, 13, 15, 17,
            18, 20, 22, 23, 25, 26, 28, 30, 32, 34,
            37, 39, 41, 44, 46, 48, 50, 52, 54, 56,
            58, 60, 62, 64, 67, 70, 73, 76, 80, 84,
            88, 92, 96, 100, 104, 107, 110, 113, 116, 120
    };

    private final Map<CharacterClass, LinkedHashSet<String>> selectedByClass = new LinkedHashMap<>();

    public Set<String> selected(CharacterClass abilityClass) {
        return Collections.unmodifiableSet(
                selectedByClass.computeIfAbsent(abilityClass, ignored -> new LinkedHashSet<>()));
    }

    public int spent(AbilityTreeDefinition definition) {
        int total = 0;
        for (String id : selected(definition.abilityClass())) {
            AbilityTreeDefinition.Node node = definition.nodes().get(id);
            if (node != null) {
                total += node.abilityPointCost();
            }
        }
        return total;
    }

    public int remaining(AbilityTreeDefinition definition, int combatLevel) {
        return abilityPointsForLevel(combatLevel) - spent(definition);
    }

    public static int abilityPointsForLevel(int combatLevel) {
        int points = 0;
        for (int level : ABILITY_POINT_LEVELS) {
            if (combatLevel < level) {
                break;
            }
            points++;
        }
        return points;
    }

    public Map<String, Integer> archetypeCounts(AbilityTreeDefinition definition) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String id : selected(definition.abilityClass())) {
            AbilityTreeDefinition.Node node = definition.nodes().get(id);
            if (node != null && node.archetype() != null && !node.archetype().isBlank()) {
                counts.merge(node.archetype(), 1, Integer::sum);
            }
        }
        return counts;
    }

    public SelectionResult toggle(AbilityTreeDefinition definition, String nodeId, int combatLevel) {
        LinkedHashSet<String> selected = selectedByClass.computeIfAbsent(
                definition.abilityClass(), ignored -> new LinkedHashSet<>());
        if (!definition.nodes().containsKey(nodeId)) {
            return new SelectionResult(false, "Unknown ability");
        }
        if (selected.contains(nodeId)) {
            selected.remove(nodeId);
            return new SelectionResult(true, "");
        }
        selected.add(nodeId);
        return new SelectionResult(true, "");
    }

    public void reset(CharacterClass abilityClass) {
        selectedByClass.remove(abilityClass);
    }

    public Map<String, Set<String>> snapshot() {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        selectedByClass.forEach((abilityClass, selected) ->
                result.put(abilityClass.apiName(), Set.copyOf(selected)));
        return Map.copyOf(result);
    }

    public void restore(Map<String, Set<String>> snapshot) {
        selectedByClass.clear();
        if (snapshot == null) {
            return;
        }
        for (CharacterClass abilityClass : CharacterClass.playableValues()) {
            Set<String> selected = snapshot.get(abilityClass.apiName());
            if (selected != null) {
                selectedByClass.put(abilityClass, new LinkedHashSet<>(selected));
            }
        }
    }

    public String exportTree(AbilityTreeDefinition definition) {
        return WynnBuilderTreeCodec.encode(definition, selected(definition.abilityClass()));
    }

    public SelectionResult importTree(
            AbilityTreeDefinition definition,
            String encoded,
            int combatLevel
    ) {
        String prefix = "wq-atree:1:" + definition.abilityClass().apiName() + ":";
        if (encoded == null || encoded.isBlank()) {
            return new SelectionResult(false, "Clipboard does not contain a tree code");
        }
        encoded = encoded.trim();
        LinkedHashSet<String> requested = new LinkedHashSet<>();
        if (encoded.startsWith("wq-atree:")) {
            if (!encoded.startsWith(prefix)) {
                return new SelectionResult(false, "Clipboard does not contain a "
                        + definition.abilityClass().displayName() + " tree");
            }
            String payload = encoded.substring(prefix.length()).trim();
            if (!payload.isEmpty()) {
                Collections.addAll(requested, payload.split(","));
            }
        } else {
            try {
                requested.addAll(WynnBuilderTreeCodec.decode(definition, encoded));
            } catch (IllegalArgumentException exception) {
                return new SelectionResult(false, exception.getMessage());
            }
        }
        if (!definition.nodes().keySet().containsAll(requested)) {
            return new SelectionResult(false, "Tree contains abilities that no longer exist");
        }

        selectedByClass.put(definition.abilityClass(), requested);
        return new SelectionResult(true, "");
    }

    public record SelectionResult(boolean changed, String message) {
    }
}
