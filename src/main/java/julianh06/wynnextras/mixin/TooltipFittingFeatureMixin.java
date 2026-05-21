package julianh06.wynnextras.mixin;

import com.wynntils.features.tooltips.TooltipFittingFeature;
import com.wynntils.utils.mc.TooltipUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.inventory.WeightDisplay;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
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
        var currentHoveredStack = WeightDisplay.getCurrentHoveredStack();
        if (!WynnExtrasConfig.INSTANCE.showWeight || currentHoveredStack == null)
            return TooltipUtils.getClientTooltipComponent(components);

        if (!WeightDisplay.isTrackedMythic(currentHoveredStack))
            return TooltipUtils.getClientTooltipComponent(components);

        String cleanName = WeightDisplay.extractCleanName(currentHoveredStack);
        WeightDisplay.ItemData scaleData = WeightDisplay.weightCacheByHash.get(currentHoveredStack.getComponents().hashCode());
        WeightDisplay.ItemData itemData = WeightDisplay.itemCache.get(cleanName);
        if (scaleData == null || scaleData.data().isEmpty() || itemData == null) return TooltipUtils.getClientTooltipComponent(components);

        int idx = Math.min(itemData.index(), scaleData.data().size() - 1);

        List<Text> expanded = new ArrayList<>(components);
        for (int i = 0; i < scaleData.data().size(); i++) expanded.add(Text.empty());
        if (scaleData.data().size() > 1) expanded.add(Text.empty());
        // per-stat weight lines
        if (WynnExtrasConfig.INSTANCE.showScales) {
            WeightDisplay.WeightData profile = itemData.data().get(idx);
            for (Text line : components) {
                String[] parts = WeightDisplay.extractStatFromLine(line.getString());
                if (parts == null) continue;
                String apiName = WeightDisplay.resolveIdentKey(parts[0], parts[1])[0];
                if (profile.identifications().getOrDefault(apiName, 0f) != 0f) expanded.add(Text.empty());
            }
        }
        return TooltipUtils.getClientTooltipComponent(expanded);

        //this is still not perfect but better than the tooltip being cut off
    }
}
