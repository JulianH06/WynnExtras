package julianh06.wynnextras.mixin.Invoker;

import julianh06.wynnextras.wtshim.features.inventory.UnidentifiedItemIconFeature;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin (value = UnidentifiedItemIconFeature.class, remap = false)
public interface UnidentifiedItemIconFeatureInvoker {
    @Invoker(value = "drawIcon", remap = false)
    void invokeDrawIcon(DrawContext context, ItemStack itemStack, int slotX, int slotY, int z);
}
