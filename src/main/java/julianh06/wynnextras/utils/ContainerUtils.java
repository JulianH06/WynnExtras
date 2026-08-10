package julianh06.wynnextras.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.sync.ComponentChangesHash;
import net.minecraft.screen.sync.ItemStackHash;
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

    public static void pressKeyOnSlot(int slot, int syncId, int hotbarKey, List<ItemStack> stacks) {
        if (MinecraftUtils.mc() == null) return;
        if (MinecraftUtils.mc().getNetworkHandler() == null || slot < 0 || slot >= stacks.size()) return;
        try {
            ComponentChangesHash.ComponentHasher hasher = MinecraftUtils.mc().getNetworkHandler().getComponentHasher();
            Int2ObjectOpenHashMap<ItemStackHash> modifiedStacks = new Int2ObjectOpenHashMap<ItemStackHash>();
            modifiedStacks.put(slot, ItemStackHash.fromItemStack(Items.AIR.getDefaultStack(), hasher));
            ItemStackHash clickedStack = ItemStackHash.fromItemStack(stacks.get(slot), hasher);
            MinecraftUtils.mc().getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                    syncId,
                    0,
                    (short) slot,
                    (byte) hotbarKey,
                    SlotActionType.SWAP,
                    modifiedStacks,
                    clickedStack
            ));
        } catch (Throwable ignored) {}
    }

    private static void click(int slot, int syncId, int mouseButton, SlotActionType action) {
        if (MinecraftUtils.mc().interactionManager == null || MinecraftUtils.localPlayerOrNull() == null) return;
        MinecraftUtils.mc().interactionManager.clickSlot(syncId, slot, mouseButton, action, MinecraftUtils.player());
    }
}
