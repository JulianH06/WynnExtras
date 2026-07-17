package julianh06.wynnextras.features.wci.model;

import java.util.List;

public record WynnBuilderBuild(String sourceUrl, int craftedItemCount, List<IngredientRequirement> requirements) {
    public WynnBuilderBuild(String sourceUrl, List<IngredientRequirement> requirements) {
        this(sourceUrl, requirements.isEmpty() ? 0 : 1, requirements);
    }

    public WynnBuilderBuild { requirements = List.copyOf(requirements); }

    public List<IngredientRequirement> ingredients() {
        return requirements;
    }

    public boolean isValid() {
        return sourceUrl != null && !sourceUrl.isBlank() && !requirements.isEmpty();
    }

    public int ingredientCount() {
        return requirements.stream()
                .filter(IngredientRequirement::isIngredient)
                .mapToInt(IngredientRequirement::amount)
                .sum();
    }

    public int materialCount() {
        return requirements.stream()
                .filter(IngredientRequirement::isMaterial)
                .mapToInt(IngredientRequirement::amount)
                .sum();
    }
}
