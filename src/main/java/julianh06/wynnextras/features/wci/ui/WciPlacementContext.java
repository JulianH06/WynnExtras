package julianh06.wynnextras.features.wci.ui;

public enum WciPlacementContext {
    TRADE_MARKET,
    BANK_OVERLAY,
    BANK_VANILLA,
    OTHER;

    public static WciPlacementContext from(boolean tradeMarketScreen, boolean bankLikeScreen,
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
