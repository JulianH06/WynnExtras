// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Relocated for the WynnExtras standalone compat shim (wtshim). Yarn: ItemStack import.
 */
package julianh06.wynnextras.wtshim.models.containers.event;

import net.minecraft.item.ItemStack;
import net.neoforged.bus.api.Event;

public class ValuableFoundEvent extends Event {
    private final ItemStack item;
    private final ItemSource itemSource;

    public ValuableFoundEvent(ItemStack item, ItemSource itemSource) {
        this.item = item;
        this.itemSource = itemSource;
    }

    public ItemStack getItem() {
        return item;
    }

    public ItemSource getItemSource() {
        return itemSource;
    }

    public enum ItemSource {
        LOOT_CHEST,
        LOOTRUN_REWARD_CHEST,
        RAID_REWARD_CHEST,
        WORLD_EVENT
    }
}
