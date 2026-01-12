package julianh06.wynnextras.features.crafting.data.recipes.weaponsmithing;

import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.materials.Ingot;
import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.materials.Wood;
import julianh06.wynnextras.utils.Pair;

import java.util.List;

public class DaggerRecipes implements IRecipeData {
    public static DaggerRecipes INSTANCE = new DaggerRecipes();

    public List<Pair<IMaterial, Integer>> getMaterials(int level) {
        if(level < 10) {
            return List.of(new Pair<>(Wood.OAK, 1), new Pair<>(Ingot.COPPER, 2));
        } else if (level < 20) {
            return List.of(new Pair<>(Wood.BIRCH, 2), new Pair<>(Ingot.GRANITE, 4));
        } else if (level < 30) {
            return List.of(new Pair<>(Wood.WILLOW, 2), new Pair<>(Ingot.GOLD, 4));
        } else if (level < 40) {
            return List.of(new Pair<>(Wood.ACACIA, 3), new Pair<>(Ingot.SANDSTONE, 6));
        } else if (level < 50) {
            return List.of(new Pair<>(Wood.SPRUCE, 3), new Pair<>(Ingot.IRON, 6));
        } else if (level < 60) {
            return List.of(new Pair<>(Wood.JUNGLE, 4), new Pair<>(Ingot.SILVER, 8));
        } else if (level < 70) {
            return List.of(new Pair<>(Wood.DARK, 4), new Pair<>(Ingot.COBALT, 8));
        } else if (level < 80) {
            return List.of(new Pair<>(Wood.LIGHT, 5), new Pair<>(Ingot.KANDERSTONE, 10));
        } else if (level < 90) {
            return List.of(new Pair<>(Wood.PINE, 5), new Pair<>(Ingot.DIAMOND, 10));
        } else if (level < 100) {
            return List.of(new Pair<>(Wood.AVO, 6), new Pair<>(Ingot.MOLTEN, 12));
        } else if (level < 103) {
            return List.of(new Pair<>(Wood.SKY, 6), new Pair<>(Ingot.VOIDSTONE, 12));
        } else if (level < 106) {
            return List.of(new Pair<>(Wood.DERNIC, 6), new Pair<>(Ingot.DERNIC, 12));
        } else {
            return List.of();
        }
    }
}