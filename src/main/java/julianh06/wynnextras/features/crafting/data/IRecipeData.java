package julianh06.wynnextras.features.crafting.data;

import julianh06.wynnextras.utils.Pair;

import java.util.List;

public interface IRecipeData {
    List<Pair<IMaterial, Integer>> getMaterials(int level);
}
