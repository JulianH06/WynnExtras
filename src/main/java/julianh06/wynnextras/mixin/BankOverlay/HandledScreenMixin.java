package julianh06.wynnextras.mixin.BankOverlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.inventory.*;
import com.wynntils.features.tooltips.TooltipFittingFeature;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemHandler;
import com.wynntils.mc.extension.ItemStackExtension;
import com.wynntils.models.emeralds.type.EmeraldUnits;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.*;
import com.wynntils.models.items.properties.DurableItemProperty;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.TooltipUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.Time;
import com.wynntils.utils.wynn.ContainerUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.InventoryKeyPressEvent;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.inventory.*;
import julianh06.wynnextras.features.inventory.BankOverlayButtons.*;
import julianh06.wynnextras.mixin.Accessor.*;
import julianh06.wynnextras.mixin.Invoker.*;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.overlays.EasyTextInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static julianh06.wynnextras.features.inventory.BankOverlay.*;
import static julianh06.wynnextras.features.inventory.WeightDisplay.currentHoveredStack;
import static julianh06.wynnextras.features.inventory.WeightDisplay.currentHoveredWynnitem;
import static julianh06.wynnextras.features.misc.CustomClassSelection.inClassSelection;

@WEModule
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow public abstract void close();

    @Unique private BankOverlay2 bankOverlay;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderInventory(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if(bankOverlay == null) bankOverlay = new BankOverlay2(ci, (HandledScreen) (Object) this);
        bankOverlay.ci = ci;
        bankOverlay.screen = (HandledScreen) (Object) this;
        bankOverlay.close = close -> {
            close();
            return null;
        };
        bankOverlay.render(context, mouseX, mouseY, delta);
    }



    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if(bankOverlay == null) return;
        if(!SimpleConfig.getInstance(WynnExtrasConfig.class).toggleBankOverlay) return;
//        if(inClassSelection) {
//            cir.cancel();
//            return;
//        }

        if (currentOverlayType != BankOverlayType.NONE) {
            cir.cancel();
        } else {
            return;
        }

        bankOverlay.mouseClicked(mouseX, mouseY, button);

        if (shouldWait) return;

        BankOverlay.activeTextInput = null;


        int playerInvIndex = xFitAmount * yFitAmount - xFitAmount + scrollOffset;
        if(bankOverlay.hoveredInvIndex != playerInvIndex) {
            if (bankOverlay.hoveredInvIndex == currentData.lastPage && activeInv == bankOverlay.hoveredInvIndex - 1) {
                ScreenHandler currScreenHandler = McUtils.containerMenu();
                if (currScreenHandler == null) {
                    return;
                }
                ContainerUtils.clickOnSlot(52, currScreenHandler.syncId, 0, currScreenHandler.getStacks());
                return;
            } else if (bankOverlay.hoveredInvIndex == currentData.lastPage) {
                if(PersonalStorageUtils != null) {
                    BankOverlay.PersonalStorageUtils.jumpToDestination(currentData.lastPage);
                    System.out.println("Jump 1");
                    activeInv = currentData.lastPage - 1;
                }
                bankOverlay.retryLoad();
                return;
            }
        }

        bankOverlay.handleButtonClick(mouseX, mouseY);

        if (bankOverlay.Searchbar.isClickInBounds((int) mouseX, (int) mouseY) != bankOverlay.Searchbar.isActive()) {
            bankOverlay.Searchbar.click();
        }

        bankOverlay.handleNameInputs(mouseX, mouseY);

        if (bankOverlay.hoveredIndex < 0 || bankOverlay.hoveredIndex >= 63) return;

        SlotActionType actionType = bankOverlay.determineActionType(button);
        if (bankOverlay.handleBankSlotClick(bankOverlay.hoveredIndex, button, actionType, cir)) return;
        if (bankOverlay.handlePlayerSlotClick(bankOverlay.hoveredIndex, button, actionType, playerInvIndex, cir)) return;
        if (bankOverlay.handlePageClick()) {
            if (actionType == SlotActionType.QUICK_MOVE) {
                heldItem = Items.AIR.getDefaultStack();
            }
            cir.cancel();
        }
    }




    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (currentOverlayType != BankOverlayType.NONE) {
            cir.cancel();
        }
    }

    @Inject(method = "isClickOutsideBounds", at = @At("HEAD"), cancellable = true)
    private void onIsClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
        if (currentOverlayType != BankOverlayType.NONE) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    public void onInit(CallbackInfo ci) {
        heldItem = Items.AIR.getDefaultStack();
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void onClose(CallbackInfo ci) {
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
            scrollOffset = 0;
            Pages.save();
        }
        currentOverlayType = BankOverlayType.NONE;
    }

    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
    private void keyPressedPre(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if(bankOverlay == null) return;
        InventoryKeyPressEvent event = new InventoryKeyPressEvent(keyCode, scanCode, modifiers, bankOverlay.touchHoveredSlot);
        event.post();

        if (event.isCanceled()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}

//TODO SHOW POSSIBLE THINGS WHEN ITEM IS UNIDENTIFIED
//TODO PAGE NAME INPUT MORE SPACE BETWEEN BORDER AND TEXT