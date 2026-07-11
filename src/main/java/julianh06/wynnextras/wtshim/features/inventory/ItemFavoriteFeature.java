// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ItemFavoriteFeature.
 * Mixin target: ItemFavoriteFeatureAccessor#callIsFavorited.
 */
package julianh06.wynnextras.wtshim.features.inventory;

import julianh06.wynnextras.wtshim.core.consumers.features.Feature;
import net.minecraft.item.ItemStack;

public class ItemFavoriteFeature extends Feature {
    /** Mixin target. Stub: nothing is favorited — WynnExtras queries this but we don't track it. */
    private boolean isFavorited(ItemStack itemStack) {
        return false;
    }
}
