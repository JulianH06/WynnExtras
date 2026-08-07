package julianh06.wynnextras.features.shoppinglist.ui;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.crafting.data.CraftingDataService;
import julianh06.wynnextras.features.shoppinglist.ShoppingListFeature;
import julianh06.wynnextras.features.shoppinglist.cart.ShoppingEntry;
import julianh06.wynnextras.features.shoppinglist.model.RequirementType;
import julianh06.wynnextras.features.shoppinglist.service.ShoppingCartService;
import julianh06.wynnextras.features.shoppinglist.service.ShoppingListHaveCountService;
import julianh06.wynnextras.features.shoppinglist.service.ShoppingListEntryCatalog;
import julianh06.wynnextras.features.shoppinglist.service.ShoppingListRequirementCalculator;
import julianh06.wynnextras.features.shoppinglist.service.ShoppingListTradeMarketPurchaseService;
import julianh06.wynnextras.features.shoppinglist.service.ShoppingListTradeMarketSearchService;
import julianh06.wynnextras.features.shoppinglist.service.ShoppingListTextCleaner;
import julianh06.wynnextras.utils.HandledScreenAccess;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.UI.WEMenuExtension;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class ShoppingListMenuExtension extends WEMenuExtension {
    private static final int PANEL_WIDTH = 190;
    private static final int PANEL_HEIGHT = 222;
    private static final int MIN_PANEL_WIDTH = 200;
    private static final int MIN_PANEL_HEIGHT = 154;
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
    private static final int EDIT_BUTTON_SIZE = 10;
    private static final int EDITOR_TYPE_Y_OFFSET = 31;
    private static final int EDITOR_NAME_Y_OFFSET = 51;
    private static final int EDITOR_SUGGESTION_Y_OFFSET = 70;
    private static final int EDITOR_SUGGESTION_HEIGHT = 13;
    private static final int LIST_HEADER_Y_OFFSET = 64;
    private static final int LIST_DIVIDER_Y_OFFSET = 72;
    private static final int LIST_TOP_Y_OFFSET = 74;
    private static final int PURCHASE_SECTION_HEIGHT = 34;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_GAP = 3;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 16;
    private static final int HEADER_DRAG_HEIGHT = 22;
    private static final int RESIZE_GRIP_SIZE = 5;
    private static final float SCROLL_SPEED = 0.3f;
    private static final float SCROLL_SNAP = 0.02f;
    private static final float TITLE_SCALE = 1.0f;
    private static final float TEXT_SCALE = 0.75f;
    private static final int VANILLA_PANEL_SCALE = 4;
    private static final int VANILLA_PANEL_SIDE_OFFSET = 3;
    private static final int VANILLA_PANEL_BOTTOM_OFFSET = 3;
    private static final CustomColor TEXT_DIM = CustomColor.fromHexString("C7B9A7");
    private static final CustomColor MATERIAL_TEXT = CustomColor.fromHexString("8FC7D2");
    private static final CustomColor MATERIAL_DIM = CustomColor.fromHexString("83A9B0");
    private static final CustomColor INGREDIENT_TEXT = CustomColor.fromHexString("F0C46A");
    private static final CustomColor INGREDIENT_DIM = CustomColor.fromHexString("C39A51");
    private static final CustomColor ROW_BG = CustomColor.fromHexString("1F1812").withAlpha(0.58f);
    private static final CustomColor ROW_HOVER = CustomColor.fromHexString("332519").withAlpha(0.78f);
    private static final int BUTTON_NINE_SLICE_SCALE = 5;
    private static final int BUTTON_CORNER_SIZE = 2;
    private static final String IMPORT_TOOLTIP = "Import a WynnBuilder crafting link from the clipboard.";
    private static final String CLEAR_TOOLTIP = "Clear the current shopping list.";
    private static final String SPEED_TOOLTIP = "Profession Speed: halves material needs only. Ingredients are unchanged.";
    private static final String AMOUNT_TOOLTIP = "Output amount.\nClick +1, Shift-click +10, Right-click -1, Shift-right-click reset.";
    private static final String CLOSE_TOOLTIP = "Close the shopping list.";
    private static final String CLOSE_KEYBIND_TOOLTIP = "You can also toggle this list using [%s].";
    private static final String CLOSE_KEYBIND_CONFIG_TOOLTIP = "You can bind this key in the wynnextras config.";
    private static final String ADD_TOOLTIP = "Add an ingredient or material manually.";
    private static final long SCREEN_TRANSITION_STABILITY_MS = 450L;
    private static final ShoppingListHaveCountService HAVE_COUNT_SERVICE = new ShoppingListHaveCountService();
    private static Screen activeScreen;
    private static ShoppingListMenuExtension activeScreenExtension;

    private final ShoppingCartService service = ShoppingListFeature.shoppingCartService();
    private final ShoppingListHaveCountService haveCountService = HAVE_COUNT_SERVICE;
    private final List<RowHit> rowHits = new ArrayList<>();
    private final List<SuggestionHit> suggestionHits = new ArrayList<>();
    private final MenuButton addButton = new MenuButton("Add", ADD_TOOLTIP, this::openAddEditor);
    private final MenuButton importButton = new MenuButton("Import", IMPORT_TOOLTIP, this::importClipboard);
    private final MenuButton clearButton = new MenuButton("Clear", CLEAR_TOOLTIP, this::clearCart);
    private final MenuButton speedButton = new MenuButton(this::speedButtonLabel, SPEED_TOOLTIP, this::toggleProfessionSpeed);
    private final OutputButton outputButton = new OutputButton();
    private final MenuButton buyRemainingButton = new MenuButton(this::buyRemainingButtonLabel, "", this::buyRemaining);
    private final MenuButton buyNeededButton = new MenuButton(this::buyNeededButtonLabel, "", this::buyNeeded);
    private final HeaderCloseButton closeButton = new HeaderCloseButton();
    private final TextInputWidget editorNameInput = editorTextInput("Name or search...");
    private final TextInputWidget editorAmountInput = editorAmountInput();
    private final MenuButton editorTypeButton = new MenuButton(this::editorTypeLabel, "Switch entry type.", this::toggleEditorType);
    private final MenuButton editorTierButton = new MenuButton(this::editorTierLabel, "Material tier.", this::cycleEditorTier);
    private final MenuButton editorSaveButton = new MenuButton(this::editorSaveLabel, "Save this entry.", this::saveEditor);
    private final MenuButton editorCancelButton = new MenuButton("Cancel", "Discard changes.", this::closeEditor);
    private final MenuButton editorDeleteButton = new MenuButton(this::editorDeleteLabel, "Remove this entry.", this::deleteEditorEntry);
    private final MenuButton editorConflictAddButton = new MenuButton("Add amounts", "Add this amount to the existing entry.",
            () -> resolveEditorConflict(ShoppingCartService.ExistingEntryPolicy.ADD));
    private final MenuButton editorConflictReplaceButton = new MenuButton("Replace", "Replace the existing amount.",
            () -> resolveEditorConflict(ShoppingCartService.ExistingEntryPolicy.REPLACE));
    private final MenuButton editorConflictBackButton = new MenuButton("Back", "Return to editing.", this::clearEditorConflict);

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int lastRenderMouseX = -1;
    private int lastRenderMouseY = -1;
    private static float scrollOffset = 0;
    private static float targetScrollOffset = 0;
    private ScrollbarLayout scrollbarLayout = ScrollbarLayout.hidden();
    private boolean draggingScrollbar = false;
    private double scrollbarDragOffsetY = 0;
    private boolean draggingPanel = false;
    private boolean panelDragMoved = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private ResizeEdges resizeEdges = ResizeEdges.NONE;
    private int resizeStartMouseX = 0;
    private int resizeStartMouseY = 0;
    private int resizeStartX = 0;
    private int resizeStartY = 0;
    private int resizeStartWidth = PANEL_WIDTH;
    private int resizeStartHeight = PANEL_HEIGHT;
    private ShoppingListPlacementContext resizePlacementContext = ShoppingListPlacementContext.OTHER;
    private ShoppingListScreenContext screenContext = ShoppingListScreenContext.UNSUPPORTED;
    private ShoppingListPlacementContext placementContext = ShoppingListPlacementContext.OTHER;
    private ShoppingListPlacementContext dragPlacementContext = ShoppingListPlacementContext.OTHER;
    private static ShoppingListHaveCountService.SourceSnapshot cachedHaveCountSnapshot;
    private static Object cachedHaveCountCart;
    private static int cachedHaveCountCartSize = -1;
    private static int cachedHaveCountCartSignature = 0;
    private static int cachedOutputCount = -1;
    private static boolean cachedProfessionSpeed = false;
    private static List<RenderedRow> cachedRenderedRows = List.of();
    private String currentScreenTransitionKey = "";
    private long currentScreenTransitionChangedAtMs = Long.MIN_VALUE;
    private ShoppingListTradeMarketPurchaseService.PurchaseContext purchaseContext;
    private EditorMode editorMode = EditorMode.CLOSED;
    private ShoppingEntry editingOriginal;
    private RequirementType editorType = RequirementType.INGREDIENT;
    private int editorTier = 1;
    private int selectedSuggestionIndex = 0;
    private boolean suggestionSelectionActive = false;
    private String editorError = "";
    private boolean editorConflict = false;
    private boolean deleteArmed = false;

    public ShoppingListMenuExtension() {
        rootWidgets.add(importButton);
        rootWidgets.add(addButton);
        rootWidgets.add(clearButton);
        rootWidgets.add(speedButton);
        rootWidgets.add(outputButton);
        rootWidgets.add(buyRemainingButton);
        rootWidgets.add(buyNeededButton);
        rootWidgets.add(closeButton);
        rootWidgets.add(editorNameInput);
        rootWidgets.add(editorAmountInput);
        rootWidgets.add(editorTypeButton);
        rootWidgets.add(editorTierButton);
        rootWidgets.add(editorSaveButton);
        rootWidgets.add(editorCancelButton);
        rootWidgets.add(editorDeleteButton);
        rootWidgets.add(editorConflictAddButton);
        rootWidgets.add(editorConflictReplaceButton);
        rootWidgets.add(editorConflictBackButton);
        editorNameInput.setOnChange(ignored -> editorFieldsChanged());
        editorAmountInput.setOnChange(ignored -> editorFieldsChanged());
        setEditorWidgetsVisible(false);
    }

    public static boolean isOpen() {
        return WynnExtrasConfig.INSTANCE.shoppingListMenuEnabled;
    }

    public static boolean isVisible() {
        return WynnExtrasConfig.INSTANCE.shoppingListMenuEnabled;
    }

    public static boolean toggleFromCommand() {
        return toggleFromCommand(ShoppingListScreenContext.UNSUPPORTED).open();
    }

    public static ToggleResult toggleFromCommand(ShoppingListScreenContext context) {
        if (isOpen()) {
            close();
            return new ToggleResult(false, true, "Shopping list disabled.");
        }
        return showFromCommand(context);
    }

    public static ToggleResult showFromCommand(ShoppingListScreenContext context) {
        if (!ShoppingListMenuRenderPolicy.canOpenFromCommand(context)) {
            return new ToggleResult(false, false, "Shopping List unavailable on this screen.");
        }
        show();
        return new ToggleResult(true, true, "Shopping list enabled.");
    }

    public static boolean toggleFromHotkey(ShoppingListScreenContext context) {
        if (isOpen()) {
            close();
            return false;
        }
        show();
        return true;
    }

    public static void show() {
        setEnabled(true);
    }

    public static void close() {
        if (activeScreenExtension != null) {
            activeScreenExtension.closeEditor();
        }
        setEnabled(false);
    }

    public static boolean shouldRender(ShoppingListScreenContext context) {
        return ShoppingListMenuRenderPolicy.shouldRenderMenu(isOpen(), context);
    }

    public static boolean isToggleKey(int keyCode) {
        return WynnExtrasConfig.INSTANCE.shoppingListToggleKey != GLFW.GLFW_KEY_UNKNOWN
                && keyCode == WynnExtrasConfig.INSTANCE.shoppingListToggleKey;
    }

    public static boolean handleGlobalMouseScrolled(double verticalAmount) {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        return currentScreen != null
                && currentScreen == activeScreen
                && activeScreenExtension != null
                && activeScreenExtension.mouseScrolled(
                        activeScreenExtension.lastRenderMouseX,
                        activeScreenExtension.lastRenderMouseY,
                        verticalAmount);
    }

    public static ShoppingListMenuLauncherButton.Bounds closeButtonBounds(int panelX, int panelY, int panelWidth) {
        return new ShoppingListMenuLauncherButton.Bounds(
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

    public static void resetPosition() {
        resetPosition(WynnExtrasConfig.INSTANCE);
        WynnExtrasConfig.save();
    }

    public static boolean clearFromCommand() {
        ShoppingListFeature.shoppingCartService().clear();
        return !ShoppingListFeature.consumeSaveFailure();
    }

    public static void copyFromCommand() {
        MinecraftClient.getInstance().keyboard.setClipboard(
                ShoppingListFormatter.format(ShoppingListFeature.shoppingCartService().cart()));
    }

    private static void setEnabled(boolean enabled) {
        if (WynnExtrasConfig.INSTANCE.shoppingListMenuEnabled != enabled) {
            WynnExtrasConfig.INSTANCE.shoppingListMenuEnabled = enabled;
            WynnExtrasConfig.save();
        }
    }

    public void setScreenContext(ShoppingListScreenContext screenContext) {
        this.screenContext = screenContext == null ? ShoppingListScreenContext.UNSUPPORTED : screenContext;
        this.placementContext = this.screenContext.placementContext();
    }

    public void setPlacementContext(ShoppingListPlacementContext placementContext) {
        this.placementContext = placementContext == null ? ShoppingListPlacementContext.OTHER : placementContext;
    }

    public boolean consumesHover(HandledScreen<?> screen, double mouseX, double mouseY) {
        if (!shouldRender(screenContext)) return false;
        computeScale();
        updatePanelBounds(screen);
        return containsPanel(mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!shouldRender(screenContext)) {
            setButtonsVisible(false);
            setEditorWidgetsVisible(false);
            rowHits.clear();
            return;
        }
        Screen current = MinecraftClient.getInstance().currentScreen;
        HandledScreen<?> handledScreen = current instanceof HandledScreen<?> screen ? screen : null;
        if (handledScreen == null
                && screenContext != ShoppingListScreenContext.HUD
                && screenContext != ShoppingListScreenContext.CHAT) {
            setButtonsVisible(false);
            setEditorWidgetsVisible(false);
            rowHits.clear();
            return;
        }
        if (current != null) {
            activeScreen = current;
            activeScreenExtension = this;
            lastRenderMouseX = mouseX;
            lastRenderMouseY = mouseY;
        }

        boolean screenStableForCounts = handledScreen == null
                || updateScreenTransitionState(handledScreen, System.currentTimeMillis());
        purchaseContext = screenContext == ShoppingListScreenContext.CHAT
                ? ShoppingListTradeMarketPurchaseService.currentContext()
                : null;
        updatePanelBounds(handledScreen);
        drawPanel(mouseX, mouseY);
        if (isEditorOpen()) {
            rowHits.clear();
            scrollbarLayout = ScrollbarLayout.hidden();
            setButtonsVisible(false);
            layoutEditorWidgets();
            drawEditor(mouseX, mouseY);
        } else {
            setEditorWidgetsVisible(false);
            layoutButtons();
            drawRows(mouseX, mouseY, delta, screenStableForCounts);
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!shouldRender(screenContext)) return;
        List<String> controlTooltip = hoveredControlTooltip();
        if (!controlTooltip.isEmpty()) {
            drawTooltip(ctx, controlTooltip, mouseX, mouseY);
            return;
        }
        RowHit rowHit = rowAt(mouseX, mouseY);
        if (!shouldRenderRowTooltip(false, rowHit != null)) return;

        if (rowHit.containsEdit(mouseX, mouseY)) {
            drawTooltip(ctx, List.of("Edit " + rowHit.detail().displayNameWithTier()), mouseX, mouseY);
            return;
        }

        drawTextTooltip(ctx, rowHit.detail().tooltipText(ShoppingListMenuRenderPolicy.primaryRowAction(screenContext)),
                mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (!shouldRender(screenContext)) return false;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && startResize(x, y)) {
            return true;
        }
        boolean editorWasOpen = isEditorOpen();
        boolean conflictWasOpen = editorConflict;
        if (super.mouseClicked(x, y, button)) {
            if (!editorWasOpen && isEditorOpen()) {
                setFocusedWidget(editorNameInput);
            } else if (conflictWasOpen && !editorConflict && isEditorOpen()) {
                setFocusedWidget(editorNameInput);
            }
            return true;
        }
        if (!containsPanel(x, y)) {
            return false;
        }

        if (isEditorOpen()) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && handleSuggestionClick(x, y)) {
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isHeaderDragArea(x, y)) {
                draggingPanel = true;
                panelDragMoved = false;
                dragPlacementContext = placementContext;
                dragOffsetX = (int) x - panelX;
                dragOffsetY = (int) y - panelY;
            }
            return true;
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
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && rowHit.containsEdit(x, y)) {
                playButtonClickSound();
                openEditEditor(rowHit.entry(), rowHit.baseAmount());
            } else {
                handleRowClick(rowHit.row(), button);
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (!shouldRender(screenContext)) {
            cancelPanelDrag();
            cancelScrollbarDrag();
            cancelResize();
            return false;
        }
        if (resizeEdges.active() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            cancelResize();
            WynnExtrasConfig.save();
            return true;
        }
        if (draggingScrollbar && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            cancelScrollbarDrag();
            return true;
        }
        if (draggingPanel && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingPanel = false;
            dragPlacementContext = ShoppingListPlacementContext.OTHER;
            if (panelDragMoved) {
                WynnExtrasConfig.save();
            }
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (!shouldRender(screenContext)) {
            cancelPanelDrag();
            cancelScrollbarDrag();
            cancelResize();
            return false;
        }
        if (resizeEdges.active() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            resizePanel((int) x, (int) y);
            return true;
        }
        if (draggingScrollbar && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            updateScrollOffsetFromThumb(y - scrollbarDragOffsetY);
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
        dragPlacementContext = ShoppingListPlacementContext.OTHER;
    }

    private void cancelScrollbarDrag() {
        draggingScrollbar = false;
        scrollbarDragOffsetY = 0;
    }

    private boolean startResize(double mouseX, double mouseY) {
        ResizeEdges edges = resizeEdgesAt(mouseX, mouseY);
        if (!edges.active()) {
            return false;
        }
        cancelPanelDrag();
        cancelScrollbarDrag();
        resizeEdges = edges;
        resizeStartMouseX = (int) mouseX;
        resizeStartMouseY = (int) mouseY;
        resizeStartX = panelX;
        resizeStartY = panelY;
        resizeStartWidth = panelW;
        resizeStartHeight = panelH;
        resizePlacementContext = placementContext;
        setPosition(WynnExtrasConfig.INSTANCE, resizePlacementContext, panelX, panelY);
        return true;
    }

    private void resizePanel(int mouseX, int mouseY) {
        int dx = mouseX - resizeStartMouseX;
        int dy = mouseY - resizeStartMouseY;
        int left = resizeStartX;
        int top = resizeStartY;
        int right = resizeStartX + resizeStartWidth;
        int bottom = resizeStartY + resizeStartHeight;

        if (resizeEdges.left()) {
            left = Math.clamp(resizeStartX + dx, PANEL_MARGIN, right - MIN_PANEL_WIDTH);
        } else if (resizeEdges.right()) {
            right = Math.clamp(right + dx, left + MIN_PANEL_WIDTH, screenWidth - PANEL_MARGIN);
        }
        if (resizeEdges.top()) {
            top = Math.clamp(resizeStartY + dy, PANEL_MARGIN, bottom - MIN_PANEL_HEIGHT);
        } else if (resizeEdges.bottom()) {
            bottom = Math.clamp(bottom + dy, top + MIN_PANEL_HEIGHT, screenHeight - PANEL_MARGIN);
        }

        panelX = left;
        panelY = top;
        panelW = right - left;
        panelH = bottom - top;
        WynnExtrasConfig.INSTANCE.shoppingListMenuWidth = panelW;
        WynnExtrasConfig.INSTANCE.shoppingListMenuHeight = panelH;
        setPosition(WynnExtrasConfig.INSTANCE, resizePlacementContext, panelX, panelY);
    }

    private void cancelResize() {
        resizeEdges = ResizeEdges.NONE;
        resizePlacementContext = ShoppingListPlacementContext.OTHER;
    }

    private ResizeEdges resizeEdgesAt(double mouseX, double mouseY) {
        boolean withinHorizontalRange = mouseX >= panelX - RESIZE_GRIP_SIZE
                && mouseX <= panelX + panelW + RESIZE_GRIP_SIZE;
        boolean withinVerticalRange = mouseY >= panelY - RESIZE_GRIP_SIZE
                && mouseY <= panelY + panelH + RESIZE_GRIP_SIZE;
        if (!withinHorizontalRange || !withinVerticalRange) {
            return ResizeEdges.NONE;
        }
        boolean left = Math.abs(mouseX - panelX) <= RESIZE_GRIP_SIZE;
        boolean right = Math.abs(mouseX - (panelX + panelW)) <= RESIZE_GRIP_SIZE;
        boolean top = Math.abs(mouseY - panelY) <= RESIZE_GRIP_SIZE;
        boolean bottom = Math.abs(mouseY - (panelY + panelH)) <= RESIZE_GRIP_SIZE;
        return new ResizeEdges(left, right, top, bottom);
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
            scrollbarDragOffsetY = y - scrollbarLayout.thumb().y();
            return true;
        }
        updateScrollOffsetFromThumb(y - scrollbarLayout.thumb().height() / 2d);
        return true;
    }

    private void updateScrollOffsetFromThumb(double thumbY) {
        targetScrollOffset = smoothScrollbarScrollOffset(scrollbarLayout, thumbY);
    }

    public boolean mouseScrolled(double x, double y, double verticalAmount) {
        if (!shouldRender(screenContext) || !containsPanel(x, y)) return false;
        if (isEditorOpen()) return true;

        List<ShoppingListFormatter.Row> rows = ShoppingListFormatter.rows(service.cart());
        int maxScroll = maxScroll(rows.size(), maxVisibleRows());
        if (maxScroll <= 0) return true;

        targetScrollOffset = Math.clamp(targetScrollOffset - (float) verticalAmount, 0f, (float) maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!shouldRender(screenContext)) {
            return false;
        }
        if (isEditorOpen()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeEditor();
                return true;
            }
            if (editorConflict) {
                if (keyCode == GLFW.GLFW_KEY_TAB) {
                    cycleEditorFocus((modifiers & GLFW.GLFW_MOD_SHIFT) != 0,
                            List.of(editorConflictAddButton, editorConflictReplaceButton, editorConflictBackButton));
                    return true;
                }
                if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
                return true;
            }
            if (editorNameInput.isFocused()
                    && (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN)) {
                List<String> suggestions = editorSuggestions();
                if (!suggestions.isEmpty()) {
                    int direction = keyCode == GLFW.GLFW_KEY_UP ? -1 : 1;
                    selectedSuggestionIndex = Math.floorMod(selectedSuggestionIndex + direction, suggestions.size());
                    suggestionSelectionActive = true;
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                List<Widget> controls = new ArrayList<>();
                controls.add(editorTypeButton);
                if (editorType == RequirementType.MATERIAL) controls.add(editorTierButton);
                controls.add(editorNameInput);
                controls.add(editorAmountInput);
                controls.add(editorSaveButton);
                if (editorMode == EditorMode.EDIT) controls.add(editorDeleteButton);
                controls.add(editorCancelButton);
                cycleEditorFocus((modifiers & GLFW.GLFW_MOD_SHIFT) != 0, controls);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                List<String> suggestions = editorSuggestions();
                if (editorNameInput.isFocused() && suggestionSelectionActive && !suggestions.isEmpty()
                        && selectedSuggestionIndex >= 0 && selectedSuggestionIndex < suggestions.size()) {
                    String selected = suggestions.get(selectedSuggestionIndex);
                    if (selected.equals(editorNameInput.getInput())) {
                        saveEditor();
                    } else {
                        editorNameInput.setInputAndMoveCursorToEnd(selected);
                    }
                } else if (editorAmountInput.isFocused()) {
                    saveEditor();
                } else if (!super.keyPressed(keyCode, scanCode, modifiers)) {
                    saveEditor();
                }
                return true;
            }
            super.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public static boolean handleGlobalCharTyped(char character) {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen == null || currentScreen != activeScreen || activeScreenExtension == null
                || !activeScreenExtension.isEditorOpen()) {
            return false;
        }
        activeScreenExtension.charTyped(character, 0);
        return true;
    }

    public static boolean handleGlobalKeyInput(int keyCode, int scanCode, int action, int modifiers) {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen == null && action == GLFW.GLFW_PRESS && isToggleKey(keyCode)) {
            toggleFromHotkey(ShoppingListScreenContext.HUD);
            return true;
        }
        if (currentScreen == null || currentScreen != activeScreen || activeScreenExtension == null
                || !activeScreenExtension.isEditorOpen()) {
            return false;
        }
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            activeScreenExtension.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return false;
    }

    private void updatePanelBounds(HandledScreen<?> screen) {
        int screenX = screen == null ? 0 : HandledScreenAccess.x(screen);
        int screenY = screen == null ? 0 : HandledScreenAccess.y(screen);
        int guiWidth = screen == null ? 0 : HandledScreenAccess.backgroundWidth(screen);
        int guiHeight = screen == null ? 0 : HandledScreenAccess.backgroundHeight(screen);

        int maxWidth = Math.max(MIN_PANEL_WIDTH, screenWidth - PANEL_MARGIN * 2);
        int maxHeight = Math.max(MIN_PANEL_HEIGHT, screenHeight - PANEL_MARGIN * 2);
        panelW = Math.clamp(WynnExtrasConfig.INSTANCE.shoppingListMenuWidth, MIN_PANEL_WIDTH, maxWidth);
        panelH = Math.clamp(WynnExtrasConfig.INSTANCE.shoppingListMenuHeight, MIN_PANEL_HEIGHT, maxHeight);

        PositionState savedPosition = positionState(WynnExtrasConfig.INSTANCE, placementContext);
        if (savedPosition.customPosition()) {
            ShoppingListMenuLauncherButton.Bounds savedBounds = clampPanelBounds(
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

        List<ShoppingListMenuLauncherButton.Bounds> forbiddenAreas = forbiddenAreas(
                placementContext,
                screenX,
                screenY,
                guiWidth,
                guiHeight,
                screenWidth,
                screenHeight);
        ShoppingListMenuLauncherButton.Bounds defaultBounds = computePanelBounds(
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

    private void movePanelTo(int x, int y, ShoppingListPlacementContext context) {
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

    public static ShoppingListMenuLauncherButton.Bounds computePanelBounds(
            ShoppingListPlacementContext placementContext,
            int screenX,
            int screenY,
            int backgroundWidth,
            int backgroundHeight,
            int panelWidth,
            int panelHeight,
            int screenWidth,
            int screenHeight,
            List<ShoppingListMenuLauncherButton.Bounds> forbiddenAreas) {
        return computePanelBounds(placementContext, screenX, screenY, backgroundWidth, backgroundHeight, panelWidth,
                panelHeight, screenWidth, screenHeight, forbiddenAreas, null);
    }

    public static ShoppingListMenuLauncherButton.Bounds computePanelBounds(
            ShoppingListPlacementContext placementContext,
            int screenX,
            int screenY,
            int backgroundWidth,
            int backgroundHeight,
            int panelWidth,
            int panelHeight,
            int screenWidth,
            int screenHeight,
            List<ShoppingListMenuLauncherButton.Bounds> forbiddenAreas,
            ShoppingListMenuLauncherButton.Bounds bankOverlayPreferredBounds) {
        List<ShoppingListMenuLauncherButton.Bounds> candidates = panelCandidatesFor(
                placementContext,
                screenX,
                screenY,
                backgroundWidth,
                panelWidth,
                panelHeight,
                screenWidth,
                screenHeight,
                bankOverlayPreferredBounds);

        for (ShoppingListMenuLauncherButton.Bounds candidate : candidates) {
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

    private static List<ShoppingListMenuLauncherButton.Bounds> panelCandidatesFor(
            ShoppingListPlacementContext placementContext,
            int screenX,
            int screenY,
            int backgroundWidth,
            int panelWidth,
            int panelHeight,
            int screenWidth,
            int screenHeight,
            ShoppingListMenuLauncherButton.Bounds bankOverlayPreferredBounds) {
        return switch (placementContext) {
            case TRADE_MARKET -> List.of(
                    new ShoppingListMenuLauncherButton.Bounds(screenX + backgroundWidth + PANEL_MARGIN, screenY,
                            panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(screenX - panelWidth - PANEL_MARGIN, screenY,
                            panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN, PANEL_MARGIN,
                            panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(PANEL_MARGIN, PANEL_MARGIN, panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN,
                            screenHeight - panelHeight - PANEL_MARGIN, panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(PANEL_MARGIN, screenHeight - panelHeight - PANEL_MARGIN,
                            panelWidth, panelHeight)
            );
            case BANK_OVERLAY -> bankOverlayPanelCandidates(screenX, screenY, backgroundWidth, panelWidth, panelHeight,
                    screenWidth, screenHeight, bankOverlayPreferredBounds);
            case BANK_VANILLA -> List.of(
                    new ShoppingListMenuLauncherButton.Bounds(BANK_VANILLA_DEFAULT_X, BANK_VANILLA_DEFAULT_Y,
                            panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(screenX + backgroundWidth + PANEL_MARGIN, screenY,
                            panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(screenX - panelWidth - PANEL_MARGIN, screenY,
                            panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN, PANEL_MARGIN,
                            panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(PANEL_MARGIN, PANEL_MARGIN, panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN,
                            screenHeight - panelHeight - PANEL_MARGIN, panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(PANEL_MARGIN, screenHeight - panelHeight - PANEL_MARGIN,
                            panelWidth, panelHeight)
            );
            case OTHER -> List.of(
                    new ShoppingListMenuLauncherButton.Bounds(screenX + backgroundWidth + PANEL_MARGIN, screenY,
                            panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(screenX - panelWidth - PANEL_MARGIN, screenY,
                            panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN, PANEL_MARGIN,
                            panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(PANEL_MARGIN, PANEL_MARGIN, panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN,
                            screenHeight - panelHeight - PANEL_MARGIN, panelWidth, panelHeight),
                    new ShoppingListMenuLauncherButton.Bounds(PANEL_MARGIN, screenHeight - panelHeight - PANEL_MARGIN,
                            panelWidth, panelHeight)
            );
        };
    }

    private static List<ShoppingListMenuLauncherButton.Bounds> bankOverlayPanelCandidates(
            int screenX,
            int screenY,
            int backgroundWidth,
            int panelWidth,
            int panelHeight,
            int screenWidth,
            int screenHeight,
            ShoppingListMenuLauncherButton.Bounds bankOverlayPreferredBounds) {
        List<ShoppingListMenuLauncherButton.Bounds> candidates = new ArrayList<>();
        if (bankOverlayPreferredBounds != null && bankOverlayPreferredBounds.visible()) {
            candidates.add(bankOverlayPreferredBounds);
        }
        candidates.add(new ShoppingListMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN, PANEL_MARGIN,
                panelWidth, panelHeight));
        candidates.add(new ShoppingListMenuLauncherButton.Bounds(screenX - panelWidth - PANEL_MARGIN, screenY,
                panelWidth, panelHeight));
        candidates.add(new ShoppingListMenuLauncherButton.Bounds(screenX + backgroundWidth + PANEL_MARGIN, screenY,
                panelWidth, panelHeight));
        candidates.add(new ShoppingListMenuLauncherButton.Bounds(PANEL_MARGIN, PANEL_MARGIN, panelWidth, panelHeight));
        candidates.add(new ShoppingListMenuLauncherButton.Bounds(screenWidth - panelWidth - PANEL_MARGIN,
                screenHeight - panelHeight - PANEL_MARGIN, panelWidth, panelHeight));
        candidates.add(new ShoppingListMenuLauncherButton.Bounds(PANEL_MARGIN, screenHeight - panelHeight - PANEL_MARGIN,
                panelWidth, panelHeight));
        return List.copyOf(candidates);
    }

    public static ShoppingListMenuLauncherButton.Bounds clampPanelBounds(int x, int y, int panelWidth, int panelHeight,
                                                                        int screenWidth, int screenHeight) {
        int maxX = Math.max(PANEL_MARGIN, screenWidth - panelWidth - PANEL_MARGIN);
        int maxY = Math.max(PANEL_MARGIN, screenHeight - panelHeight - PANEL_MARGIN);
        return new ShoppingListMenuLauncherButton.Bounds(
                clampValue(x, PANEL_MARGIN, maxX),
                clampValue(y, PANEL_MARGIN, maxY),
                panelWidth,
                panelHeight);
    }

    static void resetPosition(WynnExtrasConfig config) {
        config.shoppingListMenuDefaultPosition.reset();
        config.shoppingListMenuTradePosition.reset();
        config.shoppingListMenuBankOverlayPosition.reset();
        config.shoppingListMenuBankVanillaPosition.reset();
    }

    private static PositionState positionState(WynnExtrasConfig config, ShoppingListPlacementContext context) {
        WynnExtrasConfig.ShoppingListPosition position = position(config, context);
        return new PositionState(position.isSet(), position.x, position.y);
    }

    private static void setPosition(WynnExtrasConfig config, ShoppingListPlacementContext placementContext, int x, int y) {
        position(config, placementContext).set(x, y);
    }

    private static WynnExtrasConfig.ShoppingListPosition position(WynnExtrasConfig config, ShoppingListPlacementContext context) {
        return switch (context) {
            case TRADE_MARKET -> config.shoppingListMenuTradePosition;
            case BANK_OVERLAY -> config.shoppingListMenuBankOverlayPosition;
            case BANK_VANILLA -> config.shoppingListMenuBankVanillaPosition;
            case OTHER -> config.shoppingListMenuDefaultPosition;
        };
    }

    private List<ShoppingListMenuLauncherButton.Bounds> forbiddenAreas(ShoppingListPlacementContext placementContext,
                                                                      int screenX,
                                                                      int screenY,
                                                                      int backgroundWidth,
                                                                      int backgroundHeight,
                                                                      int screenWidth,
                                                                      int screenHeight) {
        List<ShoppingListMenuLauncherButton.Bounds> forbiddenAreas = new ArrayList<>();
        if (placementContext == ShoppingListPlacementContext.BANK_VANILLA
                || placementContext == ShoppingListPlacementContext.TRADE_MARKET) {
            forbiddenAreas.add(new ShoppingListMenuLauncherButton.Bounds(screenX - PANEL_MARGIN, screenY - PANEL_MARGIN,
                    backgroundWidth + PANEL_MARGIN * 2, backgroundHeight + PANEL_MARGIN * 2));
        }
        return forbiddenAreas;
    }

    private ShoppingListMenuLauncherButton.Bounds bankOverlayBounds() {
        if (placementContext != ShoppingListPlacementContext.BANK_OVERLAY) {
            return null;
        }
        return bankOverlayDefaultBounds(screenWidth, screenHeight, panelW, panelH);
    }

    public static ShoppingListMenuLauncherButton.Bounds bankOverlayDefaultBounds(int screenWidth, int screenHeight,
                                                                               int panelWidth, int panelHeight) {
        return clampPanelBounds(BANK_OVERLAY_DEFAULT_X, BANK_OVERLAY_DEFAULT_Y, panelWidth, panelHeight,
                screenWidth, screenHeight);
    }

    private static boolean overlapsAny(ShoppingListMenuLauncherButton.Bounds bounds,
                                       List<ShoppingListMenuLauncherButton.Bounds> forbiddenAreas) {
        for (ShoppingListMenuLauncherButton.Bounds forbiddenArea : forbiddenAreas) {
            if (bounds.overlaps(forbiddenArea)) {
                return true;
            }
        }
        return false;
    }

    private static boolean fitsInsideScreen(ShoppingListMenuLauncherButton.Bounds bounds, int screenWidth, int screenHeight) {
        return bounds.x() >= PANEL_MARGIN
                && bounds.y() >= PANEL_MARGIN
                && bounds.x() + bounds.width() <= screenWidth - PANEL_MARGIN
                && bounds.y() + bounds.height() <= screenHeight - PANEL_MARGIN;
    }

    private static int clampValue(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    private void drawPanel(int mouseX, int mouseY) {
        int innerTopOffset = listTopY() - panelY - VANILLA_PANEL_SCALE;
        if(!isEditorOpen()) {
            ui.drawVanillaPanel(panelX, panelY, panelW, panelH, VANILLA_PANEL_SCALE,
                    VANILLA_PANEL_SIDE_OFFSET, VANILLA_PANEL_SIDE_OFFSET,
                    innerTopOffset, VANILLA_PANEL_BOTTOM_OFFSET);
        } else {
            ui.drawVanillaPanel(panelX, panelY, panelW, panelH, VANILLA_PANEL_SCALE);
        }
        CustomColor panelText = CustomColor.fromHexString("FFFFFF");
        ui.drawText(WynnExtras.addWynnExtrasPrefix("Shopping List"), panelX + 8, panelY + 7, panelText, TITLE_SCALE);
        if (purchaseContext != null && !isEditorOpen()) {
            String buyingText = "Buying " + purchaseContext.itemName()
                    + " | Have: " + purchaseContext.have()
                    + " | Need: " + purchaseContext.needed();
            ui.drawText(trimToWidth(buyingText, panelW - 16, TEXT_SCALE),
                    panelX + 8, purchaseLabelY(), panelText, TEXT_SCALE);
        }
        if (!isEditorOpen()) {
            ui.drawText("Have/Need", panelX + panelW - 70, listHeaderY() - 2, panelText, TEXT_SCALE);
        }
        drawResizeHandles(mouseX, mouseY);
    }

    private void drawResizeHandles(int mouseX, int mouseY) {
        ResizeEdges hoveredEdges = resizeEdges.active() ? resizeEdges : resizeEdgesAt(mouseX, mouseY);
        int color = panelAccentColor();
        if (hoveredEdges.left()) {
            drawContext.fill(panelX, panelY, panelX + 2, panelY + panelH, color);
        }
        if (hoveredEdges.right()) {
            drawContext.fill(panelX + panelW - 2, panelY, panelX + panelW, panelY + panelH, color);
        }
        if (hoveredEdges.top()) {
            drawContext.fill(panelX, panelY, panelX + panelW, panelY + 2, color);
        }
        if (hoveredEdges.bottom()) {
            drawContext.fill(panelX, panelY + panelH - 2, panelX + panelW, panelY + panelH, color);
        }
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
        drawTextTooltip(ctx, lines.stream().map(Text::literal).toList(), mouseX, mouseY);
    }

    private void drawTextTooltip(DrawContext ctx, List<? extends Text> lines, int mouseX, int mouseY) {
        if (lines == null || lines.isEmpty()) return;
        ctx.drawTooltipImmediately(
                MinecraftClient.getInstance().textRenderer,
                lines.stream()
                        .map(line -> TooltipComponent.of(line.asOrderedText()))
                        .toList(),
                mouseX,
                mouseY,
                HoveredTooltipPositioner.INSTANCE,
                null);
    }

    private void layoutButtons() {
        setButtonsVisible(true);
        ControlLayout layout = controlLayout(panelX, panelY, panelW);
        addButton.setBounds(layout.addButton().x(), layout.addButton().y(),
                layout.addButton().width(), layout.addButton().height());
        importButton.setBounds(layout.importButton().x(), layout.importButton().y(),
                layout.importButton().width(), layout.importButton().height());
        clearButton.setBounds(layout.clearButton().x(), layout.clearButton().y(),
                layout.clearButton().width(), layout.clearButton().height());
        speedButton.setBounds(layout.speedButton().x(), layout.speedButton().y(),
                layout.speedButton().width(), layout.speedButton().height());
        outputButton.setBounds(layout.outputButton().x(), layout.outputButton().y(),
                layout.outputButton().width(), layout.outputButton().height());
        layoutPurchaseButtons();
        var closeBounds = closeButtonBounds(panelX, panelY, panelW);
        closeButton.setBounds(closeBounds.x(), closeBounds.y(), closeBounds.width(), closeBounds.height());
    }

    private void layoutPurchaseButtons() {
        if (purchaseContext == null) {
            buyRemainingButton.setVisible(false);
            buyNeededButton.setVisible(false);
            return;
        }
        int x = panelX + 8;
        int y = panelY + 74;
        int width = panelW - 16;
        int gap = 4;
        if (purchaseContext.needsRemainingButton()) {
            int buttonWidth = (width - gap) / 2;
            buyRemainingButton.setVisible(true);
            buyRemainingButton.setBounds(x, y, buttonWidth, BUTTON_HEIGHT);
            buyNeededButton.setBounds(x + buttonWidth + gap, y, width - buttonWidth - gap, BUTTON_HEIGHT);
        } else {
            buyRemainingButton.setVisible(false);
            buyNeededButton.setBounds(x, y, width, BUTTON_HEIGHT);
        }
        buyNeededButton.setVisible(true);
    }

    private void layoutEditorWidgets() {
        setEditorWidgetsVisible(true);
        closeButton.setVisible(true);
        var closeBounds = closeButtonBounds(panelX, panelY, panelW);
        closeButton.setBounds(closeBounds.x(), closeBounds.y(), closeBounds.width(), closeBounds.height());

        int x = panelX + 8;
        int width = panelW - 16;
        if (editorConflict) {
            editorNameInput.setVisible(false);
            editorAmountInput.setVisible(false);
            editorTypeButton.setVisible(false);
            editorTierButton.setVisible(false);
            editorSaveButton.setVisible(false);
            editorCancelButton.setVisible(false);
            editorDeleteButton.setVisible(false);
            int gap = 4;
            int buttonWidth = (width - gap) / 2;
            int y = panelY + panelH - 44;
            editorConflictAddButton.setBounds(x, y, buttonWidth, BUTTON_HEIGHT);
            editorConflictReplaceButton.setBounds(x + buttonWidth + gap, y, width - buttonWidth - gap, BUTTON_HEIGHT);
            editorConflictBackButton.setBounds(x, y + BUTTON_HEIGHT + 4, width, BUTTON_HEIGHT);
            return;
        }

        editorConflictAddButton.setVisible(false);
        editorConflictReplaceButton.setVisible(false);
        editorConflictBackButton.setVisible(false);
        int gap = 4;
        int typeWidth = editorType == RequirementType.MATERIAL ? (width - gap) * 2 / 3 : width;
        editorTypeButton.setBounds(x, panelY + EDITOR_TYPE_Y_OFFSET, typeWidth, BUTTON_HEIGHT);
        editorTierButton.setVisible(editorType == RequirementType.MATERIAL);
        if (editorTierButton.isVisible()) {
            editorTierButton.setBounds(x + typeWidth + gap, panelY + EDITOR_TYPE_Y_OFFSET,
                    width - typeWidth - gap, BUTTON_HEIGHT);
        }
        editorNameInput.setBounds(x, panelY + EDITOR_NAME_Y_OFFSET, width, BUTTON_HEIGHT);

        int actionY = panelY + panelH - 24;
        int amountY = actionY - BUTTON_HEIGHT - 4;
        int amountLabelWidth = 42;
        editorAmountInput.setBounds(x + amountLabelWidth, amountY, width - amountLabelWidth, BUTTON_HEIGHT);

        if (editorMode == EditorMode.EDIT) {
            int actionWidth = (width - gap * 2) / 3;
            editorSaveButton.setBounds(x, actionY, actionWidth, BUTTON_HEIGHT);
            editorDeleteButton.setVisible(true);
            editorDeleteButton.setBounds(x + actionWidth + gap, actionY, actionWidth, BUTTON_HEIGHT);
            editorCancelButton.setBounds(x + (actionWidth + gap) * 2, actionY,
                    width - actionWidth * 2 - gap * 2, BUTTON_HEIGHT);
        } else {
            int actionWidth = (width - gap) / 2;
            editorSaveButton.setBounds(x, actionY, actionWidth, BUTTON_HEIGHT);
            editorDeleteButton.setVisible(false);
            editorCancelButton.setBounds(x + actionWidth + gap, actionY, width - actionWidth - gap, BUTTON_HEIGHT);
        }
    }

    private void drawEditor(int mouseX, int mouseY) {
        suggestionHits.clear();
        CustomColor white = CustomColor.fromHexString("FFFFFF");
        if (editorConflict) {
            ui.drawCenteredText("Entry already exists", panelX + panelW / 2f, panelY + 48, white, TEXT_SCALE);
            ui.drawCenteredText("Add entered amount or replace it?", panelX + panelW / 2f,
                    panelY + 64, TEXT_DIM, TEXT_SCALE);
            return;
        }

        ui.drawText(editorMode == EditorMode.ADD ? "Add entry" : "Edit entry", panelX + 8, panelY + 20,
                TEXT_DIM, TEXT_SCALE);
        int amountY = panelY + panelH - 44;
        ui.drawText("Amount", panelX + 8, amountY + 4, white, TEXT_SCALE);
        if (!editorError.isBlank()) {
            ui.drawText(trimToWidth(editorError, panelW - 16, TEXT_SCALE), panelX + 8,
                    amountY - 12, CustomColor.fromHexString("FF7777"), TEXT_SCALE);
        }

        int suggestionY = panelY + EDITOR_SUGGESTION_Y_OFFSET;
        int limit = editorSuggestionLimit();
        List<String> suggestions = ShoppingListEntryCatalog.suggestions(editorType, editorNameInput.getInput(), limit);
        selectedSuggestionIndex = suggestions.isEmpty() ? 0
                : Math.clamp(selectedSuggestionIndex, 0, suggestions.size() - 1);
        int suggestionWidth = panelW - 16;
        for (int index = 0; index < suggestions.size(); index++) {
            int y = suggestionY + index * EDITOR_SUGGESTION_HEIGHT;
            boolean hovered = mouseX >= panelX + 8 && mouseY >= y
                    && mouseX < panelX + 8 + suggestionWidth && mouseY < y + EDITOR_SUGGESTION_HEIGHT;
            if (hovered || suggestionSelectionActive && index == selectedSuggestionIndex) {
                ui.drawRect(panelX + 8, y, suggestionWidth, EDITOR_SUGGESTION_HEIGHT - 1,
                        hovered ? ROW_HOVER : ROW_BG);
            }
            ui.drawText(trimToWidth(suggestions.get(index), suggestionWidth - 6, TEXT_SCALE),
                    panelX + 11, y + 2, rowNameTextColor(editorType), TEXT_SCALE);
            suggestionHits.add(new SuggestionHit(suggestions.get(index), panelX + 8, y,
                    suggestionWidth, EDITOR_SUGGESTION_HEIGHT));
        }
        if (suggestions.isEmpty() && !editorNameInput.getInput().isBlank()) {
            ui.drawText("Use as custom " + (editorType == RequirementType.MATERIAL ? "material" : "ingredient"),
                    panelX + 10, suggestionY + 2, TEXT_DIM, TEXT_SCALE);
        }
    }

    static ControlLayout controlLayout(int panelX, int panelY, int panelWidth) {
        int gap = 4;
        int topButtonWidth = (panelWidth - 16 - gap * 2) / 3;
        int bottomButtonWidth = (panelWidth - 16 - gap) / 2;
        int firstRowY = panelY + 24;
        int secondRowY = firstRowY + BUTTON_HEIGHT + 2;
        return new ControlLayout(
                new ShoppingListMenuLauncherButton.Bounds(panelX + 8 + topButtonWidth + gap, firstRowY,
                        topButtonWidth, BUTTON_HEIGHT),
                new ShoppingListMenuLauncherButton.Bounds(panelX + 8, firstRowY, topButtonWidth, BUTTON_HEIGHT),
                new ShoppingListMenuLauncherButton.Bounds(panelX + 8 + (topButtonWidth + gap) * 2, firstRowY,
                        panelWidth - 16 - topButtonWidth * 2 - gap * 2, BUTTON_HEIGHT),
                new ShoppingListMenuLauncherButton.Bounds(panelX + 8, secondRowY, bottomButtonWidth, BUTTON_HEIGHT),
                new ShoppingListMenuLauncherButton.Bounds(panelX + 8 + bottomButtonWidth + gap, secondRowY,
                        panelWidth - 16 - bottomButtonWidth - gap, BUTTON_HEIGHT));
    }

    private void drawRows(int mouseX, int mouseY, float delta, boolean screenStableForCounts) {
        rowHits.clear();
        List<RenderedRow> rows = rowsWithHaveCounts(screenStableForCounts);
        int listX = panelX + 7;
        int listY = listTopY();
        int listW = panelW - 14;
        int maxRows = maxVisibleRows();
        if (rows.isEmpty()) {
            scrollOffset = 0;
            targetScrollOffset = 0;
            scrollbarLayout = ScrollbarLayout.hidden();
            ui.drawText("Cart is empty.", listX + 2, listY + 4, TEXT_DIM, TEXT_SCALE);
            return;
        }

        int maxScroll = maxScroll(rows.size(), maxRows);
        targetScrollOffset = Math.clamp(targetScrollOffset, 0f, (float) maxScroll);
        scrollOffset = smoothScroll(scrollOffset, targetScrollOffset, delta);
        scrollOffset = Math.clamp(scrollOffset, 0f, (float) maxScroll);
        scrollbarLayout = scrollbarLayout(panelX, panelY, panelW, panelH, rows.size(), maxRows, scrollOffset, listY);
        int rowContentWidth = scrollbarLayout.visible()
                ? listW - SCROLLBAR_WIDTH - SCROLLBAR_GAP
                : listW;
        int listBottom = panelY + panelH - 5;
        int firstRow = Math.clamp((int) Math.floor(scrollOffset), 0, Math.max(0, rows.size() - 1));
        int rowShift = Math.round((scrollOffset - firstRow) * ROW_HEIGHT);
        drawContext.enableScissor(listX, listY - 3, listX + rowContentWidth, listBottom);
        for (int rowIndex = firstRow; rowIndex < rows.size(); rowIndex++) {
            int rowY = listY + (rowIndex - firstRow) * ROW_HEIGHT - rowShift;
            if (rowY >= listBottom) break;
            if (rowY + ROW_HEIGHT <= listY) continue;

            RenderedRow renderedRow = rows.get(rowIndex);
            ShoppingListFormatter.Row row = renderedRow.row();
            int hitY = Math.max(rowY, listY);
            int hitBottom = Math.min(rowY + ROW_HEIGHT - 1, listBottom);
            if (hitBottom > hitY) {
                rowHits.add(new RowHit(row, renderedRow.detail(), renderedRow.entry(), renderedRow.baseAmount(),
                        listX, hitY, rowContentWidth, hitBottom - hitY));
            }

            boolean hoveredRow = mouseX >= listX && mouseY >= rowY
                    && mouseX < listX + rowContentWidth
                    && mouseY < rowY + ROW_HEIGHT - 1;
            if (hoveredRow) {
                ui.drawRect(listX, rowY, rowContentWidth, ROW_HEIGHT - 1, ROW_HOVER);
            } else if (rowIndex % 2 == 0) {
                ui.drawRect(listX, rowY, rowContentWidth, ROW_HEIGHT - 1, ROW_BG);
            }

            String haveNeed = row.haveCount() + "/" + row.needCount();
            ui.drawText(row.typeLabel(), listX + 3, rowY + 2,
                    rowTypeTextColor(row.type()), TEXT_SCALE);
            ui.drawText(trimToWidth(row.displayNameWithTier(), rowContentWidth - 108, TEXT_SCALE),
                    listX + 37, rowY + 2, rowNameTextColor(row.type()), TEXT_SCALE);
            ui.drawText(haveNeed, listX + rowContentWidth - 54, rowY + 2, TEXT_DIM, TEXT_SCALE);
            int editX = listX + rowContentWidth - EDIT_BUTTON_SIZE - 2;
            ui.drawVanillaPanelButton(editX, rowY, EDIT_BUTTON_SIZE, EDIT_BUTTON_SIZE,
                    BUTTON_NINE_SLICE_SCALE, BUTTON_CORNER_SIZE, hoveredRow && mouseX >= editX);
            ui.drawCenteredText("✎", editX + EDIT_BUTTON_SIZE / 2f, rowY + EDIT_BUTTON_SIZE / 2f,
                    CustomColor.fromHexString("FFFFFF"), 0.75f);
        }
        drawContext.disableScissor();

        if (maxScroll > 0) {
            drawScrollbar(scrollbarLayout);
        }
    }

    private float smoothScroll(float current, float target, float delta) {
        float diff = target - current;
        if (Math.abs(diff) < SCROLL_SNAP || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) {
            return target;
        }
        return current + diff * Math.min(1f, SCROLL_SPEED * Math.max(0f, delta));
    }

    private void drawScrollbar(ScrollbarLayout layout) {
        if (!layout.visible()) {
            return;
        }
        drawContext.fill(layout.track().x(), layout.track().y(),
                layout.track().x() + layout.track().width(),
                layout.track().y() + layout.track().height(),
                scrollbarTrackColor());
        drawContext.fill(layout.thumb().x(), layout.thumb().y(),
                layout.thumb().x() + layout.thumb().width(),
                layout.thumb().y() + layout.thumb().height(),
                scrollbarThumbColor());
    }

    private List<RenderedRow> rowsWithHaveCounts(boolean screenStableForCounts) {
        var cart = service.cart();
        var entries = cart.entries();
        if (entries.isEmpty()) {
            return List.of();
        }

        boolean allowSnapshotRefresh = ShoppingListMenuRenderPolicy.allowsHaveCountRefresh(screenContext)
                && (screenStableForCounts
                || screenContext == ShoppingListScreenContext.UNSUPPORTED
                || screenContext == ShoppingListScreenContext.INVENTORY);
        ShoppingListHaveCountService.SnapshotResult snapshotResult = haveCountService.cachedSnapshot(allowSnapshotRefresh);
        ShoppingListHaveCountService.SourceSnapshot snapshot = snapshotResult.snapshot();
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
            cachedRenderedRows = entries.entrySet().stream()
                    .map(entry -> {
                        var haveCount = haveCountService.count(entry.getKey(), cachedHaveCountSnapshot);
                        var row = ShoppingListFormatter.Row.from(entry.getKey(),
                                adjustedRequired(entry.getKey(), entry.getValue()), haveCount.total());
                        return new RenderedRow(row, ShoppingListRowDetail.from(row, haveCount),
                                entry.getKey(), entry.getValue());
                    })
                    .sorted((first, second) -> ShoppingListFormatter.compareRows(first.row(), second.row()))
                    .toList();
        }
        return cachedRenderedRows;
    }

    private void handleRowClick(ShoppingListFormatter.Row row, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            ShoppingListTradeMarketSearchService.copied(row.tradeMarketQuery());
            return;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            switch (ShoppingListMenuRenderPolicy.primaryRowAction(screenContext)) {
                case BANK_OVERLAY_SEARCH -> searchBankOverlay(row);
                case TRADE_MARKET_SEARCH -> {
                    if (ShoppingListTradeMarketSearchService.isWorkflowActive()) {
                        ShoppingListTradeMarketSearchService.activeWorkflowResult();
                    } else {
                        ShoppingListTradeMarketSearchService.searchOrCopy(row.tradeMarketQuery());
                    }
                }
                case COPY_ONLY -> ShoppingListTradeMarketSearchService.copied(row.tradeMarketQuery());
                case NONE -> {
                }
            }
        }
    }

    private void searchBankOverlay(ShoppingListFormatter.Row row) {
        String itemName = ShoppingListTextCleaner.clean(row.displayName());
        if (!itemName.isEmpty()) {
            String searchInput = "@ " + itemName;
            if (row.type() == RequirementType.MATERIAL && row.materialTier() > 0) {
                searchInput += " materialtier:" + row.materialTier();
            } else if (row.type() == RequirementType.INGREDIENT
                    && CraftingDataService.getInstance().getPowder(row.displayName()) == null) {
                var ingredient = CraftingDataService.getInstance().getIngredient(row.displayName());
                if (ingredient != null) {
                    searchInput += " ingredienttier:" + ingredient.tier();
                }
            }
            BankOverlay2.setSearchInput(searchInput);
        }
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

    private static TextInputWidget editorTextInput(String placeholder) {
        TextInputWidget input = new TextInputWidget(0, 0, 0, 0, 4, 4, TEXT_SCALE);
        input.setPlaceholder(placeholder);
        input.setMaxLength(120);
        input.setBackgroundColor(ROW_BG);
        input.setFocusedColor(ROW_HOVER);
        return input;
    }

    private static TextInputWidget editorAmountInput() {
        TextInputWidget input = editorTextInput("Amount");
        input.setMaxLength(10);
        input.setCharacterFilter(Character::isDigit);
        return input;
    }

    private boolean isEditorOpen() {
        return editorMode != EditorMode.CLOSED;
    }

    public static boolean isEditorTextInputFocused() {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        return currentScreen != null
                && currentScreen == activeScreen
                && activeScreenExtension != null
                && activeScreenExtension.isEditorOpen()
                && (activeScreenExtension.editorNameInput.isFocused()
                || activeScreenExtension.editorAmountInput.isFocused());
    }

    private void openAddEditor() {
        editorMode = EditorMode.ADD;
        editingOriginal = null;
        editorType = RequirementType.INGREDIENT;
        editorTier = 1;
        editorNameInput.clearInput();
        editorAmountInput.setInputAndMoveCursorToEnd("1");
        resetEditorState();
    }

    private void openEditEditor(ShoppingEntry entry, int amount) {
        if (entry == null) return;
        editorMode = EditorMode.EDIT;
        editingOriginal = entry;
        editorType = entry.type();
        editorTier = entry.type() == RequirementType.MATERIAL ? entry.materialTier() : 1;
        editorNameInput.setInputAndMoveCursorToEnd(entry.displayName());
        editorAmountInput.setInputAndMoveCursorToEnd(Integer.toString(Math.max(1, amount)));
        resetEditorState();
        setFocusedWidget(editorNameInput);
    }

    private void closeEditor() {
        editorMode = EditorMode.CLOSED;
        editingOriginal = null;
        resetEditorState();
        suggestionHits.clear();
        setEditorWidgetsVisible(false);
        clearUiFocus();
    }

    private void resetEditorState() {
        editorError = "";
        editorConflict = false;
        deleteArmed = false;
        selectedSuggestionIndex = 0;
        suggestionSelectionActive = false;
    }

    private void editorFieldsChanged() {
        editorError = "";
        editorConflict = false;
        deleteArmed = false;
        selectedSuggestionIndex = 0;
        suggestionSelectionActive = false;
    }

    private void toggleEditorType() {
        editorType = editorType == RequirementType.INGREDIENT ? RequirementType.MATERIAL : RequirementType.INGREDIENT;
        editorTier = Math.clamp(editorTier, 1, 3);
        editorFieldsChanged();
    }

    private void cycleEditorTier() {
        editorTier = editorTier == 3 ? 1 : editorTier + 1;
        editorFieldsChanged();
    }

    private String editorTypeLabel() {
        return editorType == RequirementType.MATERIAL ? "Type: Material" : "Type: Ingredient";
    }

    private String editorTierLabel() {
        return "Tier " + editorTier;
    }

    private String editorSaveLabel() {
        return editorMode == EditorMode.ADD ? "Add" : "Save";
    }

    private String editorDeleteLabel() {
        return deleteArmed ? "Delete?" : "Delete";
    }

    private List<String> editorSuggestions() {
        return ShoppingListEntryCatalog.suggestions(editorType, editorNameInput.getInput(), editorSuggestionLimit());
    }

    private int editorSuggestionLimit() {
        int suggestionY = panelY + EDITOR_SUGGESTION_Y_OFFSET;
        int amountY = panelY + panelH - 44;
        int bottomGap = editorError.isBlank() ? 2 : 14;
        return Math.max(0, amountY - suggestionY - bottomGap) / EDITOR_SUGGESTION_HEIGHT;
    }

    private boolean handleSuggestionClick(double x, double y) {
        for (SuggestionHit hit : suggestionHits) {
            if (hit.contains(x, y)) {
                editorNameInput.setInputAndMoveCursorToEnd(hit.name());
                selectedSuggestionIndex = 0;
                suggestionSelectionActive = false;
                setFocusedWidget(editorNameInput);
                return true;
            }
        }
        return false;
    }

    private void saveEditor() {
        ShoppingEntry entry = editorEntry();
        if (entry == null) return;
        int amount = editorAmount();
        if (amount <= 0) return;

        ShoppingCartService.MutationResult result = editorMode == EditorMode.ADD
                ? service.addManual(entry, amount, null)
                : service.edit(editingOriginal, entry, amount, null);
        handleEditorMutationResult(result);
    }

    private void resolveEditorConflict(ShoppingCartService.ExistingEntryPolicy policy) {
        ShoppingEntry entry = editorEntry();
        if (entry == null) {
            clearEditorConflict();
            return;
        }
        int amount = editorAmount();
        if (amount <= 0) {
            clearEditorConflict();
            return;
        }
        ShoppingCartService.MutationResult result = editorMode == EditorMode.ADD
                ? service.addManual(entry, amount, policy)
                : service.edit(editingOriginal, entry, amount, policy);
        handleEditorMutationResult(result);
    }

    private void clearEditorConflict() {
        editorConflict = false;
        editorError = "";
        deleteArmed = false;
        setFocusedWidget(editorNameInput);
    }

    private void cycleEditorFocus(boolean backwards, List<? extends Widget> controls) {
        if (controls.isEmpty()) return;
        int current = controls.indexOf(focusedWidget);
        int next = current < 0
                ? (backwards ? controls.size() - 1 : 0)
                : Math.floorMod(current + (backwards ? -1 : 1), controls.size());
        setFocusedWidget(controls.get(next));
    }

    private void handleEditorMutationResult(ShoppingCartService.MutationResult result) {
        if (result.status() == ShoppingCartService.MutationStatus.CONFLICT) {
            editorConflict = true;
            editorError = "";
            setFocusedWidget(editorConflictAddButton);
            return;
        }
        if (!result.success()) {
            editorConflict = false;
            editorError = result.error();
            setFocusedWidget(editorAmountInput);
            return;
        }
        notifySaveFailure();
        invalidateHaveCountCache();
        closeEditor();
    }

    private ShoppingEntry editorEntry() {
        String name = ShoppingListTextCleaner.clean(editorNameInput.getInput());
        if (name.isBlank()) {
            editorError = "Name is required.";
            return null;
        }
        try {
            return new ShoppingEntry(name, editorType,
                    editorType == RequirementType.MATERIAL ? editorTier : 0, "Manual");
        } catch (IllegalArgumentException ex) {
            editorError = ex.getMessage();
            return null;
        }
    }

    private int editorAmount() {
        String text = editorAmountInput.getInput().trim();
        try {
            int amount = Integer.parseInt(text);
            if (amount <= 0) throw new NumberFormatException();
            return amount;
        } catch (NumberFormatException ex) {
            editorError = "Amount must be a positive whole number.";
            return -1;
        }
    }

    private void deleteEditorEntry() {
        if (editorMode != EditorMode.EDIT || editingOriginal == null) return;
        if (!deleteArmed) {
            deleteArmed = true;
            editorError = "Click delete again to confirm.";
            return;
        }
        ShoppingCartService.MutationResult result = service.remove(editingOriginal);
        handleEditorMutationResult(result);
    }

    private void setEditorWidgetsVisible(boolean visible) {
        boolean fieldsVisible = visible && !editorConflict;
        editorNameInput.setVisible(fieldsVisible);
        editorAmountInput.setVisible(fieldsVisible);
        editorTypeButton.setVisible(fieldsVisible);
        editorTierButton.setVisible(fieldsVisible && editorType == RequirementType.MATERIAL);
        editorSaveButton.setVisible(fieldsVisible);
        editorCancelButton.setVisible(fieldsVisible);
        editorDeleteButton.setVisible(fieldsVisible && editorMode == EditorMode.EDIT);
        editorConflictAddButton.setVisible(visible && editorConflict);
        editorConflictReplaceButton.setVisible(visible && editorConflict);
        editorConflictBackButton.setVisible(visible && editorConflict);
    }

    private void importClipboard() {
        String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
        ShoppingCartService.ImportResult result = service.importUrl(clipboard);
        if (!result.success()) {
            WynnExtras.sendMessageToClient("§cShopping list import failed: " + result.error());
        } else {
            notifySaveFailure();
        }
        invalidateHaveCountCache();
    }

    private void clearCart() {
        service.clear();
        scrollOffset = 0;
        targetScrollOffset = 0;
        notifySaveFailure();
        invalidateHaveCountCache();
    }

    private void notifySaveFailure() {
        if (ShoppingListFeature.consumeSaveFailure()) {
            WynnExtras.sendMessageToClient("§cShopping list changed, but saving it failed.");
        }
    }

    private void toggleProfessionSpeed() {
        WynnExtrasConfig.INSTANCE.shoppingListProfessionSpeed = !WynnExtrasConfig.INSTANCE.shoppingListProfessionSpeed;
        WynnExtrasConfig.save();
        invalidateHaveCountCache();
    }

    private boolean changeOutputCount(int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (isShiftDown()) {
                WynnExtrasConfig.INSTANCE.shoppingListCraftMultiplier = 1;
                WynnExtrasConfig.save();
            } else {
                int nextOutputCount = ShoppingListRequirementCalculator.subtractOutputs(outputCount(), 1);
                WynnExtrasConfig.INSTANCE.shoppingListCraftMultiplier = nextOutputCount;
                WynnExtrasConfig.save();
            }
            invalidateHaveCountCache();
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        int increment = ShoppingListRequirementCalculator.outputClickIncrement(isShiftDown());
        int nextOutputCount = ShoppingListRequirementCalculator.addOutputs(outputCount(), increment);
        WynnExtrasConfig.INSTANCE.shoppingListCraftMultiplier = nextOutputCount;
        WynnExtrasConfig.save();
        invalidateHaveCountCache();
        return true;
    }

    private String speedButtonLabel() {
        return WynnExtrasConfig.INSTANCE.shoppingListProfessionSpeed ? "Speed: On" : "Speed: Off";
    }

    private String outputButtonLabel() {
        return outputButtonLabel(outputCount());
    }

    private String buyRemainingButtonLabel() {
        return purchaseContext == null ? "" : "Buy remaining: " + purchaseContext.remaining();
    }

    private String buyNeededButtonLabel() {
        return purchaseContext == null ? "" : "Buy all: " + purchaseContext.needed();
    }

    private void buyRemaining() {
        if (purchaseContext != null) {
            ShoppingListTradeMarketPurchaseService.submitAmount(purchaseContext.remaining());
        }
    }

    private void buyNeeded() {
        if (purchaseContext != null) {
            ShoppingListTradeMarketPurchaseService.submitAmount(purchaseContext.needed());
        }
    }

    static String outputButtonLabel(int outputCount) {
        return "Amount x" + ShoppingListRequirementCalculator.sanitizeOutputCount(outputCount);
    }

    private int adjustedRequired(ShoppingEntry entry, int baseRequired) {
        return ShoppingListRequirementCalculator.adjustedRequired(
                entry,
                baseRequired,
                outputCount(),
                professionSpeedEnabled());
    }

    private int outputCount() {
        return ShoppingListRequirementCalculator.sanitizeOutputCount(WynnExtrasConfig.INSTANCE.shoppingListCraftMultiplier);
    }

    private boolean professionSpeedEnabled() {
        return WynnExtrasConfig.INSTANCE.shoppingListProfessionSpeed;
    }

    private void invalidateHaveCountCache() {
        cachedHaveCountSnapshot = null;
        cachedHaveCountCart = null;
        cachedHaveCountCartSize = -1;
        cachedHaveCountCartSignature = 0;
        cachedOutputCount = -1;
        cachedProfessionSpeed = false;
        cachedRenderedRows = List.of();
        currentScreenTransitionKey = "";
        currentScreenTransitionChangedAtMs = Long.MIN_VALUE;
    }

    private void setButtonsVisible(boolean visible) {
        addButton.setVisible(visible);
        importButton.setVisible(visible);
        clearButton.setVisible(visible);
        speedButton.setVisible(visible);
        outputButton.setVisible(visible);
        buyRemainingButton.setVisible(visible && purchaseContext != null && purchaseContext.needsRemainingButton());
        buyNeededButton.setVisible(visible && purchaseContext != null);
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
                                      ShoppingListScreenContext context) {
        return (context == null ? ShoppingListScreenContext.UNSUPPORTED : context).name()
                + "|" + nullToEmpty(screenClass)
                + "|" + nullToEmpty(handlerClass)
                + "|" + syncId
                + "|" + nullToEmpty(title);
    }

    private static String screenTransitionKey(HandledScreen<?> screen, ShoppingListScreenContext context) {
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
        return Math.max(1, (panelY + panelH - 7 - listY) / ROW_HEIGHT);
    }

    private int listTopY() {
        return listTopY(panelY) + purchaseSectionOffset();
    }

    private int listHeaderY() {
        return listHeaderY(panelY) + purchaseSectionOffset();
    }

    private int listDividerY() {
        return listDividerY(panelY) + purchaseSectionOffset();
    }

    private int purchaseLabelY() {
        return panelY + 63;
    }

    private int purchaseSectionOffset() {
        return purchaseContext == null ? 0 : PURCHASE_SECTION_HEIGHT;
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
        return scrollbarLayout(panelX, panelY, panelW, panelH, rowCount, visibleRows, (float) scrollOffset);
    }

    static ScrollbarLayout scrollbarLayout(
            int panelX,
            int panelY,
            int panelW,
            int panelH,
            int rowCount,
            int visibleRows,
            float scrollOffset) {
        return scrollbarLayout(panelX, panelY, panelW, panelH, rowCount, visibleRows, scrollOffset, listTopY(panelY));
    }

    private static ScrollbarLayout scrollbarLayout(
            int panelX,
            int panelY,
            int panelW,
            int panelH,
            int rowCount,
            int visibleRows,
            float scrollOffset,
            int listTopY) {
        int maxScroll = Math.max(0, rowCount - visibleRows);
        if (rowCount <= 0 || visibleRows <= 0 || maxScroll <= 0) {
            return ScrollbarLayout.hidden();
        }

        int trackX = panelX + panelW - 7 - SCROLLBAR_WIDTH;
        int trackY = listTopY;
        int trackHeight = Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, Math.min(visibleRows, rowCount) * ROW_HEIGHT - 1);
        int thumbHeight = Math.max(SCROLLBAR_MIN_THUMB_HEIGHT,
                trackHeight * Math.min(visibleRows, rowCount) / rowCount);
        thumbHeight = Math.min(thumbHeight, trackHeight);
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        float safeOffset = Math.clamp(scrollOffset, 0f, (float) maxScroll);
        int thumbY = trackY + (thumbTravel == 0 ? 0 : Math.round((float) thumbTravel * safeOffset / maxScroll));
        return new ScrollbarLayout(
                true,
                new ShoppingListMenuLauncherButton.Bounds(trackX, trackY, SCROLLBAR_WIDTH, trackHeight),
                new ShoppingListMenuLauncherButton.Bounds(trackX, thumbY, SCROLLBAR_WIDTH, thumbHeight),
                maxScroll);
    }

    static int scrollbarScrollOffset(ScrollbarLayout layout, int thumbY) {
        return Math.round(smoothScrollbarScrollOffset(layout, thumbY));
    }

    static float smoothScrollbarScrollOffset(ScrollbarLayout layout, double thumbY) {
        if (layout == null || !layout.visible() || layout.maxScroll() <= 0) {
            return 0;
        }
        int thumbTravel = Math.max(0, layout.track().height() - layout.thumb().height());
        if (thumbTravel == 0) {
            return 0;
        }
        double clampedThumbY = Math.clamp(thumbY, layout.track().y(), layout.track().y() + thumbTravel);
        float ratio = (float) ((clampedThumbY - layout.track().y()) / thumbTravel);
        return Math.clamp(ratio * layout.maxScroll(), 0f, (float) layout.maxScroll());
    }

    static CustomColor rowNameTextColor(RequirementType type) {
        return type == RequirementType.MATERIAL ? MATERIAL_TEXT : INGREDIENT_TEXT;
    }

    static CustomColor rowTypeTextColor(RequirementType type) {
        return type == RequirementType.MATERIAL ? MATERIAL_DIM : INGREDIENT_DIM;
    }

    static int panelBorderColor() {
        return UIUtils.getVanillaPanelBorderColor().asInt();
    }

    static int panelAccentColor() {
        return UIUtils.getVanillaPanelButtonFillColor().asInt();
    }

    static int scrollbarTrackColor() {
        return UIUtils.getVanillaPanelButtonOutlineColor().asInt();
    }

    static int scrollbarThumbColor() {
        return UIUtils.getVanillaPanelButtonFillColor().withAlpha(0.5f).asInt();
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
            ShoppingListMenuLauncherButton.Bounds addButton,
            ShoppingListMenuLauncherButton.Bounds importButton,
            ShoppingListMenuLauncherButton.Bounds clearButton,
            ShoppingListMenuLauncherButton.Bounds speedButton,
            ShoppingListMenuLauncherButton.Bounds outputButton) {
        public List<ShoppingListMenuLauncherButton.Bounds> buttons() {
            return List.of(addButton, importButton, clearButton, speedButton, outputButton);
        }
    }

    public record ScrollbarLayout(
            boolean visible,
            ShoppingListMenuLauncherButton.Bounds track,
            ShoppingListMenuLauncherButton.Bounds thumb,
            int maxScroll) {
        static ScrollbarLayout hidden() {
            var hidden = ShoppingListMenuLauncherButton.Bounds.hidden();
            return new ScrollbarLayout(false, hidden, hidden, 0);
        }
    }

    private record ResizeEdges(boolean left, boolean right, boolean top, boolean bottom) {
        private static final ResizeEdges NONE = new ResizeEdges(false, false, false, false);

        private boolean active() {
            return left || right || top || bottom;
        }
    }

    private record RenderedRow(ShoppingListFormatter.Row row, ShoppingListRowDetail detail, ShoppingEntry entry,
                               int baseAmount) {}

    private record RowHit(ShoppingListFormatter.Row row, ShoppingListRowDetail detail, ShoppingEntry entry,
                          int baseAmount, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        }

        boolean containsEdit(double mouseX, double mouseY) {
            int editX = x + width - EDIT_BUTTON_SIZE - 2;
            return mouseX >= editX && mouseY >= y && mouseX < editX + EDIT_BUTTON_SIZE
                    && mouseY < y + EDIT_BUTTON_SIZE;
        }
    }

    private record SuggestionHit(String name, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        }
    }

    private enum EditorMode {
        CLOSED,
        ADD,
        EDIT
    }

    private interface TooltipWidget {
        String tooltipText();
    }

    private static void playButtonClickSound() {
        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
            ui.drawVanillaPanelButton(x, y, width, height, BUTTON_NINE_SLICE_SCALE, BUTTON_CORNER_SIZE, hovered);
            String label = labelSupplier == null ? "" : labelSupplier.get();
            ui.drawCenteredText(label, x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 0.75f);
        }

        @Override
        protected boolean onClick(int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            playButtonClickSound();
            action.run();
            return true;
        }

        @Override
        protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
            if (!focused || (keyCode != GLFW.GLFW_KEY_ENTER && keyCode != GLFW.GLFW_KEY_KP_ENTER
                    && keyCode != GLFW.GLFW_KEY_SPACE)) return false;
            playButtonClickSound();
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
            ui.drawVanillaPanelButton(x, y, width, height, BUTTON_NINE_SLICE_SCALE, BUTTON_CORNER_SIZE, hovered);
            ui.drawCenteredText(outputButtonLabel(), x + width / 2f, y + height / 2f,
                    CustomColor.fromHexString("FFFFFF"), 0.75f);
        }

        @Override
        protected boolean onClick(int button) {
            boolean changed = changeOutputCount(button);
            if (changed) playButtonClickSound();
            return changed;
        }

        @Override
        public String tooltipText() {
            return AMOUNT_TOOLTIP;
        }
    }

    private final class HeaderCloseButton extends Widget implements TooltipWidget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawVanillaPanelButton(x, y, width, height, BUTTON_NINE_SLICE_SCALE, BUTTON_CORNER_SIZE, hovered);
            ui.drawCenteredText("X", x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 0.75f);
        }

        @Override
        protected boolean onClick(int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            playButtonClickSound();
            closeEditor();
            ShoppingListMenuExtension.close();
            return true;
        }

        @Override
        public String tooltipText() {
            int key = WynnExtrasConfig.INSTANCE.shoppingListToggleKey;
            String keyName = key == GLFW.GLFW_KEY_UNKNOWN
                    ? "NOT BOUND"
                    : InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString();
            String tooltip = CLOSE_TOOLTIP + "\n" + CLOSE_KEYBIND_TOOLTIP.formatted(keyName);
            return key == GLFW.GLFW_KEY_UNKNOWN
                    ? tooltip + "\n" + CLOSE_KEYBIND_CONFIG_TOOLTIP
                    : tooltip;
        }
    }
}
