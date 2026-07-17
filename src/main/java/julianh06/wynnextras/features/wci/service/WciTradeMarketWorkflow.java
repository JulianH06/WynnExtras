package julianh06.wynnextras.features.wci.service;

import java.util.Optional;

public final class WciTradeMarketWorkflow {
    private WciTradeMarketWorkflowState state = WciTradeMarketWorkflowState.IDLE;
    private String pendingItemName = "";
    private long startedAtMs = 0L;
    private String lastStatusMessage = "";

    public WciTradeMarketWorkflowState state() {
        return state;
    }

    public String pendingItemName() {
        return pendingItemName;
    }

    public String lastStatusMessage() {
        return lastStatusMessage;
    }

    public boolean isActive() {
        return state == WciTradeMarketWorkflowState.OPENING_TRADE_MARKET
                || state == WciTradeMarketWorkflowState.SEARCH_SLOT_CLICKED
                || state == WciTradeMarketWorkflowState.WAITING_FOR_SEARCH_PROMPT
                || state == WciTradeMarketWorkflowState.SENDING_SEARCH_QUERY
                || state == WciTradeMarketWorkflowState.WAITING_FOR_RESULTS;
    }

    public boolean canClickSearchSlot() {
        return state == WciTradeMarketWorkflowState.OPENING_TRADE_MARKET;
    }

    public void beginSearch(String itemName, long nowMs) {
        pendingItemName = itemName == null ? "" : itemName.trim();
        transition(WciTradeMarketWorkflowState.OPENING_TRADE_MARKET, nowMs,
                "Opening TM for " + compactItemName());
    }

    public void markSearchSlotClicked(long nowMs) {
        transition(WciTradeMarketWorkflowState.SEARCH_SLOT_CLICKED, nowMs,
                "Opening TM search for " + compactItemName());
    }

    public void markWaitingForSearchPrompt(long nowMs) {
        transition(WciTradeMarketWorkflowState.WAITING_FOR_SEARCH_PROMPT, nowMs,
                "Opening TM search for " + compactItemName());
    }

    public Optional<String> acceptSearchPrompt(long nowMs) {
        if (pendingItemName.isBlank()
                || (state != WciTradeMarketWorkflowState.OPENING_TRADE_MARKET
                && state != WciTradeMarketWorkflowState.SEARCH_SLOT_CLICKED
                && state != WciTradeMarketWorkflowState.WAITING_FOR_SEARCH_PROMPT)) {
            return Optional.empty();
        }
        transition(WciTradeMarketWorkflowState.SENDING_SEARCH_QUERY, nowMs,
                "Searching TM for " + compactItemName());
        return Optional.of(pendingItemName);
    }

    public void markSearchQuerySent(long nowMs) {
        transition(WciTradeMarketWorkflowState.WAITING_FOR_RESULTS, nowMs,
                "Searching TM for " + compactItemName());
    }

    public void markResultsVisible(long nowMs) {
        transition(WciTradeMarketWorkflowState.COMPLETE, nowMs,
                "Searching TM for " + compactItemName());
    }

    public void markFailed(long nowMs, String statusMessage) {
        transition(WciTradeMarketWorkflowState.FAILED, nowMs,
                statusMessage == null || statusMessage.isBlank() ? "Trade Market search unavailable" : statusMessage);
    }

    public Optional<String> timeoutStatusIfTimedOut(long nowMs, long timeoutMs) {
        if (state == WciTradeMarketWorkflowState.IDLE
                || state == WciTradeMarketWorkflowState.FAILED
                || state == WciTradeMarketWorkflowState.TIMED_OUT
                || state == WciTradeMarketWorkflowState.COMPLETE
                || timeoutMs <= 0L
                || nowMs - startedAtMs <= timeoutMs) {
            return Optional.empty();
        }
        transition(WciTradeMarketWorkflowState.TIMED_OUT, nowMs, "TM search timed out");
        return Optional.of(lastStatusMessage);
    }

    public void reset() {
        state = WciTradeMarketWorkflowState.IDLE;
        pendingItemName = "";
        startedAtMs = 0L;
        lastStatusMessage = "";
    }

    private void transition(WciTradeMarketWorkflowState nextState, long nowMs, String statusMessage) {
        state = nextState;
        startedAtMs = nowMs;
        lastStatusMessage = statusMessage == null ? "" : statusMessage;
    }

    private String compactItemName() {
        if (pendingItemName.isBlank()) {
            return "item";
        }
        if (pendingItemName.length() <= 28) {
            return pendingItemName;
        }
        return pendingItemName.substring(0, 25) + "...";
    }
}
