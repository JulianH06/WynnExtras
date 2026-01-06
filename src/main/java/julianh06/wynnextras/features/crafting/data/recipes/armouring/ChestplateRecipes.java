package julianh06.wynnextras.features.crafting.data.recipes.armouring;

import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.materials.Ingot;
import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.materials.Paper;
import julianh06.wynnextras.utils.Pair;

import java.util.List;

public class ChestplateRecipes implements IRecipeData {
    public static ChestplateRecipes INSTANCE = new ChestplateRecipes();

    public List<Pair<IMaterial, Integer>> getMaterials(int level) {
        if(level < 10) {
            return List.of(new Pair<>(Paper.OAK, 1), new Pair<>(Ingot.COPPER, 2));
        } else if (level < 20) {
            return List.of(new Pair<>(Paper.BIRCH, 2), new Pair<>(Ingot.GRANITE, 4));
        } else if (level < 30) {
            return List.of(new Pair<>(Paper.WILLOW, 2), new Pair<>(Ingot.GOLD, 4));
        } else if (level < 40) {
            return List.of(new Pair<>(Paper.ACACIA, 3), new Pair<>(Ingot.SANDSTONE, 6));
        } else if (level < 50) {
            return List.of(new Pair<>(Paper.SPRUCE, 3), new Pair<>(Ingot.IRON, 6));
        } else if (level < 60) {
            return List.of(new Pair<>(Paper.JUNGLE, 4), new Pair<>(Ingot.SILVER, 8));
        } else if (level < 70) {
            return List.of(new Pair<>(Paper.DARK, 4), new Pair<>(Ingot.COBALT, 8));
        } else if (level < 80) {
            return List.of(new Pair<>(Paper.LIGHT, 5), new Pair<>(Ingot.KANDERSTONE, 10));
        } else if (level < 90) {
            return List.of(new Pair<>(Paper.PINE, 5), new Pair<>(Ingot.DIAMOND, 10));
        } else if (level < 100) {
            return List.of(new Pair<>(Paper.AVO, 6), new Pair<>(Ingot.MOLTEN, 12));
        } else if (level < 104) {
            return List.of(new Pair<>(Paper.SKY, 6), new Pair<>(Ingot.VOIDSTONE, 12));
        } else if (level < 106) {
            return List.of(new Pair<>(Paper.DERNIC, 6), new Pair<>(Ingot.DERNIC, 12));
        } else {
            return List.of();
        }
    }
}