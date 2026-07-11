// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Yarn adaptation of Wynntils' MultiPlayerGameModeMixin (ContainerClickEvent only).
 */
package julianh06.wynnextras.wtshim.fabric.mixin;

import julianh06.wynnextras.wtshim.core.events.MixinHelper;
import julianh06.wynnextras.wtshim.mc.event.ContainerClickEvent;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(
            method = "clickSlot(IIILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void clickSlotPre(
            int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (syncId != player.currentScreenHandler.syncId) return;

        ContainerClickEvent event =
                new ContainerClickEvent(player.currentScreenHandler, slotId, actionType, button);
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }
}
