// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — EmeraldModel.
 *
 * Counts emerald-unit items (EMERALD, EMERALD_BLOCK, EXPERIENCE_BOTTLE used as Liquid Emerald)
 * in the currently-open screen handler, weighted by Wynncraft's unit values.
 *
 * Does NOT read emerald-pouch interior balances — that requires item-lore parsing (Phase 5+).
 */
package julianh06.wynnextras.wtshim.models.emeralds;

import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.models.emeralds.type.EmeraldUnits;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class EmeraldModel extends Model {
    public int getAmountInContainer() {
        ScreenHandler menu = McUtils.containerMenu();
        if (menu == null) return 0;

        int total = 0;
        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;
            int multiplier = multiplierFor(stack.getItem());
            if (multiplier == 0) continue;
            total += stack.getCount() * multiplier;
        }
        return total;
    }

    private static int multiplierFor(Item item) {
        if (item == Items.EMERALD) return EmeraldUnits.EMERALD.getMultiplier();
        if (item == Items.EMERALD_BLOCK) return EmeraldUnits.EMERALD_BLOCK.getMultiplier();
        if (item == Items.EXPERIENCE_BOTTLE) return EmeraldUnits.LIQUID_EMERALD.getMultiplier();
        return 0;
    }
}
