package julianh06.wynnextras.features.aspects.pages;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.LeaderboardEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

import java.util.List;

/**
 * Leaderboard page showing top 15 players with most maxed aspects
 */
public class LeaderboardPage extends AspectPage {
    private List<LeaderboardEntry> leaderboardList = null;
    private boolean fetchedLeaderboard = false;

    // Layout constants
    private static final int ENTRY_WIDTH = 800;
    private static final int ENTRY_HEIGHT = 50;
    private static final int SPACING = 8;
    private static final int START_Y = 180;

    public LeaderboardPage(AspectScreenHost host) {
        super(host);
    }

    @Override
    public String getTitle() {
        return "Leaderboard";
    }

    @Override
    public void onActivate() {
        // Fetch leaderboard if not already fetched
        if (!fetchedLeaderboard) {
            fetchedLeaderboard = true;
            WynncraftApiHandler.fetchLeaderboard(15).thenAccept(result -> {
                leaderboardList = result;
            });
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        int logicalW = getLogicalWidth();
        int logicalH = getLogicalHeight();
        int centerX = logicalW / 2;

        // Title
        drawCenteredText(context, "§6§lLEADERBOARD", centerX, 60);
        drawCenteredText(context, "§7Top 15 players with the most maxed aspects", centerX, 110);

        // Trigger fetch if needed
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
        int startX = centerX - ENTRY_WIDTH / 2;

        // Convert mouse to logical
        int logicalMouseX = toLogicalX(mouseX);
        int logicalMouseY = toLogicalY(mouseY);

        for (int i = 0; i < leaderboardList.size(); i++) {
            LeaderboardEntry entry = leaderboardList.get(i);
            int y = START_Y + (i * (ENTRY_HEIGHT + SPACING));

            // Check if hovering
            boolean hovering = logicalMouseX >= startX && logicalMouseX <= startX + ENTRY_WIDTH &&
                              logicalMouseY >= y && logicalMouseY <= y + ENTRY_HEIGHT;

            // Background box (darker when hovering)
            int bgColor = hovering ? 0xCC1a1a1a : 0xAA000000;
            drawRect(startX, y, ENTRY_WIDTH, ENTRY_HEIGHT, bgColor);

            // Border (golden for top 3, normal otherwise)
            int borderColor = getBorderColor(i, hovering);

            drawRect(startX, y, ENTRY_WIDTH, 3, borderColor); // top
            drawRect(startX, y + ENTRY_HEIGHT - 3, ENTRY_WIDTH, 3, borderColor); // bottom
            drawRect(startX, y, 3, ENTRY_HEIGHT, borderColor); // left
            drawRect(startX + ENTRY_WIDTH - 3, y, 3, ENTRY_HEIGHT, borderColor); // right

            // Rank (left)
            String rankText = getRankText(i);
            drawLeftText(context, rankText, startX + 30, y + ENTRY_HEIGHT / 2 - 5);

            // Player name (center-left)
            drawLeftText(context, "§6" + entry.getPlayerName(), startX + 120, y + ENTRY_HEIGHT / 2 - 5);

            // Max aspect count (right)
            String countText = "§a§l" + entry.getMaxAspectCount() + " §7maxed";
            drawLeftText(context, countText, startX + ENTRY_WIDTH - 200, y + ENTRY_HEIGHT / 2 - 5);
        }

        // Instructions above navigation
        drawCenteredText(context, "§7Click on a player to view their aspects", centerX, logicalH - 165);
    }

    private int getBorderColor(int rank, boolean hovering) {
        if (rank == 0) {
            return 0xFFFFD700; // Gold for 1st
        } else if (rank == 1) {
            return 0xFFC0C0C0; // Silver for 2nd
        } else if (rank == 2) {
            return 0xFFCD7F32; // Bronze for 3rd
        } else if (hovering) {
            return 0xFFFFAA00; // Golden when hovering
        } else {
            return 0xFF4e392d; // Normal
        }
    }

    private String getRankText(int rank) {
        if (rank == 0) return "§6§l#1";
        if (rank == 1) return "§f§l#2";
        if (rank == 2) return "§6§l#3";
        return "§7#" + (rank + 1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || leaderboardList == null) return false;

        int logicalW = getLogicalWidth();
        int centerX = logicalW / 2;
        int startX = centerX - ENTRY_WIDTH / 2;

        int logicalMouseX = toLogicalX(mouseX);
        int logicalMouseY = toLogicalY(mouseY);

        for (int i = 0; i < leaderboardList.size(); i++) {
            LeaderboardEntry entry = leaderboardList.get(i);
            int y = START_Y + (i * (ENTRY_HEIGHT + SPACING));

            if (logicalMouseX >= startX && logicalMouseX <= startX + ENTRY_WIDTH &&
                logicalMouseY >= y && logicalMouseY <= y + ENTRY_HEIGHT) {
                // Click on player - search for their aspects
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                host.searchPlayer(entry.getPlayerName());
                return true;
            }
        }

        return false;
    }
}
