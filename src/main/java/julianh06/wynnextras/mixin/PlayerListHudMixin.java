package julianh06.wynnextras.mixin;

import julianh06.wynnextras.wynncraft.state.StatusEffectState;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "setFooter", at = @At("TAIL"))
    private void onSetFooter(Text footer, CallbackInfo ci) {
        StatusEffectState.update(footer);
    }

    @Inject(method = "clear", at = @At("TAIL"))
    private void onClear(CallbackInfo ci) {
        StatusEffectState.clear();
    }
}
