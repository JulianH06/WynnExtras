package julianh06.wynnextras.features.qol;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

public class TerritoryMenuKey {
    private static boolean openingTerritory = false;
    private static boolean openingBank = false;
    private static long lastAction = 0;
    private static boolean territoryKeyWasDown = false;
    private static boolean bankKeyWasDown = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;

            long window = client.getWindow().getHandle();

            // Territory / Eco menu key
            if (c.territoryMenuKeyEnabled) {
                boolean tdown = GLFW.glfwGetKey(window, c.territoryMenuKey) == GLFW.GLFW_PRESS;
                if (tdown && !territoryKeyWasDown && client.currentScreen == null) {
                    if (client.getNetworkHandler() != null) {
                        client.getNetworkHandler().sendChatCommand("gu manage");
                        openingTerritory = true;
                        openingBank = false;
                        lastAction = System.currentTimeMillis();
                    }
                }
                territoryKeyWasDown = tdown;
            }

            // Guild bank key
            if (c.guildBankKeyEnabled) {
                boolean bdown = GLFW.glfwGetKey(window, c.guildBankKey) == GLFW.GLFW_PRESS;
                if (bdown && !bankKeyWasDown && client.currentScreen == null) {
                    if (client.getNetworkHandler() != null) {
                        client.getNetworkHandler().sendChatCommand("gu manage");
                        openingBank = true;
                        openingTerritory = false;
                        lastAction = System.currentTimeMillis();
                    }
                }
                bankKeyWasDown = bdown;
            }

            // Abort if we've been waiting too long
            if ((openingTerritory || openingBank) && System.currentTimeMillis() - lastAction > 3000) {
                openingTerritory = false;
                openingBank = false;
            }

            // When the "Manage" menu opens, click the appropriate slot
            if ((openingTerritory || openingBank)
                    && client.currentScreen instanceof GenericContainerScreen gcs
                    && gcs.getTitle().getString().contains(": Manage")
                    && client.player.currentScreenHandler != null) {
                ScreenHandler handler = client.player.currentScreenHandler;
                int targetSlot = openingTerritory ? 14 : 15;
                String expectedName = openingTerritory ? "Territories" : "Bank";
                if (handler.slots.size() > targetSlot) {
                    ItemStack stack = handler.slots.get(targetSlot).getStack();
                    if (stack.getName().getString().contains(expectedName)) {
                        if (client.interactionManager != null) {
                            client.interactionManager.clickSlot(handler.syncId, targetSlot, 0, SlotActionType.PICKUP, client.player);
                        }
                        openingTerritory = false;
                        openingBank = false;
                    }
                }
            }
        });
    }
}
