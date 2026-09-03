package julianh06.wynnextras.compat.wynntils.mixin;

import julianh06.wynnextras.compat.wynntils.WynntilsTooltipAdapter;
import julianh06.wynnextras.features.inventory.WeightDisplay;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "com.wynntils.utils.mc.TooltipUtils", remap = false)
public class TooltipUtilsMixin {
    @Inject(method = "getWynnItemTooltip", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void appendChatItemWeights(ItemStack stack, @Coerce Object item,
                                              CallbackInfoReturnable<List<Text>> cir) {
        String itemName = WynntilsTooltipAdapter.getSharedMythicName(stack, item);
        if (itemName == null) return;
        cir.setReturnValue(WeightDisplay.appendChatItemAnnotations(itemName, cir.getReturnValue()));
    }
}
