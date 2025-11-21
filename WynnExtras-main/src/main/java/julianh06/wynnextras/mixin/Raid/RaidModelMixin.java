package julianh06.wynnextras.mixin.Raid;

import com.wynntils.core.WynntilsMod;
import com.wynntils.models.raid.RaidModel;
import com.wynntils.models.raid.event.RaidEndedEvent;
import com.wynntils.models.raid.type.RaidInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin (value = RaidModel.class, remap = false)
public class RaidModelMixin {
    @Shadow private RaidInfo currentRaid;

    @Inject(method = "failedRaid", at = @At("HEAD"), remap = false)
    public void failedRaid(CallbackInfo ci) {
        if(currentRaid != null) new julianh06.wynnextras.event.RaidEndedEvent(this.currentRaid).post();
    }
}
