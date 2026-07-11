// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — SkillStatType. */
package julianh06.wynnextras.wtshim.models.stats.type;

import julianh06.wynnextras.wtshim.models.elements.type.Skill;

public class SkillStatType extends StatType {
    private final Skill skill;

    public SkillStatType(Skill skill, String apiName, String displayName) {
        super(apiName, displayName);
        this.skill = skill;
    }

    public Skill getSkill() { return skill; }
}
