// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — PotionItem stub. */
package julianh06.wynnextras.wtshim.models.items.items.game;

import julianh06.wynnextras.wtshim.handlers.item.ItemAnnotation;
import julianh06.wynnextras.wtshim.models.items.WynnItem;
import julianh06.wynnextras.wtshim.utils.type.CappedValue;

public class PotionItem extends WynnItem implements ItemAnnotation {
    public CappedValue getUses() { return new CappedValue(0, 0); }
}
