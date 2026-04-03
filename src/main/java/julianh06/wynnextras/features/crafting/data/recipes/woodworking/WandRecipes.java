package julianh06.wynnextras.features.crafting.data.recipes.woodworking;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.materials.Plank;
import julianh06.wynnextras.features.crafting.data.materials.StringMaterial;
import julianh06.wynnextras.utils.Pair;

import java.util.List;

public class WandRecipes implements IRecipeData {
    public static WandRecipes INSTANCE = new WandRecipes();

    public List<Pair<IMaterial, Integer>> getMaterials(int level) {
        if(level < 10) {
            return List.of(new Pair<>(Plank.OAK, 2), new Pair<>(StringMaterial.WHEAT, 1));
        } else if (level < 20) {
            return List.of(new Pair<>(Plank.BIRCH, 4), new Pair<>(StringMaterial.BARLEY, 2));
        } else if (level < 30) {
            return List.of(new Pair<>(Plank.WILLOW, 4), new Pair<>(StringMaterial.OAT, 2));
        } else if (level < 40) {
            return List.of(new Pair<>(Plank.ACACIA, 6), new Pair<>(StringMaterial.MALT, 3));
        } else if (level < 50) {
            return List.of(new Pair<>(Plank.SPRUCE, 6), new Pair<>(StringMaterial.HOPS, 3));
        } else if (level < 60) {
            return List.of(new Pair<>(Plank.JUNGLE, 8), new Pair<>(StringMaterial.RYE, 4));
        } else if (level < 70) {
            return List.of(new Pair<>(Plank.DARK, 8), new Pair<>(StringMaterial.MILLET, 4));
        } else if (level < 80) {
            return List.of(new Pair<>(Plank.LIGHT, 10), new Pair<>(StringMaterial.DECAY, 5));
        } else if (level < 90) {
            return List.of(new Pair<>(Plank.PINE, 10), new Pair<>(StringMaterial.RICE, 5));
        } else if (level < 100) {
            return List.of(new Pair<>(Plank.AVO, 12), new Pair<>(StringMaterial.SORGHUM, 6));
        } else if (level < 103) {
            return List.of(new Pair<>(Plank.SKY, 12), new Pair<>(StringMaterial.HEMP, 6));
        } else if (level < 106) {
            return List.of(new Pair<>(Plank.DERNIC, 12), new Pair<>(StringMaterial.DERNIC, 6));
        } else {
            return List.of();
        }
    }
}