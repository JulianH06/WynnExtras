package julianh06.wynnextras.features.wci.service;

public enum WciTradeMarketWorkflowState {
    IDLE,
    OPENING_TRADE_MARKET,
    SEARCH_SLOT_CLICKED,
    WAITING_FOR_SEARCH_PROMPT,
    SENDING_SEARCH_QUERY,
    WAITING_FOR_RESULTS,
    COMPLETE,
    FAILED,
    TIMED_OUT
}
