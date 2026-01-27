package julianh06.wynnextras.mixin.ShamanTotem;

import com.wynntils.core.components.Models;
import com.wynntils.handlers.labels.event.TextDisplayChangedEvent;
import com.wynntils.mc.event.AddEntityEvent;
import com.wynntils.mc.event.EntityEvent;
import com.wynntils.models.abilities.event.TotemEvent;
import com.wynntils.models.abilities.type.ShamanMaskType;
import julianh06.wynnextras.features.misc.ShamanTotemCircle;
import julianh06.wynnextras.utils.WEVec;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.util.math.Position;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TotemEvent.Updated.class)
public class ShamanTotemCircleUpdated {
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    public void updated(int totemNumber, int time, Position position, CallbackInfo ci) {
        WEVec vec = new WEVec(
                position.getX(),
                Math.floor(position.getY()) + 0.5f,
                position.getZ()
        );
        //System.out.println("updated totem " + totemNumber +  " at: " + vec.toString() + " remaining time " + time);
        if(time <= 1) {
            ShamanTotemCircle.totemPositions.remove(totemNumber);
        } else {
            ShamanTotemCircle.totemPositions.put(totemNumber, vec);
        }
    }
}
