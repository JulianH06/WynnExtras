package julianh06.wynnextras.features.aspects.oldPages;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.features.aspects.GambitData;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.ApiAspect;
import julianh06.wynnextras.features.profileviewer.data.Aspect;
import julianh06.wynnextras.features.profileviewer.data.User;
import julianh06.wynnextras.features.raid.RaidListData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gambits page showing today's gambits and misc stats
 */
public class GambitsPage extends AspectPage {
    private List<GambitData.GambitEntry> crowdsourcedGambits = null;
    private boolean fetchedCrowdsourcedGambits = false;

    // Reference to player data for stats (set by host)
    private User playerData = null;

    public GambitsPage(AspectScreenHost host) {
        super(host);
    }

    @Override
    public String getTitle() {
        return "Gambits";
    }

    public void setPlayerData(User data) {
        this.playerData = data;
    }

    @Override
    public void onActivate() {
        if (!fetchedCrowdsourcedGambits) {
            fetchedCrowdsourcedGambits = true;
            WynncraftApiHandler.fetchCrowdsourcedGambits().thenAccept(result -> {
                crowdsourcedGambits = result;
                if (result != null && !result.isEmpty()) {
                    GambitData.INSTANCE.saveGambits(result);
                }
            });
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        int logicalW = getLogicalWidth();
        int logicalH = getLogicalHeight();
        int centerX = logicalW / 2;

        // Title
        drawCenteredText(context, "§6§lTODAY'S GAMBITS", centerX, 60);

        // Countdown
        String countdown = getGambitCountdown();
        drawCenteredText(context, "§7" + countdown, centerX, 110);

        // Trigger fetch if needed
        if (!fetchedCrowdsourcedGambits) {
            fetchedCrowdsourcedGambits = true;
            WynncraftApiHandler.fetchCrowdsourcedGambits().thenAccept(result -> {
                crowdsourcedGambits = result;
                if (result != null && !result.isEmpty()) {
                    GambitData.INSTANCE.saveGambits(result);
                }
            });
        }

        int startY = 180;
        int panelWidth = Math.min(800, (logicalW - 200) / 2);
        int panelHeight = 180;
        int spacingH = 50;
        int spacingV = 40;

        // Determine data source
        List<GambitData.GambitEntry> gambitsToShow;
        String dataSource;

        if (crowdsourcedGambits != null && !crowdsourcedGambits.isEmpty()) {
            gambitsToShow = crowdsourcedGambits;
            dataSource = "§a(Crowdsourced)";
        } else {
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
            if (crowdsourcedGambits == null) {
                drawCenteredText(context, "§7Loading crowdsourced data...", centerX, startY + 150);
            } else {
                drawCenteredText(context, "§7No gambit data available yet", centerX, startY + 150);
                drawCenteredText(context, "§e§nClick to open Party Finder /pf to scan", centerX, startY + 200);
            }
        } else {
            drawCenteredText(context, dataSource, centerX, 140);

            int totalWidth = (panelWidth * 2) + spacingH;
            int startX = (logicalW - totalWidth) / 2;

            for (int i = 0; i < Math.min(gambitsToShow.size(), 4); i++) {
                GambitData.GambitEntry gambit = gambitsToShow.get(i);
                int col = i % 2;
                int row = i / 2;
                int x = startX + (col * (panelWidth + spacingH));
                int y = startY + (row * (panelHeight + spacingV));
                drawGambitPanel(context, x, y, panelWidth, panelHeight, gambit);
            }
        }

        // Misc Stats
        int statsStartY = startY + (2 * (panelHeight + spacingV)) + 80;
        drawMiscStats(context, centerX, statsStartY);
    }

    private void drawGambitPanel(DrawContext context, int x, int y, int panelWidth, int panelHeight, GambitData.GambitEntry gambit) {
        drawRect(x, y, panelWidth, panelHeight, 0xAA000000);

        // Border
        drawRect(x, y, panelWidth, 5, 0xFF4e392d);
        drawRect(x, y + panelHeight - 5, panelWidth, 5, 0xFF4e392d);
        drawRect(x, y, 5, panelHeight, 0xFF4e392d);
        drawRect(x + panelWidth - 5, y, 5, panelHeight, 0xFF4e392d);

        // Name
        String truncatedName = gambit.name;
        int maxNameChars = (panelWidth - 60) / 12;
        if (truncatedName.length() > maxNameChars) {
            truncatedName = truncatedName.substring(0, Math.max(3, maxNameChars - 3)) + "...";
        }
        drawLeftText(context, "§6§l" + truncatedName, x + 30, y + 25);

        // Description
        String desc = gambit.description;
        int charsPerLine = (panelWidth - 80) / 20;
        List<String> lines = wrapTextByChars(desc, charsPerLine);

        int textY = y + 70;
        int lineSpacing = 30;
        int maxLines = (panelHeight - 90) / lineSpacing;
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
            if (word.length() > maxChars) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
                lines.add(word.substring(0, Math.max(1, maxChars - 3)) + "...");
                continue;
            }

            int neededSpace = currentLine.length() > 0 ? word.length() + 1 : word.length();
            if (currentLine.length() + neededSpace <= maxChars) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
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

        int totalRuns = RaidListData.INSTANCE.getRaids().size();
        int completedRuns = (int) RaidListData.INSTANCE.getRaids().stream().filter(r -> r.completed).count();
        int failedRuns = totalRuns - completedRuns;

        long notgRuns = RaidListData.INSTANCE.getRaids().stream()
            .filter(r -> r.raidInfo.toString().contains("Orphion")).count();
        long nolRuns = RaidListData.INSTANCE.getRaids().stream()
            .filter(r -> r.raidInfo.toString().contains("Lair")).count();
        long tccRuns = RaidListData.INSTANCE.getRaids().stream()
            .filter(r -> r.raidInfo.toString().contains("Canyon")).count();
        long tnaRuns = RaidListData.INSTANCE.getRaids().stream()
            .filter(r -> r.raidInfo.toString().contains("Nameless")).count();

        int totalAspectsPulled = 0;
        int mythicAspectsPulled = 0;

        if (playerData != null && playerData.getAspects() != null) {
            List<ApiAspect> allAspects = WynncraftApiHandler.fetchAllAspects();
            for (Aspect playerAspect : playerData.getAspects()) {
                totalAspectsPulled += playerAspect.getAmount();
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

        int lineHeight = 30;
        int sectionGap = 40;
        int yOffset = startY + sectionGap;

        drawCenteredText(context, "§e§lTotal Runs: §f" + totalRuns, centerX, yOffset);
        yOffset += lineHeight;
        drawCenteredText(context, "§a§lCompleted: §f" + completedRuns + " §8| §c§lFailed: §f" + failedRuns, centerX, yOffset);
        yOffset += sectionGap;
        drawCenteredText(context, "§6§lTotal Aspects Pulled: §f" + totalAspectsPulled, centerX, yOffset);
        yOffset += lineHeight;
        drawCenteredText(context, "§5§lMythic Aspects Pulled: §f" + mythicAspectsPulled, centerX, yOffset);
        yOffset += sectionGap;
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int logicalW = getLogicalWidth();
        int centerX = logicalW / 2;
        int logicalMouseX = toLogicalX(mouseX);
        int logicalMouseY = toLogicalY(mouseY);

        // Check "Click to open Party Finder" click
        if (crowdsourcedGambits != null && crowdsourcedGambits.isEmpty()) {
            GambitData data = GambitData.INSTANCE;
            if (!data.hasToday() || data.gambits.isEmpty()) {
                int pfTextY = 180 + 200;
                if (logicalMouseY >= pfTextY - 20 && logicalMouseY <= pfTextY + 30 &&
                    logicalMouseX >= centerX - 400 && logicalMouseX <= centerX + 400) {
                    if (getClient() != null && getClient().player != null) {
                        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                        getClient().setScreen(null);
                        getClient().player.networkHandler.sendChatCommand("pf");
                    }
                    return true;
                }
            }
        }

        return false;
    }
}
