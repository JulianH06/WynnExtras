package julianh06.wynnextras.features.shoppinglist.ui;

public final class ShoppingListQuantityFormatter {
    private static final int STACK_SIZE = 64;

    private ShoppingListQuantityFormatter() {}

    public static String formatStacks(int count) {
        int safeCount = Math.max(0, count);
        if (safeCount < STACK_SIZE) {
            return Integer.toString(safeCount);
        }

        int stacks = safeCount / STACK_SIZE;
        int remainder = safeCount % STACK_SIZE;
        if (remainder == 0) {
            return stacks + " stx";
        }
        return stacks + " stx + " + remainder;
    }

    public static String formatStackBreakdown(int count) {
        int safeCount = Math.max(0, count);
        int stacks = safeCount / STACK_SIZE;
        int remainder = safeCount % STACK_SIZE;
        if (remainder == 0) {
            return stacks + " stx";
        }
        return stacks + " stx + " + remainder;
    }
}
