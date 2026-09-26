package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WynnCrafterTest {
    @Test
    void reproducesTheReferenceSixIngredientEffectivenessGridAndSharesEverySlot() {
        var recipes = CraftedItemCodec.recipeChoices();
        var ingredients = CraftedItemCodec.ingredientChoices();
        var recipe = recipes.stream().filter(entry -> entry.name().equals("Boots-117-119")).findFirst().orElseThrow();
        List<String> names = List.of("Decaying Heart", "Elephelk Trunk", "Negative Rafflesia",
                "Shattered Dawnlight", "Shattered Dawnlight", "Shattered Dawnlight");
        List<Integer> ids = names.stream().map(name -> ingredients.stream().filter(entry -> entry.name().equals(name))
                .findFirst().orElseThrow().id()).toList();
        var craft = new CraftedItemCodec.Craft(recipe.id(), ids, 3, 3, 0);
        var result = CraftedItemCodec.preview(craft);
        int[] expected = {115, -15, 0, 315, 175, 145};
        for (int i = 0; i < 6; i++) assertEquals(expected[i], result.stat("ingredientEffectiveness" + i));
        assertEquals(174, result.stat("durabilityMin"));
        assertEquals(180, result.stat("durabilityMax"));
        String shared = CraftedItemCodec.shareText(craft);
        assertTrue(shared.startsWith("https://wynnbuilder.github.io/crafter/#"));
        assertTrue(shared.contains("Boots Lv. 117-119 (3\u272B, 3\u272B)"));
        assertTrue(shared.contains("Decaying Heart | Elephelk Trunk"));
        assertTrue(shared.contains("Negative Rafflesia | Shattered Dawnlight"));
        assertTrue(shared.contains("Shattered Dawnlight | Shattered Dawnlight"));
        assertEquals(CraftedItemCodec.encode(craft), CraftedItemCodec.encode(CraftedItemCodec.readCraft(shared)));
        var trunk = ingredients.stream().filter(entry -> entry.name().equals("Elephelk Trunk")).findFirst().orElseThrow();
        assertTrue(trunk.rows().stream().anyMatch(row -> row.label().equals("Strength Min") && !row.beneficial()));
        assertTrue(trunk.rows().stream().anyMatch(row -> row.label().equals("Durability") && row.beneficial()));
    }

    @Test
    void reencodesAllExistingEquipmentFixturesWithTheUpstreamModernEncoder() throws Exception {
        for (var element : fixtures("/crafted-item-oracle.json")) {
            JsonObject fixture = element.getAsJsonObject();
            var recipe = CraftedItemCodec.readCraft(fixture.get("legacy").getAsString());
            String modern = fixture.get("modern").getAsString();
            assertEquals(modern, CraftedItemCodec.encode(recipe));
            assertEquals(CraftedItemCodec.decode(modern), CraftedItemCodec.preview(recipe));
        }
    }

    @Test
    void matchesUpstreamConsumableHealingDurationChargesEffectivenessAndBothRollBounds() throws Exception {
        JsonArray fixtures = fixtures("/crafted-consumable-oracle.json");
        assertTrue(fixtures.size() >= 600);
        for (var element : fixtures) {
            JsonObject fixture = element.getAsJsonObject();
            for (String version : List.of("legacy", "modern")) {
                String code = fixture.get(version).getAsString();
                var recipe = CraftedItemCodec.readCraft(code);
                WynnItem item = CraftedItemCodec.preview(recipe);
                assertEquals(fixture.get("modern").getAsString(), CraftedItemCodec.encode(recipe), code);
                assertEquals("consumable", item.type(), code);
                assertEquals(fixture.getAsJsonArray("duration").get(0).getAsInt(), item.stat("durationMin"), code);
                assertEquals(fixture.getAsJsonArray("duration").get(1).getAsInt(), item.stat("durationMax"), code);
                assertEquals(fixture.get("charges").getAsInt(), item.stat("charges"), code);
                assertEquals(fixture.getAsJsonArray("healing").get(0).getAsInt(), item.stat("hpLow"), code);
                assertEquals(fixture.getAsJsonArray("healing").get(1).getAsInt(), item.stat("hp"), code);
                for (int i = 0; i < 6; i++) {
                    assertEquals(fixture.getAsJsonArray("effectiveness").get(i).getAsInt(),
                            item.stat("ingredientEffectiveness" + i), code);
                }
                for (String skill : List.of("str", "dex", "int", "def", "agi")) {
                    assertEquals(0, item.stat(skill + "Req"), code);
                }
                Map<String, Integer> min = new LinkedHashMap<>();
                Map<String, Integer> max = new LinkedHashMap<>();
                item.identifications().forEach((key, value) -> {
                    if (value.min() != 0) min.put(key, value.min());
                    if (value.max() != 0) max.put(key, value.max());
                });
                assertEquals(ids(fixture.getAsJsonObject("minIds")), min, code);
                assertEquals(ids(fixture.getAsJsonObject("maxIds")), max, code);
                assertThrows(IllegalArgumentException.class, () -> CraftedItemCodec.decode(code));
                assertFalse(CraftedItemCodec.matchesSlot(item, "weapon"));
            }
        }
    }

    @Test
    void exposesAllRecipeTypesAndFiltersIngredientsByProfessionAndLevel() {
        var recipes = CraftedItemCodec.recipeChoices();
        assertEquals(15, recipes.stream().map(CraftedItemCodec.Recipe::type).distinct().count());
        var ingredients = CraftedItemCodec.ingredientChoices();
        var powder = ingredients.stream().filter(item -> item.name().equals("Thunder Powder VI")).findFirst().orElseThrow();
        var none = ingredients.stream().filter(item -> item.id() == 4000).findFirst().orElseThrow();
        var lowPotion = recipes.stream().filter(item -> item.type().equals("potion") && item.levelLow() == 1)
                .findFirst().orElseThrow();
        var highPotion = recipes.stream().filter(item -> item.type().equals("potion") && item.levelHigh() == 119)
                .findFirst().orElseThrow();
        var agony = ingredients.stream().filter(item -> item.name().equals("Manifestation of Agony")).findFirst().orElseThrow();
        assertEquals(3, agony.tier());
        assertFalse(agony.supports(lowPotion));
        assertTrue(agony.supports(highPotion));
        assertFalse(powder.supports(highPotion));
        assertTrue(none.supports(highPotion));
        assertFalse(agony.details().isEmpty());
    }

    @Test
    void validatesDraftStructureAndSwapsOnlyTheChosenIngredientSlots() {
        var craft = CraftedItemCodec.emptyCraft("bow").withIngredient(0, 4001);
        var swapped = craft.swap(0, 5);
        assertEquals(4000, swapped.ingredients().get(0));
        assertEquals(4001, swapped.ingredients().get(5));
        assertEquals(4001, craft.ingredients().get(0));
        assertNotEquals(CraftedItemCodec.encode(craft), CraftedItemCodec.encode(swapped));
        assertThrows(IllegalArgumentException.class, () -> new CraftedItemCodec.Craft(1, List.of(4000), 3, 3, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new CraftedItemCodec.Craft(craft.recipeId(), craft.ingredients(), 4, 3, 0));
        assertThrows(IllegalArgumentException.class, () -> CraftedItemCodec.readCraft("invalid"));
    }

    private static JsonArray fixtures(String resource) throws Exception {
        try (var stream = WynnCrafterTest.class.getResourceAsStream(resource)) {
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
        }
    }

    private static Map<String, Integer> ids(JsonObject source) {
        Map<String, Integer> ids = new LinkedHashMap<>();
        source.entrySet().forEach(entry -> ids.put(WynnBuilderIdentifications.name(entry.getKey()), entry.getValue().getAsInt()));
        return ids;
    }
}
