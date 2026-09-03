package julianh06.wynnextras.features.shoppinglist.service;

import julianh06.wynnextras.features.crafting.data.WynnDataService;
import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.materials.Gem;
import julianh06.wynnextras.features.crafting.data.materials.Grains;
import julianh06.wynnextras.features.crafting.data.materials.Ingot;
import julianh06.wynnextras.features.crafting.data.materials.Meat;
import julianh06.wynnextras.features.crafting.data.materials.Oil;
import julianh06.wynnextras.features.crafting.data.materials.Paper;
import julianh06.wynnextras.features.crafting.data.materials.Plank;
import julianh06.wynnextras.features.crafting.data.materials.StringMaterial;
import julianh06.wynnextras.features.shoppinglist.model.RequirementType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ShoppingListEntryCatalog {
    private static final List<String> MATERIAL_NAMES = materialNames();

    private ShoppingListEntryCatalog() {}

    public static List<String> suggestions(RequirementType type, String query, int limit) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<String> names = type == RequirementType.MATERIAL
                ? MATERIAL_NAMES
                : WynnDataService.getInstance().getIngredientNames();
        if (normalizedQuery.isEmpty()) {
            return names.stream().limit(Math.max(0, limit)).toList();
        }
        Comparator<String> comparator = Comparator
                .comparingInt((String name) -> matchRank(name, normalizedQuery))
                .thenComparing(String.CASE_INSENSITIVE_ORDER)
                .thenComparing(String::compareTo);
        return names.stream()
                .filter(name -> normalizedQuery.isEmpty() || name.toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(comparator)
                .limit(Math.max(0, limit))
                .toList();
    }

    private static int matchRank(String name, String query) {
        if (query.isEmpty()) return 2;
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.equals(query)) return 0;
        return normalized.startsWith(query) ? 1 : 2;
    }

    private static List<String> materialNames() {
        List<String> names = new ArrayList<>();
        add(names, Ingot.values());
        add(names, Plank.values());
        add(names, Paper.values());
        add(names, Oil.values());
        add(names, Meat.values());
        add(names, Grains.values());
        add(names, Gem.values());
        add(names, StringMaterial.values());
        return names.stream()
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(String::compareTo))
                .toList();
    }

    private static void add(List<String> names, IMaterial[] materials) {
        names.addAll(Arrays.stream(materials).map(IMaterial::getName).toList());
    }
}
