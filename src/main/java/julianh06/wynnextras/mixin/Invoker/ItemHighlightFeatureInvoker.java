package julianh06.wynnextras.mixin.Invoker;

import com.wynntils.features.inventory.ItemHighlightFeature;
import com.wynntils.utils.colors.CustomColor;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin (value = ItemHighlightFeature.class, remap = false)
public interface ItemHighlightFeatureInvoker {
    @Invoker(value = "getHighlightColor", remap = false)
    CustomColor invokeGetHighlightColor(ItemStack itemStack, boolean hotbarHighlight);
}
