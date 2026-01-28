package julianh06.wynnextras.features.aspects;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.ApiAspect;
import julianh06.wynnextras.features.profileviewer.data.Aspect;
import julianh06.wynnextras.features.profileviewer.data.User;
import julianh06.wynnextras.features.raid.RaidData;
import julianh06.wynnextras.features.raid.RaidListData;
import julianh06.wynnextras.features.raid.RaidLootConfig;
import julianh06.wynnextras.features.raid.RaidLootData;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.features.aspects.pages.*;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.wynntils.utils.wynn.ContainerUtils.clickOnSlot;

/**
 * Simple custom screen for viewing aspect loot pools and gambits
 * Page 0: Loot Pools (4 raids) - auto-navigates to party finder when clicking raid
 * Page 1: Gambits (today's 4 gambits + countdown)
 * Page 2: My Aspects (player's own aspects from API)
 */
public class AspectScreenSimple extends WEScreen implements AspectScreenHost {

    private int currentPage = 0; // 0 = Loot Pools, 1 = Gambits, 2 = My Aspects, 3 = Raid Loot, 4 = Explore, 5 = Leaderboard
    private static final int MAX_PAGE = 5; // 6 pages total

    // For tooltips
    private LootPoolData.AspectEntry hoveredAspect = null;
    private int hoveredAspectX = 0;
    private int hoveredAspectY = 0;
    private int hoveredAspectColumnX = 0; // Track which column the hovered aspect is in

    // For My Aspects page
    private User myAspectsData = null;
    private WynncraftApiHandler.FetchStatus myAspectsFetchStatus = null;
    private boolean fetchedMyAspects = false;
    private int myAspectsFetchGeneration = 0; // Track which fetch is current

    // Filters for My Aspects page
    private String classFilter = "Warrior"; // Default to Warrior, no "All" mode
    private String maxFilter = "All"; // All, Max Only, Not Max, Favorites
    private boolean showOverview = true; // Toggle between class view and overview - default to overview

    // Player search for My Aspects page
    private String searchedPlayer = ""; // Empty = show own aspects
    private User searchedPlayerData = null;
    private WynncraftApiHandler.FetchStatus searchedPlayerStatus = null;

    // Text input for player search
    private String searchInput = "";
    private boolean searchInputFocused = false;
    private int searchCursorPos = 0;
    // Recent searches are stored in FavoriteAspectsData for persistence
    private static final int MAX_RECENT_SEARCHES = 5;

    // Filter for Loot Pools page
    private boolean hideMaxInLootPools = false; // Hide maxed aspects in loot pools
    private boolean showOnlyFavoritesInLootPools = false; // Show only favorite aspects in loot pools
    private int exploreSortMode = 0; // 0 = Most Max Aspects, 1 = Username

    // Progress bar mode: true = show "max" counts, false = show "unlocked" counts
    private boolean progressBarShowMax = true;

    // Scroll offsets for each raid column (NOTG, NOL, TCC, TNA)
    private int[] raidScrollOffsets = new int[4];
    private int[] raidContentHeights = new int[4]; // Track content height for each column
    private int[] raidColumnX = new int[4]; // X positions of each column
    private int[] raidColumnWidth = new int[4]; // Width of each column
    private int raidContentStartY = 0; // Y position where content starts (below header)
    private int raidPanelHeight = 0; // Height of the content area

    // For party finder auto-navigation
    private static String pendingRaidJoin = null;

    // Debug mode for showing raid slots
    public static boolean debugMode = false;

    // Hovering for my aspects
    private ApiAspect hoveredMyAspect = null;
    private Aspect hoveredMyAspectProgress = null;

    // For Raid Loot page (page 3)
    private boolean[] raidToggles = {true, true, true, true}; // NOTG, NOL, TCC, TNA
    private boolean showRates = false; // Toggle between totals and averages

    // For Explore page (page 4)
    private List<julianh06.wynnextras.features.profileviewer.data.PlayerListEntry> playerList = null;
    private boolean fetchedPlayerList = false;

    // For Leaderboard page (page 5)
    private List<julianh06.wynnextras.features.profileviewer.data.LeaderboardEntry> leaderboardList = null;
    private boolean fetchedLeaderboard = false;

    // For Gambits page (page 2) - crowdsourced data
    private List<julianh06.wynnextras.features.aspects.GambitData.GambitEntry> crowdsourcedGambits = null;
    private boolean fetchedCrowdsourcedGambits = false;

    // For Loot Pools page (page 0) - crowdsourced data per raid
    private java.util.Map<String, List<julianh06.wynnextras.features.aspects.LootPoolData.AspectEntry>> crowdsourcedLootPools = new java.util.HashMap<>();
    private boolean fetchedCrowdsourcedLootPools = false;

    // Player's personal aspect progress (name -> amount + rarity)
    private java.util.Map<String, com.mojang.datafixers.util.Pair<Integer, String>> personalAspectProgress = new java.util.HashMap<>();
    private boolean fetchedPersonalProgress = false;

    public AspectScreenSimple() {
        super(Text.of("WynnExtras Aspects"));
    }

    // Page instances (for extracted pages)
    private GambitsPage gambitsPage;
    private RaidLootPage raidLootPage;
    private ExplorePage explorePage;
    private LeaderboardPage leaderboardPage;

    @Override
    protected void init() {
        super.init();

        // Initialize page instances
        if (gambitsPage == null) {
            gambitsPage = new GambitsPage(this);
            raidLootPage = new RaidLootPage(this);
            explorePage = new ExplorePage(this);
            leaderboardPage = new LeaderboardPage(this);
        }
    }

    // For showing import result feedback
    private String importFeedback = null;
    private long importFeedbackTime = 0;

    // ===== TEXT RENDERING HELPERS (using WEScreen UIUtils) =====

    private void drawCenteredText(DrawContext context, String text, int x, int y) {
        if (ui != null) {
            ui.drawCenteredText(text, x, y, CustomColor.fromInt(0xFFFFFF), 3f);
        }
    }

    private void drawLeftText(DrawContext context, String text, int x, int y) {
        if (ui != null) {
            ui.drawText(text, x, y, CustomColor.fromInt(0xFFFFFF), 3f);
        }
    }

    /**
     * Calculate a safe column width based on logical screen width
     */
    private int getSafeColumnWidth(int numColumns, int spacing) {
        int margin = 40;
        int availableWidth = getLogicalWidth() - (margin * 2);
        int totalSpacing = spacing * (numColumns - 1);
        int calculatedWidth = (availableWidth - totalSpacing) / numColumns;
        return Math.max(350, calculatedWidth);
    }

    /**
     * Get aspect spacing in logical units
     */
    private int getAspectSpacing() {
        return 40;
    }

    /**
     * Get panel bottom margin for raid columns in logical units
     */
    private int getRaidPanelBottomMargin() {
        return 200;
    }

    /**
     * Get panel bottom margin for class columns in logical units
     */
    private int getClassPanelBottomMargin() {
        return 150;
    }

    /**
     * Get starting Y position for loot pools/raid columns in logical units
     */
    private int getLootPoolStartY() {
        return 180;
    }

    /**
     * Get filter button Y position in logical units
     */
    private int getFilterButtonY() {
        return 160;
    }

    /**
     * Get starting Y position for My Aspects page - consistent in logical units
     */
    private int getMyAspectsStartY() {
        return 220; // Consistent in logical units
    }

    /**
     * Helper to draw a filled rectangle using UIUtils
     */
    private void drawRect(int x, int y, int width, int height, int color) {
        if (ui != null) {
            // Extract alpha and RGB from ARGB color
            float alpha = ((color >> 24) & 0xFF) / 255f;
            int rgb = color & 0xFFFFFF;
            ui.drawRect(x, y, width, height, CustomColor.fromInt(rgb).withAlpha(alpha));
        }
    }

    @Override
    protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // WEScreen handles background

        // Draw "WYNNEXTRAS" in bold dark green at the very top center
        int centerX = getLogicalWidth() / 2;
        drawCenteredText(context, "§2§lWYNNEXTRAS", centerX, 20);

        // Convert mouse coords to logical
        int logicalMouseX = (int)(mouseX * scaleFactor);
        int logicalMouseY = (int)(mouseY * scaleFactor);

        // Draw "Import Wynntils Favorites" button on Loot Pools and My Aspects pages (top left)
        if (currentPage == 0 || currentPage == 1) {
            drawImportWynntilsButton(context, logicalMouseX, logicalMouseY);
        }

        if (currentPage == 0) {
            renderLootPoolsPage(context, mouseX, mouseY);
        } else if (currentPage == 1) {
            renderMyAspectsPage(context, mouseX, mouseY);
        } else if (currentPage == 2) {
            // Use extracted GambitsPage
            gambitsPage.setUi(ui);
            gambitsPage.setPlayerData(myAspectsData);
            gambitsPage.render(context, mouseX, mouseY, delta);
        } else if (currentPage == 3) {
            // Use extracted RaidLootPage
            raidLootPage.setUi(ui);
            raidLootPage.render(context, mouseX, mouseY, delta);
        } else if (currentPage == 4) {
            // Use extracted ExplorePage
            explorePage.setUi(ui);
            explorePage.render(context, mouseX, mouseY, delta);
        } else if (currentPage == 5) {
            // Use extracted LeaderboardPage
            leaderboardPage.setUi(ui);
            leaderboardPage.render(context, mouseX, mouseY, delta);
        }

        // Page indicator and arrows (on top)
        renderNavigation(context, mouseX, mouseY);

        // Render tooltip if hovering over aspect
        if (hoveredAspect != null) {
            renderAspectTooltip(context, mouseX, mouseY);
        }
    }

    private void renderLootPoolsPage(DrawContext context, int mouseX, int mouseY) {
        // Reset hovered aspect at start of frame
        hoveredAspect = null;

        int logicalW = getLogicalWidth();
        int logicalH = getLogicalHeight();
        int centerX = logicalW / 2;

        // Fetch crowdsourced loot pools from API on first load
        if (!fetchedCrowdsourcedLootPools) {
            fetchedCrowdsourcedLootPools = true;
            String[] raids = {"NOTG", "NOL", "TCC", "TNA"};
            for (String raidType : raids) {
                WynncraftApiHandler.fetchCrowdsourcedLootPool(raidType).thenAccept(result -> {
                    if (result != null && !result.isEmpty()) {
                        crowdsourcedLootPools.put(raidType, result);
                        System.out.println("[WynnExtras] Fetched " + result.size() + " crowdsourced aspects for " + raidType);
                        // Save to local data for offline access
                        julianh06.wynnextras.features.aspects.LootPoolData.INSTANCE.saveLootPoolFull(raidType, result);
                    } else {
                        System.out.println("[WynnExtras] No crowdsourced loot pool for " + raidType);
                    }
                });
            }
        }

        // Fetch player's personal aspect progress for overlay
        if (!fetchedPersonalProgress && McUtils.player() != null) {
            fetchedPersonalProgress = true;
            String playerUUID = McUtils.player().getUuidAsString();
            WynncraftApiHandler.fetchPlayerAspectData(playerUUID, playerUUID).thenAccept(result -> {
                if (result != null && result.status() == WynncraftApiHandler.FetchStatus.OK && result.user() != null) {
                    julianh06.wynnextras.features.profileviewer.data.User userData = result.user();
                    // Convert aspect data to progress map (name -> (amount, rarity))
                    java.util.List<julianh06.wynnextras.features.profileviewer.data.Aspect> aspects = userData.getAspects();
                    if (aspects != null) {
                        for (julianh06.wynnextras.features.profileviewer.data.Aspect aspect : aspects) {
                            personalAspectProgress.put(aspect.getName(),
                                new com.mojang.datafixers.util.Pair<>(aspect.getAmount(), aspect.getRarity()));
                        }
                        System.out.println("[WynnExtras] Fetched personal progress for " + personalAspectProgress.size() + " aspects");
                    }
                }
            });
        }

        // Title
        drawCenteredText(context, "§6§lASPECT LOOT POOLS", centerX, 60);

        // Calculate next Friday 19:00 CET
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));
        ZonedDateTime nextReset = now.with(java.time.DayOfWeek.FRIDAY).withHour(19).withMinute(0).withSecond(0).withNano(0);
        if (nextReset.isBefore(now) || nextReset.isEqual(now)) {
            nextReset = nextReset.plusWeeks(1);
        }

        // Calculate time difference
        Duration duration = Duration.between(now, nextReset);
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        // Format as "Friday (in xx days xx hours xx minutes)"
        String countdown = String.format("Friday (in §e%d§7 days §e%d§7 hours §e%d§7 minutes)", days, hours, minutes);
        drawCenteredText(context, "§7Weekly reset: " + countdown, centerX, 110);

        // Toggle buttons (in logical coords)
        int toggleWidth = 200;
        int favToggleWidth = 320; // Wider for favorites
        int toggleHeight = 44;
        int toggleSpacing = 15;
        int toggleY = 14;
        // Convert mouse to logical for hover check
        int logicalMouseX = (int)(mouseX * scaleFactor);
        int logicalMouseY = (int)(mouseY * scaleFactor);

        // Favorite toggle (left of Hide Max)
        int favToggleX = logicalW - toggleWidth - favToggleWidth - toggleSpacing - 60;
        boolean hoverFavToggle = logicalMouseX >= favToggleX && logicalMouseX <= favToggleX + favToggleWidth && logicalMouseY >= toggleY && logicalMouseY <= toggleY + toggleHeight;

        // Draw textured button
        if (ui != null) {
            ui.drawButtonFade(favToggleX, toggleY, favToggleWidth, toggleHeight, 12, hoverFavToggle || showOnlyFavoritesInLootPools);
        }

        String favToggleText = showOnlyFavoritesInLootPools ? "§e§l⭐ Favorites Only" : "§7§l☆ Favorites Only";
        drawCenteredText(context, favToggleText, favToggleX + favToggleWidth / 2, toggleY + toggleHeight / 2);

        // Hide Max toggle button
        int toggleX = logicalW - toggleWidth - 60;
        boolean hoverToggle = logicalMouseX >= toggleX && logicalMouseX <= toggleX + toggleWidth && logicalMouseY >= toggleY && logicalMouseY <= toggleY + toggleHeight;

        // Draw textured button
        if (ui != null) {
            ui.drawButtonFade(toggleX, toggleY, toggleWidth, toggleHeight, 12, hoverToggle || hideMaxInLootPools);
        }

        String toggleText = hideMaxInLootPools ? "§6§lShow Max" : "§7§lHide Max";
        drawCenteredText(context, toggleText, toggleX + toggleWidth / 2, toggleY + toggleHeight / 2);

        // 4 columns for raids - using logical dimensions
        int startY = getLootPoolStartY();
        int columnSpacing = 30; // Spacing in logical units
        int columnWidth = getSafeColumnWidth(4, columnSpacing);
        int totalWidth = (columnWidth * 4) + (columnSpacing * 3);
        int startX = (logicalW - totalWidth) / 2;

        String[] raids = {"NOTG", "NOL", "TCC", "TNA"};
        String[] raidNames = {
            "Nest of the Grootslangs",
            "Orphion's Nexus of Light",
            "The Canyon Colossus",
            "The Nameless Anomaly"
        };

        // Calculate aligned separator positions
        int[] rarityMaxCounts = calculateMaxRarityCounts(raids);
        int spacing = getAspectSpacing();
        int mythicSeparatorY = startY + 150 + (rarityMaxCounts[0] * spacing) + 10;
        int fabledSeparatorY = mythicSeparatorY + (rarityMaxCounts[1] * spacing) + 25;

        // Store column info for scroll detection (in logical coords)
        raidContentStartY = startY + 150; // Content starts below header
        raidPanelHeight = logicalH - startY - getRaidPanelBottomMargin() - 150; // Content area height

        // FIRST PASS: Check hover for all columns before any drawing
        // This ensures hoveredAspect is set correctly before we draw
        for (int i = 0; i < 4; i++) {
            int x = startX + (i * (columnWidth + columnSpacing));
            raidColumnX[i] = x;
            raidColumnWidth[i] = columnWidth;
            checkRaidColumnHover(x, startY, raids[i], columnWidth, logicalMouseX, logicalMouseY, mythicSeparatorY, fabledSeparatorY, i);
        }

        // SECOND PASS: Draw all columns (now hoveredAspect is already set correctly)
        for (int i = 0; i < 4; i++) {
            int x = startX + (i * (columnWidth + columnSpacing));
            drawRaidColumn(context, x, startY, raids[i], raidNames[i], columnWidth, logicalMouseX, logicalMouseY, mythicSeparatorY, fabledSeparatorY, i);
        }

        // Instructions above navigation - only show if no data
        boolean hasAnyData = LootPoolData.INSTANCE.hasData("NOTG") || LootPoolData.INSTANCE.hasData("NOL")
                          || LootPoolData.INSTANCE.hasData("TCC") || LootPoolData.INSTANCE.hasData("TNA");
        if (!hasAnyData) {
            drawCenteredText(context, "§7Open raid preview chests to scan loot pools", centerX, logicalH - 165);
        }
    }

    /**
     * Calculate maximum count for each rarity across all raids
     * Returns [mythicMax, fabledMax, legendaryMax]
     */
    /**
     * Get loot pool for a raid, prioritizing crowdsourced data over local data
     * Overlays player's personal progress on crowdsourced aspects
     */
    private List<LootPoolData.AspectEntry> getLootPoolForRaid(String raidCode) {
        // Use crowdsourced data if available
        if (crowdsourcedLootPools.containsKey(raidCode)) {
            List<LootPoolData.AspectEntry> crowdsourced = crowdsourcedLootPools.get(raidCode);
            if (crowdsourced != null && !crowdsourced.isEmpty()) {
                // Overlay personal progress on crowdsourced data
                List<LootPoolData.AspectEntry> withProgress = new java.util.ArrayList<>();
                for (LootPoolData.AspectEntry aspect : crowdsourced) {
                    // Check if we have personal progress for this aspect
                    if (personalAspectProgress.containsKey(aspect.name)) {
                        com.mojang.datafixers.util.Pair<Integer, String> progress = personalAspectProgress.get(aspect.name);
                        int amount = progress.getFirst();
                        String rarity = progress.getSecond();

                        // Convert amount to tier info string
                        String tierInfo = convertAmountToTierInfo(amount, rarity);

                        // Create new entry with personal tier info
                        withProgress.add(new LootPoolData.AspectEntry(aspect.name, rarity, tierInfo, aspect.description));
                    } else {
                        // No personal data, use crowdsourced as-is (will show without tier)
                        withProgress.add(aspect);
                    }
                }
                return withProgress;
            }
        }
        // Fall back to local data
        return LootPoolData.INSTANCE.getLootPool(raidCode);
    }

    /**
     * Convert aspect amount to tier info display string
     * Reverse of parseAspectAmount logic
     */
    private String convertAmountToTierInfo(int amount, String rarity) {
        int[] tier1 = {1, 1, 1};
        int[] tier2 = {4, 14, 4};
        int[] tier3 = {10, 60, 25};
        int[] tier4 = {0, 0, 120};

        int rarityIndex;
        switch (rarity) {
            case "Mythic" -> rarityIndex = 0;
            case "Fabled" -> rarityIndex = 1;
            case "Legendary" -> rarityIndex = 2;
            default -> {
                return "";
            }
        }

        int maxAmount = tier1[rarityIndex] + tier2[rarityIndex] + tier3[rarityIndex] + tier4[rarityIndex];
        if (amount >= maxAmount) {
            return "[MAX]";
        }

        int tier1Total = tier1[rarityIndex];
        int tier2Total = tier1Total + tier2[rarityIndex];
        int tier3Total = tier2Total + tier3[rarityIndex];

        if (amount < tier1Total) {
            return "Tier I [" + amount + "/" + tier1[rarityIndex] + "]";
        } else if (amount < tier2Total) {
            int progress = amount - tier1Total;
            return "Tier II [" + progress + "/" + tier2[rarityIndex] + "]";
        } else if (amount < tier3Total) {
            int progress = amount - tier2Total;
            return "Tier III [" + progress + "/" + tier3[rarityIndex] + "]";
        } else {
            int progress = amount - tier3Total;
            return "Tier IV [" + progress + "/" + tier4[rarityIndex] + "]";
        }
    }

    private int[] calculateMaxRarityCounts(String[] raids) {
        int maxMythic = 0;
        int maxFabled = 0;
        int maxLegendary = 0;

        for (String raid : raids) {
            List<LootPoolData.AspectEntry> aspects = getLootPoolForRaid(raid);

            int mythicCount = (int) aspects.stream().filter(a -> a.rarity.equalsIgnoreCase("Mythic")).count();
            int fabledCount = (int) aspects.stream().filter(a -> a.rarity.equalsIgnoreCase("Fabled")).count();
            int legendaryCount = (int) aspects.stream().filter(a -> a.rarity.equalsIgnoreCase("Legendary")).count();

            maxMythic = Math.max(maxMythic, mythicCount);
            maxFabled = Math.max(maxFabled, fabledCount);
            maxLegendary = Math.max(maxLegendary, legendaryCount);
        }

        return new int[]{maxMythic, maxFabled, maxLegendary};
    }

    private void drawRaidColumn(DrawContext context, int x, int y, String raidCode, String raidName, int colWidth, int mouseX, int mouseY, int mythicSeparatorY, int fabledSeparatorY, int raidIndex) {
        int logicalH = getLogicalHeight();
        int panelHeight = logicalH - y - getRaidPanelBottomMargin();

        // Background box (using UIUtils for proper scaling)
        drawRect(x, y, colWidth, panelHeight, 0xAA000000);

        // Border
        drawRect(x, y, colWidth, 6, 0xFF4e392d); // Top
        drawRect(x, y + panelHeight - 6, colWidth, 6, 0xFF4e392d); // Bottom
        drawRect(x, y, 6, panelHeight, 0xFF4e392d); // Left
        drawRect(x + colWidth - 6, y, 6, panelHeight, 0xFF4e392d); // Right

        // Check if hovering over raid header (mouseX/Y are already in logical coords)
        int headerHeight = 120;
        boolean hoveringHeader = mouseX >= x && mouseX <= x + colWidth &&
                                 mouseY >= y && mouseY <= y + headerHeight;

        // Raid header (centered)
        drawCenteredText(context, "§6§l" + raidCode, x + colWidth / 2, y + 25);
        drawCenteredText(context, "§7" + truncate(raidName, 30), x + colWidth / 2, y + 65);

        // Get aspects (prioritize crowdsourced data)
        List<LootPoolData.AspectEntry> aspects = getLootPoolForRaid(raidCode);
        boolean usingCrowdsourced = crowdsourcedLootPools.containsKey(raidCode) &&
                                     crowdsourcedLootPools.get(raidCode) != null &&
                                     !crowdsourcedLootPools.get(raidCode).isEmpty();

        // Calculate and show score
        double score = calculateRaidScore(aspects);
        // Check if all aspects are actually maxed (have valid tierInfo that indicates max)
        boolean allMaxed = !aspects.isEmpty() && aspects.stream().allMatch(a ->
            a.tierInfo != null && (a.tierInfo.isEmpty() || a.tierInfo.contains("[MAX]"))
        );
        if (allMaxed && ui != null) {
            // Rainbow text for MAXED
            ui.drawCenteredText("MAXED", x + colWidth / 2, y + 100, CommonColors.RAINBOW, 3f);
        } else {
            String scoreText = String.format("§7Score: §e%.2f", score);
            drawCenteredText(context, scoreText, x + colWidth / 2, y + 100);
        }

        // Show data source indicator
        if (!aspects.isEmpty()) {
            String dataSource = usingCrowdsourced ? "§a§o(Crowdsourced)" : "§e§o(Local)";
            drawCenteredText(context, dataSource, x + colWidth / 2, y + 120);
        }

        // Separator line (more space below score)
        drawRect(x + 12, y + 135, colWidth - 24, 6, 0xFF4e392d);

        // Render tooltip if hovering (convert back to screen coords for tooltip)
        if (hoveringHeader && ui != null) {
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(Text.literal("§6§l" + raidCode));
            tooltip.add(Text.literal("§7" + raidName));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.literal("§7Score: §e" + String.format("%.2f", score)));
            tooltip.add(Text.literal("§8(Favorites count 3x)"));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.literal("§aClick to join party finder"));
            context.drawTooltip(textRenderer, tooltip, (int)ui.sx(mouseX), (int)ui.sy(mouseY));
        }

        int contentStartY = y + 150; // Where content starts (below header)
        int contentHeight = panelHeight - 150 - 12; // Available height for content
        int scrollOffset = raidScrollOffsets[raidIndex];

        if (aspects.isEmpty()) {
            drawCenteredText(context, "§7No data", x + colWidth / 2, contentStartY + 60);
            drawCenteredText(context, "§7Open preview", x + colWidth / 2, contentStartY + 110);
            drawCenteredText(context, "§7chest to scan", x + colWidth / 2, contentStartY + 160);
            raidContentHeights[raidIndex] = 0; // No scrollable content
        } else {
            // Calculate total content height first
            int totalContentHeight = calculateTotalContentHeight(aspects);
            raidContentHeights[raidIndex] = totalContentHeight;

            // Clamp scroll offset
            int maxScroll = Math.max(0, totalContentHeight - contentHeight);
            if (scrollOffset > maxScroll) {
                raidScrollOffsets[raidIndex] = maxScroll;
                scrollOffset = maxScroll;
            }
            if (scrollOffset < 0) {
                raidScrollOffsets[raidIndex] = 0;
                scrollOffset = 0;
            }

            // Enable scissor to clip content to panel area (convert to screen coords)
            if (ui != null) {
                context.enableScissor(
                    (int)ui.sx(x + 6),
                    (int)ui.sy(contentStartY - 12),
                    (int)ui.sx(x + colWidth - 6),
                    (int)ui.sy(contentStartY + contentHeight)
                );
            }

            // Apply scroll offset to content Y (start 12px lower to give room for first flame)
            int textY = contentStartY + 12 - scrollOffset;

            // Draw aspects by rarity (hover is already checked in pre-pass)
            textY = drawAspectsByRarity(context, x, textY, aspects, "Mythic", "", "§5", colWidth);

            // Draw separator line after mythic (adjusted for scroll)
            int adjustedMythicSepY = mythicSeparatorY - scrollOffset;
            if (adjustedMythicSepY >= contentStartY && adjustedMythicSepY <= contentStartY + contentHeight) {
                drawRect(x + 24, adjustedMythicSepY, colWidth - 48, 3, 0xFF4e392d);
            }
            textY = mythicSeparatorY + 20 - scrollOffset; // Position for fabled start

            textY = drawAspectsByRarity(context, x, textY, aspects, "Fabled", "", "§c", colWidth);

            // Draw separator line after fabled (adjusted for scroll)
            int adjustedFabledSepY = fabledSeparatorY - scrollOffset;
            if (adjustedFabledSepY >= contentStartY && adjustedFabledSepY <= contentStartY + contentHeight) {
                drawRect(x + 24, adjustedFabledSepY, colWidth - 48, 3, 0xFF4e392d);
            }
            textY = fabledSeparatorY + 20 - scrollOffset; // Position for legendary start

            textY = drawAspectsByRarity(context, x, textY, aspects, "Legendary", "", "§b", colWidth);

            // Disable scissor
            context.disableScissor();

            // Draw scroll bar if content is scrollable
            if (totalContentHeight > contentHeight) {
                drawScrollBar(context, x + colWidth - 18, contentStartY, 12, contentHeight, scrollOffset, totalContentHeight);
            }
        }
    }

    /**
     * Pre-pass to check hover state for a raid column before drawing
     * This ensures hoveredAspect is set before any column is drawn
     */
    private void checkRaidColumnHover(int x, int y, String raidCode, int colWidth, int mouseX, int mouseY, int mythicSeparatorY, int fabledSeparatorY, int raidIndex) {
        int logicalH = getLogicalHeight();
        int panelHeight = logicalH - y - getRaidPanelBottomMargin();

        // Get aspects (same logic as drawRaidColumn)
        List<LootPoolData.AspectEntry> aspects = getLootPoolForRaid(raidCode);

        if (!aspects.isEmpty()) {
            int contentStartY = y + 150;
            int contentHeight = panelHeight - 150 - 10;
            int scrollOffset = raidScrollOffsets[raidIndex];

            // Check hover for all rarities
            checkAspectHover(mouseX, mouseY, x, contentStartY + 12 - scrollOffset, aspects, "Mythic", "", colWidth);

            int fabledY = mythicSeparatorY + 20;
            checkAspectHover(mouseX, mouseY, x, fabledY - scrollOffset, aspects, "Fabled", "", colWidth);

            int legendaryY = fabledSeparatorY + 20;
            checkAspectHover(mouseX, mouseY, x, legendaryY - scrollOffset, aspects, "Legendary", "", colWidth);
        }
    }

    /**
     * Calculate total content height for a raid column
     */
    private int calculateTotalContentHeight(List<LootPoolData.AspectEntry> aspects) {
        int spacing = getAspectSpacing();
        int height = 0;

        // Mythic
        int mythicCount = (int) aspects.stream()
            .filter(a -> a.rarity.equalsIgnoreCase("Mythic"))
            .filter(a -> !hideMaxInLootPools || a.tierInfo == null || !a.tierInfo.contains("[MAX]"))
            .filter(a -> !showOnlyFavoritesInLootPools || FavoriteAspectsData.INSTANCE.isFavorite(a.name))
            .count();
        if (mythicCount > 0) {
            height += mythicCount * spacing + 6; // Aspects + spacing between groups
        }

        // Separator
        height += 10; // Separator line spacing

        // Fabled
        int fabledCount = (int) aspects.stream()
            .filter(a -> a.rarity.equalsIgnoreCase("Fabled"))
            .filter(a -> !hideMaxInLootPools || a.tierInfo == null || !a.tierInfo.contains("[MAX]"))
            .filter(a -> !showOnlyFavoritesInLootPools || FavoriteAspectsData.INSTANCE.isFavorite(a.name))
            .count();
        if (fabledCount > 0) {
            height += fabledCount * spacing + 6;
        }

        // Separator
        height += 10;

        // Legendary
        int legendaryCount = (int) aspects.stream()
            .filter(a -> a.rarity.equalsIgnoreCase("Legendary"))
            .filter(a -> !hideMaxInLootPools || a.tierInfo == null || !a.tierInfo.contains("[MAX]"))
            .filter(a -> !showOnlyFavoritesInLootPools || FavoriteAspectsData.INSTANCE.isFavorite(a.name))
            .count();
        if (legendaryCount > 0) {
            height += legendaryCount * spacing + 6;
        }

        return height;
    }

    /**
     * Draw a scroll bar indicator
     */
    private void drawScrollBar(DrawContext context, int x, int y, int barWidth, int barHeight, int scrollOffset, int totalContentHeight) {
        // Background track (in logical coords)
        drawRect(x, y, barWidth, barHeight, 0x40FFFFFF);

        // Calculate thumb size and position
        float visibleRatio = (float) barHeight / totalContentHeight;
        int thumbHeight = Math.max(60, (int)(barHeight * visibleRatio)); // Min 60 in logical units
        int maxScroll = totalContentHeight - barHeight;
        float scrollRatio = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0;
        int thumbY = y + (int)((barHeight - thumbHeight) * scrollRatio);

        // Thumb
        drawRect(x, thumbY, barWidth, thumbHeight, 0xFFAAAAAA);
    }

    private int drawAspectsByRarity(DrawContext context, int x, int y, List<LootPoolData.AspectEntry> aspects, String rarity, String prefix, String color, int colWidth) {
        List<LootPoolData.AspectEntry> filtered = aspects.stream()
            .filter(a -> a.rarity.equalsIgnoreCase(rarity))
            .filter(a -> !hideMaxInLootPools || a.tierInfo == null || !a.tierInfo.contains("[MAX]"))
            .filter(a -> !showOnlyFavoritesInLootPools || FavoriteAspectsData.INSTANCE.isFavorite(a.name))
            .toList();

        if (filtered.isEmpty()) return y;

        // Truncation - 32 chars max
        int maxChars = 32;

        for (LootPoolData.AspectEntry aspect : filtered) {
            // Check if aspect is maxed
            boolean isMaxed = aspect.tierInfo != null && aspect.tierInfo.contains("[MAX]");

            // Truncate by character count - always truncate if longer than maxChars
            String displayName = aspect.name;
            if (displayName.length() > maxChars) {
                displayName = displayName.substring(0, maxChars - 3) + "...";
            }
            String displayText = prefix.isEmpty() ? displayName : prefix + " " + displayName;

            // Check if favorited
            boolean isFavorite = FavoriteAspectsData.INSTANCE.isFavorite(aspect.name);

            // Check if this specific aspect instance is being hovered (same name AND same column)
            boolean isHovered = hoveredAspect != null && hoveredAspect.name.equals(aspect.name) && hoveredAspectColumnX == x;

            // Show star: filled if favorite, empty if hovered but not favorite, nothing otherwise
            String favoriteStar;
            if (isFavorite) {
                favoriteStar = " §e⭐";
            } else if (isHovered) {
                favoriteStar = " §7☆";
            } else {
                favoriteStar = "";
            }

            // Draw text at y position - rainbow for maxed aspects
            if (isMaxed && ui != null) {
                ui.drawText(displayText + favoriteStar, x + 55, y, CommonColors.RAINBOW, 3f);
            } else {
                drawLeftText(context, color + displayText + favoriteStar, x + 55, y);
            }

            // Draw flame icon
            ApiAspect apiAspect = findApiAspectByName(aspect.name);
            ItemStack flameItem = createAspectFlameIcon(apiAspect, isMaxed);
            if (!flameItem.isEmpty() && ui != null) {
                int screenX = (int)ui.sx(x + 20);
                int screenY = (int)ui.sy(y); // Move flame up
                float flameScale = 0.6f;
                context.getMatrices().pushMatrix();
                context.getMatrices().scale(flameScale, flameScale);
                context.drawItem(flameItem, (int)(screenX / flameScale), (int)(screenY / flameScale));
                context.getMatrices().popMatrix();
            }

            y += getAspectSpacing();
        }

        y += 15; // Spacing between rarity groups
        return y;
    }

    // Separate method to check hover (called with logical mouse coords)
    private void checkAspectHover(int mouseX, int mouseY, int x, int y, List<LootPoolData.AspectEntry> aspects, String rarity, String prefix, int colWidth) {
        List<LootPoolData.AspectEntry> filtered = aspects.stream()
            .filter(a -> a.rarity.equalsIgnoreCase(rarity))
            .filter(a -> !hideMaxInLootPools || a.tierInfo == null || !a.tierInfo.contains("[MAX]"))
            .filter(a -> !showOnlyFavoritesInLootPools || FavoriteAspectsData.INSTANCE.isFavorite(a.name))
            .toList();

        if (filtered.isEmpty()) return;

        int currentY = y; // Align with text position
        int lineHeight = getAspectSpacing();

        for (LootPoolData.AspectEntry aspect : filtered) {
            // Use full column width for hover detection (much easier to hit)
            if (mouseX >= x + 20 && mouseX <= x + colWidth - 20 &&
                mouseY >= currentY - 3 && mouseY <= currentY + lineHeight + 3) {
                hoveredAspect = aspect;
                hoveredAspectX = mouseX;
                hoveredAspectY = mouseY;
                hoveredAspectColumnX = x; // Track which column this aspect is in
                return;
            }
            currentY += lineHeight;
        }
    }

    private void renderAspectTooltip(DrawContext context, int mouseX, int mouseY) {
        if (hoveredAspect == null) return;

        List<Text> tooltipLines = new ArrayList<>();

        // Rarity color
        String rarityColor = switch(hoveredAspect.rarity.toLowerCase()) {
            case "mythic" -> "§5";
            case "fabled" -> "§c";
            case "legendary" -> "§b";
            default -> "§f";
        };

        tooltipLines.add(Text.literal(rarityColor + hoveredAspect.name));

        if (hoveredAspect.tierInfo != null && !hoveredAspect.tierInfo.isEmpty()) {
            // Strip Wynncraft symbols and colorize tier info
            String cleanTierInfo = stripWynncraftSymbols(hoveredAspect.tierInfo);
            String coloredTierInfo = colorizeTierInfo(cleanTierInfo, rarityColor);
            tooltipLines.add(Text.literal(coloredTierInfo));
        }

        // Try to get description from API if saved description is empty
        String description = hoveredAspect.description;
        if (description == null || description.isEmpty()) {
            // Fetch from API
            try {
                List<ApiAspect> allAspects = WynncraftApiHandler.fetchAllAspects();
                if (allAspects != null && !allAspects.isEmpty()) {
                    for (ApiAspect apiAspect : allAspects) {
                        if (apiAspect != null && apiAspect.getName().equals(hoveredAspect.name)) {
                            // Get description for current tier (not first tier)
                            description = getDescriptionForCurrentTierFromTierInfo(apiAspect, hoveredAspect.tierInfo);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // Silently fail - just won't show description
            }
        }

        if (description != null && !description.isEmpty()) {
            tooltipLines.add(Text.literal(""));
            // Strip symbols and handle multi-line descriptions
            String cleanDesc = stripWynncraftSymbols(description);

            // Split by newlines first (for multiple features)
            String[] descriptionLines = cleanDesc.split("\n");
            for (String descLine : descriptionLines) {
                if (descLine.trim().isEmpty()) continue;

                // Word wrap each line individually
                List<String> wrappedLines = wrapText(descLine.trim(), 200);
                for (String line : wrappedLines) {
                    tooltipLines.add(Text.literal("§7" + line));
                }
            }
        }

        context.drawTooltip(textRenderer, tooltipLines, mouseX, mouseY);
    }

    private String getDescriptionFromApiAspect(ApiAspect aspect) {
        if (aspect.getTiers() == null || aspect.getTiers().isEmpty()) return "";

        // Get the first tier (lowest tier) description
        ApiAspect.Tier firstTier = null;
        int lowestThreshold = Integer.MAX_VALUE;

        for (Map.Entry<String, ApiAspect.Tier> entry : aspect.getTiers().entrySet()) {
            int threshold = entry.getValue().getThreshold();
            if (threshold < lowestThreshold) {
                lowestThreshold = threshold;
                firstTier = entry.getValue();
            }
        }

        if (firstTier != null && firstTier.getDescription() != null && !firstTier.getDescription().isEmpty()) {
            return String.join("\n", firstTier.getDescription());
        }

        return "";
    }

    private String getDescriptionForCurrentTierFromTierInfo(ApiAspect aspect, String tierInfo) {
        if (aspect.getTiers() == null || aspect.getTiers().isEmpty()) return "";

        // Collect ALL descriptions from all tiers to show complete feature list
        StringBuilder allDescriptions = new StringBuilder();

        // Try to get the highest tier's description (which usually has all features)
        int maxTierNum = 0;
        ApiAspect.Tier maxTier = null;

        for (Map.Entry<String, ApiAspect.Tier> entry : aspect.getTiers().entrySet()) {
            String tierKey = entry.getKey();
            // Extract tier number from "tier1", "tier2", etc.
            try {
                int tierNum = Integer.parseInt(tierKey.replace("tier", "").trim());
                if (tierNum > maxTierNum) {
                    maxTierNum = tierNum;
                    maxTier = entry.getValue();
                }
            } catch (NumberFormatException e) {
                // Try roman numerals
                String tierStr = tierKey.replace("tier", "").trim();
                int tierNum = romanToInt(tierStr);
                if (tierNum > maxTierNum) {
                    maxTierNum = tierNum;
                    maxTier = entry.getValue();
                }
            }
        }

        // Use highest tier description (usually most complete)
        if (maxTier != null && maxTier.getDescription() != null && !maxTier.getDescription().isEmpty()) {
            return String.join("\n", maxTier.getDescription());
        }

        // Fallback to first tier
        return getDescriptionFromApiAspect(aspect);
    }

    /**
     * Strip Wynncraft custom font characters, formatting codes, and HTML tags
     */
    private String stripWynncraftSymbols(String text) {
        if (text == null) return "";

        // Remove Unicode private use area characters (U+E000 to U+F8FF, U+F0000 to U+FFFFD, U+100000 to U+10FFFD)
        // Also remove surrogate pairs that form these characters
        return text.replaceAll("[\\uE000-\\uF8FF]", "")
                   .replaceAll("[\\uDB40-\\uDBFF][\\uDC00-\\uDFFF]", "")
                   .replaceAll("[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]", "")
                   .replaceAll("\\{[^}]*\\}", "") // Remove {Music}, {Bloodpool}, etc.
                   .replaceAll("§.", "") // Remove Minecraft formatting codes (§a, §7, etc.)
                   .replaceAll("<[^>]*>", "") // Remove HTML tags like <span class="...">, </span>, etc.
                   .replaceAll("&[a-zA-Z]+;", "") // Remove HTML entities like &nbsp;, &amp;, etc.
                   .trim();
    }

    /**
     * Colorize tier info: "Tier I >>>>>>>>>> TIER II [8/10]"
     * - "Tier I" = gray (lower tier)
     * - ">>>>>>>>" = partially green based on progress (8/10 = 80% green, 20% gray)
     * - "TIER II" = aspect color (target tier)
     * - "[8/10]" = gray
     * - "[MAX]" = aspect color
     */
    private String colorizeTierInfo(String tierInfo, String aspectColor) {
        if (tierInfo == null || tierInfo.isEmpty()) return "";

        // Handle [MAX] case - use aspect color
        if (tierInfo.contains("[MAX]")) {
            return aspectColor + "[MAX]";
        }

        // Pattern: "Tier I >>>>>>>>>> TIER II [8/10]"
        // We need to parse the progress [X/Y] and partially color the arrows

        // Extract progress numbers
        int current = 0;
        int total = 1;
        java.util.regex.Pattern progressPattern = java.util.regex.Pattern.compile("\\[(\\d+)/(\\d+)\\]");
        java.util.regex.Matcher progressMatcher = progressPattern.matcher(tierInfo);
        if (progressMatcher.find()) {
            current = Integer.parseInt(progressMatcher.group(1));
            total = Integer.parseInt(progressMatcher.group(2));
        }

        // Calculate progress percentage
        double progressPercent = total > 0 ? (double) current / total : 0;

        // Find and color the arrow sequence
        java.util.regex.Pattern arrowPattern = java.util.regex.Pattern.compile("([>≻→»]+)");
        java.util.regex.Matcher arrowMatcher = arrowPattern.matcher(tierInfo);

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (arrowMatcher.find()) {
            // Add text before arrows
            String before = tierInfo.substring(lastEnd, arrowMatcher.start());
            result.append(colorizeNonArrowPart(before, aspectColor));

            // Color arrows based on progress
            String arrows = arrowMatcher.group(1);
            int arrowCount = arrows.length();
            int greenCount = (int) Math.ceil(arrowCount * progressPercent);

            // Green portion
            for (int i = 0; i < greenCount; i++) {
                result.append("§a").append(arrows.charAt(i));
            }
            // Gray portion
            for (int i = greenCount; i < arrowCount; i++) {
                result.append("§7").append(arrows.charAt(i));
            }

            lastEnd = arrowMatcher.end();
        }

        // Add remaining text after arrows
        if (lastEnd < tierInfo.length()) {
            result.append(colorizeNonArrowPart(tierInfo.substring(lastEnd), aspectColor));
        }

        return result.toString();
    }

    /**
     * Helper to colorize the non-arrow parts of tier info
     */
    private String colorizeNonArrowPart(String text, String aspectColor) {
        if (text == null || text.isEmpty()) return "";

        String result = text;

        // Color lower tier (lowercase "tier") = gray
        result = result.replaceAll("(?i)(tier\\s+[IVX]+)(?!.*TIER)", "§7$1");

        // Color target tier (uppercase "TIER") = aspect color
        result = result.replaceAll("(TIER\\s+[IVX]+)", aspectColor + "$1");

        // Color progress numbers [X/Y] = gray
        result = result.replaceAll("(\\[\\d+/\\d+\\])", "§7$1");

        return result;
    }

    private void renderGambitsPage(DrawContext context, int mouseX, int mouseY) {
        int logicalW = getLogicalWidth();
        int logicalH = getLogicalHeight();
        int centerX = logicalW / 2;

        // Title
        drawCenteredText(context, "§6§lTODAY'S GAMBITS", centerX, 60);

        // Calculate time until next refresh (19:00 Berlin time)
        String countdown = getGambitCountdown();
        drawCenteredText(context, "§7" + countdown, centerX, 110);

        // Fetch crowdsourced gambits from API on first load
        if (!fetchedCrowdsourcedGambits) {
            fetchedCrowdsourcedGambits = true;
            WynncraftApiHandler.fetchCrowdsourcedGambits().thenAccept(result -> {
                crowdsourcedGambits = result;
                if (result != null && !result.isEmpty()) {
                    System.out.println("[WynnExtras] Fetched " + result.size() + " crowdsourced gambits from API");
                    // Save to local data for offline access
                    julianh06.wynnextras.features.aspects.GambitData.INSTANCE.saveGambits(result);
                } else {
                    System.out.println("[WynnExtras] No crowdsourced gambits available from API");
                }
            });
        }

        int startY = 180;

        int panelWidth = Math.min(800, (logicalW - 200) / 2);
        int panelHeight = 180;
        int spacingH = 50;
        int spacingV = 40;

        // Use crowdsourced data if available, otherwise fall back to local data
        List<julianh06.wynnextras.features.aspects.GambitData.GambitEntry> gambitsToShow;
        String dataSource;

        if (crowdsourcedGambits != null && !crowdsourcedGambits.isEmpty()) {
            gambitsToShow = crowdsourcedGambits;
            dataSource = "§a(Crowdsourced)";
        } else {
            // Fall back to local data
            GambitData data = GambitData.INSTANCE;
            if (data.hasToday() && !data.gambits.isEmpty()) {
                gambitsToShow = data.gambits;
                dataSource = "§e(Local - Upload to share!)";
            } else {
                gambitsToShow = null;
                dataSource = "";
            }
        }

        if (gambitsToShow == null || gambitsToShow.isEmpty()) {
            // No gambit data at all
            if (crowdsourcedGambits == null) {
                drawCenteredText(context, "§7Loading crowdsourced data...", centerX, startY + 150);
            } else {
                drawCenteredText(context, "§7No gambit data available yet", centerX, startY + 150);
                drawCenteredText(context, "§e§nClick to open Party Finder /pf to scan", centerX, startY + 200);
            }
        } else {
            // Show data source
            drawCenteredText(context, dataSource, centerX, 140);

            // Draw gambits in 2 columns (2 per row) - in logical units
            int totalWidth = (panelWidth * 2) + spacingH;
            int startX = (logicalW - totalWidth) / 2;

            // Draw up to 4 gambits in 2 columns
            for (int i = 0; i < Math.min(gambitsToShow.size(), 4); i++) {
                julianh06.wynnextras.features.aspects.GambitData.GambitEntry gambit = gambitsToShow.get(i);

                int col = i % 2; // 0 = left, 1 = right
                int row = i / 2; // 0 = top row, 1 = bottom row

                int x = startX + (col * (panelWidth + spacingH));
                int y = startY + (row * (panelHeight + spacingV));

                drawGambitPanel(context, x, y, panelWidth, panelHeight, gambit);
            }
        }

        // Misc Stats below gambits - always show
        int statsStartY = startY + (2 * (panelHeight + spacingV)) + 80;
        drawMiscStats(context, centerX, statsStartY);
    }

    private String getRefreshTimeInfo() {
        try {
            // 19:00 Berlin time = what time in user's timezone?
            ZoneId berlinZone = ZoneId.of("Europe/Berlin");
            ZoneId localZone = ZoneId.systemDefault();

            // Create a time for 19:00 Berlin time today
            LocalDate today = LocalDate.now(berlinZone);
            LocalTime berlinRefreshTime = LocalTime.of(19, 0);
            ZonedDateTime berlinRefresh = ZonedDateTime.of(today, berlinRefreshTime, berlinZone);

            // Convert to user's local time
            ZonedDateTime localRefresh = berlinRefresh.withZoneSameInstant(localZone);

            // Format the time
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            String localTime = localRefresh.format(timeFormatter);

            // Get timezone abbreviation
            String tzAbbrev = localRefresh.getZone().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH);

            return "Gambits refresh daily at " + localTime + " " + tzAbbrev;
        } catch (Exception e) {
            return "Gambits refresh daily at 19:00 CET";
        }
    }

    private void drawGambitPanel(DrawContext context, int x, int y, int panelWidth, int panelHeight, GambitData.GambitEntry gambit) {
        // Background (using logical coords)
        drawRect(x, y, panelWidth, panelHeight, 0xAA000000);

        // Border
        drawRect(x, y, panelWidth, 5, 0xFF4e392d); // Top
        drawRect(x, y + panelHeight - 5, panelWidth, 5, 0xFF4e392d); // Bottom
        drawRect(x, y, 5, panelHeight, 0xFF4e392d); // Left
        drawRect(x + panelWidth - 5, y, 5, panelHeight, 0xFF4e392d); // Right

        // Gambit name (truncate by chars) - account for padding
        String truncatedName = gambit.name;
        int maxNameChars = (panelWidth - 60) / 12; // subtract padding
        if (truncatedName.length() > maxNameChars) {
            truncatedName = truncatedName.substring(0, Math.max(3, maxNameChars - 3)) + "...";
        }
        drawLeftText(context, "§6§l" + truncatedName, x + 30, y + 25);

        // Description (word wrapped) - very tight chars per line to prevent overflow
        String desc = gambit.description;
        int charsPerLine = (panelWidth - 80) / 20; // very conservative - account for padding and text scale
        List<String> lines = wrapTextByChars(desc, charsPerLine);

        int textY = y + 70;
        int lineSpacing = 30; // slightly tighter
        int maxLines = (panelHeight - 90) / lineSpacing; // calculate based on available space
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            if (textY < y + panelHeight - 25) {
                drawLeftText(context, "§7" + lines.get(i), x + 30, textY);
                textY += lineSpacing;
            }
        }
    }

    private List<String> wrapTextByChars(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            // If word alone is too long, truncate it
            if (word.length() > maxChars) {
                // Flush current line first
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
                lines.add(word.substring(0, Math.max(1, maxChars - 3)) + "...");
                continue;
            }

            // Check if word fits on current line
            int neededSpace = currentLine.length() > 0 ? word.length() + 1 : word.length();
            if (currentLine.length() + neededSpace <= maxChars) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                // Word doesn't fit, start new line
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        // Add final line, truncate if needed
        if (currentLine.length() > 0) {
            String finalLine = currentLine.toString();
            if (finalLine.length() > maxChars) {
                finalLine = finalLine.substring(0, Math.max(1, maxChars - 3)) + "...";
            }
            lines.add(finalLine);
        }
        return lines;
    }

    private void drawMiscStats(DrawContext context, int centerX, int startY) {
        drawCenteredText(context, "§6§lMISC STATS", centerX, startY);

        // Calculate raid stats from RaidListData
        int totalRuns = RaidListData.INSTANCE.getRaids().size();
        int completedRuns = (int) RaidListData.INSTANCE.getRaids().stream().filter(r -> r.completed).count();
        int failedRuns = totalRuns - completedRuns;

        // Calculate per-raid stats
        long notgRuns = RaidListData.INSTANCE.getRaids().stream()
            .filter(r -> r.raidInfo.toString().contains("Orphion"))
            .count();
        long nolRuns = RaidListData.INSTANCE.getRaids().stream()
            .filter(r -> r.raidInfo.toString().contains("Lair"))
            .count();
        long tccRuns = RaidListData.INSTANCE.getRaids().stream()
            .filter(r -> r.raidInfo.toString().contains("Canyon"))
            .count();
        long tnaRuns = RaidListData.INSTANCE.getRaids().stream()
            .filter(r -> r.raidInfo.toString().contains("Nameless"))
            .count();

        // Calculate aspect pull stats
        int totalAspectsPulled = 0;
        int mythicAspectsPulled = 0;

        if (myAspectsData != null && myAspectsData.getAspects() != null) {
            List<ApiAspect> allAspects = WynncraftApiHandler.fetchAllAspects();
            for (Aspect playerAspect : myAspectsData.getAspects()) {
                totalAspectsPulled += playerAspect.getAmount();

                // Check if this aspect is mythic
                for (ApiAspect apiAspect : allAspects) {
                    if (apiAspect.getName().equals(playerAspect.getName())) {
                        if (apiAspect.getRarity().equalsIgnoreCase("mythic")) {
                            mythicAspectsPulled += playerAspect.getAmount();
                        }
                        break;
                    }
                }
            }
        }

        // Use larger spacing for text at scale 3f (approx 24 logical pixels per line)
        int lineHeight = 30;
        int sectionGap = 40;
        int yOffset = startY + sectionGap;

        // Display stats in a clean layout
        drawCenteredText(context, "§e§lTotal Runs: §f" + totalRuns, centerX, yOffset);
        yOffset += lineHeight;

        drawCenteredText(context, "§a§lCompleted: §f" + completedRuns + " §8| §c§lFailed: §f" + failedRuns, centerX, yOffset);
        yOffset += sectionGap;

        // Aspect pull stats
        drawCenteredText(context, "§6§lTotal Aspects Pulled: §f" + totalAspectsPulled, centerX, yOffset);
        yOffset += lineHeight;

        drawCenteredText(context, "§5§lMythic Aspects Pulled: §f" + mythicAspectsPulled, centerX, yOffset);
        yOffset += sectionGap;

        // Per-raid breakdown
        drawCenteredText(context, "§7Per-Raid Runs:", centerX, yOffset);
        yOffset += lineHeight;

        drawCenteredText(context, "§5NOTG: §f" + notgRuns + " §8| §bNOL: §f" + nolRuns, centerX, yOffset);
        yOffset += lineHeight;

        drawCenteredText(context, "§cTCC: §f" + tccRuns + " §8| §eTNA: §f" + tnaRuns, centerX, yOffset);
    }

    private String getGambitCountdown() {
        try {
            ZoneId berlinZone = ZoneId.of("Europe/Berlin");
            ZonedDateTime now = ZonedDateTime.now(berlinZone);

            // Next refresh is at 19:00 today or tomorrow
            ZonedDateTime nextRefresh = now.withHour(19).withMinute(0).withSecond(0).withNano(0);
            if (now.isAfter(nextRefresh)) {
                nextRefresh = nextRefresh.plusDays(1);
            }

            Duration duration = Duration.between(now, nextRefresh);
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;

            return String.format("Refreshing in %dh %02dm", hours, minutes);
        } catch (Exception e) {
            return "Refreshes daily at 19:00 CET";
        }
    }

    private double calculateRaidScore(List<LootPoolData.AspectEntry> aspects) {
        double score = 0.0;

        System.out.println("[WynnExtras DEBUG] calculateRaidScore called with " + aspects.size() + " aspects");

        for (LootPoolData.AspectEntry aspect : aspects) {
            String tierInfo = aspect.tierInfo;
            System.out.println("[WynnExtras DEBUG] Aspect: " + aspect.name + " | tierInfo: " + tierInfo);

            if (tierInfo == null || tierInfo.isEmpty() || tierInfo.contains("[MAX]")) {
                System.out.println("[WynnExtras DEBUG] Skipping (maxed or no data)");
                continue; // Already maxed or no data, no score contribution
            }

            // Parse tierInfo: "Tier I >>>>>> Tier II [10/14]"
            int remaining = 0;
            String currentTierStr = "";
            String targetTierStr = "";

            // Extract remaining count [X/Y]
            java.util.regex.Pattern progressPattern = java.util.regex.Pattern.compile("\\[(\\d+)/(\\d+)\\]");
            java.util.regex.Matcher progressMatcher = progressPattern.matcher(tierInfo);
            if (!progressMatcher.find()) {
                System.out.println("[WynnExtras DEBUG] Failed to parse progress pattern");
                continue; // Can't parse progress, skip this aspect
            }

            int current = Integer.parseInt(progressMatcher.group(1));
            int max = Integer.parseInt(progressMatcher.group(2));
            remaining = max - current;
            System.out.println("[WynnExtras DEBUG] Progress: " + current + "/" + max + " | Remaining: " + remaining);

            // Extract tiers (match I, II, III, IV properly)
            java.util.regex.Pattern tierPattern = java.util.regex.Pattern.compile("Tier\\s+(IV|III|II|I)");
            java.util.regex.Matcher tierMatcher = tierPattern.matcher(tierInfo);
            if (tierMatcher.find()) {
                currentTierStr = tierMatcher.group(1); // First match = current tier
                if (tierMatcher.find()) {
                    targetTierStr = tierMatcher.group(1); // Second match = target tier
                } else {
                    // No target tier found - working to max out current tier
                    targetTierStr = currentTierStr;
                }
            } else {
                System.out.println("[WynnExtras DEBUG] Failed to parse tier pattern");
                continue; // Can't parse tiers, skip this aspect
            }

            System.out.println("[WynnExtras DEBUG] Current tier: " + currentTierStr + " | Target tier: " + targetTierStr);

            int currentTier = romanToInt(currentTierStr);
            int targetTier = romanToInt(targetTierStr);

            if (currentTier == 0 || targetTier == 0) {
                System.out.println("[WynnExtras DEBUG] Invalid tier numbers");
                continue; // Invalid tier, skip
            }

            // Apply tier-based weights
            double weight = getTierWeight(aspect.rarity, currentTier, targetTier);
            double contribution = remaining * weight;
            System.out.println("[WynnExtras DEBUG] Weight: " + weight + " | Contribution: " + contribution);

            // Favorite aspects count 3x more
            if (FavoriteAspectsData.INSTANCE.isFavorite(aspect.name)) {
                contribution *= 3.0;
                System.out.println("[WynnExtras DEBUG] Favorite! Contribution after 3x: " + contribution);
            }

            score += contribution;
        }

        System.out.println("[WynnExtras DEBUG] Final score: " + score);
        return score;
    }

    /**
     * Calculate tier weight based on rarity and tier progression.
     */
    private double getTierWeight(String rarity, int currentTier, int targetTier) {
        String key = rarity.toLowerCase() + "_" + currentTier + "_" + targetTier;

        return switch (key) {
            // Known weights from data
            case "mythic_2_3" -> 13.55;
            case "fabled_1_2" -> 10.4;
            case "fabled_2_3" -> 0.65;
            case "legendary_3_4" -> 0.905;

            // Tier progressions (estimated based on rarity pattern)
            case "mythic_1_2" -> 20.0;
            case "mythic_3_4" -> 10.0;
            case "fabled_3_4" -> 0.5;
            case "legendary_1_2" -> 15.0;
            case "legendary_2_3" -> 1.5;

            // Same-tier progression (finishing current tier)
            case "mythic_1_1" -> 20.0;
            case "mythic_2_2" -> 13.55;
            case "mythic_3_3" -> 10.0;
            case "mythic_4_4" -> 5.0;
            case "fabled_1_1" -> 10.4;
            case "fabled_2_2" -> 5.0;
            case "fabled_3_3" -> 0.65;
            case "fabled_4_4" -> 0.5;
            case "legendary_1_1" -> 15.0;
            case "legendary_2_2" -> 5.0;
            case "legendary_3_3" -> 1.5;
            case "legendary_4_4" -> 0.905;

            // Rare weights
            case "rare_1_1", "rare_2_2", "rare_3_3", "rare_4_4" -> 1.0;
            case "rare_1_2", "rare_2_3", "rare_3_4" -> 1.0;

            default -> 1.0; // Default weight
        };
    }

    private int romanToInt(String roman) {
        return switch (roman.toUpperCase()) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            default -> 0;
        };
    }

    private void renderMyAspectsPage(DrawContext context, int mouseX, int mouseY) {
        // Reset hovered aspect
        hoveredMyAspect = null;
        hoveredMyAspectProgress = null;

        int logicalW = getLogicalWidth();
        int logicalH = getLogicalHeight();
        int centerX = logicalW / 2;

        // Convert mouse to logical coords
        int logicalMouseX = (int)(mouseX * scaleFactor);
        int logicalMouseY = (int)(mouseY * scaleFactor);

        // First, check if aspect database is loaded - this must happen first
        List<ApiAspect> allAspects = new ArrayList<>(WynncraftApiHandler.fetchAllAspects());
        if (allAspects.isEmpty()) {
            drawCenteredText(context, "§eLoading aspect database...", centerX, logicalH / 2);
            return;
        }

        // Determine which data source to use (own aspects or searched player's aspects)
        User activeAspectsData;
        WynncraftApiHandler.FetchStatus activeStatus;

        if (!searchedPlayer.isEmpty()) {
            // Viewing another player's aspects
            activeAspectsData = searchedPlayerData;
            activeStatus = searchedPlayerStatus;
        } else {
            // Viewing own aspects
            activeAspectsData = myAspectsData;
            activeStatus = myAspectsFetchStatus;

            // Fetch aspects if not already fetched
            if (!fetchedMyAspects && client.player != null) {
                fetchedMyAspects = true;
                String playerUuid = client.player.getUuidAsString();
                final int fetchGen = ++myAspectsFetchGeneration; // Track this request

                WynncraftApiHandler.fetchPlayerAspectData(playerUuid, playerUuid)
                    .thenAccept(result -> {
                        // Only update if this is still the current request
                        if (fetchGen != myAspectsFetchGeneration) return;
                        if (result == null) return;
                        if (result.status() != null) {
                            if (result.status() == WynncraftApiHandler.FetchStatus.OK) {
                                myAspectsData = result.user();
                            }
                            myAspectsFetchStatus = result.status();
                        }
                    })
                    .exceptionally(ex -> {
                        // Only log if this is still the current request
                        if (fetchGen == myAspectsFetchGeneration) {
                            System.err.println("Failed to fetch aspects: " + ex.getMessage());
                        }
                        return null;
                    });
            }
        }

        // Show loading or error states
        if (activeStatus == null) {
            String loadingText = searchedPlayer.isEmpty() ? "§eLoading your aspects..." : "§eLoading " + searchedPlayer + "'s aspects...";
            drawCenteredText(context, loadingText, centerX, logicalH / 2);
            return;
        }

        switch (activeStatus) {
            case NOKEYSET:
                drawCenteredText(context, "§cYou need to set your API key to use this feature", centerX, logicalH / 2 - 30);
                drawCenteredText(context, "§7Run \"/we apikey\" for more information", centerX, logicalH / 2 + 30);
                return;
            case FORBIDDEN:
                String forbiddenText = searchedPlayer.isEmpty() ? "§cYou need to upload your aspects first" : "§c" + searchedPlayer + " hasn't uploaded their aspects";
                drawCenteredText(context, forbiddenText, centerX, logicalH / 2 - 30);

                if (searchedPlayer.isEmpty()) {
                    // Check if hovering for underline effect
                    boolean hoverForbidden = logicalMouseY >= logicalH / 2 + 10 && logicalMouseY <= logicalH / 2 + 50 &&
                                            logicalMouseX >= centerX - 400 && logicalMouseX <= centerX + 400;
                    drawCenteredText(context, hoverForbidden ? "§e§nClick here to scan your aspects" : "§7Click here to scan your aspects", centerX, logicalH / 2 + 30);
                }
                return;
            case UNAUTHORIZED:
                drawCenteredText(context, "§cYour API key is not connected to your account", centerX, logicalH / 2 - 30);
                drawCenteredText(context, "§7Run \"/we apikey\" for more information", centerX, logicalH / 2 + 30);
                return;
            case NOT_FOUND:
                drawCenteredText(context, "§cNo aspect data found for your account", centerX, logicalH / 2 - 30);
                // Check if hovering for underline effect
                boolean hoverNotFound = logicalMouseY >= logicalH / 2 + 10 && logicalMouseY <= logicalH / 2 + 50 &&
                                       logicalMouseX >= centerX - 400 && logicalMouseX <= centerX + 400;
                drawCenteredText(context, hoverNotFound ? "§e§nClick here to scan your aspects" : "§7Click here to scan your aspects", centerX, logicalH / 2 + 30);
                return;
            case SERVER_UNREACHABLE:
                drawCenteredText(context, "§cServer unreachable. Try again later.", centerX, logicalH / 2);
                return;
            case SERVER_ERROR:
                drawCenteredText(context, "§cServer error occurred!", centerX, logicalH / 2);
                return;
            case UNKNOWN_ERROR:
                String failedText = searchedPlayer.isEmpty() ? "§cFailed to load your aspects" : "§cFailed to load " + searchedPlayer + "'s aspects";
                drawCenteredText(context, failedText, centerX, logicalH / 2);
                return;
        }

        if (activeAspectsData == null || activeAspectsData.getAspects() == null) {
            drawCenteredText(context, "§cNo aspect data found", centerX, logicalH / 2);
            return;
        }

        // allAspects was already fetched at the start of this method

        // Show statistics - filtered by current class selection
        int totalForClass = (int) allAspects.stream()
            .filter(a -> classFilter.equals("All") || a.getRequiredClass().equalsIgnoreCase(classFilter))
            .count();

        int unlockedForClass = 0;
        int maxedForClass = 0;

        for (Aspect playerAspect : activeAspectsData.getAspects()) {
            ApiAspect apiAspect = allAspects.stream()
                .filter(a -> a.getName().equals(playerAspect.getName()))
                .findFirst()
                .orElse(null);

            if (apiAspect != null) {
                // Check if matches current class filter
                if (classFilter.equals("All") || apiAspect.getRequiredClass().equalsIgnoreCase(classFilter)) {
                    unlockedForClass++;

                    // Check if maxed
                    int maxAmount = switch (apiAspect.getRarity().toLowerCase()) {
                        case "mythic" -> 15;
                        case "fabled" -> 75;
                        case "legendary" -> 150;
                        default -> 0;
                    };

                    if (playerAspect.getAmount() >= maxAmount) {
                        maxedForClass++;
                    }
                }
            }
        }

        // Dynamic title based on whose aspects we're viewing
        String title = searchedPlayer.isEmpty() ? "§6§lYOUR ASPECTS" : "§6§l" + searchedPlayer.toUpperCase() + "'S ASPECTS";
        drawCenteredText(context, title, centerX, 60);

        // "Back to My Aspects" button if viewing another player (top right as styled button)
        if (!searchedPlayer.isEmpty()) {
            int buttonWidth = 260;
            int buttonHeight = 44;
            int buttonX = logicalW - buttonWidth - 40;
            int buttonY = 46;
            boolean hoverBack = logicalMouseX >= buttonX && logicalMouseX <= buttonX + buttonWidth &&
                               logicalMouseY >= buttonY && logicalMouseY <= buttonY + buttonHeight;

            // Draw styled button
            if (hoverBack) {
                drawRect(buttonX, buttonY, buttonWidth, buttonHeight, 0xAA333333);
                drawRect(buttonX, buttonY, buttonWidth, 2, 0xFFAAAA00);
                drawRect(buttonX, buttonY + buttonHeight - 2, buttonWidth, 2, 0xFFAAAA00);
                drawRect(buttonX, buttonY, 2, buttonHeight, 0xFFAAAA00);
                drawRect(buttonX + buttonWidth - 2, buttonY, 2, buttonHeight, 0xFFAAAA00);
            } else {
                drawRect(buttonX, buttonY, buttonWidth, buttonHeight, 0xAA1a1a1a);
                drawRect(buttonX, buttonY, buttonWidth, 2, 0xFF4e392d);
                drawRect(buttonX, buttonY + buttonHeight - 2, buttonWidth, 2, 0xFF4e392d);
                drawRect(buttonX, buttonY, 2, buttonHeight, 0xFF4e392d);
                drawRect(buttonX + buttonWidth - 2, buttonY, 2, buttonHeight, 0xFF4e392d);
            }
            String backText = hoverBack ? "§e§l< My Aspects" : "§7< My Aspects";
            drawCenteredText(context, backText, buttonX + buttonWidth / 2, buttonY + buttonHeight / 2);
        }

        // Draw class selector buttons at top (with Overview button) - moved down to give title space
        drawClassSelectorButtons(context, logicalMouseX, logicalMouseY, centerX, 100);

        if (showOverview) {
            // Player search input box (only on overview page) - BELOW the class buttons
            int searchBoxWidth = 500;
            int searchBoxHeight = 40;
            int searchBoxX = centerX - searchBoxWidth / 2;
            int searchBoxY = 160; // Below class buttons (which end at ~140)

            // Draw search box background using drawRect
            int boxColor = searchInputFocused ? 0xFFFFAA00 : 0xFFAAAAAA;
            drawRect(searchBoxX - 2, searchBoxY - 2, searchBoxWidth + 4, searchBoxHeight + 4, boxColor);
            drawRect(searchBoxX, searchBoxY, searchBoxWidth, searchBoxHeight, 0xFF000000);

            // Draw search text or placeholder
            if (searchInput.isEmpty() && !searchInputFocused) {
                drawLeftText(context, "§7Search player...", searchBoxX + 8, searchBoxY + 6);
            } else {
                String displayText = searchInput;
                // Truncate if too long
                int maxChars = (searchBoxWidth - 20) / 12;
                if (displayText.length() > maxChars) {
                    displayText = displayText.substring(displayText.length() - maxChars);
                }
                drawLeftText(context, displayText, searchBoxX + 8, searchBoxY + 6);

                // Draw cursor if focused - put at end of text
                if (searchInputFocused) {
                    // drawLeftText uses scale 3f, so multiply text width by 3 to get logical units
                    int textWidthPixels = textRenderer.getWidth(displayText);
                    int cursorOffset = textWidthPixels * 3; // Scale 3f used in drawLeftText
                    drawRect(searchBoxX + 8 + cursorOffset, searchBoxY + 4, 2, searchBoxHeight - 8, 0xFFFFFFFF);
                }
            }

            // Show overview with progress bars - below search box with more space
            drawOverview(context, allAspects, activeAspectsData.getAspects(), centerX, 240);

            // Show recent searches dropdown when focused and search is empty (RENDER LAST so it's on top)
            if (searchInputFocused && searchInput.isEmpty() && !FavoriteAspectsData.INSTANCE.getRecentSearches().isEmpty()) {
                int dropdownY = searchBoxY + searchBoxHeight + 4;
                int lineHeight = 28;
                int dropdownHeight = Math.min(FavoriteAspectsData.INSTANCE.getRecentSearches().size(), MAX_RECENT_SEARCHES) * lineHeight + 8;

                // Draw dropdown background
                drawRect(searchBoxX - 2, dropdownY - 2, searchBoxWidth + 4, dropdownHeight + 4, 0xFFAAAAAA);
                drawRect(searchBoxX, dropdownY, searchBoxWidth, dropdownHeight, 0xFF000000);

                // Draw recent searches
                int yOffset = dropdownY + 4;
                for (int i = 0; i < Math.min(FavoriteAspectsData.INSTANCE.getRecentSearches().size(), MAX_RECENT_SEARCHES); i++) {
                    String search = FavoriteAspectsData.INSTANCE.getRecentSearches().get(i);
                    boolean hoverRecent = logicalMouseX >= searchBoxX && logicalMouseX <= searchBoxX + searchBoxWidth &&
                                         logicalMouseY >= yOffset && logicalMouseY <= yOffset + lineHeight;

                    if (hoverRecent) {
                        drawRect(searchBoxX, yOffset - 2, searchBoxWidth, lineHeight, 0x88FFFFFF);
                    }

                    drawLeftText(context, "§7" + search, searchBoxX + 8, yOffset);
                    yOffset += lineHeight;
                }
            }
        } else {
            // Draw max filter buttons below class selector
            drawMaxFilterButtons(context, mouseX, mouseY, centerX, 180);

            // Show stats for current class with more space
            String className = classFilter;
            drawCenteredText(context, "§7" + className + " §8| §7Unlocked: §e" + unlockedForClass + "§7/§e" + totalForClass + " §8| §7Maxed: §a" + maxedForClass, centerX, 250);

            // Draw class-specific progress bars
            int progressY = drawClassProgressBars(context, allAspects, activeAspectsData.getAspects(), centerX, 290);

            // Show single class in 2-column layout below progress bars
            drawTwoColumnClassLayout(context, allAspects, activeAspectsData.getAspects(), progressY + 30, mouseX, mouseY);
        }

        // Render tooltip if hovering
        if (hoveredMyAspect != null && hoveredMyAspectProgress != null) {
            renderMyAspectTooltip(context, mouseX, mouseY);
        }

        // Instructions above navigation - make it clickable (in logical coords)
        int scanTextY = logicalH - 165;
        boolean hoverScan = logicalMouseY >= scanTextY - 20 && logicalMouseY <= scanTextY + 40 &&
                           logicalMouseX >= centerX - 400 && logicalMouseX <= centerX + 400;

        drawCenteredText(context, hoverScan ? "§e§nClick here to scan your aspects" : "§7Click here to scan your aspects", centerX, scanTextY);
    }

    private int countMaxedAspects(List<ApiAspect> allAspects, List<Aspect> playerAspects) {
        int count = 0;
        for (Aspect playerAspect : playerAspects) {
            for (ApiAspect apiAspect : allAspects) {
                if (!apiAspect.getName().equals(playerAspect.getName())) continue;

                int maxAmount = switch (apiAspect.getRarity().toLowerCase()) {
                    case "mythic" -> 15;
                    case "fabled" -> 75;
                    case "legendary" -> 150;
                    default -> 0;
                };

                if (playerAspect.getAmount() >= maxAmount) {
                    count++;
                }
                break;
            }
        }
        return count;
    }

    private void drawClassSelectorButtons(DrawContext context, int mouseX, int mouseY, int centerX, int y) {
        int buttonWidth = 170;
        int buttonHeight = 40;
        int spacing = 16;

        String[] buttons = {"Overview", "Warrior", "Shaman", "Mage", "Archer", "Assassin"};
        int totalWidth = (buttonWidth * buttons.length) + (spacing * (buttons.length - 1));
        int startX = centerX - totalWidth / 2;

        for (int i = 0; i < buttons.length; i++) {
            int x = startX + (i * (buttonWidth + spacing));
            boolean selected = (i == 0 && showOverview) || (i > 0 && !showOverview && classFilter.equals(buttons[i]));
            boolean hovered = mouseX >= x && mouseX <= x + buttonWidth && mouseY >= y && mouseY <= y + buttonHeight;

            // Button colors
            String buttonColor;
            if (i == 0) {
                buttonColor = "§6"; // Overview is gold
            } else {
                buttonColor = switch (buttons[i]) {
                    case "Warrior" -> "§c";
                    case "Shaman" -> "§b";
                    case "Mage" -> "§e";
                    case "Archer" -> "§d";
                    case "Assassin" -> "§5";
                    default -> "§7";
                };
            }

            // Draw textured button
            if (ui != null) {
                ui.drawButtonFade(x, y, buttonWidth, buttonHeight, 12, hovered || selected);
            }

            // Draw text - drawCenteredText uses VerticalAlignment.MIDDLE so Y should be center of button
            String displayText = selected ? buttonColor + "§l" + buttons[i] : (hovered ? buttonColor + buttons[i] : "§7" + buttons[i]);
            drawCenteredText(context, displayText, x + buttonWidth / 2, y + buttonHeight / 2);
        }
    }

    private void drawMaxFilterButtons(DrawContext context, int mouseX, int mouseY, int centerX, int y) {
        // Convert mouse to logical coords for hover detection
        int logicalMouseX = (int)(mouseX * scaleFactor);
        int logicalMouseY = (int)(mouseY * scaleFactor);

        int buttonWidth = 200;
        int buttonHeight = 44;
        int spacing = 12;

        String[] filters = {"All", "Max Only", "Not Max", "Favorites"};
        int totalWidth = (buttonWidth * filters.length) + (spacing * (filters.length - 1));
        int startX = centerX - totalWidth / 2;

        for (int i = 0; i < filters.length; i++) {
            int x = startX + (i * (buttonWidth + spacing));
            boolean selected = maxFilter.equals(filters[i]);
            boolean hovered = logicalMouseX >= x && logicalMouseX <= x + buttonWidth && logicalMouseY >= y && logicalMouseY <= y + buttonHeight;

            // Draw textured button
            if (ui != null) {
                ui.drawButtonFade(x, y, buttonWidth, buttonHeight, 12, hovered || selected);
            }

            // Draw text - drawCenteredText uses VerticalAlignment.MIDDLE so Y should be center of button
            String color = selected ? "§6§l" : (hovered ? "§e" : "§7");
            drawCenteredText(context, color + filters[i], x + buttonWidth / 2, y + buttonHeight / 2);
        }
    }

    private void drawImportWynntilsButton(DrawContext context, int logicalMouseX, int logicalMouseY) {
        int buttonWidth = 400;
        int buttonHeight = 44;
        int buttonX = 40;
        int buttonY = 14;

        boolean hovered = logicalMouseX >= buttonX && logicalMouseX <= buttonX + buttonWidth &&
                         logicalMouseY >= buttonY && logicalMouseY <= buttonY + buttonHeight;

        // Draw textured button
        if (ui != null) {
            ui.drawButtonFade(buttonX, buttonY, buttonWidth, buttonHeight, 12, hovered);
        }

        // Draw button text
        String color = hovered ? "§e" : "§7";
        drawCenteredText(context, color + "Import Wynntils Favorites", buttonX + buttonWidth / 2, buttonY + buttonHeight / 2);

        // Show feedback message if recent import (centered below button)
        if (importFeedback != null && System.currentTimeMillis() - importFeedbackTime < 5000) {
            drawCenteredText(context, importFeedback, buttonX + buttonWidth / 2, buttonY + buttonHeight + 16);
        } else {
            importFeedback = null;
        }
    }

    private void drawTwoColumnClassLayout(DrawContext context, List<ApiAspect> allAspects, List<Aspect> playerAspects, int startY, int mouseX, int mouseY) {
        int logicalW = getLogicalWidth();
        int logicalH = getLogicalHeight();

        // Convert mouse to logical coords
        int logicalMouseX = (int)(mouseX * scaleFactor);
        int logicalMouseY = (int)(mouseY * scaleFactor);

        // Two columns: Mythic+Fabled on left, Legendary on right - make WIDER with less spacing
        int columnWidth = Math.min(800, (logicalW - 80) / 2); // Increased max width from 700 to 800
        int columnSpacing = 20; // Reduced spacing from 30 to 20
        int leftX = (logicalW / 2) - columnWidth - (columnSpacing / 2);
        int rightX = (logicalW / 2) + (columnSpacing / 2);

        // Get aspects for current class
        List<ApiAspect> classAspects = getAspectsForClass(allAspects, playerAspects, classFilter);

        // Left column: Mythic + Fabled - fixed height, not extending to bottom
        int panelHeight = logicalH - startY - 150; // Changed from -80 to -150 (shorter boxes)
        drawRect(leftX, startY, columnWidth, panelHeight, 0xAA000000);
        drawRect(leftX, startY, columnWidth, 4, 0xFF4e392d); // top
        drawRect(leftX, startY + panelHeight - 4, columnWidth, 4, 0xFF4e392d); // bottom
        drawRect(leftX, startY, 4, panelHeight, 0xFF4e392d); // left
        drawRect(leftX + columnWidth - 4, startY, 4, panelHeight, 0xFF4e392d); // right

        // Colored title: Mythic (dark purple) & Fabled (red)
        drawCenteredText(context, "§5§lMYTHIC §f§l& §c§lFABLED", leftX + columnWidth / 2, startY + 30);
        drawRect(leftX + 8, startY + 55, columnWidth - 16, 4, 0xFF4e392d); // separator

        int leftY = startY + 80;
        leftY = drawAspectsByRarityForClass(context, leftX, leftY, classAspects, playerAspects, "mythic", "§5", columnWidth, logicalMouseX, logicalMouseY);
        leftY = drawAspectsByRarityForClass(context, leftX, leftY, classAspects, playerAspects, "fabled", "§c", columnWidth, logicalMouseX, logicalMouseY);

        // Right column: Legendary (light blue)
        drawRect(rightX, startY, columnWidth, panelHeight, 0xAA000000);
        drawRect(rightX, startY, columnWidth, 4, 0xFF4e392d); // top
        drawRect(rightX, startY + panelHeight - 4, columnWidth, 4, 0xFF4e392d); // bottom
        drawRect(rightX, startY, 4, panelHeight, 0xFF4e392d); // left
        drawRect(rightX + columnWidth - 4, startY, 4, panelHeight, 0xFF4e392d); // right

        drawCenteredText(context, "§b§lLEGENDARY", rightX + columnWidth / 2, startY + 30);
        drawRect(rightX + 8, startY + 55, columnWidth - 16, 4, 0xFF4e392d); // separator

        int rightY = startY + 80;
        drawAspectsByRarityForClass(context, rightX, rightY, classAspects, playerAspects, "legendary", "§b", columnWidth, logicalMouseX, logicalMouseY);
    }

    private void drawOverview(DrawContext context, List<ApiAspect> allAspects, List<Aspect> playerAspects, int centerX, int startY) {
        int logicalW = getLogicalWidth();

        drawCenteredText(context, "§6§lOVERVIEW", centerX, startY);

        // Mode toggle button (Max vs Unlocked) - right side
        int toggleWidth = 160;
        int toggleHeight = 36;
        int toggleX = centerX + 300;
        int toggleY = startY - 10;

        String modeText = progressBarShowMax ? "§a§lMax" : "§e§lUnlocked";
        drawRect(toggleX, toggleY, toggleWidth, toggleHeight, 0xAA1a1a1a);
        drawRect(toggleX, toggleY, toggleWidth, 2, 0xFF4e392d);
        drawRect(toggleX, toggleY + toggleHeight - 2, toggleWidth, 2, 0xFF4e392d);
        drawRect(toggleX, toggleY, 2, toggleHeight, 0xFF4e392d);
        drawRect(toggleX + toggleWidth - 2, toggleY, 2, toggleHeight, 0xFF4e392d);
        drawCenteredText(context, modeText, toggleX + toggleWidth / 2, toggleY + toggleHeight / 2);

        int barStartY = startY + 50;
        int barWidth = Math.min(800, logicalW - 600); // use logical width, give more space for labels
        int barX = centerX - barWidth / 2 + 50; // shift bar right a bit for label space

        // Calculate totals based on mode
        int totalAspects = allAspects.size();
        int totalCount = progressBarShowMax ? countMaxedAspects(allAspects, playerAspects) : countUnlockedAspects(playerAspects);

        int mythicTotal = (int) allAspects.stream().filter(a -> a.getRarity().equalsIgnoreCase("mythic")).count();
        int mythicCount = progressBarShowMax ? countMaxedByRarity(allAspects, playerAspects, "mythic") : countUnlockedByRarity(allAspects, playerAspects, "mythic");

        int fabledTotal = (int) allAspects.stream().filter(a -> a.getRarity().equalsIgnoreCase("fabled")).count();
        int fabledCount = progressBarShowMax ? countMaxedByRarity(allAspects, playerAspects, "fabled") : countUnlockedByRarity(allAspects, playerAspects, "fabled");

        int legendaryTotal = (int) allAspects.stream().filter(a -> a.getRarity().equalsIgnoreCase("legendary")).count();
        int legendaryCount = progressBarShowMax ? countMaxedByRarity(allAspects, playerAspects, "legendary") : countUnlockedByRarity(allAspects, playerAspects, "legendary");

        String suffix = progressBarShowMax ? " Max" : "";

        // Total progress bar
        drawProgressBarWithLabel(context, barX, barStartY, barWidth, 28, totalCount, totalAspects, "§6§lTotal" + suffix, 0xFF66CC66);
        barStartY += 55;

        // Rarity progress bars (muted colors) - increased spacing
        drawProgressBarWithLabel(context, barX, barStartY, barWidth, 24, mythicCount, mythicTotal, "§5Mythic" + suffix, 0xFF9966CC);
        barStartY += 48;

        drawProgressBarWithLabel(context, barX, barStartY, barWidth, 24, fabledCount, fabledTotal, "§cFabled" + suffix, 0xFFCC6666);
        barStartY += 48;

        drawProgressBarWithLabel(context, barX, barStartY, barWidth, 24, legendaryCount, legendaryTotal, "§bLegendary" + suffix, 0xFF6699CC);
        barStartY += 65;

        // Per-class progress bars
        drawCenteredText(context, "§e§lPER CLASS", centerX, barStartY);
        barStartY += 35;

        String[] classes = {"Warrior", "Shaman", "Mage", "Archer", "Assassin"};
        for (String className : classes) {
            int classTotal = (int) allAspects.stream().filter(a -> a.getRequiredClass().equalsIgnoreCase(className)).count();

            // Use toggle to show either maxed or unlocked count
            int classCount = progressBarShowMax
                ? countMaxedForClassAndRarity(allAspects, playerAspects, className, null)
                : countUnlockedForClassAndRarity(allAspects, playerAspects, className, null);

            String classColor = switch (className) {
                case "Warrior" -> "§c";
                case "Shaman" -> "§b";
                case "Mage" -> "§e";
                case "Archer" -> "§d";
                case "Assassin" -> "§5";
                default -> "§7";
            };

            int fillColor = switch (className) {
                case "Warrior" -> 0xFFCC6666;      // Muted red
                case "Shaman" -> 0xFF6699CC;       // Muted light blue
                case "Mage" -> 0xFFCCCC66;         // Muted yellow
                case "Archer" -> 0xFFCC66CC;       // Muted pink
                case "Assassin" -> 0xFF9966CC;     // Muted dark purple
                default -> 0xFF666666;
            };

            drawProgressBarWithLabel(context, barX, barStartY, barWidth, 20, classCount, classTotal, classColor + className + suffix, fillColor);
            barStartY += 44; // Increased spacing
        }
    }

    private int countMaxedByRarity(List<ApiAspect> allAspects, List<Aspect> playerAspects, String rarity) {
        int count = 0;
        for (Aspect playerAspect : playerAspects) {
            for (ApiAspect apiAspect : allAspects) {
                if (!apiAspect.getName().equals(playerAspect.getName())) continue;
                if (!apiAspect.getRarity().equalsIgnoreCase(rarity)) continue;

                int maxAmount = switch (rarity.toLowerCase()) {
                    case "mythic" -> 15;
                    case "fabled" -> 75;
                    case "legendary" -> 150;
                    default -> 0;
                };

                if (playerAspect.getAmount() >= maxAmount) {
                    count++;
                }
                break;
            }
        }
        return count;
    }

    private int countUnlockedAspects(List<Aspect> playerAspects) {
        return playerAspects.size();
    }

    private int countUnlockedByRarity(List<ApiAspect> allAspects, List<Aspect> playerAspects, String rarity) {
        int count = 0;
        for (Aspect playerAspect : playerAspects) {
            for (ApiAspect apiAspect : allAspects) {
                if (!apiAspect.getName().equals(playerAspect.getName())) continue;
                if (apiAspect.getRarity().equalsIgnoreCase(rarity)) {
                    count++;
                }
                break;
            }
        }
        return count;
    }

    private int countUnlockedForClassAndRarity(List<ApiAspect> allAspects, List<Aspect> playerAspects, String className, String rarity) {
        int count = 0;
        for (Aspect playerAspect : playerAspects) {
            for (ApiAspect apiAspect : allAspects) {
                if (!apiAspect.getName().equals(playerAspect.getName())) continue;
                if (!apiAspect.getRequiredClass().equalsIgnoreCase(className)) continue;
                if (rarity != null && !apiAspect.getRarity().equalsIgnoreCase(rarity)) continue;
                count++;
                break;
            }
        }
        return count;
    }

    private void drawProgressBarWithLabel(DrawContext context, int x, int y, int barWidth, int height, int current, int total, String label, int fillColor) {
        double progress = total > 0 ? (double) current / total : 0;
        int fillWidth = (int) (barWidth * progress);
        boolean isMaxed = current >= total && total > 0;

        // Label on left (move further left to avoid overlapping with bar)
        // Use CommonColors.RAINBOW for maxed (same as front page maxed aspects)
        if (isMaxed && ui != null) {
            // Strip formatting codes for rainbow text
            String plainLabel = label.replaceAll("§.", "");
            ui.drawText(plainLabel, x - 320, y + (height / 2) - 8, CommonColors.RAINBOW, 3f);
        } else {
            drawLeftText(context, label, x - 320, y + (height / 2) - 8);
        }

        // Background using drawRect
        drawRect(x, y, barWidth, height, 0xFF1a1a1a);

        // Border - golden if maxed
        int borderColor = isMaxed ? 0xFFFFD700 : 0xFF4e392d;
        drawRect(x, y, barWidth, 2, borderColor); // top
        drawRect(x, y + height - 2, barWidth, 2, borderColor); // bottom
        drawRect(x, y, 2, height, borderColor); // left
        drawRect(x + barWidth - 2, y, 2, height, borderColor); // right

        // Fill - golden if maxed, otherwise normal color
        if (fillWidth > 4) {
            if (isMaxed) {
                drawRect(x + 2, y + 2, fillWidth - 4, height - 4, 0xFFFFD700); // Golden fill
            } else {
                drawRect(x + 2, y + 2, fillWidth - 4, height - 4, fillColor);
            }
        }

        // Stats on right - rainbow if maxed, otherwise normal
        String statsText;
        if (isMaxed && ui != null) {
            statsText = current + "/" + total + " ★";
            ui.drawText(statsText, x + barWidth + 20, y + (height / 2) - 8, CommonColors.RAINBOW, 3f);
        } else {
            statsText = "§7" + current + "§8/§7" + total + " §8(§e" + String.format("%.1f", progress * 100) + "%§8)";
            drawLeftText(context, statsText, x + barWidth + 20, y + (height / 2) - 8);
        }
    }

    /**
     * Draw text with rainbow color animation
     */
    private void drawRainbowText(DrawContext context, String text, int x, int y) {
        if (ui == null) return;

        long time = System.currentTimeMillis();
        float animOffset = (time % 1500) / 1500f; // Full cycle every 1.5 seconds

        // Rainbow colors for text
        String[] colorCodes = {"§c", "§6", "§e", "§a", "§b", "§9", "§d"};

        StringBuilder coloredText = new StringBuilder();
        int charIndex = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Skip formatting codes
            if (c == '§' && i + 1 < text.length()) {
                i++; // Skip the format character
                continue;
            }

            // Calculate color for this character
            int colorIdx = (int)((charIndex + animOffset * colorCodes.length) % colorCodes.length);
            coloredText.append(colorCodes[colorIdx]).append(c);
            charIndex++;
        }

        drawLeftText(context, coloredText.toString(), x, y);
    }

    /**
     * Draw progress bars for the currently selected class showing All/Mythic/Fabled/Legendary maxed counts
     * @return the Y position after drawing all bars
     */
    private int drawClassProgressBars(DrawContext context, List<ApiAspect> allAspects, List<Aspect> playerAspects, int centerX, int startY) {
        int logicalW = getLogicalWidth();
        int barWidth = Math.min(600, logicalW - 600);
        int barX = centerX - barWidth / 2 + 50; // shift right for label space

        String className = classFilter;
        String classColor = switch (className) {
            case "Warrior" -> "§c";
            case "Shaman" -> "§b";
            case "Mage" -> "§e";
            case "Archer" -> "§d";
            case "Assassin" -> "§5";
            default -> "§7";
        };

        int fillColor = switch (className) {
            case "Warrior" -> 0xFFCC6666;
            case "Shaman" -> 0xFF6699CC;
            case "Mage" -> 0xFFCCCC66;
            case "Archer" -> 0xFFCC66CC;
            case "Assassin" -> 0xFF9966CC;
            default -> 0xFF666666;
        };

        // Calculate totals for this class based on mode
        int allTotal = (int) allAspects.stream().filter(a -> a.getRequiredClass().equalsIgnoreCase(className)).count();
        int allCount = progressBarShowMax
            ? countMaxedForClassAndRarity(allAspects, playerAspects, className, null)
            : countUnlockedForClassAndRarity(allAspects, playerAspects, className, null);

        int mythicTotal = (int) allAspects.stream().filter(a -> a.getRequiredClass().equalsIgnoreCase(className) && a.getRarity().equalsIgnoreCase("mythic")).count();
        int mythicCount = progressBarShowMax
            ? countMaxedForClassAndRarity(allAspects, playerAspects, className, "mythic")
            : countUnlockedForClassAndRarity(allAspects, playerAspects, className, "mythic");

        int fabledTotal = (int) allAspects.stream().filter(a -> a.getRequiredClass().equalsIgnoreCase(className) && a.getRarity().equalsIgnoreCase("fabled")).count();
        int fabledCount = progressBarShowMax
            ? countMaxedForClassAndRarity(allAspects, playerAspects, className, "fabled")
            : countUnlockedForClassAndRarity(allAspects, playerAspects, className, "fabled");

        int legendaryTotal = (int) allAspects.stream().filter(a -> a.getRequiredClass().equalsIgnoreCase(className) && a.getRarity().equalsIgnoreCase("legendary")).count();
        int legendaryCount = progressBarShowMax
            ? countMaxedForClassAndRarity(allAspects, playerAspects, className, "legendary")
            : countUnlockedForClassAndRarity(allAspects, playerAspects, className, "legendary");

        int y = startY;

        // All aspects bar
        drawProgressBarWithLabel(context, barX, y, barWidth, 20, allCount, allTotal, classColor + "All " + className, fillColor);
        y += 36; // Increased spacing

        // Mythic bar
        drawProgressBarWithLabel(context, barX, y, barWidth, 18, mythicCount, mythicTotal, "§5Mythic " + className, 0xFF9966CC);
        y += 32; // Increased spacing

        // Fabled bar
        drawProgressBarWithLabel(context, barX, y, barWidth, 18, fabledCount, fabledTotal, "§cFabled " + className, 0xFFCC6666);
        y += 32; // Increased spacing

        // Legendary bar
        drawProgressBarWithLabel(context, barX, y, barWidth, 18, legendaryCount, legendaryTotal, "§bLegendary " + className, 0xFF6699CC);
        y += 32; // Increased spacing

        return y;
    }

    /**
     * Count maxed aspects for a specific class and optionally a specific rarity
     */
    private int countMaxedForClassAndRarity(List<ApiAspect> allAspects, List<Aspect> playerAspects, String className, String rarity) {
        int count = 0;
        for (Aspect playerAspect : playerAspects) {
            for (ApiAspect apiAspect : allAspects) {
                if (!apiAspect.getName().equals(playerAspect.getName())) continue;
                if (!apiAspect.getRequiredClass().equalsIgnoreCase(className)) continue;
                if (rarity != null && !apiAspect.getRarity().equalsIgnoreCase(rarity)) continue;

                int maxAmount = switch (apiAspect.getRarity().toLowerCase()) {
                    case "mythic" -> 15;
                    case "fabled" -> 75;
                    case "legendary" -> 150;
                    default -> 0;
                };

                if (playerAspect.getAmount() >= maxAmount) {
                    count++;
                }
                break;
            }
        }
        return count;
    }

    private List<ApiAspect> getAspectsForClass(List<ApiAspect> allAspects, List<Aspect> playerAspects, String className) {
        List<ApiAspect> filtered = new ArrayList<>();

        for (ApiAspect aspect : allAspects) {
            // Check if player has this aspect
            Aspect playerAspect = null;
            for (Aspect pa : playerAspects) {
                if (pa.getName().equals(aspect.getName())) {
                    playerAspect = pa;
                    break;
                }
            }

            if (playerAspect == null) continue; // Only show unlocked

            // Check class
            if (!aspect.getRequiredClass().equalsIgnoreCase(className)) continue;

            // Check max filter
            int maxAmount = switch (aspect.getRarity().toLowerCase()) {
                case "mythic" -> 15;
                case "fabled" -> 75;
                case "legendary" -> 150;
                default -> 0;
            };

            boolean isMaxed = playerAspect.getAmount() >= maxAmount;

            if (maxFilter.equals("Max Only") && !isMaxed) continue;
            if (maxFilter.equals("Not Max") && isMaxed) continue;
            if (maxFilter.equals("Favorites") && !FavoriteAspectsData.INSTANCE.isFavorite(aspect.getName())) continue;

            filtered.add(aspect);
        }

        // Sort by rarity: Mythic -> Fabled -> Legendary
        filtered.sort((a, b) -> {
            int rarityOrder1 = getRarityOrder(a.getRarity());
            int rarityOrder2 = getRarityOrder(b.getRarity());
            return Integer.compare(rarityOrder1, rarityOrder2);
        });

        return filtered;
    }

    private int getRarityOrder(String rarity) {
        return switch (rarity.toLowerCase()) {
            case "mythic" -> 0;
            case "fabled" -> 1;
            case "legendary" -> 2;
            default -> 999;
        };
    }

    private int drawAspectsByRarityForClass(DrawContext context, int x, int y, List<ApiAspect> aspects, List<Aspect> playerAspects, String rarity, String color, int colWidth, int mouseX, int mouseY) {
        List<ApiAspect> filtered = aspects.stream()
            .filter(a -> a.getRarity().equalsIgnoreCase(rarity))
            .toList();

        if (filtered.isEmpty()) return y;

        int lineHeight = 35; // Logical spacing

        for (ApiAspect aspect : filtered) {
            // Find player progress
            Aspect playerAspect = null;
            for (Aspect pa : playerAspects) {
                if (pa.getName().equals(aspect.getName())) {
                    playerAspect = pa;
                    break;
                }
            }

            if (playerAspect == null) continue;

            // Check if aspect is maxed
            int maxAmount = switch (aspect.getRarity().toLowerCase()) {
                case "mythic" -> 15;
                case "fabled" -> 75;
                case "legendary" -> 150;
                default -> 0;
            };
            boolean isMaxed = playerAspect.getAmount() >= maxAmount;

            // Truncate name by character count (logical coords)
            int maxChars = Math.max(10, (colWidth - 80) / 12);
            String displayName = aspect.getName();
            if (displayName.length() > maxChars) {
                displayName = displayName.substring(0, maxChars - 3) + "...";
            }

            // Check hover using full row width for easier hit detection (mouseX/Y are already logical)
            // Hitbox aligned with text position (y is where text is drawn)
            boolean isHovered = false;
            if (mouseX >= x + 16 && mouseX <= x + colWidth - 16 &&
                mouseY >= y - 10 && mouseY <= y + 25) {
                hoveredMyAspect = aspect;
                hoveredMyAspectProgress = playerAspect;
                isHovered = true;
            }

            // Check if favorited
            boolean isFavorite = FavoriteAspectsData.INSTANCE.isFavorite(aspect.getName());

            // Show star: filled if favorite, empty if hovered but not favorite, nothing otherwise
            String favoriteStar;
            if (isFavorite) {
                favoriteStar = " §e⭐";
            } else if (isHovered) {
                favoriteStar = " §7☆";
            } else {
                favoriteStar = "";
            }

            // Draw text at y position - rainbow for maxed aspects
            if (isMaxed && ui != null) {
                ui.drawText(displayName + favoriteStar, x + 45, y, CommonColors.RAINBOW, 3f);
            } else {
                drawLeftText(context, color + displayName + favoriteStar, x + 45, y);
            }

            // Draw flame icon
            ItemStack flameItem = createAspectFlameIcon(aspect, isMaxed);
            if (!flameItem.isEmpty() && ui != null) {
                float flameScale = 0.6f;
                int screenX = (int) ui.sx(x + 12);
                int screenY = (int) ui.sy(y + 2); // Move flame up
                context.getMatrices().pushMatrix();
                context.getMatrices().scale(flameScale, flameScale);
                context.drawItem(flameItem, (int)(screenX / flameScale), (int)(screenY / flameScale));
                context.getMatrices().popMatrix();
            }

            y += lineHeight;
        }

        return y + 20; // Spacing after rarity group
    }

    private void renderMyAspectTooltip(DrawContext context, int mouseX, int mouseY) {
        if (hoveredMyAspect == null || hoveredMyAspectProgress == null) return;

        List<Text> tooltipLines = new ArrayList<>();

        // Rarity color
        String rarityColor = switch (hoveredMyAspect.getRarity().toLowerCase()) {
            case "mythic" -> "§5";
            case "fabled" -> "§c";
            case "legendary" -> "§b";
            default -> "§f";
        };

        tooltipLines.add(Text.literal(rarityColor + hoveredMyAspect.getName()));

        // Show progress
        int maxAmount = switch (hoveredMyAspect.getRarity().toLowerCase()) {
            case "mythic" -> 15;
            case "fabled" -> 75;
            case "legendary" -> 150;
            default -> 0;
        };

        int amount = hoveredMyAspectProgress.getAmount();
        if (amount >= maxAmount) {
            tooltipLines.add(Text.literal(rarityColor + "[MAX]"));
        } else {
            tooltipLines.add(Text.literal("§7Progress: §e" + amount + "§7/§e" + maxAmount));
        }

        // Add description for current tier
        String description = getDescriptionForCurrentTier(hoveredMyAspect, amount);
        if (description != null && !description.isEmpty()) {
            tooltipLines.add(Text.literal(""));

            // Strip Wynncraft symbols
            String cleanDesc = stripWynncraftSymbols(description);

            // Split by newlines first (for multiple features)
            String[] descriptionLines = cleanDesc.split("\n");
            for (String descLine : descriptionLines) {
                if (descLine.trim().isEmpty()) continue;

                // Word wrap each line individually
                List<String> wrappedLines = wrapText(descLine.trim(), 200);
                for (String line : wrappedLines) {
                    tooltipLines.add(Text.literal("§7" + line));
                }
            }
        }

        context.drawTooltip(textRenderer, tooltipLines, mouseX, mouseY);
    }

    private String getDescriptionForCurrentTier(ApiAspect aspect, int currentAmount) {
        if (aspect.getTiers() == null || aspect.getTiers().isEmpty()) return null;

        // Find the highest tier the player has reached
        String currentTierKey = null;
        int highestThreshold = 0;

        for (Map.Entry<String, ApiAspect.Tier> entry : aspect.getTiers().entrySet()) {
            int threshold = entry.getValue().getThreshold();
            if (currentAmount >= threshold && threshold >= highestThreshold) {
                highestThreshold = threshold;
                currentTierKey = entry.getKey();
            }
        }

        if (currentTierKey == null) return null;

        ApiAspect.Tier tier = aspect.getTiers().get(currentTierKey);
        if (tier.getDescription() == null || tier.getDescription().isEmpty()) return null;

        // Join description lines with newlines
        return String.join("\n", tier.getDescription());
    }

    private void renderNavigation(DrawContext context, int mouseX, int mouseY) {
        int logicalW = getLogicalWidth();
        int logicalH = getLogicalHeight();
        int centerX = logicalW / 2;
        int navY = logicalH - 120; // Position from bottom in logical units

        // Convert mouse to logical for hover checks
        int logicalMouseX = (int)(mouseX * scaleFactor);
        int logicalMouseY = (int)(mouseY * scaleFactor);

        // Left arrow (always visible, circular) - fixed distance from center
        int leftX = centerX - 400;
        boolean hoverLeft = logicalMouseX >= leftX - 30 && logicalMouseX <= leftX + 200 &&
                           logicalMouseY >= navY - 20 && logicalMouseY <= navY + 50;

        drawCenteredText(context, hoverLeft ? "§6§l< Previous" : "§e§l< Previous", leftX, navY);

        // Page indicator (centered)
        String pageText = switch (currentPage) {
            case 0 -> "Loot Pools";
            case 1 -> {
                // If viewing another player, show their name instead
                if (!searchedPlayer.isEmpty() && searchedPlayerData != null) {
                    yield searchedPlayer + "'s Aspects";
                } else {
                    yield "My Aspects";
                }
            }
            case 2 -> "Gambits";
            case 3 -> "Raid Loot";
            case 4 -> "Explore";
            case 5 -> "Leaderboard";
            default -> "Unknown";
        };
        drawCenteredText(context, "§e" + pageText + " §7(" + (currentPage + 1) + "/" + (MAX_PAGE + 1) + ")", centerX, navY);

        // Right arrow (always visible, circular) - fixed distance from center
        int rightX = centerX + 400;
        boolean hoverRight = logicalMouseX >= rightX - 100 && logicalMouseX <= rightX + 130 &&
                            logicalMouseY >= navY - 20 && logicalMouseY <= navY + 50;

        drawCenteredText(context, hoverRight ? "§6§lNext >" : "§e§lNext >", rightX, navY);

        // Quick page buttons at the bottom
        int buttonY = navY + 45;
        int buttonWidth = 240;
        int buttonHeight = 50;
        int buttonSpacing = 10;
        String[] pageNames = {"Loot Pools", "Aspects", "Gambits", "Raid Loot", "Explore", "Leaderboard"};
        int totalButtonsWidth = (buttonWidth * 6) + (buttonSpacing * 5);
        int buttonStartX = (logicalW - totalButtonsWidth) / 2;

        for (int i = 0; i <= MAX_PAGE; i++) {
            int bx = buttonStartX + (i * (buttonWidth + buttonSpacing));
            boolean isCurrentPage = (currentPage == i);
            boolean hovering = logicalMouseX >= bx && logicalMouseX <= bx + buttonWidth &&
                              logicalMouseY >= buttonY && logicalMouseY <= buttonY + buttonHeight;

            // Draw textured button
            if (ui != null) {
                ui.drawButtonFade(bx, buttonY, buttonWidth, buttonHeight, 12, hovering || isCurrentPage);
            }

            // Text
            String text = isCurrentPage ? "§6§l" + pageNames[i] : (hovering ? "§e" + pageNames[i] : "§7" + pageNames[i]);
            drawCenteredText(context, text, bx + buttonWidth / 2, buttonY + buttonHeight / 2);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Only handle scroll on loot pools page
        if (currentPage == 0) {
            // Convert screen coords to logical coords for comparison
            double logicalMouseX = mouseX * scaleFactor;
            double logicalMouseY = mouseY * scaleFactor;

            // Find which column the mouse is over
            for (int i = 0; i < 4; i++) {
                if (logicalMouseX >= raidColumnX[i] && logicalMouseX <= raidColumnX[i] + raidColumnWidth[i] &&
                    logicalMouseY >= raidContentStartY && logicalMouseY <= raidContentStartY + raidPanelHeight) {
                    // Scroll this column (scale scroll amount for logical units)
                    int scrollAmount = (int)(verticalAmount * 60); // 60 logical pixels per scroll tick
                    raidScrollOffsets[i] -= scrollAmount;

                    // Clamp to valid range
                    int maxScroll = Math.max(0, raidContentHeights[i] - raidPanelHeight);
                    if (raidScrollOffsets[i] < 0) raidScrollOffsets[i] = 0;
                    if (raidScrollOffsets[i] > maxScroll) raidScrollOffsets[i] = maxScroll;

                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        // Convert to logical coordinates
        int logicalW = getLogicalWidth();
        int logicalH = getLogicalHeight();
        double logicalMouseX = mouseX * scaleFactor;
        double logicalMouseY = mouseY * scaleFactor;

        int centerX = logicalW / 2;
        int navY = logicalH - 120;

        // Check for left click on aspects to favorite them (click the star)
        if (button == 0) {
            // Check if clicking on loot pool aspect (page 0)
            if (currentPage == 0 && hoveredAspect != null) {
                FavoriteAspectsData.INSTANCE.toggleFavorite(hoveredAspect.name);
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
            // Check if clicking on my aspect (page 1)
            if (currentPage == 1 && hoveredMyAspect != null) {
                FavoriteAspectsData.INSTANCE.toggleFavorite(hoveredMyAspect.getName());
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        // Check "Import Wynntils Favorites" button click (on Loot Pools and My Aspects pages)
        if (currentPage == 0 || currentPage == 1) {
            int importButtonWidth = 400;
            int importButtonHeight = 44;
            int importButtonX = 40;
            int importButtonY = 14;

            if (logicalMouseX >= importButtonX && logicalMouseX <= importButtonX + importButtonWidth &&
                logicalMouseY >= importButtonY && logicalMouseY <= importButtonY + importButtonHeight) {
                // Import favorites from Wynntils
                int imported = FavoriteAspectsData.INSTANCE.importFromWynntils();
                if (imported > 0) {
                    importFeedback = "§aImported " + imported + " favorites!";
                } else {
                    importFeedback = "§7No new favorites to import";
                }
                importFeedbackTime = System.currentTimeMillis();
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        // Check left arrow click (circular navigation) - using logical coords
        int leftX = centerX - 400;
        if (logicalMouseX >= leftX - 100 && logicalMouseX <= leftX + 100 &&
            logicalMouseY >= navY - 30 && logicalMouseY <= navY + 50) {
            currentPage = (currentPage - 1 + MAX_PAGE + 1) % (MAX_PAGE + 1);
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }

        // Check right arrow click (circular navigation)
        int rightX = centerX + 400;
        if (logicalMouseX >= rightX - 100 && logicalMouseX <= rightX + 100 &&
            logicalMouseY >= navY - 30 && logicalMouseY <= navY + 50) {
            currentPage = (currentPage + 1) % (MAX_PAGE + 1);
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }

        // Quick page button clicks - must match renderNavigation dimensions exactly!
        int navBtnY = navY + 45;
        int navBtnWidth = 240;
        int navBtnHeight = 50;
        int navBtnSpacing = 10;
        int totalNavBtnsWidth = (navBtnWidth * 6) + (navBtnSpacing * 5);
        int navBtnStartX = (logicalW - totalNavBtnsWidth) / 2;

        for (int i = 0; i <= MAX_PAGE; i++) {
            int bx = navBtnStartX + (i * (navBtnWidth + navBtnSpacing));
            if (logicalMouseX >= bx && logicalMouseX <= bx + navBtnWidth &&
                logicalMouseY >= navBtnY && logicalMouseY <= navBtnY + navBtnHeight) {
                if (currentPage != i) {
                    currentPage = i;
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                }
                return true;
            }
        }

        // Delegate to extracted page classes for pages 2-5
        if (currentPage == 2 && gambitsPage.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (currentPage == 3 && raidLootPage.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (currentPage == 4 && explorePage.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (currentPage == 5 && leaderboardPage.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Check toggle clicks (on loot pools page) - using logical coords
        if (currentPage == 0) {
            int toggleWidth = 200;
            int favToggleWidth = 320;
            int toggleHeight = 44;
            int toggleSpacing = 15;
            int toggleY = 14;

            // Favorite toggle (left of Hide Max)
            int favToggleX = logicalW - toggleWidth - favToggleWidth - toggleSpacing - 60;
            if (logicalMouseX >= favToggleX && logicalMouseX <= favToggleX + favToggleWidth &&
                logicalMouseY >= toggleY && logicalMouseY <= toggleY + toggleHeight) {
                showOnlyFavoritesInLootPools = !showOnlyFavoritesInLootPools;
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }

            // Hide Max toggle
            int toggleX = logicalW - toggleWidth - 60;
            if (logicalMouseX >= toggleX && logicalMouseX <= toggleX + toggleWidth &&
                logicalMouseY >= toggleY && logicalMouseY <= toggleY + toggleHeight) {
                hideMaxInLootPools = !hideMaxInLootPools;
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        // Check raid header clicks (on loot pools page) - using logical coords
        if (currentPage == 0) {
            int startY = getLootPoolStartY();
            int columnSpacing = 30;
            int columnWidth = getSafeColumnWidth(4, columnSpacing);
            int totalWidth = (columnWidth * 4) + (columnSpacing * 3);
            int startX = (logicalW - totalWidth) / 2;

            String[] raids = {"NOTG", "NOL", "TCC", "TNA"};

            for (int i = 0; i < 4; i++) {
                int x = startX + (i * (columnWidth + columnSpacing));
                int headerHeight = 120;

                if (logicalMouseX >= x && logicalMouseX <= x + columnWidth &&
                    logicalMouseY >= startY && logicalMouseY <= startY + headerHeight) {
                    // Clicked on this raid header - open party finder for this raid
                    joinRaidPartyFinder(raids[i]);
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
            }
        }

        // Check button clicks (on my aspects page - page 1)
        if (currentPage == 1) {
            // Class selector buttons (including Overview) - MUST match drawClassSelectorButtons dimensions
            int classButtonWidth = 170;
            int classButtonHeight = 40;
            int classSpacing = 16;
            int classY = 100; // matches drawClassSelectorButtons y param

            String[] buttons = {"Overview", "Warrior", "Shaman", "Mage", "Archer", "Assassin"};
            int totalClassWidth = (classButtonWidth * buttons.length) + (classSpacing * (buttons.length - 1));
            int classStartX = centerX - totalClassWidth / 2;

            for (int i = 0; i < buttons.length; i++) {
                int x = classStartX + (i * (classButtonWidth + classSpacing));
                // Use logical coords for click detection
                if (logicalMouseX >= x && logicalMouseX <= x + classButtonWidth &&
                    logicalMouseY >= classY && logicalMouseY <= classY + classButtonHeight) {
                    if (i == 0) {
                        showOverview = true;
                    } else {
                        showOverview = false;
                        classFilter = buttons[i];
                    }
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
            }

            // Max filter buttons (only on class view, not overview) - MUST match drawMaxFilterButtons
            if (!showOverview) {
                int maxButtonWidth = 200;
                int maxButtonHeight = 44;
                int maxSpacing = 12;
                int maxY = 180; // matches drawMaxFilterButtons y param

                String[] maxFilters = {"All", "Max Only", "Not Max", "Favorites"};
                int totalMaxWidth = (maxButtonWidth * maxFilters.length) + (maxSpacing * (maxFilters.length - 1));
                int maxStartX = centerX - totalMaxWidth / 2;

                for (int i = 0; i < maxFilters.length; i++) {
                    int x = maxStartX + (i * (maxButtonWidth + maxSpacing));
                    if (logicalMouseX >= x && logicalMouseX <= x + maxButtonWidth &&
                        logicalMouseY >= maxY && logicalMouseY <= maxY + maxButtonHeight) {
                        maxFilter = maxFilters[i];
                        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                        return true;
                    }
                }
            }

            // Check search box click (only on overview page) - MUST match renderMyAspectsPage
            if (showOverview) {
                // Progress bar mode toggle - matches drawOverview
                int toggleWidth = 160;
                int toggleHeight = 36;
                int toggleX = centerX + 300;
                int toggleY = 230; // startY (240) - 10

                if (button == 0 && logicalMouseX >= toggleX && logicalMouseX <= toggleX + toggleWidth &&
                    logicalMouseY >= toggleY && logicalMouseY <= toggleY + toggleHeight) {
                    progressBarShowMax = !progressBarShowMax;
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }

                int searchBoxWidth = 400;
                int searchBoxHeight = 30;
                int searchBoxX = centerX - searchBoxWidth / 2;
                int searchBoxY = 160; // Matches renderMyAspectsPage

                // Right-click to clear search box
                if (button == 1 && logicalMouseX >= searchBoxX && logicalMouseX <= searchBoxX + searchBoxWidth &&
                    logicalMouseY >= searchBoxY && logicalMouseY <= searchBoxY + searchBoxHeight) {
                    searchInput = "";
                    searchCursorPos = 0;
                    return true;
                }

                // Left-click on search box
                if (button == 0 && logicalMouseX >= searchBoxX && logicalMouseX <= searchBoxX + searchBoxWidth &&
                    logicalMouseY >= searchBoxY && logicalMouseY <= searchBoxY + searchBoxHeight) {
                    searchInputFocused = true;
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }

                // Click on recent searches dropdown
                if (button == 0 && searchInputFocused && searchInput.isEmpty() && !FavoriteAspectsData.INSTANCE.getRecentSearches().isEmpty()) {
                    int dropdownY = searchBoxY + searchBoxHeight + 4;
                    int lineHeight = 28;
                    int yOffset = dropdownY + 4;

                    for (int i = 0; i < Math.min(FavoriteAspectsData.INSTANCE.getRecentSearches().size(), MAX_RECENT_SEARCHES); i++) {
                        if (logicalMouseX >= searchBoxX && logicalMouseX <= searchBoxX + searchBoxWidth &&
                            logicalMouseY >= yOffset && logicalMouseY <= yOffset + lineHeight) {
                            // Clicked on this recent search
                            String selectedSearch = FavoriteAspectsData.INSTANCE.getRecentSearches().get(i);
                            searchInput = selectedSearch;
                            searchCursorPos = selectedSearch.length();
                            performPlayerSearch(selectedSearch);
                            searchInputFocused = false;
                            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                            return true;
                        }
                        yOffset += lineHeight;
                    }
                }

                // Click outside search box/dropdown - unfocus
            }
        }

        // Click handling for pages 3, 4, 5 is now handled by their respective page classes above

        // Check click on error message scan links (FORBIDDEN and NOT_FOUND) - works on any page
        if (currentPage == 1) {
            if (searchedPlayerStatus == WynncraftApiHandler.FetchStatus.FORBIDDEN && searchedPlayer.isEmpty()) {
                boolean clickedError = logicalMouseY >= logicalH / 2 + 10 && logicalMouseY <= logicalH / 2 + 50 &&
                                      logicalMouseX >= centerX - 400 && logicalMouseX <= centerX + 400;
                if (clickedError) {
                    if (client != null && client.player != null) {
                        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§eStarting aspect scan..."));
                        client.setScreen(null);
                        client.player.networkHandler.sendChatCommand("we aspects scan");
                    }
                    return true;
                }
            }

            if (searchedPlayerStatus == WynncraftApiHandler.FetchStatus.NOT_FOUND) {
                boolean clickedError = logicalMouseY >= logicalH / 2 + 10 && logicalMouseY <= logicalH / 2 + 50 &&
                                      logicalMouseX >= centerX - 400 && logicalMouseX <= centerX + 400;
                if (clickedError) {
                    if (client != null && client.player != null) {
                        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§eStarting aspect scan..."));
                        client.setScreen(null);
                        client.player.networkHandler.sendChatCommand("we aspects scan");
                    }
                    return true;
                }
            }
        }

        // Check "Back to My Aspects" button click (on overview page when viewing another player) - page 1
        if (currentPage == 1 && showOverview) {
            // Click outside search box/dropdown - unfocus
            if (button == 0 && searchInputFocused) {
                int searchBoxWidth = 400;
                int searchBoxHeight = 30;
                int searchBoxX = centerX - searchBoxWidth / 2;
                int searchBoxY = 160;
                int dropdownY = searchBoxY + searchBoxHeight + 4;
                int lineHeight = 28;
                int dropdownHeight = Math.min(FavoriteAspectsData.INSTANCE.getRecentSearches().size(), MAX_RECENT_SEARCHES) * lineHeight + 8;
                boolean clickedInDropdown = logicalMouseY >= dropdownY && logicalMouseY <= dropdownY + dropdownHeight &&
                                           logicalMouseX >= searchBoxX && logicalMouseX <= searchBoxX + searchBoxWidth;

                if (!clickedInDropdown && !(logicalMouseX >= searchBoxX && logicalMouseX <= searchBoxX + searchBoxWidth &&
                    logicalMouseY >= searchBoxY && logicalMouseY <= searchBoxY + searchBoxHeight)) {
                    searchInputFocused = false;
                }
            }

            // Check "Back to My Aspects" button click (top right styled button)
            if (!searchedPlayer.isEmpty()) {
                int buttonWidth = 260;
                int buttonHeight = 44;
                int buttonX = logicalW - buttonWidth - 40;
                int buttonY = 46;

                if (logicalMouseX >= buttonX && logicalMouseX <= buttonX + buttonWidth &&
                    logicalMouseY >= buttonY && logicalMouseY <= buttonY + buttonHeight) {
                    // Reset to viewing own aspects
                    searchedPlayer = "";
                    searchedPlayerData = null;
                    searchedPlayerStatus = null;
                    searchInput = "";
                    searchCursorPos = 0;
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
            }

            // Check click on "scan your aspects" text - use logical coords (above navigation)
            int scanTextY = logicalH - 165;
            boolean clickedScan = logicalMouseY >= scanTextY - 20 && logicalMouseY <= scanTextY + 40 &&
                                 logicalMouseX >= centerX - 400 && logicalMouseX <= centerX + 400;

            if (clickedScan) {
                // Run /we aspects scan command
                if (client != null && client.player != null) {
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§eStarting aspect scan..."));
                    client.setScreen(null);
                    client.player.networkHandler.sendChatCommand("we aspects scan");
                }
                return true;
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public void joinRaidPartyFinder(String raidCode) {
        if (client == null || client.player == null) return;

        // Close this screen
        client.setScreen(null);

        // Set the raid we want to join
        pendingRaidJoin = raidCode;

        // Open party finder
        client.player.networkHandler.sendChatCommand("pf");

        // Register tick listener to wait for party finder menu and click through
        final AtomicBoolean clickedQueue = new AtomicBoolean(false);
        final AtomicBoolean clickedRaid = new AtomicBoolean(false);
        final int[] ticksSinceQueueClick = {0}; // Wait ticks after clicking queue

        ClientTickEvents.END_CLIENT_TICK.register(clientTick -> {
            if (pendingRaidJoin == null || clickedRaid.get()) {
                return; // Done, listener will stay registered but do nothing
            }

            if (McUtils.player() == null || clientTick.currentScreen == null) return;

            ScreenHandler menu = McUtils.containerMenu();
            if (menu == null) return;

            // Step 1: Click "Party Queue" button (slot 49)
            if (!clickedQueue.get() && menu.slots.size() > 49) {
                Slot slot = menu.getSlot(49);
                if (slot != null && slot.getStack() != null && slot.getStack().getName() != null) {
                    String name = slot.getStack().getName().getString();
                    if (name.contains("Queue")) {
                        if (debugMode) {
                            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§e[Debug] Clicking Party Queue (slot 49)"));
                            System.out.println("[RaidDebug] Clicking Party Queue at slot 49");
                        }
                        clickOnSlot(49, menu.syncId, 0, menu.getStacks());
                        clickedQueue.set(true);
                        ticksSinceQueueClick[0] = 0;
                        return;
                    }
                }
            }

            // Wait 8 ticks after clicking queue before clicking raid
            if (clickedQueue.get() && !clickedRaid.get()) {
                ticksSinceQueueClick[0]++;
                if (ticksSinceQueueClick[0] < 8) {
                    return; // Wait more
                }
            }

            // Step 2: Click the specific raid button - search by name instead of hardcoded slot
            if (clickedQueue.get() && !clickedRaid.get() && menu.slots.size() > 20) {
                // Search for the raid by name in slots 10-20
                String searchName = switch (pendingRaidJoin) {
                    case "NOTG" -> "Nest of the Grootslangs";
                    case "NOL" -> "Orphion's Nexus of Light";
                    case "TCC" -> "The Canyon Colossus";
                    case "TNA" -> "The Nameless Anomaly";
                    default -> "";
                };

                if (debugMode) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§e[Debug] Searching for raid: " + searchName));
                    System.out.println("[RaidDebug] Searching for raid: " + searchName);
                }

                for (int slotIdx = 10; slotIdx <= 20; slotIdx++) {
                    if (menu.slots.size() <= slotIdx) continue;
                    Slot slot = menu.getSlot(slotIdx);
                    if (slot != null && slot.getStack() != null && slot.getStack().getName() != null) {
                        String itemName = slot.getStack().getName().getString();
                        if (debugMode) {
                            System.out.println("[RaidDebug] Slot " + slotIdx + ": " + itemName);
                        }
                        if (itemName.contains(searchName) || itemName.contains(pendingRaidJoin)) {
                            if (debugMode) {
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§e[Debug] Found " + pendingRaidJoin + " at slot " + slotIdx));
                            }
                            clickOnSlot(slotIdx, menu.syncId, 0, menu.getStacks());
                            clickedRaid.set(true);
                            pendingRaidJoin = null;
                            return;
                        }
                    }
                }

                if (debugMode) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§c[Debug] Could not find raid " + pendingRaidJoin + " in menu"));
                }
            }
        });
    }

    private ItemStack createAspectFlameIcon(ApiAspect apiAspect, boolean isMaxed) {
        if (apiAspect == null || apiAspect.getIcon() == null) {
            return ItemStack.EMPTY;
        }

        try {
            ApiAspect.IconValue iv = apiAspect.getIcon().getValueObject();
            if (iv == null) return ItemStack.EMPTY;

            // Get the item from API
            Identifier id = Identifier.of(iv.getId());
            Item item = Registries.ITEM.get(id);
            ItemStack stack = new ItemStack(item);

            // Set custom model data (same as AspectsTabWidget in Profile Viewer)
            if (iv.getCustomModelData() != null && iv.getCustomModelData().getRangeDispatch() != null) {
                int cmd = iv.getCustomModelData().getRangeDispatch().getFirst() + (isMaxed ? 1 : 0);
                stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of((float) cmd), List.of(), List.of(), List.of()));
            }

            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private ApiAspect findApiAspectByName(String name) {
        try {
            List<ApiAspect> allAspects = WynncraftApiHandler.fetchAllAspects();
            return allAspects.stream()
                .filter(a -> a.getName().equals(name))
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }


    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private String truncateToWidth(String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }

        // Binary search for the right length
        int left = 0;
        int right = text.length();
        String ellipsis = "...";
        int ellipsisWidth = textRenderer.getWidth(ellipsis);

        while (left < right) {
            int mid = (left + right + 1) / 2;
            String truncated = text.substring(0, mid) + ellipsis;
            if (textRenderer.getWidth(truncated) <= maxWidth) {
                left = mid;
            } else {
                right = mid - 1;
            }
        }

        if (left == 0) return "...";
        return text.substring(0, left) + ellipsis;
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (textRenderer.getWidth(testLine) <= maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public static void open() {
        MinecraftClient.getInstance().setScreen(new AspectScreenSimple());
    }

    public static void openLootPool() {
        AspectScreenSimple screen = new AspectScreenSimple();
        screen.currentPage = 0;
        MinecraftClient.getInstance().setScreen(screen);
    }

    public static void openMyAspects() {
        AspectScreenSimple screen = new AspectScreenSimple();
        screen.currentPage = 1;
        MinecraftClient.getInstance().setScreen(screen);
    }

    public static void openGambits() {
        AspectScreenSimple screen = new AspectScreenSimple();
        screen.currentPage = 2; // Updated after page reorder
        MinecraftClient.getInstance().setScreen(screen);
    }

    public static void openRaidLoot() {
        AspectScreenSimple screen = new AspectScreenSimple();
        screen.currentPage = 3;
        MinecraftClient.getInstance().setScreen(screen);
    }

    public static void openExplore() {
        AspectScreenSimple screen = new AspectScreenSimple();
        screen.currentPage = 4;
        MinecraftClient.getInstance().setScreen(screen);
    }

    public static void openLeaderboard() {
        AspectScreenSimple screen = new AspectScreenSimple();
        screen.currentPage = 5;
        MinecraftClient.getInstance().setScreen(screen);
    }

    public static void openPlayer(String playerName) {
        AspectScreenSimple screen = new AspectScreenSimple();
        screen.currentPage = 1; // My Aspects page
        screen.searchInput = playerName;
        screen.performPlayerSearch(playerName);
        MinecraftClient.getInstance().setScreen(screen);
    }

    public static void toggleDebug() {
        debugMode = !debugMode;
        if (debugMode) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aRaid slot debug mode ENABLED"));
            System.out.println("[WynnExtras] Raid debug mode enabled");
        } else {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cRaid slot debug mode DISABLED"));
            System.out.println("[WynnExtras] Raid debug mode disabled");
        }
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr = (char) input.codepoint();
        if (searchInputFocused) {
            // Insert character at cursor position
            searchInput = searchInput.substring(0, searchCursorPos) + chr + searchInput.substring(searchCursorPos);
            searchCursorPos++;
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (searchInputFocused) {
            if (keyCode == 259) { // Backspace
                if (searchCursorPos > 0) {
                    searchInput = searchInput.substring(0, searchCursorPos - 1) + searchInput.substring(searchCursorPos);
                    searchCursorPos--;
                }
                return true;
            } else if (keyCode == 261) { // Delete
                if (searchCursorPos < searchInput.length()) {
                    searchInput = searchInput.substring(0, searchCursorPos) + searchInput.substring(searchCursorPos + 1);
                }
                return true;
            } else if (keyCode == 263) { // Left arrow
                if (searchCursorPos > 0) {
                    searchCursorPos--;
                }
                return true;
            } else if (keyCode == 262) { // Right arrow
                if (searchCursorPos < searchInput.length()) {
                    searchCursorPos++;
                }
                return true;
            } else if (keyCode == 257 || keyCode == 335) { // Enter or numpad enter
                // Trigger search
                if (!searchInput.isEmpty()) {
                    performPlayerSearch(searchInput);
                    searchInputFocused = false;
                }
                return true;
            } else if (keyCode == 256) { // Escape
                searchInputFocused = false;
                return false; // Let it close the screen
            }
            return true;
        }
        return super.keyPressed(input);
    }

    private void renderExplorePage(DrawContext context, int mouseX, int mouseY) {
        int logicalW = getLogicalWidth();
        int logicalH = getLogicalHeight();
        int centerX = logicalW / 2;

        // Title
        drawCenteredText(context, "§6§lEXPLORE PLAYERS", centerX, 60);
        drawCenteredText(context, "§7Browse all players who have scanned their aspects", centerX, 110);

        // Fetch player list on first load
        if (!fetchedPlayerList) {
            fetchedPlayerList = true;
            WynncraftApiHandler.fetchPlayerList().thenAccept(result -> {
                playerList = result;
            });
        }

        // Show loading or data
        if (playerList == null) {
            drawCenteredText(context, "§eLoading players...", centerX, 200);
            return;
        }

        if (playerList.isEmpty()) {
            drawCenteredText(context, "§cNo players found", centerX, 200);
            return;
        }

        // Convert mouse to logical
        int logicalMouseX = (int)(mouseX * scaleFactor);
        int logicalMouseY = (int)(mouseY * scaleFactor);

        // Sort filter buttons
        int filterButtonWidth = 300;
        int filterButtonHeight = 50;
        int filterSpacing = 30;
        int filterY = 125;
        int totalFilterWidth = (filterButtonWidth * 2) + filterSpacing;
        int filterStartX = (logicalW - totalFilterWidth) / 2;

        String[] filterNames = {"Most Aspects", "Username (A-Z)"};
        for (int i = 0; i < 2; i++) {
            int fx = filterStartX + (i * (filterButtonWidth + filterSpacing));
            boolean active = exploreSortMode == i;
            boolean hovering = logicalMouseX >= fx && logicalMouseX <= fx + filterButtonWidth &&
                              logicalMouseY >= filterY && logicalMouseY <= filterY + filterButtonHeight;

            // Draw textured button
            if (ui != null) {
                ui.drawButtonFade(fx, filterY, filterButtonWidth, filterButtonHeight, 12, hovering || active);
            }

            String text = active ? "§6§l" + filterNames[i] : "§7" + filterNames[i];
            drawCenteredText(context, text, fx + filterButtonWidth / 2, filterY + filterButtonHeight / 2);
        }

        // Sort the player list based on mode
        List<julianh06.wynnextras.features.profileviewer.data.PlayerListEntry> sortedList = new java.util.ArrayList<>(playerList);
        if (exploreSortMode == 0) {
            // Sort by most aspects (descending)
            sortedList.sort((a, b) -> Integer.compare(b.getAspectCount(), a.getAspectCount()));
        } else {
            // Sort by username (A-Z)
            sortedList.sort((a, b) -> a.getPlayerName().compareToIgnoreCase(b.getPlayerName()));
        }

        // Display players in a grid - 3 columns
        int columnCount = 3;
        int entryWidth = 280;
        int entryHeight = 85;
        int spacing = 30;
        int startY = 190;

        int totalWidth = (entryWidth * columnCount) + (spacing * (columnCount - 1));
        int startX = (logicalW - totalWidth) / 2;

        int perPage = 15; // 5 rows × 3 columns
        int maxEntries = Math.min(sortedList.size(), perPage);

        for (int i = 0; i < maxEntries; i++) {
            julianh06.wynnextras.features.profileviewer.data.PlayerListEntry player = sortedList.get(i);

            int row = i / columnCount;
            int col = i % columnCount;
            int x = startX + (col * (entryWidth + spacing));
            int y = startY + (row * (entryHeight + spacing));

            // Check if hovering
            boolean hovering = logicalMouseX >= x && logicalMouseX <= x + entryWidth &&
                              logicalMouseY >= y && logicalMouseY <= y + entryHeight;

            // Background box (darker when hovering)
            int bgColor = hovering ? 0xCC1a1a1a : 0xAA000000;
            drawRect(x, y, entryWidth, entryHeight, bgColor);

            // Border (golden when hovering)
            int borderColor = hovering ? 0xFFFFAA00 : 0xFF4e392d;
            drawRect(x, y, entryWidth, 3, borderColor); // top
            drawRect(x, y + entryHeight - 3, entryWidth, 3, borderColor); // bottom
            drawRect(x, y, 3, entryHeight, borderColor); // left
            drawRect(x + entryWidth - 3, y, 3, entryHeight, borderColor); // right

            // Player name (centered, larger)
            drawCenteredText(context, "§6§l" + player.getPlayerName(), x + entryWidth / 2, y + 25);

            // Aspect count - show "Total | Max" format if max count is available
            int maxCount = player.getMaxAspectCount();
            if (maxCount > 0) {
                drawCenteredText(context, "§e" + player.getAspectCount() + " §7Total §8| §a" + maxCount + " §7Max", x + entryWidth / 2, y + 60);
            } else {
                drawCenteredText(context, "§e" + player.getAspectCount() + " §7aspects", x + entryWidth / 2, y + 60);
            }
        }

        // Instructions above navigation
        drawCenteredText(context, "§7Click on a player to view their aspects", centerX, logicalH - 165);
    }

    private void renderLeaderboardPage(DrawContext context, int mouseX, int mouseY) {
        int logicalW = getLogicalWidth();
        int logicalH = getLogicalHeight();
        int centerX = logicalW / 2;

        // Title
        drawCenteredText(context, "§6§lLEADERBOARD", centerX, 60);
        drawCenteredText(context, "§7Top 15 players with the most maxed aspects", centerX, 110);

        // Fetch leaderboard on first load
        if (!fetchedLeaderboard) {
            fetchedLeaderboard = true;
            WynncraftApiHandler.fetchLeaderboard(15).thenAccept(result -> {
                leaderboardList = result;
            });
        }

        // Show loading or data
        if (leaderboardList == null) {
            drawCenteredText(context, "§eLoading leaderboard...", centerX, 200);
            return;
        }

        if (leaderboardList.isEmpty()) {
            drawCenteredText(context, "§cNo leaderboard data", centerX, 200);
            return;
        }

        // Display leaderboard entries
        int entryWidth = 800;
        int entryHeight = 50;
        int spacing = 8;
        int startY = 180;
        int startX = centerX - entryWidth / 2;

        // Convert mouse to logical
        int logicalMouseX = (int)(mouseX * scaleFactor);
        int logicalMouseY = (int)(mouseY * scaleFactor);

        for (int i = 0; i < leaderboardList.size(); i++) {
            julianh06.wynnextras.features.profileviewer.data.LeaderboardEntry entry = leaderboardList.get(i);
            int y = startY + (i * (entryHeight + spacing));

            // Check if hovering
            boolean hovering = logicalMouseX >= startX && logicalMouseX <= startX + entryWidth &&
                              logicalMouseY >= y && logicalMouseY <= y + entryHeight;

            // Background box (darker when hovering)
            int bgColor = hovering ? 0xCC1a1a1a : 0xAA000000;
            drawRect(startX, y, entryWidth, entryHeight, bgColor);

            // Border (golden for top 3, normal otherwise)
            int borderColor;
            if (i == 0) {
                borderColor = 0xFFFFD700; // Gold for 1st
            } else if (i == 1) {
                borderColor = 0xFFC0C0C0; // Silver for 2nd
            } else if (i == 2) {
                borderColor = 0xFFCD7F32; // Bronze for 3rd
            } else if (hovering) {
                borderColor = 0xFFFFAA00; // Golden when hovering
            } else {
                borderColor = 0xFF4e392d; // Normal
            }

            drawRect(startX, y, entryWidth, 3, borderColor); // top
            drawRect(startX, y + entryHeight - 3, entryWidth, 3, borderColor); // bottom
            drawRect(startX, y, 3, entryHeight, borderColor); // left
            drawRect(startX + entryWidth - 3, y, 3, entryHeight, borderColor); // right

            // Rank (left)
            String rankText = "§7#" + (i + 1);
            if (i == 0) rankText = "§6§l#1";
            else if (i == 1) rankText = "§f§l#2";
            else if (i == 2) rankText = "§6§l#3";

            drawLeftText(context, rankText, startX + 30, y + entryHeight / 2 - 5);

            // Player name (center-left)
            drawLeftText(context, "§6" + entry.getPlayerName(), startX + 120, y + entryHeight / 2 - 5);

            // Max aspect count (right)
            String countText = "§a§l" + entry.getMaxAspectCount() + " §7maxed";
            drawLeftText(context, countText, startX + entryWidth - 200, y + entryHeight / 2 - 5);
        }

        // Instructions above navigation
        drawCenteredText(context, "§7Click on a player to view their aspects", centerX, logicalH - 165);
    }

    private void renderRaidLootPage(DrawContext context, int mouseX, int mouseY) {
        int logicalW = getLogicalWidth();
        int logicalH = getLogicalHeight();
        int centerX = logicalW / 2;

        // Convert mouse to logical
        int logicalMouseX = (int)(mouseX * scaleFactor);
        int logicalMouseY = (int)(mouseY * scaleFactor);

        // Title
        drawCenteredText(context, "§6§lRAID LOOT TRACKER", centerX, 60);

        // Raid toggles (4 buttons in a row)
        int toggleWidth = 220;
        int toggleHeight = 50;
        int toggleSpacing = 20;
        int toggleY = 120;
        String[] raidNames = {"NOTG", "NOL", "TCC", "TNA"};
        String[] raidColors = {"§5", "§b", "§c", "§e"};

        int totalToggleWidth = (toggleWidth * 4) + (toggleSpacing * 3);
        int toggleStartX = (logicalW - totalToggleWidth) / 2;

        for (int i = 0; i < 4; i++) {
            int x = toggleStartX + (i * (toggleWidth + toggleSpacing));
            boolean active = raidToggles[i];
            boolean hovering = logicalMouseX >= x && logicalMouseX <= x + toggleWidth &&
                              logicalMouseY >= toggleY && logicalMouseY <= toggleY + toggleHeight;

            // Draw textured button
            if (ui != null) {
                ui.drawButtonFade(x, toggleY, toggleWidth, toggleHeight, 12, hovering || active);
            }

            // Text - centered in button
            String text = active ? raidColors[i] + "§l" + raidNames[i] : "§7" + raidNames[i];
            drawCenteredText(context, text, x + toggleWidth / 2, toggleY + toggleHeight / 2);
        }

        // Show Rates button
        int ratesButtonWidth = 450;
        int ratesButtonHeight = 50;
        int ratesButtonX = centerX - ratesButtonWidth / 2;
        int ratesButtonY = 180;
        boolean hoveringRates = logicalMouseX >= ratesButtonX && logicalMouseX <= ratesButtonX + ratesButtonWidth &&
                               logicalMouseY >= ratesButtonY && logicalMouseY <= ratesButtonY + ratesButtonHeight;

        // Draw textured button
        if (ui != null) {
            ui.drawButtonFade(ratesButtonX, ratesButtonY, ratesButtonWidth, ratesButtonHeight, 12, hoveringRates);
        }

        String ratesText = showRates ? "§a§lShowing: Average/Run" : "§e§lShowing: Totals";
        drawCenteredText(context, ratesText, ratesButtonX + ratesButtonWidth / 2, ratesButtonY + ratesButtonHeight / 2);

        // Get loot tracker data
        RaidLootData lootData = RaidLootConfig.INSTANCE.data;

        // Calculate combined stats based on selected raids
        RaidLootData.RaidSpecificLoot combinedStats = new RaidLootData.RaidSpecificLoot();
        int totalRuns = 0;

        String[] raidCodes = {"NOTG", "NOL", "TCC", "TNA"};
        for (int i = 0; i < 4; i++) {
            if (raidToggles[i]) {
                RaidLootData.RaidSpecificLoot raidStats = lootData.perRaidData.get(raidCodes[i]);
                if (raidStats != null) {
                    combinedStats.emeraldBlocks += raidStats.emeraldBlocks;
                    combinedStats.liquidEmeralds += raidStats.liquidEmeralds;
                    combinedStats.amplifierTier1 += raidStats.amplifierTier1;
                    combinedStats.amplifierTier2 += raidStats.amplifierTier2;
                    combinedStats.amplifierTier3 += raidStats.amplifierTier3;
                    combinedStats.totalBags += raidStats.totalBags;
                    combinedStats.stuffedBags += raidStats.stuffedBags;
                    combinedStats.packedBags += raidStats.packedBags;
                    combinedStats.variedBags += raidStats.variedBags;
                    combinedStats.totalTomes += raidStats.totalTomes;
                    combinedStats.mythicTomes += raidStats.mythicTomes;
                    combinedStats.fabledTomes += raidStats.fabledTomes;
                    combinedStats.totalCharms += raidStats.totalCharms;
                    combinedStats.completionCount += raidStats.completionCount;
                }
            }
        }
        totalRuns = combinedStats.completionCount;

        // Display stats
        int startY = 260;
        int lineHeight = 35;

        drawCenteredText(context, "§6§lSTATISTICS", centerX, startY);
        startY += 50;

        // Calculate emeralds
        long totalEmeralds = (combinedStats.liquidEmeralds * 64 * 64) + (combinedStats.emeraldBlocks * 64);
        long stacks = totalEmeralds / 262144;
        long remainingAfterStx = totalEmeralds % 262144;
        long le = remainingAfterStx / 4096;
        long remainingAfterLE = remainingAfterStx % 4096;
        long eb = remainingAfterLE / 64;

        if (showRates && totalRuns > 0) {
            // Show averages per run
            drawCenteredText(context, "§6§lTotal Runs: §f" + totalRuns, centerX, startY);
            startY += lineHeight;

            // Calculate total LE (including stx converted to LE)
            long totalLeValue = (stacks * 64) + le;
            double avgLe = (double) totalLeValue / totalRuns;

            String emeraldAvgText;
            if (avgLe >= 64) {
                // Show in stx if average is at least 1 stx (64 LE)
                emeraldAvgText = String.format("%.2fstx", avgLe / 64);
            } else {
                emeraldAvgText = String.format("%.1fle", avgLe);
            }
            drawCenteredText(context, "§a§lEmeralds/Run: §f" + emeraldAvgText, centerX, startY);
            startY += lineHeight;

            drawCenteredText(context, "§e§lAmplifiers/Run: §f" + String.format("%.2f", (double)combinedStats.getTotalAmplifiers() / totalRuns), centerX, startY);
            startY += lineHeight + 8;

            drawCenteredText(context, "§b§lBags/Run: §f" + String.format("%.2f", (double)combinedStats.totalBags / totalRuns), centerX, startY);
            startY += lineHeight + 8;

            drawCenteredText(context, "§d§lTomes/Run: §f" + String.format("%.2f", (double)combinedStats.totalTomes / totalRuns) + " §7(§5" + String.format("%.2f", (double)combinedStats.mythicTomes / totalRuns) + " §7mythic§7)", centerX, startY);
            startY += lineHeight + 8;

            drawCenteredText(context, "§5§lAspects/Run: §f" + String.format("%.2f", (double)combinedStats.totalAspects / totalRuns) + " §7(§5" + String.format("%.2f", (double)combinedStats.mythicAspects / totalRuns) + " §7mythic§7)", centerX, startY);
            startY += lineHeight + 25;

        } else {
            // Show totals
            drawCenteredText(context, "§6§lTotal Runs: §f" + totalRuns, centerX, startY);
            startY += lineHeight;

            // Build emerald text - always show stx + le if stx exists
            StringBuilder emeraldText = new StringBuilder();
            if (stacks > 0) {
                emeraldText.append(stacks).append("stx");
                if (le > 0) {
                    emeraldText.append(" + ").append(le).append("le");
                }
            } else if (le > 0) {
                emeraldText.append(le).append("le");
            } else {
                emeraldText.append(eb).append("eb");
            }
            drawCenteredText(context, "§a§lEmeralds: §f" + emeraldText.toString(), centerX, startY);
            startY += lineHeight;

            drawCenteredText(context, "§e§lAmplifiers: §f" + combinedStats.getTotalAmplifiers() + " §7(I: " + combinedStats.amplifierTier1 + " | II: " + combinedStats.amplifierTier2 + " | III: " + combinedStats.amplifierTier3 + ")", centerX, startY);
            startY += lineHeight + 8;

            drawCenteredText(context, "§b§lBags: §f" + combinedStats.totalBags + " §7(Stuffed: " + combinedStats.stuffedBags + " | Packed: " + combinedStats.packedBags + " | Varied: " + combinedStats.variedBags + ")", centerX, startY);
            startY += lineHeight + 8;

            drawCenteredText(context, "§d§lTomes: §f" + combinedStats.totalTomes + " §7(§5" + combinedStats.mythicTomes + " §7mythic, §d" + combinedStats.fabledTomes + " §7fabled§7)", centerX, startY);
            startY += lineHeight + 8;

            drawCenteredText(context, "§5§lAspects: §f" + combinedStats.totalAspects + " §7(§5" + combinedStats.mythicAspects + " §7mythic, §c" + combinedStats.fabledAspects + " §7fabled, §b" + combinedStats.legendaryAspects + " §7legendary§7)", centerX, startY);
            startY += lineHeight + 25;
        }

        // Per-raid breakdown
        if (raidToggles[0] || raidToggles[1] || raidToggles[2] || raidToggles[3]) {
            drawCenteredText(context, "§e§lPER-RAID BREAKDOWN", centerX, startY);
            startY += 40;

            for (int i = 0; i < 4; i++) {
                if (!raidToggles[i]) continue;

                RaidLootData.RaidSpecificLoot raidStats = lootData.perRaidData.get(raidCodes[i]);
                if (raidStats == null) {
                    drawCenteredText(context, raidColors[i] + "§l" + raidNames[i] + ": §7No data", centerX, startY);
                    startY += 30;
                    continue;
                }

                int runs = raidStats.completionCount;
                long raidTotalEmeralds = (raidStats.liquidEmeralds * 64 * 64) + (raidStats.emeraldBlocks * 64);
                long raidStacks = raidTotalEmeralds / 262144;
                long raidRemaining = raidTotalEmeralds % 262144;
                long raidLe = raidRemaining / 4096;
                long raidRemaining2 = raidRemaining % 4096;
                long raidEb = raidRemaining2 / 64;

                if (showRates && runs > 0) {
                    // Calculate total LE (including stx converted to LE)
                    // 1 stx = 64 LE, so totalLE = (stacks * 64) + remainderLE
                    long totalLeValue = (raidStacks * 64) + raidLe;
                    double avgLe = (double) totalLeValue / runs;

                    String emeraldText;
                    if (avgLe >= 64) {
                        // Show in stx if average is at least 1 stx (64 LE)
                        emeraldText = String.format("%.2fstx/run", avgLe / 64);
                    } else {
                        emeraldText = String.format("%.1fle/run", avgLe);
                    }
                    drawCenteredText(context, raidColors[i] + "§l" + raidNames[i] + ": §f" + runs + " runs §8| §a" + emeraldText, centerX, startY);
                } else {
                    // Show totals - always show stx + le if both exist
                    String emeraldText;
                    if (raidStacks > 0 && raidLe > 0) {
                        emeraldText = raidStacks + "stx + " + raidLe + "le";
                    } else if (raidStacks > 0) {
                        emeraldText = raidStacks + "stx";
                    } else if (raidLe > 0) {
                        emeraldText = raidLe + "le";
                    } else {
                        emeraldText = raidEb + "eb";
                    }
                    drawCenteredText(context, raidColors[i] + "§l" + raidNames[i] + ": §f" + runs + " runs §8| §a" + emeraldText + " total", centerX, startY);
                }
                startY += 30;
            }
        }
    }

    private void performPlayerSearch(String playerName) {
        searchedPlayer = playerName;
        searchedPlayerData = null;
        searchedPlayerStatus = null; // null = loading

        String requestingUUID = McUtils.player() != null ? McUtils.player().getUuidAsString() : null;
        final String expectedPlayer = playerName; // Capture to detect race condition

        // First convert username to UUID
        WynncraftApiHandler.fetchUUID(playerName).thenCompose(rawUUID -> {
            // Check if search target changed while we were fetching UUID
            if (!expectedPlayer.equals(searchedPlayer)) {
                return CompletableFuture.completedFuture(null);
            }
            if (rawUUID == null) {
                searchedPlayerStatus = WynncraftApiHandler.FetchStatus.NOT_FOUND;
                return CompletableFuture.completedFuture(null);
            }

            // Format UUID and fetch aspect data
            String formattedUUID = WynncraftApiHandler.formatUUID(rawUUID);
            return WynncraftApiHandler.fetchPlayerAspectData(formattedUUID, requestingUUID);
        }).thenAccept(result -> {
            // Only update if we're still searching for the same player
            if (!expectedPlayer.equals(searchedPlayer)) return;
            if (result == null) return;
            if (result.status() != null) {
                if (result.status() == WynncraftApiHandler.FetchStatus.OK) {
                    searchedPlayerData = result.user();
                    // Add to recent searches on success
                    addToRecentSearches(playerName);
                }
                searchedPlayerStatus = result.status();
            }
        }).exceptionally(ex -> {
            // Only log/update if we're still searching for the same player
            if (expectedPlayer.equals(searchedPlayer)) {
                System.err.println("[WynnExtras] Error fetching aspects for " + playerName + ": " + ex.getMessage());
                searchedPlayerStatus = WynncraftApiHandler.FetchStatus.UNKNOWN_ERROR;
            }
            return null;
        });
    }

    private void addToRecentSearches(String playerName) {
        // Use FavoriteAspectsData for persistence
        FavoriteAspectsData.INSTANCE.addRecentSearch(playerName);
    }

    // === AspectScreenHost Implementation ===

    @Override
    public int getHostLogicalWidth() {
        return getLogicalWidth();
    }

    @Override
    public int getHostLogicalHeight() {
        return getLogicalHeight();
    }

    @Override
    public double getScaleFactor() {
        return scaleFactor;
    }

    @Override
    public MinecraftClient getClient() {
        return client;
    }

    @Override
    public int getHostSafeColumnWidth(int numColumns, int spacing) {
        return getSafeColumnWidth(numColumns, spacing);
    }

    @Override
    public void setCurrentPage(int page) {
        this.currentPage = page;
    }

    @Override
    public int getCurrentPage() {
        return currentPage;
    }

    @Override
    public void setHoveredAspect(LootPoolData.AspectEntry aspect, int x, int y, int columnX) {
        this.hoveredAspect = aspect;
        this.hoveredAspectX = x;
        this.hoveredAspectY = y;
        this.hoveredAspectColumnX = columnX;
    }

    @Override
    public void clearHoveredAspect() {
        this.hoveredAspect = null;
    }

    @Override
    public void searchPlayer(String playerName) {
        // Switch to My Aspects page and search for player
        currentPage = 1;
        performPlayerSearch(playerName);
    }

    @Override
    public List<ApiAspect> getAllApiAspects() {
        return WynncraftApiHandler.fetchAllAspects();
    }
}
