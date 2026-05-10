package julianh06.wynnextras.mixin.BankOverlay;

import com.wynntils.core.components.Models;
import com.wynntils.models.containers.Container;
import com.wynntils.models.containers.containers.CharacterInfoContainer;
import com.wynntils.models.containers.containers.CharacterSelectionContainer;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import com.wynntils.models.containers.containers.ItemIdentifierContainer;
import com.wynntils.models.containers.containers.personal.AccountBankContainer;
import com.wynntils.models.containers.containers.personal.BookshelfContainer;
import com.wynntils.models.containers.containers.personal.CharacterBankContainer;
import com.wynntils.models.containers.containers.personal.MiscBucketContainer;
import com.wynntils.utils.mc.McUtils;
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
import julianh06.wynnextras.features.misc.IdentifierOverlay;
import julianh06.wynnextras.features.misc.QuickRepair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
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
    @Shadow public abstract void close();

    @Shadow public Slot focusedSlot;

    @Shadow protected int x;
    @Shadow protected int y;

    @Unique private julianh06.wynnextras.features.bankoverlay.BankOverlay2 bankOverlay;
    @Unique private Boolean isBankScreen = null;

    @Unique private IdentifierOverlay identifierOverlay;

    @Unique private PartyFinderOpenLootpoolOverlay partyFinderOpenLootpoolOverlay;

    @Unique private CraftingHelperOverlay craftingHelperOverlay;

    @Unique private ClassSelectionOverlay classSelectionOverlay;

    @Unique private CompassMenuOverlay compassMenuOverlay;

    @Unique private QuickRepair quickRepairOverlay;

    @Inject(method = "renderBackground", at = @At(value = "HEAD"), cancellable = true)
    private void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci){
        if (WynnExtrasConfig.INSTANCE.toggleBankOverlay && currentOverlayType != BankOverlayType.NONE) {
            ci.cancel();
        }
        if (classSelectionOverlay != null) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderInventory(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Encounter Selection Overlay (must render FIRST and cancel vanilla render so chest UI is fully hidden)
        {
            HandledScreen<?> encSelf = (HandledScreen<?>) (Object) this;
            if (julianh06.wynnextras.features.qol.EncounterOverlay.isReadyToRender(encSelf)) {
                julianh06.wynnextras.features.qol.EncounterOverlay.render(context, encSelf, mouseX, mouseY);
                ci.cancel();
                return;
            }
            // Tick the settle state regardless (for non-ready cases).
            julianh06.wynnextras.features.qol.EncounterOverlay.tickSettle(encSelf);
        }
        // Class Selection Overlay
        if (WynnExtrasConfig.INSTANCE.customClassSelectionEnabled && !ClassSelectionOverlay.vanillaMode) {
            HandledScreen<?> self = (HandledScreen<?>) (Object) this;
            String title = self.getTitle().getString();
            if (ClassSelectionOverlay.isClassSelectionScreen(title)) {
                if (classSelectionOverlay == null || classSelectionOverlay.getMode() != ClassSelectionOverlay.ScreenMode.CLASS_SELECTION) {
                    classSelectionOverlay = new ClassSelectionOverlay(self, ClassSelectionOverlay.ScreenMode.CLASS_SELECTION);
                }
                classSelectionOverlay.render(context, mouseX, mouseY, delta);
                ci.cancel();
                return;
            } else if (ClassSelectionOverlay.isClassEditScreen(title)) {
                if (classSelectionOverlay == null || classSelectionOverlay.getMode() != ClassSelectionOverlay.ScreenMode.CLASS_EDIT) {
                    classSelectionOverlay = new ClassSelectionOverlay(self, ClassSelectionOverlay.ScreenMode.CLASS_EDIT);
                }
                classSelectionOverlay.render(context, mouseX, mouseY, delta);
                ci.cancel();
                return;
            } else if (ClassSelectionOverlay.isIconEditScreen(title)) {
                if (classSelectionOverlay == null || classSelectionOverlay.getMode() != ClassSelectionOverlay.ScreenMode.ICON_EDIT) {
                    classSelectionOverlay = new ClassSelectionOverlay(self, ClassSelectionOverlay.ScreenMode.ICON_EDIT);
                }
                classSelectionOverlay.render(context, mouseX, mouseY, delta);
                ci.cancel();
                return;
            } else {
                classSelectionOverlay = null;
            }
        } else {
            classSelectionOverlay = null;
        }

        // Only create BankOverlay2 for bank-type containers to avoid expensive
        // initialization on every GUI open
        if (isBankScreen == null) {
            Container container = Models.Container.getCurrentContainer();
            if (container != null) {
                isBankScreen = container instanceof AccountBankContainer ||
                    container instanceof CharacterBankContainer ||
                    container instanceof BookshelfContainer ||
                    container instanceof MiscBucketContainer;
            }
        }

        if (Boolean.TRUE.equals(isBankScreen) || currentOverlayType != BankOverlayType.NONE) {
            if (bankOverlay == null) bankOverlay = new BankOverlay2(ci, (HandledScreen<?>) (Object) this);
            bankOverlay.updateRenderContext(ci, (HandledScreen<?>) (Object) this, close -> {
                close();
                return null;
            });
            bankOverlay.render(context, mouseX, mouseY, delta);
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
    }

    @Unique
    private void renderCharacterSelectionHighlight(DrawContext context, HandledScreen<?> screen) {
        // Only in character selection menu
        Container container = Models.Container.getCurrentContainer();
        if (!(container instanceof CharacterSelectionContainer)) return;

        String targetName = julianh06.wynnextras.features.bankoverlay.BankOverlay2.getTargetCharacterNameForClassMenu();
        if (targetName == null || targetName.isEmpty()) return;

        ScreenHandler handler = screen.getScreenHandler();

        // Find exact match (count to ensure uniqueness for auto-click)
        int matchCount = 0;
        Slot matchSlot = null;
        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;
            String itemName = stack.getName().getString().replaceAll("\u00a7[0-9a-fk-or]", "");
            if (targetName.equalsIgnoreCase(itemName)) {
                matchCount++;
                matchSlot = slot;
            }
        }

        // Highlight all matches
        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;
            String itemName = stack.getName().getString().replaceAll("\u00a7[0-9a-fk-or]", "");
            if (!targetName.equalsIgnoreCase(itemName)) continue;
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
            final int syncId = handler.syncId;
            final int slotId = matchSlot.id;
            julianh06.wynnextras.utils.TickScheduler.runAfterTicks(3, () -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.interactionManager != null && mc.player != null) {
                    mc.interactionManager.clickSlot(syncId, slotId, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
                }
            });
        }
    }



    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(Click click, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        // Encounter Selection overlay (intercept before anything else so vanilla slots aren't touched)
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        if (julianh06.wynnextras.features.qol.EncounterOverlay.handleClick(mouseX, mouseY, self)) {
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
            if (identifierOverlay != null && Models.Container.getCurrentContainer() instanceof ItemIdentifierContainer) {
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

        if (craftingHelperOverlay != null && Models.Container.getCurrentContainer() instanceof CraftingStationContainer && WynnExtrasConfig.INSTANCE.craftingHelperOverlay) {
            craftingHelperOverlay.mouseClicked(mouseX, mouseY, button);
        }

        if(bankOverlay != null) {
            bankOverlay.mouseClicked(mouseX, mouseY, button);

            if (WynnExtrasConfig.INSTANCE.toggleBankOverlay) {
                if (currentOverlayType != BankOverlayType.NONE) {
                    cir.cancel();
                }
            }
        }

        if (Models.Container.getCurrentContainer() instanceof CharacterInfoContainer
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
                && Models.Container.getCurrentContainer() instanceof CharacterInfoContainer
                && WynnExtrasConfig.INSTANCE.skillpointHelper
                && !CompassMenuOverlay.isSelectingWeapon()) {
            compassMenuOverlay.mouseClicked(mouseX, mouseY, button);
        }
    }




    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(Click click, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

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
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(Click click, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = click.x();
        double mouseY = click.y();

        // Class Selection Overlay dragging (for drag-to-reorder)
        if (classSelectionOverlay != null) {
            classSelectionOverlay.onMouseDragged(mouseX, mouseY);
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
        classSelectionOverlay = null;
        BankOverlaySlotBridge.restoreAll();
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void onClose(CallbackInfo ci) {
        BankOverlaySlotBridge.restoreAll();
        craftingHelperOverlay = null;
        classSelectionOverlay = null;

        // Clear Trade Market Comparison on close
        TradeMarketComparisonPanel.clearAllPanels();

        // Vanilla-mode bank cache persistence: in vanilla mode the drawVanillaBankBagsOverlay
        // hook has been live-updating cached bank pages for the current page while the bank
        // was open. Flush those updates to disk now (the custom-mode branch below already
        // does its own save).
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

        ScreenHandler currScreenHandler = McUtils.containerMenu();
        if (currScreenHandler == null) {
            return;
        }

        Screen currScreen = McUtils.mc().currentScreen;
        if (currScreen == null) {
            return;
        }

        if (currentOverlayType != BankOverlayType.NONE) {
            heldItem = Items.AIR.getDefaultStack();

            if (Pages != null && activeInv != -1 && !shouldWait) {
                List<ItemStack> stacks = new ArrayList<>();
                for (int j = 0; j < Math.min(45, activeInvSlots.size()); j++) {
                    stacks.add(activeInvSlots.get(j).getStack());
                }
                Pages.getBankPages().put(activeInv, stacks);
                Pages.save();
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

        // Block all key presses when nickname input is active (handled via CharInputEvent/KeyInputEvent)
        if (ClassSelectionOverlay.nicknameInputActive) {
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

        if(bankOverlay != null) {
            if (WynnExtrasConfig.INSTANCE.toggleBankOverlay) {
                // Offhand swap (F key) in custom bank overlay
                Slot touchHoveredSlot = bankOverlay.getTouchHoveredSlot();
                if (touchHoveredSlot != null) {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (((julianh06.wynnextras.mixin.Accessor.KeybindingAccessor) mc.options.swapHandsKey).getBoundKey().getCode() == keyCode) {
                        ScreenHandler handler = McUtils.containerMenu();
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
