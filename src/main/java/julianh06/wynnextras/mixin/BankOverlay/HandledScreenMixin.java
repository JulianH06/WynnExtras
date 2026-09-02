package julianh06.wynnextras.mixin.BankOverlay;

import julianh06.wynnextras.compat.wynntils.WynntilsBankAdapter;
import julianh06.wynnextras.wynncraft.menu.MenuType;
import julianh06.wynnextras.wynncraft.menu.WynncraftMenuService;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.InventoryKeyPressEvent;
import julianh06.wynnextras.features.aspects.PartyFinderOpenLootpoolOverlay;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.bankoverlay.BankOverlaySlotBridge;
import julianh06.wynnextras.features.crafting.CraftingHelperOverlay;
import julianh06.wynnextras.features.inventory.*;
import julianh06.wynnextras.features.misc.ClassSelectionOverlay;
import julianh06.wynnextras.features.misc.CompassMenuOverlay;
import julianh06.wynnextras.features.misc.IdentifierCaseOpeningOverlay;
import julianh06.wynnextras.features.misc.IdentifierOverlay;
import julianh06.wynnextras.features.misc.ItemComponentsDebugOverlay;
import julianh06.wynnextras.features.misc.ProfessionOverlay;
import julianh06.wynnextras.features.misc.QuickRepair;
import julianh06.wynnextras.features.mount.MountOverlay;
import julianh06.wynnextras.features.shoppinglist.ui.ShoppingListScreenContext;
import julianh06.wynnextras.features.shoppinglist.ui.ShoppingListMenuExtension;
import julianh06.wynnextras.features.shoppinglist.ui.ShoppingListMenuLauncherButton;
import julianh06.wynnextras.features.shoppinglist.service.ShoppingListTradeMarketPurchaseService;
import julianh06.wynnextras.utils.LunarCompat;
import julianh06.wynnextras.utils.SmoothGuiCompat;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.List;

import static julianh06.wynnextras.features.inventory.BankOverlay.*;

@WEModule
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Unique private static final int WYNNTILS_BANK_WIDGET_Y_OFFSET = 14;

    @Shadow public abstract void close();

    @Shadow public Slot focusedSlot;

    @Shadow protected int x;
    @Shadow protected int y;

    @Unique private julianh06.wynnextras.features.bankoverlay.BankOverlay2 bankOverlay;
    @Unique private Boolean isBankScreen = null;

    @Unique private IdentifierOverlay identifierOverlay;
    @Unique private IdentifierCaseOpeningOverlay identifierCaseOpeningOverlay;

    @Unique private PartyFinderOpenLootpoolOverlay partyFinderOpenLootpoolOverlay;

    @Unique private CraftingHelperOverlay craftingHelperOverlay;

    @Unique private PowderCombineHelperOverlay powderCombineHelperOverlay;

    @Unique private ClassSelectionOverlay classSelectionOverlay;

    @Unique private CompassMenuOverlay compassMenuOverlay;

    @Unique private QuickRepair quickRepairOverlay;

    @Unique private ShoppingListMenuExtension shoppingListMenuExtension;
    @Unique private ShoppingListMenuLauncherButton shoppingListMenuLauncherButton;
    @Unique private boolean shoppingListRenderedThisFrame = false;

    @Inject(method = "renderBackground", at = @At(value = "HEAD"), cancellable = true)
    private void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci){
        if (WynnExtrasConfig.INSTANCE.toggleBankOverlay && currentOverlayType != BankOverlayType.NONE) {
            ci.cancel();
        }
        if (classSelectionOverlay != null) {
            ci.cancel();
        }
        if (identifierCaseOpeningOverlay != null && identifierCaseOpeningOverlay.shouldHideVanilla()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderInventory(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        shoppingListRenderedThisFrame = false;
        LunarCompat.recordHandledScreenMixinRender((HandledScreen<?>) (Object) this);
        // Encounter Selection Overlay (must render FIRST and cancel vanilla render so chest UI is fully hidden)
        {
            HandledScreen<?> encSelf = (HandledScreen<?>) (Object) this;
            if (julianh06.wynnextras.features.qol.EncounterOverlay.isReadyToRender(encSelf)) {
                julianh06.wynnextras.features.qol.EncounterOverlay.render(context, encSelf, mouseX, mouseY);
                ProfessionOverlay.renderOnScreen(context);
                ci.cancel();
                return;
            }
            // Tick the settle state regardless (for non-ready cases).
            julianh06.wynnextras.features.qol.EncounterOverlay.tickSettle(encSelf);
        }
        if (identifierCaseOpeningOverlay != null && identifierCaseOpeningOverlay.shouldHideVanilla()) {
            identifierCaseOpeningOverlay.render(context, mouseX, mouseY, delta);
            ci.cancel();
            return;
        }
        // Class Selection Overlay
        if (WynnExtrasConfig.INSTANCE.customClassSelectionEnabled) {
            HandledScreen<?> self = (HandledScreen<?>) (Object) this;
            String title = self.getTitle().getString();
            if (ClassSelectionOverlay.isClassSelectionScreen(title)) {
                if (classSelectionOverlay == null || classSelectionOverlay.getMode() != ClassSelectionOverlay.ScreenMode.CLASS_SELECTION) {
                    classSelectionOverlay = new ClassSelectionOverlay(self, ClassSelectionOverlay.ScreenMode.CLASS_SELECTION);
                }
                classSelectionOverlay.render(context, mouseX, mouseY, delta);
                ProfessionOverlay.renderOnScreen(context);
                ci.cancel();
                return;
            } else {
                classSelectionOverlay = null;
            }
        } else {
            classSelectionOverlay = null;
        }

        MountOverlay.render(context, mouseX, mouseY);
        // Only create BankOverlay2 for bank-type containers to avoid expensive
        // initialization on every GUI open
        if (isBankScreen == null) {
            isBankScreen = WynncraftMenuService.isCurrentAny(
                    MenuType.ACCOUNT_BANK, MenuType.CHARACTER_BANK, MenuType.BOOKSHELF, MenuType.MISC_BUCKET);
        }

        if (Boolean.TRUE.equals(isBankScreen) || currentOverlayType != BankOverlayType.NONE) {
            if (bankOverlay == null) bankOverlay = new BankOverlay2(ci, (HandledScreen<?>) (Object) this);
            bankOverlay.updateRenderContext(ci, (HandledScreen<?>) (Object) this, close -> {
                close();
                return null;
            });
            boolean poppedSmoothGui = SmoothGuiCompat.popIfApplied(context);
            bankOverlay.render(context, mouseX, mouseY, delta);
            SmoothGuiCompat.pushIfNeeded(context, poppedSmoothGui && !ci.isCancelled());
            renderWynntilsBankPageJumpButtons(context, mouseX, mouseY, delta, (HandledScreen<?>) (Object) this);
            if (isCustomBankOverlayReplacingVanilla() && ci.isCancelled()) {
                renderShoppingListMenu(context, mouseX, mouseY, delta, ShoppingListScreenContext.BANK_OVERLAY, true);
                renderShoppingListMenuLauncherButton(context, mouseX, mouseY, delta, true);
            }
        }

        if(WynnExtrasConfig.INSTANCE.sourceOfTruthToggle) {
            if (identifierOverlay == null) {
                identifierOverlay = new IdentifierOverlay();
            }

            identifierOverlay.render(context, mouseX, mouseY, delta);
        }

        if(WynnExtrasConfig.INSTANCE.showLootpoolButtonInPartyFinder) {
            if(partyFinderOpenLootpoolOverlay == null) {
                partyFinderOpenLootpoolOverlay = new PartyFinderOpenLootpoolOverlay();
            }

            partyFinderOpenLootpoolOverlay.render(context, mouseX, mouseY, delta);
        }

        if(WynnExtrasConfig.INSTANCE.craftingHelperOverlay) {
            if (craftingHelperOverlay == null) {
                craftingHelperOverlay = new CraftingHelperOverlay();
            }

            craftingHelperOverlay.render(context, mouseX, mouseY, delta);
        }

        if(WynnExtrasConfig.INSTANCE.skillpointHelper) {
            if(compassMenuOverlay == null) {
                compassMenuOverlay = new CompassMenuOverlay();
            }

            compassMenuOverlay.render(context, mouseX, mouseY, delta);
        }


        // Character selection highlighting (when clicking cross-class bank page)
        renderCharacterSelectionHighlight(context, (HandledScreen<?>) (Object) this);

        if (ci.isCancelled()) {
            ProfessionOverlay.renderOnScreen(context);
        }
    }

    @Inject(method = "render", at = @At("TAIL"), cancellable = true)
    private void renderForeground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Vanilla mode toggle button for class selection
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        ClassSelectionOverlay.renderVanillaToggleButton(context, self);
        // Trade Market Overlay (Your Trades value display)
        TradeMarketOverlay.renderOnScreen(context);

        // Trade Market Comparison Panel
        TradeMarketComparisonPanel.render(context);

        // Bank bag overlay in vanilla bank mode (custom mode draws it from BankOverlay2.render())
        BankOverlay2.drawVanillaBankBagsOverlay(context, self);

        // Quick Repair button in blacksmith
        if (quickRepairOverlay == null) quickRepairOverlay = new QuickRepair();
        quickRepairOverlay.render(context, mouseX, mouseY, delta);

        if (WynnExtrasConfig.INSTANCE.powderCombineHelper && PowderCombineHelperOverlay.isSupportedScreen()) {
            if (powderCombineHelperOverlay == null) powderCombineHelperOverlay = new PowderCombineHelperOverlay();
            powderCombineHelperOverlay.render(context, mouseX, mouseY, delta);
        } else {
            powderCombineHelperOverlay = null;
        }

        if (!(self instanceof InventoryScreen)) {
            boolean shoppingListBankOverlayPlacementMode = isShoppingListBankOverlayPlacementMode();
            renderShoppingListMenu(context, mouseX, mouseY, delta,
                    currentShoppingListScreenContext(shoppingListBankOverlayPlacementMode), shoppingListBankOverlayPlacementMode);
            renderShoppingListMenuLauncherButton(context, mouseX, mouseY, delta, shoppingListBankOverlayPlacementMode);
        }

        ProfessionOverlay.renderOnScreen(context);
        if (!(self instanceof InventoryScreen)) {
            ItemComponentsDebugOverlay.render(context, mouseX, mouseY);
        }
        if (compassMenuOverlay != null
                && WynnExtrasConfig.INSTANCE.skillpointHelper
                && WynncraftMenuService.isCurrent(MenuType.CHARACTER_INFO)) {
            compassMenuOverlay.renderHoveredTooltip(context, mouseX, mouseY);
        }
        if (WynnExtrasConfig.INSTANCE.identifierCaseOpening
                && WynncraftMenuService.isCurrentAny(MenuType.ITEM_IDENTIFIER, MenuType.AUGMENT_IDENTIFIER)) {
            ensureIdentifierCaseOpeningOverlay().render(context, mouseX, mouseY, delta);
        }
    }

    @Unique
    private IdentifierCaseOpeningOverlay ensureIdentifierCaseOpeningOverlay() {
        if (identifierCaseOpeningOverlay == null) {
            identifierCaseOpeningOverlay = new IdentifierCaseOpeningOverlay();
        }
        return identifierCaseOpeningOverlay;
    }

    @Unique
    private ShoppingListMenuExtension ensureShoppingListMenuExtension() {
        if (!ShoppingListMenuExtension.isVisible()) {
            return null;
        }
        if (shoppingListMenuExtension == null) {
            shoppingListMenuExtension = new ShoppingListMenuExtension();
        }
        return shoppingListMenuExtension;
    }

    @Unique
    private void renderShoppingListMenu(DrawContext context, int mouseX, int mouseY, float delta,
                                       ShoppingListScreenContext screenContext, boolean customBankOverlayActive) {
        if (shoppingListRenderedThisFrame) {
            return;
        }
        if (!ShoppingListMenuExtension.shouldRender(screenContext)) {
            return;
        }
        ShoppingListMenuExtension extension = ensureShoppingListMenuExtension();
        if (extension == null) {
            return;
        }
        extension.setScreenContext(screenContext);
        extension.setPlacementContext(screenContext.placementContext());
        extension.render(context, mouseX, mouseY, delta);
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        if (extension.consumesHover(self, mouseX, mouseY)) {
            BankOverlay2.suppressHoveredTooltip(self);
        }
        shoppingListRenderedThisFrame = true;
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void consumeTooltipBehindShoppingList(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (identifierCaseOpeningOverlay != null && identifierCaseOpeningOverlay.isReplacingMenu()) {
            focusedSlot = null;
            ci.cancel();
            return;
        }
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        if (self instanceof InventoryScreen) return;

        boolean bankOverlayPlacementMode = isShoppingListBankOverlayPlacementMode();
        ShoppingListMenuExtension extension = ensureShoppingListMenuExtension();
        if (extension == null) return;
        configureShoppingListMenuExtension(extension, bankOverlayPlacementMode);
        if (!extension.consumesHover(self, mouseX, mouseY)) return;

        BankOverlay2.suppressHoveredTooltip(self);
        ci.cancel();
    }

    @Unique
    private ShoppingListMenuLauncherButton ensureShoppingListMenuLauncherButton() {
        if (shoppingListMenuLauncherButton == null) {
            shoppingListMenuLauncherButton = new ShoppingListMenuLauncherButton();
        }
        return shoppingListMenuLauncherButton;
    }

    @Unique
    private void renderShoppingListMenuLauncherButton(DrawContext context, int mouseX, int mouseY, float delta,
                                                     boolean customBankOverlayActive) {
        ensureShoppingListMenuLauncherButton().render(context, (HandledScreen<?>) (Object) this, mouseX, mouseY, delta,
                customBankOverlayActive);
    }

    @Unique
    private boolean isCustomBankOverlayReplacingVanilla() {
        return WynnExtrasConfig.INSTANCE.toggleBankOverlay
                && currentOverlayType != BankOverlayType.NONE;
    }

    @Unique
    private boolean isShoppingListBankOverlayPlacementMode() {
        if (!WynnExtrasConfig.INSTANCE.toggleBankOverlay) {
            return false;
        }
        if (currentOverlayType != BankOverlayType.NONE) {
            return true;
        }
        return ShoppingListMenuLauncherButton.isBankLikeMenu(WynncraftMenuService.currentType());
    }

    @Unique
    private ShoppingListScreenContext currentShoppingListScreenContext(boolean customBankOverlayActive) {
        return ShoppingListScreenContext.detect((HandledScreen<?>) (Object) this, customBankOverlayActive);
    }

    @Unique
    private void configureShoppingListMenuExtension(ShoppingListMenuExtension extension, boolean customBankOverlayActive) {
        ShoppingListScreenContext context = currentShoppingListScreenContext(customBankOverlayActive);
        extension.setScreenContext(context);
        extension.setPlacementContext(context.placementContext());
    }

    @Unique
    private void renderWynntilsBankPageJumpButtons(DrawContext context, int mouseX, int mouseY, float delta, HandledScreen<?> screen) {
        if (!BankOverlay2.shouldShowWynntilsPageJumpButtons()) return;

        for (Element child : screen.children()) {
            if (isPersonalStorageUtilitiesWidget(child) && child instanceof Drawable widget) {
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(0, -WYNNTILS_BANK_WIDGET_Y_OFFSET);
                widget.render(context, mouseX, mouseY + WYNNTILS_BANK_WIDGET_Y_OFFSET, delta);
                context.getMatrices().popMatrix();
            }
        }
    }

    @Unique
    private boolean handleWynntilsBankPageJumpButtonClick(Click click, boolean doubleClick, HandledScreen<?> screen) {
        if (!BankOverlay2.shouldShowWynntilsPageJumpButtons()) return false;

        double mouseX = click.x();
        double mouseY = click.y();
        double translatedMouseY = mouseY + WYNNTILS_BANK_WIDGET_Y_OFFSET;

        for (Element child : screen.children()) {
            if (!isPersonalStorageUtilitiesWidget(child)) continue;

            if (child.isMouseOver(mouseX, translatedMouseY)) {
                BankOverlay2.saveActivePageSnapshot();
                return child.mouseClicked(new Click(mouseX, translatedMouseY, click.buttonInfo()), doubleClick);
            }

            if (child.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }

        return false;
    }

    @Unique
    private void renderCharacterSelectionHighlight(DrawContext context, HandledScreen<?> screen) {
        // Only in character selection menu
        if (!WynncraftMenuService.isCurrent(MenuType.CLASS_SELECTION)) return;

        String targetName = julianh06.wynnextras.features.bankoverlay.BankOverlay2.getTargetCharacterNameForClassMenu();
        int targetLevel = julianh06.wynnextras.features.bankoverlay.BankOverlay2.getTargetCharacterLevelForClassMenu();
        if (targetName == null || targetName.isEmpty()) return;

        ScreenHandler handler = screen.getScreenHandler();

        // Find exact match (count to ensure uniqueness for auto-click)
        int matchCount = 0;
        Slot matchSlot = null;
        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;
            if (ClassSelectionOverlay.matchesCrossClassTarget(stack, targetName, targetLevel)) {
                matchCount++;
                matchSlot = slot;
            }
        }

        // Highlight all matches
        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;
            if (!ClassSelectionOverlay.matchesCrossClassTarget(stack, targetName, targetLevel)) continue;
            int slotX = slot.x + this.x;
            int slotY = slot.y + this.y;
            context.fill(slotX - 2, slotY - 2, slotX + 18, slotY, 0xFFFFAA00);
            context.fill(slotX - 2, slotY + 16, slotX + 18, slotY + 18, 0xFFFFAA00);
            context.fill(slotX - 2, slotY, slotX, slotY + 16, 0xFFFFAA00);
            context.fill(slotX + 16, slotY, slotX + 18, slotY + 16, 0xFFFFAA00);
            context.drawText(MinecraftClient.getInstance().textRenderer,
                    "\u00a7e\u25c0 " + targetName,
                    slotX - 10, slotY - 12, 0xFFFFAA00, true);
        }

        // Auto-click if exactly one match \u2014 clear targets first to prevent re-queuing
        if (matchCount == 1 && matchSlot != null) {
            julianh06.wynnextras.features.bankoverlay.BankOverlay2.clearTargetCharacterForClassMenu();
            final int slotId = matchSlot.id;
            julianh06.wynnextras.utils.TickScheduler.runAfterTicks(3, () -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.interactionManager != null && mc.player != null && mc.player.currentScreenHandler != null) {
                    ScreenHandler liveHandler = mc.player.currentScreenHandler;
                    if (slotId >= 0 && slotId < liveHandler.slots.size()) {
                        mc.interactionManager.clickSlot(liveHandler.syncId, slotId, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
                    }
                }
            });
        }
    }



    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(Click click, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (identifierCaseOpeningOverlay != null && identifierCaseOpeningOverlay.isReplacingMenu()) {
            identifierCaseOpeningOverlay.mouseClicked(mouseX, mouseY, button);
            cir.setReturnValue(true);
            return;
        }

        if (!((Object) this instanceof InventoryScreen) && ItemComponentsDebugOverlay.mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
            return;
        }

        // Encounter Selection overlay (intercept before anything else so vanilla slots aren't touched)
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        ShoppingListTradeMarketPurchaseService.handleAmountSlotClick(self, focusedSlot, button);
        if (julianh06.wynnextras.features.qol.EncounterOverlay.handleClick(mouseX, mouseY, self)) {
            cir.setReturnValue(true);
            return;
        }

        if (powderCombineHelperOverlay != null && WynnExtrasConfig.INSTANCE.powderCombineHelper
                && PowderCombineHelperOverlay.isSupportedScreen()
                && powderCombineHelperOverlay.mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
            return;
        }

        // Bag overlay sort-mode toggle (top-right clickable label)
        if (BankOverlay2.handleSortToggleClick(mouseX, mouseY)) {
            cir.setReturnValue(true);
            return;
        }

        // Vanilla mode toggle click (shown when in vanilla mode on class selection screens)
        if (ClassSelectionOverlay.handleVanillaToggleClick(mouseX, mouseY, self)) {
            cir.setReturnValue(true);
            return;
        }

        if (handleWynntilsBankPageJumpButtonClick(click, doubleClick, self)) {
            cir.setReturnValue(true);
            return;
        }

        // Class Selection Overlay click handling
        if (classSelectionOverlay != null) {
            classSelectionOverlay.mouseClicked(mouseX, mouseY, button);
            cir.setReturnValue(true);
            return;
        }
        // Quick Repair button click
        if (quickRepairOverlay != null && quickRepairOverlay.mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
            return;
        }

        if (!(self instanceof InventoryScreen)) {
            boolean shoppingListBankOverlayPlacementMode = isShoppingListBankOverlayPlacementMode();
            if (ensureShoppingListMenuLauncherButton().mouseClicked(mouseX, mouseY, button, self, shoppingListBankOverlayPlacementMode)) {
                cir.setReturnValue(true);
                return;
            }

            ShoppingListMenuExtension shoppingListExtension = ensureShoppingListMenuExtension();
            if (shoppingListExtension != null) {
                configureShoppingListMenuExtension(shoppingListExtension, shoppingListBankOverlayPlacementMode);
            }
            if (shoppingListExtension != null && shoppingListExtension.mouseClicked(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                return;
            }
        }

        // Trade Market Comparison Panel click handling
        if (TradeMarketComparisonPanel.handleClick(mouseX, mouseY, button, 1)) {
            cir.setReturnValue(true);
            return;
        }

        // Trade Market Overlay click handling
        if (TradeMarketOverlay.handleClick(mouseX, mouseY, button, 1)) {
            cir.setReturnValue(true);
            return;
        }

        if(WynnExtrasConfig.INSTANCE.sourceOfTruthToggle) {
            if (identifierOverlay != null && WynncraftMenuService.isCurrent(MenuType.ITEM_IDENTIFIER)) {
                identifierOverlay.mouseClicked(mouseX, mouseY, button);
            }
        }

        if(WynnExtrasConfig.INSTANCE.showLootpoolButtonInPartyFinder &&
                MinecraftClient.getInstance().currentScreen != null && MinecraftClient.getInstance().currentScreen.getTitle() != null &&
                (MinecraftClient.getInstance().currentScreen.getTitle().getString().equals("\uDAFF\uDFE4\uE03E") ||
                MinecraftClient.getInstance().currentScreen.getTitle().getString().equals("\uDAFF\uDFE4\uE03F") ||
                MinecraftClient.getInstance().currentScreen.getTitle().getString().equals("\uDAFF\uDFE1\uE00C"))) {
            if (partyFinderOpenLootpoolOverlay != null) {
                partyFinderOpenLootpoolOverlay.mouseClicked(mouseX, mouseY, button);
            }
        }

        if (craftingHelperOverlay != null && WynncraftMenuService.isCurrent(MenuType.CRAFTING_STATION) && WynnExtrasConfig.INSTANCE.craftingHelperOverlay) {
            craftingHelperOverlay.mouseClicked(mouseX, mouseY, button);
        }

        if(bankOverlay != null) {
            boolean handledByBankOverlay = bankOverlay.mouseClicked(mouseX, mouseY, button, doubleClick);

            if (handledByBankOverlay) {
                cir.setReturnValue(true);
                return;
            }
        }

        if (WynncraftMenuService.isCurrent(MenuType.CHARACTER_INFO)
                && WynnExtrasConfig.INSTANCE.skillpointHelper
                && CompassMenuOverlay.isSelectingWeapon()) {
            if (compassMenuOverlay != null) {
                compassMenuOverlay.mouseClicked(mouseX, mouseY, button);
            }
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        if (compassMenuOverlay != null
                && WynncraftMenuService.isCurrent(MenuType.CHARACTER_INFO)
                && WynnExtrasConfig.INSTANCE.skillpointHelper
                && !CompassMenuOverlay.isSelectingWeapon()) {
            compassMenuOverlay.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Unique
    private static boolean isPersonalStorageUtilitiesWidget(Element element) {
        return WynntilsBankAdapter.isPersonalStorageWidget(element);
    }




    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(Click click, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (identifierCaseOpeningOverlay != null && identifierCaseOpeningOverlay.isReplacingMenu()) {
            cir.setReturnValue(true);
            return;
        }

        if (!((Object) this instanceof InventoryScreen) && ItemComponentsDebugOverlay.mouseReleased(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
            return;
        }

        boolean shoppingListBankOverlayPlacementMode = isShoppingListBankOverlayPlacementMode();
        if (!((Object) this instanceof InventoryScreen)) {
            ShoppingListScreenContext releaseShoppingListContext = currentShoppingListScreenContext(shoppingListBankOverlayPlacementMode);
            if (ensureShoppingListMenuLauncherButton().mouseReleased(button, releaseShoppingListContext)) {
                cir.setReturnValue(true);
                return;
            }
        }

        // Class Selection Overlay release (for drag-to-reorder)
        if (classSelectionOverlay != null) {
            classSelectionOverlay.onMouseReleased(mouseX, mouseY, button);
            cir.setReturnValue(true);
            return;
        }

        // Trade Market Comparison Panel release
        if (TradeMarketComparisonPanel.handleClick(mouseX, mouseY, button, 0)) {
            cir.setReturnValue(true);
            return;
        }

        // Trade Market Overlay release
        if (TradeMarketOverlay.handleClick(mouseX, mouseY, button, 0)) {
            cir.setReturnValue(true);
            return;
        }

        if(bankOverlay != null) {
            bankOverlay.mouseReleased(mouseX, mouseY, button);

            if (WynnExtrasConfig.INSTANCE.toggleBankOverlay) {
                if (currentOverlayType != BankOverlayType.NONE) {
                    cir.cancel();
                }
            }
        }

        if(craftingHelperOverlay != null && WynnExtrasConfig.INSTANCE.craftingHelperOverlay) {
            craftingHelperOverlay.mouseReleased(mouseX, mouseY, button);
        }

        if (!((Object) this instanceof InventoryScreen)) {
            ShoppingListMenuExtension shoppingListExtension = ensureShoppingListMenuExtension();
            if (shoppingListExtension != null) {
                configureShoppingListMenuExtension(shoppingListExtension, shoppingListBankOverlayPlacementMode);
            }
            if (shoppingListExtension != null && shoppingListExtension.mouseReleased(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(Click click, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (identifierCaseOpeningOverlay != null && identifierCaseOpeningOverlay.isReplacingMenu()) {
            cir.setReturnValue(true);
            return;
        }

        if (!((Object) this instanceof InventoryScreen) && ItemComponentsDebugOverlay.mouseDragged(mouseX, mouseY)) {
            cir.setReturnValue(true);
            return;
        }

        // Class Selection Overlay dragging (for drag-to-reorder)
        if (classSelectionOverlay != null) {
            classSelectionOverlay.onMouseDragged(mouseX, mouseY, click.button(), deltaX, deltaY);
            cir.setReturnValue(true);
            return;
        }

        // Handle Trade Market Comparison Panel dragging
        if (TradeMarketComparisonPanel.isDragging()) {
            TradeMarketComparisonPanel.handleMouseMove(mouseX, mouseY);
        }

        // Handle Trade Market Overlay dragging
        if (TradeMarketOverlay.isDragging()) {
            TradeMarketOverlay.handleMouseMove(mouseX, mouseY);
        }

        boolean shoppingListBankOverlayPlacementMode = isShoppingListBankOverlayPlacementMode();
        if (!((Object) this instanceof InventoryScreen)) {
            ShoppingListScreenContext dragShoppingListContext = currentShoppingListScreenContext(shoppingListBankOverlayPlacementMode);
            if (ensureShoppingListMenuLauncherButton().mouseDragged(mouseX, mouseY, click.button(), dragShoppingListContext)) {
                cir.setReturnValue(true);
                return;
            }
        }

        if(bankOverlay != null) {
            if (bankOverlay.mouseDragged(mouseX, mouseY, click.button(), deltaX, deltaY)) {
                cir.setReturnValue(true);
            }
        }

        if (!((Object) this instanceof InventoryScreen)) {
            ShoppingListMenuExtension shoppingListExtension = ensureShoppingListMenuExtension();
            if (shoppingListExtension != null) {
                configureShoppingListMenuExtension(shoppingListExtension, shoppingListBankOverlayPlacementMode);
            }
            if (shoppingListExtension != null && shoppingListExtension.mouseDragged(mouseX, mouseY, click.button(), deltaX, deltaY)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (identifierCaseOpeningOverlay != null && identifierCaseOpeningOverlay.isReplacingMenu()) {
            cir.setReturnValue(true);
            return;
        }
        if (!((Object) this instanceof InventoryScreen) && ItemComponentsDebugOverlay.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            cir.setReturnValue(true);
            return;
        }
        if (!((Object) this instanceof InventoryScreen)) {
            ShoppingListMenuExtension shoppingListExtension = ensureShoppingListMenuExtension();
            if (shoppingListExtension != null) {
                configureShoppingListMenuExtension(shoppingListExtension, isShoppingListBankOverlayPlacementMode());
            }
            if (shoppingListExtension != null && shoppingListExtension.mouseScrolled(mouseX, mouseY, verticalAmount)) {
                cir.setReturnValue(true);
                return;
            }
        }
        if (BankOverlay2.handleMouseScrolled(verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isClickOutsideBounds", at = @At("HEAD"), cancellable = true)
    private void onIsClickOutsideBounds(double mouseX, double mouseY, int left, int top, CallbackInfoReturnable<Boolean> cir) {
        if (classSelectionOverlay != null) {
            cir.setReturnValue(false);
            return;
        }
        if(WynnExtrasConfig.INSTANCE.toggleBankOverlay) {
            if (currentOverlayType != BankOverlayType.NONE) {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    public void onInit(CallbackInfo ci) {
        heldItem = Items.AIR.getDefaultStack();
        craftingHelperOverlay = null;
        powderCombineHelperOverlay = null;
        classSelectionOverlay = null;
        shoppingListMenuExtension = null;
        shoppingListMenuLauncherButton = null;
        BankOverlaySlotBridge.restoreAll();

        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        if (!(self instanceof InventoryScreen)) {
            ScreenMouseEvents.allowMouseScroll(self).register((screen, mouseX, mouseY, horizontalAmount, verticalAmount) -> {
                ShoppingListMenuExtension extension = ensureShoppingListMenuExtension();
                if (extension == null) {
                    return true;
                }
                configureShoppingListMenuExtension(extension, isShoppingListBankOverlayPlacementMode());
                return !extension.mouseScrolled(mouseX, mouseY, verticalAmount);
            });
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void onClose(CallbackInfo ci) {
        boolean wasWaiting = shouldWait;
        boolean wasBankTypeSwitching = BankOverlay2.isBankTypeSwitchInProgress();
        BankOverlay2.resetInteractionBlockers();
        BankOverlaySlotBridge.restoreAll();
        craftingHelperOverlay = null;
        powderCombineHelperOverlay = null;
        PowderCombineHelperOverlay.onHandledScreenClosed();
        classSelectionOverlay = null;
        shoppingListMenuExtension = null;
        shoppingListMenuLauncherButton = null;
        if (!((Object) this instanceof InventoryScreen)) {
            ItemComponentsDebugOverlay.reset();
        }

        // Clear Trade Market Comparison on close
        TradeMarketComparisonPanel.clearAllPanels();

        if (!WynnExtrasConfig.INSTANCE.toggleBankOverlay) {
            if (BankOverlay2.isCurrentContainerBank()) {
                BankOverlay2.cacheCurrentBankPageIfPossible();
                BankOverlay2.saveCurrentBankData();
            }
            return;
        }
        bankOverlay = null;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        ScreenHandler currScreenHandler = MinecraftUtils.containerMenu();
        if (currScreenHandler == null) {
            return;
        }

        Screen currScreen = MinecraftUtils.mc().currentScreen;
        if (currScreen == null) {
            return;
        }

        if (currentOverlayType != BankOverlayType.NONE) {
            heldItem = Items.AIR.getDefaultStack();

            if (Pages != null && activeInv != -1 && !wasWaiting && !BankOverlay.isCharacterBankMissingCharacterId()
                    && !wasBankTypeSwitching) {
                List<ItemStack> stacks = new ArrayList<>();
                Inventory playerInv = client.player.getInventory();
                for (Slot slot : currScreenHandler.slots) {
                    if (slot.inventory == playerInv) continue;
                    stacks.add(slot.getStack().copy());
                    if (stacks.size() >= 45) break;
                }
                if (stacks.size() < 45) {
                    stacks.clear();
                    for (int j = 0; j < Math.min(45, activeInvSlots.size()); j++) {
                        stacks.add(activeInvSlots.get(j).getStack().copy());
                    }
                }
                if (stacks.size() >= 45) {
                    Pages.getBankPages().put(activeInv, stacks);
                    Pages.saveAsyncDebounced();
                }
            }

            activeInvSlots.clear();
            activeInv = -1;
            annotationCache.clear();
        }
        currentOverlayType = BankOverlayType.NONE;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void keyPressedPre(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();

        if (identifierCaseOpeningOverlay != null && identifierCaseOpeningOverlay.isReplacingMenu()) {
            identifierCaseOpeningOverlay.keyPressed(keyCode, scanCode, modifiers);
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        // Block all key presses when a class selection text input is active (handled via CharInputEvent/KeyInputEvent)
        if (ClassSelectionOverlay.isTextInputActive()) {
            ClassSelectionOverlay.handleScreenKeyInput(keyCode, scanCode, modifiers);
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        if (ItemComponentsDebugOverlay.handleKeyPressed(keyCode, modifiers)) {
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        // F1 key in Trade Market for item comparison
        if (keyCode == GLFW.GLFW_KEY_F1 && TradeMarketComparisonPanel.isInTradeMarket()) {
            if (TradeMarketComparisonPanel.handleF1Press(focusedSlot)) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        // F2 key in Trade Market to toggle scale background
        if (keyCode == GLFW.GLFW_KEY_F2 && TradeMarketComparisonPanel.isInTradeMarket()) {
            if (TradeMarketComparisonPanel.handleF2Press()) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        if (!((Object) this instanceof InventoryScreen)
                && WynnExtrasConfig.INSTANCE.debugItemComponentsKey != GLFW.GLFW_KEY_UNKNOWN
                && keyCode == WynnExtrasConfig.INSTANCE.debugItemComponentsKey) {
            if (ItemComponentsDebugOverlay.openHoveredStack((HandledScreen<?>) (Object) this)) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        if (!((Object) this instanceof InventoryScreen)) {
            if (ShoppingListMenuExtension.isToggleKey(keyCode)) {
                ShoppingListMenuExtension.toggleFromHotkey(currentShoppingListScreenContext(isShoppingListBankOverlayPlacementMode()));
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }

            ShoppingListMenuExtension shoppingListExtension = ensureShoppingListMenuExtension();
            if (shoppingListExtension != null) {
                configureShoppingListMenuExtension(shoppingListExtension, isShoppingListBankOverlayPlacementMode());
            }
            if (shoppingListExtension != null && shoppingListExtension.keyPressed(keyCode, scanCode, modifiers)) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        if(bankOverlay != null) {
            if (WynnExtrasConfig.INSTANCE.toggleBankOverlay) {
                // Offhand swap (F key) in custom bank overlay
                Slot touchHoveredSlot = bankOverlay.getTouchHoveredSlot();
                if (touchHoveredSlot != null) {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.options.swapHandsKey.matchesKey(new KeyInput(keyCode, scanCode, modifiers))) {
                        ScreenHandler handler = MinecraftUtils.containerMenu();
                        if (handler != null) {
                            int slotIndex = touchHoveredSlot.id;
                            mc.interactionManager.clickSlot(handler.syncId, slotIndex, 40, net.minecraft.screen.slot.SlotActionType.SWAP, mc.player);
                            cir.setReturnValue(true);
                            cir.cancel();
                            return;
                        }
                    }
                }

                InventoryKeyPressEvent event = new InventoryKeyPressEvent(keyCode, scanCode, modifiers, touchHoveredSlot);
                event.post();

                if (event.isCanceled()) {
                    cir.setReturnValue(true);
                    cir.cancel();
                }
            }
        }
    }
}
