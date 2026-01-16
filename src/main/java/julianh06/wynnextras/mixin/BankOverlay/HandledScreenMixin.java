package julianh06.wynnextras.mixin.BankOverlay;

import com.wynntils.core.components.Models;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import com.wynntils.models.containers.containers.ItemIdentifierContainer;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.InventoryKeyPressEvent;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.crafting.CraftingHelperOverlay;
import julianh06.wynnextras.features.inventory.*;
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

    @Unique private BankOverlay2 bankOverlay;

    @Unique private IdentifierOverlay identifierOverlay;

    @Unique private CraftingHelperOverlay craftingHelperOverlay;

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

        if(SimpleConfig.getInstance(WynnExtrasConfig.class).sourceOfTruthToggle) {
            if (identifierOverlay == null) {
                identifierOverlay = new IdentifierOverlay();
            }

            identifierOverlay.render(context, mouseX, mouseY, delta);
        }

        if(SimpleConfig.getInstance(WynnExtrasConfig.class).craftingHelperOverlay && MinecraftClient.getInstance().options.getGuiScale().getValue() != 1) {
            if (craftingHelperOverlay == null) {
                craftingHelperOverlay = new CraftingHelperOverlay();
            }

            craftingHelperOverlay.render(context, mouseX, mouseY, delta);
        }
    }



    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(Click click, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if(SimpleConfig.getInstance(WynnExtrasConfig.class).sourceOfTruthToggle) {
            if (identifierOverlay != null && Models.Container.getCurrentContainer() instanceof ItemIdentifierContainer) {
                identifierOverlay.mouseClicked(mouseX, mouseY, button);
            }
        }

        if (craftingHelperOverlay != null && Models.Container.getCurrentContainer() instanceof CraftingStationContainer && SimpleConfig.getInstance(WynnExtrasConfig.class).craftingHelperOverlay && MinecraftClient.getInstance().options.getGuiScale().getValue() != 1) {
            craftingHelperOverlay.mouseClicked(mouseX, mouseY, button);
        }

        if(bankOverlay != null) {
            bankOverlay.mouseClicked(mouseX, mouseY, button);

            if (SimpleConfig.getInstance(WynnExtrasConfig.class).toggleBankOverlay) {
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

        if(bankOverlay != null) {
            bankOverlay.mouseReleased(mouseX, mouseY, button);

            if (SimpleConfig.getInstance(WynnExtrasConfig.class).toggleBankOverlay) {
                if (currentOverlayType != BankOverlayType.NONE) {
                    cir.cancel();
                }
            }
        }

        if(craftingHelperOverlay != null && SimpleConfig.getInstance(WynnExtrasConfig.class).craftingHelperOverlay && MinecraftClient.getInstance().options.getGuiScale().getValue() != 1) {
            craftingHelperOverlay.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Inject(method = "isClickOutsideBounds", at = @At("HEAD"), cancellable = true)
    private void onIsClickOutsideBounds(double mouseX, double mouseY, int left, int top, CallbackInfoReturnable<Boolean> cir) {
        if(SimpleConfig.getInstance(WynnExtrasConfig.class).toggleBankOverlay) {
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

        if(!SimpleConfig.getInstance(WynnExtrasConfig.class).toggleBankOverlay) return;
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

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void keyPressedPre(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();

        if(bankOverlay != null) {
            if (SimpleConfig.getInstance(WynnExtrasConfig.class).toggleBankOverlay) {
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