package julianh06.wynnextras.mixin;


import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.badges.BadgeService;
import julianh06.wynnextras.duck.EntityRenderStateAccess;
import julianh06.wynnextras.utils.EntityVisibility;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRenderMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir){
        if (EntityVisibility.isHidden(entity)) {
            cir.setReturnValue(false);
            return;
        }
        if(WynnExtrasConfig.INSTANCE.arrowHiderToggle && entity.getType().equals(EntityType.ARROW)){
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void storeEntity(T entity, S state, float tickDelta, CallbackInfo ci) {
        ((EntityRenderStateAccess) state).wynnExtras$setEntity(entity);
    }

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"))
    private void appendWynnExtrasBadge(S state, net.minecraft.client.util.math.MatrixStack matrices, net.minecraft.client.render.command.OrderedRenderCommandQueue renderQueue, net.minecraft.client.render.state.CameraRenderState camera, CallbackInfo ci) {
        state.displayName = BadgeService.appendBadge(state, state.displayName);
    }
}
