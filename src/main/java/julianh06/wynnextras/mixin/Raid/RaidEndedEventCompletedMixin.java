package julianh06.wynnextras.mixin.Raid;

import com.wynntils.models.raid.event.RaidEndedEvent;
import com.wynntils.models.raid.type.RaidInfo;
import julianh06.wynnextras.features.chat.RaidChatNotifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RaidEndedEvent.Completed.class)
public class RaidEndedEventCompletedMixin {
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    public void ended (RaidInfo raidInfo, CallbackInfo ci) {
        RaidChatNotifier.onRoomCompleted(raidInfo);
        new julianh06.wynnextras.event.RaidEndedEvent.Completed(raidInfo).post();
    }
}
