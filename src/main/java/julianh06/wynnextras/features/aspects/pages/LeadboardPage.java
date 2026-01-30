package julianh06.wynnextras.features.aspects.pages;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.features.aspects.AspectScreen;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.LeaderboardEntry;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public class LeadboardPage extends PageWidget {
    private List<LeaderboardEntry> leaderboardList = null;
    private boolean fetchedLeaderboard = false;

    public LeadboardPage(AspectScreen parent) {
        super(parent);
    }

    @Override
    public void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        int logicalW = (int) (width * ui.getScaleFactorF());
        int logicalH = (int) (height * ui.getScaleFactorF());
        int centerX = logicalW / 2;

        // Title
        ui.drawCenteredText("§6§lLEADERBOARD", centerX, 60);
        ui.drawCenteredText("§7Top 15 players with the most maxed aspects", centerX, 110);

        // Fetch leaderboard on first load
        if (!fetchedLeaderboard) {
            fetchedLeaderboard = true;
            WynncraftApiHandler.fetchLeaderboard(15).thenAccept(result -> {
                leaderboardList = result;
            });
        }

        // Show loading or data
        if (leaderboardList == null) {
            ui.drawCenteredText("§eLoading leaderboard...", centerX, 200);
            return;
        }

        if (leaderboardList.isEmpty()) {
            ui.drawCenteredText("§cNo leaderboard data", centerX, 200);
            return;
        }

        // Display leaderboard entries
        int entryWidth = 800;
        int entryHeight = 50;
        int spacing = 8;
        int startY = 180;
        int startX = centerX - entryWidth / 2;

        // Convert mouse to logical
        int logicalMouseX = (int)(mouseX * ui.getScaleFactorF());
        int logicalMouseY = (int)(mouseY * ui.getScaleFactorF());

        for (int i = 0; i < leaderboardList.size(); i++) {
            julianh06.wynnextras.features.profileviewer.data.LeaderboardEntry entry = leaderboardList.get(i);
            int y = startY + (i * (entryHeight + spacing));

            // Check if hovering
            boolean hovering = logicalMouseX >= startX && logicalMouseX <= startX + entryWidth &&
                    logicalMouseY >= y && logicalMouseY <= y + entryHeight;

            // Background box (darker when hovering)
            int bgColor = hovering ? 0xCC1a1a1a : 0xAA000000;
            ui.drawRect(startX, y, entryWidth, entryHeight, CustomColor.fromInt(bgColor));

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

            ui.drawRect(startX, y, entryWidth, 3, CustomColor.fromInt(borderColor)); // top
            ui.drawRect(startX, y + entryHeight - 3, entryWidth, 3, CustomColor.fromInt(borderColor)); // bottom
            ui.drawRect(startX, y, 3, entryHeight, CustomColor.fromInt(borderColor)); // left
            ui.drawRect(startX + entryWidth - 3, y, 3, entryHeight, CustomColor.fromInt(borderColor)); // right

            // Rank (left)
            String rankText = "§7#" + (i + 1);
            if (i == 0) rankText = "§6§l#1";
            else if (i == 1) rankText = "§f§l#2";
            else if (i == 2) rankText = "§6§l#3";

            ui.drawText(rankText, startX + 30, y + entryHeight / 2 - 10);

            // Player name (center-left)
            ui.drawText("§6" + entry.getPlayerName(), startX + 120, y + entryHeight / 2 - 10);

            // Max aspect count (right)
            String countText = "§a§l" + entry.getMaxAspectCount() + " §7maxed";
            ui.drawText(countText, startX + entryWidth - 200, y + entryHeight / 2 - 10);
        }

        // Instructions above navigation
        ui.drawCenteredText("§7Click on a player to view their aspects", centerX, logicalH - 165);
    }
}
