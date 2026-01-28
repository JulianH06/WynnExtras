package julianh06.wynnextras.features.aspects;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.core.WynnExtras;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug utility to log screen titles for discovering menu identifiers.
 *
 * This class logs every unique screen title to both:
 * - The game log (visible in logs/latest.log)
 * - In-game chat (when debug mode is enabled)
 *
 * Used to discover Unicode identifiers for:
 * - Preview chest (for loot pool crowdsourcing)
 * - Party Finder menu (for gambit detection)
 *
 * Enable/disable via /we aspects debug command.
 */
public class ScreenTitleDebugger {
    private static final Logger LOGGER = LoggerFactory.getLogger("WynnExtras-ScreenDebug");

    private static String lastLoggedTitle = null;
    private static boolean debugEnabled = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!debugEnabled) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;

            Screen screen = mc.currentScreen;
            if (screen == null) {
                if (lastLoggedTitle != null) {
                    lastLoggedTitle = null;
                }
                return;
            }

            String title = screen.getTitle().getString();
            String titleHex = toHexString(title);

            // Only log when title changes
            if (!title.equals(lastLoggedTitle)) {
                lastLoggedTitle = title;

                // Log to file
                LOGGER.info("=== SCREEN OPENED ===");
                LOGGER.info("Title (raw): {}", title);
                LOGGER.info("Title (hex): {}", titleHex);
                LOGGER.info("Screen class: {}", screen.getClass().getName());
                LOGGER.info("=====================");

                // Log to chat
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§e[DEBUG] Screen: §f" + title));
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§e[DEBUG] Hex: §7" + titleHex));
            }
        });
    }

    /**
     * Toggle debug mode on/off
     */
    public static void toggleDebug() {
        debugEnabled = !debugEnabled;
        if (debugEnabled) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aScreen title debugging ENABLED"));
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7Open any menu to see its title logged"));
        } else {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cScreen title debugging DISABLED"));
        }
    }

    /**
     * Check if debug is currently enabled
     */
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Convert string to hex representation for comparing Unicode characters
     */
    private static String toHexString(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(String.format("\\u%04X", (int) c));
        }
        return sb.toString();
    }
}
