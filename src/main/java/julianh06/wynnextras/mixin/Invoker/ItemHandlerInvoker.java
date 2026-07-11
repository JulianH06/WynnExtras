package julianh06.wynnextras.mixin.Invoker;

import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.handlers.item.ItemAnnotation;
import julianh06.wynnextras.wtshim.handlers.item.ItemHandler;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin (value = ItemHandler.class, remap = false)
public interface ItemHandlerInvoker {
    @Invoker(value = "calculateAnnotation", remap = false)
    ItemAnnotation invokeCalculateAnnotation(ItemStack itemStack, StyledText name);
}
