package julianh06.wynnextras.features.buildplanner.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

public record WynnItem(
    String displayName,
    String type,
    String subType,
    String tier,
    String attackSpeed,
    Map<String, Identification> identifications,
    Map<String, Integer> baseStats,
    String iconId,
    String iconModel,
    int customModelData,
    String craftedCode,
    List<Integer> ingredientPowders,
    Map<String, String> majorIds,
    String lore,
    String restriction
) {
    public WynnItem {
        identifications = Map.copyOf(new LinkedHashMap<>(identifications));
        baseStats = Map.copyOf(new LinkedHashMap<>(baseStats));
        craftedCode = craftedCode == null ? "" : craftedCode;
        ingredientPowders = List.copyOf(ingredientPowders == null ? List.of() : ingredientPowders);
        majorIds = Map.copyOf(majorIds == null ? Map.of() : majorIds);
        lore = lore == null ? "" : lore;
        restriction = restriction == null ? "" : restriction;
    }

    public WynnItem(
            String displayName, String type, String subType, String tier, String attackSpeed,
            Map<String, Identification> identifications, Map<String, Integer> baseStats,
            String iconId, String iconModel, int customModelData, String craftedCode, List<Integer> ingredientPowders,
            Map<String, String> majorIds
    ) {
        this(displayName, type, subType, tier, attackSpeed, identifications, baseStats,
                iconId, iconModel, customModelData, craftedCode, ingredientPowders, majorIds, "", "");
    }

    public WynnItem(
            String displayName, String type, String subType, String tier, String attackSpeed,
            Map<String, Identification> identifications, Map<String, Integer> baseStats,
            String iconId, String iconModel, int customModelData, String craftedCode, List<Integer> ingredientPowders
    ) {
        this(displayName, type, subType, tier, attackSpeed, identifications, baseStats,
                iconId, iconModel, customModelData, craftedCode, ingredientPowders, Map.of());
    }

    public WynnItem(
            String displayName, String type, String subType, String tier, String attackSpeed,
            Map<String, Identification> identifications, Map<String, Integer> baseStats,
            String iconId, String iconModel, int customModelData
    ) {
        this(displayName, type, subType, tier, attackSpeed, identifications, baseStats,
                iconId, iconModel, customModelData, "", List.of());
    }

    public WynnItem(
            String displayName,
            String type,
            String subType,
            String tier,
            String attackSpeed,
            Map<String, Identification> identifications,
            Map<String, Integer> baseStats
    ) {
        this(displayName, type, subType, tier, attackSpeed, identifications, baseStats, "", "", 0);
    }

    public int stat(String key) {
        return baseStats.getOrDefault(key, 0);
    }

    public boolean isCrafted() {
        return !craftedCode.isEmpty();
    }

    public String reference() {
        return isCrafted() ? craftedCode : displayName;
    }
}
