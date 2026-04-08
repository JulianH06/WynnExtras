package julianh06.wynnextras.features.crafting.data.recipes.weaponsmithing;

import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.materials.Ingot;
import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.materials.Plank;
import julianh06.wynnextras.utils.Pair;

import java.util.List;

public class SpearRecipes implements IRecipeData {
    public static SpearRecipes INSTANCE = new SpearRecipes();

    public List<Pair<IMaterial, Integer>> getMaterials(int level) {
        if(level < 10) {
            return List.of(new Pair<>(Plank.OAK, 2), new Pair<>(Ingot.COPPER, 1));
        } else if (level < 20) {
            return List.of(new Pair<>(Plank.BIRCH, 4), new Pair<>(Ingot.GRANITE, 2));
        } else if (level < 30) {
            return List.of(new Pair<>(Plank.WILLOW, 4), new Pair<>(Ingot.GOLD, 2));
        } else if (level < 40) {
            return List.of(new Pair<>(Plank.ACACIA, 6), new Pair<>(Ingot.SANDSTONE, 3));
        } else if (level < 50) {
            return List.of(new Pair<>(Plank.SPRUCE, 6), new Pair<>(Ingot.IRON, 3));
        } else if (level < 60) {
            return List.of(new Pair<>(Plank.JUNGLE, 8), new Pair<>(Ingot.SILVER, 4));
        } else if (level < 70) {
            return List.of(new Pair<>(Plank.DARK, 8), new Pair<>(Ingot.COBALT, 4));
        } else if (level < 80) {
            return List.of(new Pair<>(Plank.LIGHT, 10), new Pair<>(Ingot.KANDERSTONE, 5));
        } else if (level < 90) {
            return List.of(new Pair<>(Plank.PINE, 10), new Pair<>(Ingot.DIAMOND, 5));
        } else if (level < 100) {
            return List.of(new Pair<>(Plank.AVO, 12), new Pair<>(Ingot.MOLTEN, 6));
        } else if (level < 103) {
            return List.of(new Pair<>(Plank.SKY, 12), new Pair<>(Ingot.VOIDSTONE, 6));
        } else if (level < 106) {
            return List.of(new Pair<>(Plank.DERNIC, 12), new Pair<>(Ingot.DERNIC, 6));
        } else {
            return List.of();
        }
    }
}