// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ContainerUtils.
 * Thin wrapper around MinecraftClient.interactionManager for slot clicks.
 */
package julianh06.wynnextras.wtshim.utils.wynn;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;

public final class ContainerUtils {
    private static final int INVENTORY_SLOTS = 36;

    private ContainerUtils() {}

    /**
     * Source: Wynntils ContainerUtils#openInventory. Used by QueryStep.useItemInHotbar to "use"
     * a hotbar item by clicking its slot in the player's own screen handler (syncId 0) when no
     * other container is open.
     */
    public static boolean openInventory(int slotNum) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        ScreenHandler menu = mc.player.currentScreenHandler;
        int containerId = menu == null ? 0 : menu.syncId;
        if (containerId != 0) {
            // Another inventory is already open, cannot do this
            return false;
        }
        clickOnSlot(INVENTORY_SLOTS + slotNum, containerId, GLFW.GLFW_MOUSE_BUTTON_LEFT, List.of());
        return true;
    }

    /** Source: Wynntils ContainerUtils#closeContainer. */
    public static void closeContainer(int containerId) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(containerId));
    }

    /**
     * Source: Wynntils ContainerUtils#closeBackgroundContainer.
     * Closes an invisible container opened in the background without closing the visible screen.
     */
    public static void closeBackgroundContainer() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        closeContainer(mc.player.currentScreenHandler.syncId);
        mc.player.currentScreenHandler = mc.player.playerScreenHandler;
    }

    /**
     * Matches the Wynntils call shape used by WynnExtras:
     * {@code clickOnSlot(slotIndex, containerSyncId, mouseButton, stacksList)}.
     */
    public static void clickOnSlot(int slot, int containerId, int mouseButton, List<ItemStack> stacks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(containerId, slot, mouseButton, SlotActionType.PICKUP, mc.player);
    }

    public static void clickOnSlot(int slot, int containerId, int mouseButton, DefaultedList<ItemStack> stacks) {
        clickOnSlot(slot, containerId, mouseButton, (List<ItemStack>) stacks);
    }

    public static void shiftClickOnSlot(int slot, ScreenHandler handler) {
        if (handler == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
    }

    /** WynnExtras-shape overload: shiftClickOnSlot(slot, syncId, button, stacks). */
    public static void shiftClickOnSlot(int slot, int containerId, int mouseButton, List<ItemStack> stacks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(containerId, slot, mouseButton, SlotActionType.QUICK_MOVE, mc.player);
    }

    public static void shiftClickOnSlot(int slot, int containerId, int mouseButton, DefaultedList<ItemStack> stacks) {
        shiftClickOnSlot(slot, containerId, mouseButton, (List<ItemStack>) stacks);
    }

    /**
     * Source: Wynntils ContainerUtils#pressKeyOnSlot. Wynncraft interprets SWAP actions on bank
     * nav buttons as quick-jump requests; buttonNum selects the quick-jump destination index.
     */
    public static void pressKeyOnSlot(int clickedSlot, int containerId, int buttonNum, List<ItemStack> stacks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(containerId, clickedSlot, buttonNum, SlotActionType.SWAP, mc.player);
    }
}
