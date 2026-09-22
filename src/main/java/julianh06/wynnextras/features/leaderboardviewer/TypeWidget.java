package julianh06.wynnextras.features.leaderboardviewer;

import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TypeWidget extends Widget {
    private final LVScreen.Type type;
    private final List<CategoryWidget> categoryWidgets = new ArrayList<>();
    private final List<LeaderboardButtonWidget> leaderboardButtonWidgets = new ArrayList<>();
    private final LeaderboardButtonListWidget leaderboardButtonListWidget = new LeaderboardButtonListWidget();
    private final LeaderboardListWidget leaderboardListWidget = new LeaderboardListWidget(this);
    private CategoryWidget activeCategory;
    private LeaderboardDefinition activeLeaderboardGroup;
    private LeaderboardDefinition activeLeaderboard;
    private List<LeaderboardEntry> leaderboardEntries = List.of();
    private boolean leaderboardLoading;
    private Throwable leaderboardError;
    private long requestGeneration;
    private Map<String, GuildSeason> guildSeasons = Map.of();
    private String ownPlayerName;
    private String ownGuildName;
    private String ownGuildPrefix;

    public TypeWidget(LVScreen.Type type, List<LeaderboardCategory> categories) {
        super(0, 0, 0, 0);
        this.type = type;
        addChild(leaderboardButtonListWidget);
        addChild(leaderboardListWidget);
        setCategories(categories);
    }

    public void setCategories(List<LeaderboardCategory> categories) {
        State state = snapshotState();
        for (CategoryWidget categoryWidget : categoryWidgets) removeChild(categoryWidget);
        categoryWidgets.clear();
        for (LeaderboardCategory category : categories) {
            CategoryWidget categoryWidget = new CategoryWidget(category, this::selectCategory);
            categoryWidgets.add(categoryWidget);
            addChild(categoryWidget);
        }
        activeCategory = null;
        if (categoryWidgets.isEmpty()) return;
        restoreState(state);
    }

    @Override
    protected void updateValues() {
        int totalWidth = 0;
        int xStart = x + 22;
        for (CategoryWidget categoryWidget : categoryWidgets) {
            int signWidth = categoryWidget.getPreferredWidth();
            categoryWidget.setBounds(xStart + totalWidth, y - 56, signWidth, 55);
            totalWidth += signWidth + 12;
        }
        leaderboardButtonListWidget.setBounds(x + 12, y + 12, 570, height - 24);
        leaderboardListWidget.setBounds(x + 600, y + 10, width - 612, height - 20);
        updateCurrentSeasonIndicators();
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
    }

    private void selectCategory(CategoryWidget categoryWidget) {
        if (activeCategory == categoryWidget) return;
        if (activeCategory != null) activeCategory.setActive(false);
        activeCategory = categoryWidget;
        activeCategory.setActive(true);
        activeLeaderboardGroup = null;
        activeLeaderboard = null;
        leaderboardEntries = List.of();
        leaderboardLoading = false;
        leaderboardError = null;
        requestGeneration++;
        leaderboardListWidget.resetPage();
        rebuildLeaderboardButtons();
    }

    public void selectLeaderboard(LeaderboardDefinition leaderboard) {
        if (leaderboard.hasVariants()) {
            activeLeaderboardGroup = leaderboard;
            selectLeaderboardVariant(leaderboard.variants().getFirst());
            return;
        }
        activeLeaderboardGroup = null;
        fetchLeaderboard(leaderboard, true);
    }

    void selectLeaderboardVariant(LeaderboardDefinition leaderboard) {
        fetchLeaderboard(leaderboard, true);
    }

    private void fetchLeaderboard(LeaderboardDefinition leaderboard, boolean resetPage) {
        for (LeaderboardButtonWidget buttonWidget : leaderboardButtonWidgets) {
            buttonWidget.setActive(buttonWidget.getLeaderboard().equals(leaderboard)
                    || buttonWidget.getLeaderboard().equals(activeLeaderboardGroup));
        }
        activeLeaderboard = leaderboard;
        if (resetPage) leaderboardListWidget.resetPage();
        if (resetPage) leaderboardEntries = List.of();
        leaderboardError = null;
        leaderboardLoading = true;
        long generation = ++requestGeneration;

        LeaderboardService.fetchLeaderboard(leaderboard.id()).whenComplete((entries, error) ->
                MinecraftClient.getInstance().execute(() -> {
                    if (generation != requestGeneration || !leaderboard.equals(activeLeaderboard)) return;
                    leaderboardLoading = false;
                    if (error != null) {
                        leaderboardError = error;
                        return;
                    }
                    leaderboardEntries = entries;
                }));
    }

    public boolean reloadLeaderboard() {
        if (activeLeaderboard == null || leaderboardLoading) return false;
        LeaderboardService.invalidateLeaderboard(activeLeaderboard.id());
        fetchLeaderboard(activeLeaderboard, false);
        return true;
    }

    public boolean selectLeaderboardById(String id) {
        for (CategoryWidget categoryWidget : categoryWidgets) {
            for (LeaderboardDefinition leaderboard : categoryWidget.getCategory().leaderboards()) {
                if (id.equals(leaderboard.id())) {
                    selectCategory(categoryWidget);
                    selectLeaderboard(leaderboard);
                    return true;
                }
                for (LeaderboardDefinition variant : leaderboard.variants()) {
                    if (!id.equals(variant.id())) continue;
                    selectCategory(categoryWidget);
                    activeLeaderboardGroup = leaderboard;
                    selectLeaderboardVariant(variant);
                    return true;
                }
            }
        }
        return false;
    }

    public LVScreen.Type getType() {
        return type;
    }

    public CategoryWidget getActiveCategory() {
        return activeCategory;
    }

    public LeaderboardDefinition getActiveLeaderboard() {
        return activeLeaderboard;
    }

    public LeaderboardDefinition getActiveLeaderboardGroup() {
        return activeLeaderboardGroup;
    }

    public List<LeaderboardEntry> getLeaderboardEntries() {
        return leaderboardEntries;
    }

    public boolean isLeaderboardLoading() {
        return leaderboardLoading;
    }

    public Throwable getLeaderboardError() {
        return leaderboardError;
    }

    public void setGuildSeasons(Map<String, GuildSeason> guildSeasons) {
        this.guildSeasons = guildSeasons;
        updateCurrentSeasonIndicators();
    }

    public GuildSeason getActiveGuildSeason() {
        return activeLeaderboard == null || activeLeaderboard.id() == null
                ? null : guildSeasons.get(activeLeaderboard.id());
    }

    public void setOwnIdentity(String playerName, String guildName, String guildPrefix) {
        ownPlayerName = playerName;
        ownGuildName = guildName;
        ownGuildPrefix = guildPrefix;
        leaderboardListWidget.rebuildCurrentPage();
    }

    public boolean isOwnEntry(LeaderboardEntry entry) {
        if (type == LVScreen.Type.Guild) {
            return equalsIgnoreCase(entry.name(), ownGuildName)
                    || equalsIgnoreCase(entry.prefix(), ownGuildPrefix);
        }
        return equalsIgnoreCase(entry.name(), ownPlayerName);
    }

    private static boolean equalsIgnoreCase(String first, String second) {
        return first != null && second != null && !first.isBlank() && first.equalsIgnoreCase(second);
    }

    public State snapshotState() {
        return new State(
                activeCategory == null ? null : activeCategory.getName(),
                activeLeaderboard == null ? null : activeLeaderboard.id(),
                leaderboardListWidget.getCurrentPage(),
                leaderboardButtonListWidget.getTargetOffset(),
                leaderboardButtonListWidget.getActualOffset());
    }

    public void restoreState(State state) {
        if (categoryWidgets.isEmpty()) return;
        CategoryWidget categoryToSelect = categoryWidgets.stream()
                .filter(category -> category.getName().equals(state.categoryName()))
                .findFirst()
                .orElse(categoryWidgets.getFirst());
        selectCategory(categoryToSelect);
        if (state.leaderboardId() != null) selectLeaderboardById(state.leaderboardId());
        leaderboardListWidget.restorePage(state.page());
        leaderboardButtonListWidget.restoreScroll(state.targetOffset(), state.actualOffset());
    }

    private void rebuildLeaderboardButtons() {
        for (LeaderboardButtonWidget buttonWidget : leaderboardButtonWidgets) {
            leaderboardButtonListWidget.removeButton(buttonWidget);
        }
        leaderboardButtonWidgets.clear();
        for (LeaderboardDefinition leaderboard : activeCategory.getCategory().leaderboards()) {
            LeaderboardButtonWidget buttonWidget = new LeaderboardButtonWidget(leaderboard, this::selectLeaderboard);
            leaderboardButtonWidgets.add(buttonWidget);
            leaderboardButtonListWidget.addButton(buttonWidget);
        }
        leaderboardButtonListWidget.resetScroll();
        updateCurrentSeasonIndicators();
    }

    private void updateCurrentSeasonIndicators() {
        for (LeaderboardButtonWidget button : leaderboardButtonWidgets) {
            String id = button.getLeaderboard().id();
            GuildSeason season = id == null ? null : guildSeasons.get(id);
            button.setCurrentSeason(season != null && season.isActive());
        }
    }

    public record State(String categoryName, String leaderboardId, int page,
                        float targetOffset, float actualOffset) {
    }
}
