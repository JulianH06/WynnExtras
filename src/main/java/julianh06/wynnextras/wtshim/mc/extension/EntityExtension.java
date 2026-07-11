// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — EntityExtension marker. */
package julianh06.wynnextras.wtshim.mc.extension;

public interface EntityExtension {
    default void setRendered(boolean rendered) {}
    default boolean isRendered() { return true; }
}
