package julianh06.wynnextras.mixin.Raid;

import julianh06.wynnextras.wtshim.core.components.Handlers;
import julianh06.wynnextras.wtshim.models.raid.event.RaidStartedEvent;
import julianh06.wynnextras.wtshim.models.raid.raids.RaidKind;
import julianh06.wynnextras.features.misc.PlayerHider;
import julianh06.wynnextras.features.chat.RaidChatNotifier;
import julianh06.wynnextras.features.raid.PartyIgnoreOnRaid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RaidStartedEvent.class)
public class RaidStartEventMixin {
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    public void started (RaidKind raidKind, CallbackInfo ci) {
        Handlers.Command.queueCommand("party list");
        PlayerHider.onRaidStarted(raidKind);
        RaidChatNotifier.resetCounters();
        PartyIgnoreOnRaid.onRaidStarted();
    }
}
