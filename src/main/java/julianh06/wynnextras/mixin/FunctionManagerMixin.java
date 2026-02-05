package julianh06.wynnextras.mixin;

import com.wynntils.core.consumers.functions.Function;
import com.wynntils.core.consumers.functions.FunctionManager;
import julianh06.wynnextras.features.misc.HealthRegenTick;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(FunctionManager.class)
public class FunctionManagerMixin {
    @Shadow @Final private List<Function<?>> functions;

    @Inject(method = "registerAllFunctions", at = @At("HEAD"), remap = false)
    private void registerFunctions(CallbackInfo ci) {
        this.functions.add(new HealthRegenTick.NextHealthRegenTickFunction());
    }
}
