package julianh06.wynnextras.wynncraft.codec;

public record WynnBuilderCraft(
        int[] ingredientIds,
        int recipeId,
        int material1Tier,
        int material2Tier,
        int attackSpeed
) {
    public WynnBuilderCraft {
        ingredientIds = ingredientIds.clone();
    }

    @Override public int[] ingredientIds() { return ingredientIds.clone(); }

    @Override
    public boolean equals(Object other) {
        return other instanceof WynnBuilderCraft craft
                && java.util.Arrays.equals(ingredientIds, craft.ingredientIds)
                && recipeId == craft.recipeId && material1Tier == craft.material1Tier
                && material2Tier == craft.material2Tier && attackSpeed == craft.attackSpeed;
    }

    @Override
    public int hashCode() {
        int result = java.util.Arrays.hashCode(ingredientIds);
        result = 31 * result + recipeId;
        result = 31 * result + material1Tier;
        result = 31 * result + material2Tier;
        return 31 * result + attackSpeed;
    }
}
