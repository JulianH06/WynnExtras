package julianh06.wynnextras.features.guildviewer;

import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;

public record BannerGuiElementState(
        ScreenRect bounds,
        float scale,
        BannerBlockEntity banner
) implements SpecialGuiElementRenderState {

    @Override
    public int x1() { return bounds.getLeft(); }

    @Override
    public int y1() { return bounds.getTop(); }

    @Override
    public int x2() { return bounds.getRight(); }

    @Override
    public int y2() { return bounds.getBottom(); }

    @Override
    public ScreenRect bounds() {
        return bounds;
    }

    @Override
    public ScreenRect scissorArea() {
        return null;
    }
}