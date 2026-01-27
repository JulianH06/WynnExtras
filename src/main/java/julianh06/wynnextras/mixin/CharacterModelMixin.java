package julianh06.wynnextras.mixin;

import com.wynntils.models.character.CharacterModel;
import com.wynntils.models.worlds.event.WorldStateEvent;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import julianh06.wynnextras.features.inventory.data.CharacterBankData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CharacterModel.class, remap = false)
public class CharacterModelMixin {
    @Shadow
    private String id;

    @Inject(
            method = "onWorldStateChanged",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/wynntils/models/character/CharacterModel;scanCharacterInfo()V",
                    shift = At.Shift.AFTER
            )
    )
    private void onWorldStateChanged(WorldStateEvent e, CallbackInfo ci) {
        String id = this.id;

        if (id == null || id.isEmpty() || id.equals("-")) {
            return;
        }

        BankOverlay.Pages = null;
        BankOverlay.currentData = null;
        BankOverlay.activeInvSlots.clear();
        BankOverlay.annotationCache.clear();
        BankOverlay.expectedOverlayType = BankOverlayType.NONE;

        BankOverlay.currentCharacterID = id;
        CharacterBankData.INSTANCE.load();
    }
}