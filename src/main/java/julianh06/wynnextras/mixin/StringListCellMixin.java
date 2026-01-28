package julianh06.wynnextras.mixin;

import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.Nullable;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringListListEntry.StringListCell.class)
public class StringListCellMixin {
    @Inject(method = "substituteDefault", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectPlaceholder(@Nullable String value, CallbackInfoReturnable<String> cir) {
        if (value == null || value.isBlank()) {
            //cir.setReturnValue("<Type your Word/Phrase here>|<Your Notification Text here>");
            cir.setReturnValue("<Default>");
        }
    }
}
