package julianh06.wynnextras.features.crafting.data.recipes.tailoring;

import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.materials.Ingot;
import julianh06.wynnextras.features.crafting.data.materials.StringMaterial;
import julianh06.wynnextras.utils.Pair;

import java.util.List;

public class BootsRecipes implements IRecipeData {
    public static BootsRecipes INSTANCE = new BootsRecipes();

    public List<Pair<IMaterial, Integer>> getMaterials(int level) {
        if(level < 10) {
            return List.of(new Pair<>(Ingot.COPPER, 1), new Pair<>(StringMaterial.WHEAT, 2));
        } else if (level < 20) {
            return List.of(new Pair<>(Ingot.GRANITE, 2), new Pair<>(StringMaterial.BARLEY, 4));
        } else if (level < 30) {
            return List.of(new Pair<>(Ingot.GOLD, 2), new Pair<>(StringMaterial.OAT, 4));
        } else if (level < 40) {
            return List.of(new Pair<>(Ingot.SANDSTONE, 3), new Pair<>(StringMaterial.MALT, 6));
        } else if (level < 50) {
            return List.of(new Pair<>(Ingot.IRON, 3), new Pair<>(StringMaterial.HOPS, 6));
        } else if (level < 60) {
            return List.of(new Pair<>(Ingot.SILVER, 4), new Pair<>(StringMaterial.RYE, 8));
        } else if (level < 70) {
            return List.of(new Pair<>(Ingot.COBALT, 4), new Pair<>(StringMaterial.MILLET, 8));
        } else if (level < 80) {
            return List.of(new Pair<>(Ingot.KANDERSTONE, 5), new Pair<>(StringMaterial.DECAY, 10));
        } else if (level < 90) {
            return List.of(new Pair<>(Ingot.DIAMOND, 5), new Pair<>(StringMaterial.RICE, 10));
        } else if (level < 100) {
            return List.of(new Pair<>(Ingot.MOLTEN, 6), new Pair<>(StringMaterial.SORGHUM, 12));
        } else if (level < 104) {
            return List.of(new Pair<>(Ingot.VOIDSTONE, 6), new Pair<>(StringMaterial.HEMP, 12));
        } else if (level < 106) {
            return List.of(new Pair<>(Ingot.DERNIC, 6), new Pair<>(StringMaterial.DERNIC, 12));
        } else {
            return List.of();
        }
    }
}
