package julianh06.wynnextras.mixin.Invoker;

import julianh06.wynnextras.wtshim.features.inventory.DurabilityOverlayFeature;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = DurabilityOverlayFeature.class, remap = false)
public interface DurabilityOverlayFeatureInvoker {
    @Invoker(value = "drawDurabilityArc", remap = false)
    void invokeDrawDurabilityArc(DrawContext context, ItemStack stack, int x, int y);

    @Invoker(value = "drawDurabilityBar", remap = false)
    void invokeDrawDurabilityBar(DrawContext context, ItemStack stack, int x, int y);

    @Invoker(value = "drawDurabilityPercentage", remap = false)
    void invokeDrawDurabilityPercentage(DrawContext context, ItemStack stack, int x, int y);
}
