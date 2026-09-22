package julianh06.wynnextras.features.leaderboardviewer;

import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class LeaderboardButtonWidget extends Widget {
    private static final Identifier ACTIVE_SEASON = Identifier.of(
            "wynnextras", "textures/gui/profileviewer/onlinecircle.png");
    private static final Identifier ACTIVE_SEASON_DARK = Identifier.of(
            "wynnextras", "textures/gui/profileviewer/onlinecircle_dark.png");
    private final LeaderboardDefinition leaderboard;
    private final Consumer<LeaderboardDefinition> onSelected;
    private boolean active;
    private boolean currentSeason;

    public LeaderboardButtonWidget(LeaderboardDefinition leaderboard, Consumer<LeaderboardDefinition> onSelected) {
        this.leaderboard = leaderboard;
        this.onSelected = onSelected;
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        ui.drawFixedVanillaPanelButtonFade(x, y, width, height, 10, 2, hovered || active);
        ui.drawCenteredText(leaderboard.displayName(), x + width / 2f, y + height / 2f,
                active ? CustomColor.fromHexString("FFFF00") : CustomColor.fromHexString("FFFFFF"));
        if (currentSeason) {
            PVScreen.DarkModeToggleWidget.drawImageWithFade(
                    ACTIVE_SEASON_DARK, ACTIVE_SEASON, x + 12, y + height / 2f - 11, 18, 18, ui);
        }
    }

    @Override
    protected boolean onClick(int button) {
        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        onSelected.accept(leaderboard);
        return true;
    }

    public LeaderboardDefinition getLeaderboard() {
        return leaderboard;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCurrentSeason(boolean currentSeason) {
        this.currentSeason = currentSeason;
    }
}
