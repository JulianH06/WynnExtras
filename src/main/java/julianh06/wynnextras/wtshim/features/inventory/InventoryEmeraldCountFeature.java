// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — InventoryEmeraldCountFeature.
 * Mixin target: InventoryEmeraldCountFeatureInvoker#invokeGetRenderableEmeraldAmounts.
 */
package julianh06.wynnextras.wtshim.features.inventory;

import julianh06.wynnextras.wtshim.core.consumers.features.Feature;
import julianh06.wynnextras.wtshim.models.emeralds.type.EmeraldUnits;

public class InventoryEmeraldCountFeature extends Feature {
    /**
     * Mixin target — produces a 4-element string array [EMERALD, BLOCK, LE, STACK_OF_LE] of
     * pretty-printed counts, matching Wynntils' unit split.
     */
    private String[] getRenderableEmeraldAmounts(int emeralds) {
        int stackOfLe = emeralds / EmeraldUnits.STACK_OF_LIQUID_EMERALD.getMultiplier();
        emeralds %= EmeraldUnits.STACK_OF_LIQUID_EMERALD.getMultiplier();
        int le = emeralds / EmeraldUnits.LIQUID_EMERALD.getMultiplier();
        emeralds %= EmeraldUnits.LIQUID_EMERALD.getMultiplier();
        int block = emeralds / EmeraldUnits.EMERALD_BLOCK.getMultiplier();
        int single = emeralds % EmeraldUnits.EMERALD_BLOCK.getMultiplier();
        return new String[] {
                String.valueOf(single),
                String.valueOf(block),
                String.valueOf(le),
                String.valueOf(stackOfLe)
        };
    }
}
