package julianh06.wynnextras.compat.wynntils.mixin;

import julianh06.wynnextras.compat.wynntils.WynntilsNametagAdapter;
import julianh06.wynnextras.features.badges.BadgeService;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.wynntils.features.players.CustomNametagRendererFeature", remap = false)
public class CustomNametagRendererFeatureMixin {
    @Inject(method = "onPlayerNameTagRender", at = @At("HEAD"), require = 0)
    private void appendWynnExtrasBadge(@Coerce Object event, CallbackInfo ci) {
        EntityRenderState state = WynntilsNametagAdapter.getEntityRenderState(event).orElse(null);
        if (state == null) return;
        state.displayName = BadgeService.appendBadge(state, state.displayName);
    }
}
