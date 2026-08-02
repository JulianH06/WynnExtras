package julianh06.wynnextras.features.shoppinglist.ui;

public enum ShoppingListPlacementContext {
    TRADE_MARKET,
    BANK_OVERLAY,
    BANK_VANILLA,
    OTHER;

    public static ShoppingListPlacementContext from(boolean tradeMarketScreen, boolean bankLikeScreen,
                                           boolean customBankOverlayActive) {
        if (tradeMarketScreen) {
            return TRADE_MARKET;
        }
        if (customBankOverlayActive) {
            return BANK_OVERLAY;
        }
        if (bankLikeScreen) {
            return BANK_VANILLA;
        }
        return OTHER;
    }
}
