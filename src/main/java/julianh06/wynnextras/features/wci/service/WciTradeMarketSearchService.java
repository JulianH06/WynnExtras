package julianh06.wynnextras.features.wci.service;

import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.api.WEEventBus;
import julianh06.wynnextras.features.wci.compat.WciWynnMarketSearchCompat;
import julianh06.wynnextras.features.wci.ui.WciScreenContext;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.neoforged.bus.api.SubscribeEvent;

public final class WciTradeMarketSearchService {
    private static final String SEARCH_PROMPT = "Type the item name or type 'cancel' to cancel:";
    private static final int LEGACY_SEARCH_SLOT = 47;
    private static final long WORKFLOW_TIMEOUT_MS = 30_000L;
    private static final long SAME_QUERY_DEBOUNCE_MS = 750L;
    private static final WciTradeMarketWorkflow WORKFLOW = new WciTradeMarketWorkflow();

    private static boolean registered = false;
    private static long statusRevision = 0L;
    private static Result latestResult = new Result(Status.IDLE, "", "", 0L);
    private static String lastStartedQuery = "";
    private static long lastStartedAtMs = 0L;
    private static WciWynnMarketSearchCompat.SuppressionHandle wynnMarketSearchSuppression = null;

    private WciTradeMarketSearchService() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        WEEventBus.registerEventListener(new ChatListener());
        ClientTickEvents.END_CLIENT_TICK.register(WciTradeMarketSearchService::tick);
    }

    public static Result searchOrCopy(String query) {
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery.isEmpty()) {
            return publish(Status.FAILED, normalizedQuery);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.keyboard == null) {
            return publish(Status.FAILED, normalizedQuery);
        }

        client.keyboard.setClipboard(normalizedQuery);
        if (client.player == null || client.player.networkHandler == null) {
            return publish(Status.FAILED, normalizedQuery);
        }

        long now = System.currentTimeMillis();
        if (WORKFLOW.isActive()) {
            return publish(Status.ALREADY_RUNNING, WORKFLOW.pendingItemName().isBlank()
                    ? normalizedQuery
                    : WORKFLOW.pendingItemName());
        }
        if (normalizedQuery.equalsIgnoreCase(lastStartedQuery)
                && now - lastStartedAtMs < SAME_QUERY_DEBOUNCE_MS) {
            return publish(Status.ALREADY_RUNNING, normalizedQuery);
        }

        WciScreenContext context = currentScreenContext(client);
        if (context == WciScreenContext.TRADE_MARKET_FILTER
                || context == WciScreenContext.TRADE_MARKET_DETAIL
                || context == WciScreenContext.BLOCKED_MODAL) {
            return publish(Status.COPIED_FOR_TRADE_MARKET, normalizedQuery);
        }

        lastStartedQuery = normalizedQuery;
        lastStartedAtMs = now;
        suppressWynnMarketSearch();
        WciCursorRestoreService.recordBeforeWciTradeMarketSearch();
        WORKFLOW.beginSearch(normalizedQuery, now);
        publish(Status.OPENING_TRADE_MARKET, normalizedQuery);

        if (context == WciScreenContext.TRADE_MARKET) {
            if (tryClickTradeMarketSearchSlot(client, now)) {
                return latestResult;
            }
            WORKFLOW.markFailed(now, "Trade Market search unavailable");
            return publish(Status.FAILED, normalizedQuery);
        }

        client.player.networkHandler.sendChatCommand("trade market");
        return latestResult;
    }

    public static Result copied(String query) {
        String normalizedQuery = normalizeQuery(query);
        if (!normalizedQuery.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.keyboard != null) {
                client.keyboard.setClipboard(normalizedQuery);
            }
        }
        return result(normalizedQuery.isEmpty() ? Status.FAILED : Status.COPIED, normalizedQuery);
    }

    public static Result latestResult() {
        return latestResult;
    }

    public static boolean isWorkflowActive() {
        return WORKFLOW.isActive();
    }

    public static Result activeWorkflowResult() {
        if (!WORKFLOW.isActive()) {
            return latestResult;
        }
        return result(Status.ALREADY_RUNNING, WORKFLOW.pendingItemName());
    }

    public static long statusRevision() {
        return statusRevision;
    }

    public static boolean isSearchPrompt(String message) {
        return WciTextCleaner.clean(message).contains(SEARCH_PROMPT);
    }

    public static Result result(Status status, String query) {
        String normalizedQuery = normalizeQuery(query);
        return new Result(status, normalizedQuery, statusMessage(status, normalizedQuery), statusRevision);
    }

    public static String statusMessage(Status status, String query) {
        String compactQuery = compactQuery(query);
        return switch (status) {
            case OPENING_TRADE_MARKET -> "Opening TM for " + compactQuery;
            case OPENING_SEARCH -> "Opening TM search for " + compactQuery;
            case SEARCHED -> "Searching TM for " + compactQuery;
            case ALREADY_RUNNING -> "TM search already running";
            case COPIED -> "Copied " + compactQuery;
            case COPIED_FOR_TRADE_MARKET -> "Copied " + compactQuery + "; open TM";
            case NOT_TRADE_MARKET -> "Copied " + compactQuery + "; open TM";
            case TIMED_OUT -> "TM search timed out";
            case FAILED -> "Trade Market search unavailable";
            case IDLE -> "";
        };
    }

    private static void tick(MinecraftClient client) {
        if (client == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (WORKFLOW.isActive() && (client.player == null || client.player.networkHandler == null)) {
            WORKFLOW.markFailed(now, "Trade Market search unavailable");
            publish(Status.FAILED, WORKFLOW.pendingItemName());
            return;
        }

        if (WORKFLOW.state() == WciTradeMarketWorkflowState.OPENING_TRADE_MARKET
                && currentScreenContext(client) == WciScreenContext.TRADE_MARKET) {
            tryClickTradeMarketSearchSlot(client, now);
        }

        if (WORKFLOW.timeoutStatusIfTimedOut(now, WORKFLOW_TIMEOUT_MS).isPresent()) {
            publish(Status.TIMED_OUT, "");
            return;
        }

        WciCursorRestoreService.TickResult cursorRestoreResult = WciCursorRestoreService.tick(client);
        if ((cursorRestoreResult == WciCursorRestoreService.TickResult.RESTORED
                || cursorRestoreResult == WciCursorRestoreService.TickResult.TIMED_OUT)
                && WORKFLOW.state() == WciTradeMarketWorkflowState.WAITING_FOR_RESULTS) {
            WORKFLOW.markResultsVisible(now);
        }
    }

    private static boolean tryClickTradeMarketSearchSlot(MinecraftClient client, long now) {
        if (!WORKFLOW.canClickSearchSlot()) {
            return false;
        }
        if (!(client.currentScreen instanceof HandledScreen<?> screen)
                || currentScreenContext(client) != WciScreenContext.TRADE_MARKET
                || client.interactionManager == null
                || client.player == null) {
            return false;
        }

        ScreenHandler handler = screen.getScreenHandler();
        int searchSlotIndex = findSearchSlotIndex(client, handler);
        if (searchSlotIndex < 0) {
            return false;
        }

        WORKFLOW.markSearchSlotClicked(now);
        publish(Status.OPENING_SEARCH, WORKFLOW.pendingItemName());
        client.interactionManager.clickSlot(handler.syncId, searchSlotIndex, 0, SlotActionType.PICKUP, client.player);
        WORKFLOW.markWaitingForSearchPrompt(now);
        return true;
    }

    private static int findSearchSlotIndex(MinecraftClient client, ScreenHandler handler) {
        if (handler == null) {
            return -1;
        }
        if (isValidSearchSlot(client, handler, LEGACY_SEARCH_SLOT)) {
            return LEGACY_SEARCH_SLOT;
        }

        for (int slotIndex = 0; slotIndex < handler.slots.size(); slotIndex++) {
            if (slotIndex != LEGACY_SEARCH_SLOT && isValidSearchSlot(client, handler, slotIndex)) {
                return slotIndex;
            }
        }
        return -1;
    }

    private static boolean isValidSearchSlot(MinecraftClient client, ScreenHandler handler, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= handler.slots.size()) {
            return false;
        }
        Slot slot = handler.slots.get(slotIndex);
        if (client.player != null && slot.inventory == client.player.getInventory()) {
            return false;
        }
        if (!slot.hasStack()) {
            return false;
        }
        ItemStack stack = slot.getStack();
        return stack != null
                && !stack.isEmpty()
                && WciTradeMarketSlotMatcher.isSearchFilterLabel(stack.getName().getString());
    }

    private static WciScreenContext currentScreenContext(MinecraftClient client) {
        if (client != null && client.currentScreen instanceof HandledScreen<?> screen) {
            return WciScreenContext.detect(screen, false);
        }
        return WciScreenContext.UNSUPPORTED;
    }

    private static void handleChatMessage(String message) {
        if (!isSearchPrompt(message)) {
            return;
        }

        long now = System.currentTimeMillis();
        var searchName = WORKFLOW.acceptSearchPrompt(now);
        if (searchName.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null && client.player.networkHandler != null) {
            String name = searchName.get();
            client.player.networkHandler.sendChatMessage(name);
            boolean cursorRestoreArmed = WciCursorRestoreService.armForSearchResults();
            WORKFLOW.markSearchQuerySent(now);
            publish(Status.SEARCHED, name);
            releaseWynnMarketSearchSuppression();
            if (!cursorRestoreArmed) {
                WORKFLOW.markResultsVisible(now);
            }
            return;
        }

        WORKFLOW.markFailed(now, "Trade Market search unavailable");
        publish(Status.FAILED, WORKFLOW.pendingItemName());
    }

    private static Result publish(Status status, String query) {
        if (status == Status.FAILED || status == Status.TIMED_OUT) {
            WciCursorRestoreService.clearPending();
            releaseWynnMarketSearchSuppression();
            lastStartedQuery = "";
            lastStartedAtMs = 0L;
        }
        latestResult = new Result(status, normalizeQuery(query), statusMessage(status, query), ++statusRevision);
        return latestResult;
    }

    private static String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private static String compactQuery(String query) {
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery.length() <= 28) {
            return normalizedQuery.isEmpty() ? "item" : normalizedQuery;
        }
        return normalizedQuery.substring(0, 25) + "...";
    }

    private static void suppressWynnMarketSearch() {
        releaseWynnMarketSearchSuppression();
        wynnMarketSearchSuppression = WciWynnMarketSearchCompat.suppressDuringWciSearch();
    }

    private static void releaseWynnMarketSearchSuppression() {
        if (wynnMarketSearchSuppression == null) {
            return;
        }
        wynnMarketSearchSuppression.close();
        wynnMarketSearchSuppression = null;
    }

    public enum Status {
        IDLE,
        OPENING_TRADE_MARKET,
        OPENING_SEARCH,
        SEARCHED,
        ALREADY_RUNNING,
        COPIED,
        COPIED_FOR_TRADE_MARKET,
        NOT_TRADE_MARKET,
        TIMED_OUT,
        FAILED
    }

    public record Result(Status status, String query, String message, long revision) {
        public boolean error() {
            return status == Status.FAILED || status == Status.TIMED_OUT;
        }
    }

    private static final class ChatListener {
        @SubscribeEvent
        public void onChat(ChatEvent event) {
            if (event != null && event.message != null) {
                handleChatMessage(event.message.getString());
            }
        }
    }
}
