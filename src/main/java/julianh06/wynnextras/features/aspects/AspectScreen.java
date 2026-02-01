package julianh06.wynnextras.features.aspects;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

/**
 * In-game GUI screen for viewing aspect loot pools
 * Shows 4 raids side-by-side with their current loot pools
 */
public class AspectScreen extends WEScreen {

    private static final int COLUMN_WIDTH = 280;
    private static final int COLUMN_SPACING = 40;
    private static final int HEADER_HEIGHT = 55;
    private static final int ASPECT_LINE_HEIGHT = 22;

    public AspectScreen() {
        super(Text.of("WynnExtras Aspects"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        int centerX = getLogicalWidth() / 2;
        int startY = 120;

        // Title
        ui.drawCenteredText("ASPECT LOOT POOLS", centerX, 50, CustomColor.fromHexString("FFD700"), 10f);
        ui.drawCenteredText("Weekly reset: Friday 19:00 CET", centerX, 80, CustomColor.fromHexString("AAAAAA"), 4f);

        // Calculate starting X for 4 columns centered
        int totalWidth = (COLUMN_WIDTH * 4) + (COLUMN_SPACING * 3);
        int startX = (getLogicalWidth() - totalWidth) / 2;

        // Draw each raid column
        int col1X = startX;
        int col2X = startX + (COLUMN_WIDTH + COLUMN_SPACING);
        int col3X = startX + (COLUMN_WIDTH + COLUMN_SPACING) * 2;
        int col4X = startX + (COLUMN_WIDTH + COLUMN_SPACING) * 3;

        drawRaidColumn(ctx, col1X, startY, "NOTG", "Nest of the Grootslangs");
        drawRaidColumn(ctx, col2X, startY, "NOL", "Orphion's Nexus of Light");
        drawRaidColumn(ctx, col3X, startY, "TCC", "The Canyon Colossus");
        drawRaidColumn(ctx, col4X, startY, "TNA", "The Nameless Anomaly");

        // Instructions at bottom
        drawInstructions(ctx, centerX, getLogicalHeight() - 60);
    }

    private void drawRaidColumn(DrawContext ctx, int x, int y, String raidCode, String raidName) {
        LootPoolData data = LootPoolData.INSTANCE;
        List<LootPoolData.AspectEntry> aspects = data.getLootPool(raidCode);

        int panelHeight = getLogicalHeight() - y - 100;

        // Background panel
        ui.drawRect(x, y, COLUMN_WIDTH, panelHeight, CustomColor.fromHexString("1A1A1A"));
        ui.drawRectBorders(x, y, COLUMN_WIDTH, panelHeight, CustomColor.fromHexString("4e392d"));

        // Raid header
        ui.drawCenteredText(raidCode, x + COLUMN_WIDTH / 2, y + 12, CustomColor.fromHexString("FFD700"), 7f);
        ui.drawCenteredText(raidName, x + COLUMN_WIDTH / 2, y + 32, CustomColor.fromHexString("AAAAAA"), 3.5f);

        // Separator line (aligned with panel edges)
        ui.drawRect(x + 2, y + HEADER_HEIGHT, COLUMN_WIDTH - 4, 2, CustomColor.fromHexString("4e392d"));

        int aspectY = y + HEADER_HEIGHT + 12;

        if (aspects.isEmpty()) {
            // No data yet
            ui.drawCenteredText("No data", x + COLUMN_WIDTH / 2, aspectY + 40, CustomColor.fromHexString("666666"), 5f);
            ui.drawCenteredText("Open the", x + COLUMN_WIDTH / 2, aspectY + 65, CustomColor.fromHexString("666666"), 3.5f);
            ui.drawCenteredText("preview chest", x + COLUMN_WIDTH / 2, aspectY + 82, CustomColor.fromHexString("666666"), 3.5f);
            ui.drawCenteredText("to scan aspects", x + COLUMN_WIDTH / 2, aspectY + 99, CustomColor.fromHexString("666666"), 3.5f);
        } else {
            // Draw aspects grouped by rarity
            aspectY = drawAspectsByRarity(ctx, x, aspectY, aspects, "Mythic", "★");
            aspectY = drawAspectsByRarity(ctx, x, aspectY, aspects, "Fabled", "◇");
            aspectY = drawAspectsByRarity(ctx, x, aspectY, aspects, "Legendary", "◆");
        }
    }

    private int drawAspectsByRarity(DrawContext ctx, int x, int y, List<LootPoolData.AspectEntry> aspects, String rarity, String symbol) {
        // Filter aspects by rarity
        List<LootPoolData.AspectEntry> filtered = aspects.stream()
            .filter(a -> a.rarity.equalsIgnoreCase(rarity))
            .toList();

        if (filtered.isEmpty()) {
            return y;
        }

        // Get color for rarity
        CustomColor color = getRarityColor(rarity);

        // Draw each aspect
        for (LootPoolData.AspectEntry aspect : filtered) {
            String displayName = aspect.name;

            // Shorten long names to fit column
            if (displayName.length() > 32) {
                displayName = displayName.substring(0, 29) + "...";
            }

            ui.drawText(symbol + " " + displayName, x + 10, y, color, 4f);
            y += ASPECT_LINE_HEIGHT;
        }

        // Add spacing after each rarity group
        y += 8;

        return y;
    }

    private CustomColor getRarityColor(String rarity) {
        return switch (rarity.toLowerCase()) {
            case "mythic" -> CustomColor.fromHexString("AA00AA"); // Dark purple
            case "fabled" -> CustomColor.fromHexString("FF5555"); // Red
            case "legendary" -> CustomColor.fromHexString("55FFFF"); // Light blue
            default -> CustomColor.fromHexString("FFFFFF"); // White
        };
    }

    private void drawInstructions(DrawContext ctx, int centerX, int y) {
        ui.drawCenteredText("Open raid preview chests to automatically scan and save loot pools", centerX, y, CustomColor.fromHexString("AAAAAA"), 3.5f);
        ui.drawCenteredText("Switch raids inside the chest to scan all 4 pools", centerX, y + 18, CustomColor.fromHexString("AAAAAA"), 3.5f);
    }

    @Override
    public void updateValues() {
        // No dynamic sizing needed for now
    }

    public static void open() {
        WEScreen.open(AspectScreen::new);
    }
}
