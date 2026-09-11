package julianh06.wynnextras.features.leaderboardviewer;

import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

public class CategoryWidget extends Widget {
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
        ui.drawCenteredText(category.displayName(), x + width / 2f, y + 4 + height / 2f,
                (hovered || active)
                        ? CustomColor.fromHexString("FFFF00") : CustomColor.fromHexString("FFFFFF"));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!hovered) return false;
        onSelected.accept(this);
        return true;
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
