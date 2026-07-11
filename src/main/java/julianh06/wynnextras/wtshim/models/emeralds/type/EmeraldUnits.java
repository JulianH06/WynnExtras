// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — EmeraldUnits enum. */
package julianh06.wynnextras.wtshim.models.emeralds.type;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public enum EmeraldUnits {
    EMERALD(1, "\u00B2\u00B4\u00B7", Items.EMERALD),
    EMERALD_BLOCK(64, "\u00BA\u00B6\u00B8", Items.EMERALD_BLOCK),
    LIQUID_EMERALD(4096, "\u00B5\u00B9\u00BB", Items.EXPERIENCE_BOTTLE),
    STACK_OF_LIQUID_EMERALD(262144, "", Items.EXPERIENCE_BOTTLE);

    private final int multiplier;
    private final String symbol;
    private final net.minecraft.item.Item item;

    EmeraldUnits(int multiplier, String symbol, net.minecraft.item.Item item) {
        this.multiplier = multiplier;
        this.symbol = symbol;
        this.item = item;
    }

    public int getMultiplier() { return multiplier; }
    public String getSymbol() { return symbol; }
    public ItemStack getItemStack() { return new ItemStack(item); }
}
