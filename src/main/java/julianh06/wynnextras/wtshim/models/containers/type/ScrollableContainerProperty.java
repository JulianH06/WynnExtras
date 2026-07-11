// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — ScrollableContainerProperty. */
package julianh06.wynnextras.wtshim.models.containers.type;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;

public interface ScrollableContainerProperty {
    default int getNextItemSlot() { return -1; }
    default int getPreviousItemSlot() { return -1; }

    /** Legacy API surface — WynnExtras passes a HandledScreen; returns an optional button slot. */
    default <T extends ScreenHandler> java.util.Optional<Integer> getScrollButton(
            HandledScreen<T> screen, boolean previousPage) {
        int slot = previousPage ? getPreviousItemSlot() : getNextItemSlot();
        return slot < 0 ? java.util.Optional.empty() : java.util.Optional.of(slot);
    }
}
