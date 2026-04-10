package julianh06.wynnextras.features.crafting.data.recipes;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.materials.Oil;
import julianh06.wynnextras.features.crafting.data.materials.Paper;
import julianh06.wynnextras.utils.Pair;

import java.util.List;

public class ScribingRecipes implements IRecipeData {
    public static ScribingRecipes INSTANCE = new ScribingRecipes();

    public List<Pair<IMaterial, Integer>> getMaterials(int level) {
//        return switch (level) {
//            case int l when l < 10 -> List.of(new Pair<>(Paper.OAK, 1), new Pair<>(Oil.GUDGEON, 1));
//        } this apparently only works with jdk 23, ill keep this here in case minecraft decides to upgrade the java version

        if (level < 10) {
            return List.of(new Pair<>(Paper.OAK, 1), new Pair<>(Oil.GUDGEON, 1));
        } else if (level < 20) {
            return List.of(new Pair<>(Paper.BIRCH, 2), new Pair<>(Oil.TROUT, 2));
        } else if (level < 30) {
            return List.of(new Pair<>(Paper.WILLOW, 2), new Pair<>(Oil.SALMON, 2));
        } else if (level < 40) {
            return List.of(new Pair<>(Paper.ACACIA, 3), new Pair<>(Oil.CARP, 3));
        } else if (level < 50) {
            return List.of(new Pair<>(Paper.SPRUCE, 3), new Pair<>(Oil.ICEFISH, 3));
        } else if (level < 60) {
            return List.of(new Pair<>(Paper.JUNGLE, 4), new Pair<>(Oil.PIRANHA, 4));
        } else if (level < 70) {
            return List.of(new Pair<>(Paper.DARK, 4), new Pair<>(Oil.KOI, 4));
        } else if (level < 80) {
            return List.of(new Pair<>(Paper.LIGHT, 5), new Pair<>(Oil.GYLIA, 5));
        } else if (level < 90) {
            return List.of(new Pair<>(Paper.PINE, 5), new Pair<>(Oil.BASS, 5));
        } else if (level < 100) {
            return List.of(new Pair<>(Paper.AVO, 6), new Pair<>(Oil.MOLTEN, 6));
        } else if (level < 105) {
            return List.of(new Pair<>(Paper.SKY, 6), new Pair<>(Oil.STARFISH, 6));
        } else if (level < 110) {
            return List.of(new Pair<>(Paper.DERNIC, 6), new Pair<>(Oil.DERNIC, 6));
        } else if (level < 115) {
            return List.of(new Pair<>(Paper.MAPLE, 7), new Pair<>(Oil.STURGEON, 7));
        } else if (level < 120) {
            return List.of(new Pair<>(Paper.REDWOOD, 7), new Pair<>(Oil.MAHSEER, 7));
        } else {
            return List.of();
        }
    }
}