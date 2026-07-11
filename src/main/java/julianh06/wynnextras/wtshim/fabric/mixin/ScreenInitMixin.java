// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Yarn adaptation of Wynntils' ScreenMixin (ScreenInitEvent only; no TitleScreen
 * specials, no WynntilsScreen crash wrapping — the shim doesn't need them).
 * Mojmap rebuildWidgets() == Yarn clearAndInit().
 */
package julianh06.wynnextras.wtshim.fabric.mixin;

import julianh06.wynnextras.wtshim.core.events.MixinHelper;
import julianh06.wynnextras.wtshim.mc.event.ScreenInitEvent;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenInitMixin {
    @Inject(
            method = "init(II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;init()V"))
    private void onFirstScreenInitPre(int width, int height, CallbackInfo ci) {
        MixinHelper.post(new ScreenInitEvent.Pre((Screen) (Object) this, true));
    }

    @Inject(
            method = "init(II)V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/gui/screen/Screen;init()V",
                            shift = At.Shift.AFTER))
    private void onFirstScreenInitPost(int width, int height, CallbackInfo ci) {
        MixinHelper.post(new ScreenInitEvent.Post((Screen) (Object) this, true));
    }

    @Inject(
            method = "clearAndInit()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;init()V"))
    private void onScreenInitPre(CallbackInfo ci) {
        MixinHelper.post(new ScreenInitEvent.Pre((Screen) (Object) this, false));
    }

    @Inject(method = "clearAndInit()V", at = @At("RETURN"))
    private void onScreenInitPost(CallbackInfo ci) {
        MixinHelper.post(new ScreenInitEvent.Post((Screen) (Object) this, false));
    }
}
