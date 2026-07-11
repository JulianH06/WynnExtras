// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — DurableItemProperty marker interface. Filled in Phase 5.
 */
package julianh06.wynnextras.wtshim.models.items.properties;

public interface DurableItemProperty {
    default julianh06.wynnextras.wtshim.utils.type.CappedValue getDurability() {
        return julianh06.wynnextras.wtshim.utils.type.CappedValue.EMPTY;
    }
}
