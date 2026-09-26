package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class WynnBuilderTreeCodecTest {
    private static final String EXAMPLE = "-ZMVZEtKvG";
    private static final JsonObject FIXTURES = loadFixtures();

    @ParameterizedTest
    @EnumSource(AbilityTreeClass.class)
    void matchesUpstreamOracleForAllClasses(AbilityTreeClass abilityClass) {
        AbilityTreeDefinition definition = definition(abilityClass);
        for (JsonElement element : FIXTURES.getAsJsonObject(abilityClass.displayName()).getAsJsonArray("vectors")) {
            JsonObject vector = element.getAsJsonObject();
            String hash = vector.get("hash").getAsString();
            Set<String> selected = new LinkedHashSet<>();
            for (JsonElement id : vector.getAsJsonArray("selected")) {
                selected.add(id.getAsString());
            }
            assertEquals(selected, WynnBuilderTreeCodec.decode(definition, hash), hash);
            assertEquals(hash, WynnBuilderTreeCodec.encode(definition, selected), hash);
        }
    }

    @Test
    void importsTheRealExampleThroughNormalValidationAndExportsItUnchanged() {
        AbilityTreeDefinition definition = definition(AbilityTreeClass.ARCHER);
        AbilityTreeState state = new AbilityTreeState();
        AbilityTreeState.SelectionResult result = state.importTree(definition, " \n" + EXAMPLE + "\r\n", 120);
        assertTrue(result.changed(), result.message());
        assertEquals(36, state.selected(AbilityTreeClass.ARCHER).size());
        assertTrue(state.selected(AbilityTreeClass.ARCHER).containsAll(
                Set.of("arrowbomb", "bowProficiency", "arrowBombCost1", "escapeCost1", "arrowStormCost1")));
        assertEquals(EXAMPLE, state.exportTree(definition));
    }

    @Test
    void keepsLegacyImportsAndOtherClassSelections() {
        AbilityTreeDefinition definition = definition(AbilityTreeClass.ARCHER);
        AbilityTreeState state = new AbilityTreeState();
        state.restore(Map.of("mage", Set.of("meteor")));
        assertTrue(state.importTree(definition, "wq-atree:1:archer:bowProficiency,arrowbomb", 120).changed());
        assertEquals(Set.of("bowProficiency", "arrowbomb"), state.selected(AbilityTreeClass.ARCHER));
        String compact = state.exportTree(definition);
        assertFalse(compact.startsWith("wq-atree:"));
        assertTrue(state.importTree(definition, compact, 120).changed());
        assertEquals(Set.of("meteor"), state.selected(AbilityTreeClass.MAGE));
        assertTrue(state.importTree(definition, "wq-atree:1:archer:", 120).changed());
        assertTrue(state.selected(AbilityTreeClass.ARCHER).isEmpty());
    }

    @Test
    void convertsTheExistingWynnQolExampleWithoutLosingAnyAbilities() {
        AbilityTreeDefinition definition = definition(AbilityTreeClass.ARCHER);
        AbilityTreeState state = new AbilityTreeState();
        String legacy = "wq-atree:1:archer:archerThunderPath,archerWaterPath,arrowBombCost1,"
                + "arrowStormCost1,arrowStormCost2,arrowbomb,arrowshield,arrowstorm,"
                + "betterGuardianAngels,betterLeap,betterWindyFeet,boltslingerUlt,bouncing,"
                + "bowProficiency,directHit,divineIntervention,dontGetHit,escape,escapeCost1,"
                + "fierceStomp,frenzyAbility,grapeBomb,guardianAngels,hastyShots,helicopter,"
                + "implosion,leap,nimbleString,pyrotechnics,recycling,shrapnelBomb,"
                + "shriekingBolts,transcience,tripleShots,windstorm,windyfeet";
        AbilityTreeState.SelectionResult result = state.importTree(definition, legacy, 120);
        assertTrue(result.changed(), result.message());
        Set<String> expected = Set.copyOf(state.selected(AbilityTreeClass.ARCHER));
        String compact = state.exportTree(definition);
        state.reset(AbilityTreeClass.ARCHER);
        assertTrue(state.importTree(definition, compact, 120).changed());
        assertEquals(expected, state.selected(AbilityTreeClass.ARCHER));
    }

    @Test
    void rejectsOverBudgetAndConflictingUpstreamSelections() {
        AbilityTreeDefinition definition = definition(AbilityTreeClass.ARCHER);
        String allAbilities = WynnBuilderTreeCodec.encode(definition, definition.nodes().keySet());
        AbilityTreeState state = new AbilityTreeState();
        state.restore(Map.of("archer", Set.of("arrowbomb")));
        assertFalse(state.importTree(definition, allAbilities, 120).changed());
        assertEquals(Set.of("arrowbomb"), state.selected(AbilityTreeClass.ARCHER));
    }

    @Test
    void rejectsBadCodesAndRequirementsWithoutChangingAnyClass() {
        AbilityTreeDefinition definition = definition(AbilityTreeClass.ARCHER);
        AbilityTreeState state = new AbilityTreeState();
        state.restore(Map.of("archer", Set.of("arrowbomb"), "mage", Set.of("meteor")));
        Map<String, Set<String>> previous = state.snapshot();
        for (String hash : List.of("", " ", "#", "https://wynnbuilder.github.io/#" + EXAMPLE,
                EXAMPLE + "0", EXAMPLE.substring(0, EXAMPLE.length() - 1),
                "2", "000000000000000000", "wq-atree:1:mage:meteor",
                "wq-atree:1:archer:missing", "wq-atree:1:archer:arrowbomb,pyrotechnics")) {
            assertFalse(state.importTree(definition, hash, 120).changed(), hash);
            assertEquals(previous, state.snapshot(), hash);
        }
        assertFalse(state.importTree(definition, null, 120).changed());
        assertFalse(state.importTree(definition, EXAMPLE, 1).changed());
        assertEquals(previous, state.snapshot());
    }

    @Test
    void requiresARootWhenCopyingAndUsesWynnBuilderImplicitRootOnPaste() {
        AbilityTreeDefinition definition = definition(AbilityTreeClass.ARCHER);
        assertThrows(IllegalArgumentException.class, () -> WynnBuilderTreeCodec.encode(definition, Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> WynnBuilderTreeCodec.encode(definition, Set.of("bowProficiency")));
        assertEquals("0", WynnBuilderTreeCodec.encode(definition, Set.of(definition.rootId())));
        assertEquals(Set.of(definition.rootId()), WynnBuilderTreeCodec.decode(definition, "0"));
        assertThrows(IllegalArgumentException.class,
                () -> WynnBuilderTreeCodec.encode(definition, Set.of(definition.rootId(), "pyrotechnics")));
        assertThrows(IllegalArgumentException.class,
                () -> WynnBuilderTreeCodec.encode(definition, Set.of(definition.rootId(), "newAbility")));
    }

    @Test
    void rejectsMissingOrAmbiguousApiAbilitiesWithoutChangingTheSelection() {
        AbilityTreeDefinition original = definition(AbilityTreeClass.ARCHER);
        Map<String, AbilityTreeDefinition.Node> nodes = new LinkedHashMap<>(original.nodes());
        nodes.remove("bowProficiency");
        AbilityTreeDefinition missing = withNodes(original, nodes);
        AbilityTreeState state = new AbilityTreeState();
        state.restore(Map.of("archer", Set.of("arrowbomb")));
        assertFalse(state.importTree(missing, EXAMPLE, 120).changed());
        assertEquals(Set.of("arrowbomb"), state.selected(AbilityTreeClass.ARCHER));

        AbilityTreeDefinition.Node root = original.nodes().get(original.rootId());
        nodes.put("duplicate", new AbilityTreeDefinition.Node(
                "duplicate", root.name(), root.description(), root.x(), root.y(), root.page(),
                root.abilityPointCost(), root.combatLevel(), root.requiredNodes(),
                root.archetypeRequirement(), root.locks(), root.archetype(), root.color(), root.iconName()));
        assertThrows(IllegalArgumentException.class,
                () -> WynnBuilderTreeCodec.decode(withNodes(original, nodes), "0"));
    }

    private static AbilityTreeDefinition withNodes(
            AbilityTreeDefinition original, Map<String, AbilityTreeDefinition.Node> nodes
    ) {
        return new AbilityTreeDefinition(original.abilityClass(), original.archetypes(), Map.copyOf(nodes),
                original.connectors(), original.adjacency(), original.rootId(), original.maxY());
    }

    private static AbilityTreeDefinition definition(AbilityTreeClass abilityClass) {
        JsonObject fixture = FIXTURES.getAsJsonObject(abilityClass.displayName());
        return AbilityTreeDatabase.getInstance().parse(
                abilityClass, fixture.getAsJsonObject("tree"), fixture.getAsJsonObject("map"));
    }

    private static JsonObject loadFixtures() {
        try (var stream = WynnBuilderTreeCodecTest.class.getResourceAsStream("/wynnbuilder-tree-codec.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing codec fixtures");
            }
            return JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
