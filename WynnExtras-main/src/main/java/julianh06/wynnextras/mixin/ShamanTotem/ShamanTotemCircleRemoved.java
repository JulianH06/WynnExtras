package julianh06.wynnextras.mixin.ShamanTotem;

import com.wynntils.models.abilities.event.TotemEvent;
import com.wynntils.models.abilities.type.ShamanTotem;
import julianh06.wynnextras.features.misc.ShamanTotemCircle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin (TotemEvent.Removed.class) 
public class ShamanTotemCircleRemoved {
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    public void removed(int totemNumber, ShamanTotem totem, CallbackInfo ci) {
        ShamanTotemCircle.totemPositions.remove(totemNumber - 1);
        //System.out.println("removed totem " + totemNumber);
    }
}
