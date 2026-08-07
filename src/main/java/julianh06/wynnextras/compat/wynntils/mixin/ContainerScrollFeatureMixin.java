package julianh06.wynnextras.compat.wynntils.mixin;

import julianh06.wynnextras.compat.wynntils.WynntilsBankAdapter;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Pseudo
@Mixin(targets = "com.wynntils.features.ui.ContainerScrollFeature", remap = false)
public class ContainerScrollFeatureMixin {
    @Redirect(
            method = "onInteract",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/wynntils/models/containers/type/ScrollableContainerProperty;getScrollButton(Lnet/minecraft/client/gui/screen/ingame/HandledScreen;Z)Ljava/util/Optional;"
            ),
            require = 0
    )
    public Optional<Integer> getScrollButton(@Coerce Object instance, HandledScreen<?> screen, boolean previousPage) {
        if(BankOverlay.currentOverlayType != BankOverlayType.NONE) {
            return Optional.empty();
        }
        return WynntilsBankAdapter.scrollButton(instance, screen, previousPage);
    }
}
