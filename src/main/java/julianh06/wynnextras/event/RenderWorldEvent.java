package julianh06.wynnextras.event;

import julianh06.wynnextras.event.api.WEEvent;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public final class RenderWorldEvent extends WEEvent {
    public MatrixStack matrices;
    public Camera camera;
    public VertexConsumerProvider.Immediate vertexConsumerProvider;
    public float partialTicks;
    public boolean isCurrentlyDeferring = true;

    public RenderWorldEvent(MatrixStack matrices, Camera camera, VertexConsumerProvider.Immediate vertexConsumerProvider, float partialTicks) {
        this.matrices = matrices;
        this.camera = camera;
        this.vertexConsumerProvider = vertexConsumerProvider;
        this.partialTicks = partialTicks;
    }
}
