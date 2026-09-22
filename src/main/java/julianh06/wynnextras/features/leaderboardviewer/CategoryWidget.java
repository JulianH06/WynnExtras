package julianh06.wynnextras.features.leaderboardviewer;

import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class CategoryWidget extends Widget {
    private static final Identifier TAB_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/tableft.png");
    private static final Identifier TAB_MID = Identifier.of("wynnextras", "textures/gui/profileviewer/tabmid.png");
    private static final Identifier TAB_RIGHT = Identifier.of("wynnextras", "textures/gui/profileviewer/tabright.png");
    private static final Identifier TAB_LEFT_DARK = Identifier.of("wynnextras", "textures/gui/profileviewer/tableft_dark.png");
    private static final Identifier TAB_MID_DARK = Identifier.of("wynnextras", "textures/gui/profileviewer/tabmid_dark.png");
    private static final Identifier TAB_RIGHT_DARK = Identifier.of("wynnextras", "textures/gui/profileviewer/tabright_dark.png");

    private final LeaderboardCategory category;
    private final Consumer<CategoryWidget> onSelected;
    private boolean active;

    public CategoryWidget(LeaderboardCategory category, Consumer<CategoryWidget> onSelected) {
        super(0, 0, 0, 0);
        this.category = category;
        this.onSelected = onSelected;
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        int middleSegments = Math.max(0, (width - 60) / 30);
        PVScreen.DarkModeToggleWidget.drawImageWithFade(TAB_LEFT_DARK, TAB_LEFT, x, y, 30, 60, ui);
        for (int i = 0; i < middleSegments; i++) {
            PVScreen.DarkModeToggleWidget.drawImageWithFade(TAB_MID_DARK, TAB_MID, x + 30 * (i + 1), y, 30, 60, ui);
        }
        PVScreen.DarkModeToggleWidget.drawImageWithFade(
                TAB_RIGHT_DARK, TAB_RIGHT, x + 30 * (middleSegments + 1), y, 30, 60, ui);
        ui.drawCenteredText(category.displayName(), x + width / 2f, y + 6 + height / 2f,
                (hovered || active)
                        ? CustomColor.fromHexString("FFFF00") : CustomColor.fromHexString("FFFFFF"));
    }

    @Override
    protected boolean onClick(int button) {
        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        onSelected.accept(this);
        return true;
    }

    public int getPreferredWidth() {
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(category.displayName()) + 10;
        return 60 + Math.max(0, Math.ceilDiv(textWidth - 15, 10)) * 30;
    }

    public String getName() {
        return category.displayName();
    }

    public LeaderboardCategory getCategory() {
        return category;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
