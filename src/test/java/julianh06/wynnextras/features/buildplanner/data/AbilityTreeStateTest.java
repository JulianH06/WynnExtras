package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AbilityTreeStateTest {
    @Test
    void snapshotsAndRestoresSelectionsForEveryClass() {
        AbilityTreeState state = new AbilityTreeState();
        AbilityTreeDefinition archer = definition(AbilityTreeClass.ARCHER);
        AbilityTreeDefinition mage = definition(AbilityTreeClass.MAGE);
        state.toggle(archer, "root", 121);
        state.toggle(mage, "root", 121);

        AbilityTreeState restored = new AbilityTreeState();
        restored.restore(state.snapshot());

        assertEquals(Set.of("root"), restored.selected(AbilityTreeClass.ARCHER));
        assertEquals(Set.of("root"), restored.selected(AbilityTreeClass.MAGE));
    }

    @Test
    void usesTheCurrentLevelIndexedAbilityPointCap() {
        assertTrue(AbilityTreeState.abilityPointsForLevel(1) == 1);
        assertTrue(AbilityTreeState.abilityPointsForLevel(104) == 45);
        assertTrue(AbilityTreeState.abilityPointsForLevel(120) == 50);
    }

    @Test
    void enforcesConnectivityAndPreventsBreakingSelectedBranches() {
        AbilityTreeDefinition definition = definition();
        AbilityTreeState state = new AbilityTreeState();

        assertFalse(state.toggle(definition, "branch", 120).changed());
        assertTrue(state.toggle(definition, "root", 120).changed());
        assertTrue(state.toggle(definition, "branch", 120).changed());
        assertFalse(state.toggle(definition, "root", 120).changed());
        assertTrue(state.toggle(definition, "branch", 120).changed());
        assertTrue(state.toggle(definition, "root", 120).changed());
    }

    @Test
    void enforcesLocksAndArchetypeThresholds() {
        AbilityTreeDefinition definition = definition();
        AbilityTreeState state = new AbilityTreeState();

        assertTrue(state.toggle(definition, "root", 120).changed());
        assertTrue(state.toggle(definition, "branch", 120).changed());
        assertFalse(state.toggle(definition, "locked", 120).changed());
        assertFalse(state.toggle(definition, "archetypeGate", 120).changed());
    }

    private AbilityTreeDefinition definition() {
        AbilityTreeDefinition.Node root = node(
                "root", List.of(), null, List.of(), "", 1);
        AbilityTreeDefinition.Node branch = node(
                "branch", List.of("root"), null, List.of("locked"), "alpha", 2);
        AbilityTreeDefinition.Node locked = node(
                "locked", List.of(), null, List.of("branch"), "", 2);
        AbilityTreeDefinition.Node gate = node(
                "archetypeGate", List.of(),
                new AbilityTreeDefinition.ArchetypeRequirement("alpha", 2),
                List.of(), "", 2);
        return new AbilityTreeDefinition(
                AbilityTreeClass.ARCHER,
                Map.of("alpha", new AbilityTreeDefinition.Archetype(
                        "alpha", "Alpha", "", 0xFFFFFFFF)),
                Map.of("root", root, "branch", branch, "locked", locked, "archetypeGate", gate),
                List.of(),
                Map.of(
                        "root", Set.of("branch", "locked"),
                        "branch", Set.of("root", "archetypeGate"),
                        "locked", Set.of("root"),
                        "archetypeGate", Set.of("branch")),
                "root",
                4);
    }

    private AbilityTreeDefinition definition(AbilityTreeClass abilityClass) {
        AbilityTreeDefinition.Node root = node(
                "root", List.of(), null, List.of(), "", 1);
        return new AbilityTreeDefinition(
                abilityClass, Map.of(), Map.of("root", root),
                List.of(), Map.of("root", Set.of()), "root", 1);
    }

    private AbilityTreeDefinition.Node node(
            String id,
            List<String> requirements,
            AbilityTreeDefinition.ArchetypeRequirement archetypeRequirement,
            List<String> locks,
            String archetype,
            int y
    ) {
        return new AbilityTreeDefinition.Node(
                id, id, List.of(), 1, y, 1, 1, 0,
                requirements, archetypeRequirement, locks, archetype, 0xFFFFFFFF,
                "abilityTree.nodeWhite");
    }
}
