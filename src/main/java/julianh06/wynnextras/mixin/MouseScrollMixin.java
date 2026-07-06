package julianh06.wynnextras.mixin;

import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Mouse.class, priority = 13000)
public class MouseScrollMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (BankOverlay2.handleMouseScrolled(vertical)) {
            ci.cancel();
        }
    }
}