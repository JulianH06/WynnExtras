package julianh06.wynnextras.mixin;

import com.wynntils.core.components.Models;
import com.wynntils.models.containers.Container;
import com.wynntils.models.containers.containers.personal.*;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import julianh06.wynnextras.features.inventory.data.AccountBankData;
import julianh06.wynnextras.features.inventory.data.BookshelfData;
import julianh06.wynnextras.features.inventory.data.CharacterBankData;
import julianh06.wynnextras.features.inventory.data.MiscBucketData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class InventoryScreenMixin {
    @Unique
    private static WynnExtrasConfig config;

    @Unique
    private static int normalGUIScale = -1;

    @Inject(method = "renderInGameBackground", at = @At("HEAD"), cancellable = true)
    public void renderInGameBackground(DrawContext context, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        ScreenHandler currScreenHandler = McUtils.containerMenu();
        if (currScreenHandler == null) return;

        if (config == null) config = SimpleConfig.getInstance(WynnExtrasConfig.class);

        if(config.differentGUIScale) {
            if(normalGUIScale == -1) {
                normalGUIScale = MinecraftClient.getInstance().options.getGuiScale().getValue();
            }
            MinecraftClient.getInstance().options.getGuiScale().setValue(config.customGUIScale);
        }

        if(!config.toggleBankOverlay) {
            Container container = Models.Container.getCurrentContainer();
            if (container instanceof AccountBankContainer ||
                container instanceof CharacterBankContainer ||
                container instanceof BookshelfContainer ||
                container instanceof MiscBucketContainer
            ) {
                BankOverlay.currentOverlayType = BankOverlayType.NONE;
                BankOverlay.currentData = null;
                ci.cancel();
            }
            return;
        }

        BankOverlay.updateOverlayType();

        if(BankOverlay.expectedOverlayType == null) {
            BankOverlay.expectedOverlayType = BankOverlay.currentOverlayType;
        }

//        System.out.println("EXPECTED: " + BankOverlay.expectedOverlayType + " CURRENT: " + BankOverlay.currentOverlayType);
//
//        if(BankOverlay.expectedOverlayType != null && BankOverlay.expectedOverlayType != BankOverlay.currentOverlayType) return;
//
//        System.out.println("EXPECTED EQUALS CURRENT");

        if (BankOverlay.currentOverlayType != BankOverlayType.NONE) {
            ci.cancel();

            Inventory playerInv = client.player.getInventory();
            BankOverlay.playerInvSlots.clear();
            BankOverlay.activeInvSlots.clear();

            for (Slot slot : currScreenHandler.slots) {
                if (slot.inventory == playerInv) {
                    BankOverlay.playerInvSlots.add(slot);
                } else {
                    BankOverlay.activeInvSlots.add(slot);
                }
            }

            WynnExtras.testInv = currScreenHandler.slots;
            WynnExtras.testInvSize = currScreenHandler.slots.size() - 36;
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void close(CallbackInfo ci) {
        if(config == null) config = SimpleConfig.getInstance(WynnExtrasConfig.class);
        if(config.differentGUIScale && normalGUIScale != -1) {
            MinecraftClient.getInstance().options.getGuiScale().setValue(normalGUIScale);
            normalGUIScale = -1;
        }
    }
}