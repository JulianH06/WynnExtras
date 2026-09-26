package julianh06.wynnextras.features.buildplanner.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record AbilityTreeDefinition(
        AbilityTreeClass abilityClass,
        Map<String, Archetype> archetypes,
        Map<String, Node> nodes,
        List<Connector> connectors,
        Map<String, Set<String>> adjacency,
        String rootId,
        int maxY
) {
    public record Archetype(String id, String name, String description, int color) {
    }

    public record Node(
            String id,
            String name,
            List<String> description,
            int x,
            int y,
            int page,
            int abilityPointCost,
            int combatLevel,
            List<String> requiredNodes,
            ArchetypeRequirement archetypeRequirement,
            List<String> locks,
            String archetype,
            int color,
            String iconName
    ) {
    }

    public record ArchetypeRequirement(String archetype, int amount) {
    }

    public record Connector(
            int x,
            int y,
            Set<Direction> directions,
            List<Edge> paths,
            String iconName
    ) {
    }

    public record Edge(String first, String second) {
    }

    public enum Direction {
        UP,
        RIGHT,
        DOWN,
        LEFT
    }
}
