// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Yarn adaptation of Wynntils' AbstractContainerScreenMixin (ContainerCloseEvent only).
 */
package julianh06.wynnextras.wtshim.fabric.mixin;

import julianh06.wynnextras.wtshim.core.events.MixinHelper;
import julianh06.wynnextras.wtshim.mc.event.ContainerCloseEvent;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenCloseMixin {
    @Inject(method = "close()V", at = @At("HEAD"), cancellable = true)
    private void onCloseContainerPre(CallbackInfo ci) {
        ContainerCloseEvent.Pre event = new ContainerCloseEvent.Pre();
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "close()V", at = @At("RETURN"))
    private void onCloseContainerPost(CallbackInfo ci) {
        MixinHelper.post(new ContainerCloseEvent.Post());
    }
}
