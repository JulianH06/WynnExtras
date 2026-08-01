package julianh06.wynnextras.mixin.BankOverlay;

import com.wynntils.features.ui.ContainerScrollFeature;
import com.wynntils.mc.event.MouseScrollEvent;
import com.wynntils.models.containers.type.ScrollableContainerProperty;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import julianh06.wynnextras.features.wci.ui.WciShoppingMenuExtension;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin (ContainerScrollFeature.class)
public class ContainerScrollFeatureMixin {
    @Inject(method = "onInteract", at = @At("HEAD"), cancellable = true)
    private void blockWciPanelScroll(MouseScrollEvent event, CallbackInfo ci) {
        double verticalAmount = event.isScrollingUp() ? 1 : -1;
        if (!WciShoppingMenuExtension.handleGlobalMouseScrolled(verticalAmount)) return;
        event.setCanceled(true);
        ci.cancel();
    }

    @Redirect(
            method = "onInteract",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/wynntils/models/containers/type/ScrollableContainerProperty;getScrollButton(Lnet/minecraft/client/gui/screen/ingame/HandledScreen;Z)Ljava/util/Optional;"
            )
    )
    public Optional<Integer> getScrollButton(ScrollableContainerProperty instance, HandledScreen<?> screen, boolean previousPage) {
        if(BankOverlay.currentOverlayType != BankOverlayType.NONE) {
            return Optional.empty();
        }
        return instance.getScrollButton(screen, previousPage);
    }
}
