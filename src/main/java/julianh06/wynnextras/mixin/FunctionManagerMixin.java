package julianh06.wynnextras.mixin;

import com.wynntils.core.consumers.functions.Function;
import com.wynntils.core.consumers.functions.FunctionManager;
import julianh06.wynnextras.functions.RaidFunctions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FunctionManager.class, remap = false)
public abstract class FunctionManagerMixin {
    @Shadow
    protected abstract void registerFunction(Function<?> function);

    @Inject(method = "registerAllFunctions()V", at = @At("TAIL"))
    private void registerWEFunctions(CallbackInfo ci){
        registerFunction(new RaidFunctions.RaidDropFunction());
    }
}
