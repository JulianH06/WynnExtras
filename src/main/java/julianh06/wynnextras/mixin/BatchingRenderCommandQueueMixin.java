package julianh06.wynnextras.mixin;

import julianh06.wynnextras.utils.EntityShader;
import julianh06.wynnextras.utils.ShaderVertexConsumer;
import net.minecraft.client.render.command.BatchingRenderCommandQueue;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

@Mixin(BatchingRenderCommandQueue.class)
public class BatchingRenderCommandQueueMixin {
    @ModifyArgs(
        method = "submitItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl$ItemCommand;<init>(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V"
        )
    )
    private void we$tintItem(Args args) {
        Integer shader = EntityShader.activeShader;
        if (shader == null) return;
        List<BakedQuad> quads = args.get(6);
        if (quads == null || quads.isEmpty()) return;
        int[] tintLayers = args.get(5);
        args.set(5, EntityShader.mixedTintLayers(tintLayers, quads, shader));
        args.set(6, EntityShader.quadsWithDefaultTintIndex(quads));
    }

    @ModifyArg(
        method = "submitModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl$ModelCommand;<init>(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/model/Model;Ljava/lang/Object;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
        ),
        index = 5
    )
    private int we$tintModel(int color) {
        Integer s = EntityShader.activeShader;
        return s != null ? s : color;
    }

    @ModifyArg(
        method = "submitModelPart",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl$ModelPartCommand;<init>(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/model/ModelPart;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V"
        ),
        index = 7
    )
    private int we$tintModelPart(int color) {
        Integer s = EntityShader.activeShader;
        return s != null ? s : color;
    }

    @ModifyArg(
        method = "submitCustom",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/CustomCommandRenderer$Commands;add(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue$Custom;)V"
        ),
        index = 2
    )
    private OrderedRenderCommandQueue.Custom we$tintCustom(OrderedRenderCommandQueue.Custom original) {
        Integer shader = EntityShader.activeShader;
        if (shader == null) return original;
        return (entry, consumer) -> original.render(entry, ShaderVertexConsumer.wrap(consumer, shader));
    }
}
