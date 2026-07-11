// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — GearRequirements record. */
package julianh06.wynnextras.wtshim.models.gear.type;

import julianh06.wynnextras.wtshim.models.character.type.ClassType;
import julianh06.wynnextras.wtshim.models.elements.type.Skill;
import julianh06.wynnextras.wtshim.utils.type.Pair;
import java.util.List;
import java.util.Optional;

public record GearRequirements(
        int level,
        Optional<ClassType> classType,
        List<Pair<Skill, Integer>> skills,
        Optional<String> quest
) {
    public int getLevel() { return level; }
}
