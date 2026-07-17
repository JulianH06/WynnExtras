package julianh06.wynnextras.features.wci.service;

import julianh06.wynnextras.features.wci.cart.ShoppingEntry;
import julianh06.wynnextras.features.wci.model.RequirementType;

public final class WciRequirementCalculator {
    private WciRequirementCalculator() {}

    public static int adjustedRequired(ShoppingEntry entry, int baseRequired, int outputCount,
                                       boolean professionSpeedEnabled) {
        int requiredPerCraft = Math.max(0, baseRequired);
        if (entry != null
                && entry.type() == RequirementType.MATERIAL
                && professionSpeedEnabled) {
            requiredPerCraft /= 2;
        }
        long adjustedRequired = (long) requiredPerCraft * sanitizeOutputCount(outputCount);
        return adjustedRequired > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) adjustedRequired;
    }

    public static int sanitizeMultiplier(int multiplier) {
        return sanitizeOutputCount(multiplier);
    }

    public static int sanitizeOutputCount(int outputCount) {
        return Math.max(1, outputCount);
    }

    public static int outputClickIncrement(boolean shiftDown) {
        return shiftDown ? 10 : 1;
    }

    public static int addOutputs(int currentOutputCount, int increment) {
        long next = (long) sanitizeOutputCount(currentOutputCount) + Math.max(0, increment);
        return next > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) next;
    }

    public static int subtractOutputs(int currentOutputCount, int decrement) {
        long next = (long) sanitizeOutputCount(currentOutputCount) - Math.max(0, decrement);
        return (int) Math.max(1, next);
    }
}
