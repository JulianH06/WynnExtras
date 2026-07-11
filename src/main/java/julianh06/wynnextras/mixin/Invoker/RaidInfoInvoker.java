package julianh06.wynnextras.mixin.Invoker;

import julianh06.wynnextras.wtshim.models.raid.type.RaidInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin (value = RaidInfo.class)
public interface RaidInfoInvoker {
    @Invoker(value = "getTimeInRooms", remap = false)
    long invokeGetTimeInRooms();
}
