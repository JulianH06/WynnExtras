package julianh06.wynnextras.utils;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

public final class ContainerUtils {
    private ContainerUtils() {}

    public static void clickOnSlot(int slot, int syncId, int mouseButton, List<ItemStack> ignoredStacks) {
        click(slot, syncId, mouseButton, SlotActionType.PICKUP);
    }

    public static void shiftClickOnSlot(int slot, int syncId, int mouseButton, List<ItemStack> ignoredStacks) {
        click(slot, syncId, mouseButton, SlotActionType.QUICK_MOVE);
    }

    private static void click(int slot, int syncId, int mouseButton, SlotActionType action) {
        if (MinecraftUtils.mc().interactionManager == null || MinecraftUtils.localPlayerOrNull() == null) return;
        MinecraftUtils.mc().interactionManager.clickSlot(syncId, slot, mouseButton, action, MinecraftUtils.player());
    }
}
