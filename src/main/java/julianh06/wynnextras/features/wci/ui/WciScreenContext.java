package julianh06.wynnextras.features.wci.ui;

import com.wynntils.core.components.Models;
import com.wynntils.models.containers.Container;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import julianh06.wynnextras.features.inventory.TradeMarketComparisonPanel;
import julianh06.wynnextras.features.wci.service.WciTextCleaner;
import julianh06.wynnextras.features.wci.service.WciTradeMarketSlotMatcher;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

import java.util.Locale;

public enum WciScreenContext {
    TRADE_MARKET,
    TRADE_MARKET_FILTER,
    TRADE_MARKET_DETAIL,
    BANK_OVERLAY,
    BANK_VANILLA,
    CRAFTING,
    BLOCKED_MODAL,
    UNSUPPORTED;

    public boolean supportsWciMenu() {
        return this == TRADE_MARKET
                || this == TRADE_MARKET_FILTER
                || this == TRADE_MARKET_DETAIL
                || this == BANK_OVERLAY
                || this == BANK_VANILLA
                || this == CRAFTING;
    }

    public WciPlacementContext placementContext() {
        return switch (this) {
            case TRADE_MARKET, TRADE_MARKET_FILTER, TRADE_MARKET_DETAIL -> WciPlacementContext.TRADE_MARKET;
            case BANK_OVERLAY -> WciPlacementContext.BANK_OVERLAY;
            case BANK_VANILLA -> WciPlacementContext.BANK_VANILLA;
            case CRAFTING, BLOCKED_MODAL, UNSUPPORTED -> WciPlacementContext.OTHER;
        };
    }

    public static WciScreenContext detect(HandledScreen<?> screen, boolean customBankOverlayActive) {
        if (screen == null) {
            return UNSUPPORTED;
        }
        Container container = Models.Container.getCurrentContainer();
        boolean tradeMarketMainScreen = hasTradeMarketSearchAndFilterSlot(screen);
        return fromSignals(
                screen.getTitle() == null ? "" : screen.getTitle().getString(),
                TradeMarketComparisonPanel.isInTradeMarket(),
                tradeMarketMainScreen,
                !tradeMarketMainScreen && hasTradeMarketFilterSignal(screen),
                !tradeMarketMainScreen && hasTradeMarketDetailSignal(screen),
                WciShoppingMenuLauncherButton.isBankLikeContainer(container),
                customBankOverlayActive,
                container instanceof CraftingStationContainer);
    }

    public static WciScreenContext fromSignals(String title, boolean tradeMarketScreen, boolean bankLikeScreen,
                                               boolean customBankOverlayActive, boolean craftingScreen) {
        return fromSignals(
                title,
                tradeMarketScreen,
                isTradeMarketMainTitle(title),
                isTradeMarketFilterTitle(title),
                isTradeMarketDetailTitle(title),
                bankLikeScreen,
                customBankOverlayActive,
                craftingScreen);
    }

    public static WciScreenContext fromSignals(String title, boolean tradeMarketScreen,
                                               boolean tradeMarketMainScreen,
                                               boolean tradeMarketFilterScreen,
                                               boolean tradeMarketDetailScreen,
                                               boolean bankLikeScreen,
                                               boolean customBankOverlayActive,
                                               boolean craftingScreen) {
        if (isExplicitlyUnsupportedTitle(title)) {
            return BLOCKED_MODAL;
        }
        if (tradeMarketScreen || tradeMarketMainScreen || tradeMarketFilterScreen || tradeMarketDetailScreen) {
            if (tradeMarketMainScreen) {
                return TRADE_MARKET;
            }
            if (tradeMarketFilterScreen) {
                return TRADE_MARKET_FILTER;
            }
            if (tradeMarketDetailScreen) {
                return TRADE_MARKET_DETAIL;
            }
            return TRADE_MARKET_DETAIL;
        }
        if (bankLikeScreen || customBankOverlayActive) {
            return customBankOverlayActive ? BANK_OVERLAY : BANK_VANILLA;
        }
        if (craftingScreen) {
            return CRAFTING;
        }
        return UNSUPPORTED;
    }

    public static boolean isExplicitlyUnsupportedTitle(String title) {
        String normalized = normalizeTitle(title);
        return normalized.equals("do you want to leave?")
                || normalized.equals("are you sure?")
                || normalized.equals("confirm")
                || normalized.equals("confirmation");
    }

    private static String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static boolean isTradeMarketMainTitle(String title) {
        String normalized = normalizeTitle(title);
        return normalized.equals("trade market")
                || normalized.contains("browse")
                || normalized.contains("search results")
                || normalized.contains("your trades");
    }

    private static boolean isTradeMarketFilterTitle(String title) {
        String normalized = normalizeTitle(title);
        return normalized.contains("filter")
                || normalized.contains("search and filter")
                || normalized.contains("search & filter")
                || normalized.contains("search/filter");
    }

    private static boolean isTradeMarketDetailTitle(String title) {
        String normalized = normalizeTitle(title);
        return normalized.contains("view trade")
                || normalized.contains("item listing")
                || normalized.contains("confirm purchase")
                || normalized.contains("purchase")
                || normalized.contains("buy order")
                || normalized.contains("sell order");
    }

    private static boolean hasTradeMarketSearchAndFilterSlot(HandledScreen<?> screen) {
        if (screen == null || screen.getScreenHandler() == null) {
            return false;
        }
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot != null && slot.hasStack() && isSearchAndFilterLabel(slot.getStack())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTradeMarketFilterSignal(HandledScreen<?> screen) {
        if (screen == null || screen.getScreenHandler() == null) {
            return false;
        }
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot == null || !slot.hasStack()) {
                continue;
            }
            String label = cleanSlotLabel(slot.getStack());
            if (label.contains("filter") || label.contains("sort")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTradeMarketDetailSignal(HandledScreen<?> screen) {
        if (screen == null || screen.getScreenHandler() == null) {
            return false;
        }
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot == null || !slot.hasStack()) {
                continue;
            }
            String label = cleanSlotLabel(slot.getStack());
            if (label.contains("view trade")
                    || label.equals("buy")
                    || label.contains("buy now")
                    || label.contains("confirm purchase")
                    || label.contains("claim items")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSearchAndFilterLabel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return WciTradeMarketSlotMatcher.isSearchFilterLabel(stack.getName().getString());
    }

    private static String cleanSlotLabel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return WciTextCleaner.clean(stack.getName().getString()).toLowerCase(Locale.ROOT);
    }
}
