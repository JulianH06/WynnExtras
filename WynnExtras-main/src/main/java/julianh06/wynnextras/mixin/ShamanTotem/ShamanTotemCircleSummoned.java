package julianh06.wynnextras.mixin.ShamanTotem;

import com.wynntils.models.abilities.event.TotemEvent;
import julianh06.wynnextras.features.misc.ShamanTotemCircle;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TotemEvent.Summoned.class)
public class ShamanTotemCircleSummoned {
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    public void summoned(int totemNumber, ArmorStandEntity totemEntity, CallbackInfo ci) {
        //System.out.println("activated totem " + totemNumber +  " at: " + position.toString());
        ShamanTotemCircle.totemPositions.remove(totemNumber);
    }
}
