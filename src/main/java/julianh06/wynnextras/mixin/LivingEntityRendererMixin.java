package julianh06.wynnextras.mixin;

import com.wynntils.mc.extension.EntityRenderStateExtension;
import julianh06.wynnextras.features.render.PlayerRenderFilter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends EntityRenderer<T, S> implements FeatureRendererContext<S, M> {

    protected LivingEntityRendererMixin(EntityRendererFactory.Context context) {
        super(context);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(S state, MatrixStack matrices, OrderedRenderCommandQueue renderQueue, CameraRenderState camera, CallbackInfo ci) {    EntityRenderStateExtension entityRenderStateExtension = state instanceof EntityRenderStateExtension ? ((EntityRenderStateExtension) state) : null;
        if(entityRenderStateExtension != null) {
            PlayerEntity player = entityRenderStateExtension.getEntity() instanceof PlayerEntity ? ((PlayerEntity) entityRenderStateExtension.getEntity()) : null;
            if(player != null) {
                if (PlayerRenderFilter.isHidden(player)) {
                    ci.cancel();
                }
            }
        }
    }
}