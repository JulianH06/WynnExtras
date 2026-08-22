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



    public static void clickOnSlot(int slot, int syncId, int mouseButton, List<ItemStack> stacks) {
        click(slot, syncId, 0, mouseButton, SlotActionType.PICKUP, stacks);
    }

    public static void clickOnSlot(int slot, int syncId, int revision, int mouseButton, List<ItemStack> stacks) {
        click(slot, syncId, revision, mouseButton, SlotActionType.PICKUP, stacks);
    }

    public static void shiftClickOnSlot(int slot, int syncId, int mouseButton, List<ItemStack> stacks) {
        click(slot, syncId, 0, mouseButton, SlotActionType.QUICK_MOVE, stacks);
    }

    public static void shiftClickOnSlot(int slot, int syncId, int revision, int mouseButton, List<ItemStack> stacks) {
        click(slot, syncId, revision, mouseButton, SlotActionType.QUICK_MOVE, stacks);
    }

    public static void pressKeyOnSlot(int slot, int syncId, int hotbarKey, List<ItemStack> stacks) {
        click(slot, syncId, 0, hotbarKey, SlotActionType.SWAP, stacks);
    }

    public static void pressKeyOnSlot(int slot, int syncId, int revision, int hotbarKey, List<ItemStack> stacks) {
        click(slot, syncId, revision, hotbarKey, SlotActionType.SWAP, stacks);
    }

    private static void click(int slot, int syncId, int revision, int button, SlotActionType action, List<ItemStack> stacks) {
        if (MinecraftUtils.mc() == null || stacks == null) return;
        if (MinecraftUtils.mc().getNetworkHandler() == null || slot < 0 || slot >= stacks.size()) return;
        try {
            ComponentChangesHash.ComponentHasher hasher = MinecraftUtils.mc().getNetworkHandler().getComponentHasher();
            Int2ObjectOpenHashMap<ItemStackHash> modifiedStacks = new Int2ObjectOpenHashMap<ItemStackHash>();
            modifiedStacks.put(slot, ItemStackHash.fromItemStack(Items.AIR.getDefaultStack(), hasher));
            ItemStackHash clickedStack = ItemStackHash.fromItemStack(stacks.get(slot), hasher);
            MinecraftUtils.mc().getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                    syncId,
                    revision,
                    (short) slot,
                    (byte) button,
                    action,
                    modifiedStacks,
                    clickedStack
            ));
        } catch (Throwable ignored) {}
    }
}
