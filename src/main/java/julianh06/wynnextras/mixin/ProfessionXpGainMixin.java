package julianh06.wynnextras.mixin;

import com.wynntils.models.profession.event.ProfessionXpGainEvent;
import com.wynntils.models.profession.type.ProfessionType;
import julianh06.wynnextras.features.misc.ProfessionOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProfessionXpGainEvent.class)
public class ProfessionXpGainMixin {
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    public void onXpGain(ProfessionType profession, float gainedXpRaw, float currentXpPercentage, CallbackInfo ci) {
        ProfessionOverlay.onXpGain(profession, gainedXpRaw);
    }
}
