package julianh06.wynnextras.features.buildplanner.data;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AtlasSearchTest {
    private AtlasState allTypes(boolean ingredients) {
        return new AtlasState(ingredients, "", "", "", "0", "121", "filters", false, 0, "",
                List.of(), List.of(), List.of());
    }

    @Test
    void itemAndIngredientDefaultsSelectNoTypes() {
        var items = AtlasSearch.items(List.of(
                new WynnItem("Bow", "weapon", "bow", "rare", "", Map.of(), Map.of("lvl", 20))));
        var ingredients = AtlasSearch.ingredients(CraftedItemCodec.ingredientChoices());
        for (boolean ingredientMode : List.of(false, true)) {
            var state = AtlasState.defaults(ingredientMode);
            assertEquals("~none", state.category());
            assertEquals("", state.rarity());
            assertFalse(AtlasSearch.matchesSelection(state.category(), ingredientMode ? "ALCHEMISM" : "bow"));
            var catalog = ingredientMode ? ingredients : items;
            assertTrue(AtlasSearch.search(catalog, state).isEmpty());
            assertFalse(AtlasSearch.search(catalog, allTypes(ingredientMode)).isEmpty());
            var selected = new AtlasState(ingredientMode, "", ingredientMode ? "ALCHEMISM" : "bow", "",
                    "0", "121", "filters", false, 0, "", List.of(), List.of(), List.of());
            assertFalse(AtlasSearch.search(catalog, selected).isEmpty());
        }
    }

    @Test
    void addedFiltersDefaultToDescendingWithoutBounds() {
        var filter = new AtlasState.NumericFilter("manaRegen");
        assertEquals("", filter.minimum());
        assertEquals("", filter.maximum());
        assertTrue(filter.descending());
        var catalog = AtlasSearch.items(List.of(
                new WynnItem("Negative", "weapon", "bow", "rare", "",
                        Map.of("manaRegen", new Identification(-6, -4, -2)), Map.of("lvl", 20)),
                new WynnItem("Zero", "weapon", "bow", "rare", "", Map.of(), Map.of("lvl", 20)),
                new WynnItem("Positive", "weapon", "bow", "rare", "",
                        Map.of("manaRegen", new Identification(2, 4, 6)), Map.of("lvl", 20))));
        var state = new AtlasState(false, "", "", "", "", "", "filters", false, 0, "",
                List.of(filter), List.of(), List.of());
        assertEquals(List.of("Positive", "Zero", "Negative"), AtlasSearch.search(catalog, state)
                .stream().map(AtlasSearch.Entry::name).toList());
    }

    @Test
    void defaultsHaveNoAutomaticNumericFilters() {
        assertTrue(AtlasState.defaults().filters().isEmpty());
        assertTrue(AtlasState.defaults(true).filters().isEmpty());
        var catalog = AtlasSearch.items(List.of(
                new WynnItem("Low", "weapon", "bow", "rare", "", Map.of(), Map.of("lvl", 1)),
                new WynnItem("High", "weapon", "bow", "rare", "", Map.of(), Map.of("lvl", 100))));
        assertEquals(List.of("High", "Low"), AtlasSearch.search(catalog, allTypes(false))
                .stream().map(AtlasSearch.Entry::name).toList());
        var manualLevel = new AtlasState(false, "", "", "", "", "", "filters", false, 0, "",
                List.of(new AtlasState.NumericFilter("lvl", "50", "", false)), List.of(), List.of());
        assertEquals(List.of("High"), AtlasSearch.search(catalog, manualLevel)
                .stream().map(AtlasSearch.Entry::name).toList());
    }

    @Test
    void exposesAllResultsRatherThanTheOldHundredItemPickerLimit() {
        var database = new ItemDatabase();
        List<WynnItem> items = new ArrayList<>();
        for (int i = 0; i < 235; i++) items.add(new WynnItem("Atlas item " + i, "weapon", "bow", "normal", "normal", Map.of(), Map.of("lvl", 100)));
        long revision = database.revision();
        database.replaceItems(items);
        assertTrue(database.revision() > revision);
        assertEquals(235, database.allItems().size());
        assertEquals(100, database.searchItems("Atlas").size());
        assertEquals(235, AtlasSearch.search(AtlasSearch.items(database.allItems()), allTypes(false)).size());
    }

    @Test
    void findsBothMasterworksAndOriginalsAndSupportsEffectsRarityLevelAndType() throws Exception {
        List<WynnItem> items;
        try (var stream = getClass().getResourceAsStream("/api-masterwork-items.json")) {
            items = new ItemDatabase().parseItems(JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        }
        var catalog = AtlasSearch.items(items);
        var state = new AtlasState(false, "divzer", "bow", "mythic", "0", "121", "level", true, 0, "");
        var results = AtlasSearch.search(catalog, state);
        assertEquals("Masterwork Divzer", results.get(0).name());
        assertEquals("Divzer", results.get(1).name());
        assertEquals(1, AtlasSearch.search(catalog,
                new AtlasState(false, "phase vector", "bow", "mythic", "100", "121", "name", false, 0, "")).size());
        assertTrue(AtlasSearch.search(catalog,
                new AtlasState(false, "mana regen", "", "", "0", "121", "name", false, 0, "")).size() > 0);
    }

    @Test
    void filtersIngredientProfessionsAndTiersAndExcludesTheEmptySlotPlaceholder() {
        var catalog = AtlasSearch.ingredients(CraftedItemCodec.ingredientChoices());
        assertFalse(catalog.stream().anyMatch(entry -> entry.name().equals("No Ingredient")));
        var state = new AtlasState(true, "manifestation of agony", "ALCHEMISM", "3", "110", "119", "level", false, 0, "");
        assertEquals(1, AtlasSearch.search(catalog, state).size());
        assertTrue(AtlasSearch.search(catalog,
                new AtlasState(true, "manifestation of agony", "JEWELING", "3", "0", "121", "name", false, 0, "")).isEmpty());
        assertFalse(AtlasSearch.search(catalog,
                new AtlasState(true, "effectiveness touching", "", "", "0", "121", "name", false, 0, "")).isEmpty());
    }

    @Test
    void hidesAllPowdersOnlyFromAtlasWhileRetainingRegularIngredients() {
        var choices = CraftedItemCodec.ingredientChoices();
        var catalog = AtlasSearch.ingredients(choices);
        var ids = catalog.stream().map(entry -> entry.ingredient().id()).toList();
        for (int id = 4001; id <= 4035; id++) {
            int powderId = id;
            assertTrue(choices.stream().anyMatch(ingredient -> ingredient.id() == powderId));
            assertTrue(CraftedItemCodec.isPowderIngredient(id));
            assertFalse(ids.contains(id));
            assertFalse(CraftedItemCodec.ingredientStats(id).isEmpty());
        }
        assertFalse(CraftedItemCodec.isPowderIngredient(4000));
        assertEquals(choices.stream().filter(ingredient -> ingredient.id() < 4000 || ingredient.id() > 4035)
                .map(CraftedItemCodec.Ingredient::id).toList(), ids);
        var state = new AtlasState(true, "thunder powder", "", "", "0", "121", "name", false, 0, "");
        assertTrue(AtlasSearch.search(catalog, state).isEmpty());
    }

    @Test
    void reportsInvalidFilterInputsInsteadOfSilentlyChangingTheirMeaning() {
        for (AtlasState state : List.of(
                new AtlasState(false, "", "", "", "bad", "121", "name", false, 0, ""),
                new AtlasState(false, "", "", "", "100", "50", "name", false, 0, ""),
                new AtlasState(false, "", "", "", "0", "121", "unknown", false, 0, ""))) {
            assertThrows(IllegalArgumentException.class, () -> AtlasSearch.search(List.of(), state));
        }
    }

    @Test
    void multipleTypesAndRaritiesUseOrWithinEachGroupAndNoTypesStillMatchesNothing() {
        var catalog = AtlasSearch.items(List.of(
                new WynnItem("Bow", "weapon", "bow", "rare", "", Map.of(), Map.of("lvl", 20)),
                new WynnItem("Wand", "weapon", "wand", "mythic", "", Map.of(), Map.of("lvl", 30)),
                new WynnItem("Spear", "weapon", "spear", "rare", "", Map.of(), Map.of("lvl", 40))));
        assertEquals(List.of("Bow", "Wand"), AtlasSearch.search(catalog,
                new AtlasState(false, "", "bow,wand", "rare,mythic", "0", "121", "name", false, 0, ""))
                .stream().map(AtlasSearch.Entry::name).toList());
        assertTrue(AtlasSearch.search(catalog,
                new AtlasState(false, "", "~none", "", "0", "121", "name", false, 0, "")).isEmpty());
        var ingredients = AtlasSearch.ingredients(CraftedItemCodec.ingredientChoices());
        var results = AtlasSearch.search(ingredients,
                new AtlasState(true, "", "ALCHEMISM,COOKING", "2,3", "0", "121", "name", false, 0, ""));
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(entry -> entry.ingredient().professions().contains("ALCHEMISM")
                || entry.ingredient().professions().contains("COOKING")));
        assertEquals(results.size(), results.stream().map(AtlasSearch.Entry::id).distinct().count());
    }

    @Test
    void noItemRaritiesSelectedShowsEveryRarityButKeepsOtherFilters() {
        List<WynnItem> items = new ArrayList<>();
        for (String rarity : List.of("normal", "unique", "rare", "legendary", "set", "fabled", "mythic")) {
            items.add(new WynnItem("Shared " + rarity, "weapon", "bow", rarity, "", Map.of(), Map.of("lvl", 20)));
        }
        items.add(new WynnItem("Unrelated", "weapon", "bow", "mythic", "", Map.of(), Map.of("lvl", 20)));
        items.add(new WynnItem("Shared wand", "weapon", "wand", "mythic", "", Map.of(), Map.of("lvl", 20)));
        items.add(new WynnItem("Shared high level", "weapon", "bow", "mythic", "", Map.of(), Map.of("lvl", 100)));
        var catalog = AtlasSearch.items(items);
        var cleared = new AtlasState(false, "shared", "bow", "~none", "0", "30", "name", false, 0, "");
        var all = new AtlasState(false, "shared", "bow", "", "0", "30", "name", false, 0, "");
        assertEquals(7, AtlasSearch.search(catalog, cleared).size());
        assertEquals(AtlasSearch.search(catalog, all), AtlasSearch.search(catalog, cleared));
        assertFalse(AtlasSearch.matchesSelection("~none", "mythic"));
        for (String selection : List.of("mythic", "rare,mythic")) {
            var selected = new AtlasState(false, "shared", "bow", selection, "0", "30", "name", false, 0, "");
            var results = AtlasSearch.search(catalog, selected);
            assertEquals(selection.split(",").length, results.size());
            assertTrue(results.stream().allMatch(entry -> List.of(selection.split(",")).contains(entry.rarity())));
        }
        assertEquals(items.size(), AtlasSearch.search(catalog,
                new AtlasState(false, "", "", "~none", "0", "121", "name", false, 0, "")).size());
    }

    @Test
    void clearingIngredientStarsStillMatchesNothing() {
        var catalog = AtlasSearch.ingredients(CraftedItemCodec.ingredientChoices());
        assertFalse(AtlasSearch.search(catalog, allTypes(true)).isEmpty());
        assertTrue(AtlasSearch.search(catalog,
                new AtlasState(true, "", "", "~none", "0", "121", "name", false, 0, "")).isEmpty());
    }

    @Test
    void numericBoundsExclusionsAndStringFiltersComposeAndUseRawNumbers() {
        var catalog = AtlasSearch.items(List.of(
                    new WynnItem("First", "weapon", "bow", "rare", "", Map.of("manaRegen", new Identification(1, 2, 3)),
                            Map.of("lvl", 20), "", "", 0, "", List.of(), Map.of("Test Major", "Special effect"), "Ancient bow", ""),
                    new WynnItem("Second", "weapon", "bow", "rare", "", Map.of("manaRegen", new Identification(2, 4, 6)),
                            Map.of("lvl", 30)),
                    new WynnItem("Excluded", "weapon", "bow", "rare", "", Map.of("manaRegen", new Identification(2, 4, 6),
                            "walkSpeed", new Identification(-1, -1, -1)), Map.of("lvl", 30))));
        var state = new AtlasState(false, "", "", "", "0", "121", "filters", false, 0, "",
                    List.of(new AtlasState.NumericFilter("manaRegen", "3", "", true)), List.of("walkSpeed"), List.of());
        assertEquals(List.of("Second", "First"), AtlasSearch.search(catalog, state).stream().map(AtlasSearch.Entry::name).toList());
        var lore = new AtlasState(false, "", "", "", "0", "121", "filters", false, 0, "",
                    state.filters(), state.excluded(), List.of(new AtlasState.StringFilter("lore", "ancient"),
                    new AtlasState.StringFilter("majorId", "special effect")));
        assertEquals("First", AtlasSearch.search(catalog, lore).getFirst().name());
        for (String bound : List.of("NaN", "Infinity", "--", "hello")) {
            var invalid = new AtlasState(false, "", "", "", "", "", "filters", false, 0, "",
                    List.of(new AtlasState.NumericFilter("manaRegen", bound, "", false)), List.of(), List.of());
            assertThrows(IllegalArgumentException.class, () -> AtlasSearch.search(catalog, invalid));
        }
        assertEquals(3, AtlasSearch.search(catalog, allTypes(false)).size());
    }

    @Test
    void ingredientNumericFiltersUseMaximumRollsAndSeparateDurationFromDurability() {
        var catalog = AtlasSearch.ingredients(CraftedItemCodec.ingredientChoices());
        assertTrue(AtlasSearch.numericKeys(catalog).containsAll(List.of("durability", "duration", "effectiveness touching")));
        var state = new AtlasState(true, "", "", "", "", "", "filters", false, 0, "",
                List.of(new AtlasState.NumericFilter("effectiveness touching", "1", "", true)), List.of(), List.of());
        var results = AtlasSearch.search(catalog, state);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(entry -> entry.stats().get("effectiveness touching") >= 1));
        assertTrue(results.getFirst().stats().get("effectiveness touching")
                >= results.getLast().stats().get("effectiveness touching"));
    }
}
