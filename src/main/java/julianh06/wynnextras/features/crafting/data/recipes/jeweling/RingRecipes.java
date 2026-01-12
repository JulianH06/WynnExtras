package julianh06.wynnextras.features.crafting.data.recipes.jeweling;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.materials.Gem;
import julianh06.wynnextras.features.crafting.data.materials.Grains;
import julianh06.wynnextras.features.crafting.data.materials.Oil;
import julianh06.wynnextras.utils.Pair;

import java.util.List;

public class RingRecipes implements IRecipeData {
    public static RingRecipes INSTANCE = new RingRecipes();

    public List<Pair<IMaterial, Integer>> getMaterials(int level) {
        if(level < 10) {
            return List.of(new Pair<>(Gem.COPPER, 1), new Pair<>(Oil.GUDGEON, 1));
        } else if (level < 20) {
            return List.of(new Pair<>(Gem.GRANITE, 2), new Pair<>(Oil.TROUT, 2));
        } else if (level < 30) {
            return List.of(new Pair<>(Gem.GOLD, 2), new Pair<>(Oil.SALMON, 2));
        } else if (level < 40) {
            return List.of(new Pair<>(Gem.SANDSTONE, 3), new Pair<>(Oil.CARP, 3));
        } else if (level < 50) {
            return List.of(new Pair<>(Gem.IRON, 3), new Pair<>(Oil.ICEFISH, 3));
        } else if (level < 60) {
            return List.of(new Pair<>(Gem.SILVER, 4), new Pair<>(Oil.PIRANHA, 4));
        } else if (level < 70) {
            return List.of(new Pair<>(Gem.COBALT, 4), new Pair<>(Oil.KOI, 4));
        } else if (level < 80) {
            return List.of(new Pair<>(Gem.KANDERSTONE, 5), new Pair<>(Oil.GYLIA, 5));
        } else if (level < 90) {
            return List.of(new Pair<>(Gem.DIAMOND, 5), new Pair<>(Oil.BASS, 5));
        } else if (level < 100) {
            return List.of(new Pair<>(Gem.MOLTEN, 6), new Pair<>(Oil.MOLTEN, 6));
        } else if (level < 103) {
            return List.of(new Pair<>(Gem.VOIDSTONE, 6), new Pair<>(Oil.STARFISH, 6));
        } else if (level < 106) {
            return List.of(new Pair<>(Gem.DERNIC, 6), new Pair<>(Oil.DERNIC, 6));
        } else {
            return List.of();
        }
    }
}