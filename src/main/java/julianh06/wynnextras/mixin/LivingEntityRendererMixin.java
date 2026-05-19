package julianh06.wynnextras.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.wynntils.mc.extension.EntityRenderStateExtension;
import julianh06.wynnextras.features.misc.HuntedModeTracker;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.render.PlayerRenderFilter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

    @ModifyExpressionValue(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    private Entity showOwnNameTag(Entity entity) {
        if(entity == MinecraftClient.getInstance().player && WynnExtrasConfig.INSTANCE.showOwnNametag) return null;

        return entity;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"))
    private void beforeSuperRender(S state, MatrixStack matrices, OrderedRenderCommandQueue renderQueue, CameraRenderState camera, CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState playerState)) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || playerState.id != mc.player.getId()) return;
        if (!WynnExtrasConfig.INSTANCE.showOwnNametag) return;
        if (!HuntedModeTracker.huntedMode) return;

        Text pvpPrefix = Text.literal("[PVP] ").formatted(Formatting.RED);
        if (playerState.playerName != null) {
            playerState.playerName = pvpPrefix.copy().append(playerState.playerName);
        }
        if (playerState.displayName != null) {
            playerState.displayName = pvpPrefix.copy().append(playerState.displayName);
        }
    }
}