package julianh06.wynnextras.features.achievements;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

public class AchievementScreen extends WEScreen {

    private static final int TAB_OVERVIEW = -1;
    private static final int TAB_ALL = 0;

    private int selectedTab = TAB_OVERVIEW; // -1 = overview, 0 = all, 1+ = category index
    private AchievementCategory selectedCategory = null; // null = show all
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;

    public AchievementScreen() {
        super(Text.of("WynnExtras Achievements"));
    }

    public static void open() {
        WEScreen.open(AchievementScreen::new);
    }

    @Override
    protected void init() {
        super.init();
        registerScrolling();
        // Initialize the achievement manager if not already done
        AchievementManager.INSTANCE.initialize();
    }

    @Override
    protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
        int logicalW = getLogicalWidth();
        int logicalH = getLogicalHeight();
        int centerX = logicalW / 2;

        // Convert mouse coords to logical
        int logicalMouseX = (int)(mouseX * scaleFactor);
        int logicalMouseY = (int)(mouseY * scaleFactor);

        // Header - "WynnExtras Achievements" on top
        if (ui != null) {
            ui.drawCenteredText("§e§lWynnExtras Achievements", centerX, 30, CustomColor.fromInt(0xFFFFFF), 3.5f);
        }

        // Draw category tabs on the left
        int tabX = 40;
        int tabY = 120;
        int tabHeight = 50;
        int tabWidth = 200;
        int tabSpacing = 10;

        // "Overview" tab
        boolean overviewHovered = isInBounds(logicalMouseX, logicalMouseY, tabX, tabY, tabWidth, tabHeight);
        int overviewColor = selectedTab == TAB_OVERVIEW ? 0xFF4a7c59 : (overviewHovered ? 0xFF3d3d3d : 0xFF2a2a2a);
        drawRect(tabX, tabY, tabWidth, tabHeight, overviewColor);
        drawRect(tabX, tabY, tabWidth, 4, 0xFFFFAA00);
        if (ui != null) {
            ui.drawCenteredText("§e⭐ Overview", tabX + tabWidth / 2, tabY + tabHeight / 2 - 5, CustomColor.fromInt(0xFFFFFF), 2.5f);
        }
        tabY += tabHeight + tabSpacing;

        // "All" tab
        boolean allHovered = isInBounds(logicalMouseX, logicalMouseY, tabX, tabY, tabWidth, tabHeight);
        int allColor = selectedTab == TAB_ALL ? 0xFF4a7c59 : (allHovered ? 0xFF3d3d3d : 0xFF2a2a2a);
        drawRect(tabX, tabY, tabWidth, tabHeight, allColor);
        drawRect(tabX, tabY, tabWidth, 4, 0xFF555555);
        if (ui != null) {
            ui.drawCenteredText("§fAll", tabX + tabWidth / 2, tabY + tabHeight / 2 - 10, CustomColor.fromInt(0xFFFFFF), 2.5f);
            String allProgress = getProgressText(AchievementManager.INSTANCE.getAllAchievements());
            ui.drawCenteredText("§7" + allProgress, tabX + tabWidth / 2, tabY + tabHeight / 2 + 10, CustomColor.fromInt(0xAAAAAA), 2f);
        }
        tabY += tabHeight + tabSpacing;

        // Category tabs
        int catIndex = 1;
        for (AchievementCategory category : AchievementCategory.values()) {
            boolean hovered = isInBounds(logicalMouseX, logicalMouseY, tabX, tabY, tabWidth, tabHeight);
            int bgColor = selectedTab == catIndex ? 0xFF4a7c59 : (hovered ? 0xFF3d3d3d : 0xFF2a2a2a);
            drawRect(tabX, tabY, tabWidth, tabHeight, bgColor);
            drawRect(tabX, tabY, tabWidth, 4, category.getColor() | 0xFF000000);

            if (ui != null) {
                ui.drawCenteredText("§f" + category.getDisplayName(), tabX + tabWidth / 2, tabY + tabHeight / 2 - 10, CustomColor.fromInt(0xFFFFFF), 2.5f);
                List<Achievement> catAchievements = AchievementManager.INSTANCE.getAchievementsByCategory(category);
                String catProgress = getProgressText(catAchievements);
                ui.drawCenteredText("§7" + catProgress, tabX + tabWidth / 2, tabY + tabHeight / 2 + 10, CustomColor.fromInt(0xAAAAAA), 2f);
            }

            tabY += tabHeight + tabSpacing;
            catIndex++;
        }

        // Content area on the right
        int listX = 280;
        int listY = 120;
        int listWidth = logicalW - listX - 40;
        int listHeight = logicalH - listY - 40;

        // Background for content
        drawRect(listX, listY, listWidth, listHeight, 0xAA000000);
        drawRect(listX, listY, listWidth, 4, 0xFF4e392d);
        drawRect(listX, listY + listHeight - 4, listWidth, 4, 0xFF4e392d);
        drawRect(listX, listY, 4, listHeight, 0xFF4e392d);
        drawRect(listX + listWidth - 4, listY, 4, listHeight, 0xFF4e392d);

        if (selectedTab == TAB_OVERVIEW) {
            // Draw Overview page with category progress bars
            drawOverviewPage(context, listX, listY, listWidth, listHeight, logicalMouseX, logicalMouseY);
        } else {
            // Draw achievement list
            drawAchievementList(context, listX, listY, listWidth, listHeight, logicalMouseX, logicalMouseY);
        }
    }

    private void drawOverviewPage(DrawContext context, int listX, int listY, int listWidth, int listHeight,
                                   int logicalMouseX, int logicalMouseY) {
        if (ui == null) return;

        int y = listY + 30;
        int barHeight = 35; // Slightly bigger progress bars
        int barSpacing = 65;
        int padding = 40;

        // Title - "Achievement Progress" below the main header
        ui.drawCenteredText("§7Achievement Progress", listX + listWidth / 2, y, CustomColor.fromInt(0xAAAAAA), 2.5f);
        y += 45;

        // Overall progress - count tiers for tiered achievements
        float overallProgress = calculateWeightedProgress(AchievementManager.INSTANCE.getAllAchievements());
        String overallText = getProgressText(AchievementManager.INSTANCE.getAllAchievements());

        ui.drawText("§fOverall Progress", listX + padding, y, CustomColor.fromInt(0xFFFFFF), 2.5f);
        ui.drawText("§7" + overallText + " (" + String.format("%.1f", overallProgress * 100) + "%)",
                listX + listWidth - padding - 200, y, CustomColor.fromInt(0xAAAAAA), 2f);
        y += 25;
        drawCategoryProgressBar(listX + padding, y, listWidth - padding * 2, barHeight, overallProgress, 0xFFFFAA00);
        y += barSpacing;

        // Category progress bars
        for (AchievementCategory category : AchievementCategory.values()) {
            List<Achievement> catAchievements = AchievementManager.INSTANCE.getAchievementsByCategory(category);
            float progress = calculateWeightedProgress(catAchievements);
            String progressText = getProgressText(catAchievements);

            int catColor = category.getColor() | 0xFF000000;

            ui.drawText("§f" + category.getDisplayName(), listX + padding, y, CustomColor.fromInt(0xFFFFFF), 2.5f);
            ui.drawText("§7" + progressText + " (" + String.format("%.1f", progress * 100) + "%)",
                    listX + listWidth - padding - 200, y, CustomColor.fromInt(0xAAAAAA), 2f);
            y += 25;
            drawCategoryProgressBar(listX + padding, y, listWidth - padding * 2, barHeight, progress, catColor);
            y += barSpacing;
        }

        maxScrollOffset = 0; // No scrolling on overview
    }

    /**
     * Calculate weighted progress including tier progress for tiered achievements.
     * - Non-tiered: 0 or 1
     * - Tiered: Bronze=0.33, Silver=0.66, Gold=1.0
     */
    private float calculateWeightedProgress(java.util.Collection<Achievement> achievements) {
        if (achievements.isEmpty()) return 0f;

        float totalProgress = 0f;
        int totalWeight = 0;

        for (Achievement ach : achievements) {
            if (ach instanceof TieredAchievement tiered) {
                // For tiered achievements, count each tier as 1/3 progress
                // Bronze = 1/3, Silver = 2/3, Gold = 3/3
                TieredAchievement.TierLevel tier = tiered.getHighestUnlockedTier();
                float tierProgress = switch (tier) {
                    case NONE -> 0f;
                    case BRONZE -> 0.33f;
                    case SILVER -> 0.66f;
                    case GOLD -> 1f;
                };
                totalProgress += tierProgress;
                totalWeight += 1;
            } else {
                // Non-tiered: 0 or 1
                totalProgress += ach.isUnlocked() ? 1f : 0f;
                totalWeight += 1;
            }
        }

        return totalWeight > 0 ? totalProgress / totalWeight : 0f;
    }

    /**
     * Get progress text showing tiers completed / total tiers
     */
    private String getProgressText(java.util.Collection<Achievement> achievements) {
        int totalTiers = 0;
        int completedTiers = 0;

        for (Achievement ach : achievements) {
            if (ach instanceof TieredAchievement tiered) {
                // Each tiered achievement has 3 tiers
                totalTiers += 3;
                TieredAchievement.TierLevel tier = tiered.getHighestUnlockedTier();
                completedTiers += switch (tier) {
                    case NONE -> 0;
                    case BRONZE -> 1;
                    case SILVER -> 2;
                    case GOLD -> 3;
                };
            } else {
                // Non-tiered counts as 1 tier
                totalTiers += 1;
                completedTiers += ach.isUnlocked() ? 1 : 0;
            }
        }

        return completedTiers + "/" + totalTiers;
    }

    private void drawCategoryProgressBar(int x, int y, int width, int height, float progress, int fillColor) {
        // Background
        drawRect(x, y, width, height, 0xFF1a1a1a);

        // Fill
        int fillWidth = (int)(width * Math.min(1f, progress));
        if (fillWidth > 0) {
            drawRect(x, y, fillWidth, height, fillColor);
        }

        // Glow effect at fill edge
        if (fillWidth > 0 && fillWidth < width) {
            drawRect(x + fillWidth - 4, y, 4, height, (fillColor & 0x00FFFFFF) | 0x66000000);
        }

        // Border
        drawRect(x, y, width, 3, 0xFF333333);
        drawRect(x, y + height - 3, width, 3, 0xFF333333);
        drawRect(x, y, 3, height, 0xFF333333);
        drawRect(x + width - 3, y, 3, height, 0xFF333333);

        // Percentage text in center if bar is wide enough
        if (width > 100 && ui != null) {
            String percentText = String.format("%.0f%%", progress * 100);
            ui.drawCenteredText("§f" + percentText, x + width / 2, y + height / 2 - 8, CustomColor.fromInt(0xFFFFFF), 2f);
        }
    }

    private void drawAchievementList(DrawContext context, int listX, int listY, int listWidth, int listHeight,
                                      int logicalMouseX, int logicalMouseY) {
        // Get achievements to display
        List<Achievement> achievements;
        if (selectedTab == TAB_ALL) {
            achievements = List.copyOf(AchievementManager.INSTANCE.getAllAchievements());
        } else {
            AchievementCategory[] categories = AchievementCategory.values();
            int catIndex = selectedTab - 1;
            if (catIndex >= 0 && catIndex < categories.length) {
                selectedCategory = categories[catIndex];
                achievements = AchievementManager.INSTANCE.getAchievementsByCategory(selectedCategory);
            } else {
                achievements = List.copyOf(AchievementManager.INSTANCE.getAllAchievements());
            }
        }

        // Draw achievements
        int itemY = listY + 20 - scrollOffset;
        int itemHeight = 80;
        int itemSpacing = 10;
        int visibleAreaTop = listY + 10;
        int visibleAreaBottom = listY + listHeight - 10;

        for (Achievement achievement : achievements) {
            if (itemY + itemHeight > visibleAreaTop && itemY < visibleAreaBottom) {
                drawAchievementItem(context, listX + 20, itemY, listWidth - 40, itemHeight - itemSpacing,
                        achievement, logicalMouseX, logicalMouseY);
            }
            itemY += itemHeight;
        }

        // Calculate max scroll
        int totalContentHeight = achievements.size() * itemHeight;
        maxScrollOffset = Math.max(0, totalContentHeight - listHeight + 40);
    }

    private void drawAchievementItem(DrawContext context, int x, int y, int width, int height,
                                     Achievement achievement, int mouseX, int mouseY) {
        // Background
        boolean unlocked = achievement.isUnlocked();
        int bgColor = unlocked ? 0xAA2a4a2a : 0xAA1a1a1a;
        boolean hovered = isInBounds(mouseX, mouseY, x, y, width, height);
        if (hovered) {
            bgColor = unlocked ? 0xAA3a5a3a : 0xAA2a2a2a;
        }
        drawRect(x, y, width, height, bgColor);

        // Border color based on tier or unlock status
        int borderColor = 0xFF555555;
        if (achievement instanceof TieredAchievement tiered) {
            TieredAchievement.TierLevel tier = tiered.getHighestUnlockedTier();
            if (tier != TieredAchievement.TierLevel.NONE) {
                borderColor = tier.getColor() | 0xFF000000;
            }
        } else if (unlocked) {
            borderColor = 0xFF55FF55;
        }
        drawRect(x, y, 4, height, borderColor);

        if (ui != null) {
            // Title
            String title = achievement.getDisplayTitle();
            if (achievement instanceof TieredAchievement tiered) {
                TieredAchievement.TierLevel tier = tiered.getHighestUnlockedTier();
                if (tier != TieredAchievement.TierLevel.NONE) {
                    title = tier.getEmoji() + " " + title;
                }
            } else if (unlocked) {
                title = "§a✓ " + title;
            }
            ui.drawText(title, x + 20, y + 12, CustomColor.fromInt(0xFFFFFF), 2.5f);

            // Description
            String desc = achievement.getDisplayDescription();
            ui.drawText("§7" + desc, x + 20, y + 38, CustomColor.fromInt(0xAAAAAA), 2f);

            // Progress bar or completion status
            if (achievement instanceof TieredAchievement tiered) {
                drawProgressBar(x + 20, y + 54, width - 220, 14, tiered.getTotalProgress()); // Bigger bar, more left space
                ui.drawText("§7" + tiered.getProgressText(), x + width - 200, y + 52, CustomColor.fromInt(0xAAAAAA), 2f);
            } else if (achievement instanceof ProgressAchievement progress && !progress.isUnlocked()) {
                drawProgressBar(x + 20, y + 54, width - 180, 14, progress.getProgress()); // Bigger bar
                ui.drawText("§7" + progress.getProgressText(), x + width - 160, y + 52, CustomColor.fromInt(0xAAAAAA), 2f);
            } else if (unlocked && achievement.getUnlockedAt() != null) {
                ui.drawText("§6§lComplete!", x + 20, y + 52, CustomColor.fromInt(0xFFAA00), 2.2f); // Golden "Complete!"
            }
        }
    }

    private void drawProgressBar(int x, int y, int width, int height, float progress) {
        // Background
        drawRect(x, y, width, height, 0xFF222222);
        // Fill
        int fillWidth = (int)(width * Math.min(1f, progress));
        if (fillWidth > 0) {
            // Golden when complete, green otherwise
            int fillColor = progress >= 1f ? 0xFFFFAA00 : 0xFF4a7c59;
            drawRect(x, y, fillWidth, height, fillColor);
        }
        // Border
        drawRect(x, y, width, 2, 0xFF444444);
        drawRect(x, y + height - 2, width, 2, 0xFF444444);
        drawRect(x, y, 2, height, 0xFF444444);
        drawRect(x + width - 2, y, 2, height, 0xFF444444);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int logicalMouseX = (int)(mouseX * scaleFactor);
        int logicalMouseY = (int)(mouseY * scaleFactor);

        // Check tab clicks
        int tabX = 40;
        int tabY = 120;
        int tabHeight = 50;
        int tabWidth = 200;
        int tabSpacing = 10;

        // "Overview" tab
        if (isInBounds(logicalMouseX, logicalMouseY, tabX, tabY, tabWidth, tabHeight)) {
            selectedTab = TAB_OVERVIEW;
            selectedCategory = null;
            scrollOffset = 0;
            return true;
        }
        tabY += tabHeight + tabSpacing;

        // "All" tab
        if (isInBounds(logicalMouseX, logicalMouseY, tabX, tabY, tabWidth, tabHeight)) {
            selectedTab = TAB_ALL;
            selectedCategory = null;
            scrollOffset = 0;
            return true;
        }
        tabY += tabHeight + tabSpacing;

        // Category tabs
        int catIndex = 1;
        for (AchievementCategory category : AchievementCategory.values()) {
            if (isInBounds(logicalMouseX, logicalMouseY, tabX, tabY, tabWidth, tabHeight)) {
                selectedTab = catIndex;
                selectedCategory = category;
                scrollOffset = 0;
                return true;
            }
            tabY += tabHeight + tabSpacing;
            catIndex++;
        }

        return super.mouseClicked(click, doubleClick);
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
