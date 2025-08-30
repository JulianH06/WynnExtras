package julianh06.wynnextras.mixin.Invoker;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemHandler;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin (ItemHandler.class)
public interface ItemHandlerInvoker {
    @Invoker("calculateAnnotation")
    ItemAnnotation invokeCalculateAnnotation(ItemStack itemStack, StyledText name);
}
