package julianh06.wynnextras.features.crafting.data.recipes.woodworking;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.materials.Oil;
import julianh06.wynnextras.features.crafting.data.materials.StringMaterial;
import julianh06.wynnextras.features.crafting.data.materials.Wood;
import julianh06.wynnextras.utils.Pair;

import java.util.List;

public class RelikRecipes implements IRecipeData {
    public static RelikRecipes INSTANCE = new RelikRecipes();

    public List<Pair<IMaterial, Integer>> getMaterials(int level) {
        if(level < 10) {
            return List.of(new Pair<>(Wood.OAK, 1), new Pair<>(Oil.GUDGEON, 2));
        } else if (level < 20) {
            return List.of(new Pair<>(Wood.BIRCH, 2), new Pair<>(Oil.TROUT, 4));
        } else if (level < 30) {
            return List.of(new Pair<>(Wood.WILLOW, 2), new Pair<>(Oil.SALMON, 4));
        } else if (level < 40) {
            return List.of(new Pair<>(Wood.ACACIA, 3), new Pair<>(Oil.CARP, 6));
        } else if (level < 50) {
            return List.of(new Pair<>(Wood.SPRUCE, 3), new Pair<>(Oil.ICEFISH, 6));
        } else if (level < 60) {
            return List.of(new Pair<>(Wood.JUNGLE, 4), new Pair<>(Oil.PIRANHA, 8));
        } else if (level < 70) {
            return List.of(new Pair<>(Wood.DARK, 4), new Pair<>(Oil.KOI, 8));
        } else if (level < 80) {
            return List.of(new Pair<>(Wood.LIGHT, 5), new Pair<>(Oil.GYLIA, 10));
        } else if (level < 90) {
            return List.of(new Pair<>(Wood.PINE, 5), new Pair<>(Oil.BASS, 10));
        } else if (level < 100) {
            return List.of(new Pair<>(Wood.AVO, 6), new Pair<>(Oil.MOLTEN, 12));
        } else if (level < 103) {
            return List.of(new Pair<>(Wood.SKY, 6), new Pair<>(Oil.STARFISH, 12));
        } else if (level < 106) {
            return List.of(new Pair<>(Wood.DERNIC, 6), new Pair<>(Oil.DERNIC, 12));
        } else {
            return List.of();
        }
    }
}