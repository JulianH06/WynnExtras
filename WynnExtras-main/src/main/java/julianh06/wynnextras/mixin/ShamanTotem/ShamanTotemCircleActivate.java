package julianh06.wynnextras.mixin.ShamanTotem;

import com.wynntils.core.components.Models;
import com.wynntils.models.abilities.ShamanTotemModel;
import com.wynntils.models.abilities.event.TotemEvent;
import com.wynntils.models.abilities.type.ShamanMaskType;
import julianh06.wynnextras.features.misc.ShamanTotemCircle;
import julianh06.wynnextras.utils.WEVec;
import net.minecraft.util.math.Position;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin (TotemEvent.Activated.class)
public class ShamanTotemCircleActivate {
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    public void activated(int totemNumber, Position position, CallbackInfo ci) {
        if(Models.ShamanMask.getCurrentMaskType() == ShamanMaskType.FANATIC) return;
        WEVec vec = new WEVec(
                position.getX(),
                Math.floor(position.getY()) + 0.5f,
                position.getZ()
        );
        System.out.println("activated totem " + totemNumber +  " at: " + vec.toString());
        ShamanTotemCircle.totemPositions.put(totemNumber, vec);
    }
}

