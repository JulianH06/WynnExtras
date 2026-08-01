package julianh06.wynnextras.features.wci.ui;

public final class WciMenuRenderPolicy {
    private WciMenuRenderPolicy() {}

    public static boolean shouldRenderLauncher(WciScreenContext context) {
        return context != null && context.supportsWciMenu();
    }

    public static boolean shouldRenderMenu(boolean menuOpen, WciScreenContext context, boolean pinnedByUser,
                                           boolean allowPersistentMenu) {
        if (!menuOpen || context == null) {
            return false;
        }
        if (context == WciScreenContext.BLOCKED_MODAL) {
            return false;
        }
        if (context.supportsWciMenu()) {
            return true;
        }
        return pinnedByUser || allowPersistentMenu;
    }

    public static boolean canOpenFromCommand(WciScreenContext context, boolean allowPersistentMenu) {
        return context != null
                && context != WciScreenContext.BLOCKED_MODAL
                && (context.supportsWciMenu() || allowPersistentMenu);
    }

    public static boolean shouldPinCommandOpen(WciScreenContext context, boolean allowPersistentMenu) {
        return context != null
                && context != WciScreenContext.BLOCKED_MODAL
                && !context.supportsWciMenu()
                && allowPersistentMenu;
    }

    public static WciRowPrimaryAction primaryRowAction(WciScreenContext context) {
        if (context == null || context == WciScreenContext.BLOCKED_MODAL) {
            return WciRowPrimaryAction.NONE;
        }
        return switch (context) {
            case TRADE_MARKET, BANK_OVERLAY, BANK_VANILLA, CRAFTING -> WciRowPrimaryAction.TRADE_MARKET_SEARCH;
            case TRADE_MARKET_FILTER, TRADE_MARKET_DETAIL, INVENTORY, CHAT, HUD, UNSUPPORTED -> WciRowPrimaryAction.COPY_ONLY;
            case BLOCKED_MODAL -> WciRowPrimaryAction.NONE;
        };
    }

    public static boolean allowsAutomatedTradeMarketSearch(WciScreenContext context) {
        return primaryRowAction(context) == WciRowPrimaryAction.TRADE_MARKET_SEARCH;
    }

    public static boolean allowsHaveCountRefresh(WciScreenContext context) {
        return context != null && context != WciScreenContext.BLOCKED_MODAL;
    }

    public static boolean shouldForwardClickToWci(WciScreenContext context, boolean insidePanel,
                                                  boolean insideLauncher, boolean menuOpen,
                                                  boolean pinnedByUser, boolean allowPersistentMenu) {
        if (context == WciScreenContext.BLOCKED_MODAL) {
            return false;
        }
        return (insideLauncher && shouldRenderLauncher(context))
                || (insidePanel && shouldRenderMenu(menuOpen, context, pinnedByUser, allowPersistentMenu));
    }

    public enum WciRowPrimaryAction {
        TRADE_MARKET_SEARCH,
        COPY_ONLY,
        NONE
    }
}
