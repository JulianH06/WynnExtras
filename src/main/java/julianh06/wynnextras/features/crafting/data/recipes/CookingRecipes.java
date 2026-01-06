package julianh06.wynnextras.features.crafting.data.recipes;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.materials.Grains;
import julianh06.wynnextras.features.crafting.data.materials.Meat;
import julianh06.wynnextras.utils.Pair;

import java.util.List;

public class CookingRecipes implements IRecipeData {
    public static CookingRecipes INSTANCE = new CookingRecipes();

    public List<Pair<IMaterial, Integer>> getMaterials(int level) {
        if(level < 10) {
            return List.of(new Pair<>(Meat.GUDGEON, 2), new Pair<>(Grains.WHEAT, 1));
        } else if (level < 20) {
            return List.of(new Pair<>(Meat.TROUT, 4), new Pair<>(Grains.BARLEY, 2));
        } else if (level < 30) {
            return List.of(new Pair<>(Meat.SALMON, 4), new Pair<>(Grains.OAT, 2));
        } else if (level < 40) {
            return List.of(new Pair<>(Meat.CARP, 6), new Pair<>(Grains.MALT, 3));
        } else if (level < 50) {
            return List.of(new Pair<>(Meat.ICEFISH, 6), new Pair<>(Grains.HOPS, 3));
        } else if (level < 60) {
            return List.of(new Pair<>(Meat.PIRANHA, 8), new Pair<>(Grains.RYE, 4));
        } else if (level < 70) {
            return List.of(new Pair<>(Meat.KOI, 8), new Pair<>(Grains.MILLET, 4));
        } else if (level < 80) {
            return List.of(new Pair<>(Meat.GYLIA, 10), new Pair<>(Grains.DECAY, 5));
        } else if (level < 90) {
            return List.of(new Pair<>(Meat.BASS, 10), new Pair<>(Grains.RICE, 5));
        } else if (level < 100) {
            return List.of(new Pair<>(Meat.MOLTEN, 12), new Pair<>(Grains.SORGHUM, 6));
        } else if (level < 104) {
            return List.of(new Pair<>(Meat.STARFISH, 12), new Pair<>(Grains.HEMP, 6));
        } else if (level < 106) {
            return List.of(new Pair<>(Meat.DERNIC, 12), new Pair<>(Grains.DERNIC, 6));
        } else {
            return List.of();
        }
    }
}