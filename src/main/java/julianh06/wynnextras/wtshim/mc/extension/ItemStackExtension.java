// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — ItemStackExtension marker. */
package julianh06.wynnextras.wtshim.mc.extension;

import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.handlers.item.ItemAnnotation;

public interface ItemStackExtension {
    default void setAnnotation(ItemAnnotation annotation) {}
    default ItemAnnotation getAnnotation() { return null; }

    // Original (pre-decoration) item name. Default null → callers recompute and cache it
    // themselves; a backing ItemStack mixin may override these to persist it.
    default StyledText getOriginalName() { return null; }
    default void setOriginalName(StyledText name) {}
}
