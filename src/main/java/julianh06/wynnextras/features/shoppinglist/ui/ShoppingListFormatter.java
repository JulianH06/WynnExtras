package julianh06.wynnextras.features.shoppinglist.ui;

import julianh06.wynnextras.features.shoppinglist.cart.ShoppingCart;
import julianh06.wynnextras.features.shoppinglist.cart.ShoppingEntry;
import julianh06.wynnextras.features.shoppinglist.model.RequirementType;
import julianh06.wynnextras.features.shoppinglist.util.ShoppingListMaterialNameNormalizer;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

public final class ShoppingListFormatter {
    private static final Comparator<Row> ROW_COMPARATOR = Comparator
            .comparingInt((Row row) -> row.type() == RequirementType.MATERIAL ? 0 : 1)
            .thenComparing(Comparator.comparingInt(Row::needCount).reversed())
            .thenComparing(Row::displayNameWithTier, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Row::displayNameWithTier);

    private ShoppingListFormatter() {}

    @FunctionalInterface
    public interface RequiredCountProvider {
        int required(ShoppingEntry entry, int baseRequired);
    }

    public static List<Row> rows(ShoppingCart cart) {
        return rows(cart, entry -> 0);
    }

    public static List<Row> rows(ShoppingCart cart, ToIntFunction<ShoppingEntry> haveCountProvider) {
        return rows(cart, haveCountProvider, (entry, baseRequired) -> baseRequired);
    }

    public static List<Row> rows(
            ShoppingCart cart,
            ToIntFunction<ShoppingEntry> haveCountProvider,
            RequiredCountProvider requiredCountProvider) {
        if (cart == null) return List.of();
        ToIntFunction<ShoppingEntry> provider = haveCountProvider == null ? entry -> 0 : haveCountProvider;
        RequiredCountProvider requiredProvider = requiredCountProvider == null
                ? (entry, baseRequired) -> baseRequired
                : requiredCountProvider;
        return cart.entries().entrySet().stream()
                .map(entry -> Row.from(
                        entry.getKey(),
                        Math.max(0, requiredProvider.required(entry.getKey(), entry.getValue())),
                        safeHaveCount(provider, entry.getKey())))
                .sorted(ROW_COMPARATOR)
                .toList();
    }

    static int compareRows(Row first, Row second) {
        return ROW_COMPARATOR.compare(first, second);
    }

    private static int safeHaveCount(ToIntFunction<ShoppingEntry> provider, ShoppingEntry entry) {
        return Math.max(0, provider.applyAsInt(entry));
    }

    public static String format(ShoppingCart cart) {
        List<Row> rows = rows(cart);
        StringBuilder out = new StringBuilder("Shopping List:");
        if (rows.isEmpty()) {
            return out.append(System.lineSeparator()).append("- Empty").toString();
        }

        for (Row row : rows) {
            out.append(System.lineSeparator())
                    .append("- ")
                    .append(row.typeLabel())
                    .append(' ')
                    .append(row.displayNameWithTier());
            out.append(" x").append(row.needCount());
        }
        return out.toString();
    }

    private static String tradeMarketQuery(ShoppingEntry entry) {
        String displayName = entry.displayName();
        if (entry.type() != RequirementType.MATERIAL) {
            return displayName;
        }
        return materialTradeMarketQuery(displayName);
    }

    static String materialTradeMarketQuery(String displayName) {
        return ShoppingListMaterialNameNormalizer.tradeMarketQuery(displayName);
    }

    public record Row(
            RequirementType type,
            String typeLabel,
            String displayName,
            int haveCount,
            int needCount,
            int materialTier,
            String tradeMarketQuery) {
        static Row from(ShoppingEntry entry, int required, int haveCount) {
            RequirementType type = entry.type();
            String displayName = ShoppingListFormatter.displayName(entry);
            return new Row(
                    type,
                    type == RequirementType.MATERIAL ? "[Mat]" : "[Ing]",
                    displayName,
                    Math.max(0, haveCount),
                    Math.max(0, required),
                    type == RequirementType.MATERIAL ? entry.materialTier() : 0,
                    ShoppingListFormatter.tradeMarketQuery(entry));
        }

        public String displayNameWithTier() {
            return displayName + (materialTier > 0 ? " T" + materialTier : "");
        }
    }

    private static String displayName(ShoppingEntry entry) {
        if (entry.type() != RequirementType.MATERIAL) {
            return entry.displayName();
        }
        return ShoppingListMaterialNameNormalizer.baseName(entry.displayName());
    }
}
