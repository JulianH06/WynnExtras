package julianh06.wynnextras.features.aspects.oldPages;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.features.raid.RaidLootConfig;
import julianh06.wynnextras.features.raid.RaidLootData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

/**
 * Raid Loot page showing tracked loot statistics
 */
public class RaidLootPage extends AspectPage {
    private boolean[] raidToggles = {true, true, true, true}; // NOTG, NOL, TCC, TNA
    private boolean showRates = false;

    private static final int TOGGLE_WIDTH = 220;
    private static final int TOGGLE_HEIGHT = 50;
    private static final int TOGGLE_SPACING = 20;
    private static final int TOGGLE_Y = 120;

    private static final String[] RAID_NAMES = {"NOTG", "NOL", "TCC", "TNA"};
    private static final String[] RAID_COLORS = {"§5", "§b", "§c", "§e"};
    private static final String[] RAID_CODES = {"NOTG", "NOL", "TCC", "TNA"};

    // For hover tooltips
    private int amplifiersLineY = 0;
    private int bagsLineY = 0;
    private int lineHeight = 35;
    private RaidLootData.RaidSpecificLoot currentStats = null;
    private int currentTotalRuns = 0;

    public RaidLootPage(AspectScreenHost host) {
        super(host);
    }

    @Override
    public String getTitle() {
        return "Raid Loot";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        int logicalW = getLogicalWidth();
        int centerX = logicalW / 2;
        int logicalMouseX = toLogicalX(mouseX);
        int logicalMouseY = toLogicalY(mouseY);

        // Title
        drawCenteredText(context, "§6§lRAID LOOT TRACKER", centerX, 60);

        // Raid toggles
        int totalToggleWidth = (TOGGLE_WIDTH * 4) + (TOGGLE_SPACING * 3);
        int toggleStartX = (logicalW - totalToggleWidth) / 2;

        for (int i = 0; i < 4; i++) {
            int x = toggleStartX + (i * (TOGGLE_WIDTH + TOGGLE_SPACING));
            boolean active = raidToggles[i];
            boolean hovering = logicalMouseX >= x && logicalMouseX <= x + TOGGLE_WIDTH &&
                              logicalMouseY >= TOGGLE_Y && logicalMouseY <= TOGGLE_Y + TOGGLE_HEIGHT;

            if (ui != null) {
                ui.drawButtonFade(x, TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT, 12, hovering || active);
            }

            String text = active ? RAID_COLORS[i] + "§l" + RAID_NAMES[i] : "§7" + RAID_NAMES[i];
            drawCenteredText(context, text, x + TOGGLE_WIDTH / 2, TOGGLE_Y + TOGGLE_HEIGHT / 2);
        }

        // Show Rates button
        int ratesButtonWidth = 450;
        int ratesButtonHeight = 50;
        int ratesButtonX = centerX - ratesButtonWidth / 2;
        int ratesButtonY = 180;
        boolean hoveringRates = logicalMouseX >= ratesButtonX && logicalMouseX <= ratesButtonX + ratesButtonWidth &&
                               logicalMouseY >= ratesButtonY && logicalMouseY <= ratesButtonY + ratesButtonHeight;

        if (ui != null) {
            ui.drawButtonFade(ratesButtonX, ratesButtonY, ratesButtonWidth, ratesButtonHeight, 12, hoveringRates);
        }

        String ratesText = showRates ? "§a§lShowing: Average/Run" : "§e§lShowing: Totals";
        drawCenteredText(context, ratesText, ratesButtonX + ratesButtonWidth / 2, ratesButtonY + ratesButtonHeight / 2);

        // Get and display loot data
        renderLootStats(context, centerX, logicalW);

        // Draw tooltips for amplifiers and bags when in rate mode
        if (showRates && currentStats != null && currentTotalRuns > 0) {
            renderHoverTooltips(context, logicalMouseX, logicalMouseY, centerX);
        }
    }

    private void renderHoverTooltips(DrawContext context, int logicalMouseX, int logicalMouseY, int centerX) {
        int tooltipWidth = 420;
        int tooltipHeight = 140;
        int lineHitHeight = lineHeight + 8;
        int padding = 15;
        int textLineHeight = 30;

        // Check amplifiers line hover
        if (logicalMouseY >= amplifiersLineY - 15 && logicalMouseY <= amplifiersLineY + lineHitHeight - 15 &&
            logicalMouseX >= centerX - 300 && logicalMouseX <= centerX + 300) {
            // Draw amplifier breakdown tooltip
            int tooltipX = logicalMouseX + 20;
            int tooltipY = logicalMouseY + 20;

            // Background and border (draw border on top of background)
            drawRect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 0xEE000000);
            drawRect(tooltipX, tooltipY, tooltipWidth, 3, 0xFF4e392d);  // top
            drawRect(tooltipX, tooltipY + tooltipHeight - 3, tooltipWidth, 3, 0xFF4e392d);  // bottom
            drawRect(tooltipX, tooltipY, 3, tooltipHeight, 0xFF4e392d);  // left
            drawRect(tooltipX + tooltipWidth - 3, tooltipY, 3, tooltipHeight, 0xFF4e392d);  // right

            drawLeftText(context, "§e§lAmplifiers/Run Breakdown:", tooltipX + padding, tooltipY + padding);
            drawLeftText(context, "§6Tier III: §f" + String.format("%.3f", (double) currentStats.amplifierTier3 / currentTotalRuns) + "/run", tooltipX + padding, tooltipY + padding + textLineHeight);
            drawLeftText(context, "§eTier II: §f" + String.format("%.3f", (double) currentStats.amplifierTier2 / currentTotalRuns) + "/run", tooltipX + padding, tooltipY + padding + textLineHeight * 2);
            drawLeftText(context, "§fTier I: §f" + String.format("%.3f", (double) currentStats.amplifierTier1 / currentTotalRuns) + "/run", tooltipX + padding, tooltipY + padding + textLineHeight * 3);
        }

        // Check bags line hover
        if (logicalMouseY >= bagsLineY - 15 && logicalMouseY <= bagsLineY + lineHitHeight - 15 &&
            logicalMouseX >= centerX - 300 && logicalMouseX <= centerX + 300) {
            // Draw bags breakdown tooltip
            int tooltipX = logicalMouseX + 20;
            int tooltipY = logicalMouseY + 20;

            // Background and border (draw border on top of background)
            drawRect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 0xEE000000);
            drawRect(tooltipX, tooltipY, tooltipWidth, 3, 0xFF4e392d);  // top
            drawRect(tooltipX, tooltipY + tooltipHeight - 3, tooltipWidth, 3, 0xFF4e392d);  // bottom
            drawRect(tooltipX, tooltipY, 3, tooltipHeight, 0xFF4e392d);  // left
            drawRect(tooltipX + tooltipWidth - 3, tooltipY, 3, tooltipHeight, 0xFF4e392d);  // right

            drawLeftText(context, "§b§lBags/Run Breakdown:", tooltipX + padding, tooltipY + padding);
            drawLeftText(context, "§6Stuffed: §f" + String.format("%.3f", (double) currentStats.stuffedBags / currentTotalRuns) + "/run", tooltipX + padding, tooltipY + padding + textLineHeight);
            drawLeftText(context, "§ePacked: §f" + String.format("%.3f", (double) currentStats.packedBags / currentTotalRuns) + "/run", tooltipX + padding, tooltipY + padding + textLineHeight * 2);
            drawLeftText(context, "§aVaried: §f" + String.format("%.3f", (double) currentStats.variedBags / currentTotalRuns) + "/run", tooltipX + padding, tooltipY + padding + textLineHeight * 3);
        }
    }

    private void renderLootStats(DrawContext context, int centerX, int logicalW) {
        RaidLootData lootData = RaidLootConfig.INSTANCE.data;

        // Calculate combined stats
        RaidLootData.RaidSpecificLoot combinedStats = new RaidLootData.RaidSpecificLoot();

        for (int i = 0; i < 4; i++) {
            if (raidToggles[i]) {
                RaidLootData.RaidSpecificLoot raidStats = lootData.perRaidData.get(RAID_CODES[i]);
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
                    combinedStats.totalAspects += raidStats.totalAspects;
                    combinedStats.mythicAspects += raidStats.mythicAspects;
                    combinedStats.fabledAspects += raidStats.fabledAspects;
                    combinedStats.legendaryAspects += raidStats.legendaryAspects;
                }
            }
        }

        int totalRuns = combinedStats.completionCount;
        int startY = 260;
        int lineHeight = 35;

        drawCenteredText(context, "§6§lSTATISTICS", centerX, startY);
        startY += 50;

        // Calculate emeralds
        long totalEmeralds = (combinedStats.liquidEmeralds * 64L * 64L) + (combinedStats.emeraldBlocks * 64L);
        long stacks = totalEmeralds / 262144;
        long remainingAfterStx = totalEmeralds % 262144;
        long le = remainingAfterStx / 4096;
        long remainingAfterLE = remainingAfterStx % 4096;
        long eb = remainingAfterLE / 64;

        // Save stats for tooltip rendering
        currentStats = combinedStats;
        currentTotalRuns = totalRuns;

        if (showRates && totalRuns > 0) {
            renderRateStats(context, centerX, startY, lineHeight, combinedStats, totalRuns, stacks, le);
        } else {
            renderTotalStats(context, centerX, startY, lineHeight, combinedStats, totalRuns, stacks, le, eb);
        }

        // Per-raid breakdown
        startY = showRates ? 580 : 560;
        if (raidToggles[0] || raidToggles[1] || raidToggles[2] || raidToggles[3]) {
            drawCenteredText(context, "§e§lPER-RAID BREAKDOWN", centerX, startY);
            startY += 40;

            for (int i = 0; i < 4; i++) {
                if (!raidToggles[i]) continue;

                RaidLootData.RaidSpecificLoot raidStats = lootData.perRaidData.get(RAID_CODES[i]);
                if (raidStats == null) {
                    drawCenteredText(context, RAID_COLORS[i] + "§l" + RAID_NAMES[i] + ": §7No data", centerX, startY);
                    startY += 30;
                    continue;
                }

                int runs = raidStats.completionCount;
                long raidTotalEmeralds = (raidStats.liquidEmeralds * 64L * 64L) + (raidStats.emeraldBlocks * 64L);
                long raidStacks = raidTotalEmeralds / 262144;
                long raidRemaining = raidTotalEmeralds % 262144;
                long raidLe = raidRemaining / 4096;
                long raidRemaining2 = raidRemaining % 4096;
                long raidEb = raidRemaining2 / 64;

                if (showRates && runs > 0) {
                    long totalLeValue = (raidStacks * 64) + raidLe;
                    double avgLe = (double) totalLeValue / runs;
                    String emeraldText = avgLe >= 64 ? String.format("%.2fstx/run", avgLe / 64) : String.format("%.1fle/run", avgLe);
                    drawCenteredText(context, RAID_COLORS[i] + "§l" + RAID_NAMES[i] + ": §f" + runs + " runs §8| §a" + emeraldText, centerX, startY);
                } else {
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
                    drawCenteredText(context, RAID_COLORS[i] + "§l" + RAID_NAMES[i] + ": §f" + runs + " runs §8| §a" + emeraldText + " total", centerX, startY);
                }
                startY += 30;
            }
        }
    }

    private void renderRateStats(DrawContext context, int centerX, int startY, int lineHeight,
                                  RaidLootData.RaidSpecificLoot stats, int totalRuns, long stacks, long le) {
        drawCenteredText(context, "§6§lTotal Runs: §f" + totalRuns, centerX, startY);
        startY += lineHeight;

        long totalLeValue = (stacks * 64) + le;
        double avgLe = (double) totalLeValue / totalRuns;
        String emeraldAvgText = avgLe >= 64 ? String.format("%.2fstx", avgLe / 64) : String.format("%.1fle", avgLe);
        drawCenteredText(context, "§a§lEmeralds/Run: §f" + emeraldAvgText, centerX, startY);
        startY += lineHeight;

        // Track amplifiers line Y for tooltip
        amplifiersLineY = startY;
        drawCenteredText(context, "§e§lAmplifiers/Run: §f" + String.format("%.2f", (double)stats.getTotalAmplifiers() / totalRuns) + " §7(hover for breakdown)", centerX, startY);
        startY += lineHeight + 8;

        // Track bags line Y for tooltip
        bagsLineY = startY;
        drawCenteredText(context, "§b§lBags/Run: §f" + String.format("%.2f", (double)stats.totalBags / totalRuns) + " §7(hover for breakdown)", centerX, startY);
        startY += lineHeight + 8;

        drawCenteredText(context, "§d§lTomes/Run: §f" + String.format("%.2f", (double)stats.totalTomes / totalRuns) + " §7(§5" + String.format("%.2f", (double)stats.mythicTomes / totalRuns) + " §7mythic§7)", centerX, startY);
        startY += lineHeight + 8;

        drawCenteredText(context, "§5§lAspects/Run: §f" + String.format("%.2f", (double)stats.totalAspects / totalRuns) + " §7(§5" + String.format("%.2f", (double)stats.mythicAspects / totalRuns) + " §7mythic§7)", centerX, startY);
    }

    private void renderTotalStats(DrawContext context, int centerX, int startY, int lineHeight,
                                   RaidLootData.RaidSpecificLoot stats, int totalRuns, long stacks, long le, long eb) {
        drawCenteredText(context, "§6§lTotal Runs: §f" + totalRuns, centerX, startY);
        startY += lineHeight;

        StringBuilder emeraldText = new StringBuilder();
        if (stacks > 0) {
            emeraldText.append(stacks).append("stx");
            if (le > 0) emeraldText.append(" + ").append(le).append("le");
        } else if (le > 0) {
            emeraldText.append(le).append("le");
        } else {
            emeraldText.append(eb).append("eb");
        }
        drawCenteredText(context, "§a§lEmeralds: §f" + emeraldText.toString(), centerX, startY);
        startY += lineHeight;

        drawCenteredText(context, "§e§lAmplifiers: §f" + stats.getTotalAmplifiers() + " §7(I: " + stats.amplifierTier1 + " | II: " + stats.amplifierTier2 + " | III: " + stats.amplifierTier3 + ")", centerX, startY);
        startY += lineHeight + 8;

        drawCenteredText(context, "§b§lBags: §f" + stats.totalBags + " §7(Stuffed: " + stats.stuffedBags + " | Packed: " + stats.packedBags + " | Varied: " + stats.variedBags + ")", centerX, startY);
        startY += lineHeight + 8;

        drawCenteredText(context, "§d§lTomes: §f" + stats.totalTomes + " §7(§5" + stats.mythicTomes + " §7mythic, §c" + stats.fabledTomes + " §7fabled§7)", centerX, startY);
        startY += lineHeight + 8;

        drawCenteredText(context, "§5§lAspects: §f" + stats.totalAspects + " §7(§5" + stats.mythicAspects + " §7mythic, §c" + stats.fabledAspects + " §7fabled, §b" + stats.legendaryAspects + " §7legendary§7)", centerX, startY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int logicalW = getLogicalWidth();
        int centerX = logicalW / 2;
        int logicalMouseX = toLogicalX(mouseX);
        int logicalMouseY = toLogicalY(mouseY);

        // Check raid toggle clicks
        int totalToggleWidth = (TOGGLE_WIDTH * 4) + (TOGGLE_SPACING * 3);
        int toggleStartX = (logicalW - totalToggleWidth) / 2;

        for (int i = 0; i < 4; i++) {
            int x = toggleStartX + (i * (TOGGLE_WIDTH + TOGGLE_SPACING));
            if (logicalMouseX >= x && logicalMouseX <= x + TOGGLE_WIDTH &&
                logicalMouseY >= TOGGLE_Y && logicalMouseY <= TOGGLE_Y + TOGGLE_HEIGHT) {
                raidToggles[i] = !raidToggles[i];
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        // Check show rates button
        int ratesButtonWidth = 450;
        int ratesButtonHeight = 50;
        int ratesButtonX = centerX - ratesButtonWidth / 2;
        int ratesButtonY = 180;

        if (logicalMouseX >= ratesButtonX && logicalMouseX <= ratesButtonX + ratesButtonWidth &&
            logicalMouseY >= ratesButtonY && logicalMouseY <= ratesButtonY + ratesButtonHeight) {
            showRates = !showRates;
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }

        return false;
    }
}
