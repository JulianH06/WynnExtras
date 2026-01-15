package julianh06.wynnextras.mixin;

import com.wynntils.core.components.Models;
import com.wynntils.features.ui.ProfessionHighlightFeature;
import com.wynntils.mc.event.ScreenInitEvent;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProfessionHighlightFeature.class)
public class ProfessionHighlightFeatureMixin {
    @Inject(method = "onScreenInit", at = @At(value = "HEAD"), cancellable = true, remap = false)
    void onScreenInit(ScreenInitEvent.Pre event, CallbackInfo ci) {
        if((Models.Container.getCurrentContainer() instanceof CraftingStationContainer) && SimpleConfig.getInstance(WynnExtrasConfig.class).craftingHelperOverlay) ci.cancel();
    }
}
