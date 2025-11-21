package julianh06.wynnextras.event.api;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.RenderWorldEvent;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

@WEModule
public class RenderEvents {
    public RenderEvents() {
        WorldRenderEvents.AFTER_ENTITIES.register(event -> {
            VertexConsumerProvider vertexConsumers = event.consumers();
            if (!(vertexConsumers instanceof VertexConsumerProvider.Immediate immediateVertexConsumers)) return;

            MatrixStack stack = event.matrixStack() != null ? event.matrixStack() : new MatrixStack();

            new RenderWorldEvent(stack, event.camera(), immediateVertexConsumers, event.tickCounter().getTickDelta(true)).post();
        });
    }
}
