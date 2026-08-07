package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.wynncraft.menu.MenuType;
import julianh06.wynnextras.wynncraft.menu.WynncraftMenuService;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.crafting.CraftingHelperOverlay;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import julianh06.wynnextras.features.mount.MountOverlay;
import julianh06.wynnextras.utils.LunarCompat;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

import java.util.WeakHashMap;

public final class LunarScreenOverlayFallback {
    private static final WeakHashMap<Screen, State> STATES = new WeakHashMap<>();

    private LunarScreenOverlayFallback() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof HandledScreen<?> handledScreen)) return;

            ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, tickDelta) ->
                    render(screen, handledScreen, context, mouseX, mouseY, tickDelta));
            ScreenMouseEvents.allowMouseClick(screen).register((s, click) ->
                    !mouseClicked(screen, click.x(), click.y(), click.button()));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, click) ->
                    !mouseReleased(screen, click.x(), click.y(), click.button()));
            ScreenMouseEvents.allowMouseDrag(screen).register((s, click, deltaX, deltaY) ->
                    !mouseDragged(screen, click.x(), click.y(), click.button(), deltaX, deltaY));
            ScreenEvents.remove(screen).register(s -> STATES.remove(screen));
        });
    }

    private static void render(Screen screen, HandledScreen<?> handledScreen, DrawContext context, int mouseX, int mouseY, float delta) {
        if (!shouldRender(screen)) return;
        State state = state(screen);

        if (WynnExtrasConfig.INSTANCE.customClassSelectionEnabled && ClassSelectionOverlay.isClassSelectionScreen(screen.getTitle().getString())) {
            if (state.classSelectionOverlay == null) {
                state.classSelectionOverlay = new ClassSelectionOverlay(handledScreen, ClassSelectionOverlay.ScreenMode.CLASS_SELECTION);
            }
            state.classSelectionOverlay.render(context, mouseX, mouseY, delta);
            ProfessionOverlay.renderOnScreen(context);
            return;
        } else {
            state.classSelectionOverlay = null;
        }

        MountOverlay.render(context, mouseX, mouseY);
        BankOverlay.updateOverlayType();

        if (isBankContainer() || BankOverlay.currentOverlayType != BankOverlayType.NONE) {
            if (state.bankOverlay == null) state.bankOverlay = new BankOverlay2(null, handledScreen);
            state.bankOverlay.updateRenderContext(null, handledScreen, close -> {
                handledScreen.close();
                return null;
            });
            state.bankOverlay.render(context, mouseX, mouseY, delta);
        }

        if (WynnExtrasConfig.INSTANCE.craftingHelperOverlay && WynncraftMenuService.isCurrent(MenuType.CRAFTING_STATION)) {
            if (state.craftingHelperOverlay == null) state.craftingHelperOverlay = new CraftingHelperOverlay();
            state.craftingHelperOverlay.render(context, mouseX, mouseY, delta);
        } else {
            state.craftingHelperOverlay = null;
        }

        if (WynnExtrasConfig.INSTANCE.skillpointHelper && WynncraftMenuService.isCurrent(MenuType.CHARACTER_INFO)) {
            if (state.compassMenuOverlay == null) state.compassMenuOverlay = new CompassMenuOverlay();
            state.compassMenuOverlay.render(context, mouseX, mouseY, delta);
        } else {
            state.compassMenuOverlay = null;
        }

        BankOverlay2.drawVanillaBankBagsOverlay(context, handledScreen);
        ProfessionOverlay.renderOnScreen(context);
        if (state.bankOverlay != null) {
            BankOverlay2.renderLunarFallbackTooltip(context, mouseX, mouseY);
        }
        if (state.compassMenuOverlay != null) {
            state.compassMenuOverlay.renderHoveredTooltip(context, mouseX, mouseY);
        }
    }

    private static boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        if (!shouldRender(screen)) return false;
        State state = state(screen);

        if (state.classSelectionOverlay != null) {
            state.classSelectionOverlay.mouseClicked(mouseX, mouseY, button);
            return true;
        }

        if (state.bankOverlay != null) {
            boolean handledByBankOverlay = state.bankOverlay.mouseClicked(mouseX, mouseY, button, false);
            if (handledByBankOverlay && WynnExtrasConfig.INSTANCE.toggleBankOverlay && BankOverlay.currentOverlayType != BankOverlayType.NONE) {
                return true;
            }
        }

        if (state.craftingHelperOverlay != null && WynnExtrasConfig.INSTANCE.craftingHelperOverlay
                && WynncraftMenuService.isCurrent(MenuType.CRAFTING_STATION)) {
            state.craftingHelperOverlay.mouseClicked(mouseX, mouseY, button);
        }

        if (state.compassMenuOverlay != null && WynnExtrasConfig.INSTANCE.skillpointHelper
                && WynncraftMenuService.isCurrent(MenuType.CHARACTER_INFO)) {
            state.compassMenuOverlay.mouseClicked(mouseX, mouseY, button);
            return CompassMenuOverlay.isSelectingWeapon();
        }

        return false;
    }

    private static boolean mouseReleased(Screen screen, double mouseX, double mouseY, int button) {
        if (!shouldRender(screen)) return false;
        State state = state(screen);

        if (state.classSelectionOverlay != null) {
            state.classSelectionOverlay.onMouseReleased(mouseX, mouseY, button);
            return true;
        }

        if (state.bankOverlay != null) {
            state.bankOverlay.mouseReleased(mouseX, mouseY, button);
            if (WynnExtrasConfig.INSTANCE.toggleBankOverlay && BankOverlay.currentOverlayType != BankOverlayType.NONE) return true;
        }

        if (state.craftingHelperOverlay != null && WynnExtrasConfig.INSTANCE.craftingHelperOverlay) {
            state.craftingHelperOverlay.mouseReleased(mouseX, mouseY, button);
        }

        return false;
    }

    private static boolean mouseDragged(Screen screen, double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!shouldRender(screen)) return false;
        State state = state(screen);

        if (state.classSelectionOverlay != null) {
            state.classSelectionOverlay.onMouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            return true;
        }

        return state.bankOverlay != null && state.bankOverlay.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private static boolean shouldRender(Screen screen) {
        return LunarCompat.isLunarClient()
                && !LunarCompat.wasHandledScreenMixinRenderedRecently(screen);
    }

    private static State state(Screen screen) {
        return STATES.computeIfAbsent(screen, s -> new State());
    }

    private static boolean isBankContainer() {
        return WynncraftMenuService.isCurrentAny(
                MenuType.ACCOUNT_BANK, MenuType.CHARACTER_BANK, MenuType.BOOKSHELF, MenuType.MISC_BUCKET);
    }

    private static final class State {
        private BankOverlay2 bankOverlay;
        private ClassSelectionOverlay classSelectionOverlay;
        private CraftingHelperOverlay craftingHelperOverlay;
        private CompassMenuOverlay compassMenuOverlay;
    }
}