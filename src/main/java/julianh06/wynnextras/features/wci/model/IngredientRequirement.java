package julianh06.wynnextras.features.wci.model;

import julianh06.wynnextras.features.wci.util.IngredientNormalizer;

import java.util.Objects;

public final class IngredientRequirement {
    private final RequirementType type;
    private final String id;
    private final String displayName;
    private final int amount;
    private final String source;
    private final int materialTier;

    public IngredientRequirement(String name, int amount, RequirementType type) {
        this(type, IngredientNormalizer.key(name), name, amount, "WynnBuilder", 0);
    }

    public IngredientRequirement(String name, int amount, RequirementType type, int materialTier, String source) {
        this(type, IngredientNormalizer.key(name), name, amount, source, materialTier);
    }

    public IngredientRequirement(
            RequirementType type,
            String id,
            String displayName,
            int amount,
            String source,
            int materialTier) {
        this.type = type == null ? RequirementType.INGREDIENT : type;
        this.id = Objects.requireNonNull(id, "id").trim();
        this.displayName = Objects.requireNonNull(displayName, "displayName").trim();
        this.amount = amount;
        this.source = source == null || source.isBlank() ? "WynnBuilder" : source.trim();
        this.materialTier = this.type == RequirementType.MATERIAL ? Math.max(1, materialTier) : 0;
        if (this.id.isBlank()) throw new IllegalArgumentException("Requirement id cannot be blank");
        if (this.displayName.isBlank()) throw new IllegalArgumentException("Requirement name cannot be blank");
        if (this.amount <= 0) throw new IllegalArgumentException("Requirement amount must be positive");
    }

    public static IngredientRequirement material(
            String id,
            String displayName,
            int amount,
            String source,
            int materialTier) {
        return new IngredientRequirement(RequirementType.MATERIAL, id, displayName, amount, source, materialTier);
    }

    public RequirementType type() {
        return type;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String name() {
        return displayName;
    }

    public int amount() {
        return amount;
    }

    public String source() {
        return source;
    }

    public int materialTier() {
        return materialTier;
    }

    public int tier() {
        return materialTier;
    }

    public boolean isIngredient() {
        return type == RequirementType.INGREDIENT;
    }

    public boolean isMaterial() {
        return type == RequirementType.MATERIAL;
    }
}
