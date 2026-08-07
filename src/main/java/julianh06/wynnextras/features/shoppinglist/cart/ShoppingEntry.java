package julianh06.wynnextras.features.shoppinglist.cart;

import julianh06.wynnextras.features.shoppinglist.model.RequirementType;
import julianh06.wynnextras.features.shoppinglist.util.IngredientNormalizer;

import java.util.Objects;

public final class ShoppingEntry {
    private final RequirementType type;
    private final String id;
    private final String displayName;
    private final int materialTier;
    private final String source;

    public ShoppingEntry(String name, RequirementType type) {
        this(IngredientNormalizer.key(name), name, type, 0, null);
    }

    public ShoppingEntry(String name, RequirementType type, int materialTier, String source) {
        this(IngredientNormalizer.key(name), name, type, materialTier, source);
    }

    public ShoppingEntry(String id, String displayName, RequirementType type, int materialTier, String source) {
        this.type = type == null ? RequirementType.INGREDIENT : type;
        this.id = Objects.requireNonNull(id, "id").trim();
        this.displayName = Objects.requireNonNull(displayName, "displayName").trim();
        this.materialTier = this.type == RequirementType.MATERIAL ? Math.max(1, materialTier) : 0;
        this.source = source == null || source.isBlank() ? null : source.trim();
        if (this.id.isBlank()) throw new IllegalArgumentException("Shopping entry id cannot be blank");
        if (this.displayName.isBlank()) throw new IllegalArgumentException("Shopping entry name cannot be blank");
    }

    public RequirementType type() {
        return type;
    }

    public String id() {
        return id;
    }

    public String name() {
        return displayName;
    }

    public String displayName() {
        return displayName;
    }

    public int tier() {
        return materialTier;
    }

    public int materialTier() {
        return materialTier;
    }

    public String source() {
        return source;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ShoppingEntry that)) return false;
        return materialTier == that.materialTier && type == that.type && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id, materialTier);
    }

    @Override
    public String toString() {
        return type + ":" + id + (type == RequirementType.MATERIAL ? ":tier_" + materialTier : "");
    }
}
