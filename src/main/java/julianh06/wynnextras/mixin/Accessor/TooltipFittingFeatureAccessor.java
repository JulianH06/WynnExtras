package julianh06.wynnextras.mixin.Accessor;

import com.wynntils.core.persisted.config.Config;
import com.wynntils.features.tooltips.TooltipFittingFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin (value = TooltipFittingFeature.class, remap = false)
public interface TooltipFittingFeatureAccessor {
    @Accessor("wrapText")
    Config<Boolean> getWrapText();

    @Accessor("fitToScreen")
    Config<Boolean> getFitToScreen();
}
