// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — base WynnItem. Filled out in Phase 5.
 * Implements ItemAnnotation because WynnExtras widely treats a parsed WynnItem and an
 * ItemAnnotation interchangeably (matching Wynntils' behavior).
 */
package julianh06.wynnextras.wtshim.models.items;

import julianh06.wynnextras.wtshim.handlers.item.ItemAnnotation;

public abstract class WynnItem implements ItemAnnotation {
    public String getName() {
        return "";
    }
}
