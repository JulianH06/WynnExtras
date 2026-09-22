package julianh06.wynnextras.features.leaderboardviewer;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.render.HorizontalAlignment;
import julianh06.wynnextras.utils.render.VerticalAlignment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuildSeasonRewardsWidget extends Widget {
    private final RewardTabButton ratingTab = new RewardTabButton("SR Rewards", true);
    private final RewardTabButton placementTab = new RewardTabButton("Placement Rewards", false);
    private final RewardListWidget rewardList = new RewardListWidget();
    private GuildSeason season;
    private boolean showRatingRewards = true;

    public GuildSeasonRewardsWidget() {
        addChild(ratingTab);
        addChild(placementTab);
        addChild(rewardList);
        setVisible(false);
    }

    public void setSeason(GuildSeason season) {
        if (this.season == season) return;
        this.season = season;
        showRatingRewards = true;
        rebuildRewards();
    }

    public void toggle() {
        setVisible(!isVisible());
    }

    @Override
    protected void updateValues() {
        int tabWidth = (width - 54) / 2;
        ratingTab.setBounds(x + 18, y + 48, tabWidth, 38);
        placementTab.setBounds(x + 36 + tabWidth, y + 48, tabWidth, 38);
        rewardList.setBounds(x + 18, y + 100, width - 36, height - 118);
        ratingTab.setActive(showRatingRewards);
        placementTab.setActive(!showRatingRewards);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        ui.drawCenteredText("Season " + season.number() + " Rewards", x + width / 2f, y + 25,
                CustomColor.fromHexString("FFD966"));
    }

    @Override
    protected boolean onClick(int button) {
        return true;
    }

    private void selectTab(boolean ratingRewards) {
        if (showRatingRewards == ratingRewards) return;
        showRatingRewards = ratingRewards;
        rebuildRewards();
        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
    }

    private void rebuildRewards() {
        if (season == null) {
            rewardList.setRewards(List.of());
            return;
        }
        rewardList.setRewards(showRatingRewards ? season.ratingRewards() : season.leaderboardRewards());
    }

    public static String formatReward(GuildSeason.Reward reward) {
        String value = reward.value();
        return switch (reward.type()) {
            case "BADGE" -> "Badge: " + value;
            case "EMERALD" -> formatNumber(value) + " Emeralds";
            case "PUBLIC_BANK_SLOT" -> quantity(value, "Public Bank Slot");
            case "PRIVATE_BANK_SLOT" -> quantity(value, "Private Bank Slot");
            case "GUILD_TOME" -> quantity(value, "Guild Tome");
            case "EFFECT" -> clean(value);
            case "STRUCTURE" -> "Guild Structure: " + clean(value);
            case "COSMETIC" -> "Cosmetic: " + clean(value);
            default -> clean(reward.type()) + (value == null ? "" : ": " + clean(value));
        };
    }

    public static String formatCondition(GuildSeason.Reward reward) {
        return "SR".equals(reward.conditionType())
                ? NumberFormat.getIntegerInstance(Locale.getDefault()).format(reward.threshold()) + " SR"
                : "Top " + reward.threshold();
    }

    private static String quantity(String value, String singular) {
        int amount = value == null ? 1 : Integer.parseInt(value);
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(amount)
                + " " + singular + (amount == 1 ? "" : "s");
    }

    private static String formatNumber(String value) {
        if (value == null) return "1";
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(Long.parseLong(value));
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) return "Reward";
        String spaced = value.replace('_', ' ').replaceAll("([a-z])([A-Z0-9])", "$1 $2").toLowerCase(Locale.ROOT);
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private class RewardTabButton extends Widget {
        private final String label;
        private final boolean ratingRewards;
        private boolean active;

        private RewardTabButton(String label, boolean ratingRewards) {
            this.label = label;
            this.ratingRewards = ratingRewards;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawFixedVanillaPanelButtonFade(x, y, width, height, 10, 2, hovered || active);
            ui.drawCenteredText(label, x + width / 2f, y + height / 2f,
                    active ? CustomColor.fromHexString("FFFF00") : CustomColor.fromHexString("FFFFFF"), 2.4f);
        }

        @Override
        protected boolean onClick(int button) {
            selectTab(ratingRewards);
            return true;
        }

        private void setActive(boolean active) {
            this.active = active;
        }
    }

    private static class RewardRowWidget extends Widget {
        private final GuildSeason.Reward reward;

        private RewardRowWidget(GuildSeason.Reward reward) {
            this.reward = reward;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawFixedVanillaPanelButtonFade(x, y, width, height, 10, 2, hovered);
            ui.drawText(formatCondition(reward), x + 18, y + height / 2f,
                    CustomColor.fromHexString("FFD966"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.2f);
            String rewardText = formatReward(reward);
            if (reward.expires() != null) {
                rewardText += " | Expires " + GuildSeason.formatDate(reward.expires());
            }
            ui.drawText(rewardText, x + 185, y + height / 2f, CustomColor.fromHexString("FFFFFF"),
                    HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.2f);
        }
    }

    private static class RewardListWidget extends Widget {
        private static final int ROW_HEIGHT = 44;
        private static final int ROW_GAP = 6;
        private static final int SCROLLBAR_WIDTH = 18;
        private final List<RewardRowWidget> rows = new ArrayList<>();
        private final RewardScrollBarWidget scrollBar = new RewardScrollBarWidget(this);
        private float targetOffset;
        private float actualOffset;
        private float maxOffset;

        private RewardListWidget() {
            addChild(scrollBar);
        }

        private void setRewards(List<GuildSeason.Reward> rewards) {
            for (RewardRowWidget row : rows) removeChild(row);
            rows.clear();
            for (GuildSeason.Reward reward : rewards) {
                RewardRowWidget row = new RewardRowWidget(reward);
                rows.add(row);
                addChild(row);
            }
            targetOffset = 0;
            actualOffset = 0;
        }

        @Override
        protected void updateValues() {
            int contentHeight = rows.isEmpty() ? 0 : rows.size() * ROW_HEIGHT + (rows.size() - 1) * ROW_GAP;
            maxOffset = Math.max(0, contentHeight - height);
            targetOffset = Math.clamp(targetOffset, 0, maxOffset);
            actualOffset = Math.clamp(actualOffset, 0, maxOffset);
        }

        @Override
        protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ctx.enableScissor((int) ui.sx(x), (int) ui.sy(y), (int) ui.sx(x + width), (int) ui.sy(y + height));
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            float difference = targetOffset - actualOffset;
            if (Math.abs(difference) < 0.5f || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) {
                actualOffset = targetOffset;
            } else {
                actualOffset += difference * Math.min(1f, 0.3f * tickDelta);
            }
            int rowWidth = maxOffset > 0 ? width - SCROLLBAR_WIDTH - 10 : width;
            int rowY = y - Math.round(actualOffset);
            for (RewardRowWidget row : rows) {
                row.setBounds(x, rowY, rowWidth, ROW_HEIGHT);
                rowY += ROW_HEIGHT + ROW_GAP;
            }
            scrollBar.setVisible(maxOffset > 0);
            if (maxOffset > 0) scrollBar.setBounds(x + width - SCROLLBAR_WIDTH, y, SCROLLBAR_WIDTH, height);
        }

        @Override
        protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ctx.disableScissor();
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!contains((int) mx, (int) my)) return false;
            return super.mouseClicked(mx, my, button);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            if (maxOffset <= 0 || !contains((int) mx, (int) my)) return false;
            targetOffset = Math.clamp(targetOffset - (float) delta * 80f, 0, maxOffset);
            return true;
        }
    }

    private static class RewardScrollBarWidget extends Widget {
        private final RewardListWidget owner;
        private final RewardScrollThumbWidget thumb = new RewardScrollThumbWidget();
        private int currentMouseY;

        private RewardScrollBarWidget(RewardListWidget owner) {
            this.owner = owner;
            addChild(thumb);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            currentMouseY = mouseY;
            ui.drawSliderFade(x, y, width, height, 5, PVScreen.DarkModeToggleWidget.fade);
            int thumbHeight = Math.max(40, Math.round(height * height / (height + owner.maxOffset)));
            int travel = height - thumbHeight;
            int thumbY = owner.maxOffset <= 0 ? y : y + Math.round(travel * owner.actualOffset / owner.maxOffset);
            thumb.setBounds(x, thumbY, width, thumbHeight);
        }

        @Override
        protected boolean onClick(int button) {
            setOffset(currentMouseY);
            owner.actualOffset = owner.targetOffset;
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (!thumb.held) return false;
            setOffset((int) mouseY);
            owner.actualOffset = owner.targetOffset;
            return true;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            if (!thumb.held) return false;
            thumb.held = false;
            return true;
        }

        private void setOffset(int mouseY) {
            int travel = height - thumb.getHeight();
            if (travel <= 0) return;
            float logicalMouseY = (float) ((mouseY - ui.getYStart()) * ui.getScaleFactor());
            float thumbY = Math.clamp(logicalMouseY - y - thumb.getHeight() / 2f, 0, travel);
            owner.targetOffset = thumbY / travel * owner.maxOffset;
        }
    }

    private static class RewardScrollThumbWidget extends Widget {
        private boolean held;

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButtonFade(x, y, width, height, 5, hovered || held);
        }

        @Override
        protected boolean onClick(int button) {
            held = true;
            return true;
        }
    }
}
