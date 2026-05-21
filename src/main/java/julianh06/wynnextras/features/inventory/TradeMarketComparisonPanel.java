package julianh06.wynnextras.features.inventory;

import com.wynntils.core.components.Models;
import com.wynntils.models.items.WynnItem;
import com.wynntils.utils.mc.TooltipUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Trade Market Item Comparison Panel
 * Press F1 on items to add them for comparison (max 3 panels)
 * Shows the full tooltip with all formatting
 */
public class TradeMarketComparisonPanel {
    private static final int MAX_PANELS = 3;

    // Border colors for each panel position
    private static final int[] PANEL_BORDER_COLORS = {
            0xFFFF0000,  // 1st, Red
            0xFFFFFF00,  // 2nd, Dark green
            0xFF88FF00   // 3rd, Light green/lime
    };

    private static class ComparisonPanel {
        ItemStack item;
        List<Text> tooltip;
        int x, y;
        int width, height;
        float scale = 1.0f;
        boolean dragging;
        int dragOffsetX, dragOffsetY;
        int borderColor;
        int slotIndex;
        String itemId; // For matching items in slots

        ComparisonPanel(ItemStack item, List<Text> tooltip, int x, int y, int slotIndex) {
            this.item = item;
            this.tooltip = tooltip;
            this.slotIndex = slotIndex;
            this.borderColor = PANEL_BORDER_COLORS[slotIndex];
            this.x = x;
            this.y = y;
            this.width = calculateWidth(tooltip);
            this.height = tooltip.size() * 10 + 8;
            this.dragging = false;
            this.itemId = getItemIdentifier(item);
        }

        private static int findFreeSlot() {
            boolean[] used = new boolean[MAX_PANELS];

            for (ComparisonPanel panel : panels) {
                used[panel.slotIndex] = true;
            }

            for (int i = 0; i < MAX_PANELS; i++) {
                if (!used[i]) return i;
            }
            return -1;
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

    private static final List<ComparisonPanel> panels = new ArrayList<>();
    private static ItemStack lastHoveredStack = null;
    private static List<Text> lastHoveredTooltip = null;

    private static boolean slotDebugEnabled = false;

    private static final int CLOSE_BUTTON_SIZE = 10;

    private static final int STOP_BUTTON_WIDTH = 100;
    private static final int STOP_BUTTON_HEIGHT = 14;

    private static final int TOGGLE_BUTTON_WIDTH = 110;
    private static final int TOGGLE_BUTTON_HEIGHT = 14;

    private static final int INFO_BUTTON_WIDTH = 150;
    private static final int INFO_BUTTON_HEIGHT = 25;

    private static final List<String> TRADE_MARKET_TITLES = List.of(
            "\uDAFF\uDFE8\uE013", // Your Trades
            "\uDAFF\uDFE8\uE00F", // Browse
            "\uDAFF\uDFE8\uE010", // Search Results
            "\uDAFF\uDFE8\uE011", // Item listing / search
            "Trade Market"
    );

    // Called from ItemStatInfoFeatureMixin to cache the fully processed tooltip
    public static void cacheHoveredTooltip(ItemStack stack, List<Text> tooltip) {
        lastHoveredStack = stack;
        lastHoveredTooltip = tooltip;
    }

    public static boolean isInTradeMarket() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) return false;

        String title = mc.currentScreen.getTitle().getString();
        for (String marketTitle : TRADE_MARKET_TITLES) {
            if (title.contains(marketTitle)) return true;
        }
        return title.toLowerCase().contains("trade");
    }

    public static boolean handleF1Press(Slot slot) {
        if (slot == null) return false;

        ItemStack stack = slot.getStack();
        if (stack == null || stack.isEmpty()) return false;

        addPanel(stack.copy());
        return true;
    }

    public static boolean handleF2Press() {
        if (!isInTradeMarket()) return false;

        WynnExtrasConfig.INSTANCE.scaleBackgroundEnabled = !WynnExtrasConfig.INSTANCE.scaleBackgroundEnabled;
        WynnExtrasConfig.save();
        return true;
    }

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

    private static void addPanel(ItemStack stack) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = mc.getWindow().getScaledWidth();

        int slotIndex = ComparisonPanel.findFreeSlot();

        if (slotIndex == -1) {
            panels.remove(0);
            slotIndex = ComparisonPanel.findFreeSlot();
        }

        if (slotIndex < 0 || slotIndex >= MAX_PANELS) return;

        int panelNum = slotIndex + 1;
        List<Text> tooltip = buildTooltip(stack, panelNum + ":");

        int panelWidth = ComparisonPanel.calculateWidth(tooltip);
        int x, y = 25;

        switch (slotIndex) {
            case 0 -> x = 5;
            case 1 -> x = screenWidth - panelWidth - 5;
            case 2 -> x = (screenWidth - panelWidth) / 2;
            default -> x = 5;
        }

        ComparisonPanel panel = new ComparisonPanel(stack, tooltip, x, y, slotIndex);
        panel.scale = computeScale(y, panel.height);
        panels.add(panel);
    }


    private static List<Text> buildTooltip(ItemStack stack, String headerPrefix) {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Text> tooltip = new ArrayList<>();
        List<Text> processedTooltip;

        if (lastHoveredStack != null && lastHoveredTooltip != null && !lastHoveredTooltip.isEmpty()) {
            String cachedName = lastHoveredStack.getName().getString();
            String stackName = stack.getName().getString();
            if (cachedName.equals(stackName)) {
                processedTooltip = new ArrayList<>(lastHoveredTooltip);
            } else {
                processedTooltip = getFallbackTooltip(stack, mc);
            }
        } else {
            processedTooltip = getFallbackTooltip(stack, mc);
        }

        WeightDisplay.setCurrentHoveredStack(stack);

        if (WeightDisplay.isTrackedMythic(stack) && !WeightDisplay.isUnidentified(stack)) {
            processedTooltip = WeightDisplay.modifyTooltip(processedTooltip, stack);
        }

        tooltip.add(WynnExtras.addWynnExtrasPrefix("§6Item Comparison " + headerPrefix));
        tooltip.add(Text.of(" "));
        tooltip.addAll(processedTooltip);

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

    public static boolean hasAnyComparison() {
        return !panels.isEmpty();
    }

    public static void render(DrawContext context) {
        if (!isInTradeMarket()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) return;

        TextRenderer textRenderer = mc.textRenderer;

        if(!WynnExtrasConfig.INSTANCE.hideScaleBackgroundButton) renderToggleButton(context, textRenderer);

        if (hasAnyComparison()) {
            renderStopButton(context, textRenderer);

            for (ComparisonPanel panel : panels) {
                renderTooltipAt(context, textRenderer, panel.tooltip, panel.x, panel.y, panel.width, panel.height, panel.borderColor, panel.scale);
                int closeX = panel.x + (int)(panel.width * panel.scale) - CLOSE_BUTTON_SIZE + 2;
                renderCloseButton(context, textRenderer, closeX, panel.y - 4);
            }
        } else if(!WynnExtrasConfig.INSTANCE.hideTMInfoText) {
            renderInfoButton(context, textRenderer);
        }
    }

    public static int getComparisonBorderColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        for (ComparisonPanel panel : panels) {
            if (isSameForComparison(stack, panel.item)) {
                return panel.borderColor;
            }
        }
        return 0;
    }

    private static boolean isSameForComparison(ItemStack a, ItemStack b) {
        if(a.isEmpty() || b.isEmpty()) return false;

        if(!a.isOf(b.getItem())) {
            return false;
        }

        if(a.getCustomName() == null || b.getCustomName() == null) return false;

        if(!a.getCustomName().getString().contains(b.getCustomName().getString())) return false;

        return Objects.equals(a.get(DataComponentTypes.LORE),
                b.get(DataComponentTypes.LORE));
    }

    private static float computeScale(int y, int height) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenHeight = mc.getWindow() != null ? mc.getWindow().getScaledHeight() : 1080;
        int totalHeight = height + 7;
        int availableHeight = screenHeight - y - 5;
        return totalHeight > availableHeight ? (float) availableHeight / totalHeight : 1.0f;
    }

    private static void renderTooltipAt(DrawContext context, TextRenderer textRenderer, List<Text> lines, int x, int y, int width, int height, int borderColor, float scale) {
        if (lines.isEmpty()) return;

        if (scale < 1.0f) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(x, y);
            context.getMatrices().scale(scale, scale);
            context.getMatrices().translate(-x, -y);
        }

        int bgColor = 0xF0100010;
        context.fill(x - 3, y - 4, x + width + 3, y + height + 3, bgColor);

        // Colored border (2px thick)
        // Top
        context.fill(x - 4, y - 4, x + width + 4, y - 2, borderColor);
        // Bottom
        context.fill(x - 4, y + height + 2, x + width + 4, y + height + 4, borderColor);
        // Left
        context.fill(x - 4, y - 2, x - 2, y + height + 2, borderColor);
        // Right
        context.fill(x + width + 2, y - 2, x + width + 4, y + height + 2, borderColor);

        int textY = y;
        for (Text line : lines) {
            context.drawText(textRenderer, line, x, textY, 0xFFFFFFFF, true);
            textY += 10;
        }

        if (scale < 1.0f) {
            context.getMatrices().popMatrix();
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

        context.drawText(textRenderer, Text.literal("§lX"), x + 2, y + 1, 0xFFFFFFFF, false);
    }

    private static void renderStopButton(DrawContext context, TextRenderer textRenderer) {
        int buttonX = 0;
        int buttonY = 0;

        // Button background (dark red)
        context.fill(buttonX, buttonY, buttonX + STOP_BUTTON_WIDTH, buttonY + STOP_BUTTON_HEIGHT, 0xFFAA0000);
        // Border
        context.fill(buttonX, buttonY, buttonX + STOP_BUTTON_WIDTH, buttonY + 1, 0xFF660000);
        context.fill(buttonX, buttonY + STOP_BUTTON_HEIGHT - 1, buttonX + STOP_BUTTON_WIDTH, buttonY + STOP_BUTTON_HEIGHT, 0xFF660000);
        context.fill(buttonX, buttonY, buttonX + 1, buttonY + STOP_BUTTON_HEIGHT, 0xFF660000);
        context.fill(buttonX + STOP_BUTTON_WIDTH - 1, buttonY, buttonX + STOP_BUTTON_WIDTH, buttonY + STOP_BUTTON_HEIGHT, 0xFF660000);

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

        String text = enabled ? "Disable Scale BG" : "Enable Scale BG";
        int textWidth = textRenderer.getWidth(text);
        int textX = buttonX + (TOGGLE_BUTTON_WIDTH - textWidth) / 2;
        int textY = buttonY + (TOGGLE_BUTTON_HEIGHT - 8) / 2;
        context.drawText(textRenderer, Text.literal(text), textX, textY, 0xFFFFFFFF, true);
    }

    private static void renderInfoButton(DrawContext context, TextRenderer textRenderer) {
        int buttonX = 0;
        int buttonY = 0;

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
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("(Click to hide this text)"), (int) (INFO_BUTTON_WIDTH / 2f), textY + 10, 0xFFa0a0a0);

    }

    public static boolean handleClick(double mouseX, double mouseY, int button, int action) {
        if (!isInTradeMarket()) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 400;

        if (button != 0 || action != 1) {
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

        int stopButtonX = 0;
        int stopButtonY = 0;
        if (mouseX >= stopButtonX && mouseX <= stopButtonX + STOP_BUTTON_WIDTH &&
                mouseY >= stopButtonY && mouseY <= stopButtonY + STOP_BUTTON_HEIGHT) {
            clearAllPanels();
            return true;
        }

        for (int i = panels.size() - 1; i >= 0; i--) {
            ComparisonPanel panel = panels.get(i);

            int closeX = panel.x + (int)(panel.width * panel.scale) - CLOSE_BUTTON_SIZE + 2;
            int closeY = panel.y - 4;
            if (mouseX >= closeX && mouseX <= closeX + CLOSE_BUTTON_SIZE &&
                    mouseY >= closeY && mouseY <= closeY + CLOSE_BUTTON_SIZE) {
                panels.remove(i);
                return true;
            }

            boolean inBounds = mouseX >= panel.x - 4 && mouseX <= panel.x + (int)(panel.width * panel.scale) + 4 &&
                    mouseY >= panel.y - 4 && mouseY <= panel.y + (int)(panel.height * panel.scale) + 4;
            if (inBounds) {
                panel.dragging = true;
                panel.dragOffsetX = (int) mouseX - panel.x;
                panel.dragOffsetY = (int) mouseY - panel.y;
                return true;
            }
        }

        return false;
    }

    public static void handleMouseMove(double mouseX, double mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = mc.getWindow() != null ? mc.getWindow().getScaledWidth() : 1920;
        int screenHeight = mc.getWindow() != null ? mc.getWindow().getScaledHeight() : 1080;

        for (ComparisonPanel panel : panels) {
            if (panel.dragging) {
                panel.x = (int) mouseX - panel.dragOffsetX;
                panel.y = (int) mouseY - panel.dragOffsetY;
                panel.x = Math.max(5, Math.min(panel.x, screenWidth - (int)(panel.width * panel.scale) - 10));
                panel.y = Math.max(5, Math.min(panel.y, screenHeight - (int)(panel.height * panel.scale) - 10));
            }
        }
    }

    public static boolean isDragging() {
        for (ComparisonPanel panel : panels) {
            if (panel.dragging) return true;
        }
        return false;
    }
}
