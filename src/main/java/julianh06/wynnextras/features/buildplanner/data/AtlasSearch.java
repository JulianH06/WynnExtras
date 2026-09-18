package julianh06.wynnextras.features.buildplanner.data;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;

public final class AtlasSearch {
    private static final java.util.regex.Pattern SPACES = java.util.regex.Pattern.compile("[\\s_]+");
    private AtlasSearch() {}

    public static List<Entry> items(List<WynnItem> items) {
        return items.stream().filter(item -> Set.of("weapon", "armour", "accessory").contains(item.type()))
                .map(item -> new Entry("item:" + item.reference(), item.displayName(), item.stat("lvl"),
                        item.subType(), item.tier().toLowerCase(Locale.ROOT),
                        item.displayName() + " " + String.join(" ", item.identifications().keySet())
                                + " " + String.join(" ", item.majorIds().keySet()),
                        item, null, itemStats(item))).toList();
    }

    public static List<Entry> ingredients(List<CraftedItemCodec.Ingredient> ingredients) {
        return ingredients.stream().filter(ingredient -> ingredient.id() != 4000)
                .filter(ingredient -> !CraftedItemCodec.isPowderIngredient(ingredient.id()))
                .map(ingredient -> new Entry("ingredient:" + ingredient.id(), ingredient.name(), ingredient.level(),
                        String.join(",", ingredient.professions()), Integer.toString(ingredient.tier()),
                        ingredient.name() + " " + String.join(" ", ingredient.details()),
                        null, ingredient, CraftedItemCodec.ingredientStats(ingredient.id()))).toList();
    }

    public static List<Entry> search(List<Entry> catalog, AtlasState state) {
        var ranges = state.filters().stream().map(filter -> {
            double min = bound(filter.minimum(), Double.NEGATIVE_INFINITY);
            double max = bound(filter.maximum(), Double.POSITIVE_INFINITY);
            if (min > max) throw new IllegalArgumentException(label(filter.key()) + ": minimum must not exceed maximum.");
            return new Range(filter.key(), min, max);
        }).toList();
        for (var filter : state.filters()) {
            if (!knownKey(catalog, filter.key())) throw new IllegalArgumentException("Unknown numeric filter: " + filter.key());
        }
        for (String key : state.excluded()) {
            if (!knownKey(catalog, key)) throw new IllegalArgumentException("Unknown excluded filter: " + key);
        }
        for (var filter : state.strings()) {
            if (!List.of("name", "lore", "majorId", "restriction", "attackSpeed").contains(filter.key())) {
                throw new IllegalArgumentException("Unknown string filter: " + filter.key());
            }
        }
        String query = normalized(state.query());
        Comparator<Entry> order = switch (state.sort()) {
            case "name", "filters" -> Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER);
            case "level" -> Comparator.comparingInt(Entry::level);
            case "rarity" -> Comparator.comparingInt(entry -> rarityRank(entry.rarity()));
            default -> throw new IllegalArgumentException("Unknown Atlas sort order: " + state.sort());
        };
        if (state.descending()) order = order.reversed();
        // Numeric rows are ordered sort priorities, with independent directions.
        Comparator<Entry> priority = null;
        for (var filter : state.sort().equals("filters") ? state.filters() : List.<AtlasState.NumericFilter>of()) {
            Comparator<Entry> next = Comparator.comparingDouble(entry -> entry.stats().getOrDefault(filter.key(), 0D));
            if (filter.descending()) next = next.reversed();
            priority = priority == null ? next : priority.thenComparing(next);
        }
        if (priority != null) order = priority.thenComparing(order);
        return catalog.stream()
                .filter(entry -> (entry.ingredient() != null) == state.ingredients())
                .filter(entry -> matchesSelection(state.category(), entry.category()))
                .filter(entry -> (!state.ingredients() && state.rarity().equals("~none"))
                        || matchesSelection(state.rarity(), entry.rarity()))
                .filter(entry -> ranges.stream().allMatch(range -> range.matches(entry)))
                .filter(entry -> state.excluded().stream().allMatch(key -> entry.stats().getOrDefault(key, 0D) == 0))
                .filter(entry -> state.strings().stream().allMatch(filter ->
                        normalized(stringValue(entry, filter.key())).contains(normalized(filter.value()))))
                .filter(entry -> query.isEmpty() || entry.searchText().contains(query))
                .sorted(order.thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public static boolean matchesSelection(String selection, String values) {
        if (selection.isEmpty()) return true;
        var selected = List.of(selection.split(","));
        return java.util.Arrays.stream(values.split(",")).anyMatch(selected::contains);
    }

    public static List<String> numericKeys(List<Entry> catalog) {
        var keys = new java.util.TreeSet<String>();
        keys.add("lvl");
        catalog.forEach(entry -> keys.addAll(entry.stats().keySet()));
        return keys.stream().sorted(Comparator.comparing(AtlasSearch::label)).toList();
    }

    private static boolean knownKey(List<Entry> catalog, String key) {
        return key.equals("lvl") || catalog.stream().anyMatch(entry -> entry.stats().containsKey(key));
    }

    public static String label(String key) {
        return switch (key) {
            case "lvl" -> "Combat Level";
            case "hp" -> "Health";
            case "strReq" -> "Strength Requirement";
            case "dexReq" -> "Dexterity Requirement";
            case "intReq" -> "Intelligence Requirement";
            case "defReq" -> "Defense Requirement";
            case "agiReq" -> "Agility Requirement";
            default -> ItemInspection.readable(key);
        };
    }

    private static Map<String, Double> itemStats(WynnItem item) {
        Map<String, Double> stats = new LinkedHashMap<>();
        item.baseStats().forEach((key, value) -> stats.put(key, value.doubleValue()));
        item.identifications().forEach((key, value) -> stats.put(key, (double) value.max()));
        stats.put("lvl", (double) item.stat("lvl"));
        return Map.copyOf(stats);
    }

    private static String stringValue(Entry entry, String key) {
        if (key.equals("name")) return entry.name();
        var item = entry.item();
        if (item == null) return "";
        return switch (key) {
            case "lore" -> item.lore();
            case "majorId" -> String.join(" ", item.majorIds().keySet()) + " " + String.join(" ", item.majorIds().values());
            case "restriction" -> item.restriction();
            case "attackSpeed" -> item.attackSpeed();
            default -> throw new IllegalArgumentException("Unknown string filter: " + key);
        };
    }

    private static double bound(String input, double empty) {
        if (input.isBlank()) return empty;
        try {
            double value = Double.parseDouble(input);
            if (!Double.isFinite(value)) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a number for filter bounds, or leave them blank for no limit.");
        }
    }

    private record Range(String key, double min, double max) {
        boolean matches(Entry entry) {
            double value = entry.stats().getOrDefault(key, 0D);
            return value >= min && value <= max;
        }
    }

    private static String normalized(String text) {
        return SPACES.matcher(text.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private static int rarityRank(String rarity) {
        return switch (rarity) {
            case "0", "normal" -> 0;
            case "1", "unique" -> 1;
            case "2", "rare" -> 2;
            case "3", "legendary" -> 3;
            case "set" -> 4;
            case "fabled" -> 5;
            case "mythic" -> 6;
            default -> 7;
        };
    }

    public record Entry(String id, String name, int level, String category, String rarity, String searchText,
            WynnItem item, CraftedItemCodec.Ingredient ingredient, Map<String, Double> stats) {
        public Entry { searchText = normalized(searchText); stats = Map.copyOf(stats); }
    }
}
