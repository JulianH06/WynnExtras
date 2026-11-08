package julianh06.wynnextras.mixin;

import julianh06.wynnextras.config.WynnExtrasConfig;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//hides the sidebar scoreboard when "Hide Scoreboard" is on (handy for proffing)
//only the drawing gets cancelled, the scoreboard data still comes in so stuff that reads it
//(like the attack timer) keeps working
@Mixin(InGameHud.class)
public class InGameHudScoreboardMixin {
    @Inject(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void wynnextras$hideScoreboard(net.minecraft.client.gui.DrawContext context,
                                           net.minecraft.client.render.RenderTickCounter tickCounter,
                                           CallbackInfo ci) {
        if (WynnExtrasConfig.INSTANCE.hideScoreboard) ci.cancel();
    }
}
