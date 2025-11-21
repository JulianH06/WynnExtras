package julianh06.wynnextras.mixin;


import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Perspective.class)
public class PerspectiveMixin {
    @Inject(method = "next", at = @At("HEAD"), cancellable = true)
    private void next(CallbackInfoReturnable<Perspective> cir) {
        if (!SimpleConfig.getInstance(WynnExtrasConfig.class).removeFrontPersonView) {
            return;
        }
        Perspective self = (Perspective)(Object)this;

        if (self == Perspective.THIRD_PERSON_BACK) {
            cir.setReturnValue(Perspective.FIRST_PERSON);
        }
    }
}
