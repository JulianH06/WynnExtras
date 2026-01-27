package julianh06.wynnextras.features.guildviewer;

import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.util.DyeColor;

import javax.annotation.Nullable;

public record BannerGuiElementState(
        BannerFlagBlockModel flag,
        DyeColor baseColor,
        BannerPatternsComponent resultBannerPatterns,
        int x1,
        int y1,
        int x2,
        int y2,
        @Nullable ScreenRect scissorArea,
        @Nullable ScreenRect bounds,
        float scale
) implements SpecialGuiElementRenderState {
    public BannerGuiElementState(
            BannerFlagBlockModel bannerFlagBlockModel,
            DyeColor color,
            BannerPatternsComponent bannerPatterns,
            int x1,
            int y1,
            int x2,
            int y2,
            @Nullable ScreenRect scissorArea,
            float scale
    ) {
        this(bannerFlagBlockModel, color, bannerPatterns, x1, y1, x2, y2, scissorArea, SpecialGuiElementRenderState.createBounds(x1, y1, x2, y2, scissorArea), scale);
    }
}