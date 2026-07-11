// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim) with Mojmap->Yarn mappings.
 */
package julianh06.wynnextras.wtshim.mc.event;

import net.minecraft.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class ContainerSetSlotEvent extends Event {
    private final int containerId;
    private final int stateId;
    private final int slot;
    private final ItemStack itemStack;

    protected ContainerSetSlotEvent(int containerId, int stateId, int slot, ItemStack itemStack) {
        this.containerId = containerId;
        this.stateId = stateId;
        this.slot = slot;
        this.itemStack = itemStack;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getContainerId() {
        return containerId;
    }

    public int getSlot() {
        return slot;
    }

    public int getStateId() {
        return stateId;
    }

    public static class Pre extends ContainerSetSlotEvent implements ICancellableEvent {
        public Pre(int containerId, int stateId, int slot, ItemStack itemStack) {
            super(containerId, stateId, slot, itemStack);
        }
    }

    public static class Post extends ContainerSetSlotEvent {
        public Post(int containerId, int stateId, int slot, ItemStack itemStack) {
            super(containerId, stateId, slot, itemStack);
        }
    }
}
