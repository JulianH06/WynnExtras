package julianh06.wynnextras.features.showcase;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

public class FeatureShowcaseScreen extends WEScreen {

    private int scrollOffset = 0;
    private int maxScrollOffset = 0;

    public FeatureShowcaseScreen() {
        super(Text.of("WynnExtras Features"));
    }

    public static void open() {
        WEScreen.open(FeatureShowcaseScreen::new);
    }

    @Override
    protected void init() {
        super.init();
        registerScrolling();
    }

    @Override
    protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
        int logicalW = getLogicalWidth();
        int logicalH = getLogicalHeight();
        int centerX = logicalW / 2;

        // Convert mouse coords to logical
        int logicalMouseX = (int)(mouseX * scaleFactor);
        int logicalMouseY = (int)(mouseY * scaleFactor);

        // Header
        if (ui != null) {
            ui.drawCenteredText("§2§lWYNNEXTRAS", centerX, 20, CustomColor.fromInt(0xFFFFFF), 3f);
            ui.drawCenteredText("§e§lFEATURES", centerX, 60, CustomColor.fromInt(0xFFFFFF), 3f);
            ui.drawCenteredText("§7Discover all WynnExtras features", centerX, 100, CustomColor.fromInt(0xAAAAAA), 2.5f);
        }

        // Feature list area
        int listX = 60;
        int listY = 140;
        int listWidth = logicalW - 120;
        int listHeight = logicalH - 180;

        // Background
        drawRect(listX, listY, listWidth, listHeight, 0xAA000000);
        drawRect(listX, listY, listWidth, 4, 0xFF4e392d);
        drawRect(listX, listY + listHeight - 4, listWidth, 4, 0xFF4e392d);
        drawRect(listX, listY, 4, listHeight, 0xFF4e392d);
        drawRect(listX + listWidth - 4, listY, 4, listHeight, 0xFF4e392d);

        // Draw features grouped by category
        int itemY = listY + 20 - scrollOffset;
        int itemHeight = 70;
        int categoryHeight = 40;
        int visibleAreaTop = listY + 10;
        int visibleAreaBottom = listY + listHeight - 10;

        List<String> categories = FeatureRegistry.getCategories();
        int totalContentHeight = 0;

        for (String category : categories) {
            // Category header
            if (itemY + categoryHeight > visibleAreaTop && itemY < visibleAreaBottom) {
                drawCategoryHeader(listX + 20, itemY, listWidth - 40, category);
            }
            itemY += categoryHeight;
            totalContentHeight += categoryHeight;

            // Features in category
            List<FeatureInfo> features = FeatureRegistry.getFeaturesByCategory(category);
            for (FeatureInfo feature : features) {
                if (itemY + itemHeight > visibleAreaTop && itemY < visibleAreaBottom) {
                    drawFeatureItem(context, listX + 30, itemY, listWidth - 60, itemHeight - 10,
                            feature, logicalMouseX, logicalMouseY);
                }
                itemY += itemHeight;
                totalContentHeight += itemHeight;
            }

            // Spacing between categories
            itemY += 20;
            totalContentHeight += 20;
        }

        // Calculate max scroll
        maxScrollOffset = Math.max(0, totalContentHeight - listHeight + 40);
    }

    private void drawCategoryHeader(int x, int y, int width, String category) {
        // Category background
        drawRect(x, y, width, 30, 0xFF333344);
        drawRect(x, y, 4, 30, getCategoryColor(category));

        if (ui != null) {
            ui.drawText("§l" + category.toUpperCase(), x + 20, y + 8, CustomColor.fromInt(0xFFFFFF), 2.5f);
        }
    }

    private void drawFeatureItem(DrawContext context, int x, int y, int width, int height,
                                 FeatureInfo feature, int mouseX, int mouseY) {
        boolean hovered = isInBounds(mouseX, mouseY, x, y, width, height);
        boolean enabled = feature.isEnabled();

        // Background
        int bgColor = hovered ? 0xAA3a3a3a : 0xAA2a2a2a;
        drawRect(x, y, width, height, bgColor);

        // Enabled indicator
        int indicatorColor = enabled ? 0xFF55FF55 : 0xFF555555;
        drawRect(x, y, 4, height, indicatorColor);

        if (ui != null) {
            // Feature name
            String name = (enabled ? "§a" : "§7") + feature.getName();
            ui.drawText(name, x + 20, y + 8, CustomColor.fromInt(0xFFFFFF), 2.5f);

            // Description
            ui.drawText("§7" + feature.getDescription(), x + 20, y + 30, CustomColor.fromInt(0xAAAAAA), 2f);

            // Keybind/command
            String keybind = feature.getKeybind();
            if (keybind != null && !keybind.isEmpty()) {
                int keybindWidth = (int)(keybind.length() * 8);
                drawRect(x + width - keybindWidth - 30, y + 8, keybindWidth + 20, 22, 0xFF444466);
                ui.drawText("§e" + keybind, x + width - keybindWidth - 20, y + 12, CustomColor.fromInt(0xFFFF55), 2f);
            }

            // Status text
            String status = enabled ? "§aEnabled" : "§cDisabled";
            ui.drawText(status, x + width - 100, y + 35, CustomColor.fromInt(0xAAAAAA), 1.8f);
        }
    }

    private int getCategoryColor(String category) {
        return switch (category) {
            case "Social" -> 0xFF55FFFF;
            case "Inventory" -> 0xFFFFAA00;
            case "Raids" -> 0xFFFF5555;
            case "Aspects" -> 0xFF00AAFF;
            case "Misc" -> 0xFFAAAAAA;
            case "Builds" -> 0xFF55FF55;
            case "Rendering" -> 0xFFFF55FF;
            case "Chat" -> 0xFFFFFF55;
            case "Navigation" -> 0xFF5555FF;
            case "Combat" -> 0xFFFF0000;
            case "Economy" -> 0xFFFFD700;
            case "Crafting" -> 0xFF8B4513;
            case "Lootruns" -> 0xFFAA00FF;
            default -> 0xFFFFFFFF;
        };
    }

    @Override
    protected void scrollList(float delta) {
        scrollOffset -= (int)delta;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
    }

    private boolean isInBounds(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    protected void drawRect(int x, int y, int width, int height, int color) {
        if (drawContext != null) {
            drawContext.fill(
                    (int)(x / scaleFactor + xStart),
                    (int)(y / scaleFactor + yStart),
                    (int)((x + width) / scaleFactor + xStart),
                    (int)((y + height) / scaleFactor + yStart),
                    color
            );
        }
    }

    protected int getLogicalWidth() {
        return (int)(screenWidth * scaleFactor);
    }

    protected int getLogicalHeight() {
        return (int)(screenHeight * scaleFactor);
    }
}
