package julianh06.wynnextras.features.misc;

import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.wynntils.utils.wynn.ContainerUtils.clickOnSlot;

public class FastRequeue {
    static boolean inRaidChest = false;

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

            //atomicboolean instead of normal because it can be final (needed for lambda) while still being able to be changed
            final AtomicBoolean opened = new AtomicBoolean(false);
            ClientTickEvents.END_CLIENT_TICK.register(clientt -> {
                if(opened.get()) return;
                if(McUtils.player() == null) { return; }
                if(clientt.currentScreen == null) { return; }

                ScreenHandler menu = McUtils.containerMenu();
                if(menu == null) return;
                if(menu.slots.size() < 50) return;

                Slot slot = menu.getSlot(49);

                if(slot == null) return;
                if(slot.getStack() == null) return;
                if(slot.getStack().getCustomName() == null) return;
                if(slot.getStack().getCustomName().getString().contains("Queue")) {
                    clickOnSlot(49, McUtils.containerMenu().syncId, 0, McUtils.containerMenu().getStacks());
                    opened.set(true);
                }
            });
        }
    }
}

