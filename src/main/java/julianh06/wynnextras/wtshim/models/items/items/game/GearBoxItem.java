// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — GearBoxItem stub.
 */
package julianh06.wynnextras.wtshim.models.items.items.game;

import julianh06.wynnextras.wtshim.models.items.WynnItem;

public class GearBoxItem extends WynnItem {
    public julianh06.wynnextras.wtshim.models.raid.raids.RaidKind getRaidKind() { return null; }
    public julianh06.wynnextras.wtshim.models.gear.type.GearTier getGearTier() {
        return julianh06.wynnextras.wtshim.models.gear.type.GearTier.NORMAL;
    }
    public julianh06.wynnextras.wtshim.models.gear.type.GearType getGearType() {
        return julianh06.wynnextras.wtshim.models.gear.type.GearType.UNKNOWN;
    }
}
