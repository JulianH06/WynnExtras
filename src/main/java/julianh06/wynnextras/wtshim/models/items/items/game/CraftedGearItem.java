// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — CraftedGearItem stub. Filled in Phase 5. */
package julianh06.wynnextras.wtshim.models.items.items.game;

import julianh06.wynnextras.wtshim.models.gear.type.GearRequirements;
import julianh06.wynnextras.wtshim.models.gear.type.GearTier;
import julianh06.wynnextras.wtshim.models.gear.type.GearType;
import julianh06.wynnextras.wtshim.models.items.WynnItem;

public class CraftedGearItem extends WynnItem {
    public GearTier getGearTier() { return GearTier.CRAFTED; }
    public GearType getGearType() { return GearType.UNKNOWN; }
    public GearRequirements getRequirements() {
        return new GearRequirements(0, java.util.Optional.empty(), java.util.List.of(), java.util.Optional.empty());
    }

    public java.util.List<julianh06.wynnextras.wtshim.utils.type.Pair<
            julianh06.wynnextras.wtshim.models.stats.type.StatType,
            julianh06.wynnextras.wtshim.models.stats.type.StatPossibleValues>> variableStats() { return java.util.List.of(); }
    public java.util.List<julianh06.wynnextras.wtshim.models.stats.type.StatActualValue> getIdentifications() {
        return java.util.List.of();
    }
    public java.util.List<julianh06.wynnextras.wtshim.utils.type.Pair<
            julianh06.wynnextras.wtshim.models.stats.type.DamageType,
            julianh06.wynnextras.wtshim.utils.type.RangedValue>> getDamages() { return java.util.List.of(); }
    public int duration() { return 0; }
    public int charges() { return 0; }
    public float durabilityModifier() { return 1f; }
    public Object getIdentificationDecorator() { return null; }
    public java.util.List<?> identificationDecorations() { return java.util.List.of(); }

    public java.util.List<julianh06.wynnextras.wtshim.models.stats.type.StatPossibleValues> getPossibleValues() {
        return java.util.List.of();
    }
}
