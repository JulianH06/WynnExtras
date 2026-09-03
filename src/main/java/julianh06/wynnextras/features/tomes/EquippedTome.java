package julianh06.wynnextras.features.tomes;

import net.minecraft.item.ItemStack;

public record EquippedTome(int slot, TomeType type, ItemStack stack) {
    public EquippedTome {
        stack = stack == null ? ItemStack.EMPTY : stack.copy();
    }
}
