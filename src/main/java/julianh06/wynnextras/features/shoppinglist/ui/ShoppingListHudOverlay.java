package julianh06.wynnextras.features.shoppinglist.ui;

import julianh06.wynnextras.features.shoppinglist.service.ShoppingListTradeMarketPurchaseService;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderTickCounter;

import java.util.WeakHashMap;

public final class ShoppingListHudOverlay {
    private static final ShoppingListMenuExtension EXTENSION = new ShoppingListMenuExtension();
    private static final WeakHashMap<Screen, ScreenState> SCREEN_STATES = new WeakHashMap<>();

    private ShoppingListHudOverlay() {}

    public static void register() {
        HudRenderCallback.EVENT.register(ShoppingListHudOverlay::render);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            ShoppingListScreenContext screenContext = screenContext(screen);
            if (screenContext == null) return;

            ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, tickDelta) ->
                    renderOnScreen(s, context, mouseX, mouseY, tickDelta));
            ScreenMouseEvents.allowMouseClick(screen).register((s, click) ->
                    !mouseClicked(s, click.x(), click.y(), click.button()));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, click) ->
                    !mouseReleased(s, click.x(), click.y(), click.button()));
            ScreenMouseEvents.allowMouseDrag(screen).register((s, click, deltaX, deltaY) ->
                    !mouseDragged(s, click.x(), click.y(), click.button(), deltaX, deltaY));
            ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontalAmount, verticalAmount) ->
                    !mouseScrolled(s, mouseX, mouseY, verticalAmount));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, input) ->
                    !keyPressed(s, input.key(), input.scancode(), input.modifiers()));
            ScreenEvents.remove(screen).register(removedScreen -> {
                SCREEN_STATES.remove(removedScreen);
                if (removedScreen instanceof ChatScreen) {
                    ShoppingListTradeMarketPurchaseService.clearContext();
                }
            });
        });
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.currentScreen != null || client.options.hudHidden) {
            return;
        }
        if (!ShoppingListMenuExtension.shouldRender(ShoppingListScreenContext.HUD)) {
            return;
        }

        EXTENSION.setScreenContext(ShoppingListScreenContext.HUD);
        EXTENSION.setPlacementContext(ShoppingListPlacementContext.OTHER);
        EXTENSION.render(context, -1, -1, tickCounter.getTickProgress(false));
    }

    private static void renderOnScreen(Screen screen, DrawContext context, int mouseX, int mouseY, float tickDelta) {
        ShoppingListScreenContext screenContext = screenContext(screen);
        if (screenContext == null) return;

        ScreenState state = state(screen);
        if (screen instanceof InventoryScreen inventoryScreen) {
            state.launcher().render(context, inventoryScreen, mouseX, mouseY, tickDelta, false);
        }
        if (!ShoppingListMenuExtension.shouldRender(screenContext)) return;

        state.extension().setScreenContext(screenContext);
        state.extension().setPlacementContext(screenContext.placementContext());
        state.extension().render(context, mouseX, mouseY, tickDelta);
    }

    private static boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        ShoppingListScreenContext screenContext = screenContext(screen);
        if (screenContext == null) return false;

        ScreenState state = state(screen);
        if (screen instanceof InventoryScreen inventoryScreen
                && state.launcher().mouseClicked(mouseX, mouseY, button, inventoryScreen, false)) {
            return true;
        }
        configure(state.extension(), screenContext);
        return state.extension().mouseClicked(mouseX, mouseY, button);
    }

    private static boolean mouseReleased(Screen screen, double mouseX, double mouseY, int button) {
        ShoppingListScreenContext screenContext = screenContext(screen);
        if (screenContext == null) return false;

        ScreenState state = state(screen);
        if (state.launcher().mouseReleased(button, screenContext)) {
            return true;
        }
        configure(state.extension(), screenContext);
        return state.extension().mouseReleased(mouseX, mouseY, button);
    }

    private static boolean mouseDragged(Screen screen, double mouseX, double mouseY, int button,
                                        double deltaX, double deltaY) {
        ShoppingListScreenContext screenContext = screenContext(screen);
        if (screenContext == null) return false;

        ScreenState state = state(screen);
        if (state.launcher().mouseDragged(mouseX, mouseY, button, screenContext)) {
            return true;
        }
        configure(state.extension(), screenContext);
        return state.extension().mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private static boolean mouseScrolled(Screen screen, double mouseX, double mouseY, double verticalAmount) {
        ShoppingListScreenContext screenContext = screenContext(screen);
        if (screenContext == null) return false;

        ShoppingListMenuExtension extension = state(screen).extension();
        configure(extension, screenContext);
        return extension.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    private static boolean keyPressed(Screen screen, int keyCode, int scanCode, int modifiers) {
        ShoppingListScreenContext screenContext = screenContext(screen);
        if (screenContext == null) return false;
        if (ShoppingListMenuExtension.isToggleKey(keyCode)) {
            ShoppingListMenuExtension.toggleFromHotkey(screenContext);
            return true;
        }

        ShoppingListMenuExtension extension = state(screen).extension();
        configure(extension, screenContext);
        return extension.keyPressed(keyCode, scanCode, modifiers);
    }

    private static void configure(ShoppingListMenuExtension extension, ShoppingListScreenContext screenContext) {
        extension.setScreenContext(screenContext);
        extension.setPlacementContext(screenContext.placementContext());
    }

    private static ShoppingListScreenContext screenContext(Screen screen) {
        if (screen instanceof InventoryScreen) return ShoppingListScreenContext.INVENTORY;
        if (screen instanceof ChatScreen) return ShoppingListScreenContext.CHAT;
        return null;
    }

    private static ScreenState state(Screen screen) {
        return SCREEN_STATES.computeIfAbsent(screen, ignored -> new ScreenState());
    }

    private record ScreenState(ShoppingListMenuExtension extension, ShoppingListMenuLauncherButton launcher) {
        private ScreenState() {
            this(new ShoppingListMenuExtension(), new ShoppingListMenuLauncherButton());
        }
    }
}
