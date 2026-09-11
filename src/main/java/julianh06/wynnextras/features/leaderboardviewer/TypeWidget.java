package julianh06.wynnextras.features.leaderboardviewer;

import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class TypeWidget extends Widget {
    private final LVScreen.Type type;
    private final List<CategoryWidget> categoryWidgets = new ArrayList<>();
    private CategoryWidget activeCategory;
    private LeaderboardDefinition activeLeaderboard;
    private List<LeaderboardEntry> leaderboardEntries = List.of();
    private boolean leaderboardLoading;
    private Throwable leaderboardError;
    private long requestGeneration;

    static Identifier tabLeft = Identifier.of("wynnextras", "textures/gui/profileviewer/tableft.png");
    static Identifier tabMid = Identifier.of("wynnextras", "textures/gui/profileviewer/tabmid.png");
    static Identifier tagRight = Identifier.of("wynnextras", "textures/gui/profileviewer/tabright.png");

    static Identifier tabLeftDark = Identifier.of("wynnextras", "textures/gui/profileviewer/tableft_dark.png");
    static Identifier tabMidDark = Identifier.of("wynnextras", "textures/gui/profileviewer/tabmid_dark.png");
    static Identifier tagRightDark = Identifier.of("wynnextras", "textures/gui/profileviewer/tabright_dark.png");

    public TypeWidget(LVScreen.Type type, List<LeaderboardCategory> categories) {
        super(0, 0, 0, 0);
        this.type = type;
        for (LeaderboardCategory category : categories) {
            categoryWidgets.add(new CategoryWidget(category, this::selectCategory));
        }
        if (!categoryWidgets.isEmpty()) selectCategory(categoryWidgets.getFirst());
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        int totalWidth = 0;
        int xStart = x + 22;
        for (CategoryWidget categoryWidget : categoryWidgets) {
            int signWidth = drawDynamicNameSign(categoryWidget.getName(), xStart + totalWidth, y - 56);
            categoryWidget.setBounds(xStart + totalWidth, y - 56, signWidth, 55);
            categoryWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
            totalWidth += signWidth + 12;

            if (categoryWidget == activeCategory) {
                int currentY = y + 10;
                for (LeaderboardDefinition leaderboard : categoryWidget.getCategory().leaderboards()) {
                    ui.drawVanillaPanelButton(x + 10, currentY, 570, 38, 9, 2, hovered);
                    ui.drawCenteredText(leaderboard.displayName(), x + 285, currentY + 20);
                    currentY += 48;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (CategoryWidget categoryWidget : categoryWidgets) {
            if (categoryWidget.isHovered()) {
                return categoryWidget.mouseClicked(mx, my, button);
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void selectCategory(CategoryWidget categoryWidget) {
        if (activeCategory == categoryWidget) return;
        if (activeCategory != null) activeCategory.setActive(false);
        activeCategory = categoryWidget;
        activeCategory.setActive(true);
        activeLeaderboard = null;
        leaderboardEntries = List.of();
        leaderboardLoading = false;
        leaderboardError = null;
        requestGeneration++;
    }

    public void selectLeaderboard(LeaderboardDefinition leaderboard) {
        activeLeaderboard = leaderboard;
        leaderboardEntries = List.of();
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

    public LVScreen.Type getType() {
        return type;
    }

    public CategoryWidget getActiveCategory() {
        return activeCategory;
    }

    public LeaderboardDefinition getActiveLeaderboard() {
        return activeLeaderboard;
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

    public int drawDynamicNameSign(String input, int x, int y) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int strWidth = textRenderer.getWidth(input) + 10;
        int strMidWidth = strWidth - 15;
        int amount = Math.max(0, Math.ceilDiv(strMidWidth, 10));
        PVScreen.DarkModeToggleWidget.drawImageWithFade(tabLeftDark, tabLeft, x, y, 30, 60, ui);

        for (int i = 0; i < amount; i++) {
            PVScreen.DarkModeToggleWidget.drawImageWithFade(tabMidDark, tabMid, x + 30 * (i + 1), y, 30, 60, ui);
        }

        PVScreen.DarkModeToggleWidget.drawImageWithFade(tagRightDark, tagRight, x + 30 * (amount + 1), y, 30, 60, ui);
        return 60 + amount * 30;
    }
}
