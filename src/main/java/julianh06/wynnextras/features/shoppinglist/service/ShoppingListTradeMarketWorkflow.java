package julianh06.wynnextras.features.shoppinglist.service;

import java.util.Optional;

public final class ShoppingListTradeMarketWorkflow {
    private ShoppingListTradeMarketWorkflowState state = ShoppingListTradeMarketWorkflowState.IDLE;
    private String pendingItemName = "";
    private long startedAtMs = 0L;
    private String lastStatusMessage = "";

    public ShoppingListTradeMarketWorkflowState state() {
        return state;
    }

    public String pendingItemName() {
        return pendingItemName;
    }

    public String lastStatusMessage() {
        return lastStatusMessage;
    }

    public boolean isActive() {
        return state == ShoppingListTradeMarketWorkflowState.OPENING_TRADE_MARKET
                || state == ShoppingListTradeMarketWorkflowState.SEARCH_SLOT_CLICKED
                || state == ShoppingListTradeMarketWorkflowState.WAITING_FOR_SEARCH_PROMPT
                || state == ShoppingListTradeMarketWorkflowState.SENDING_SEARCH_QUERY
                || state == ShoppingListTradeMarketWorkflowState.WAITING_FOR_RESULTS;
    }

    public boolean canClickSearchSlot() {
        return state == ShoppingListTradeMarketWorkflowState.OPENING_TRADE_MARKET;
    }

    public void beginSearch(String itemName, long nowMs) {
        pendingItemName = itemName == null ? "" : itemName.trim();
        transition(ShoppingListTradeMarketWorkflowState.OPENING_TRADE_MARKET, nowMs,
                "Opening TM for " + compactItemName());
    }

    public void markSearchSlotClicked(long nowMs) {
        transition(ShoppingListTradeMarketWorkflowState.SEARCH_SLOT_CLICKED, nowMs,
                "Opening TM search for " + compactItemName());
    }

    public void markWaitingForSearchPrompt(long nowMs) {
        transition(ShoppingListTradeMarketWorkflowState.WAITING_FOR_SEARCH_PROMPT, nowMs,
                "Opening TM search for " + compactItemName());
    }

    public Optional<String> acceptSearchPrompt(long nowMs) {
        if (pendingItemName.isBlank()
                || (state != ShoppingListTradeMarketWorkflowState.OPENING_TRADE_MARKET
                && state != ShoppingListTradeMarketWorkflowState.SEARCH_SLOT_CLICKED
                && state != ShoppingListTradeMarketWorkflowState.WAITING_FOR_SEARCH_PROMPT)) {
            return Optional.empty();
        }
        transition(ShoppingListTradeMarketWorkflowState.SENDING_SEARCH_QUERY, nowMs,
                "Searching TM for " + compactItemName());
        return Optional.of(pendingItemName);
    }

    public void markSearchQuerySent(long nowMs) {
        transition(ShoppingListTradeMarketWorkflowState.WAITING_FOR_RESULTS, nowMs,
                "Searching TM for " + compactItemName());
    }

    public void markResultsVisible(long nowMs) {
        transition(ShoppingListTradeMarketWorkflowState.COMPLETE, nowMs,
                "Searching TM for " + compactItemName());
    }

    public void markFailed(long nowMs, String statusMessage) {
        transition(ShoppingListTradeMarketWorkflowState.FAILED, nowMs,
                statusMessage == null || statusMessage.isBlank() ? "Trade Market search unavailable" : statusMessage);
    }

    public Optional<String> timeoutStatusIfTimedOut(long nowMs, long timeoutMs) {
        if (state == ShoppingListTradeMarketWorkflowState.IDLE
                || state == ShoppingListTradeMarketWorkflowState.FAILED
                || state == ShoppingListTradeMarketWorkflowState.TIMED_OUT
                || state == ShoppingListTradeMarketWorkflowState.COMPLETE
                || timeoutMs <= 0L
                || nowMs - startedAtMs <= timeoutMs) {
            return Optional.empty();
        }
        transition(ShoppingListTradeMarketWorkflowState.TIMED_OUT, nowMs, "TM search timed out");
        return Optional.of(lastStatusMessage);
    }

    public void reset() {
        state = ShoppingListTradeMarketWorkflowState.IDLE;
        pendingItemName = "";
        startedAtMs = 0L;
        lastStatusMessage = "";
    }

    private void transition(ShoppingListTradeMarketWorkflowState nextState, long nowMs, String statusMessage) {
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
