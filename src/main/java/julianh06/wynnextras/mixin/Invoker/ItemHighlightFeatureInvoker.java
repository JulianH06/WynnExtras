package julianh06.wynnextras.mixin.Invoker;

import julianh06.wynnextras.wtshim.features.inventory.ItemHighlightFeature;
import julianh06.wynnextras.wtshim.utils.colors.CustomColor;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin (value = ItemHighlightFeature.class, remap = false)
public interface ItemHighlightFeatureInvoker {
    @Invoker(value = "getHighlightColor", remap = false)
    CustomColor invokeGetHighlightColor(ItemStack itemStack, boolean hotbarHighlight);
}
