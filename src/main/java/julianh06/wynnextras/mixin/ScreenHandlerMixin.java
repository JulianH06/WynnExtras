package julianh06.wynnextras.mixin;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.aspects.maintracking;
import julianh06.wynnextras.features.misc.FastRequeue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {
    @Inject(method = "onSlotClick", at = @At("HEAD"))
    private void onMouseClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if(slotIndex == 4) {
            //maintracking.setRaiddone(true);
            if(WynnExtrasConfig.INSTANCE.toggleFastRequeue) FastRequeue.notifyClick();
        }
    }
}