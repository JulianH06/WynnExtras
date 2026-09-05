package julianh06.wynnextras.features.shoppinglist.service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ShoppingListTradeMarketSlotMatcher {
    private static final Set<String> SEARCH_FILTER_LABELS = Set.of(
            "search",
            "filter",
            "search and filter",
            "search filter"
    );
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private ShoppingListTradeMarketSlotMatcher() {}

    public static boolean isSearchFilterLabel(String displayName) {
        String label = normalizedLabel(displayName);
        return SEARCH_FILTER_LABELS.contains(label);
    }

    static String normalizedLabel(String displayName) {
        String cleaned = ShoppingListTextCleaner.clean(displayName).toLowerCase(Locale.ROOT);
        cleaned = cleaned.replace("&", " and ");
        cleaned = cleaned.replace("+", " and ");
        cleaned = cleaned.replace("/", " ");
        cleaned = NON_ALPHANUMERIC.matcher(cleaned).replaceAll(" ");
        cleaned = WHITESPACE.matcher(cleaned).replaceAll(" ").trim();
        return cleaned;
    }
}
