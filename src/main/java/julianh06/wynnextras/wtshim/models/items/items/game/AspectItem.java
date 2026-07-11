// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — AspectItem stub. */
package julianh06.wynnextras.wtshim.models.items.items.game;

import julianh06.wynnextras.wtshim.models.gear.type.GearTier;
import julianh06.wynnextras.wtshim.models.items.WynnItem;

public class AspectItem extends WynnItem {
    // TODO(phase6-item-pipeline): the real AspectItem carries a parsed GearTier. The shim
    // ItemModel is heuristic and does not annotate aspect tiers yet, so RaidModel's reward-chest
    // mythic-aspect detection is inert until the item pipeline lands. Returns NORMAL for now.
    public GearTier getGearTier() { return GearTier.NORMAL; }
}
