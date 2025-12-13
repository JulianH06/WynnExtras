package julianh06.wynnextras.mixin.Invoker;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = com.wynnmod.feature.item.ItemOverlayFeature.class, remap = false)
public interface wmd$ItemOverlayFeatureInvoker {
    @Invoker(remap = false)
    boolean callOnRenderItem(DrawContext context, ItemStack stack, int x, int y, boolean hotbar);
}
