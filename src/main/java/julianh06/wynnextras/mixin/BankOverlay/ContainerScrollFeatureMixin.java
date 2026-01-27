package julianh06.wynnextras.mixin.BankOverlay;

import com.wynntils.features.ui.ContainerScrollFeature;
import com.wynntils.models.containers.type.ScrollableContainerProperty;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin (ContainerScrollFeature.class)
public class ContainerScrollFeatureMixin {
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
