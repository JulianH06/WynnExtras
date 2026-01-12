package julianh06.wynnextras.features.crafting.data.recipes.jeweling;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.materials.Gem;
import julianh06.wynnextras.features.crafting.data.materials.Oil;
import julianh06.wynnextras.utils.Pair;

import java.util.List;

public class NecklaceRecipes implements IRecipeData {
    public static NecklaceRecipes INSTANCE = new NecklaceRecipes();

    public List<Pair<IMaterial, Integer>> getMaterials(int level) {
        if(level < 10) {
            return List.of(new Pair<>(Gem.COPPER, 3), new Pair<>(Oil.GUDGEON, 1));
        } else if (level < 20) {
            return List.of(new Pair<>(Gem.GRANITE, 6), new Pair<>(Oil.TROUT, 2));
        } else if (level < 30) {
            return List.of(new Pair<>(Gem.GOLD, 6), new Pair<>(Oil.SALMON, 2));
        } else if (level < 40) {
            return List.of(new Pair<>(Gem.SANDSTONE, 9), new Pair<>(Oil.CARP, 3));
        } else if (level < 50) {
            return List.of(new Pair<>(Gem.IRON, 9), new Pair<>(Oil.ICEFISH, 3));
        } else if (level < 60) {
            return List.of(new Pair<>(Gem.SILVER, 12), new Pair<>(Oil.PIRANHA, 4));
        } else if (level < 70) {
            return List.of(new Pair<>(Gem.COBALT, 12), new Pair<>(Oil.KOI, 4));
        } else if (level < 80) {
            return List.of(new Pair<>(Gem.KANDERSTONE, 15), new Pair<>(Oil.GYLIA, 5));
        } else if (level < 90) {
            return List.of(new Pair<>(Gem.DIAMOND, 15), new Pair<>(Oil.BASS, 5));
        } else if (level < 100) {
            return List.of(new Pair<>(Gem.MOLTEN, 18), new Pair<>(Oil.MOLTEN, 6));
        } else if (level < 103) {
            return List.of(new Pair<>(Gem.VOIDSTONE, 18), new Pair<>(Oil.STARFISH, 6));
        } else if (level < 106) {
            return List.of(new Pair<>(Gem.DERNIC, 18), new Pair<>(Oil.DERNIC, 6));
        } else {
            return List.of();
        }
    }
}