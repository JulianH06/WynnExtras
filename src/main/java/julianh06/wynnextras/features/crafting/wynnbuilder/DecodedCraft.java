package julianh06.wynnextras.features.crafting.wynnbuilder;

public record DecodedCraft(
        int[] ingredientIds,
        int recipeId,
        int mat1Tier,
        int mat2Tier
) {}
