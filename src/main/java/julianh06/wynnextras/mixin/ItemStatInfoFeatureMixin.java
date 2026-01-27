package julianh06.wynnextras.mixin;

import com.wynntils.features.tooltips.ItemStatInfoFeature;
import com.wynntils.models.items.WynnItem;
import com.wynntils.utils.mc.TooltipUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

import static julianh06.wynnextras.features.inventory.WeightDisplay.*;

@Mixin(ItemStatInfoFeature.class)
public class ItemStatInfoFeatureMixin {
    @Redirect(
            method = "onTooltipPre",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/wynntils/utils/mc/TooltipUtils;getWynnItemTooltip(Lnet/minecraft/item/ItemStack;Lcom/wynntils/models/items/WynnItem;)Ljava/util/List;"
            )
    )
    private List<Text> redirectGetWynnItemTooltip(ItemStack itemStack, WynnItem wynnItem) {
        currentHoveredStack = itemStack;
        currentHoveredWynnitem = wynnItem;
        return TooltipUtils.getWynnItemTooltip(itemStack, wynnItem);
    }
}
