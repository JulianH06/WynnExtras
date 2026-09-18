package julianh06.wynnextras.features.buildplanner.data;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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

    private final Map<AbilityTreeClass, LinkedHashSet<String>> selectedByClass = new LinkedHashMap<>();

    public Set<String> selected(AbilityTreeClass abilityClass) {
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
        if (selected.contains(nodeId)) {
            LinkedHashSet<String> candidate = new LinkedHashSet<>(selected);
            candidate.remove(nodeId);
            String problem = validateWholeTree(definition, candidate, combatLevel);
            if (!problem.isEmpty()) {
                return new SelectionResult(false, problem);
            }
            selected.remove(nodeId);
            return new SelectionResult(true, "");
        }

        AbilityTreeDefinition.Node node = definition.nodes().get(nodeId);
        if (node == null) {
            return new SelectionResult(false, "Unknown ability");
        }
        String problem = validateAddition(definition, selected, node, combatLevel);
        if (!problem.isEmpty()) {
            return new SelectionResult(false, problem);
        }
        selected.add(nodeId);
        return new SelectionResult(true, "");
    }

    public void reset(AbilityTreeClass abilityClass) {
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
        for (AbilityTreeClass abilityClass : AbilityTreeClass.values()) {
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

        LinkedHashSet<String> previous = new LinkedHashSet<>(selected(definition.abilityClass()));
        reset(definition.abilityClass());
        LinkedHashSet<String> remaining = new LinkedHashSet<>(requested);
        boolean progressed;
        do {
            progressed = false;
            for (String id : List.copyOf(remaining)) {
                SelectionResult result = toggle(definition, id, combatLevel);
                if (result.changed()) {
                    remaining.remove(id);
                    progressed = true;
                }
            }
        } while (progressed && !remaining.isEmpty());
        if (!remaining.isEmpty()) {
            selectedByClass.put(definition.abilityClass(), previous);
            return new SelectionResult(false, "Imported tree does not meet the current requirements");
        }
        return new SelectionResult(true, "");
    }

    private String validateAddition(
            AbilityTreeDefinition definition,
            Set<String> selected,
            AbilityTreeDefinition.Node node,
            int combatLevel
    ) {
        if (combatLevel < node.combatLevel()) {
            return "Requires combat level " + node.combatLevel();
        }
        if (spent(definition) + node.abilityPointCost() > abilityPointsForLevel(combatLevel)) {
            return "Not enough ability points";
        }
        for (String requirement : node.requiredNodes()) {
            if (!selected.contains(requirement)) {
                AbilityTreeDefinition.Node required = definition.nodes().get(requirement);
                return "Requires " + (required == null ? requirement : required.name());
            }
        }
        String conflict = lockConflict(definition, selected, node);
        if (!conflict.isEmpty()) {
            AbilityTreeDefinition.Node locked = definition.nodes().get(conflict);
            return "Blocked by " + (locked == null ? conflict : locked.name());
        }
        AbilityTreeDefinition.ArchetypeRequirement archetype = node.archetypeRequirement();
        if (archetype != null
                && archetypeCounts(definition).getOrDefault(archetype.archetype(), 0) < archetype.amount()) {
            AbilityTreeDefinition.Archetype display = definition.archetypes().get(archetype.archetype());
            return "Requires " + archetype.amount() + " "
                    + (display == null ? archetype.archetype() : display.name()) + " abilities";
        }
        if (!node.id().equals(definition.rootId())
                && definition.adjacency().getOrDefault(node.id(), Set.of()).stream()
                        .filter(selected::contains)
                        .map(definition.nodes()::get)
                        .noneMatch(parent -> parent != null && parent.y() <= node.y())) {
            return "Must connect to an unlocked ability";
        }
        return "";
    }

    private String validateWholeTree(
            AbilityTreeDefinition definition,
            Set<String> selected,
            int combatLevel
    ) {
        if (selected.isEmpty()) {
            return "";
        }
        if (!selected.contains(definition.rootId())) {
            return "The root ability supports the selected tree";
        }
        Set<String> reachable = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(definition.rootId());
        reachable.add(definition.rootId());
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            for (String neighbor : definition.adjacency().getOrDefault(current, Set.of())) {
                AbilityTreeDefinition.Node currentNode = definition.nodes().get(current);
                AbilityTreeDefinition.Node neighborNode = definition.nodes().get(neighbor);
                if (selected.contains(neighbor)
                        && currentNode != null
                        && neighborNode != null
                        && neighborNode.y() >= currentNode.y()
                        && reachable.add(neighbor)) {
                    queue.addLast(neighbor);
                }
            }
        }
        if (reachable.size() != selected.size()) {
            return "This ability supports another selected branch";
        }
        Map<String, Integer> archetypes = countArchetypes(definition, selected);
        for (String id : selected) {
            AbilityTreeDefinition.Node node = definition.nodes().get(id);
            if (node == null || combatLevel < node.combatLevel()) {
                return "A selected ability no longer meets its requirements";
            }
            if (!selected.containsAll(node.requiredNodes())) {
                return node.name() + " requires another selected ability";
            }
            if (!lockConflict(definition, selected, node).isEmpty()) {
                return node.name() + " conflicts with another selected ability";
            }
            AbilityTreeDefinition.ArchetypeRequirement requirement = node.archetypeRequirement();
            if (requirement != null
                    && archetypes.getOrDefault(requirement.archetype(), 0) < requirement.amount()) {
                return node.name() + " requires more archetype abilities";
            }
        }
        return "";
    }

    private String lockConflict(
            AbilityTreeDefinition definition,
            Set<String> selected,
            AbilityTreeDefinition.Node candidate
    ) {
        for (String lock : candidate.locks()) {
            if (selected.contains(lock)) {
                return lock;
            }
        }
        for (String selectedId : selected) {
            AbilityTreeDefinition.Node selectedNode = definition.nodes().get(selectedId);
            if (selectedNode != null && selectedNode.locks().contains(candidate.id())) {
                return selectedId;
            }
        }
        return "";
    }

    private Map<String, Integer> countArchetypes(
            AbilityTreeDefinition definition,
            Set<String> selected
    ) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String id : selected) {
            AbilityTreeDefinition.Node node = definition.nodes().get(id);
            if (node != null && node.archetype() != null && !node.archetype().isBlank()) {
                counts.merge(node.archetype(), 1, Integer::sum);
            }
        }
        return counts;
    }

    public record SelectionResult(boolean changed, String message) {
    }
}
