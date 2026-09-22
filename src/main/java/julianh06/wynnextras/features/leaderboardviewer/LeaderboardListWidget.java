package julianh06.wynnextras.features.leaderboardviewer;

import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.colors.CustomColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardListWidget extends Widget {
    private static final int PAGE_SIZE = 10;
    private static final int PAGE_COUNT = 10;
    private static final int HEADER_HEIGHT = 58;
    private static final int SUBTYPE_HEADER_HEIGHT = 102;
    private static final int SEASON_HEADER_HEIGHT = 102;
    private static final int FOOTER_HEIGHT = 54;
    private static final int ROW_HEIGHT = 52;
    private static final int ROW_GAP = 5;

    private final TypeWidget owner;
    private final List<LeaderboardEntryWidget> entryWidgets = new ArrayList<>();
    private final List<LeaderboardPageButtonWidget> pageButtons = new ArrayList<>();
    private final List<LeaderboardButtonWidget> subtypeButtons = new ArrayList<>();
    private final RewardsButtonWidget rewardsButton = new RewardsButtonWidget();
    private final GuildSeasonRewardsWidget rewardsWidget = new GuildSeasonRewardsWidget();
    private List<LeaderboardEntry> displayedEntries = List.of();
    private LeaderboardDefinition displayedGroup;
    private GuildSeason displayedSeason;
    private int currentPage = 1;

    public LeaderboardListWidget(TypeWidget owner) {
        this.owner = owner;
        for (int page = 1; page <= PAGE_COUNT; page++) {
            LeaderboardPageButtonWidget button = new LeaderboardPageButtonWidget(page, this::selectPage);
            pageButtons.add(button);
            addChild(button);
        }
        addChild(rewardsButton);
        addChild(rewardsWidget);
    }

    @Override
    protected void updateValues() {
        List<LeaderboardEntry> entries = owner.getLeaderboardEntries();
        if (entries != displayedEntries) rebuildEntries(entries);
        LeaderboardDefinition activeGroup = owner.getActiveLeaderboardGroup();
        if (activeGroup != displayedGroup) rebuildSubtypeButtons(activeGroup);
        GuildSeason season = owner.getActiveGuildSeason();
        if (season != displayedSeason) {
            displayedSeason = season;
            rewardsWidget.setVisible(false);
            rewardsWidget.setSeason(season);
            rebuildPageEntries();
        }

        for (int i = 0; i < entryWidgets.size(); i++) {
            LeaderboardEntryWidget entry = entryWidgets.get(i);
            entry.setBounds(x + 12, y + getHeaderHeight() + i * (ROW_HEIGHT + ROW_GAP),
                    width - 24, ROW_HEIGHT);
            entry.setVisible(!rewardsWidget.isVisible());
        }

        if (!subtypeButtons.isEmpty()) {
            int buttonWidth = 260;
            int buttonGap = 18;
            int totalWidth = subtypeButtons.size() * buttonWidth
                    + (subtypeButtons.size() - 1) * buttonGap;
            int buttonX = x + (width - totalWidth) / 2;
            for (int i = 0; i < subtypeButtons.size(); i++) {
                LeaderboardButtonWidget button = subtypeButtons.get(i);
                button.setBounds(buttonX + i * (buttonWidth + buttonGap), y + 45, buttonWidth, 45);
                button.setActive(button.getLeaderboard().equals(owner.getActiveLeaderboard()));
            }
        }

        int buttonWidth = 62;
        int buttonGap = 8;
        int totalButtonWidth = PAGE_COUNT * buttonWidth + (PAGE_COUNT - 1) * buttonGap;
        int buttonX = x + (width - totalButtonWidth) / 2;
        for (int i = 0; i < pageButtons.size(); i++) {
            LeaderboardPageButtonWidget button = pageButtons.get(i);
            button.setBounds(buttonX + i * (buttonWidth + buttonGap), y + height - FOOTER_HEIGHT - 2,
                    buttonWidth, 34);
            button.setActive(i + 1 == currentPage);
            button.setVisible(!displayedEntries.isEmpty() && !rewardsWidget.isVisible());
        }

        rewardsButton.setVisible(season != null);
        if (season != null) {
            rewardsButton.setBounds(x + width - 172, y + 12, 150, 36);
            rewardsWidget.setBounds(x + 12, y + 92, width - 24, height - 104);
        }
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        LeaderboardDefinition leaderboard = owner.getActiveLeaderboard();
        LeaderboardDefinition group = owner.getActiveLeaderboardGroup();
        if (displayedSeason != null) {
            ui.drawCenteredText("Season " + displayedSeason.number() + " | " + displayedSeason.status(),
                    x + width / 2f, y + 35, CustomColor.fromHexString("FFD966"), 4);
            ui.drawCenteredText(displayedSeason.dateRange(), x + width / 2f, y + 75,
                    CustomColor.fromHexString("FFFFFF"), 3);
        } else {
            ui.drawCenteredText(group != null ? group.displayName()
                            : leaderboard == null ? "Leaderboard" : leaderboard.displayName(),
                    x + width / 2f, y + 27, CustomColor.fromHexString("FFD966"));
        }

        if (rewardsWidget.isVisible()) {
            return;
        } else if (leaderboard == null) {
            drawStatus("Select a leaderboard on the left.");
        } else if (displayedEntries.isEmpty()) {
            if (owner.isLeaderboardLoading()) drawStatus("Loading leaderboard...");
            else if (owner.getLeaderboardError() != null) drawStatus("Failed to load the leaderboard.");
            else drawStatus("This leaderboard has no entries.");
        } else if (entryWidgets.isEmpty()) {
            drawStatus("This page has no entries.");
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if (rewardsWidget.isVisible()) return;
        for (LeaderboardEntryWidget entry : entryWidgets) {
            if (!entry.isHovered()) continue;
            List<Text> tooltip = entry.getSeasonTooltip();
            if (!tooltip.isEmpty()) TextInputWidget.drawFittingTooltip(ctx, tooltip, mouseX, mouseY);
            return;
        }
    }

    private void drawStatus(String text) {
        ui.drawCenteredText(text, x + width / 2f, y + height / 2f, CustomColor.fromHexString("D0D0D0"));
    }

    private void rebuildEntries(List<LeaderboardEntry> entries) {
        displayedEntries = entries;
        rebuildPageEntries();
    }

    public void resetPage() {
        currentPage = 1;
        rebuildPageEntries();
    }

    public void restorePage(int page) {
        currentPage = Math.clamp(page, 1, PAGE_COUNT);
        rebuildPageEntries();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void rebuildCurrentPage() {
        rebuildPageEntries();
    }

    private void selectPage(int page) {
        if (page == currentPage) return;
        currentPage = page;
        rebuildPageEntries();
    }

    private void rebuildPageEntries() {
        for (LeaderboardEntryWidget entryWidget : entryWidgets) removeChild(entryWidget);
        entryWidgets.clear();

        int fromIndex = Math.min(displayedEntries.size(), (currentPage - 1) * PAGE_SIZE);
        int toIndex = Math.min(displayedEntries.size(), fromIndex + PAGE_SIZE);
        for (int entryIndex = fromIndex; entryIndex < toIndex; entryIndex++) {
            LeaderboardEntry entry = displayedEntries.get(entryIndex);
            LeaderboardEntry nextEntry = entryIndex + 1 < displayedEntries.size()
                    ? displayedEntries.get(entryIndex + 1) : null;
            LeaderboardEntryWidget entryWidget = new LeaderboardEntryWidget(
                    entry, owner.getType(), owner.getActiveLeaderboard(), owner.getActiveGuildSeason(),
                    owner.isOwnEntry(entry), nextEntry);
            entryWidgets.add(entryWidget);
            addChild(entryWidget);
        }
        raiseSeasonControls();
    }

    private void rebuildSubtypeButtons(LeaderboardDefinition group) {
        for (LeaderboardButtonWidget button : subtypeButtons) removeChild(button);
        subtypeButtons.clear();
        displayedGroup = group;
        if (group == null) return;
        for (LeaderboardDefinition variant : group.variants()) {
            LeaderboardButtonWidget button = new LeaderboardButtonWidget(variant, owner::selectLeaderboardVariant);
            subtypeButtons.add(button);
            addChild(button);
        }
        raiseSeasonControls();
    }

    private int getHeaderHeight() {
        if (displayedSeason != null) return SEASON_HEADER_HEIGHT;
        return displayedGroup == null ? HEADER_HEIGHT : SUBTYPE_HEADER_HEIGHT;
    }

    private void raiseSeasonControls() {
        removeChild(rewardsButton);
        removeChild(rewardsWidget);
        addChild(rewardsButton);
        addChild(rewardsWidget);
    }

    private class RewardsButtonWidget extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawFixedVanillaPanelButtonFade(x, y, width, height, 10, 2,
                    hovered || rewardsWidget.isVisible());
            ui.drawCenteredText(rewardsWidget.isVisible() ? "Close" : "Rewards",
                    x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 2.3f);
        }

        @Override
        protected boolean onClick(int button) {
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            rewardsWidget.toggle();
            return true;
        }
    }
}
