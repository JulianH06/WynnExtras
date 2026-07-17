package julianh06.wynnextras.features.wci.service.wynnbuilder;

import julianh06.wynnextras.features.wci.model.IngredientRequirement;
import julianh06.wynnextras.features.wci.model.RequirementType;
import julianh06.wynnextras.features.wci.util.IngredientNormalizer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class WynnBuilderCraftParser {
    public static final String B64 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz+-";
    private static final int NO_INGREDIENT_ID = 4000;

    private final WynnBuilderIngredientRegistry ingredientRegistry;
    private final WynnBuilderRecipeRegistry recipeRegistry;

    public WynnBuilderCraftParser() {
        this(new WynnBuilderIngredientRegistry(), new WynnBuilderRecipeRegistry());
    }

    WynnBuilderCraftParser(
            WynnBuilderIngredientRegistry ingredientRegistry,
            WynnBuilderRecipeRegistry recipeRegistry) {
        this.ingredientRegistry = ingredientRegistry;
        this.recipeRegistry = recipeRegistry;
    }

    public Optional<List<IngredientRequirement>> parse(String payload) {
        String hash = payload == null ? "" : payload.trim();
        if (hash.contains("#")) hash = hash.substring(hash.lastIndexOf('#') + 1);
        if (hash.startsWith("CR-")) hash = hash.substring(3);
        if (hash.contains("CR-")) hash = hash.substring(hash.indexOf("CR-") + 3);
        if (hash.isBlank() || hash.chars().anyMatch(c -> B64.indexOf(c) < 0)) return Optional.empty();
        try {
            DecodedCraft craft = hash.charAt(0) == '1' && hash.length() >= 16
                    ? decodeLegacy(hash)
                    : decodeBitPacked(hash);
            return requirements(craft);
        } catch (IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    public static boolean isNoIngredient(int id) {
        return id == 0 || id == NO_INGREDIENT_ID;
    }

    private DecodedCraft decodeLegacy(String hash) {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ids.add(readLegacyPair(hash, 1 + i * 2));
        }
        int recipe = readLegacyPair(hash, 13);
        int tierCombo = B64.indexOf(hash.charAt(15));
        if (tierCombo < 1 || tierCombo > 9) {
            throw new IllegalArgumentException("Unsupported WynnBuilder legacy material tier combo: " + tierCombo);
        }
        int mat1Tier = tierCombo % 3 == 0 ? 3 : tierCombo % 3;
        int mat2Tier = (int) Math.floor((tierCombo - 0.5) / 3.0) + 1;
        return new DecodedCraft(ids, recipe, mat1Tier, mat2Tier);
    }

    private int readLegacyPair(String hash, int index) {
        return B64.indexOf(hash.charAt(index)) * 64 + B64.indexOf(hash.charAt(index + 1));
    }

    private DecodedCraft decodeBitPacked(String hash) {
        return decodeBitPacked(new WynnBuilderBitCursor(hash), false);
    }

    DecodedCraft decodeEmbedded(WynnBuilderBitCursor cursor) {
        return decodeBitPacked(cursor, true);
    }

    private DecodedCraft decodeBitPacked(WynnBuilderBitCursor cursor, boolean embedded) {
        int start = cursor.bitIndex();
        if (cursor.read(1) != 0) {
            if (embedded) {
                throw new IllegalArgumentException("Unsupported legacy WynnBuilder craft embedded in build hash");
            }
            return null;
        }
        int version = cursor.read(7);
        if (version != 2) return null;
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < 6; i++) ids.add(cursor.read(12));
        int recipe = cursor.read(12);
        int mat1Tier = cursor.read(3) + 1;
        int mat2Tier = cursor.read(3) + 1;
        if (embedded) {
            WynnBuilderRecipeRegistry.Recipe recipeData = recipeRegistry.recipe(recipe);
            if (recipeData == null) {
                throw new IllegalArgumentException(staleRecipeRegistryMessage(recipe, true));
            }
            if (isWeaponRecipe(recipeData)) {
                cursor.skip(4);
            }
            int remainder = (cursor.bitIndex() - start) % 6;
            cursor.skip(remainder == 0 ? 6 : 6 - remainder);
        }
        return new DecodedCraft(ids, recipe, mat1Tier, mat2Tier);
    }

    Optional<List<IngredientRequirement>> requirements(DecodedCraft craft) {
        if (craft == null) return Optional.empty();
        WynnBuilderRecipeRegistry.Recipe recipe = recipeRegistry.recipe(craft.recipeId());
        if (recipe == null) {
            throw new IllegalArgumentException(staleRecipeRegistryMessage(craft.recipeId(), false));
        }

        String source = recipe.label();
        List<IngredientRequirement> out = new ArrayList<>();
        Set<Integer> unknownIds = new LinkedHashSet<>();
        for (int id : craft.ingredientIds()) {
            if (isNoIngredient(id)) continue;
            String name = ingredientRegistry.displayName(id);
            if (name == null) {
                unknownIds.add(id);
                continue;
            }
            out.add(new IngredientRequirement(
                    RequirementType.INGREDIENT,
                    IngredientNormalizer.key(name),
                    name,
                    1,
                    source,
                    0));
        }
        if (!unknownIds.isEmpty()) {
            throw new IllegalArgumentException("Unsupported WynnBuilder ingredient ids in crafted hash: " + unknownIds);
        }
        for (WynnBuilderRecipeRegistry.RecipeMaterial material : recipe.materials()) {
            int tier = material.slot() == 0 ? craft.mat1Tier() : craft.mat2Tier();
            out.add(IngredientRequirement.material(
                    IngredientNormalizer.key(material.item()),
                    material.item(),
                    material.amount(),
                    source,
                    tier));
        }
        return out.isEmpty() ? Optional.empty() : Optional.of(out);
    }

    private boolean isWeaponRecipe(WynnBuilderRecipeRegistry.Recipe recipe) {
        String type = recipe.type().toUpperCase();
        return type.equals("BOW")
                || type.equals("DAGGER")
                || type.equals("SPEAR")
                || type.equals("WAND")
                || type.equals("RELIK");
    }

    private static String staleRecipeRegistryMessage(int recipeId, boolean embedded) {
        return "Unsupported WynnBuilder recipe id in " + (embedded ? "embedded craft" : "crafted hash") + ": "
                + recipeId
                + ". The bundled WynnBuilder recipe registry may be out of date.";
    }

    record DecodedCraft(List<Integer> ingredientIds, int recipeId, int mat1Tier, int mat2Tier) {}
}
