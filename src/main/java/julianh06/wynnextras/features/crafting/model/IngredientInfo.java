package julianh06.wynnextras.features.crafting.model;

import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.enums.WEProfessionType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record IngredientInfo(String name, int tier, int level, Optional<String> internalName, Object material,
                             List<WEProfessionType> professions, List<Pair<Skill, Integer>> skillRequirements,
                             Map<IngredientPosition, Integer> positionModifiers, List<Object> ingredientEffectiveness,
                             int duration, int charges, int durabilityModifier,
                             List<Pair<StatType, RangedValue>> variableStats) {}
