// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — StatModel (= Models.Stat), SLIM.
 * Wynntils' StatModel is a large registry keyed on the official Wynncraft api-names (loaded from
 * id_keys.json). That registry is NOT ported (see Phase 6b/7 deviations). The only fork caller is
 * CraftingDataService, which maps identification api-names from the Wynncraft /items API to a
 * StatType. So fromApiName builds a StatType ad-hoc, mirroring GearModel.statTypeFor: skill
 * identifications become SkillStatType, everything else a plain StatType with a prettified label.
 * Returns null only for null/blank input (the /items API keys are always valid api-names).
 */
package julianh06.wynnextras.wtshim.models.stats;

import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.models.elements.type.Skill;
import julianh06.wynnextras.wtshim.models.stats.type.SkillStatType;
import julianh06.wynnextras.wtshim.models.stats.type.StatType;

public final class StatModel extends Model {
    public StatType fromApiName(String apiName) {
        if (apiName == null || apiName.isBlank()) return null;

        Skill skill = skillFromApiName(apiName);
        if (skill != null) {
            return new SkillStatType(skill, apiName, prettify(apiName));
        }
        return new StatType(apiName, prettify(apiName));
    }

    private static Skill skillFromApiName(String apiName) {
        return switch (apiName) {
            case "rawStrength", "strength" -> Skill.STRENGTH;
            case "rawDexterity", "dexterity" -> Skill.DEXTERITY;
            case "rawIntelligence", "intelligence" -> Skill.INTELLIGENCE;
            case "rawDefence", "defence" -> Skill.DEFENCE;
            case "rawAgility", "agility" -> Skill.AGILITY;
            default -> null;
        };
    }

    /** camelCase apiName -> "Camel Case" display label. */
    private static String prettify(String apiName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < apiName.length(); i++) {
            char c = apiName.charAt(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                sb.append(' ').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
