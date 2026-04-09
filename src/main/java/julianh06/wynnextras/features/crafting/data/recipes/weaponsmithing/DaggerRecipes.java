package julianh06.wynnextras.features.crafting.data.recipes.weaponsmithing;

import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.materials.Ingot;
import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.materials.Paper;
import julianh06.wynnextras.features.crafting.data.materials.Plank;
import julianh06.wynnextras.utils.Pair;

import java.util.List;

public class DaggerRecipes implements IRecipeData {
    public static DaggerRecipes INSTANCE = new DaggerRecipes();

    public List<Pair<IMaterial, Integer>> getMaterials(int level) {
        if(level < 10) {
            return List.of(new Pair<>(Plank.OAK, 1), new Pair<>(Ingot.COPPER, 2));
        } else if (level < 20) {
            return List.of(new Pair<>(Plank.BIRCH, 2), new Pair<>(Ingot.GRANITE, 4));
        } else if (level < 30) {
            return List.of(new Pair<>(Plank.WILLOW, 2), new Pair<>(Ingot.GOLD, 4));
        } else if (level < 40) {
            return List.of(new Pair<>(Plank.ACACIA, 3), new Pair<>(Ingot.SANDSTONE, 6));
        } else if (level < 50) {
            return List.of(new Pair<>(Plank.SPRUCE, 3), new Pair<>(Ingot.IRON, 6));
        } else if (level < 60) {
            return List.of(new Pair<>(Plank.JUNGLE, 4), new Pair<>(Ingot.SILVER, 8));
        } else if (level < 70) {
            return List.of(new Pair<>(Plank.DARK, 4), new Pair<>(Ingot.COBALT, 8));
        } else if (level < 80) {
            return List.of(new Pair<>(Plank.LIGHT, 5), new Pair<>(Ingot.KANDERSTONE, 10));
        } else if (level < 90) {
            return List.of(new Pair<>(Plank.PINE, 5), new Pair<>(Ingot.DIAMOND, 10));
        } else if (level < 100) {
            return List.of(new Pair<>(Plank.AVO, 6), new Pair<>(Ingot.MOLTEN, 12));
        } else if (level < 105) {
            return List.of(new Pair<>(Plank.SKY, 6), new Pair<>(Ingot.VOIDSTONE, 12));
        } else if (level < 110) {
            return List.of(new Pair<>(Plank.DERNIC, 6), new Pair<>(Ingot.DERNIC, 12));
        } else if (level < 115) {
            return List.of(new Pair<>(Plank.MAPLE, 7), new Pair<>(Ingot.TITANIUM, 14));
        } else if (level < 120) {
            return List.of(new Pair<>(Plank.REDWOOD, 7), new Pair<>(Ingot.CINNABAR, 14));
        } else {
            return List.of();
        }
    }
}