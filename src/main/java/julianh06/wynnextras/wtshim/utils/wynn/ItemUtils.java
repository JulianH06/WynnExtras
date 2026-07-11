// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ItemUtils. Minimal subset needed by the container-query framework
 * (item name extraction + list equality for the double-send dedup guard).
 */
package julianh06.wynnextras.wtshim.utils.wynn;

import julianh06.wynnextras.wtshim.core.text.StyledText;
import java.util.List;
import net.minecraft.item.ItemStack;

public final class ItemUtils {
    private ItemUtils() {}

    public static StyledText getItemName(ItemStack itemStack) {
        return StyledText.fromComponent(itemStack.getName());
    }

    public static boolean isItemListsEqual(List<ItemStack> firstItems, List<ItemStack> secondItems) {
        if (firstItems.size() != secondItems.size()) return false;

        for (int i = 0; i < firstItems.size(); i++) {
            if (!isItemEqual(firstItems.get(i), secondItems.get(i))) return false;
        }
        return true;
    }

    public static boolean isItemEqual(ItemStack oldItem, ItemStack newItem) {
        if (oldItem == null || newItem == null) return oldItem == newItem;

        // Yarn ItemStack.areEqual == same item, same count, same components (the Mojmap source
        // compared item + damage + count + isSameItemSameComponents; areEqual is the equivalent).
        return ItemStack.areEqual(oldItem, newItem);
    }
}
