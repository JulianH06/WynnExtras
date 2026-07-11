// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — IngredientInfo (faithful record).
 * Upgraded from the earlier 4-field slim stub to Wynntils' real 13-component shape: the merged
 * upstream CraftingDataService + Recipe now construct/read the full record (professions as
 * List<ProfessionType>, positionModifiers, variableStats, charges/duration/durabilityModifier).
 */
package julianh06.wynnextras.wtshim.models.ingredients.type;

import julianh06.wynnextras.wtshim.models.elements.type.Skill;
import julianh06.wynnextras.wtshim.models.profession.type.ProfessionType;
import julianh06.wynnextras.wtshim.models.stats.type.StatType;
import julianh06.wynnextras.wtshim.models.wynnitem.type.ItemMaterial;
import julianh06.wynnextras.wtshim.models.wynnitem.type.ItemObtainInfo;
import julianh06.wynnextras.wtshim.utils.type.Pair;
import julianh06.wynnextras.wtshim.utils.type.RangedValue;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record IngredientInfo(
        String name,
        int tier,
        int level,
        Optional<String> apiName,
        ItemMaterial material,
        List<ProfessionType> professions,
        List<Pair<Skill, Integer>> skillRequirements,
        Map<IngredientPosition, Integer> positionModifiers,
        List<ItemObtainInfo> obtainInfo,
        int duration,
        int charges,
        int durabilityModifier,
        List<Pair<StatType, RangedValue>> variableStats) {}
