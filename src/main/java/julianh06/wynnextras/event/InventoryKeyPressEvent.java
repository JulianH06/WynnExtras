package julianh06.wynnextras.event;

import julianh06.wynnextras.event.api.WEEvent;
import net.minecraft.screen.slot.Slot;
import net.neoforged.bus.api.ICancellableEvent;

public class InventoryKeyPressEvent extends WEEvent implements ICancellableEvent {
    private final int keyCode;
    private final int scanCode;
    private final int modifiers;
    private final Slot hoveredSlot;

    public InventoryKeyPressEvent(int keyCode, int scanCode, int modifiers, Slot hoveredSlot) {
        this.keyCode = keyCode;
        this.scanCode = scanCode;
        this.modifiers = modifiers;
        this.hoveredSlot = hoveredSlot;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public int getScanCode() {
        return scanCode;
    }

    public int getModifiers() {
        return modifiers;
    }

    public Slot getHoveredSlot() {
        return hoveredSlot;
    }
}
