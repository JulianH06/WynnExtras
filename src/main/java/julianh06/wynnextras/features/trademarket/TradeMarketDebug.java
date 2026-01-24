package julianh06.wynnextras.features.trademarket;

import com.wynntils.utils.mc.McUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.List;

/**
 * DEBUG FILE for Trade Market analysis
 *
 * Wait 1 second after screen opens before dumping to allow items to load.
 */
public class TradeMarketDebug {

    private static boolean debuggedCurrentScreen = false;
    private static String lastScreenTitle = "";
    private static int ticksWaited = 0;
    private static final int WAIT_TICKS = 20; // Wait 1 second (20 ticks) for items to load

    // Set to true to enable debug output
    public static boolean DEBUG_ENABLED = false;

    // Set to true to output to chat (visible in-game), false for log only
    public static boolean OUTPUT_TO_CHAT = false; // Disabled chat spam, check logs instead

    // Only debug screens with this title (Your Trades screen)
    private static final String YOUR_TRADES_TITLE = "󏿨";

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!DEBUG_ENABLED) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;

            Screen screen = mc.currentScreen;
            if (screen == null) {
                debuggedCurrentScreen = false;
                lastScreenTitle = "";
                ticksWaited = 0;
                return;
            }

            String screenTitle = screen.getTitle().getString();

            // Reset debug flag if screen changed
            if (!screenTitle.equals(lastScreenTitle)) {
                debuggedCurrentScreen = false;
                lastScreenTitle = screenTitle;
                ticksWaited = 0;
            }

            // Wait for items to load, then debug
            if (!debuggedCurrentScreen) {
                ticksWaited++;
                if (ticksWaited >= WAIT_TICKS) {
                    // Log title with unicode escape codes
                    StringBuilder hexCodes = new StringBuilder();
                    for (int i = 0; i < screenTitle.length(); i++) {
                        hexCodes.append(String.format("\\u%04X", (int) screenTitle.charAt(i)));
                    }
                    System.out.println("[TradeMarketDebug] Screen title: '" + screenTitle + "' hex: " + hexCodes);
                    debugScreen(screen, screenTitle);
                    debuggedCurrentScreen = true;
                }
            }
        });
    }

    private static void debugScreen(Screen screen, String screenTitle) {
        ScreenHandler handler = McUtils.containerMenu();
        if (handler == null) {
            log("§c[TradeMarketDebug] No container menu found");
            return;
        }

        log("§6========================================");
        log("§6[TradeMarketDebug] YOUR TRADES SCREEN");
        log("§6========================================");
        log("§eTotal Slots: §f" + handler.slots.size());
        log("§6----------------------------------------");

        int itemCount = 0;
        long totalPendingValue = 0;
        long totalSoldValue = 0;

        // Iterate slots 0-44 (container slots, not player inventory)
        for (int i = 0; i < Math.min(54, handler.slots.size()); i++) {
            Slot slot = handler.getSlot(i);
            if (slot == null) continue;

            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;

            // Skip UI elements (Back button, Sell button, etc)
            String itemName = stack.getName().getString();
            if (itemName.equals("Back") || itemName.equals("Sell an Item") ||
                itemName.contains("Page") || itemName.contains("Filter")) {
                continue;
            }

            itemCount++;
            logItem(i, slot, stack);
        }

        log("§6----------------------------------------");
        log("§eTotal items found: §f" + itemCount);
        log("§6========================================");
        log("§6[TradeMarketDebug] END");
        log("§6========================================");
    }

    private static void logItem(int slotIndex, Slot slot, ItemStack stack) {
        String itemName = stack.getName().getString();
        int count = stack.getCount();

        log("§a--- Slot " + slotIndex + ": " + itemName + " ---");
        log("  §bCount: §f" + count);

        // Lore (where Pending/Fulfilled and prices are)
        try {
            if (stack.getComponents() != null) {
                LoreComponent loreComponent = stack.getComponents().get(DataComponentTypes.LORE);
                if (loreComponent != null) {
                    List<Text> loreLines = loreComponent.lines();
                    if (!loreLines.isEmpty()) {
                        log("  §bLore:");
                        for (int j = 0; j < loreLines.size(); j++) {
                            Text line = loreLines.get(j);
                            String lineStr = line.getString();
                            log("    §7[" + j + "] §f" + lineStr);

                            // Highlight key info
                            String lower = lineStr.toLowerCase();
                            if (lower.contains("pending") || lower.contains("fulfilled") ||
                                lower.contains("sold") || lower.contains("price") ||
                                lower.contains("emerald") || lower.contains("stx") ||
                                lower.contains(" le") || lower.contains(" eb")) {
                                log("    §e    ^^^ KEY INFO ^^^");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log("  §cError reading lore: " + e.getMessage());
        }
    }

    private static void log(String message) {
        // Always log to console
        System.out.println("[TradeMarketDebug] " + stripFormatting(message));

        // Optionally send to chat
        if (OUTPUT_TO_CHAT) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal(message), false);
            }
        }
    }

    private static String stripFormatting(String input) {
        if (input == null) return "null";
        return input.replaceAll("§.", "");
    }
}
