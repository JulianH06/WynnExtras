package julianh06.wynnextras.features.aspects.pages;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.aspects.AspectScreen;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.LeaderboardEntry;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public class LeadboardPage extends PageWidget {
    private static final int LEADERBOARD_LIMIT = 25;
    private static final int ENTRY_WIDTH = 800;
    private static final int ENTRY_HEIGHT = 50;
    private static final int ENTRY_SPACING = 10;
    private static final int SCROLL_PADDING = 16;

    private static List<LeaderboardEntry> leaderboardList = null;
    private static List<LeaderBoardEntryWidget> leaderBoardEntryWidgets = new ArrayList<>();
    private static boolean fetchedLeaderboard = false;

    private final RefreshButton refreshButton;
    private final ScrollBarWidget scrollBarWidget;
    private float targetOffset = 0;
    private float actualOffset = 0;
    private float maxOffset = 0;
    private int listTop = 150;
    private int listBottom = 700;

    public LeadboardPage(AspectScreen parent) {
        super(parent);

        refreshButton = new RefreshButton();
        scrollBarWidget = new ScrollBarWidget();
    }

    @Override
    public void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        int logicalW = (int) (width * ui.getScaleFactorF());
        int logicalH = (int) (height * ui.getScaleFactorF());
        int centerX = logicalW / 2;

        ui.drawCenteredText("§6§lLEADERBOARD", centerX, 60);
        ui.drawCenteredText("§7Top " + LEADERBOARD_LIMIT + " players with the most maxed aspects", centerX, 95);

        if (!fetchedLeaderboard) {
            fetchedLeaderboard = true;
            WynncraftApiHandler.fetchLeaderboard(LEADERBOARD_LIMIT).thenAccept(result -> {
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

        int startX = centerX - ENTRY_WIDTH / 2;
        listTop = 150;
        listBottom = Math.max(listTop + 80, logicalH - 120);
        int visibleHeight = listBottom - listTop;
        int contentHeight = leaderBoardEntryWidgets.size() * ENTRY_HEIGHT
                + Math.max(0, leaderBoardEntryWidgets.size() - 1) * ENTRY_SPACING;
        maxOffset = Math.max(0, contentHeight - visibleHeight + SCROLL_PADDING);
        targetOffset = Math.min(targetOffset, maxOffset);
        actualOffset = Math.min(actualOffset, maxOffset);

        float snapValue = 0.5f;
        float speed = 0.3f;
        float diff = targetOffset - actualOffset;
        if(Math.abs(diff) < snapValue || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) actualOffset = targetOffset;
        else actualOffset += diff * speed * tickDelta;

        int startY = listTop - (int) actualOffset;

        ctx.enableScissor(
                (int) (startX / ui.getScaleFactor()),
                (int) (listTop / ui.getScaleFactor()),
                (int) ((startX + ENTRY_WIDTH + 8) / ui.getScaleFactor()),
                (int) (listBottom / ui.getScaleFactor()));
        for (LeaderBoardEntryWidget leaderBoardEntryWidget : leaderBoardEntryWidgets) {
            leaderBoardEntryWidget.setBounds(startX, startY, ENTRY_WIDTH, ENTRY_HEIGHT);
            leaderBoardEntryWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
            startY += ENTRY_HEIGHT + ENTRY_SPACING;
        }
        ctx.disableScissor();

        if(maxOffset > 0) {
            scrollBarWidget.setBounds(startX + ENTRY_WIDTH + 20, listTop, 25, visibleHeight);
            scrollBarWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
        } else {
            scrollBarWidget.setBounds(0, 0, 0, 0);
        }

        ui.drawCenteredText("§7Click on a player to view their aspects", centerX, logicalH - 95);

        refreshButton.setBounds(0, 0, 360, 60);
        refreshButton.draw(ctx, mouseX, mouseY, tickDelta, ui);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if(refreshButton.isHovered()) {
            refreshButton.onClick(button);
            return true;
        }

        if(scrollBarWidget.isHovered()) {
            scrollBarWidget.onClick(button);
            return true;
        }

        if(!isInListViewport(mx, my)) return false;

        for(LeaderBoardEntryWidget leaderBoardEntryWidget : leaderBoardEntryWidgets) {
            if(leaderBoardEntryWidget.mouseClicked(mx, my, button)) return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        scrollBarWidget.scrollBarButtonWidget.isHold = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return scrollBarWidget.dragTo(my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if(maxOffset <= 0 || !isInListViewport(mx, my)) return false;
        if(delta > 0) targetOffset -= 60f;
        else targetOffset += 60f;
        targetOffset = Math.max(0, Math.min(targetOffset, maxOffset));
        return true;
    }

    private boolean isInListViewport(double mx, double my) {
        if(ui == null) return false;
        return mx >= ui.sx(centeredListX())
                && mx < ui.sx(centeredListX() + ENTRY_WIDTH)
                && my >= ui.sy(listTop)
                && my < ui.sy(listBottom);
    }

    private int centeredListX() {
        return (int) (width * ui.getScaleFactorF()) / 2 - ENTRY_WIDTH / 2;
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

    private class ScrollBarWidget extends Widget {
        final ScrollBarButtonWidget scrollBarButtonWidget;
        int currentMouseY = 0;

        public ScrollBarWidget() {
            super(0, 0, 0, 0);
            this.scrollBarButtonWidget = new ScrollBarButtonWidget();
            addChild(scrollBarButtonWidget);
        }

        private void setOffset(int mouseY, int maxOffset, int scrollAreaHeight) {
            if(maxOffset <= 0 || scrollAreaHeight <= 0) {
                targetOffset = 0;
                return;
            }

            float relativeY = mouseY - y - scrollBarButtonWidget.getHeight() / 2f;
            relativeY = Math.max(0, Math.min(relativeY, scrollAreaHeight));

            targetOffset = relativeY / scrollAreaHeight * maxOffset;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            currentMouseY = mouseY;
            ui.drawSliderBackground(x, y, width, height);

            int buttonHeight = Math.max(30, (int) (height * (height / (float) (height + maxOffset))));
            int scrollAreaHeight = height - buttonHeight;

            if(scrollBarButtonWidget.isHold) {
                setOffset((int) (mouseY * ui.getScaleFactor()), (int) maxOffset, scrollAreaHeight);
                actualOffset = targetOffset;
            }

            int yPos = maxOffset == 0 ? y : (int) (y + scrollAreaHeight * Math.min(actualOffset / maxOffset, 1));
            scrollBarButtonWidget.setBounds(x, yPos, width, buttonHeight);
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());

            int buttonHeight = scrollBarButtonWidget.getHeight();
            int scrollAreaHeight = height - buttonHeight;

            if(scrollBarButtonWidget.isHovered()) scrollBarButtonWidget.isHold = true;
            setOffset((int) (currentMouseY * ui.getScaleFactor() + buttonHeight / 2f), (int) maxOffset, scrollAreaHeight);

            return true;
        }

        private boolean dragTo(double mouseY) {
            if(!scrollBarButtonWidget.isHold) return false;

            int buttonHeight = scrollBarButtonWidget.getHeight();
            setOffset((int) (mouseY * ui.getScaleFactor()), (int) maxOffset, height - buttonHeight);
            actualOffset = targetOffset;
            return true;
        }

        private static class ScrollBarButtonWidget extends Widget {
            public boolean isHold = false;

            public ScrollBarButtonWidget() {
                super(0, 0, 0, 0);
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                ui.drawButton(x, y, width, height, hovered || isHold);
            }
        }
    }

    private class RefreshButton extends Widget {
        public RefreshButton() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, hovered);
            ui.drawCenteredText("Reload leaderboards", x + width / 2f, y + height / 2f);
        }

        @Override
        protected boolean onClick(int button) {
            leaderboardList = null;
            leaderBoardEntryWidgets = new ArrayList<>();
            fetchedLeaderboard = false;
            targetOffset = 0;
            actualOffset = 0;
            maxOffset = 0;

            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
    }
}
