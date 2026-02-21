package julianh06.wynnextras.mixin.BankOverlay;

import com.wynntils.core.components.Models;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import com.wynntils.models.containers.containers.ItemIdentifierContainer;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.InventoryKeyPressEvent;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.crafting.CraftingHelperOverlay;
import julianh06.wynnextras.features.crafting.CraftingResultPreviewer;
import julianh06.wynnextras.features.inventory.*;
import julianh06.wynnextras.features.misc.IdentifierOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
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

    @Unique private BankOverlay2 bankOverlay;

    @Unique private IdentifierOverlay identifierOverlay;

    @Unique private CraftingHelperOverlay craftingHelperOverlay;

    @Inject(method = "render", at = @At("TAIL"), cancellable = true)
    private void renderForeground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Trade Market Overlay (Your Trades value display)
        TradeMarketOverlay.renderOnScreen(context);

        // Trade Market Comparison Panel
        TradeMarketComparisonPanel.render(context);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderInventory(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if(bankOverlay == null) bankOverlay = new BankOverlay2(ci, (HandledScreen<?>) (Object) this);
        bankOverlay.ci = ci;
        bankOverlay.screen = (HandledScreen<?>) (Object) this;
        bankOverlay.close = close -> {
            close();
            return null;
        };
        bankOverlay.render(context, mouseX, mouseY, delta);

        if(WynnExtrasConfig.INSTANCE.sourceOfTruthToggle) {
            if (identifierOverlay == null) {
                identifierOverlay = new IdentifierOverlay();
            }

            identifierOverlay.render(context, mouseX, mouseY, delta);
        }

        if(WynnExtrasConfig.INSTANCE.craftingHelperOverlay && MinecraftClient.getInstance().options.getGuiScale().getValue() != 1) {
            if (craftingHelperOverlay == null) {
                craftingHelperOverlay = new CraftingHelperOverlay();
            }

            craftingHelperOverlay.render(context, mouseX, mouseY, delta);
        }
    }



    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
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

        if (craftingHelperOverlay != null && Models.Container.getCurrentContainer() instanceof CraftingStationContainer && WynnExtrasConfig.INSTANCE.craftingHelperOverlay && MinecraftClient.getInstance().options.getGuiScale().getValue() != 1) {
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
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        // Handle Trade Market Comparison Panel dragging
        if (TradeMarketComparisonPanel.isDragging()) {
            TradeMarketComparisonPanel.handleMouseMove(mouseX, mouseY);
        }

        // Handle Trade Market Overlay dragging
        if (TradeMarketOverlay.isDragging()) {
            TradeMarketOverlay.handleMouseMove(mouseX, mouseY);
        }
    }


    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if(bankOverlay != null) {
            bankOverlay.mouseReleased(mouseX, mouseY, button);

            if (WynnExtrasConfig.INSTANCE.toggleBankOverlay) {
                if (currentOverlayType != BankOverlayType.NONE) {
                    cir.cancel();
                }
            }
        }

        if(craftingHelperOverlay != null && WynnExtrasConfig.INSTANCE.craftingHelperOverlay && MinecraftClient.getInstance().options.getGuiScale().getValue() != 1) {
            craftingHelperOverlay.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Inject(method = "isClickOutsideBounds", at = @At("HEAD"), cancellable = true)
    private void onIsClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
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
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void onClose(CallbackInfo ci) {
        craftingHelperOverlay = null;

        // Clear Trade Market Comparison on close
        TradeMarketComparisonPanel.clearComparison();

        if(!WynnExtrasConfig.INSTANCE.toggleBankOverlay) return;
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

            List<ItemStack> stacks = new ArrayList<>();
            for (Slot slot : BankOverlay.activeInvSlots) {
                stacks.add(slot.getStack());
            }
            if(activeInv != -1) {
                Pages.BankPages.put(activeInv, stacks);
            }
            BankOverlay.activeInvSlots.clear();
            activeInv = 1;
            annotationCache.clear();
            Pages.save();
        }
        currentOverlayType = BankOverlayType.NONE;
    }

    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
    private void keyPressedPre(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        // F1 key in Trade Market for item comparison
        if (keyCode == GLFW.GLFW_KEY_F1 && TradeMarketComparisonPanel.isInTradeMarket()) {
            // If hovering a slot, add/toggle that item
            if (focusedSlot != null) {
                if (TradeMarketComparisonPanel.handleF1Press(focusedSlot)) {
                    cir.setReturnValue(true);
                    cir.cancel();
                    return;
                }
            } else {
                // No slot focused - clear all panels
                if (TradeMarketComparisonPanel.handleF1NoSlot()) {
                    cir.setReturnValue(true);
                    cir.cancel();
                    return;
                }
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
                InventoryKeyPressEvent event = new InventoryKeyPressEvent(keyCode, scanCode, modifiers, bankOverlay.touchHoveredSlot);
                event.post();

                if (event.isCanceled()) {
                    cir.setReturnValue(true);
                    cir.cancel();
                }
            }
        }
    }
}