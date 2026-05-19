package julianh06.wynnextras.features.misc;

import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import static com.wynntils.utils.wynn.ContainerUtils.clickOnSlot;

public class FastRequeue {
    static boolean inRaidChest = false;
    private static boolean waitingForQueue = false;

    public static void registerFastRequeue() {
        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if(client.player == null || client.world == null) { return; }

            ScreenHandler currScreenHandler = McUtils.containerMenu();
            if(currScreenHandler == null) { return; }

            Screen currScreen = McUtils.mc().currentScreen;
            if(currScreen == null) { return; }

            String InventoryTitle = currScreen.getTitle().getString();
            inRaidChest = InventoryTitle.equals("\uDAFF\uDFEA\uE00E");

            // Handle queue click in the same listener instead of registering new ones
            if (waitingForQueue) {
                if (McUtils.player() == null || client.currentScreen == null) return;

                ScreenHandler menu = McUtils.containerMenu();
                if (menu == null || menu.slots.size() < 50) return;

                Slot slot = menu.getSlot(49);
                if (slot == null || slot.getStack() == null || slot.getStack().getCustomName() == null) return;

                if (slot.getStack().getCustomName().getString().contains("Queue")) {
                    clickOnSlot(49, McUtils.containerMenu().syncId, 0, McUtils.containerMenu().getStacks());
                    waitingForQueue = false;
                }
            }
        });
    }

    public static void notifyClick() {
        if(inRaidChest) {
            MinecraftClient client = MinecraftClient.getInstance();
            if(client.player == null || client.world == null || client.currentScreen == null || client.getNetworkHandler() == null)
            { return; }
            ScreenHandler currScreenHandler = McUtils.containerMenu();
            if(currScreenHandler == null) { return; }
            McUtils.sendChat("/partyfinder");
            waitingForQueue = true;
        }
    }
}
