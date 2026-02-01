package julianh06.wynnextras.mixin.BankOverlay;

import com.wynntils.core.components.Models;
import com.wynntils.models.containers.Container;
import com.wynntils.models.containers.containers.CharacterSelectionContainer;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import com.wynntils.models.containers.containers.ItemIdentifierContainer;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.InventoryKeyPressEvent;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.crafting.CraftingHelperOverlay;
import julianh06.wynnextras.features.inventory.*;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.misc.IdentifierOverlay;
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

    @Unique private IdentifierOverlay identifierOverlay;

    @Unique
    private static boolean isControlDown() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return false;
        long handle = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
               GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    @Unique private CraftingHelperOverlay craftingHelperOverlay;

    @Inject(method = "renderBackground", at = @At(value = "HEAD"), cancellable = true)
    private void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci){
        if (WynnExtrasConfig.INSTANCE.toggleBankOverlay && currentOverlayType != BankOverlayType.NONE) {
            ci.cancel();
        }
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

        // Trade Market Overlay (Your Trades value display)
        TradeMarketOverlay.renderOnScreen(context);

        // Trade Market Comparison Panel
        TradeMarketComparisonPanel.render(context);

        // Character selection highlighting (when clicking cross-class bank page)
        renderCharacterSelectionHighlight(context, (HandledScreen<?>) (Object) this);
    }

    @Unique
    private void renderCharacterSelectionHighlight(DrawContext context, HandledScreen<?> screen) {
        // Only render if we have a target character
        if (julianh06.wynnextras.features.bankoverlay.BankOverlay2.targetCharacterNameForClassMenu == null) return;

        // Only in character selection menu
        Container container = Models.Container.getCurrentContainer();
        if (!(container instanceof CharacterSelectionContainer)) {
            // Clear target when leaving character selection
            return;
        }

        // Search through slots to find matching character
        ScreenHandler handler = screen.getScreenHandler();
        String targetName = julianh06.wynnextras.features.bankoverlay.BankOverlay2.targetCharacterNameForClassMenu;

        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;

            // Check if this item's name/lore matches our target character
            String itemName = stack.getName().getString().replaceAll("§[0-9a-fk-or]", "");

            // Characters in /class menu show class name in item name
            // Match by checking if target name starts with the class name in the item
            if (targetName.toLowerCase().startsWith(itemName.toLowerCase())) {
                // Draw highlight around this slot
                int slotX = slot.x + this.x;
                int slotY = slot.y + this.y;

                // Draw yellow/gold border
                context.fill(slotX - 2, slotY - 2, slotX + 18, slotY, 0xFFFFAA00); // top
                context.fill(slotX - 2, slotY + 16, slotX + 18, slotY + 18, 0xFFFFAA00); // bottom
                context.fill(slotX - 2, slotY, slotX, slotY + 16, 0xFFFFAA00); // left
                context.fill(slotX + 16, slotY, slotX + 18, slotY + 16, 0xFFFFAA00); // right

                // Draw label above
                context.drawText(MinecraftClient.getInstance().textRenderer,
                        "§e◀ " + targetName,
                        slotX - 10, slotY - 12, 0xFFFFAA00, true);
            }
        }
    }



    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(Click click, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

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




    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(Click click, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

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

        if(craftingHelperOverlay != null && WynnExtrasConfig.INSTANCE.craftingHelperOverlay && MinecraftClient.getInstance().options.getGuiScale().getValue() != 1) {
            craftingHelperOverlay.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void onMouseDragged(Click click, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = click.x();
        double mouseY = click.y();

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
            for (Slot slot : activeInvSlots) {
                stacks.add(slot.getStack());
            }
            if(activeInv != -1) {
                Pages.BankPages.put(activeInv, stacks);
            }
            activeInvSlots.clear();
            activeInv = 1;
            annotationCache.clear();
            Pages.save();
        }
        currentOverlayType = BankOverlayType.NONE;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void keyPressedPre(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();

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