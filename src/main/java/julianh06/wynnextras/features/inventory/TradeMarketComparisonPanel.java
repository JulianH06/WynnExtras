package julianh06.wynnextras.features.inventory;

import com.wynntils.core.components.Models;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.utils.mc.TooltipUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.utils.ItemUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Trade Market Item Comparison Panel
 * Press F1 on items to add them for comparison (max 3 panels)
 * Shows the full Wynntils tooltip with all formatting
 */
public class TradeMarketComparisonPanel {

    private static final int MAX_PANELS = 3;

    // Border colors for each panel position
    // 1st = Red, 2nd = Dark Green, 3rd = Light Green/Lime
    private static final int[] PANEL_BORDER_COLORS = {
            0xFFFF0000,  // Red for 1st item
            0xFF006600,  // Dark green for 2nd item
            0xFF88FF00   // Light green/lime for 3rd item
    };

    // Panel data class
    private static class ComparisonPanel {
        ItemStack item;
        List<Text> tooltip;
        int x, y;
        int width, height;
        boolean dragging;
        int dragOffsetX, dragOffsetY;
        int borderColor;
        String itemId;  // For matching items in slots

        ComparisonPanel(ItemStack item, List<Text> tooltip, int x, int y, int borderColor) {
            this.item = item;
            this.tooltip = tooltip;
            this.x = x;
            this.y = y;
            this.width = calculateWidth(tooltip);
            this.height = tooltip.size() * 10 + 8;
            this.dragging = false;
            this.borderColor = borderColor;
            this.itemId = getItemIdentifier(item);
        }

        private static int calculateWidth(List<Text> tooltip) {
            MinecraftClient mc = MinecraftClient.getInstance();
            TextRenderer textRenderer = mc.textRenderer;
            int width = 0;
            for (Text line : tooltip) {
                int lineWidth = textRenderer.getWidth(line);
                if (lineWidth > width) {
                    width = lineWidth;
                }
            }
            return width + 8;
        }

        static String getItemIdentifier(ItemStack stack) {
            // Use name + component toString for reliable matching
            // Components contain all the item data including stat rolls
            String name = stack.getName().getString();
            String components = stack.getComponents().toString();
            return name + "|" + components.hashCode();
        }
    }

    // All comparison panels (max 3)
    private static final List<ComparisonPanel> panels = new ArrayList<>();

    // Cache for the last hovered tooltip (with Wynntils [XX.X%] percentages included)
    private static ItemStack lastHoveredStack = null;
    private static List<Text> lastHoveredTooltip = null;

    // Slot debug mode
    private static boolean slotDebugEnabled = false;
    private static String lastDebuggedItemId = null;

    // Close button size
    private static final int CLOSE_BUTTON_SIZE = 10;

    // Stop Comparing button
    private static final int STOP_BUTTON_WIDTH = 100;
    private static final int STOP_BUTTON_HEIGHT = 14;

    // Toggle Scale BG button
    private static final int TOGGLE_BUTTON_WIDTH = 70;
    private static final int TOGGLE_BUTTON_HEIGHT = 14;

    private static final int INFO_BUTTON_WIDTH = 150;
    private static final int INFO_BUTTON_HEIGHT = 25;

    // Trade Market screen titles (various screens)
    private static final List<String> TRADE_MARKET_TITLES = List.of(
            "\uDAFF\uDFE8\uE013", // Your Trades
            "\uDAFF\uDFE8\uE00F", // Browse
            "\uDAFF\uDFE8\uE010", // Search Results
            "\uDAFF\uDFE8\uE011", // Item listing / search
            "Trade Market"
    );

    /**
     * Called from ItemStatInfoFeatureMixin to cache the fully processed tooltip
     */
    public static void cacheHoveredTooltip(ItemStack stack, List<Text> tooltip) {
        lastHoveredStack = stack;
        lastHoveredTooltip = tooltip;
    }

    /**
     * Check if we're in a trade market screen
     */
    public static boolean isInTradeMarket() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) return false;

        String title = mc.currentScreen.getTitle().getString();
        for (String marketTitle : TRADE_MARKET_TITLES) {
            if (title.contains(marketTitle)) return true;
        }
        return title.toLowerCase().contains("trade");
    }

    /**
     * Handle F1 key press on a slot - adds new panel (max 3)
     */
    public static boolean handleF1Press(Slot slot) {
        if (slot == null) return false;

        ItemStack stack = slot.getStack();
        if (stack == null || stack.isEmpty()) return false;

        addPanel(stack.copy());
        return true;
    }

    /**
     * Handle F1 when no slot is focused
     */
    public static boolean handleF1NoSlot() {
        return false; // Don't clear panels, user must use buttons
    }

    /**
     * Handle F2 key press - toggle scale background on/off
     */
    public static boolean handleF2Press() {
        if (!isInTradeMarket()) return false;

        WynnExtrasConfig.INSTANCE.scaleBackgroundEnabled = !WynnExtrasConfig.INSTANCE.scaleBackgroundEnabled;
        WynnExtrasConfig.save();
        return true;
    }

    /**
     * Toggle slot debug mode and print all slots
     */
    public static void toggleSlotDebug() {
        slotDebugEnabled = !slotDebugEnabled;
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player != null) {
            if (slotDebugEnabled) {
                mc.player.sendMessage(Text.literal("§e[Debug] Slot debug §aENABLED"), false);
                // Print all slots if in a container
                if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen) {
                    var handler = handledScreen.getScreenHandler();
                    for (int i = 0; i < handler.slots.size(); i++) {
                        var slot = handler.slots.get(i);
                        ItemStack stack = slot.getStack();
                        if (stack != null && !stack.isEmpty()) {
                            String name = stack.getName().getString();
                            mc.player.sendMessage(Text.literal("§e[Slot " + i + "] §f" + name), false);
                        }
                    }
                } else {
                    mc.player.sendMessage(Text.literal("§7Not in a container - open one to see slots"), false);
                }
            } else {
                mc.player.sendMessage(Text.literal("§e[Debug] Slot debug §cDISABLED"), false);
            }
        }
    }

    /**
     * Check if slot debug is enabled
     */
    public static boolean isSlotDebugEnabled() {
        return slotDebugEnabled;
    }

    private static void addPanel(ItemStack stack) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 800;

        // Determine panel index and border color
        int panelIndex = panels.size();
        if (panelIndex >= MAX_PANELS) {
            panels.remove(0);
            panelIndex = panels.size();  // Will be MAX_PANELS - 1 after removal
        }
        int borderColor = PANEL_BORDER_COLORS[panelIndex % PANEL_BORDER_COLORS.length];

        // Build the tooltip
        int panelNum = panelIndex + 1;
        List<Text> tooltip = buildTooltip(stack, panelNum + ":");

        // Calculate initial position based on panel number
        int panelWidth = ComparisonPanel.calculateWidth(tooltip);
        int x, y = 25; // Start below the Stop Comparing button

        if (panelIndex == 0) {
            // First panel: all the way to the left
            x = 5;
        } else if (panelIndex == 1) {
            // Second panel: all the way to the right
            x = screenWidth - panelWidth - 5;
        } else {
            // Third panel: center
            x = (screenWidth - panelWidth) / 2;
        }

        panels.add(new ComparisonPanel(stack, tooltip, x, y, borderColor));
    }

    private static List<Text> buildTooltip(ItemStack stack, String headerPrefix) {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Text> tooltip = new ArrayList<>();
        List<Text> wynntilsTooltip;

        // Check if we have a cached tooltip from hovering (which includes [XX.X%] percentages)
        if (lastHoveredStack != null && lastHoveredTooltip != null && !lastHoveredTooltip.isEmpty()) {
            String cachedName = lastHoveredStack.getName().getString();
            String stackName = stack.getName().getString();
            if (cachedName.equals(stackName)) {
                wynntilsTooltip = new ArrayList<>(lastHoveredTooltip);
            } else {
                wynntilsTooltip = getFallbackTooltip(stack, mc);
            }
        } else {
            wynntilsTooltip = getFallbackTooltip(stack, mc);
        }

        // For mythics, add weight display
        Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(stack);
        if (wynnItemOpt.isPresent()) {
            WynnItem wynnItem = wynnItemOpt.get();
            WeightDisplay.currentHoveredStack = stack;
            WeightDisplay.currentHoveredWynnitem = wynnItem;

            if (wynnItem instanceof GearItem gearItem && gearItem.getGearTier() == GearTier.MYTHIC) {
                String encodedItem = ItemUtils.itemStackToItemString(stack);
                if (encodedItem != null) {
                    WeightDisplay.WeightData weightData = WeightDisplay.getCachedWeight(encodedItem, true, stack, wynntilsTooltip);
                    if (weightData != null) {
                        wynntilsTooltip = WeightDisplay.modifyTooltip(wynntilsTooltip, weightData, stack, encodedItem);
                    }
                }
            }
        }

        // Filter out unwanted lines
        List<Text> filteredTooltip = new ArrayList<>();
        for (Text line : wynntilsTooltip) {
            if(line.equals(Text.empty()) || line.getString().equals(" ")) continue;
            System.out.println(line);
            String str = line.getString().toLowerCase();
            // Skip attack speed
            if (str.contains("attack speed")) continue;
            // Skip powder specials header
            if (str.contains("powder special")) continue;
            // Skip powder slots
            if (str.contains("powder slot") || str.contains("[0/")) continue;
            // Skip powder special effects
            if (str.contains("duration") || str.contains("radius") || str.contains("chains") ||
                str.contains("min. lost") || str.contains("rage") || str.contains("curse") ||
                str.contains("courage")) continue;
            // Skip combat level requirement
            if (str.contains("combat lv.") || str.contains("combat level")) continue;
            // Skip class requirement
            if (str.contains("class req:") || str.contains("class requirement")) continue;
            // Skip skill point requirements (strength, dexterity, intelligence, defence, agility)
            if (str.contains("✤ str") || str.contains("✦ dex") || str.contains("✽ int") ||
                str.contains("✹ def") || str.contains("❋ agi") ||
                str.contains("strength min") || str.contains("dexterity min") ||
                str.contains("intelligence min") || str.contains("defence min") || str.contains("agility min")) continue;
            // Skip skill point additions (+ Strength, + Dexterity, etc.)
            if ((str.contains("strength") || str.contains("dexterity") || str.contains("intelligence") ||
                str.contains("defence") || str.contains("agility") || str.contains("health")) && !str.contains("[")) continue;
            filteredTooltip.add(line);
            if(line.toString().toLowerCase().contains("item")) break;
        }

        // Build tooltip with header including item name
        tooltip.add(WynnExtras.addWynnExtrasPrefix("§6Item Comparison " + headerPrefix));
        tooltip.add(Text.of(" "));
        tooltip.addAll(filteredTooltip);
        tooltip.add(filteredTooltip.size() + 1, Text.of(" "));

        return tooltip;
    }

    private static List<Text> getFallbackTooltip(ItemStack stack, MinecraftClient mc) {
        Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(stack);
        if (wynnItemOpt.isPresent()) {
            return new ArrayList<>(TooltipUtils.getWynnItemTooltip(stack, wynnItemOpt.get()));
        } else {
            return new ArrayList<>(stack.getTooltip(
                    net.minecraft.item.Item.TooltipContext.DEFAULT,
                    mc.player,
                    net.minecraft.item.tooltip.TooltipType.BASIC
            ));
        }
    }

    public static void clearAllPanels() {
        panels.clear();
    }

    // Legacy method for compatibility
    public static void clearComparison() {
        clearAllPanels();
    }

    public static boolean hasAnyComparison() {
        return !panels.isEmpty();
    }

    // Legacy method for compatibility
    public static boolean hasComparison() {
        return hasAnyComparison();
    }

    /**
     * Render all comparison panels - called from a late render stage
     */
    public static void render(DrawContext context) {
        if (!isInTradeMarket()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) return;

        TextRenderer textRenderer = mc.textRenderer;

        // Always render the toggle button in trade market
        if(!WynnExtrasConfig.INSTANCE.hideScaleBackgroundButton) renderToggleButton(context, textRenderer);

        // Only render comparison panels if we have any
        if (hasAnyComparison()) {
            // Render "Stop Comparing" button
            renderStopButton(context, textRenderer);

            // Render all panels with close buttons and colored borders
            for (ComparisonPanel panel : panels) {
                renderTooltipAt(context, textRenderer, panel.tooltip, panel.x, panel.y, panel.width, panel.height, panel.borderColor);
                renderCloseButton(context, textRenderer, panel.x + panel.width - CLOSE_BUTTON_SIZE + 2, panel.y - 4);
            }
        } else if(!WynnExtrasConfig.INSTANCE.hideTMInfoText) {
            renderInfoButton(context, textRenderer);
        }
    }

    /**
     * Render panels on top layer - call this from a post-render hook
     */
    public static void renderOnTop(DrawContext context) {
        render(context);
    }

    /**
     * Get the border color for an item if it's being compared, or 0 if not
     */
    public static int getComparisonBorderColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        for (ComparisonPanel panel : panels) {
            // Use Minecraft's built-in comparison which handles all edge cases
            if (ItemStack.areItemsAndComponentsEqual(stack, panel.item)) {
                return panel.borderColor;
            }
        }
        return 0;
    }

    /**
     * Check if an item is being compared
     */
    public static boolean isItemBeingCompared(ItemStack stack) {
        return getComparisonBorderColor(stack) != 0;
    }

    private static void renderTooltipAt(DrawContext context, TextRenderer textRenderer, List<Text> lines, int x, int y, int width, int height, int borderColor) {
        if (lines.isEmpty()) return;

        // Background
        int bgColor = 0xF0100010;
        context.fill(x - 3, y - 4, x + width + 3, y + height + 3, bgColor);

        // Colored border (2px thick for visibility)
        // Top
        context.fill(x - 4, y - 4, x + width + 4, y - 2, borderColor);
        // Bottom
        context.fill(x - 4, y + height + 2, x + width + 4, y + height + 4, borderColor);
        // Left
        context.fill(x - 4, y - 2, x - 2, y + height + 2, borderColor);
        // Right
        context.fill(x + width + 2, y - 2, x + width + 4, y + height + 2, borderColor);

        // Text
        int textY = y;
        for (Text line : lines) {
            context.drawText(textRenderer, line, x, textY, 0xFFFFFFFF, true);
            textY += 10;
        }
    }

    private static void renderCloseButton(DrawContext context, TextRenderer textRenderer, int x, int y) {
        // Red background
        context.fill(x, y, x + CLOSE_BUTTON_SIZE, y + CLOSE_BUTTON_SIZE, 0xFFAA0000);
        // Darker border
        context.fill(x, y, x + CLOSE_BUTTON_SIZE, y + 1, 0xFF660000);
        context.fill(x, y + CLOSE_BUTTON_SIZE - 1, x + CLOSE_BUTTON_SIZE, y + CLOSE_BUTTON_SIZE, 0xFF660000);
        context.fill(x, y, x + 1, y + CLOSE_BUTTON_SIZE, 0xFF660000);
        context.fill(x + CLOSE_BUTTON_SIZE - 1, y, x + CLOSE_BUTTON_SIZE, y + CLOSE_BUTTON_SIZE, 0xFF660000);
        // X text
        context.drawText(textRenderer, Text.literal("§lX"), x + 2, y + 1, 0xFFFFFFFF, false);
    }

    private static void renderStopButton(DrawContext context, TextRenderer textRenderer) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 400;

        int buttonX = 0;
        int buttonY = 0;

        // Button background (dark red)
        context.fill(buttonX, buttonY, buttonX + STOP_BUTTON_WIDTH, buttonY + STOP_BUTTON_HEIGHT, 0xFFAA0000);
        // Border
        context.fill(buttonX, buttonY, buttonX + STOP_BUTTON_WIDTH, buttonY + 1, 0xFF660000);
        context.fill(buttonX, buttonY + STOP_BUTTON_HEIGHT - 1, buttonX + STOP_BUTTON_WIDTH, buttonY + STOP_BUTTON_HEIGHT, 0xFF660000);
        context.fill(buttonX, buttonY, buttonX + 1, buttonY + STOP_BUTTON_HEIGHT, 0xFF660000);
        context.fill(buttonX + STOP_BUTTON_WIDTH - 1, buttonY, buttonX + STOP_BUTTON_WIDTH, buttonY + STOP_BUTTON_HEIGHT, 0xFF660000);

        // Text centered
        String text = "Stop Comparing";
        int textWidth = textRenderer.getWidth(text);
        int textX = buttonX + (STOP_BUTTON_WIDTH - textWidth) / 2;
        int textY = buttonY + (STOP_BUTTON_HEIGHT - 8) / 2;
        context.drawText(textRenderer, Text.literal(text), textX, textY, 0xFFFFFFFF, true);
    }

    private static void renderToggleButton(DrawContext context, TextRenderer textRenderer) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 400;

        int buttonX = screenWidth - TOGGLE_BUTTON_WIDTH;
        int buttonY = 0;

        boolean enabled = WynnExtrasConfig.INSTANCE.scaleBackgroundEnabled;

        // Button background (green if enabled, gray if disabled)
        int bgColor = enabled ? 0xFF006600 : 0xFF444444;
        int borderColor = enabled ? 0xFF004400 : 0xFF222222;

        context.fill(buttonX, buttonY, buttonX + TOGGLE_BUTTON_WIDTH, buttonY + TOGGLE_BUTTON_HEIGHT, bgColor);
        // Border
        context.fill(buttonX, buttonY, buttonX + TOGGLE_BUTTON_WIDTH, buttonY + 1, borderColor);
        context.fill(buttonX, buttonY + TOGGLE_BUTTON_HEIGHT - 1, buttonX + TOGGLE_BUTTON_WIDTH, buttonY + TOGGLE_BUTTON_HEIGHT, borderColor);
        context.fill(buttonX, buttonY, buttonX + 1, buttonY + TOGGLE_BUTTON_HEIGHT, borderColor);
        context.fill(buttonX + TOGGLE_BUTTON_WIDTH - 1, buttonY, buttonX + TOGGLE_BUTTON_WIDTH, buttonY + TOGGLE_BUTTON_HEIGHT, borderColor);

        // Text centered - "Scale BG" when using weight scale, "Rarity BG" when using Wynntils rarity
        String text = enabled ? "Scale BG" : "Rarity BG";
        int textWidth = textRenderer.getWidth(text);
        int textX = buttonX + (TOGGLE_BUTTON_WIDTH - textWidth) / 2;
        int textY = buttonY + (TOGGLE_BUTTON_HEIGHT - 8) / 2;
        context.drawText(textRenderer, Text.literal(text), textX, textY, 0xFFFFFFFF, true);
    }

    private static void renderInfoButton(DrawContext context, TextRenderer textRenderer) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 400;

        int buttonX = 0;
        int buttonY = 0;

        boolean enabled = WynnExtrasConfig.INSTANCE.scaleBackgroundEnabled;

        // Button background (green if enabled, gray if disabled)
        int bgColor = 0xFF505050;
        int borderColor = 0xFF404040;

        context.fill(buttonX, buttonY, buttonX + INFO_BUTTON_WIDTH, buttonY + INFO_BUTTON_HEIGHT, bgColor);
        // Border
        context.fill(buttonX, buttonY, buttonX + INFO_BUTTON_WIDTH, buttonY + 1, borderColor);
        context.fill(buttonX, buttonY + INFO_BUTTON_HEIGHT - 1, buttonX + INFO_BUTTON_WIDTH, buttonY + INFO_BUTTON_HEIGHT, borderColor);
        context.fill(buttonX, buttonY, buttonX + 1, buttonY + INFO_BUTTON_HEIGHT, borderColor);
        context.fill(buttonX + INFO_BUTTON_WIDTH - 1, buttonY, buttonX + INFO_BUTTON_WIDTH, buttonY + INFO_BUTTON_HEIGHT, borderColor);

        String text = "Press F1 to compare items.";
        int textWidth = textRenderer.getWidth(text);
        int textX = buttonX + (INFO_BUTTON_WIDTH - textWidth) / 2;
        int textY = buttonY + 4;
        context.drawText(textRenderer, Text.literal(text), textX, textY, 0xFFFFFFFF, true);
        context.drawText(textRenderer, Text.literal("(Click to disable this text)"), textX, textY + 10, 0xFFa0a0a0, true);

    }

    /**
     * Handle mouse click for dragging (left-click) and close buttons
     */
    public static boolean handleClick(double mouseX, double mouseY, int button, int action) {
        if (!isInTradeMarket()) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 400;

        // Only handle left-click (button 0)
        if (button != 0 || action != 1) {
            // Release drag on mouse up
            if (action == 0 && hasAnyComparison()) {
                boolean wasDragging = false;
                for (ComparisonPanel panel : panels) {
                    if (panel.dragging) {
                        panel.dragging = false;
                        wasDragging = true;
                    }
                }
                return wasDragging;
            }
            return false;
        }

        if(!WynnExtrasConfig.INSTANCE.hideScaleBackgroundButton) {
            int toggleButtonX = screenWidth - TOGGLE_BUTTON_WIDTH;
            int toggleButtonY = 0;
            if (mouseX >= toggleButtonX && mouseX <= toggleButtonX + TOGGLE_BUTTON_WIDTH &&
                    mouseY >= toggleButtonY && mouseY <= toggleButtonY + TOGGLE_BUTTON_HEIGHT) {
                WynnExtrasConfig.INSTANCE.scaleBackgroundEnabled = !WynnExtrasConfig.INSTANCE.scaleBackgroundEnabled;
                WynnExtrasConfig.save();
                return true;
            }
        }

        // Rest only applies if we have panels
        if (!hasAnyComparison() && !WynnExtrasConfig.INSTANCE.hideTMInfoText) {
            int infoButtonX = 0;
            int infoButtonY = 0;
            if (mouseX >= infoButtonX && mouseX <= infoButtonX + INFO_BUTTON_WIDTH &&
                    mouseY >= infoButtonY && mouseY <= infoButtonY + INFO_BUTTON_HEIGHT) {
                WynnExtrasConfig.INSTANCE.hideTMInfoText = true;
                WynnExtrasConfig.save();
                return true;
            }
            return false;
        }

        // Check Stop Comparing button
        int stopButtonX = 0;
        int stopButtonY = 0;
        if (mouseX >= stopButtonX && mouseX <= stopButtonX + STOP_BUTTON_WIDTH &&
                mouseY >= stopButtonY && mouseY <= stopButtonY + STOP_BUTTON_HEIGHT) {
            clearAllPanels();
            return true;
        }

        // Check panels in reverse order (top panels first)
        for (int i = panels.size() - 1; i >= 0; i--) {
            ComparisonPanel panel = panels.get(i);

            // Check close button
            int closeX = panel.x + panel.width - CLOSE_BUTTON_SIZE + 2;
            int closeY = panel.y - 4;
            if (mouseX >= closeX && mouseX <= closeX + CLOSE_BUTTON_SIZE &&
                    mouseY >= closeY && mouseY <= closeY + CLOSE_BUTTON_SIZE) {
                panels.remove(i);
                return true;
            }

            // Check if clicking on panel for dragging
            boolean inBounds = mouseX >= panel.x - 4 && mouseX <= panel.x + panel.width + 4 &&
                    mouseY >= panel.y - 4 && mouseY <= panel.y + panel.height + 4;
            if (inBounds) {
                panel.dragging = true;
                panel.dragOffsetX = (int) mouseX - panel.x;
                panel.dragOffsetY = (int) mouseY - panel.y;
                return true;
            }
        }

        return false;
    }

    /**
     * Handle mouse movement for dragging
     */
    public static void handleMouseMove(double mouseX, double mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 1920;
        int screenHeight = mc.getWindow() != null ? mc.getWindow().getScaledHeight() : 1080;

        for (ComparisonPanel panel : panels) {
            if (panel.dragging) {
                panel.x = (int) mouseX - panel.dragOffsetX;
                panel.y = (int) mouseY - panel.dragOffsetY;
                panel.x = Math.max(5, Math.min(panel.x, screenWidth - panel.width - 10));
                panel.y = Math.max(5, Math.min(panel.y, screenHeight - panel.height - 10));
            }
        }
    }

    public static boolean isDragging() {
        for (ComparisonPanel panel : panels) {
            if (panel.dragging) return true;
        }
        return false;
    }

    // Legacy methods for compatibility with HandledScreenMixin
    public static boolean handleCtrlClick(Slot slot) {
        return handleF1Press(slot);
    }

    public static boolean handleF1Toggle(Slot slot) {
        return false;
    }
}
