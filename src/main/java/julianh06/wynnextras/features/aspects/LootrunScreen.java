package julianh06.wynnextras.features.aspects;

import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.features.aspects.pages.LootrunLootPoolPage;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen for viewing lootrun loot pools (5 camps)
 * Displays items (normal, shiny, tome) from lootrun chests
 */
public class LootrunScreen extends WEScreen {

    // Crowdsourced loot pools per camp
    private Map<String, List<LootrunLootPoolData.LootrunItem>> crowdsourcedLootPools = new HashMap<>();
    private boolean fetchedCrowdsourcedLootPools = false;

    // Scroll offsets for each camp column
    private int[] campScrollOffsets = new int[5];
    private int[] campContentHeights = new int[5];
    private int[] campColumnX = new int[5];
    private int[] campColumnWidth = new int[5];

    // For tooltips
    private LootrunLootPoolData.LootrunItem hoveredItem = null;
    private int hoveredItemX = 0;
    private int hoveredItemY = 0;

    public LootrunScreen() {
        super(Text.of("Lootrun Loot Pools"));
    }

    public static void open() {
        WEScreen.open(LootrunScreen::new);
    }

    @Override
    protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // Reset hovered item
        hoveredItem = null;

        int logicalW = getLogicalWidth();
        int logicalH = getLogicalHeight();
        int centerX = logicalW / 2;

        // Convert mouse coords to logical
        int logicalMouseX = (int)(mouseX * scaleFactor);
        int logicalMouseY = (int)(mouseY * scaleFactor);

        // Fetch crowdsourced loot pools from API on first load
        if (!fetchedCrowdsourcedLootPools) {
            fetchedCrowdsourcedLootPools = true;
            for (LootrunLootPoolPage.Camp camp : LootrunLootPoolPage.Camp.values()) {
                WynncraftApiHandler.fetchCrowdsourcedLootrunLootPool(camp.name()).thenAccept(result -> {
                    if (result != null && !result.isEmpty()) {
                        crowdsourcedLootPools.put(camp.name(), result);
                        System.out.println("[WynnExtras] Fetched " + result.size() + " crowdsourced lootrun items for " + camp.name());
                        // Save to local data for offline access
                        LootrunLootPoolData.INSTANCE.saveLootPool(camp.name(), result);
                    } else {
                        System.out.println("[WynnExtras] No crowdsourced lootrun pool for " + camp.name());
                    }
                });
            }
        }

        // Header
        if (ui != null) {
            ui.drawCenteredText("§2§lWYNNEXTRAS", centerX, 20, CustomColor.fromInt(0xFFFFFF), 3f);
            ui.drawCenteredText("§6§lLOOTRUN LOOT POOLS", centerX, 60, CustomColor.fromInt(0xFFFFFF), 3f);
            ui.drawCenteredText("§7Open a lootrun chest to scan items", centerX, 100, CustomColor.fromInt(0xFFFFFF), 2.5f);
        }

        // Draw camp columns
        int startY = 150;
        int spacing = 20;
        int numCamps = 5;
        int margin = 40;
        int availableWidth = logicalW - (margin * 2);
        int totalSpacing = spacing * (numCamps - 1);
        int colWidth = Math.max(280, (availableWidth - totalSpacing) / numCamps);

        // Calculate total width of all columns
        int totalWidth = (colWidth * numCamps) + totalSpacing;
        int startX = (logicalW - totalWidth) / 2;

        // Draw each camp column
        for (int i = 0; i < numCamps; i++) {
            String campCode = LootrunLootPoolPage.Camp.values()[i].name();
            int x = startX + i * (colWidth + spacing);

            campColumnX[i] = x;
            campColumnWidth[i] = colWidth;

            drawCampColumn(context, x, startY, campCode, colWidth, logicalMouseX, logicalMouseY, i);
        }

        // Render tooltip if hovering over item
        if (hoveredItem != null) {
            renderItemTooltip(context, mouseX, mouseY);
        }
    }

    private void drawCampColumn(DrawContext context, int x, int y, String campCode, int colWidth, int mouseX, int mouseY, int campIndex) {
        int logicalH = getLogicalHeight();
        int panelHeight = logicalH - y - 150;

        // Background
        drawRect(x, y, colWidth, panelHeight, 0xAA000000);

        // Border
        drawRect(x, y, colWidth, 6, 0xFF4e392d);
        drawRect(x, y + panelHeight - 6, colWidth, 6, 0xFF4e392d);
        drawRect(x, y, 6, panelHeight, 0xFF4e392d);
        drawRect(x + colWidth - 6, y, 6, panelHeight, 0xFF4e392d);

        // Header - show full camp name in bold orange
        String displayName = getCampDisplayName(campCode);
        if (ui != null) {
            ui.drawCenteredText("§6§l" + displayName, x + colWidth / 2, y + 20, CustomColor.fromInt(0xFFFFFF), 3f);
        }

        // Get items (prioritize crowdsourced data)
        List<LootrunLootPoolData.LootrunItem> items = getLootPoolForCamp(campCode);
        boolean usingCrowdsourced = crowdsourcedLootPools.containsKey(campCode) &&
                                     crowdsourcedLootPools.get(campCode) != null &&
                                     !crowdsourcedLootPools.get(campCode).isEmpty();

        // Show data source and count
        if (!items.isEmpty()) {
            String dataSource = usingCrowdsourced ? "§a(Crowdsourced)" : "§e(Local)";
            if (ui != null) {
                ui.drawCenteredText("§7" + items.size() + " items " + dataSource, x + colWidth / 2, y + 80, CustomColor.fromInt(0xFFFFFF), 2f);
            }
        }

        // Separator
        drawRect(x + 12, y + 95, colWidth - 24, 4, 0xFF4e392d);

        int contentStartY = y + 110;
        int contentHeight = panelHeight - 110 - 12;
        int scrollOffset = campScrollOffsets[campIndex];

        if (items.isEmpty()) {
            if (ui != null) {
                ui.drawCenteredText("§7No data", x + colWidth / 2, contentStartY + 40, CustomColor.fromInt(0xFFFFFF), 3f);
                ui.drawCenteredText("§7Open lootrun", x + colWidth / 2, contentStartY + 80, CustomColor.fromInt(0xFFFFFF), 2.5f);
                ui.drawCenteredText("§7chest to scan", x + colWidth / 2, contentStartY + 110, CustomColor.fromInt(0xFFFFFF), 2.5f);
            }
            campContentHeights[campIndex] = 0;
        } else {
            // Calculate total content height
            int itemSpacing = 32;
            int totalContentHeight = items.size() * itemSpacing + 20;
            campContentHeights[campIndex] = totalContentHeight;

            // Clamp scroll offset
            int maxScroll = Math.max(0, totalContentHeight - contentHeight);
            if (scrollOffset > maxScroll) {
                campScrollOffsets[campIndex] = maxScroll;
                scrollOffset = maxScroll;
            }
            if (scrollOffset < 0) {
                campScrollOffsets[campIndex] = 0;
                scrollOffset = 0;
            }

            // Enable scissor
            if (ui != null) {
                context.enableScissor(
                    (int)ui.sx(x + 6),
                    (int)ui.sy(contentStartY),
                    (int)ui.sx(x + colWidth - 6),
                    (int)ui.sy(contentStartY + contentHeight)
                );
            }

            int textY = contentStartY + 10 - scrollOffset;

            // Draw items in order: Shiny → Mythics (non-shiny) → Tomes → rest by rarity
            textY = drawShinyItems(context, x, textY, items, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset);
            textY = drawMythicItems(context, x, textY, items, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset);
            textY = drawTomeItems(context, x, textY, items, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset);
            textY = drawItemsByRarity(context, x, textY, items, "Fabled", colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset);
            textY = drawItemsByRarity(context, x, textY, items, "Legendary", colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset);
            textY = drawItemsByRarity(context, x, textY, items, "Rare", colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset);
            textY = drawItemsByRarity(context, x, textY, items, "Set", colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset);
            textY = drawItemsByRarity(context, x, textY, items, "Unique", colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset);

            context.disableScissor();

            // Draw scroll bar if needed
            if (totalContentHeight > contentHeight) {
                int scrollBarHeight = Math.max(20, contentHeight * contentHeight / totalContentHeight);
                int scrollBarY = contentStartY + (scrollOffset * (contentHeight - scrollBarHeight) / maxScroll);
                drawRect(x + colWidth - 12, scrollBarY, 6, scrollBarHeight, 0xFFAAAAAA);
            }
        }
    }

    /**
     * Draw shiny items (type = shiny) - always 1 per lootpool, no count shown
     */
    private int drawShinyItems(DrawContext context, int x, int textY, List<LootrunLootPoolData.LootrunItem> items,
                                int colWidth, int mouseX, int mouseY, int contentStartY, int contentHeight, int scrollOffset) {
        int itemSpacing = 32;
        List<LootrunLootPoolData.LootrunItem> shinyItems = items.stream()
            .filter(i -> i.type.equals("shiny"))
            .toList();

        if (shinyItems.isEmpty()) return textY;

        // Draw section header - no count since always 1, rainbow text
        if (ui != null && textY + scrollOffset >= contentStartY && textY + scrollOffset <= contentStartY + contentHeight) {
            ui.drawText("✦ Shiny", x + 15, textY, CommonColors.RAINBOW, 2.5f);
        }
        textY += 25;

        for (LootrunLootPoolData.LootrunItem item : shinyItems) {
            textY = drawItem(context, x, textY, item, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset, itemSpacing);
        }
        return textY + 10;
    }

    /**
     * Draw mythic items (rarity = Mythic, but NOT shiny or tome)
     */
    private int drawMythicItems(DrawContext context, int x, int textY, List<LootrunLootPoolData.LootrunItem> items,
                                 int colWidth, int mouseX, int mouseY, int contentStartY, int contentHeight, int scrollOffset) {
        int itemSpacing = 32;
        List<LootrunLootPoolData.LootrunItem> mythicItems = items.stream()
            .filter(i -> i.rarity.equals("Mythic") && !i.type.equals("shiny") && !i.type.equals("tome"))
            .toList();

        if (mythicItems.isEmpty()) return textY;

        // Draw section header
        if (ui != null && textY + scrollOffset >= contentStartY && textY + scrollOffset <= contentStartY + contentHeight) {
            ui.drawText("§5§lMythic (" + mythicItems.size() + ")", x + 15, textY, CustomColor.fromInt(0xFFFFFF), 2.5f);
        }
        textY += 25;

        for (LootrunLootPoolData.LootrunItem item : mythicItems) {
            textY = drawItem(context, x, textY, item, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset, itemSpacing);
        }
        return textY + 10;
    }

    /**
     * Draw tome items (type = tome)
     */
    private int drawTomeItems(DrawContext context, int x, int textY, List<LootrunLootPoolData.LootrunItem> items,
                               int colWidth, int mouseX, int mouseY, int contentStartY, int contentHeight, int scrollOffset) {
        int itemSpacing = 32;
        List<LootrunLootPoolData.LootrunItem> tomeItems = items.stream()
            .filter(i -> i.type.equals("tome"))
            .toList();

        if (tomeItems.isEmpty()) return textY;

        // Draw section header
        if (ui != null && textY + scrollOffset >= contentStartY && textY + scrollOffset <= contentStartY + contentHeight) {
            ui.drawText("§d§l📖 Tomes (" + tomeItems.size() + ")", x + 15, textY, CustomColor.fromInt(0xFFFFFF), 2.5f);
        }
        textY += 25;

        for (LootrunLootPoolData.LootrunItem item : tomeItems) {
            textY = drawItem(context, x, textY, item, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset, itemSpacing);
        }
        return textY + 10;
    }

    /**
     * Draw items of a specific rarity (excluding shiny and tome)
     */
    private int drawItemsByRarity(DrawContext context, int x, int textY, List<LootrunLootPoolData.LootrunItem> items,
                                   String rarity, int colWidth, int mouseX, int mouseY, int contentStartY, int contentHeight, int scrollOffset) {
        int itemSpacing = 32;
        List<LootrunLootPoolData.LootrunItem> filteredItems = items.stream()
            .filter(i -> i.rarity.equals(rarity) && !i.type.equals("shiny") && !i.type.equals("tome"))
            .toList();

        if (filteredItems.isEmpty()) return textY;

        String rarityColor = getRarityColor(rarity);
        // Draw section header
        if (ui != null && textY + scrollOffset >= contentStartY && textY + scrollOffset <= contentStartY + contentHeight) {
            ui.drawText(rarityColor + "§l" + rarity + " (" + filteredItems.size() + ")", x + 15, textY, CustomColor.fromInt(0xFFFFFF), 2.5f);
        }
        textY += 25;

        for (LootrunLootPoolData.LootrunItem item : filteredItems) {
            textY = drawItem(context, x, textY, item, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset, itemSpacing);
        }
        return textY + 10;
    }

    /**
     * Draw a single item
     */
    private int drawItem(DrawContext context, int x, int textY, LootrunLootPoolData.LootrunItem item,
                          int colWidth, int mouseX, int mouseY, int contentStartY, int contentHeight, int scrollOffset, int itemSpacing) {
        // Only render if in visible area
        int adjustedY = textY + scrollOffset;
        if (adjustedY >= contentStartY - itemSpacing && adjustedY <= contentStartY + contentHeight + itemSpacing) {
            // Check for hover
            boolean hovering = mouseX >= x + 12 && mouseX <= x + colWidth - 12 &&
                               mouseY >= textY && mouseY <= textY + itemSpacing - 5;

            // Color based on rarity, with shiny indicator
            String rarityColor = item.type.equals("tome") ? "§d" : getRarityColor(item.rarity);
            String displayName = truncate(item.name, 40);

            if (ui != null) {
                if (item.type.equals("shiny")) {
                    // Draw shiny star in rainbow, then name
                    ui.drawText("✦ ", x + 20, textY, CommonColors.RAINBOW, 2.2f);
                    ui.drawText(rarityColor + displayName, x + 38, textY, CustomColor.fromInt(0xFFFFFF), 2.2f);
                } else {
                    ui.drawText(rarityColor + displayName, x + 20, textY, CustomColor.fromInt(0xFFFFFF), 2.2f);
                }
                // Show shiny stat below with more spacing
                if (item.type.equals("shiny") && item.shinyStat != null && !item.shinyStat.isEmpty()) {
                    ui.drawText("§7[§a" + item.shinyStat + "§7]", x + 25, textY + 18, CustomColor.fromInt(0xFFFFFF), 1.8f);
                }
            }

            if (hovering) {
                hoveredItem = item;
                hoveredItemX = mouseX;
                hoveredItemY = mouseY;
            }
        }
        // Extra spacing for shiny items with stat displayed below
        int extraSpacing = (item.type.equals("shiny") && item.shinyStat != null && !item.shinyStat.isEmpty()) ? 18 : 0;
        return textY + itemSpacing + extraSpacing;
    }

    private List<LootrunLootPoolData.LootrunItem> getLootPoolForCamp(String campCode) {
        // First try crowdsourced data
        if (crowdsourcedLootPools.containsKey(campCode) && crowdsourcedLootPools.get(campCode) != null) {
            List<LootrunLootPoolData.LootrunItem> items = crowdsourcedLootPools.get(campCode);
            if (!items.isEmpty()) {
                return items;
            }
        }
        // Fall back to local data
        return LootrunLootPoolData.INSTANCE.getLootPool(campCode);
    }

    private String getRarityColor(String rarity) {
        return switch (rarity) {
            case "Mythic" -> "§5";
            case "Fabled" -> "§c";
            case "Legendary" -> "§b";
            case "Rare" -> "§d";
            case "Set" -> "§a";
            case "Unique" -> "§e";
            default -> "§f";
        };
    }

    private void renderItemTooltip(DrawContext context, int mouseX, int mouseY) {
        if (hoveredItem == null) return;

        List<Text> tooltip = new ArrayList<>();

        String rarityColor = getRarityColor(hoveredItem.rarity);

        tooltip.add(Text.literal(rarityColor + hoveredItem.name));
        tooltip.add(Text.literal("§7Rarity: " + rarityColor + hoveredItem.rarity));
        tooltip.add(Text.literal("§7Type: §f" + capitalize(hoveredItem.type)));

        // Show shiny stat prominently
        if (hoveredItem.type.equals("shiny") && hoveredItem.shinyStat != null && !hoveredItem.shinyStat.isEmpty()) {
            tooltip.add(Text.literal(""));
            tooltip.add(Text.literal("§e✦ Shiny Stat: §a" + hoveredItem.shinyStat));
        }

        // Show full tooltip if available
        if (hoveredItem.tooltip != null && !hoveredItem.tooltip.isEmpty()) {
            tooltip.add(Text.literal(""));
            tooltip.add(Text.literal("§8--- Item Tooltip ---"));
            for (String line : hoveredItem.tooltip.split("\n")) {
                if (!line.isEmpty()) {
                    tooltip.add(Text.literal("§7" + line));
                }
            }
        }

        context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int logicalMouseX = (int)(mouseX * scaleFactor);
        int logicalMouseY = (int)(mouseY * scaleFactor);

        // Check which column the mouse is over
        for (int i = 0; i < 5; i++) {
            if (campColumnX[i] > 0 && campColumnWidth[i] > 0) {
                if (logicalMouseX >= campColumnX[i] && logicalMouseX <= campColumnX[i] + campColumnWidth[i]) {
                    int scrollAmount = (int)(-verticalAmount * 40);
                    campScrollOffsets[i] += scrollAmount;

                    // Clamp scroll
                    if (campScrollOffsets[i] < 0) campScrollOffsets[i] = 0;
                    int contentHeight = getLogicalHeight() - 150 - 150 - 110 - 12;
                    int maxScroll = Math.max(0, campContentHeights[i] - contentHeight);
                    if (campScrollOffsets[i] > maxScroll) campScrollOffsets[i] = maxScroll;

                    return true;
                }
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void drawRect(int x, int y, int width, int height, int color) {
        if (ui != null) {
            float alpha = ((color >> 24) & 0xFF) / 255f;
            int rgb = color & 0xFFFFFF;
            ui.drawRect(x, y, width, height, CustomColor.fromInt(rgb).withAlpha(alpha));
        }
    }

    private String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private String getCampDisplayName(String campCode) {
        return switch (campCode) {
            case "SI" -> "Sky Islands";
            case "SE" -> "Silent Expanse";
            case "CORK" -> "Corkus";
            case "MH" -> "Molten Heights";
            case "COTL" -> "COTL";
            default -> campCode;
        };
    }
}
