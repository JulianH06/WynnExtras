// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — CraftedConsumableItem stub. */
package julianh06.wynnextras.wtshim.models.items.items.game;

import julianh06.wynnextras.wtshim.models.character.type.ClassType;
import julianh06.wynnextras.wtshim.models.gear.type.ConsumableType;
import julianh06.wynnextras.wtshim.models.items.WynnItem;

public class CraftedConsumableItem extends WynnItem {
    public ClassType getRequiredClass() { return ClassType.NONE; }
    public ConsumableType getConsumableType() { return null; }
    public int getUses() { return 0; }
    public int getCount() { return 0; }
    public int charges() { return 0; }
    public int duration() { return 0; }
    public java.util.List<?> variableStats() { return java.util.List.of(); }
    public java.util.List<?> getIdentifications() { return java.util.List.of(); }
}
