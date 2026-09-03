package julianh06.wynnextras.features.aspects.pages;

import julianh06.wynnextras.features.aspects.AspectScreen;
import julianh06.wynnextras.features.raid.RaidLootConfig;
import julianh06.wynnextras.features.raid.RaidLootData;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class RaidLootPage extends PageWidget {
    private enum Raid { NOTG, NOL, TCC, TNA, TWP }

    private final List<RaidToggleWidget> raidToggleWidgets = new ArrayList<>();
    private final ShowTotalWidget showTotalWidget;

    private final AmplifiersPerRunWidget amplifiersPerRunWidget;
    private final BagsPerRunWidget bagsPerRunWidget;
    private final PowderStatsWidget powderStatsWidget;

    private static List<Text> amplifiersPerRunTooltip;
    private static List<Text> bagsPerRunTooltip;
    private static List<Text> hoveredTooltip = new ArrayList<>();

    private static boolean showRates = false;

    private static final int TOGGLE_WIDTH = 220;
    private static final int TOGGLE_HEIGHT = 50;
    private static final int TOGGLE_SPACING = 20;
    private static final int TOGGLE_Y = 120;

    private static final String[] RAID_NAMES = {"NOTG", "NOL", "TCC", "TNA", "TWP"};
    private static final String[] RAID_COLORS = {"§2", "§e", "§3", "§5", "§4"};
    private static final String[] RAID_CODES = {"NOTG", "NOL", "TCC", "TNA", "TWP"};
    private static final String[] POWDER_TIER_NAMES = {"I", "II", "III", "IV", "V", "VI", "VII"};

    // For hover tooltips
    private int amplifiersLineY = 0;
    private String amplifiersText = "";
    private int bagsLineY = 0;
    private String bagsText = "";
    private RaidLootData.RaidSpecificLoot currentStats = null;
    private int currentTotalRuns = 0;

    public RaidLootPage(AspectScreen parent) {
        super(parent);

        for(Raid raid : Raid.values()) {
            raidToggleWidgets.add(new RaidToggleWidget(raid));
        }

        showTotalWidget = new ShowTotalWidget();
        amplifiersPerRunWidget = new AmplifiersPerRunWidget();
        bagsPerRunWidget = new BagsPerRunWidget();
        powderStatsWidget = new PowderStatsWidget();
    }

    @Override
    public void drawContent(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        hoveredTooltip.clear();

        int logicalW = (int) (width * ui.getScaleFactorF());
        int centerX = logicalW / 2;

        ui.drawCenteredText("§6§lRAID LOOT TRACKER", centerX, 60);

        int totalToggleWidth = (TOGGLE_WIDTH * Raid.values().length) + (TOGGLE_SPACING * (Raid.values().length - 1));
        int toggleStartX = (logicalW - totalToggleWidth) / 2;

        for(RaidToggleWidget raidToggleWidget : raidToggleWidgets) {
            raidToggleWidget.setBounds(toggleStartX, TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT);
            raidToggleWidget.draw(context, mouseX, mouseY, tickDelta, ui);
            toggleStartX += TOGGLE_WIDTH + TOGGLE_SPACING;
        }

        int ratesButtonWidth = 450;
        int ratesButtonHeight = 50;
        int ratesButtonX = centerX - ratesButtonWidth / 2;
        int ratesButtonY = 180;

        showTotalWidget.setBounds(ratesButtonX, ratesButtonY, ratesButtonWidth, ratesButtonHeight);
        showTotalWidget.draw(context, mouseX, mouseY, tickDelta, ui);

        if(showRates) {
            int amplifierWidth = MinecraftClient.getInstance().textRenderer.getWidth(amplifiersText) * 3;
            amplifiersPerRunWidget.setBounds(Math.round(centerX - amplifierWidth / 2f), amplifiersLineY - 20, amplifierWidth, 40);
            amplifiersPerRunWidget.draw(context, mouseX, mouseY, tickDelta, ui);

            int bagWidth = MinecraftClient.getInstance().textRenderer.getWidth(bagsText) * 3;
            bagsPerRunWidget.setBounds(Math.round(centerX - bagWidth / 2f), bagsLineY - 20, bagWidth, 40);
            bagsPerRunWidget.draw(context, mouseX, mouseY, tickDelta, ui);
        }

        renderLootStats(context, centerX, logicalW);
        powderStatsWidget.draw(context, mouseX, mouseY, tickDelta, ui);

        amplifiersPerRunTooltip = List.of(
                Text.of("§e§lAmplifiers/Run Breakdown:"),
                Text.of("§6Tier IV: §f" + String.format("%.3f", (double) currentStats.amplifierTier4 / currentTotalRuns) + "/run"),
                Text.of("§6Tier III: §f" + String.format("%.3f", (double) currentStats.amplifierTier3 / currentTotalRuns) + "/run"),
                Text.of("§eTier II: §f" + String.format("%.3f", (double) currentStats.amplifierTier2 / currentTotalRuns) + "/run"),
                Text.of("§fTier I: §f" + String.format("%.3f", (double) currentStats.amplifierTier1 / currentTotalRuns) + "/run")
        );

        bagsPerRunTooltip = List.of(
                Text.of("§b§lBags/Run Breakdown:"),
                Text.of("§6Stuffed: §f" + String.format("%.3f", (double) currentStats.stuffedBags / currentTotalRuns) + "/run"),
                Text.of("§ePacked: §f" + String.format("%.3f", (double) currentStats.packedBags / currentTotalRuns) + "/run"),
                Text.of("§aVaried: §f" + String.format("%.3f", (double) currentStats.variedBags / currentTotalRuns) + "/run")
        );
    }

    @Override
    public void drawForeground(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        if(hoveredTooltip.isEmpty()) return;
        TextInputWidget.drawFittingTooltip(context, hoveredTooltip, mouseX, mouseY);
    }

    private void renderLootStats(DrawContext context, int centerX, int logicalW) {
        RaidLootData lootData = RaidLootConfig.INSTANCE.data;

        // Calculate combined stats
        RaidLootData.RaidSpecificLoot combinedStats = new RaidLootData.RaidSpecificLoot();

        for (int i = 0; i < RAID_CODES.length; i++) {
            if (raidToggleWidgets.get(i).toggled) {
                RaidLootData.RaidSpecificLoot raidStats = lootData.perRaidData.get(RAID_CODES[i]);
                if (raidStats != null) {
                    combinedStats.emeraldBlocks += raidStats.emeraldBlocks;
                    combinedStats.liquidEmeralds += raidStats.liquidEmeralds;
                    combinedStats.amplifierTier1 += raidStats.amplifierTier1;
                    combinedStats.amplifierTier2 += raidStats.amplifierTier2;
                    combinedStats.amplifierTier3 += raidStats.amplifierTier3;
                    combinedStats.amplifierTier4 += raidStats.amplifierTier4;
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
                    for (int element = 0; element < RaidLootData.POWDER_ELEMENT_COUNT; element++) {
                        for (int tier = 0; tier < RaidLootData.POWDER_TIER_COUNT; tier++) {
                            combinedStats.addPowder(element, tier, raidStats.getPowderCount(element, tier));
                        }
                    }
                    combinedStats.totalWards += raidStats.totalWards;
                }
            }
        }

        int totalRuns = combinedStats.completionCount;
        int startY = 260;
        int lineHeight = 35;

        ui.drawCenteredText("§6§lSTATISTICS", centerX, startY);
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
        startY = 620;
        if (raidToggleWidgets.stream().anyMatch(widget -> widget.toggled)) {
            ui.drawCenteredText("§e§lPER-RAID BREAKDOWN", centerX, startY);
            startY += 40;

            for (int i = 0; i < RAID_CODES.length; i++) {
                if (!raidToggleWidgets.get(i).toggled) continue;

                RaidLootData.RaidSpecificLoot raidStats = lootData.perRaidData.get(RAID_CODES[i]);
                if (raidStats == null) {
                    ui.drawCenteredText(RAID_COLORS[i] + "§l" + RAID_NAMES[i] + ": §7No data", centerX, startY);
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
                    ui.drawCenteredText(RAID_COLORS[i] + "§l" + RAID_NAMES[i] + ": §f" + runs + " runs §8| §a" + emeraldText, centerX, startY);
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
                    ui.drawCenteredText(RAID_COLORS[i] + "§l" + RAID_NAMES[i] + ": §f" + runs + " runs §8| §a" + emeraldText + " total", centerX, startY);
                }
                startY += 30;
            }
        }
    }

    private void renderRateStats(DrawContext context, int centerX, int startY, int lineHeight,
                                 RaidLootData.RaidSpecificLoot stats, int totalRuns, long stacks, long le) {
        ui.drawCenteredText("§6§lTotal Runs: §f" + totalRuns, centerX, startY);
        startY += lineHeight;

        long totalLeValue = (stacks * 64) + le;
        double avgLe = (double) totalLeValue / totalRuns;
        String emeraldAvgText = avgLe >= 64 ? String.format("%.2fstx", avgLe / 64) : String.format("%.1fle", avgLe);
        ui.drawCenteredText("§a§lEmeralds/Run: §f" + emeraldAvgText, centerX, startY);
        startY += lineHeight;

        // Track amplifiers line Y for tooltip
        amplifiersLineY = startY;
        amplifiersText = "§e§lAmplifiers/Run: §f" + String.format("%.2f", (double)stats.getTotalAmplifiers() / totalRuns) + " §7(hover for breakdown)";
        ui.drawCenteredText(amplifiersText, centerX, startY);
        startY += lineHeight + 8;

        // Track bags line Y for tooltip
        bagsLineY = startY;
        bagsText = "§b§lBags/Run: §f" + String.format("%.2f", (double)stats.totalBags / totalRuns) + " §7(hover for breakdown)";
        ui.drawCenteredText(bagsText, centerX, startY);
        startY += lineHeight + 8;

        ui.drawCenteredText("§d§lTomes/Run: §f" + String.format("%.2f", (double)stats.totalTomes / totalRuns) + " §7(§5" + String.format("%.2f", (double)stats.mythicTomes / totalRuns) + " §7mythic§7)", centerX, startY);
        startY += lineHeight + 8;

        String powdersText = "§6§lPowders/Run: §f" + String.format("%.2f", (double) stats.getTotalPowders() / totalRuns)
                + " §7(hover for breakdown)";
        powderStatsWidget.setContent(powdersText, centerX, startY, createPowderTooltip(stats, totalRuns, true));
        startY += lineHeight;

        ui.drawCenteredText("§c§lWards/Run: §f" + String.format("%.2f", (double) stats.totalWards / totalRuns), centerX, startY);
        startY += lineHeight;

        ui.drawCenteredText("§5§lAspects/Run: §f" + String.format("%.2f", (double)stats.totalAspects / totalRuns) + " §7(§5" + String.format("%.2f", (double)stats.mythicAspects / totalRuns) + " §7mythic§7)", centerX, startY);
    }

    private void renderTotalStats(DrawContext context, int centerX, int startY, int lineHeight,
                                  RaidLootData.RaidSpecificLoot stats, int totalRuns, long stacks, long le, long eb) {
        ui.drawCenteredText("§6§lTotal Runs: §f" + totalRuns, centerX, startY);
        startY += lineHeight;

        StringBuilder emeraldText = new StringBuilder();
        if (stacks > 0) {
            emeraldText.append(stacks).append("stx");
            if (le > 0) emeraldText.append(" ").append(le).append("le");
        } else if (le > 0) {
            emeraldText.append(le).append("le");
        } else {
            emeraldText.append(eb).append("eb");
        }
        ui.drawCenteredText("§a§lEmeralds: §f" + emeraldText.toString(), centerX, startY);
        startY += lineHeight;

        ui.drawCenteredText("§e§lAmplifiers: §f" + stats.getTotalAmplifiers() + " §7(I: " + stats.amplifierTier1 + " | II: " + stats.amplifierTier2 + " | III: " + stats.amplifierTier3 + " | IV: " + stats.amplifierTier4 + ")", centerX, startY);
        startY += lineHeight + 8;

        ui.drawCenteredText("§b§lBags: §f" + stats.totalBags + " §7(Stuffed: " + stats.stuffedBags + " | Packed: " + stats.packedBags + " | Varied: " + stats.variedBags + ")", centerX, startY);
        startY += lineHeight + 8;

        ui.drawCenteredText("§d§lTomes: §f" + stats.totalTomes + " §7(§5" + stats.mythicTomes + " §7mythic, §c" + stats.fabledTomes + " §7fabled§7)", centerX, startY);
        startY += lineHeight + 8;

        String powdersText = "§6§lPowders: §f" + stats.getTotalPowders() + " §7(hover for breakdown)";
        powderStatsWidget.setContent(powdersText, centerX, startY, createPowderTooltip(stats, totalRuns, false));
        startY += lineHeight;

        ui.drawCenteredText("§c§lWards: §f" + stats.totalWards, centerX, startY);
        startY += lineHeight;

        ui.drawCenteredText("§5§lAspects: §f" + stats.totalAspects + " §7(§5" + stats.mythicAspects + " §7mythic, §c" + stats.fabledAspects + " §7fabled, §b" + stats.legendaryAspects + " §7legendary§7)", centerX, startY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for(RaidToggleWidget raidToggleWidget : raidToggleWidgets) {
            if(raidToggleWidget.mouseClicked(mouseX, mouseY, button)) return true;
        }

        if(showTotalWidget.mouseClicked(mouseX, mouseY, button)) return true;

        return false;
    }

    private static class RaidToggleWidget extends Widget {
        final Raid raid;
        private boolean toggled = true;

        public RaidToggleWidget(Raid raid) {
            this.raid = raid;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, hovered);

            String textColor = "";
            if(!toggled) textColor = "§7";
            else textColor = RAID_COLORS[raid.ordinal()];

            ui.drawCenteredText(textColor + raid.name(), x + width / 2f, y + height / 2f);
        }

        @Override
        protected boolean onClick(int button) {
            toggled = !toggled;
            return true;
        }
    }

    private static class ShowTotalWidget extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, hovered);

            ui.drawCenteredText(showRates ? "§a§lShowing: Average/Run" : "§e§lShowing: Totals", x + width / 2f, y + height / 2f);
        }

        @Override
        protected boolean onClick(int button) {
            showRates = !showRates;
            return true;
        }
    }

    private static class AmplifiersPerRunWidget extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(hovered) hoveredTooltip = new ArrayList<>(amplifiersPerRunTooltip);
        }
    }

    private static class BagsPerRunWidget extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(hovered) hoveredTooltip = new ArrayList<>(bagsPerRunTooltip);
        }
    }

    private static List<Text> createPowderTooltip(RaidLootData.RaidSpecificLoot stats, int totalRuns, boolean showRates) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.of("§6§lPowder Breakdown:"));
        for (int tier = 0; tier < POWDER_TIER_NAMES.length; tier++) {
            String amount = showRates
                    ? String.format("%.3f/run", (double) stats.getPowderTierTotal(tier) / totalRuns)
                    : String.valueOf(stats.getPowderTierTotal(tier));
            tooltip.add(Text.of("§6Tier " + POWDER_TIER_NAMES[tier] + ": §f" + amount));
        }
        return tooltip;
    }

    private static class PowderStatsWidget extends Widget {
        private String text = "";
        private int logicalCenterX;
        private int logicalY;
        private List<Text> tooltip = List.of();

        private void setContent(String text, int centerX, int y, List<Text> tooltip) {
            this.text = text;
            this.logicalCenterX = centerX;
            this.logicalY = y;
            this.tooltip = tooltip;
            int logicalWidth = MinecraftClient.getInstance().textRenderer.getWidth(text) * 3;
            setBounds(Math.round(centerX - logicalWidth / 2f), y - 14, logicalWidth, 28);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawCenteredText(text, logicalCenterX, logicalY);
            if(hovered) hoveredTooltip = new ArrayList<>(tooltip);
        }
    }
}