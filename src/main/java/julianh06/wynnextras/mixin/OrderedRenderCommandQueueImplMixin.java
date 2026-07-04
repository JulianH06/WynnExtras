package julianh06.wynnextras.mixin;

import julianh06.wynnextras.features.badges.BadgeService;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(OrderedRenderCommandQueueImpl.class)
public class OrderedRenderCommandQueueImplMixin {
    @ModifyArg(
            method = "submitLabel(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;ILnet/minecraft/text/Text;ZIDLnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/BatchingRenderCommandQueue;submitLabel(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;ILnet/minecraft/text/Text;ZIDLnet/minecraft/client/render/state/CameraRenderState;)V"),
            index = 3)
    private Text appendWynnExtrasBadge(Text label) {
        return BadgeService.appendBadge(null, label);
    }
}