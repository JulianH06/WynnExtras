package julianh06.wynnextras.mixin.compat.wynntils;

import julianh06.wynnextras.features.inventory.ScaleBackgroundRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces Wynntils' rarity highlight only for the slot receiving a scale background. */
@Pseudo
@Mixin(targets = "com.wynntils.features.inventory.ItemHighlightFeature", remap = false)
public abstract class WynntilsItemHighlightFeatureMixin {
    @Inject(method = "onRenderSlot", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void cancelRarityHighlightForScaleBackground(@Coerce Object event, CallbackInfo ci) {
        if (!(event instanceof WynntilsSlotRenderEventAccessor accessor)) return;
        Slot slot = accessor.wynnextras$getSlot();
        ItemStack stack = slot == null ? ItemStack.EMPTY : slot.getStack();
        if (ScaleBackgroundRenderState.resolve(stack) != null) {
            ScaleBackgroundRenderState.begin(stack);
            ci.cancel();
        }
    }
}
