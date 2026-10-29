package julianh06.wynnextras.features.crafting.wynnbuilder;

import julianh06.wynnextras.features.crafting.data.WynnDataService;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderBuildCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public final class WynnBuilderBuildDecoder {
    private WynnBuilderBuildDecoder() {}

    public static boolean isBuildHash(String payload) {
        return WynnBuilderBuildCodec.isBuildHash(payload);
    }

    public static List<DecodedCraft> decode(String payload, WynnDataService dataService) {
        String hash = normalize(payload);
        if (!isBuildHash(hash)) return List.of();
        if (dataService == null || dataService.getState() != WynnDataService.State.READY) {
            throw new IllegalStateException("Crafting data is not ready");
        }
        return decode(hash, recipeId -> {
            WynnDataService.RecipeData recipe = dataService.getRecipeByWynnBuilderId(recipeId);
            return recipe == null ? null : recipe.type().isWeapon();
        });
    }

    static List<DecodedCraft> decode(String hash, IntFunction<Boolean> weaponRecipeLookup) {
        try {
            return WynnBuilderBuildCodec.craftedItems(hash, weaponRecipeLookup).stream()
                    .map(craft -> new DecodedCraft(craft.ingredientIds(), craft.recipeId(),
                            craft.material1Tier(), craft.material2Tier(), craft.attackSpeed()))
                    .toList();
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unable to decode WynnBuilder build: " + ex.getMessage(), ex);
        }
    }

    private static String normalize(String payload) {
        return julianh06.wynnextras.wynncraft.codec.WynnBuilderEncoding.hash(payload);
    }
}
