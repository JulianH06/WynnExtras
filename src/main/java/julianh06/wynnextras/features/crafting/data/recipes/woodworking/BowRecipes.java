package julianh06.wynnextras.features.crafting.data.recipes.woodworking;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.materials.StringMaterial;
import julianh06.wynnextras.features.crafting.data.materials.Plank;
import julianh06.wynnextras.utils.Pair;

import java.util.List;

public class BowRecipes implements IRecipeData {
    public static BowRecipes INSTANCE = new BowRecipes();

    public List<Pair<IMaterial, Integer>> getMaterials(int level) {
        if(level < 10) {
            return List.of(new Pair<>(Plank.OAK, 1), new Pair<>(StringMaterial.WHEAT, 2));
        } else if (level < 20) {
            return List.of(new Pair<>(Plank.BIRCH, 2), new Pair<>(StringMaterial.BARLEY, 4));
        } else if (level < 30) {
            return List.of(new Pair<>(Plank.WILLOW, 2), new Pair<>(StringMaterial.OAT, 4));
        } else if (level < 40) {
            return List.of(new Pair<>(Plank.ACACIA, 3), new Pair<>(StringMaterial.MALT, 6));
        } else if (level < 50) {
            return List.of(new Pair<>(Plank.SPRUCE, 3), new Pair<>(StringMaterial.HOPS, 6));
        } else if (level < 60) {
            return List.of(new Pair<>(Plank.JUNGLE, 4), new Pair<>(StringMaterial.RYE, 8));
        } else if (level < 70) {
            return List.of(new Pair<>(Plank.DARK, 4), new Pair<>(StringMaterial.MILLET, 8));
        } else if (level < 80) {
            return List.of(new Pair<>(Plank.LIGHT, 5), new Pair<>(StringMaterial.DECAY, 10));
        } else if (level < 90) {
            return List.of(new Pair<>(Plank.PINE, 5), new Pair<>(StringMaterial.RICE, 10));
        } else if (level < 100) {
            return List.of(new Pair<>(Plank.AVO, 6), new Pair<>(StringMaterial.SORGHUM, 12));
        } else if (level < 103) {
            return List.of(new Pair<>(Plank.SKY, 6), new Pair<>(StringMaterial.HEMP, 12));
        } else if (level < 106) {
            return List.of(new Pair<>(Plank.DERNIC, 6), new Pair<>(StringMaterial.DERNIC, 12));
        } else {
            return List.of();
        }
    }
}