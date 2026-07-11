// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — TomeItem stub.
 */
package julianh06.wynnextras.wtshim.models.items.items.game;

import julianh06.wynnextras.wtshim.models.character.type.ClassType;
import julianh06.wynnextras.wtshim.models.gear.type.GearTier;
import julianh06.wynnextras.wtshim.models.items.WynnItem;

public class TomeItem extends WynnItem {
    public ClassType getRequiredClass() { return ClassType.NONE; }

    // TODO(phase6-item-pipeline): real TomeItem carries a parsed GearTier; the heuristic shim
    // ItemModel does not annotate tome tiers yet, so RaidModel's mythic-tome detection is inert
    // until the item pipeline lands. Returns NORMAL for now.
    public GearTier getGearTier() { return GearTier.NORMAL; }
}
