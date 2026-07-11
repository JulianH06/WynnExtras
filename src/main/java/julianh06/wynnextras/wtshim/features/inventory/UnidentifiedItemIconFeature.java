// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — UnidentifiedItemIconFeature.
 * Mixin target: UnidentifiedItemIconFeatureInvoker#invokeDrawIcon.
 */
package julianh06.wynnextras.wtshim.features.inventory;

import julianh06.wynnextras.wtshim.core.consumers.features.Feature;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public class UnidentifiedItemIconFeature extends Feature {
    /** Mixin target. No-op stub — method must exist so the invoker binds. */
    private void drawIcon(DrawContext context, ItemStack itemStack, int slotX, int slotY, int z) {
        /* no-op */
    }
}
