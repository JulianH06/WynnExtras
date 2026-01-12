package julianh06.wynnextras.features.crafting.data.recipes;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.materials.Oil;
import julianh06.wynnextras.features.crafting.data.materials.Grains;
import julianh06.wynnextras.utils.Pair;

import java.util.List;

public class AlchemismRecipes implements IRecipeData {
    public static AlchemismRecipes INSTANCE = new AlchemismRecipes();

    public List<Pair<IMaterial, Integer>> getMaterials(int level) {
        if(level < 10) {
            return List.of(new Pair<>(Grains.WHEAT, 2), new Pair<>(Oil.GUDGEON, 1));
        } else if (level < 20) {
            return List.of(new Pair<>(Grains.BARLEY, 4), new Pair<>(Oil.TROUT, 2));
        } else if (level < 30) {
            return List.of(new Pair<>(Grains.OAT, 4), new Pair<>(Oil.SALMON, 2));
        } else if (level < 40) {
            return List.of(new Pair<>(Grains.MALT, 6), new Pair<>(Oil.CARP, 3));
        } else if (level < 50) {
            return List.of(new Pair<>(Grains.HOPS, 6), new Pair<>(Oil.ICEFISH, 3));
        } else if (level < 60) {
            return List.of(new Pair<>(Grains.RYE, 8), new Pair<>(Oil.PIRANHA, 4));
        } else if (level < 70) {
            return List.of(new Pair<>(Grains.MILLET, 8), new Pair<>(Oil.KOI, 4));
        } else if (level < 80) {
            return List.of(new Pair<>(Grains.DECAY, 10), new Pair<>(Oil.GYLIA, 5));
        } else if (level < 90) {
            return List.of(new Pair<>(Grains.RICE, 10), new Pair<>(Oil.BASS, 5));
        } else if (level < 100) {
            return List.of(new Pair<>(Grains.SORGHUM, 12), new Pair<>(Oil.MOLTEN, 6));
        } else if (level < 103) {
            return List.of(new Pair<>(Grains.HEMP, 12), new Pair<>(Oil.STARFISH, 6));
        } else if (level < 106) {
            return List.of(new Pair<>(Grains.DERNIC, 12), new Pair<>(Oil.DERNIC, 6));
        } else {
            return List.of();
        }
    }
}