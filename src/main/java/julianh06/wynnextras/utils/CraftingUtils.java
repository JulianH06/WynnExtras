package julianh06.wynnextras.utils;

import com.wynntils.core.components.Models;
import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.gear.type.GearRequirements;
import com.wynntils.models.ingredients.type.IngredientInfo;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CraftingUtils {

    private static Map<String, IngredientInfo> allIngredients;

    public static IngredientInfo getIng(String name) {
        if (allIngredients == null) {
            try {
                allIngredients = Models.Ingredient.getAllIngredientInfos()
                        .collect(Collectors.toMap(
                                IngredientInfo::name,
                                ingredient -> ingredient
                        ));
            } catch (Exception e) {
                System.err.println("Failed to load ingredient list from wynntills");
                return null;
            }
        }

        return allIngredients.get(name);
    }

    public static StatPossibleValues applyMultiplier(StatPossibleValues value, Double multiplier) {
        int min = (int) Math.floor(value.range().low() * multiplier);
        int max = (int) Math.floor(value.range().high() * multiplier);
        int raw = (int) Math.floor(value.baseValue() * multiplier);
        // happens on recipes that flip ingredients with negative ingredient effectiveness
        if (min > max) return new StatPossibleValues(value.statType(), new RangedValue(max, min), raw, true);
        return new StatPossibleValues(value.statType(), new RangedValue(min, max), raw, true);
    }

    public static List<Pair<Skill, Integer>> applyMultiplier(List<Pair<Skill, Integer>> list, Double multiplier) {
        List<Pair<Skill, Integer>> result = new ArrayList<>();
        for (Pair<Skill, Integer> pair : list) {
            int newValue = (int) Math.round(pair.b() * multiplier);
            result.add(new Pair<>(pair.a(), newValue));
        }
        return result;
    }

    public static void addIds(List<StatPossibleValues> values, StatPossibleValues toAdd) {
        for (int i = 0; i < values.size(); i++) {
            StatPossibleValues existing = values.get(i);
            if (existing.statType().equals(toAdd.statType())) {
                RangedValue mergedRange = addRanges(existing.range(), toAdd.range());
                int newBaseValue = existing.baseValue() + toAdd.baseValue();

                // replace the existing entry
                values.set(i, new StatPossibleValues(
                        toAdd.statType(),
                        mergedRange,
                        newBaseValue,
                        true
                ));
                return;
            }
        }

        // if not found add new entry
        values.add(toAdd);
    }

    private static RangedValue addRanges(RangedValue a, RangedValue b) {
        return new RangedValue(a.low() + b.low(), a.high() + b.high());
    }

    public static GearRequirements addGearRequirements(GearRequirements value, List<Pair<Skill, Integer>> toAdd, double multiplier) {

        List<Pair<Skill, Integer>> toAddScaled = applyMultiplier(toAdd, multiplier);

        if (value == null) {
            return new GearRequirements(
                    0,
                    null,
                    toAddScaled,
                    null
            );
        }

        List<Pair<Skill, Integer>> newReqs = new ArrayList<>(value.skills());

        for (Pair<Skill, Integer> newReq : toAddScaled) {
            boolean found = false;

            // Check if this skill already has a requirement
            for (int i = 0; i < newReqs.size(); i++) {
                Pair<Skill, Integer> existingReq = newReqs.get(i);

                if (existingReq.a().equals(newReq.a())) {
                    int newValue = existingReq.b() + newReq.b();
                    newReqs.set(i, new Pair<>(newReq.a(), newValue));
                    found = true;
                    break;
                }
            }

            if (!found) {
                newReqs.add(newReq);
            }
        }

        return new GearRequirements(
                value.level(),
                value.classType(),
                newReqs,
                value.quest()
        );
    }
}
