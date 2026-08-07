package julianh06.wynnextras.features.shoppinglist.ui;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.wynncraft.menu.MenuType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class ShoppingListMenuLauncherButton {
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 16;
    private static final int SCREEN_MARGIN = 4;
    private static final CustomColor TEXT = CustomColor.fromHexString("FFFFFF");

    private final UIUtils ui = new UIUtils(null, 1, 0, 0);
    private Bounds bounds = Bounds.hidden();

    public void render(DrawContext context, HandledScreen<?> screen, int mouseX, int mouseY, float delta,
                       boolean customBankOverlayActive) {
        ShoppingListScreenContext screenContext = ShoppingListScreenContext.detect(screen, customBankOverlayActive);
        if (!shouldRender(screenContext)) {
            bounds = Bounds.hidden();
            return;
        }

        bounds = boundsFor(screen);
        ui.updateContext(context, 1, 0, 0);

        boolean active = ShoppingListMenuExtension.isVisible();
        boolean hovered = bounds.contains(mouseX, mouseY);
        ui.drawButton(bounds.x(), bounds.y(), bounds.width(), bounds.height(), hovered);
        ui.drawCenteredText(label(active), bounds.x() + bounds.width() / 2f, bounds.y() + bounds.height() / 2f, TEXT, 0.75f);
        if (hovered) {
            int toggleKey = WynnExtrasConfig.INSTANCE.shoppingListToggleKey;
            String keyName = toggleKey == GLFW.GLFW_KEY_UNKNOWN
                    ? "NOT BOUND"
                    : InputUtil.Type.KEYSYM.createFromCode(toggleKey).getLocalizedText().getString();
            List<Text> tooltipLines = toggleKey == GLFW.GLFW_KEY_UNKNOWN
                    ? List.of(
                            Text.literal("You can hide this button in the WynnExtras config."),
                            Text.literal("You can also toggle this list using [" + keyName + "]."),
                            Text.literal("You can bind this key in the WynnExtras config."))
                    : List.of(
                            Text.literal("You can hide this button in the WynnExtras config."),
                            Text.literal("You can also toggle this list using [" + keyName + "]."));
            context.drawTooltipImmediately(
                    MinecraftClient.getInstance().textRenderer,
                    tooltipLines.stream()
                            .map(line -> TooltipComponent.of(line.asOrderedText()))
                            .toList(),
                    mouseX,
                    mouseY,
                    HoveredTooltipPositioner.INSTANCE,
                    null);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, HandledScreen<?> screen,
                                boolean customBankOverlayActive) {
        ShoppingListScreenContext screenContext = ShoppingListScreenContext.detect(screen, customBankOverlayActive);
        if (!shouldRender(screenContext)) {
            return false;
        }

        Bounds clickBounds = bounds.visible() ? bounds : boundsFor(screen);
        if (!clickBounds.contains(mouseX, mouseY)) {
            return false;
        }

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        if (ShoppingListMenuExtension.isVisible()) {
            ShoppingListMenuExtension.close();
        } else {
            ShoppingListMenuExtension.show();
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, ShoppingListScreenContext screenContext) {
        if (!shouldRender(screenContext)) {
            cancelDrag();
            return false;
        }
        return mouseDragged(mouseX, mouseY, button);
    }

    public boolean mouseReleased(int button) {
        return false;
    }

    public boolean mouseReleased(int button, ShoppingListScreenContext screenContext) {
        if (!shouldRender(screenContext)) {
            cancelDrag();
            return false;
        }
        return mouseReleased(button);
    }

    public void cancelDrag() {}

    public boolean shouldRender(HandledScreen<?> screen, boolean customBankOverlayActive) {
        return shouldRender(ShoppingListScreenContext.detect(screen, customBankOverlayActive));
    }

    public static boolean shouldRender(boolean tradeMarketScreen, boolean bankLikeScreen, boolean customBankOverlayActive) {
        return shouldRender(ShoppingListScreenContext.fromSignals("", tradeMarketScreen, bankLikeScreen, customBankOverlayActive, false));
    }

    public static boolean shouldRender(ShoppingListScreenContext screenContext) {
        return WynnExtrasConfig.INSTANCE.shoppingListShowQuickToggleButton
                && ShoppingListMenuRenderPolicy.shouldRenderLauncher(screenContext);
    }

    public static boolean isBankLikeMenu(MenuType menuType) {
        return menuType == MenuType.ACCOUNT_BANK
                || menuType == MenuType.CHARACTER_BANK
                || menuType == MenuType.BOOKSHELF
                || menuType == MenuType.MISC_BUCKET;
    }

    public static String label(boolean active) {
        return "Toggle Shoppinglist";
    }

    public static Bounds computeBounds(int screenX, int screenY, int backgroundWidth, int screenWidth, int screenHeight) {
        return computeBounds(screenX, screenY, backgroundWidth, 166, screenWidth, screenHeight, List.of());
    }

    public static Bounds computeBounds(int screenX, int screenY, int backgroundWidth, int backgroundHeight,
                                       int screenWidth, int screenHeight, List<Bounds> forbiddenAreas) {
        return computeBounds(ShoppingListPlacementContext.OTHER, screenX, screenY, backgroundWidth, backgroundHeight,
                screenWidth, screenHeight, forbiddenAreas);
    }

    public static Bounds computeBounds(ShoppingListPlacementContext placementContext, int screenX, int screenY,
                                       int backgroundWidth, int backgroundHeight, int screenWidth, int screenHeight,
                                       List<Bounds> forbiddenAreas) {
        return computeBounds(placementContext, screenX, screenY, backgroundWidth, backgroundHeight,
                screenWidth, screenHeight, forbiddenAreas, null);
    }

    public static Bounds computeBounds(ShoppingListPlacementContext placementContext, int screenX, int screenY,
                                       int backgroundWidth, int backgroundHeight, int screenWidth, int screenHeight,
                                       List<Bounds> forbiddenAreas, Bounds bankOverlayPreferredBounds) {
        return bottomLeftBounds(screenWidth, screenHeight);
    }

    public static Bounds clampSavedBounds(int x, int y, int screenWidth, int screenHeight) {
        return clampBounds(new Bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT), screenWidth, screenHeight);
    }

    public static void resetPosition() {
        resetPosition(WynnExtrasConfig.INSTANCE);
        WynnExtrasConfig.save();
    }

    static void resetPosition(WynnExtrasConfig config) {
        config.shoppingListLauncherButtonDefaultPosition.reset();
        config.shoppingListLauncherButtonTradePosition.reset();
        config.shoppingListLauncherButtonBankOverlayPosition.reset();
        config.shoppingListLauncherButtonBankVanillaPosition.reset();
    }

    private static int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
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

    private Bounds boundsFor(HandledScreen<?> screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) {
            return Bounds.hidden();
        }
        return bottomLeftBounds(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }

    public static Bounds bankOverlayDefaultBounds(int screenWidth, int screenHeight) {
        return bottomLeftBounds(screenWidth, screenHeight);
    }

    private static Bounds bottomLeftBounds(int screenWidth, int screenHeight) {
        return clampBounds(new Bounds(SCREEN_MARGIN, screenHeight - BUTTON_HEIGHT - SCREEN_MARGIN,
                BUTTON_WIDTH, BUTTON_HEIGHT), screenWidth, screenHeight);
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
}
