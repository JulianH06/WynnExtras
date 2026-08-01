package julianh06.wynnextras.features.wci.ui;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.wci.WynnExtrasWciFeature;
import julianh06.wynnextras.features.wci.cart.ShoppingEntry;
import julianh06.wynnextras.features.wci.model.RequirementType;
import julianh06.wynnextras.features.wci.service.ShoppingCartService;
import julianh06.wynnextras.features.wci.service.WciHaveCountService;
import julianh06.wynnextras.features.wci.service.WciImportStatusFormatter;
import julianh06.wynnextras.features.wci.service.WciRequirementCalculator;
import julianh06.wynnextras.features.wci.service.WciTradeMarketSearchService;
import julianh06.wynnextras.utils.HandledScreenAccess;
import julianh06.wynnextras.utils.UI.WEMenuExtension;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class WciShoppingMenuExtension extends WEMenuExtension {
    private static final int PANEL_WIDTH = 190;
    private static final int PANEL_HEIGHT = 222;
    private static final int PANEL_MARGIN = 6;
    private static final int BANK_OVERLAY_DEFAULT_X = 736;
    private static final int BANK_OVERLAY_DEFAULT_Y = 63;
    private static final int BANK_VANILLA_DEFAULT_X = 606;
    private static final int BANK_VANILLA_DEFAULT_Y = 149;
    private static final int BUTTON_HEIGHT = 16;
    private static final int CLOSE_BUTTON_SIZE = 14;
    private static final int CLOSE_BUTTON_RIGHT_MARGIN = 5;
    private static final int CLOSE_BUTTON_TOP_MARGIN = 4;
    private static final int ROW_HEIGHT = 12;
    private static final int LIST_HEADER_Y_OFFSET = 64;
    private static final int LIST_DIVIDER_Y_OFFSET = 72;
    private static final int LIST_TOP_Y_OFFSET = 74;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_GAP = 3;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 16;
    private static final int HEADER_DRAG_HEIGHT = 22;
    private static final float TITLE_SCALE = 1.0f;
    private static final float TEXT_SCALE = 0.75f;
    private static final CustomColor PANEL_BG = CustomColor.fromHexString("17120E").withAlpha(0.90f);
    private static final CustomColor PANEL_BORDER = CustomColor.fromHexString("6F543A");
    private static final CustomColor PANEL_ACCENT = CustomColor.fromHexString("A87A4A");
    private static final CustomColor TEXT = CustomColor.fromHexString("F3E8D8");
    private static final CustomColor TEXT_DIM = CustomColor.fromHexString("C7B9A7");
    private static final CustomColor TEXT_ERROR = CustomColor.fromHexString("FF7777");
    private static final CustomColor MATERIAL_TEXT = CustomColor.fromHexString("8FC7D2");
    private static final CustomColor MATERIAL_DIM = CustomColor.fromHexString("83A9B0");
    private static final CustomColor INGREDIENT_TEXT = CustomColor.fromHexString("F0C46A");
    private static final CustomColor INGREDIENT_DIM = CustomColor.fromHexString("C39A51");
    private static final CustomColor ROW_BG = CustomColor.fromHexString("1F1812").withAlpha(0.58f);
    private static final CustomColor ROW_HOVER = CustomColor.fromHexString("332519").withAlpha(0.78f);
    private static final int SCROLL_TRACK = 0xCC2A2119;
    private static final int SCROLL_THUMB = 0xFF8A6746;
    private static final int BUTTON_CUSTOM_SCALE = 5;
    private static final int TOOLTIP_PADDING = 6;
    private static final int TOOLTIP_LINE_HEIGHT = 10;
    private static final int TOOLTIP_GAP = 8;
    private static final int TOOLTIP_BG = 0xF017120E;
    private static final int TOOLTIP_BORDER = 0xFFA87A4A;
    private static final int TOOLTIP_TEXT = 0xFFF3E8D8;
    private static final int TOOLTIP_DIM = 0xFFC7B9A7;
    private static final String IMPORT_TOOLTIP = "Import WCI/WynnBuilder crafting link from clipboard.";
    private static final String CLEAR_TOOLTIP = "Clear the current WCI shopping cart.";
    private static final String SPEED_TOOLTIP = "Profession Speed: halves material needs only. Ingredients are unchanged.";
    private static final String QTY_TOOLTIP = "Output quantity.\nClick +1, Shift-click +10, Right-click -1, Shift-right-click reset.";
    private static final String CLOSE_TOOLTIP = "Close the WCI shopping panel.";
    private static final String DEFAULT_STATUS = "Click row to copy/search TM.";
    private static final String PINNED_STATUS = "WCI menu pinned.";
    private static final String PROFESSION_SPEED_ON_STATUS = "Profession Speed on.";
    private static final String PROFESSION_SPEED_OFF_STATUS = "Profession Speed off.";
    private static final String BANK_COUNTS_UPDATING_STATUS = "Bank counts updating...";
    private static final String BANK_CACHE_UNAVAILABLE_STATUS = "Bank cache unavailable.";
    private static final String BANK_CACHE_INCOMPLETE_STATUS = "Bank cache may be incomplete.";
    private static final String BANK_CACHE_REFRESH_TOOLTIP =
            "Reload your bank tabs to refresh WCI counts.\nCounts may be missing until every bank tab is refreshed.";
    private static final long SCREEN_TRANSITION_STABILITY_MS = 450L;
    private static boolean menuPinnedByUser = false;

    private final ShoppingCartService service = WynnExtrasWciFeature.shoppingCartService();
    private final WciHaveCountService haveCountService = new WciHaveCountService();
    private final List<RowHit> rowHits = new ArrayList<>();
    private final MenuButton importButton = new MenuButton("Import", IMPORT_TOOLTIP, this::importClipboard);
    private final MenuButton clearButton = new MenuButton("Clear", CLEAR_TOOLTIP, this::clearCart);
    private final MenuButton speedButton = new MenuButton(this::speedButtonLabel, SPEED_TOOLTIP, this::toggleProfessionSpeed);
    private final OutputButton outputButton = new OutputButton();
    private final HeaderCloseButton closeButton = new HeaderCloseButton();

    private String status = DEFAULT_STATUS;
    private boolean statusError = false;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int scrollOffset = 0;
    private ScrollbarLayout scrollbarLayout = ScrollbarLayout.hidden();
    private boolean draggingScrollbar = false;
    private int scrollbarDragOffsetY = 0;
    private boolean draggingPanel = false;
    private boolean panelDragMoved = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private WciScreenContext screenContext = WciScreenContext.UNSUPPORTED;
    private WciPlacementContext placementContext = WciPlacementContext.OTHER;
    private WciPlacementContext dragPlacementContext = WciPlacementContext.OTHER;
    private long lastTradeMarketStatusRevision = WciTradeMarketSearchService.statusRevision();
    private WciHaveCountService.SourceSnapshot cachedHaveCountSnapshot;
    private Object cachedHaveCountCart;
    private int cachedHaveCountCartSize = -1;
    private int cachedHaveCountCartSignature = 0;
    private int cachedOutputCount = -1;
    private boolean cachedProfessionSpeed = false;
    private boolean cachedHaveCountsUpdating = false;
    private List<RenderedRow> cachedRenderedRows = List.of();
    private String currentScreenTransitionKey = "";
    private long currentScreenTransitionChangedAtMs = Long.MIN_VALUE;

    public WciShoppingMenuExtension() {
        rootWidgets.add(importButton);
        rootWidgets.add(clearButton);
        rootWidgets.add(speedButton);
        rootWidgets.add(outputButton);
        rootWidgets.add(closeButton);
        applyPendingFeatureStatus();
    }

    public static boolean isOpen() {
        return WynnExtrasConfig.INSTANCE.wciShoppingMenuEnabled;
    }

    public static boolean isVisible() {
        return WynnExtrasConfig.INSTANCE.wciShoppingMenuEnabled;
    }

    public static boolean toggleFromCommand() {
        return toggleFromCommand(WciScreenContext.UNSUPPORTED).open();
    }

    public static ToggleResult toggleFromCommand(WciScreenContext context) {
        if (isOpen()) {
            close();
            return new ToggleResult(false, true, "WCI shopping menu disabled.");
        }
        return showFromCommand(context);
    }

    public static ToggleResult showFromCommand(WciScreenContext context) {
        if (!WciMenuRenderPolicy.canOpenFromCommand(context, WynnExtrasConfig.INSTANCE.wciAllowPersistentMenu)) {
            String message = context == WciScreenContext.BLOCKED_MODAL
                    ? "WCI unavailable on this screen."
                    : "Open Trade Market, Bank, or Crafting first.";
            return new ToggleResult(false, false, message);
        }
        if (WciMenuRenderPolicy.shouldPinCommandOpen(context, WynnExtrasConfig.INSTANCE.wciAllowPersistentMenu)) {
            showPinned();
        } else {
            show();
        }
        return new ToggleResult(true, true, "WCI shopping menu enabled.");
    }

    public static boolean toggleFromHotkey(WciScreenContext context) {
        if (isOpen()) {
            close();
            return false;
        }
        if (context == WciScreenContext.BLOCKED_MODAL) {
            return false;
        }
        if (context != null && context.supportsWciMenu()) {
            show();
        } else {
            showPinned();
        }
        return true;
    }

    public static void show() {
        menuPinnedByUser = false;
        setEnabled(true);
    }

    public static void showPinned() {
        menuPinnedByUser = true;
        setEnabled(true);
    }

    public static void close() {
        setEnabled(false);
    }

    public static boolean isPinnedByUser() {
        return menuPinnedByUser;
    }

    public static boolean shouldRender(WciScreenContext context) {
        return WciMenuRenderPolicy.shouldRenderMenu(
                isOpen(),
                context,
                menuPinnedByUser,
                WynnExtrasConfig.INSTANCE.wciAllowPersistentMenu);
    }

    public static boolean isToggleKey(int keyCode) {
        return WynnExtrasConfig.INSTANCE.wciToggleKey != GLFW.GLFW_KEY_UNKNOWN
                && keyCode == WynnExtrasConfig.INSTANCE.wciToggleKey;
    }

    public static WciShoppingMenuLauncherButton.Bounds closeButtonBounds(int panelX, int panelY, int panelWidth) {
        return new WciShoppingMenuLauncherButton.Bounds(
                panelX + panelWidth - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_RIGHT_MARGIN,
                panelY + CLOSE_BUTTON_TOP_MARGIN,
                CLOSE_BUTTON_SIZE,
                CLOSE_BUTTON_SIZE);
    }

    public static boolean isHeaderDragArea(int panelX, int panelY, int panelWidth, double x, double y) {
        return x >= panelX
                && y >= panelY
                && x < panelX + panelWidth
                && y < panelY + HEADER_DRAG_HEIGHT
                && !closeButtonBounds(panelX, panelY, panelWidth).contains(x, y);
    }

    public static WciShoppingMenuLauncherButton.Bounds statusBounds(int panelX, int panelY, int panelWidth, int panelHeight) {
        return new WciShoppingMenuLauncherButton.Bounds(panelX + 8, panelY + panelHeight - 15, panelWidth - 16, 11);
    }

    public static void resetPosition() {
        resetPosition(WynnExtrasConfig.INSTANCE);
        WynnExtrasConfig.save();
    }

    public static boolean clearFromCommand() {
        WynnExtrasWciFeature.shoppingCartService().clear();
        return !WynnExtrasWciFeature.consumeSaveFailure();
    }

    public static void copyFromCommand() {
        MinecraftClient.getInstance().keyboard.setClipboard(
                WciShoppingListFormatter.format(WynnExtrasWciFeature.shoppingCartService().cart()));
    }

    private static void setEnabled(boolean enabled) {
        if (!enabled) {
            menuPinnedByUser = false;
        }
        if (WynnExtrasConfig.INSTANCE.wciShoppingMenuEnabled != enabled) {
            WynnExtrasConfig.INSTANCE.wciShoppingMenuEnabled = enabled;
            WynnExtrasConfig.save();
        }
    }

    public void setScreenContext(WciScreenContext screenContext) {
        this.screenContext = screenContext == null ? WciScreenContext.UNSUPPORTED : screenContext;
        this.placementContext = this.screenContext.placementContext();
    }

    public void setPlacementContext(WciPlacementContext placementContext) {
        this.placementContext = placementContext == null ? WciPlacementContext.OTHER : placementContext;
    }

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (screenContext == WciScreenContext.BLOCKED_MODAL) {
            setButtonsVisible(false);
            rowHits.clear();
            draggingPanel = false;
            return;
        }
        WynnExtrasWciFeature.loadPersistedCart();
        applyPendingFeatureStatus();
        if (!shouldRender(screenContext)) {
            setButtonsVisible(false);
            rowHits.clear();
            return;
        }
        Screen current = MinecraftClient.getInstance().currentScreen;
        if (!(current instanceof HandledScreen<?> handledScreen)) {
            setButtonsVisible(false);
            rowHits.clear();
            return;
        }

        boolean screenStableForCounts = updateScreenTransitionState(handledScreen, System.currentTimeMillis());
        updatePanelBounds(handledScreen);
        drawPanel();
        layoutButtons();
        drawRows(mouseX, mouseY, screenStableForCounts);
        applyTradeMarketStatus();
        drawStatus();
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!shouldRender(screenContext)) return;
        List<String> controlTooltip = hoveredControlTooltip();
        if (!controlTooltip.isEmpty()) {
            drawTooltip(ctx, controlTooltip, mouseX, mouseY);
            return;
        }
        List<String> statusTooltip = hoveredStatusTooltip(mouseX, mouseY);
        if (!statusTooltip.isEmpty()) {
            drawTooltip(ctx, statusTooltip, mouseX, mouseY);
            return;
        }
        RowHit rowHit = rowAt(mouseX, mouseY);
        if (!shouldRenderRowTooltip(false, rowHit != null)) return;

        drawTooltip(ctx, rowHit.detail().tooltipLines(WciMenuRenderPolicy.primaryRowAction(screenContext)),
                mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (screenContext == WciScreenContext.BLOCKED_MODAL) return false;
        if (!shouldRender(screenContext)) return false;
        if (super.mouseClicked(x, y, button)) {
            return true;
        }
        if (!containsPanel(x, y)) {
            return false;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && handleScrollbarClick(x, y)) {
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isHeaderDragArea(x, y)) {
            draggingPanel = true;
            panelDragMoved = false;
            dragPlacementContext = placementContext;
            dragOffsetX = (int) x - panelX;
            dragOffsetY = (int) y - panelY;
            return true;
        }

        RowHit rowHit = rowAt(x, y);
        if (rowHit != null) {
            handleRowClick(rowHit.row(), button);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (screenContext == WciScreenContext.BLOCKED_MODAL) {
            cancelPanelDrag();
            cancelScrollbarDrag();
            return false;
        }
        if (!shouldRender(screenContext)) {
            cancelPanelDrag();
            cancelScrollbarDrag();
            return false;
        }
        if (draggingScrollbar && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            cancelScrollbarDrag();
            return true;
        }
        if (draggingPanel && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingPanel = false;
            dragPlacementContext = WciPlacementContext.OTHER;
            if (panelDragMoved) {
                WynnExtrasConfig.save();
            }
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (screenContext == WciScreenContext.BLOCKED_MODAL) {
            cancelPanelDrag();
            cancelScrollbarDrag();
            return false;
        }
        if (!shouldRender(screenContext)) {
            cancelPanelDrag();
            cancelScrollbarDrag();
            return false;
        }
        if (draggingScrollbar && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            updateScrollOffsetFromThumb((int) y - scrollbarDragOffsetY);
            return true;
        }
        if (draggingPanel && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            movePanelTo((int) x - dragOffsetX, (int) y - dragOffsetY, dragPlacementContext);
            panelDragMoved = true;
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    private void cancelPanelDrag() {
        draggingPanel = false;
        panelDragMoved = false;
        dragPlacementContext = WciPlacementContext.OTHER;
    }

    private void cancelScrollbarDrag() {
        draggingScrollbar = false;
        scrollbarDragOffsetY = 0;
    }

    private boolean handleScrollbarClick(double x, double y) {
        if (!scrollbarLayout.visible()) {
            return false;
        }
        if (!scrollbarLayout.track().contains(x, y)) {
            return false;
        }
        if (scrollbarLayout.thumb().contains(x, y)) {
            draggingScrollbar = true;
            scrollbarDragOffsetY = (int) y - scrollbarLayout.thumb().y();
            return true;
        }
        updateScrollOffsetFromThumb((int) y - scrollbarLayout.thumb().height() / 2);
        return true;
    }

    private void updateScrollOffsetFromThumb(int thumbY) {
        scrollOffset = scrollbarScrollOffset(scrollbarLayout, thumbY);
    }

    public boolean mouseScrolled(double x, double y, double verticalAmount) {
        if (screenContext == WciScreenContext.BLOCKED_MODAL) return false;
        if (!shouldRender(screenContext) || !containsPanel(x, y)) return false;

        List<WciShoppingListFormatter.Row> rows = WciShoppingListFormatter.rows(service.cart());
        int maxScroll = maxScroll(rows.size(), maxVisibleRows());
        if (maxScroll <= 0) return true;

        int previousOffset = scrollOffset;
        if (verticalAmount < 0) {
            scrollOffset++;
        } else if (verticalAmount > 0) {
            scrollOffset--;
        }
        scrollOffset = clamp(scrollOffset, 0, maxScroll);

        if (scrollOffset != previousOffset) {
            status = "Rows " + (scrollOffset + 1) + "-" + Math.min(rows.size(), scrollOffset + maxVisibleRows())
                    + " of " + rows.size() + ".";
            statusError = false;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (screenContext == WciScreenContext.BLOCKED_MODAL || !shouldRender(screenContext)) {
            return false;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void updatePanelBounds(HandledScreen<?> screen) {
        int screenX = HandledScreenAccess.x(screen);
        int screenY = HandledScreenAccess.y(screen);
        int guiWidth = HandledScreenAccess.backgroundWidth(screen);
        int guiHeight = HandledScreenAccess.backgroundHeight(screen);

        panelW = Math.clamp(screenWidth - PANEL_MARGIN * 2, 150, PANEL_WIDTH);
        panelH = Math.clamp(screenHeight - PANEL_MARGIN * 2, 120, PANEL_HEIGHT);

        PositionState savedPosition = positionState(WynnExtrasConfig.INSTANCE, placementContext);
        if (savedPosition.customPosition()) {
            WciShoppingMenuLauncherButton.Bounds savedBounds = clampPanelBounds(
                    savedPosition.x() < 0 ? PANEL_MARGIN : savedPosition.x(),
                    savedPosition.y() < 0 ? PANEL_MARGIN : savedPosition.y(),
                    panelW,
                    panelH,
                    screenWidth,
                    screenHeight);
            panelX = savedBounds.x();
            panelY = savedBounds.y();
            return;
        }

        List<WciShoppingMenuLauncherButton.Bounds> forbiddenAreas = forbiddenAreas(
                placementContext,
                screenX,
                screenY,
                guiWidth,
                guiHeight,
                screenWidth,
                screenHeight);
        WciShoppingMenuLauncherButton.Bounds defaultBounds = computePanelBounds(
                placementContext,
                screenX,
                screenY,
                guiWidth,
                guiHeight,
                panelW,
                panelH,
                screenWidth,
                screenHeight,
                forbiddenAreas,
                bankOverlayBounds());
        panelX = defaultBounds.x();
        panelY = defaultBounds.y();
    }

    private void movePanelTo(int x, int y, WciPlacementContext context) {
        panelX = clampPanelX(x);
        panelY = clampPanelY(y);
        setPosition(WynnExtrasConfig.INSTANCE, context, panelX, panelY);
    }

    private int clampPanelX(int x) {
        return clamp(x, PANEL_MARGIN, Math.max(PANEL_MARGIN, screenWidth - panelW - PANEL_MARGIN));
    }

    private int clampPanelY(int y) {
        return clamp(y, PANEL_MARGIN, Math.max(PANEL_MARGIN, screenHeight - panelH - PANEL_MARGIN));
    }

    public static WciShoppingMenuLauncherButton.Bounds computePanelBounds(
            WciPlacementContext placementContext,
            int screenX,
            int screenY,
            int backgroundWidth,
            int backgroundHeight,
            int panelWidth,
            int panelHeight,
            int screenWidth,
            int screenHeight,
            List<WciShoppingMenuLauncherButton.Bounds> forbiddenAreas) {
        return computePanelBounds(placementContext, screenX, screenY, backgroundWidth, backgroundHeight, panelWidth,
                panelHeight, screenWidth, screenHeight, forbiddenAreas, null);
    }

    public static WciShoppingMenuLauncherButton.Bounds computePanelBounds(
            WciPlacementContext placementContext,
            int screenX,
            int screenY,
            int backgroundWidth,
            int backgroundHeight,
            int panelWidth,
            int panelHeight,
            int screenWidth,
            int screenHeight,
            List<WciShoppingMenuLauncherButton.Bounds> forbiddenAreas,
            WciShoppingMenuLauncherButton.Bounds bankOverlayPreferredBounds) {
        List<WciShoppingMenuLauncherButton.Bounds> candidates = panelCandidatesFor(
                placementContext,
                screenX,
                screenY,
                backgroundWidth,
                panelWidth,
                panelHeight,
                screenWidth,
                screenHeight,
                bankOverlayPreferredBounds);

        for (WciShoppingMenuLauncherButton.Bounds candidate : candidates) {
            if (!fitsInsideScreen(candidate, screenWidth, screenHeight)) {
                continue;
            }
            if (!overlapsAny(candidate, forbiddenAreas)) {
                return candidate;
            }
        }

        int preferredX = screenX + backgroundWidth + PANEL_MARGIN;
        int fallbackLeftX = screenX - panelWidth - PANEL_MARGIN;
        int x;
        if (preferredX + panelWidth <= screenWidth - PANEL_MARGIN) {
            x = preferredX;
        } else if (fallbackLeftX >= PANEL_MARGIN) {
            x = fallbackLeftX;
        } else {
            x = screenWidth - panelWidth - PANEL_MARGIN;
        }
        int y = clampValue(screenY, PANEL_MARGIN, Math.max(PANEL_MARGIN, screenHeight - panelHeight - PANEL_MARGIN));
        return clampPanelBounds(x, y, panelWidth, panelHeight, screenWidth, screenHeight);
    }

    private static List<WciShoppingMenuLauncherButton.Bounds> panelCandidatesFor(
            WciPlacementContext placementContext,
            int screenX,
            int screenY,
            int backgroundWidth,
            int panelWidth,
            int panelHeight,
            int screenWidth,
            int screenHeight,
            WciShoppingMenuLauncherButton.Bounds bankOverlayPreferredBounds) {
        return switch (placementContext) {
            case TRADE_MARKET -> List.of(
                    new WciShoppingMenuLauncherButton.Bounds(screenX + backgroundWidth + PANEL_MARGIN, screenY,
                            panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(screenX - panelWidth - PANEL_MARGIN, screenY,
                            panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN, PANEL_MARGIN,
                            panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(PANEL_MARGIN, PANEL_MARGIN, panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN,
                            screenHeight - panelHeight - PANEL_MARGIN, panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(PANEL_MARGIN, screenHeight - panelHeight - PANEL_MARGIN,
                            panelWidth, panelHeight)
            );
            case BANK_OVERLAY -> bankOverlayPanelCandidates(screenX, screenY, backgroundWidth, panelWidth, panelHeight,
                    screenWidth, screenHeight, bankOverlayPreferredBounds);
            case BANK_VANILLA -> List.of(
                    new WciShoppingMenuLauncherButton.Bounds(BANK_VANILLA_DEFAULT_X, BANK_VANILLA_DEFAULT_Y,
                            panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(screenX + backgroundWidth + PANEL_MARGIN, screenY,
                            panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(screenX - panelWidth - PANEL_MARGIN, screenY,
                            panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN, PANEL_MARGIN,
                            panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(PANEL_MARGIN, PANEL_MARGIN, panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN,
                            screenHeight - panelHeight - PANEL_MARGIN, panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(PANEL_MARGIN, screenHeight - panelHeight - PANEL_MARGIN,
                            panelWidth, panelHeight)
            );
            case OTHER -> List.of(
                    new WciShoppingMenuLauncherButton.Bounds(screenX + backgroundWidth + PANEL_MARGIN, screenY,
                            panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(screenX - panelWidth - PANEL_MARGIN, screenY,
                            panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN, PANEL_MARGIN,
                            panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(PANEL_MARGIN, PANEL_MARGIN, panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN,
                            screenHeight - panelHeight - PANEL_MARGIN, panelWidth, panelHeight),
                    new WciShoppingMenuLauncherButton.Bounds(PANEL_MARGIN, screenHeight - panelHeight - PANEL_MARGIN,
                            panelWidth, panelHeight)
            );
        };
    }

    private static List<WciShoppingMenuLauncherButton.Bounds> bankOverlayPanelCandidates(
            int screenX,
            int screenY,
            int backgroundWidth,
            int panelWidth,
            int panelHeight,
            int screenWidth,
            int screenHeight,
            WciShoppingMenuLauncherButton.Bounds bankOverlayPreferredBounds) {
        List<WciShoppingMenuLauncherButton.Bounds> candidates = new ArrayList<>();
        if (bankOverlayPreferredBounds != null && bankOverlayPreferredBounds.visible()) {
            candidates.add(bankOverlayPreferredBounds);
        }
        candidates.add(new WciShoppingMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN, PANEL_MARGIN,
                panelWidth, panelHeight));
        candidates.add(new WciShoppingMenuLauncherButton.Bounds(screenX - panelWidth - PANEL_MARGIN, screenY,
                panelWidth, panelHeight));
        candidates.add(new WciShoppingMenuLauncherButton.Bounds(screenX + backgroundWidth + PANEL_MARGIN, screenY,
                panelWidth, panelHeight));
        candidates.add(new WciShoppingMenuLauncherButton.Bounds(PANEL_MARGIN, PANEL_MARGIN, panelWidth, panelHeight));
        candidates.add(new WciShoppingMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN,
                screenHeight - panelHeight - PANEL_MARGIN, panelWidth, panelHeight));
        candidates.add(new WciShoppingMenuLauncherButton.Bounds(PANEL_MARGIN, screenHeight - panelHeight - PANEL_MARGIN,
                panelWidth, panelHeight));
        return List.copyOf(candidates);
    }

    public static WciShoppingMenuLauncherButton.Bounds clampPanelBounds(int x, int y, int panelWidth, int panelHeight,
                                                                        int screenWidth, int screenHeight) {
        int maxX = Math.max(PANEL_MARGIN, screenWidth - panelWidth - PANEL_MARGIN);
        int maxY = Math.max(PANEL_MARGIN, screenHeight - panelHeight - PANEL_MARGIN);
        return new WciShoppingMenuLauncherButton.Bounds(
                clampValue(x, PANEL_MARGIN, maxX),
                clampValue(y, PANEL_MARGIN, maxY),
                panelWidth,
                panelHeight);
    }

    static void resetPosition(WynnExtrasConfig config) {
        config.wciShoppingMenuDefaultPosition.reset();
        config.wciShoppingMenuTradePosition.reset();
        config.wciShoppingMenuBankOverlayPosition.reset();
        config.wciShoppingMenuBankVanillaPosition.reset();
    }

    private static PositionState positionState(WynnExtrasConfig config, WciPlacementContext context) {
        WynnExtrasConfig.WciPosition position = position(config, context);
        return new PositionState(position.isSet(), position.x, position.y);
    }

    private static void setPosition(WynnExtrasConfig config, WciPlacementContext placementContext, int x, int y) {
        position(config, placementContext).set(x, y);
    }

    private static WynnExtrasConfig.WciPosition position(WynnExtrasConfig config, WciPlacementContext context) {
        return switch (context) {
            case TRADE_MARKET -> config.wciShoppingMenuTradePosition;
            case BANK_OVERLAY -> config.wciShoppingMenuBankOverlayPosition;
            case BANK_VANILLA -> config.wciShoppingMenuBankVanillaPosition;
            case OTHER -> config.wciShoppingMenuDefaultPosition;
        };
    }

    private List<WciShoppingMenuLauncherButton.Bounds> forbiddenAreas(WciPlacementContext placementContext,
                                                                      int screenX,
                                                                      int screenY,
                                                                      int backgroundWidth,
                                                                      int backgroundHeight,
                                                                      int screenWidth,
                                                                      int screenHeight) {
        List<WciShoppingMenuLauncherButton.Bounds> forbiddenAreas = new ArrayList<>();
        if (placementContext == WciPlacementContext.BANK_VANILLA
                || placementContext == WciPlacementContext.TRADE_MARKET) {
            forbiddenAreas.add(new WciShoppingMenuLauncherButton.Bounds(screenX - PANEL_MARGIN, screenY - PANEL_MARGIN,
                    backgroundWidth + PANEL_MARGIN * 2, backgroundHeight + PANEL_MARGIN * 2));
        }
        return forbiddenAreas;
    }

    private WciShoppingMenuLauncherButton.Bounds bankOverlayBounds() {
        if (placementContext != WciPlacementContext.BANK_OVERLAY) {
            return null;
        }
        return bankOverlayDefaultBounds(screenWidth, screenHeight, panelW, panelH);
    }

    public static WciShoppingMenuLauncherButton.Bounds bankOverlayDefaultBounds(int screenWidth, int screenHeight,
                                                                               int panelWidth, int panelHeight) {
        return clampPanelBounds(BANK_OVERLAY_DEFAULT_X, BANK_OVERLAY_DEFAULT_Y, panelWidth, panelHeight,
                screenWidth, screenHeight);
    }

    private static boolean overlapsAny(WciShoppingMenuLauncherButton.Bounds bounds,
                                       List<WciShoppingMenuLauncherButton.Bounds> forbiddenAreas) {
        for (WciShoppingMenuLauncherButton.Bounds forbiddenArea : forbiddenAreas) {
            if (bounds.overlaps(forbiddenArea)) {
                return true;
            }
        }
        return false;
    }

    private static boolean fitsInsideScreen(WciShoppingMenuLauncherButton.Bounds bounds, int screenWidth, int screenHeight) {
        return bounds.x() >= PANEL_MARGIN
                && bounds.y() >= PANEL_MARGIN
                && bounds.x() + bounds.width() <= screenWidth - PANEL_MARGIN
                && bounds.y() + bounds.height() <= screenHeight - PANEL_MARGIN;
    }

    private static int clampValue(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    private void drawPanel() {
        ui.drawRect(panelX, panelY, panelW, panelH, PANEL_BG);
        drawBorder(panelX, panelY, panelW, panelH, PANEL_BORDER.asInt());
        ui.drawText("WCI Shopping", panelX + 8, panelY + 7, TEXT, TITLE_SCALE);
        ui.drawText("Have/Need", panelX + panelW - 56, listHeaderY(panelY), TEXT_DIM, TEXT_SCALE);
        drawContext.fill(panelX + 6, listDividerY(panelY), panelX + panelW - 6,
                listDividerY(panelY) + 1, PANEL_ACCENT.asInt());
    }

    private void drawBorder(int x, int y, int width, int height, int color) {
        drawContext.fill(x, y, x + width, y + 1, color);
        drawContext.fill(x, y + height - 1, x + width, y + height, color);
        drawContext.fill(x, y, x + 1, y + height, color);
        drawContext.fill(x + width - 1, y, x + width, y + height, color);
    }

    private List<String> hoveredControlTooltip() {
        for (int i = rootWidgets.size() - 1; i >= 0; i--) {
            Widget widget = rootWidgets.get(i);
            if (widget.isVisible() && widget.isHovered() && widget instanceof TooltipWidget tooltipWidget) {
                return tooltipLines(tooltipWidget.tooltipText());
            }
        }
        return List.of();
    }

    private List<String> hoveredStatusTooltip(double mouseX, double mouseY) {
        if (!statusBounds(panelX, panelY, panelW, panelH).contains(mouseX, mouseY)) {
            return List.of();
        }
        return statusTooltipLines(status);
    }

    static boolean shouldRenderRowTooltip(boolean controlTooltipVisible, boolean rowHovered) {
        return !controlTooltipVisible && rowHovered;
    }

    static List<String> tooltipLines(String tooltipText) {
        if (tooltipText == null || tooltipText.isBlank()) {
            return List.of();
        }
        return tooltipText.lines()
                .filter(line -> !line.isBlank())
                .toList();
    }

    private void drawTooltip(DrawContext ctx, List<String> lines, int mouseX, int mouseY) {
        if (lines == null || lines.isEmpty()) return;

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, textRenderer.getWidth(line));
        }
        int maxTooltipWidth = Math.max(80, screenWidth - PANEL_MARGIN * 2);
        width = Math.min(width + TOOLTIP_PADDING * 2, maxTooltipWidth);
        int textMaxWidth = Math.max(0, width - TOOLTIP_PADDING * 2);
        int height = TOOLTIP_PADDING * 2 + lines.size() * TOOLTIP_LINE_HEIGHT;

        int x = mouseX + TOOLTIP_GAP;
        int y = mouseY + TOOLTIP_GAP;
        if (x + width > screenWidth - PANEL_MARGIN) {
            x = mouseX - TOOLTIP_GAP - width;
        }
        if (y + height > screenHeight - PANEL_MARGIN) {
            y = screenHeight - PANEL_MARGIN - height;
        }
        x = clamp(x, PANEL_MARGIN, Math.max(PANEL_MARGIN, screenWidth - PANEL_MARGIN - width));
        y = clamp(y, PANEL_MARGIN, Math.max(PANEL_MARGIN, screenHeight - PANEL_MARGIN - height));

        ctx.fill(x, y, x + width, y + height, TOOLTIP_BG);
        drawContext.fill(x, y, x + width, y + 1, TOOLTIP_BORDER);
        drawContext.fill(x, y + height - 1, x + width, y + height, TOOLTIP_BORDER);
        drawContext.fill(x, y, x + 1, y + height, TOOLTIP_BORDER);
        drawContext.fill(x + width - 1, y, x + width, y + height, TOOLTIP_BORDER);

        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 ? TOOLTIP_TEXT : TOOLTIP_DIM;
            String line = textRenderer.trimToWidth(lines.get(i), textMaxWidth);
            ctx.drawText(textRenderer, line, x + TOOLTIP_PADDING, y + TOOLTIP_PADDING + i * TOOLTIP_LINE_HEIGHT, color, false);
        }
    }

    private void layoutButtons() {
        setButtonsVisible(true);
        ControlLayout layout = controlLayout(panelX, panelY, panelW);
        importButton.setBounds(layout.importButton().x(), layout.importButton().y(),
                layout.importButton().width(), layout.importButton().height());
        clearButton.setBounds(layout.clearButton().x(), layout.clearButton().y(),
                layout.clearButton().width(), layout.clearButton().height());
        speedButton.setBounds(layout.speedButton().x(), layout.speedButton().y(),
                layout.speedButton().width(), layout.speedButton().height());
        outputButton.setBounds(layout.outputButton().x(), layout.outputButton().y(),
                layout.outputButton().width(), layout.outputButton().height());
        var closeBounds = closeButtonBounds(panelX, panelY, panelW);
        closeButton.setBounds(closeBounds.x(), closeBounds.y(), closeBounds.width(), closeBounds.height());
    }

    static ControlLayout controlLayout(int panelX, int panelY, int panelWidth) {
        int gap = 4;
        int buttonWidth = (panelWidth - 16 - gap) / 2;
        int firstRowY = panelY + 24;
        int secondRowY = firstRowY + BUTTON_HEIGHT + 2;
        return new ControlLayout(
                new WciShoppingMenuLauncherButton.Bounds(panelX + 8, firstRowY, buttonWidth, BUTTON_HEIGHT),
                new WciShoppingMenuLauncherButton.Bounds(panelX + 8 + buttonWidth + gap, firstRowY, buttonWidth, BUTTON_HEIGHT),
                new WciShoppingMenuLauncherButton.Bounds(panelX + 8, secondRowY, buttonWidth, BUTTON_HEIGHT),
                new WciShoppingMenuLauncherButton.Bounds(panelX + 8 + buttonWidth + gap, secondRowY, buttonWidth, BUTTON_HEIGHT));
    }

    private void drawRows(int mouseX, int mouseY, boolean screenStableForCounts) {
        rowHits.clear();
        List<RenderedRow> rows = rowsWithHaveCounts(screenStableForCounts);
        int listX = panelX + 7;
        int listY = listTopY();
        int listW = panelW - 14;
        int maxRows = maxVisibleRows();
        if (rows.isEmpty()) {
            scrollOffset = 0;
            scrollbarLayout = ScrollbarLayout.hidden();
            ui.drawText("Cart is empty.", listX + 2, listY + 4, TEXT_DIM, TEXT_SCALE);
            return;
        }

        scrollOffset = clamp(scrollOffset, 0, maxScroll(rows.size(), maxRows));
        scrollbarLayout = scrollbarLayout(panelX, panelY, panelW, panelH, rows.size(), maxRows, scrollOffset);
        int rowContentWidth = scrollbarLayout.visible()
                ? listW - SCROLLBAR_WIDTH - SCROLLBAR_GAP
                : listW;
        int renderedRows = Math.min(maxRows, rows.size() - scrollOffset);
        for (int i = 0; i < renderedRows; i++) {
            RenderedRow renderedRow = rows.get(scrollOffset + i);
            WciShoppingListFormatter.Row row = renderedRow.row();
            int rowY = listY + i * ROW_HEIGHT;
            rowHits.add(new RowHit(row, renderedRow.detail(), listX, rowY, rowContentWidth, ROW_HEIGHT - 1));

            boolean hoveredRow = mouseX >= listX && mouseY >= rowY
                    && mouseX < listX + rowContentWidth
                    && mouseY < rowY + ROW_HEIGHT - 1;
            if (hoveredRow) {
                ui.drawRect(listX, rowY, rowContentWidth, ROW_HEIGHT - 1, ROW_HOVER);
            } else if (i % 2 == 0) {
                ui.drawRect(listX, rowY, rowContentWidth, ROW_HEIGHT - 1, ROW_BG);
            }

            String haveNeed = row.haveCount() + "/" + row.needCount();
            ui.drawText(row.typeLabel(), listX + 3, rowY + 2,
                    rowTypeTextColor(row.type()), TEXT_SCALE);
            ui.drawText(trimToWidth(row.displayNameWithTier(), rowContentWidth - 86, TEXT_SCALE),
                    listX + 37, rowY + 2, rowNameTextColor(row.type()), TEXT_SCALE);
            ui.drawText(haveNeed, listX + rowContentWidth - 38, rowY + 2, TEXT_DIM, TEXT_SCALE);
        }

        if (rows.size() > renderedRows) {
            String range = (scrollOffset + 1) + "-" + (scrollOffset + renderedRows) + " / " + rows.size();
            ui.drawText(range, listX + 2, panelY + panelH - 23, TEXT_DIM, TEXT_SCALE);
            drawScrollbar(scrollbarLayout);
            if (scrollOffset > 0) {
                ui.drawText("↑", listX + listW - 20, listY - 1, TEXT_DIM, TEXT_SCALE);
            }
            if (scrollOffset + renderedRows < rows.size()) {
                ui.drawText("↓", listX + listW - 10, panelY + panelH - 23, TEXT_DIM, TEXT_SCALE);
            }
        }
    }

    private void drawScrollbar(ScrollbarLayout layout) {
        if (!layout.visible()) {
            return;
        }
        drawContext.fill(layout.track().x(), layout.track().y(),
                layout.track().x() + layout.track().width(),
                layout.track().y() + layout.track().height(),
                SCROLL_TRACK);
        drawContext.fill(layout.thumb().x(), layout.thumb().y(),
                layout.thumb().x() + layout.thumb().width(),
                layout.thumb().y() + layout.thumb().height(),
                SCROLL_THUMB);
    }

    private List<RenderedRow> rowsWithHaveCounts(boolean screenStableForCounts) {
        var cart = service.cart();
        var entries = cart.entries();
        if (entries.isEmpty()) {
            return List.of();
        }

        boolean allowSnapshotRefresh = screenStableForCounts
                && WciMenuRenderPolicy.allowsHaveCountRefresh(screenContext);
        WciHaveCountService.SnapshotResult snapshotResult = haveCountService.cachedSnapshot(allowSnapshotRefresh);
        WciHaveCountService.SourceSnapshot snapshot = snapshotResult.snapshot();
        int cartSize = entries.size();
        int cartSignature = entries.hashCode();
        int outputCount = outputCount();
        boolean professionSpeed = professionSpeedEnabled();
        if (!Objects.equals(snapshot, cachedHaveCountSnapshot)
                || cachedHaveCountSnapshot == null
                || cachedHaveCountCart != cart
                || cachedHaveCountCartSize != cartSize
                || cachedHaveCountCartSignature != cartSignature
                || cachedOutputCount != outputCount
                || cachedProfessionSpeed != professionSpeed) {
            cachedHaveCountSnapshot = snapshot;
            cachedHaveCountCart = cart;
            cachedHaveCountCartSize = cartSize;
            cachedHaveCountCartSignature = cartSignature;
            cachedOutputCount = outputCount;
            cachedProfessionSpeed = professionSpeed;
            cachedHaveCountsUpdating = snapshotResult.bankCountsUpdating();
            cachedRenderedRows = entries.entrySet().stream()
                    .map(entry -> {
                        var haveCount = haveCountService.count(entry.getKey(), cachedHaveCountSnapshot);
                        var row = WciShoppingListFormatter.Row.from(entry.getKey(),
                                adjustedRequired(entry.getKey(), entry.getValue()), haveCount.total());
                        return new RenderedRow(row, WciRowDetail.from(row, haveCount));
                    })
                    .toList();
        } else {
            cachedHaveCountsUpdating = snapshotResult.bankCountsUpdating();
        }
        updateBankCacheStatus(cachedHaveCountSnapshot, cachedHaveCountsUpdating,
                snapshotResult.signatureCheckThrottled());
        return cachedRenderedRows;
    }

    private void updateBankCacheStatus(WciHaveCountService.SourceSnapshot snapshot, boolean bankCountsUpdating,
                                       boolean signatureCheckThrottled) {
        String cacheStatus = bankCacheStatus(
                bankCountsUpdating,
                snapshot.bankCacheAvailable(),
                snapshot.bankCachePossiblyIncomplete());
        String nextStatus = statusAfterBankCacheUpdate(status, statusError, cacheStatus, signatureCheckThrottled);
        if (!Objects.equals(status, nextStatus)) {
            status = nextStatus;
            statusError = false;
        }
    }

    static String bankCacheStatus(boolean bankCountsUpdating, boolean bankCacheAvailable,
                                  boolean bankCachePossiblyIncomplete) {
        if (bankCountsUpdating) {
            return BANK_COUNTS_UPDATING_STATUS;
        }
        if (!bankCacheAvailable) {
            return BANK_CACHE_UNAVAILABLE_STATUS;
        }
        if (bankCachePossiblyIncomplete) {
            return BANK_CACHE_INCOMPLETE_STATUS;
        }
        return null;
    }

    static String statusAfterBankCacheUpdate(String currentStatus, boolean statusError, String cacheStatus) {
        return statusAfterBankCacheUpdate(currentStatus, statusError, cacheStatus, false);
    }

    static String statusAfterBankCacheUpdate(String currentStatus, boolean statusError, String cacheStatus,
                                             boolean signatureCheckThrottled) {
        String safeStatus = currentStatus == null ? DEFAULT_STATUS : currentStatus;
        if (statusError) {
            return safeStatus;
        }
        if (cacheStatus == null) {
            if (signatureCheckThrottled && isBankCacheStatus(safeStatus)) {
                return safeStatus;
            }
            return isBankCacheStatus(safeStatus) ? DEFAULT_STATUS : safeStatus;
        }
        if (DEFAULT_STATUS.equals(safeStatus)
                || safeStatus.startsWith("Imported ")
                || isBankCacheStatus(safeStatus)) {
            return cacheStatus;
        }
        return safeStatus;
    }

    private static boolean isBankCacheStatus(String currentStatus) {
        return BANK_COUNTS_UPDATING_STATUS.equals(currentStatus)
                || BANK_CACHE_UNAVAILABLE_STATUS.equals(currentStatus)
                || BANK_CACHE_INCOMPLETE_STATUS.equals(currentStatus);
    }

    private static boolean requiresBankCacheRefresh(String currentStatus) {
        return BANK_CACHE_UNAVAILABLE_STATUS.equals(currentStatus)
                || BANK_CACHE_INCOMPLETE_STATUS.equals(currentStatus);
    }

    private void drawStatus() {
        String text = trimToWidth(status, panelW - 16, TEXT_SCALE);
        ui.drawText(text, panelX + 8, panelY + panelH - 13, statusTextColor(status, statusError), TEXT_SCALE);
    }

    static CustomColor statusTextColor(String currentStatus, boolean statusError) {
        return statusError || requiresBankCacheRefresh(currentStatus) ? TEXT_ERROR : TEXT_DIM;
    }

    static List<String> statusTooltipLines(String currentStatus) {
        if (!requiresBankCacheRefresh(currentStatus)) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        lines.add(currentStatus);
        lines.addAll(tooltipLines(BANK_CACHE_REFRESH_TOOLTIP));
        return List.copyOf(lines);
    }

    private void handleRowClick(WciShoppingListFormatter.Row row, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE || (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isShiftDown())) {
            setTradeMarketStatus(WciTradeMarketSearchService.copied(row.tradeMarketQuery()));
            return;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            switch (WciMenuRenderPolicy.primaryRowAction(screenContext)) {
                case TRADE_MARKET_SEARCH -> {
                    if (WciTradeMarketSearchService.isWorkflowActive()) {
                        setTradeMarketStatus(WciTradeMarketSearchService.activeWorkflowResult());
                    } else {
                        setTradeMarketStatus(WciTradeMarketSearchService.searchOrCopy(row.tradeMarketQuery()));
                    }
                }
                case COPY_ONLY -> setTradeMarketStatus(WciTradeMarketSearchService.copied(row.tradeMarketQuery()));
                case NONE -> {
                }
            }
        }
    }

    private void setTradeMarketStatus(WciTradeMarketSearchService.Result result) {
        status = result.message();
        statusError = result.error();
        lastTradeMarketStatusRevision = result.revision();
    }

    private void applyTradeMarketStatus() {
        long revision = WciTradeMarketSearchService.statusRevision();
        if (revision == lastTradeMarketStatusRevision) {
            return;
        }
        WciTradeMarketSearchService.Result result = WciTradeMarketSearchService.latestResult();
        lastTradeMarketStatusRevision = revision;
        if (result.message() == null || result.message().isBlank()) {
            return;
        }
        status = result.message();
        statusError = result.error();
    }

    private boolean isShiftDown() {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private RowHit rowAt(double x, double y) {
        for (RowHit rowHit : rowHits) {
            if (rowHit.contains(x, y)) {
                return rowHit;
            }
        }
        return null;
    }

    private void importClipboard() {
        String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
        ShoppingCartService.ImportResult result = service.importUrl(clipboard);
        if (result.success() && WynnExtrasWciFeature.consumeSaveFailure()) {
            status = "Imported; save failed.";
            statusError = true;
        } else {
            status = WciImportStatusFormatter.format(result);
            statusError = !result.success();
        }
        invalidateHaveCountCache();
    }

    private void clearCart() {
        service.clear();
        scrollOffset = 0;
        if (WynnExtrasWciFeature.consumeSaveFailure()) {
            status = "Cart cleared; save failed.";
            statusError = true;
        } else {
            status = "Cleared WCI shopping cart.";
            statusError = false;
        }
        invalidateHaveCountCache();
    }

    private void toggleProfessionSpeed() {
        WynnExtrasConfig.INSTANCE.wciProfessionSpeed = !WynnExtrasConfig.INSTANCE.wciProfessionSpeed;
        WynnExtrasConfig.save();
        status = WynnExtrasConfig.INSTANCE.wciProfessionSpeed
                ? PROFESSION_SPEED_ON_STATUS
                : PROFESSION_SPEED_OFF_STATUS;
        statusError = false;
        invalidateHaveCountCache();
    }

    private boolean changeOutputCount(int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (isShiftDown()) {
                WynnExtrasConfig.INSTANCE.wciCraftMultiplier = 1;
                WynnExtrasConfig.save();
                status = outputResetStatusMessage();
            } else {
                int nextOutputCount = WciRequirementCalculator.subtractOutputs(outputCount(), 1);
                WynnExtrasConfig.INSTANCE.wciCraftMultiplier = nextOutputCount;
                WynnExtrasConfig.save();
                status = outputStatusMessage(nextOutputCount);
            }
            statusError = false;
            invalidateHaveCountCache();
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        int increment = WciRequirementCalculator.outputClickIncrement(isShiftDown());
        int nextOutputCount = WciRequirementCalculator.addOutputs(outputCount(), increment);
        WynnExtrasConfig.INSTANCE.wciCraftMultiplier = nextOutputCount;
        WynnExtrasConfig.save();
        status = outputStatusMessage(nextOutputCount);
        statusError = false;
        invalidateHaveCountCache();
        return true;
    }

    private String speedButtonLabel() {
        return WynnExtrasConfig.INSTANCE.wciProfessionSpeed ? "Speed: On" : "Speed: Off";
    }

    private String outputButtonLabel() {
        return outputButtonLabel(outputCount());
    }

    static String outputButtonLabel(int outputCount) {
        return "Qty x" + WciRequirementCalculator.sanitizeOutputCount(outputCount);
    }

    static String outputStatusMessage(int outputCount) {
        return "Quantity x" + WciRequirementCalculator.sanitizeOutputCount(outputCount) + ".";
    }

    static String outputResetStatusMessage() {
        return "Quantity reset to x1.";
    }

    private int adjustedRequired(ShoppingEntry entry, int baseRequired) {
        return WciRequirementCalculator.adjustedRequired(
                entry,
                baseRequired,
                outputCount(),
                professionSpeedEnabled());
    }

    private int outputCount() {
        return WciRequirementCalculator.sanitizeOutputCount(WynnExtrasConfig.INSTANCE.wciCraftMultiplier);
    }

    private boolean professionSpeedEnabled() {
        return WynnExtrasConfig.INSTANCE.wciProfessionSpeed;
    }

    private void invalidateHaveCountCache() {
        cachedHaveCountSnapshot = null;
        cachedHaveCountCart = null;
        cachedHaveCountCartSize = -1;
        cachedHaveCountCartSignature = 0;
        cachedOutputCount = -1;
        cachedProfessionSpeed = false;
        cachedHaveCountsUpdating = false;
        cachedRenderedRows = List.of();
        currentScreenTransitionKey = "";
        currentScreenTransitionChangedAtMs = Long.MIN_VALUE;
        haveCountService.invalidateCache();
    }

    private void applyPendingFeatureStatus() {
        if (!menuPinnedByUser && PINNED_STATUS.equals(status)) {
            status = DEFAULT_STATUS;
            statusError = false;
        }
        if (!DEFAULT_STATUS.equals(status)) {
            return;
        }
        WynnExtrasWciFeature.consumePendingStatus().ifPresent(message -> {
            status = message;
            statusError = message.toLowerCase(java.util.Locale.ROOT).contains("failed");
        });
        if (menuPinnedByUser && DEFAULT_STATUS.equals(status)) {
            status = PINNED_STATUS;
            statusError = false;
        }
    }

    private void setButtonsVisible(boolean visible) {
        importButton.setVisible(visible);
        clearButton.setVisible(visible);
        speedButton.setVisible(visible);
        outputButton.setVisible(visible);
        closeButton.setVisible(visible);
    }

    private boolean containsPanel(double x, double y) {
        return x >= panelX && y >= panelY && x < panelX + panelW && y < panelY + panelH;
    }

    private boolean updateScreenTransitionState(HandledScreen<?> screen, long nowMs) {
        String nextKey = screenTransitionKey(screen, screenContext);
        if (!Objects.equals(currentScreenTransitionKey, nextKey)) {
            currentScreenTransitionKey = nextKey;
            currentScreenTransitionChangedAtMs = nowMs;
            return false;
        }
        return isScreenTransitionStable(nowMs, currentScreenTransitionChangedAtMs);
    }

    static boolean isScreenTransitionStable(long nowMs, long changedAtMs) {
        return changedAtMs != Long.MIN_VALUE
                && nowMs - changedAtMs >= SCREEN_TRANSITION_STABILITY_MS;
    }

    static String screenTransitionKey(String screenClass, String handlerClass, int syncId, String title,
                                      WciScreenContext context) {
        return (context == null ? WciScreenContext.UNSUPPORTED : context).name()
                + "|" + nullToEmpty(screenClass)
                + "|" + nullToEmpty(handlerClass)
                + "|" + syncId
                + "|" + nullToEmpty(title);
    }

    private static String screenTransitionKey(HandledScreen<?> screen, WciScreenContext context) {
        String screenClass = screen == null ? "" : screen.getClass().getName();
        String title = screen == null || screen.getTitle() == null ? "" : screen.getTitle().getString();
        String handlerClass = "";
        int syncId = -1;
        if (screen != null && screen.getScreenHandler() != null) {
            handlerClass = screen.getScreenHandler().getClass().getName();
            syncId = screen.getScreenHandler().syncId;
        }
        return screenTransitionKey(screenClass, handlerClass, syncId, title, context);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isHeaderDragArea(double x, double y) {
        return isHeaderDragArea(panelX, panelY, panelW, x, y);
    }

    private int maxVisibleRows() {
        int listY = listTopY();
        return Math.max(1, (panelY + panelH - 28 - listY) / ROW_HEIGHT);
    }

    private int listTopY() {
        return listTopY(panelY);
    }

    static int listHeaderY(int panelY) {
        return panelY + LIST_HEADER_Y_OFFSET;
    }

    static int listDividerY(int panelY) {
        return panelY + LIST_DIVIDER_Y_OFFSET;
    }

    static int listTopY(int panelY) {
        return panelY + LIST_TOP_Y_OFFSET;
    }

    private int maxScroll(int rowCount, int maxRows) {
        return Math.max(0, rowCount - maxRows);
    }

    static ScrollbarLayout scrollbarLayout(
            int panelX,
            int panelY,
            int panelW,
            int panelH,
            int rowCount,
            int visibleRows,
            int scrollOffset) {
        int maxScroll = Math.max(0, rowCount - visibleRows);
        if (rowCount <= 0 || visibleRows <= 0 || maxScroll <= 0) {
            return ScrollbarLayout.hidden();
        }

        int trackX = panelX + panelW - 7 - SCROLLBAR_WIDTH;
        int trackY = listTopY(panelY);
        int trackHeight = Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, Math.min(visibleRows, rowCount) * ROW_HEIGHT - 1);
        int thumbHeight = Math.max(SCROLLBAR_MIN_THUMB_HEIGHT,
                trackHeight * Math.min(visibleRows, rowCount) / rowCount);
        thumbHeight = Math.min(thumbHeight, trackHeight);
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int safeOffset = clamp(scrollOffset, 0, maxScroll);
        int thumbY = trackY + (thumbTravel == 0 ? 0 : Math.round((float) thumbTravel * safeOffset / maxScroll));
        return new ScrollbarLayout(
                true,
                new WciShoppingMenuLauncherButton.Bounds(trackX, trackY, SCROLLBAR_WIDTH, trackHeight),
                new WciShoppingMenuLauncherButton.Bounds(trackX, thumbY, SCROLLBAR_WIDTH, thumbHeight),
                maxScroll);
    }

    static int scrollbarScrollOffset(ScrollbarLayout layout, int thumbY) {
        if (layout == null || !layout.visible() || layout.maxScroll() <= 0) {
            return 0;
        }
        int thumbTravel = Math.max(0, layout.track().height() - layout.thumb().height());
        if (thumbTravel == 0) {
            return 0;
        }
        int clampedThumbY = clamp(thumbY, layout.track().y(), layout.track().y() + thumbTravel);
        float ratio = (float) (clampedThumbY - layout.track().y()) / thumbTravel;
        return clamp(Math.round(ratio * layout.maxScroll()), 0, layout.maxScroll());
    }

    static CustomColor rowNameTextColor(RequirementType type) {
        return type == RequirementType.MATERIAL ? MATERIAL_TEXT : INGREDIENT_TEXT;
    }

    static CustomColor rowTypeTextColor(RequirementType type) {
        return type == RequirementType.MATERIAL ? MATERIAL_DIM : INGREDIENT_DIM;
    }

    static int panelBorderColor() {
        return PANEL_BORDER.asInt();
    }

    static int panelAccentColor() {
        return PANEL_ACCENT.asInt();
    }

    static int scrollbarTrackColor() {
        return SCROLL_TRACK;
    }

    static int scrollbarThumbColor() {
        return SCROLL_THUMB;
    }

    static boolean usesWynnExtrasCustomButtonStyle() {
        return BUTTON_CUSTOM_SCALE > 0;
    }

    static String importTooltipText() {
        return IMPORT_TOOLTIP;
    }

    static String clearTooltipText() {
        return CLEAR_TOOLTIP;
    }

    static String speedTooltipText() {
        return SPEED_TOOLTIP;
    }

    static String qtyTooltipText() {
        return QTY_TOOLTIP;
    }

    static String closeTooltipText() {
        return CLOSE_TOOLTIP;
    }

    private static int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    private String trimToWidth(String text, int maxWidth, float textScale) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int logicalMaxWidth = Math.max(0, (int) (maxWidth / Math.max(0.01f, textScale)));
        if (textRenderer.getWidth(text) <= logicalMaxWidth) return text;
        String suffix = "...";
        int suffixWidth = textRenderer.getWidth(suffix);
        if (logicalMaxWidth <= suffixWidth) return textRenderer.trimToWidth(text, logicalMaxWidth);
        return textRenderer.trimToWidth(text, logicalMaxWidth - suffixWidth) + suffix;
    }

    public record PositionState(boolean customPosition, int x, int y) {}

    public record ToggleResult(boolean open, boolean handled, String message) {}

    public record ControlLayout(
            WciShoppingMenuLauncherButton.Bounds importButton,
            WciShoppingMenuLauncherButton.Bounds clearButton,
            WciShoppingMenuLauncherButton.Bounds speedButton,
            WciShoppingMenuLauncherButton.Bounds outputButton) {
        public List<WciShoppingMenuLauncherButton.Bounds> buttons() {
            return List.of(importButton, clearButton, speedButton, outputButton);
        }
    }

    public record ScrollbarLayout(
            boolean visible,
            WciShoppingMenuLauncherButton.Bounds track,
            WciShoppingMenuLauncherButton.Bounds thumb,
            int maxScroll) {
        static ScrollbarLayout hidden() {
            var hidden = WciShoppingMenuLauncherButton.Bounds.hidden();
            return new ScrollbarLayout(false, hidden, hidden, 0);
        }
    }

    private record RenderedRow(WciShoppingListFormatter.Row row, WciRowDetail detail) {}

    private record RowHit(WciShoppingListFormatter.Row row, WciRowDetail detail, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        }
    }

    private interface TooltipWidget {
        String tooltipText();
    }

    private static final class MenuButton extends Widget implements TooltipWidget {
        private final Supplier<String> labelSupplier;
        private final String tooltipText;
        private final Runnable action;

        private MenuButton(String label, String tooltipText, Runnable action) {
            this(() -> label, tooltipText, action);
        }

        private MenuButton(Supplier<String> labelSupplier, String tooltipText, Runnable action) {
            this.labelSupplier = labelSupplier;
            this.tooltipText = tooltipText == null ? "" : tooltipText;
            this.action = action;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButtonCustom(x, y, width, height, BUTTON_CUSTOM_SCALE, hovered, false);
            String label = labelSupplier == null ? "" : labelSupplier.get();
            ui.drawCenteredText(label, x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 0.75f);
        }

        @Override
        protected boolean onClick(int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            action.run();
            return true;
        }

        @Override
        public String tooltipText() {
            return tooltipText;
        }
    }

    private final class OutputButton extends Widget implements TooltipWidget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButtonCustom(x, y, width, height, BUTTON_CUSTOM_SCALE, hovered, false);
            ui.drawCenteredText(outputButtonLabel(), x + width / 2f, y + height / 2f,
                    CustomColor.fromHexString("FFFFFF"), 0.75f);
        }

        @Override
        protected boolean onClick(int button) {
            return changeOutputCount(button);
        }

        @Override
        public String tooltipText() {
            return QTY_TOOLTIP;
        }
    }

    private static final class HeaderCloseButton extends Widget implements TooltipWidget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            CustomColor bg = hovered
                    ? CustomColor.fromHexString("7A3D2E").withAlpha(0.92f)
                    : CustomColor.fromHexString("3A2A1F").withAlpha(0.88f);
            ui.drawRect(x, y, width, height, bg);
            ui.drawCenteredText("X", x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 0.75f);
        }

        @Override
        protected boolean onClick(int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            WciShoppingMenuExtension.close();
            return true;
        }

        @Override
        public String tooltipText() {
            return CLOSE_TOOLTIP;
        }
    }
}
