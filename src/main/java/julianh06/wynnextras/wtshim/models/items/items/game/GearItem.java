// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — GearItem stub. Filled in Phase 5.
 */
package julianh06.wynnextras.wtshim.models.items.items.game;

import julianh06.wynnextras.wtshim.models.character.type.ClassType;
import julianh06.wynnextras.wtshim.models.items.WynnItem;
import julianh06.wynnextras.wtshim.models.items.properties.ClassableItemProperty;
import julianh06.wynnextras.wtshim.models.items.properties.GearTierItemProperty;
import julianh06.wynnextras.wtshim.models.items.properties.LeveledItemProperty;

public class GearItem extends WynnItem
        implements GearTierItemProperty, LeveledItemProperty, ClassableItemProperty {
    public julianh06.wynnextras.wtshim.models.gear.type.GearTier getGearTier() {
        return julianh06.wynnextras.wtshim.models.gear.type.GearTier.NORMAL;
    }

    @Override public int getLevel() { return getRequirements().getLevel(); }

    @Override public ClassType getRequiredClass() {
        return getRequirements().classType().orElse(ClassType.NONE);
    }
    public julianh06.wynnextras.wtshim.models.gear.type.GearType getGearType() {
        return julianh06.wynnextras.wtshim.models.gear.type.GearType.UNKNOWN;
    }
    public julianh06.wynnextras.wtshim.models.gear.type.GearRequirements getRequirements() {
        return new julianh06.wynnextras.wtshim.models.gear.type.GearRequirements(
                0, java.util.Optional.empty(), java.util.List.of(), java.util.Optional.empty());
    }
    public java.util.List<julianh06.wynnextras.wtshim.models.stats.type.StatPossibleValues> getPossibleValues() {
        return java.util.List.of();
    }
    public java.util.List<julianh06.wynnextras.wtshim.models.stats.type.StatActualValue> getIdentifications() {
        return java.util.List.of();
    }
    public String getName() { return ""; }

    /** Legacy getter — returns the gear's full metadata record. Filled out in Phase 5. */
    public GearInfo getItemInfo() { return null; }

    public record GearInfo(
            julianh06.wynnextras.wtshim.models.gear.type.GearRequirements requirements,
            java.util.List<julianh06.wynnextras.wtshim.utils.type.Pair<
                    julianh06.wynnextras.wtshim.models.stats.type.StatType,
                    julianh06.wynnextras.wtshim.models.stats.type.StatPossibleValues>> variableStats
    ) {}
}
