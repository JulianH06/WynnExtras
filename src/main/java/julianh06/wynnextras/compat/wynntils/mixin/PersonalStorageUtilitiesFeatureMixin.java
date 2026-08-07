package julianh06.wynnextras.compat.wynntils.mixin;

import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.compat.wynntils.WynntilsBankAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.wynntils.features.inventory.PersonalStorageUtilitiesFeature", remap = false)
public class PersonalStorageUtilitiesFeatureMixin {
    @Inject(method = "<init>", at = @At("TAIL"), remap = false, require = 0)
    void onInit(CallbackInfo ci) {
        BankOverlay.setPersonalStorageUtils(WynntilsBankAdapter.storage(this));
    }
}
