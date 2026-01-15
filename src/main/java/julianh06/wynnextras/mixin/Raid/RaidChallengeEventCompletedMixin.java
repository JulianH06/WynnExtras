package julianh06.wynnextras.mixin.Raid;

import com.wynntils.models.raid.event.RaidChallengeEvent;
import com.wynntils.models.raid.type.RaidInfo;
import julianh06.wynnextras.features.chat.RaidChatNotifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RaidChallengeEvent.Completed.class)
public class RaidChallengeEventCompletedMixin {
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    public void completed (RaidInfo raidInfo, CallbackInfo ci) {
        RaidChatNotifier.onRoomCompleted(raidInfo);
    }
}
