package julianh06.wynnextras.mixin;

import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.wtshim.models.containers.containers.CraftingStationContainer;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
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
            // Check cheap static field first to avoid expensive getCurrentContainer() call
            if(BankOverlay.currentOverlayType != BankOverlayType.NONE && WynnExtrasConfig.INSTANCE.toggleBankOverlay) {
                if (BankOverlay2.shouldShowWynntilsPageJumpButtons()) return;
                ci.cancel();
                return;
            }
            if(WynnExtrasConfig.INSTANCE.craftingHelperOverlay && Models.Container.getCurrentContainer() instanceof CraftingStationContainer) ci.cancel();
        } catch (Exception ignored) {}
    }
}
