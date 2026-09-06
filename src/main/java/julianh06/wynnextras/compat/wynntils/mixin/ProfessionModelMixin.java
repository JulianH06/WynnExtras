package julianh06.wynnextras.compat.wynntils.mixin;

import julianh06.wynnextras.compat.wynntils.WynntilsProfessionAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.wynntils.models.profession.ProfessionModel", remap = false)
public class ProfessionModelMixin {
    @Inject(method = "onXpGain", at = @At("TAIL"), remap = false, require = 0)
    private void wynnExtras$observeXpGain(@Coerce Object event, CallbackInfo ci) {
        WynntilsProfessionAdapter.observeXpGain(event);
    }
}
