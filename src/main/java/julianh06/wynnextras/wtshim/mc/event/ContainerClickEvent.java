// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim) with Mojmap->Yarn mappings.
 */
package julianh06.wynnextras.wtshim.mc.event;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Fired on click in a container */
public class ContainerClickEvent extends Event implements ICancellableEvent {
    private final ScreenHandler containerMenu;
    private final int slotNum;
    private final SlotActionType clickType;
    private final int mouseButton;

    public ContainerClickEvent(ScreenHandler containerMenu, int slotNum, SlotActionType clickType, int mouseButton) {
        this.containerMenu = containerMenu;
        this.slotNum = slotNum;
        this.clickType = clickType;
        this.mouseButton = mouseButton;
    }

    public ScreenHandler getContainerMenu() {
        return containerMenu;
    }

    public int getSlotNum() {
        return slotNum;
    }

    public SlotActionType getClickType() {
        return clickType;
    }

    public int getMouseButton() {
        return mouseButton;
    }

    public ItemStack getItemStack() {
        if (slotNum >= 0) {
            return containerMenu.getSlot(slotNum).getStack();
        } else {
            return ItemStack.EMPTY;
        }
    }
}
