package julianh06.wynnextras.compat.wynntils.mixin;

import julianh06.wynnextras.compat.wynntils.WynntilsFunctionAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.wynntils.core.consumers.functions.FunctionManager", remap = false)
public class FunctionManagerMixin {
    @Inject(method = "registerAllFunctions()V", at = @At("TAIL"), remap = false, require = 0)
    private void registerWEFunctions(CallbackInfo ci) {
        WynntilsFunctionAdapter.registerRaidFunction(this);
    }
}
