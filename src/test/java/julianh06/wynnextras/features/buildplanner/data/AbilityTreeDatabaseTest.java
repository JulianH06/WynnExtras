package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class AbilityTreeDatabaseTest {
    @Test
    void parsesLiveApiRequirementShapesAndMapConnections() {
        String treeJson = """
                {
                  "archetypes": {
                    "test": {
                      "name": "<span style='color:#12AB34'>Test Type</span>",
                      "description": "Description"
                    }
                  },
                  "pages": {
                    "1": {
                      "root": {
                        "name": "<b>Root</b>",
                        "icon": {"value":{"name":"abilityTree.nodeRed"}},
                        "coordinates":{"x":5,"y":1},
                        "description":["Root description"],
                        "requirements":{"ABILITY_POINTS":1},
                        "locks":null,
                        "page":1
                      },
                      "child": {
                        "name": "Child",
                        "icon": {"value":{"name":"abilityTree.nodeBlue"}},
                        "coordinates":{"x":5,"y":3},
                        "description":["\uE01B Damage: +50%", "Test Type Archetype"],
                        "requirements":{
                          "ABILITY_POINTS":2,
                          "COMBAT_LEVEL":20,
                          "NODE":["root"],
                          "ARCHETYPE":{"name":"test","amount":1}
                        },
                        "locks":["ROOT"],
                        "page":1
                      }
                    }
                  }
                }
                """;
        String mapJson = """
                {
                  "1": [
                    {
                      "type":"ability",
                      "coordinates":{"x":5,"y":1},
                      "meta":{"id":"root"}
                    },
                    {
                      "type":"connector",
                      "coordinates":{"x":5,"y":2},
                      "meta":{
                        "icon":"connector_up_down",
                        "paths":[["root","child"]]
                      }
                    },
                    {
                      "type":"ability",
                      "coordinates":{"x":5,"y":3},
                      "meta":{"id":"child"}
                    }
                  ]
                }
                """;

        AbilityTreeDefinition result = AbilityTreeDatabase.getInstance().parse(
                AbilityTreeClass.ARCHER,
                JsonParser.parseString(treeJson).getAsJsonObject(),
                JsonParser.parseString(mapJson).getAsJsonObject());

        assertEquals("root", result.rootId());
        assertEquals(2, result.nodes().size());
        assertEquals(20, result.nodes().get("child").combatLevel());
        assertEquals("test", result.nodes().get("child").archetype());
        assertEquals("Damage: +50%", result.nodes().get("child").description().getFirst());
        assertEquals(1, result.nodes().get("child").archetypeRequirement().amount());
        assertEquals(List.of("root"), result.nodes().get("child").locks());
        assertTrue(result.adjacency().get("root").contains("child"));
        assertTrue(result.connectors().getFirst().directions()
                .contains(AbilityTreeDefinition.Direction.DOWN));
    }
}
