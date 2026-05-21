package julianh06.wynnextras.features.bankoverlay;

import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.mixin.Accessor.SlotAccessor;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Keeps the backing ScreenHandler slots aligned with BankOverlay's custom layout.
 *
 * Other mods usually discover inventory interaction through HandledScreen#getHoveredSlot
 * and ScreenHandler#slots. BankOverlay renders its own widgets, so those consumers need
 * the real active slots moved to the same screen coordinates while the overlay is active.
 */
public final class BankOverlaySlotBridge {
    private static final int HIDDEN_SLOT_COORD = -100000;
    private static final Map<Slot, SlotPosition> ORIGINAL_POSITIONS = new IdentityHashMap<>();
    private static final Set<Slot> EXPOSED_SLOTS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Slot> PREVIOUS_EXPOSED_SLOTS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static ScreenHandler activeHandler;
    private static int activeSlotCount = -1;

    private BankOverlaySlotBridge() {}

    public static void beginFrame(HandledScreen<?> screen) {
        if (screen == null || screen.getScreenHandler() == null) return;

        ScreenHandler handler = screen.getScreenHandler();
        if (handler != activeHandler) {
            restoreAll();
            activeHandler = handler;
            activeSlotCount = handler.slots.size();
            hideAll(handler);
            return;
        }

        if (handler.slots.size() != activeSlotCount) {
            restoreAll();
            activeHandler = handler;
            activeSlotCount = handler.slots.size();
            hideAll(handler);
            return;
        }

        PREVIOUS_EXPOSED_SLOTS.clear();
        PREVIOUS_EXPOSED_SLOTS.addAll(EXPOSED_SLOTS);
        EXPOSED_SLOTS.clear();
    }

    public static void expose(HandledScreen<?> screen, Slot slot, int screenX, int screenY) {
        if (screen == null || slot == null) return;

        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        remember(slot);
        move(slot, screenX - accessor.getX(), screenY - accessor.getY());
        EXPOSED_SLOTS.add(slot);
        PREVIOUS_EXPOSED_SLOTS.remove(slot);
    }

    public static void endFrame() {
        for (Slot slot : PREVIOUS_EXPOSED_SLOTS) {
            hide(slot);
        }
        PREVIOUS_EXPOSED_SLOTS.clear();
    }

    public static void restoreAll() {
        for (Map.Entry<Slot, SlotPosition> entry : ORIGINAL_POSITIONS.entrySet()) {
            move(entry.getKey(), entry.getValue().x(), entry.getValue().y());
        }
        ORIGINAL_POSITIONS.clear();
        EXPOSED_SLOTS.clear();
        PREVIOUS_EXPOSED_SLOTS.clear();
        activeHandler = null;
        activeSlotCount = -1;
    }

    private static void hideAll(ScreenHandler handler) {
        for (Slot slot : handler.slots) {
            hide(slot);
        }
    }

    private static void hide(Slot slot) {
        remember(slot);
        move(slot, HIDDEN_SLOT_COORD, HIDDEN_SLOT_COORD);
    }

    private static void remember(Slot slot) {
        ORIGINAL_POSITIONS.computeIfAbsent(slot, s -> new SlotPosition(s.x, s.y));
    }

    private static void move(Slot slot, int x, int y) {
        if (slot.x == x && slot.y == y) return;
        SlotAccessor accessor = (SlotAccessor) slot;
        accessor.setX(x);
        accessor.setY(y);
    }

    private record SlotPosition(int x, int y) {}
}
