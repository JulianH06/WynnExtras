package julianh06.wynnextras.mixin;

import com.wynntils.core.components.Models;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PressableWidget.class)
public class PressableWidgetMixin {
    @Inject(method = "renderWidget", at = @At(value = "HEAD"), cancellable = true)
    void renderWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            if((Models.Container.getCurrentContainer() instanceof CraftingStationContainer) && SimpleConfig.getInstance(WynnExtrasConfig.class).craftingHelperOverlay) ci.cancel();
        } catch (Exception ignored) {}
    }
}
