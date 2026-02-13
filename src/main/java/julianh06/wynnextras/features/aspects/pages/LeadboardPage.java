package julianh06.wynnextras.features.aspects.pages;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.features.aspects.AspectScreen;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.LeaderboardEntry;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public class LeadboardPage extends PageWidget {
    private List<LeaderboardEntry> leaderboardList = null;
    private List<LeaderBoardEntryWidget> leaderBoardEntryWidgets = new ArrayList<>();
    private boolean fetchedLeaderboard = false;

    public LeadboardPage(AspectScreen parent) {
        super(parent);
    }

    @Override
    public void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        int logicalW = (int) (width * ui.getScaleFactorF());
        int logicalH = (int) (height * ui.getScaleFactorF());
        int centerX = logicalW / 2;

        ui.drawCenteredText("§6§lLEADERBOARD", centerX, 60);
        ui.drawCenteredText("§7Top 15 players with the most maxed aspects", centerX, 95);

        if (!fetchedLeaderboard) {
            fetchedLeaderboard = true;
            WynncraftApiHandler.fetchLeaderboard(15).thenAccept(result -> {
                leaderboardList = result;
            });
        }

        if (leaderboardList == null) {
            ui.drawCenteredText("§eLoading leaderboard...", centerX, 200);
            return;
        }

        if (leaderboardList.isEmpty()) {
            ui.drawCenteredText("§cNo leaderboard data", centerX, 200);
            return;
        }

        if(leaderBoardEntryWidgets.isEmpty()) {
            for (int i = 0; i < leaderboardList.size(); i++) {
                LeaderboardEntry entry = leaderboardList.get(i);
                leaderBoardEntryWidgets.add(new LeaderBoardEntryWidget(entry, i));
            }
        }

        boolean compact = 58 * 15 > logicalH - 150;

        int entryWidth = 800;
        int entryHeight = compact ? 47 : 50;
        int spacing = compact ? 5 : 10;
        int startY = compact ? 120 : 150;
        int startX = centerX - entryWidth / 2;

        for (LeaderBoardEntryWidget leaderBoardEntryWidget : leaderBoardEntryWidgets) {
            leaderBoardEntryWidget.setBounds(startX, startY, entryWidth, entryHeight);
            leaderBoardEntryWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
            startY += entryHeight + spacing;
        }

        ui.drawCenteredText("§7Click on a player to view their aspects", centerX, logicalH - 95);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for(LeaderBoardEntryWidget leaderBoardEntryWidget : leaderBoardEntryWidgets) {
            if(leaderBoardEntryWidget.mouseClicked(mx, my, button)) return true;
        }

        return false;
    }

    private static class LeaderBoardEntryWidget extends Widget {
        final int i;
        final LeaderboardEntry entry;

        public LeaderBoardEntryWidget(LeaderboardEntry entry, int i) {
            this.entry = entry;
            this.i = i;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            int bgColor = hovered ? 0xCC1a1a1a : 0xAA000000;
            ui.drawRect(x, y, width, height, CustomColor.fromInt(bgColor));

            int borderColor;
            if (i == 0) {
                borderColor = 0xFFFFD700; // Gold for 1st
            } else if (i == 1) {
                borderColor = 0xFFC0C0C0; // Silver for 2nd
            } else if (i == 2) {
                borderColor = 0xFFCD7F32; // Bronze for 3rd
            } else if (hovered) {
                borderColor = 0xFFFFAA00; // Golden when hovering
            } else {
                borderColor = 0xFF4e392d; // Normal
            }

            ui.drawRect(x, y, width, 3, CustomColor.fromInt(borderColor)); // top
            ui.drawRect(x, y + height - 3, width, 3, CustomColor.fromInt(borderColor)); // bottom
            ui.drawRect(x, y, 3, height, CustomColor.fromInt(borderColor)); // left
            ui.drawRect(x + width - 3, y, 3, height, CustomColor.fromInt(borderColor)); // right

            String rankText = "§7#" + (i + 1);
            if (i == 0) rankText = "§6§l#1";
            else if (i == 1) rankText = "§f§l#2";
            else if (i == 2) rankText = "§6§l#3";

            ui.drawText(rankText, x + 30, y + height / 2f - 10);

            ui.drawText("§6" + entry.getPlayerName(), x + 120, y + height / 2f - 10);

            String countText = "§a§l" + entry.getMaxAspectCount() + " §7maxed";
            ui.drawText(countText, x + width - 200, y + height / 2f - 10);
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            AspectsPage.performPlayerSearch(entry.getPlayerName());
            AspectScreen.currentPage = AspectScreen.Page.Aspects;
            return true;
        }
    }
}
