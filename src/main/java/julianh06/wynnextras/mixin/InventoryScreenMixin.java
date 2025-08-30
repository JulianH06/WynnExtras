package julianh06.wynnextras.mixin;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.inventory.BankOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class InventoryScreenMixin {
    private static WynnExtrasConfig config;

    boolean isNewInv = true;
    String lastInvName = "";

    @Inject(method = "renderInGameBackground", at = @At("HEAD"), cancellable = true)
    public void renderInGameBackground(DrawContext context, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if(client.player == null || client.world == null) { return; }

        ScreenHandler currScreenHandler = McUtils.containerMenu();
        if(currScreenHandler == null) { return; }

        Screen currScreen = McUtils.mc().currentScreen;
        if(currScreen == null) { return; }

        String InventoryTitle = currScreen.getTitle().getString();
        if(InventoryTitle.isEmpty()) { return; }

        if(config == null) {
            config = SimpleConfig.getInstance(WynnExtrasConfig.class);
        }

        if(!config.toggleBankOverlay) { return; }
//        if(!lastInvName.isEmpty()) {
//            System.out.println("lastinvname: " + lastInvName + " newInvname: " + InventoryTitle);
//            isNewInv = !lastInvName.equals(InventoryTitle);
//        }
//        lastInvName = InventoryTitle;
//        Wynnarsch.testBackgroundWidth = currScreen.width;
//        Wynnarsch.testBackgroundHeight = currScreen.height;
//        System.out.println("isnewInv: " + isNewInv);
//        if(isNewInv) { //the name doesnt change when scrolling through bank pages, needs to be considered later
//            BankOverlay.playerInvSlots.clear();
//            BankOverlay.activeInvSlots.clear();
//            isNewInv = false;
//            for (Slot slot : currScreenHandler.slots) {
//                //System.out.println("Slotname: " + slot.getStack().getItem().getName());
//                if (slot.inventory == MinecraftClient.getInstance().player.getInventory()) {
//                    if (slot.y == 197) {
//                        continue;
//                    }
//                    //System.out.println("Slot.x: " + slot.x + " Slot.y: " + slot.y + " index: " + slot.getIndex());
////                if(slot.y == 139) {
////                    ((SlotAccessor)slot).setY(197);
////                    BankOverlay.playerInvSlots.add(slot);
////                    break;
////                }
////                if (slot.y == 197) {
////                    ((SlotAccessor)slot).setY(139);
////                    BankOverlay.playerInvSlots.add(slot);
////                    break;
////                }
//                    if (!BankOverlay.playerInvSlots.contains(slot)) {
//                        BankOverlay.playerInvSlots.add(slot);
//                    }
//                } else {
//                    if (!BankOverlay.activeInvSlots.contains(slot)) {
//                        //if(BankOverlay.activeInvSlots.size() == 36) { BankOverlay.activeInvSlots.clear(); }
//                        //System.out.println("ADDED TO ACTIVEINV! " + slot.getStack().getName());
//                        BankOverlay.activeInvSlots.add(slot);
//                    }
//                }
//
//            }
//       }

        if(config.toggleBankOverlay && InventoryTitle.equals("\uDAFF\uDFF0\uE00F\uDAFF\uDF68\uF000")) {
            ci.cancel();
        }

        WynnExtras.testInv = currScreenHandler.slots;
        WynnExtras.testInvSize = currScreenHandler.slots.size() - 36;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if(config == null) {
            config = SimpleConfig.getInstance(WynnExtrasConfig.class);
        }

        if(!config.toggleBankOverlay) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        ScreenHandler currScreenHandler = McUtils.containerMenu();
        Screen currScreen = McUtils.mc().currentScreen;

        if (currScreenHandler == null || currScreen == null) {
            return;
        }

        if(currScreenHandler.slots.isEmpty()) {
            return;
        }

        String InventoryTitle = currScreen.getTitle().getString();
        if(InventoryTitle.isEmpty()) {
            return;
        }

        if(InventoryTitle.equals("\uDAFF\uDFF0\uE00F\uDAFF\uDF68\uF000")) {
            ci.cancel();
        } else {
            return;
        }

        Inventory playerInv = MinecraftClient.getInstance().player.getInventory();
        BankOverlay.playerInvSlots.clear();
        BankOverlay.activeInvSlots.clear();
        for (Slot slot : currScreenHandler.slots) {
            if (slot.inventory == playerInv) {
                BankOverlay.playerInvSlots.add(slot);
            } else {
                BankOverlay.activeInvSlots.add(slot);
            }
        }
    }
}


