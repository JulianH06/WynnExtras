package julianh06.wynnextras.features.shoppinglist.ui;

public final class ShoppingListMenuRenderPolicy {
    private ShoppingListMenuRenderPolicy() {}

    public static boolean shouldRenderLauncher(ShoppingListScreenContext context) {
        return context != null;
    }

    public static boolean shouldRenderMenu(boolean menuOpen, ShoppingListScreenContext context) {
        return menuOpen && context != null;
    }

    public static boolean canOpenFromCommand(ShoppingListScreenContext context) {
        return context != null;
    }

    public static ShoppingListRowPrimaryAction primaryRowAction(ShoppingListScreenContext context) {
        if (context == null) {
            return ShoppingListRowPrimaryAction.NONE;
        }
        return switch (context) {
            case BANK_OVERLAY -> ShoppingListRowPrimaryAction.BANK_OVERLAY_SEARCH;
            case TRADE_MARKET, BANK_VANILLA, CRAFTING -> ShoppingListRowPrimaryAction.TRADE_MARKET_SEARCH;
            case TRADE_MARKET_FILTER, TRADE_MARKET_DETAIL, INVENTORY, CHAT, HUD, BLOCKED_MODAL, UNSUPPORTED -> ShoppingListRowPrimaryAction.COPY_ONLY;
        };
    }

    public static boolean allowsAutomatedTradeMarketSearch(ShoppingListScreenContext context) {
        return primaryRowAction(context) == ShoppingListRowPrimaryAction.TRADE_MARKET_SEARCH;
    }

    public static boolean allowsHaveCountRefresh(ShoppingListScreenContext context) {
        return context != null;
    }

    public static boolean shouldForwardClickToShoppingList(ShoppingListScreenContext context, boolean insidePanel,
                                                  boolean insideLauncher, boolean menuOpen) {
        return (insideLauncher && shouldRenderLauncher(context))
                || (insidePanel && shouldRenderMenu(menuOpen, context));
    }

    public enum ShoppingListRowPrimaryAction {
        BANK_OVERLAY_SEARCH,
        TRADE_MARKET_SEARCH,
        COPY_ONLY,
        NONE
    }
}
