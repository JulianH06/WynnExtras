package julianh06.wynnextras.mixin.BankOverlay;

import com.wynntils.features.inventory.PersonalStorageUtilitiesFeature;
import julianh06.wynnextras.features.inventory.BankOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin (PersonalStorageUtilitiesFeature.class)
public class PersonalStorageUtilitiesFeatureMixin {
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    void onInit(CallbackInfo ci) {
        BankOverlay.PersonalStorageUtils = (PersonalStorageUtilitiesFeature) (Object) this;
    }
}
