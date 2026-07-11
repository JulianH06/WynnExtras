// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ItemTextOverlayFeature.
 * Mixin target: ItemTextOverlayFeatureMixin#invokeDrawTextOverlay.
 */
package julianh06.wynnextras.wtshim.features.inventory;

import julianh06.wynnextras.wtshim.core.consumers.features.Feature;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public class ItemTextOverlayFeature extends Feature {
    /**
     * Mixin target. No-op stub — we don't render Wynntils' own text overlays, but the method
     * must exist so the @Invoker binds (otherwise BankOverlay crashes with AbstractMethodError).
     */
    private void drawTextOverlay(DrawContext context, ItemStack itemStack, int slotX, int slotY, boolean hotbar) {
        /* no-op — overlays would be rendered by WynnExtras directly if needed */
    }
}
