// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ItemHandler.
 *
 * Mixin target: WynnExtras' ItemHandlerInvoker calls calculateAnnotation(ItemStack, StyledText)
 * via @Invoker — so this method MUST exist as an instance method. We delegate to ItemModel to
 * reuse our lore-parsing path.
 */
package julianh06.wynnextras.wtshim.handlers.item;

import julianh06.wynnextras.wtshim.core.components.Handler;
import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import java.util.Optional;
import net.minecraft.item.ItemStack;

public class ItemHandler extends Handler {
    public static Optional<ItemAnnotation> getItemStackAnnotation(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        return Models.Item.getWynnItem(stack).map(w -> (ItemAnnotation) w);
    }

    /** Mixin target for WynnExtras ItemHandlerInvoker#invokeCalculateAnnotation. */
    private ItemAnnotation calculateAnnotation(ItemStack itemStack, StyledText name) {
        return Models.Item.getWynnItem(itemStack).map(w -> (ItemAnnotation) w).orElse(null);
    }
}
