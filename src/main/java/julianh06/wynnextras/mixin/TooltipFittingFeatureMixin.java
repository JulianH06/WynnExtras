package julianh06.wynnextras.mixin;

import com.wynntils.features.tooltips.TooltipFittingFeature;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.WynnItem;
import com.wynntils.utils.mc.TooltipUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.inventory.WeightDisplay;
import julianh06.wynnextras.utils.ItemUtils;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin (value = TooltipFittingFeature.class, remap = false)
public class TooltipFittingFeatureMixin {
    @Redirect(
            method = "onTooltipPre",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/wynntils/utils/mc/TooltipUtils;getClientTooltipComponent(Ljava/util/List;)Ljava/util/List;"
            )
    )
    private List<TooltipComponent> redirectGetClientTooltipComponent(List<Text> components) {
        if (!SimpleConfig.getInstance(WynnExtrasConfig.class).showWeight) {
            return TooltipUtils.getClientTooltipComponent(components);
        }

        if (!ItemUtils.isTier(WeightDisplay.currentHoveredStack, GearTier.MYTHIC)) {
            return TooltipUtils.getClientTooltipComponent(components);
        }
        List<Text> modified = WeightDisplay.getWynnItemTooltipWithScale(WeightDisplay.currentHoveredStack, WeightDisplay.currentHoveredWynnitem);
        return TooltipUtils.getClientTooltipComponent(modified);

    }
}
