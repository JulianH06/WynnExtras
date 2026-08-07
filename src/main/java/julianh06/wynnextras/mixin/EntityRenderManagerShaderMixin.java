package julianh06.wynnextras.mixin;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.duck.EntityRenderStateAccess;
import julianh06.wynnextras.features.misc.CurseTracker;
import julianh06.wynnextras.utils.EntityShader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderManager.class)
public class EntityRenderManagerShaderMixin {
    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/render/state/CameraRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V",
        at = @At("HEAD")
    )
    private <S extends EntityRenderState> void we$beforeRender(S state, CameraRenderState camera, double x, double y, double z, MatrixStack matrices, OrderedRenderCommandQueue queue, CallbackInfo ci) {
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
        if (!c.curseTrackerEnabled || !c.curseTrackerColorMobs) return;
        if (!(state instanceof EntityRenderStateAccess access)) return;
        Entity entity = access.wynnExtras$getEntity();
        if (entity == null) return;
        if (entity == MinecraftClient.getInstance().player) return;
        if (entity instanceof PlayerEntity) return;
        if (!(entity instanceof LivingEntity)) return;
        if (CurseTracker.cursedEntityIds.contains(entity.getId())) {
            EntityShader.activeShader = 0xFF000000 | c.curseTrackerMobColor.getRGB();
        }
    }

    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/render/state/CameraRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V",
        at = @At("RETURN")
    )
    private <S extends EntityRenderState> void we$afterRender(S state, CameraRenderState camera, double x, double y, double z, MatrixStack matrices, OrderedRenderCommandQueue queue, CallbackInfo ci) {
        EntityShader.activeShader = null;
    }
}
