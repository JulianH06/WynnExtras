package julianh06.wynnextras.features.buildplanner.data;

import java.util.List;

public record AtlasState(boolean ingredients, String query, String category, String rarity,
        String minLevel, String maxLevel, String sort, boolean descending, int page, String selected,
        List<NumericFilter> filters, List<String> excluded, List<StringFilter> strings) {
    public AtlasState {
        query = query == null ? "" : query;
        category = category == null ? "" : category;
        rarity = rarity == null ? "" : rarity;
        minLevel = minLevel == null ? "0" : minLevel;
        maxLevel = maxLevel == null ? "121" : maxLevel;
        sort = sort == null ? "name" : sort;
        selected = selected == null ? "" : selected;
        page = Math.max(0, page);
        // Older tabs stored only a level interval and one type/tier.
        filters = filters == null ? List.of(new NumericFilter("lvl", minLevel, maxLevel, false)) : List.copyOf(filters);
        excluded = excluded == null ? List.of() : List.copyOf(excluded);
        strings = strings == null ? List.of() : List.copyOf(strings);
    }

    public AtlasState(boolean ingredients, String query, String category, String rarity,
            String minLevel, String maxLevel, String sort, boolean descending, int page, String selected) {
        this(ingredients, query, category, rarity, minLevel, maxLevel, sort, descending, page, selected, null, null, null);
    }

    public static AtlasState defaults() {
        return defaults(false);
    }

    public static AtlasState defaults(boolean ingredients) {
        return new AtlasState(ingredients, "", "~none", "", "0", "121", "filters", false, 0, "",
                List.of(), List.of(), List.of());
    }

    public AtlasState withPage(int page, String selected) {
        return new AtlasState(ingredients, query, category, rarity, minLevel, maxLevel, sort, descending,
                page, selected, filters, excluded, strings);
    }

    public record NumericFilter(String key, String minimum, String maximum, boolean descending) {
        public NumericFilter(String key) {
            this(key, "", "", true);
        }

        public NumericFilter {
            if (key == null || key.isBlank()) throw new IllegalArgumentException("Missing numeric filter");
            minimum = minimum == null ? "" : minimum;
            maximum = maximum == null ? "" : maximum;
        }
    }

    public record StringFilter(String key, String value) {
        public StringFilter {
            if (key == null || key.isBlank()) throw new IllegalArgumentException("Missing string filter");
            value = value == null ? "" : value;
        }
    }
}
