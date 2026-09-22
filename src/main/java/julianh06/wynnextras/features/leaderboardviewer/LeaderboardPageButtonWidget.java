package julianh06.wynnextras.features.leaderboardviewer;

import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

import java.util.function.IntConsumer;

public class LeaderboardPageButtonWidget extends Widget {
    private final int page;
    private final IntConsumer onSelected;
    private boolean active;

    public LeaderboardPageButtonWidget(int page, IntConsumer onSelected) {
        this.page = page;
        this.onSelected = onSelected;
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        ui.drawFixedVanillaPanelButtonFade(x, y, width, height, 10, 2, hovered || active);
        ui.drawCenteredText(Integer.toString(page), x + width / 2f, y + height / 2f,
                active ? CustomColor.fromHexString("FFFF00") : CustomColor.fromHexString("FFFFFF"), 2.4f);
    }

    @Override
    protected boolean onClick(int button) {
        if (active) return false;
        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        onSelected.accept(page);
        return true;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
