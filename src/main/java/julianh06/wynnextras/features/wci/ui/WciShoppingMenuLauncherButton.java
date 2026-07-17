package julianh06.wynnextras.features.wci.ui;

import com.wynntils.models.containers.Container;
import com.wynntils.models.containers.containers.personal.AccountBankContainer;
import com.wynntils.models.containers.containers.personal.BookshelfContainer;
import com.wynntils.models.containers.containers.personal.CharacterBankContainer;
import com.wynntils.models.containers.containers.personal.MiscBucketContainer;
import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.utils.HandledScreenAccess;
import julianh06.wynnextras.utils.UI.UIUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class WciShoppingMenuLauncherButton {
    private static final int BUTTON_WIDTH = 54;
    private static final int BUTTON_HEIGHT = 16;
    private static final int GAP = 6;
    private static final int SCREEN_MARGIN = 4;
    private static final int BANK_OVERLAY_DEFAULT_X = 681;
    private static final int BANK_OVERLAY_DEFAULT_Y = 44;
    private static final int BANK_VANILLA_DEFAULT_X = 571;
    private static final int BANK_VANILLA_DEFAULT_Y = 115;
    private static final CustomColor TEXT = CustomColor.fromHexString("FFFFFF");
    private static final int ACTIVE_ACCENT = 0xFF4BA3C7;

    private final UIUtils ui = new UIUtils(null, 1, 0, 0);
    private Bounds bounds = Bounds.hidden();
    private boolean dragging = false;
    private boolean dragMoved = false;
    private int dragButton = -1;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private WciPlacementContext dragContext = WciPlacementContext.OTHER;

    public void render(DrawContext context, HandledScreen<?> screen, int mouseX, int mouseY, float delta,
                       boolean customBankOverlayActive) {
        WciScreenContext screenContext = WciScreenContext.detect(screen, customBankOverlayActive);
        if (!shouldRender(screenContext)) {
            bounds = Bounds.hidden();
            return;
        }

        WciPlacementContext placementContext = placementContext(screenContext, customBankOverlayActive);
        bounds = boundsFor(screen, placementContext);
        ui.updateContext(context, 1, 0, 0);

        boolean active = WciShoppingMenuExtension.isVisible();
        boolean hovered = bounds.contains(mouseX, mouseY);
        ui.drawButton(bounds.x(), bounds.y(), bounds.width(), bounds.height(), hovered || active);
        if (active || dragging) {
            context.fill(bounds.x(), bounds.y(), bounds.x() + 2, bounds.y() + bounds.height(), ACTIVE_ACCENT);
        }
        ui.drawCenteredText(label(active), bounds.x() + bounds.width() / 2f, bounds.y() + bounds.height() / 2f, TEXT, 0.75f);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, HandledScreen<?> screen,
                                boolean customBankOverlayActive) {
        WciScreenContext screenContext = WciScreenContext.detect(screen, customBankOverlayActive);
        if (!shouldRender(screenContext)) {
            return false;
        }

        WciPlacementContext placementContext = placementContext(screenContext, customBankOverlayActive);
        Bounds clickBounds = bounds.visible() ? bounds : boundsFor(screen, placementContext);
        if (!clickBounds.contains(mouseX, mouseY)) {
            return false;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT || button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            startDrag(clickBounds, mouseX, mouseY, button, placementContext);
            return true;
        }

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        if (WciShoppingMenuExtension.isVisible()) {
            WciShoppingMenuExtension.close();
        } else {
            WciShoppingMenuExtension.show();
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!dragging || button != dragButton) {
            return false;
        }
        Bounds movedBounds = clampSavedBounds((int) mouseX - dragOffsetX, (int) mouseY - dragOffsetY,
                screenWidth(), screenHeight());
        bounds = movedBounds;
        setPosition(WynnExtrasConfig.INSTANCE, dragContext, movedBounds.x(), movedBounds.y());
        dragMoved = true;
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, WciScreenContext screenContext) {
        if (!shouldRender(screenContext)) {
            cancelDrag();
            return false;
        }
        return mouseDragged(mouseX, mouseY, button);
    }

    public boolean mouseReleased(int button) {
        if (!dragging || button != dragButton) {
            return false;
        }
        dragging = false;
        dragButton = -1;
        dragContext = WciPlacementContext.OTHER;
        if (dragMoved) {
            WynnExtrasConfig.save();
        }
        dragMoved = false;
        return true;
    }

    public boolean mouseReleased(int button, WciScreenContext screenContext) {
        if (!shouldRender(screenContext)) {
            cancelDrag();
            return false;
        }
        return mouseReleased(button);
    }

    public void cancelDrag() {
        dragging = false;
        dragMoved = false;
        dragButton = -1;
        dragContext = WciPlacementContext.OTHER;
    }

    public boolean shouldRender(HandledScreen<?> screen, boolean customBankOverlayActive) {
        return shouldRender(WciScreenContext.detect(screen, customBankOverlayActive));
    }

    public static boolean shouldRender(boolean tradeMarketScreen, boolean bankLikeScreen, boolean customBankOverlayActive) {
        return shouldRender(WciScreenContext.fromSignals("", tradeMarketScreen, bankLikeScreen, customBankOverlayActive, false));
    }

    public static boolean shouldRender(WciScreenContext screenContext) {
        return WciMenuRenderPolicy.shouldRenderLauncher(screenContext);
    }

    public static boolean isBankLikeContainer(Container container) {
        return container instanceof AccountBankContainer
                || container instanceof CharacterBankContainer
                || container instanceof BookshelfContainer
                || container instanceof MiscBucketContainer;
    }

    public static String label(boolean active) {
        return "WCI";
    }

    public static Bounds computeBounds(int screenX, int screenY, int backgroundWidth, int screenWidth, int screenHeight) {
        return computeBounds(screenX, screenY, backgroundWidth, 166, screenWidth, screenHeight, List.of());
    }

    public static Bounds computeBounds(int screenX, int screenY, int backgroundWidth, int backgroundHeight,
                                       int screenWidth, int screenHeight, List<Bounds> forbiddenAreas) {
        return computeBounds(WciPlacementContext.OTHER, screenX, screenY, backgroundWidth, backgroundHeight,
                screenWidth, screenHeight, forbiddenAreas);
    }

    public static Bounds computeBounds(WciPlacementContext placementContext, int screenX, int screenY,
                                       int backgroundWidth, int backgroundHeight, int screenWidth, int screenHeight,
                                       List<Bounds> forbiddenAreas) {
        return computeBounds(placementContext, screenX, screenY, backgroundWidth, backgroundHeight,
                screenWidth, screenHeight, forbiddenAreas, null);
    }

    public static Bounds computeBounds(WciPlacementContext placementContext, int screenX, int screenY,
                                       int backgroundWidth, int backgroundHeight, int screenWidth, int screenHeight,
                                       List<Bounds> forbiddenAreas, Bounds bankOverlayPreferredBounds) {
        List<Bounds> candidates = candidatesFor(placementContext, screenX, screenY, backgroundWidth, screenWidth,
                screenHeight, bankOverlayPreferredBounds);

        for (Bounds candidate : candidates) {
            if (!fitsInsideScreen(candidate, screenWidth, screenHeight)) {
                continue;
            }
            if (!overlapsAny(candidate, forbiddenAreas)) {
                return candidate;
            }
        }

        int preferredX = screenX + backgroundWidth + GAP;
        int fallbackLeftX = screenX - BUTTON_WIDTH - GAP;
        int x;
        if (preferredX + BUTTON_WIDTH <= screenWidth - SCREEN_MARGIN) {
            x = preferredX;
        } else if (fallbackLeftX >= SCREEN_MARGIN) {
            x = fallbackLeftX;
        } else {
            x = clamp(screenWidth - BUTTON_WIDTH - SCREEN_MARGIN, SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenWidth - BUTTON_WIDTH));
        }

        int y = clamp(screenY + 4, SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenHeight - BUTTON_HEIGHT - SCREEN_MARGIN));
        return clampBounds(new Bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT), screenWidth, screenHeight);
    }

    private static List<Bounds> candidatesFor(WciPlacementContext placementContext, int screenX, int screenY,
                                              int backgroundWidth, int screenWidth, int screenHeight,
                                              Bounds bankOverlayPreferredBounds) {
        return switch (placementContext) {
            case TRADE_MARKET -> List.of(
                    new Bounds(screenX + backgroundWidth + GAP, screenY + 4, BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(screenX - BUTTON_WIDTH - GAP, screenY + 4, BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(screenWidth - BUTTON_WIDTH - SCREEN_MARGIN, SCREEN_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(SCREEN_MARGIN, SCREEN_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(screenWidth - BUTTON_WIDTH - SCREEN_MARGIN, screenHeight - BUTTON_HEIGHT - SCREEN_MARGIN,
                            BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(SCREEN_MARGIN, screenHeight - BUTTON_HEIGHT - SCREEN_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT)
            );
            case BANK_OVERLAY -> bankOverlayCandidates(screenX, screenY, backgroundWidth, screenWidth, screenHeight,
                    bankOverlayPreferredBounds);
            case BANK_VANILLA -> List.of(
                    new Bounds(BANK_VANILLA_DEFAULT_X, BANK_VANILLA_DEFAULT_Y, BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(screenX + backgroundWidth + GAP, screenY + 4, BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(screenX - BUTTON_WIDTH - GAP, screenY + 4, BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(screenWidth - BUTTON_WIDTH - SCREEN_MARGIN, SCREEN_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(SCREEN_MARGIN, SCREEN_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(screenWidth - BUTTON_WIDTH - SCREEN_MARGIN, screenHeight - BUTTON_HEIGHT - SCREEN_MARGIN,
                            BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(SCREEN_MARGIN, screenHeight - BUTTON_HEIGHT - SCREEN_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT)
            );
            case OTHER -> List.of(
                    new Bounds(screenX + backgroundWidth + GAP, screenY + 4, BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(screenX - BUTTON_WIDTH - GAP, screenY + 4, BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(screenWidth - BUTTON_WIDTH - SCREEN_MARGIN, SCREEN_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(SCREEN_MARGIN, SCREEN_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(screenX + backgroundWidth + GAP, screenY - BUTTON_HEIGHT - GAP, BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(screenX - BUTTON_WIDTH - GAP, screenY - BUTTON_HEIGHT - GAP, BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(screenWidth - BUTTON_WIDTH - SCREEN_MARGIN, screenHeight - BUTTON_HEIGHT - SCREEN_MARGIN,
                            BUTTON_WIDTH, BUTTON_HEIGHT),
                    new Bounds(SCREEN_MARGIN, screenHeight - BUTTON_HEIGHT - SCREEN_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT)
            );
        };
    }

    private static List<Bounds> bankOverlayCandidates(int screenX, int screenY, int backgroundWidth,
                                                      int screenWidth, int screenHeight,
                                                      Bounds bankOverlayPreferredBounds) {
        List<Bounds> candidates = new ArrayList<>();
        if (bankOverlayPreferredBounds != null && bankOverlayPreferredBounds.visible()) {
            candidates.add(bankOverlayPreferredBounds);
        }
        candidates.add(new Bounds(screenWidth - BUTTON_WIDTH - SCREEN_MARGIN, SCREEN_MARGIN,
                BUTTON_WIDTH, BUTTON_HEIGHT));
        candidates.add(new Bounds(screenX - BUTTON_WIDTH - GAP, screenY + 4, BUTTON_WIDTH, BUTTON_HEIGHT));
        candidates.add(new Bounds(screenX + backgroundWidth + GAP, screenY + 4, BUTTON_WIDTH, BUTTON_HEIGHT));
        candidates.add(new Bounds(SCREEN_MARGIN, SCREEN_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT));
        candidates.add(new Bounds(screenWidth - BUTTON_WIDTH - SCREEN_MARGIN,
                screenHeight - BUTTON_HEIGHT - SCREEN_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT));
        candidates.add(new Bounds(SCREEN_MARGIN, screenHeight - BUTTON_HEIGHT - SCREEN_MARGIN,
                BUTTON_WIDTH, BUTTON_HEIGHT));
        return List.copyOf(candidates);
    }

    public static Bounds clampSavedBounds(int x, int y, int screenWidth, int screenHeight) {
        return clampBounds(new Bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT), screenWidth, screenHeight);
    }

    public static void resetPosition() {
        resetPosition(WynnExtrasConfig.INSTANCE);
        WynnExtrasConfig.save();
    }

    public static PositionState resetPositionState(PositionState ignored) {
        return new PositionState(false, -1, -1);
    }

    static void resetPosition(WynnExtrasConfig config) {
        ContextPositionStates reset = resetPositionStates(positionStates(config));
        applyPositionStates(config, reset);
    }

    public static ContextPositionStates resetPositionStates(ContextPositionStates ignored) {
        PositionState reset = resetPositionState(null);
        return new ContextPositionStates(reset, reset, reset, reset, reset);
    }

    public static PositionState selectPositionState(ContextPositionStates states, WciPlacementContext placementContext,
                                                    boolean legacyFallback) {
        PositionState contextState = switch (placementContext) {
            case TRADE_MARKET -> states.tradeMarket();
            case BANK_OVERLAY -> states.bankOverlay();
            case BANK_VANILLA -> states.bankVanilla();
            case OTHER -> states.legacy();
        };
        if (contextState.customPosition()) {
            return contextState;
        }
        if (isBankContext(placementContext)) {
            if (states.bank().customPosition()) {
                return states.bank();
            }
            PositionState siblingBankState = siblingBankState(states, placementContext);
            if (siblingBankState.customPosition()) {
                return siblingBankState;
            }
        }
        if (placementContext != WciPlacementContext.OTHER
                && legacyFallback
                && states.legacy().customPosition()) {
            return states.legacy();
        }
        return contextState;
    }

    private static boolean isBankContext(WciPlacementContext placementContext) {
        return placementContext == WciPlacementContext.BANK_OVERLAY
                || placementContext == WciPlacementContext.BANK_VANILLA;
    }

    private static PositionState siblingBankState(ContextPositionStates states, WciPlacementContext placementContext) {
        return placementContext == WciPlacementContext.BANK_OVERLAY
                ? states.bankVanilla()
                : states.bankOverlay();
    }

    private static ContextPositionStates positionStates(WynnExtrasConfig config) {
        return new ContextPositionStates(
                new PositionState(config.wciLauncherButtonCustomPosition, config.wciLauncherButtonX, config.wciLauncherButtonY),
                new PositionState(config.wciLauncherButtonTradeCustomPosition, config.wciLauncherButtonTradeX,
                        config.wciLauncherButtonTradeY),
                new PositionState(config.wciLauncherButtonBankCustomPosition, config.wciLauncherButtonBankX,
                        config.wciLauncherButtonBankY),
                new PositionState(config.wciLauncherButtonBankOverlayCustomPosition, config.wciLauncherButtonBankOverlayX,
                        config.wciLauncherButtonBankOverlayY),
                new PositionState(config.wciLauncherButtonBankVanillaCustomPosition, config.wciLauncherButtonBankVanillaX,
                        config.wciLauncherButtonBankVanillaY));
    }

    private static void applyPositionStates(WynnExtrasConfig config, ContextPositionStates states) {
        PositionState legacy = states.legacy();
        PositionState trade = states.tradeMarket();
        PositionState bank = states.bank();
        PositionState bankOverlay = states.bankOverlay();
        PositionState bankVanilla = states.bankVanilla();
        config.wciLauncherButtonCustomPosition = legacy.customPosition();
        config.wciLauncherButtonX = legacy.x();
        config.wciLauncherButtonY = legacy.y();
        config.wciLauncherButtonTradeCustomPosition = trade.customPosition();
        config.wciLauncherButtonTradeX = trade.x();
        config.wciLauncherButtonTradeY = trade.y();
        config.wciLauncherButtonBankCustomPosition = bank.customPosition();
        config.wciLauncherButtonBankX = bank.x();
        config.wciLauncherButtonBankY = bank.y();
        config.wciLauncherButtonBankOverlayCustomPosition = bankOverlay.customPosition();
        config.wciLauncherButtonBankOverlayX = bankOverlay.x();
        config.wciLauncherButtonBankOverlayY = bankOverlay.y();
        config.wciLauncherButtonBankVanillaCustomPosition = bankVanilla.customPosition();
        config.wciLauncherButtonBankVanillaX = bankVanilla.x();
        config.wciLauncherButtonBankVanillaY = bankVanilla.y();
    }

    private static void setPosition(WynnExtrasConfig config, WciPlacementContext placementContext, int x, int y) {
        switch (placementContext) {
            case TRADE_MARKET -> {
                config.wciLauncherButtonTradeCustomPosition = true;
                config.wciLauncherButtonTradeX = x;
                config.wciLauncherButtonTradeY = y;
            }
            case BANK_OVERLAY -> {
                config.wciLauncherButtonBankOverlayCustomPosition = true;
                config.wciLauncherButtonBankOverlayX = x;
                config.wciLauncherButtonBankOverlayY = y;
            }
            case BANK_VANILLA -> {
                config.wciLauncherButtonBankVanillaCustomPosition = true;
                config.wciLauncherButtonBankVanillaX = x;
                config.wciLauncherButtonBankVanillaY = y;
            }
            case OTHER -> {
                config.wciLauncherButtonCustomPosition = true;
                config.wciLauncherButtonX = x;
                config.wciLauncherButtonY = y;
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean overlapsAny(Bounds bounds, List<Bounds> forbiddenAreas) {
        for (Bounds forbiddenArea : forbiddenAreas) {
            if (bounds.overlaps(forbiddenArea)) {
                return true;
            }
        }
        return false;
    }

    private static boolean fitsInsideScreen(Bounds bounds, int screenWidth, int screenHeight) {
        return bounds.x() >= SCREEN_MARGIN
                && bounds.y() >= SCREEN_MARGIN
                && bounds.x() + bounds.width() <= screenWidth - SCREEN_MARGIN
                && bounds.y() + bounds.height() <= screenHeight - SCREEN_MARGIN;
    }

    private static Bounds clampBounds(Bounds bounds, int screenWidth, int screenHeight) {
        int maxX = Math.max(SCREEN_MARGIN, screenWidth - bounds.width() - SCREEN_MARGIN);
        int maxY = Math.max(SCREEN_MARGIN, screenHeight - bounds.height() - SCREEN_MARGIN);
        return new Bounds(
                clamp(bounds.x(), SCREEN_MARGIN, maxX),
                clamp(bounds.y(), SCREEN_MARGIN, maxY),
                bounds.width(),
                bounds.height());
    }

    private Bounds boundsFor(HandledScreen<?> screen, WciPlacementContext placementContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) {
            return Bounds.hidden();
        }
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        PositionState savedPosition = selectPositionState(positionStates(WynnExtrasConfig.INSTANCE), placementContext, true);
        if (savedPosition.customPosition()) {
            return clampSavedBounds(
                    savedPosition.x(),
                    savedPosition.y(),
                    screenWidth,
                    screenHeight);
        }

        int screenX = HandledScreenAccess.x(screen);
        int screenY = HandledScreenAccess.y(screen);
        int backgroundWidth = HandledScreenAccess.backgroundWidth(screen);
        int backgroundHeight = HandledScreenAccess.backgroundHeight(screen);
        List<Bounds> forbiddenAreas = forbiddenAreas(placementContext, screenX, screenY, backgroundWidth, backgroundHeight,
                screenWidth, screenHeight);
        Bounds bankOverlayBounds = placementContext == WciPlacementContext.BANK_OVERLAY
                ? bankOverlayDefaultBounds(screenWidth, screenHeight)
                : null;
        return computeBounds(
                placementContext,
                screenX,
                screenY,
                backgroundWidth,
                backgroundHeight,
                screenWidth,
                screenHeight,
                forbiddenAreas,
                bankOverlayBounds);
    }

    private List<Bounds> forbiddenAreas(WciPlacementContext placementContext, int screenX, int screenY,
                                        int backgroundWidth, int backgroundHeight,
                                        int screenWidth, int screenHeight) {
        List<Bounds> forbiddenAreas = new ArrayList<>();

        if (placementContext == WciPlacementContext.BANK_VANILLA
                || placementContext == WciPlacementContext.TRADE_MARKET) {
            forbiddenAreas.add(new Bounds(screenX - GAP, screenY - GAP,
                    backgroundWidth + GAP * 2, backgroundHeight + GAP * 2));
        }
        return forbiddenAreas;
    }

    public static Bounds bankOverlayDefaultBounds(int screenWidth, int screenHeight) {
        return clampBounds(new Bounds(BANK_OVERLAY_DEFAULT_X, BANK_OVERLAY_DEFAULT_Y, BUTTON_WIDTH, BUTTON_HEIGHT),
                screenWidth, screenHeight);
    }

    private void startDrag(Bounds clickBounds, double mouseX, double mouseY, int button,
                           WciPlacementContext placementContext) {
        dragging = true;
        dragMoved = false;
        dragButton = button;
        dragContext = placementContext;
        dragOffsetX = (int) mouseX - clickBounds.x();
        dragOffsetY = (int) mouseY - clickBounds.y();
    }

    private static WciPlacementContext placementContext(WciScreenContext screenContext, boolean customBankOverlayActive) {
        if (screenContext == WciScreenContext.TRADE_MARKET
                || screenContext == WciScreenContext.TRADE_MARKET_FILTER
                || screenContext == WciScreenContext.TRADE_MARKET_DETAIL) {
            return WciPlacementContext.TRADE_MARKET;
        }
        if (screenContext == WciScreenContext.BANK_OVERLAY) {
            return WciPlacementContext.BANK_OVERLAY;
        }
        if (screenContext == WciScreenContext.BANK_VANILLA) {
            return WciPlacementContext.BANK_VANILLA;
        }
        return WciPlacementContext.OTHER;
    }

    private int screenWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.getWindow() != null ? client.getWindow().getScaledWidth() : BUTTON_WIDTH + SCREEN_MARGIN * 2;
    }

    private int screenHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.getWindow() != null ? client.getWindow().getScaledHeight() : BUTTON_HEIGHT + SCREEN_MARGIN * 2;
    }

    public record Bounds(int x, int y, int width, int height) {
        static Bounds hidden() {
            return new Bounds(0, 0, 0, 0);
        }

        boolean visible() {
            return width > 0 && height > 0;
        }

        public boolean contains(double mouseX, double mouseY) {
            return visible() && mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        }

        public boolean overlaps(Bounds other) {
            return visible() && other != null && other.visible()
                    && x < other.x + other.width
                    && x + width > other.x
                    && y < other.y + other.height
                    && y + height > other.y;
        }
    }

    public record PositionState(boolean customPosition, int x, int y) {}

    public record ContextPositionStates(
            PositionState legacy,
            PositionState tradeMarket,
            PositionState bank,
            PositionState bankOverlay,
            PositionState bankVanilla) {}
}
