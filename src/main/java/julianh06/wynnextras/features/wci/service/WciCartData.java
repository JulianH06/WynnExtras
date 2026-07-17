package julianh06.wynnextras.features.wci.service;

import julianh06.wynnextras.features.wci.cart.ShoppingCart;
import julianh06.wynnextras.features.wci.cart.ShoppingEntry;
import julianh06.wynnextras.features.wci.model.RequirementType;

import java.util.ArrayList;
import java.util.List;

public class WciCartData {
    private static final int CURRENT_SCHEMA_VERSION = 1;

    private int schemaVersion = CURRENT_SCHEMA_VERSION;
    private List<EntryData> entries = new ArrayList<>();

    public static WciCartData fromCart(ShoppingCart cart) {
        WciCartData data = new WciCartData();
        if (cart == null) {
            return data;
        }

        for (var entry : cart.entries().entrySet()) {
            data.entries.add(EntryData.from(entry.getKey(), entry.getValue()));
        }
        return data;
    }

    public ShoppingCart toCart() {
        ShoppingCart cart = new ShoppingCart();
        if (entries == null) {
            return cart;
        }

        for (EntryData entry : entries) {
            if (entry == null) {
                continue;
            }
            cart.add(entry.toShoppingEntry(), entry.requiredAmount());
        }
        return cart;
    }

    public int schemaVersion() {
        return schemaVersion <= 0 ? CURRENT_SCHEMA_VERSION : schemaVersion;
    }

    public int version() {
        return schemaVersion();
    }

    public List<EntryData> entries() {
        return entries == null ? List.of() : List.copyOf(entries);
    }

    public static class EntryData {
        private String id;
        private String displayName;
        private RequirementType type;
        private int materialTier;
        private String source;
        private int requiredAmount;

        private EntryData() {}

        private EntryData(
                String id,
                String displayName,
                RequirementType type,
                int materialTier,
                String source,
                int requiredAmount) {
            this.id = id;
            this.displayName = displayName;
            this.type = type;
            this.materialTier = materialTier;
            this.source = source;
            this.requiredAmount = requiredAmount;
        }

        static EntryData from(ShoppingEntry entry, int requiredAmount) {
            return new EntryData(
                    entry.id(),
                    entry.displayName(),
                    entry.type(),
                    entry.materialTier(),
                    entry.source(),
                    Math.max(0, requiredAmount));
        }

        ShoppingEntry toShoppingEntry() {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("WCI cart entry id is blank");
            }
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("WCI cart entry display name is blank");
            }
            return new ShoppingEntry(id, displayName, type, materialTier, source);
        }

        int requiredAmount() {
            if (requiredAmount <= 0) {
                throw new IllegalArgumentException("WCI cart entry required amount must be positive");
            }
            return requiredAmount;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public RequirementType type() {
            return type;
        }

        public int materialTier() {
            return materialTier;
        }

        public String source() {
            return source;
        }

        public int amount() {
            return requiredAmount;
        }
    }
}
