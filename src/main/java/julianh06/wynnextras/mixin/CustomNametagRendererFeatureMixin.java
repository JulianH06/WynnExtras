package julianh06.wynnextras.mixin;

import com.wynntils.features.players.CustomNametagRendererFeature;
import com.wynntils.mc.event.PlayerNametagRenderEvent;
import julianh06.wynnextras.features.badges.BadgeService;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomNametagRendererFeature.class)
public class CustomNametagRendererFeatureMixin {
    @Inject(method = "onPlayerNameTagRender", at = @At("HEAD"))
    private void appendWynnExtrasBadge(PlayerNametagRenderEvent event, CallbackInfo ci) {
        EntityRenderState state = event.getEntityRenderState();
        state.displayName = BadgeService.appendBadge(state, state.displayName);
    }
}
