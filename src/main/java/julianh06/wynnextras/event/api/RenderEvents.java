package julianh06.wynnextras.event.api;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.RenderWorldEvent;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

@WEModule
public class RenderEvents {
    public RenderEvents() {
        WorldRenderEvents.AFTER_ENTITIES.register(event -> {
            VertexConsumerProvider vertexConsumers = event.consumers();
            if (!(vertexConsumers instanceof VertexConsumerProvider.Immediate immediateVertexConsumers)) return;

            MatrixStack stack = event.matrices();

            new RenderWorldEvent(stack, event.gameRenderer().getCamera(), immediateVertexConsumers, event.worldState().time).post();
        });
    }
}
