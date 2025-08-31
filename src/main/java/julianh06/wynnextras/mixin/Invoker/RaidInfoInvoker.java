package julianh06.wynnextras.mixin.Invoker;

import com.wynntils.models.raid.type.RaidInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin (value = RaidInfo.class, remap = false)
public interface RaidInfoInvoker {
    @Invoker("getTimeInRooms")
    long invokeGetTimeInRooms();
}
