package julianh06.wynnextras.features.shoppinglist.ui;

public final class ShoppingListMenuRenderPolicy {
    private ShoppingListMenuRenderPolicy() {}

    public static boolean shouldRenderLauncher(ShoppingListScreenContext context) {
        return context != null && context.supportsShoppingListMenu();
    }

    public static boolean shouldRenderMenu(boolean menuOpen, ShoppingListScreenContext context, boolean pinnedByUser,
                                           boolean allowPersistentMenu) {
        if (!menuOpen || context == null) {
            return false;
        }
        if (context == ShoppingListScreenContext.BLOCKED_MODAL) {
            return false;
        }
        if (context.supportsShoppingListMenu()) {
            return true;
        }
        return pinnedByUser || allowPersistentMenu;
    }

    public static boolean canOpenFromCommand(ShoppingListScreenContext context, boolean allowPersistentMenu) {
        return context != null
                && context != ShoppingListScreenContext.BLOCKED_MODAL
                && (context.supportsShoppingListMenu() || allowPersistentMenu);
    }

    public static boolean shouldPinCommandOpen(ShoppingListScreenContext context, boolean allowPersistentMenu) {
        return context != null
                && context != ShoppingListScreenContext.BLOCKED_MODAL
                && !context.supportsShoppingListMenu()
                && allowPersistentMenu;
    }

    public static ShoppingListRowPrimaryAction primaryRowAction(ShoppingListScreenContext context) {
        if (context == null || context == ShoppingListScreenContext.BLOCKED_MODAL) {
            return ShoppingListRowPrimaryAction.NONE;
        }
        return switch (context) {
            case BANK_OVERLAY -> ShoppingListRowPrimaryAction.BANK_OVERLAY_SEARCH;
            case TRADE_MARKET, BANK_VANILLA, CRAFTING -> ShoppingListRowPrimaryAction.TRADE_MARKET_SEARCH;
            case TRADE_MARKET_FILTER, TRADE_MARKET_DETAIL, INVENTORY, CHAT, HUD, UNSUPPORTED -> ShoppingListRowPrimaryAction.COPY_ONLY;
            case BLOCKED_MODAL -> ShoppingListRowPrimaryAction.NONE;
        };
    }

    public static boolean allowsAutomatedTradeMarketSearch(ShoppingListScreenContext context) {
        return primaryRowAction(context) == ShoppingListRowPrimaryAction.TRADE_MARKET_SEARCH;
    }

    public static boolean allowsHaveCountRefresh(ShoppingListScreenContext context) {
        return context != null && context != ShoppingListScreenContext.BLOCKED_MODAL;
    }

    public static boolean shouldForwardClickToShoppingList(ShoppingListScreenContext context, boolean insidePanel,
                                                  boolean insideLauncher, boolean menuOpen,
                                                  boolean pinnedByUser, boolean allowPersistentMenu) {
        if (context == ShoppingListScreenContext.BLOCKED_MODAL) {
            return false;
        }
        return (insideLauncher && shouldRenderLauncher(context))
                || (insidePanel && shouldRenderMenu(menuOpen, context, pinnedByUser, allowPersistentMenu));
    }

    public enum ShoppingListRowPrimaryAction {
        BANK_OVERLAY_SEARCH,
        TRADE_MARKET_SEARCH,
        COPY_ONLY,
        NONE
    }
}
