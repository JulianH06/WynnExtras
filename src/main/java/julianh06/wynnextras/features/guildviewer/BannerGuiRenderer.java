package julianh06.wynnextras.features.guildviewer;

import julianh06.wynnextras.mixin.WorldRendererAccessor;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.BannerBlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class BannerGuiRenderer extends SpecialGuiElementRenderer<BannerGuiElementState> {

    public BannerGuiRenderer(VertexConsumerProvider.Immediate consumers) {
        super(consumers);
    }

    @Override
    protected void render(BannerGuiElementState element, MatrixStack matrices) {
        BannerBlockEntity banner = element.banner();

        BlockEntityRenderer<BannerBlockEntity, BannerBlockEntityRenderState> renderer =
                MinecraftClient.getInstance()
                        .getBlockEntityRenderDispatcher()
                        .get(banner);

        if (renderer == null) return;

        float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);

        BannerBlockEntityRenderState state = renderer.createRenderState();

        renderer.updateRenderState(banner, state, tickDelta, Vec3d.ZERO, null);

        state.standing = true;
        state.yaw = 180f;

        matrices.push();
        matrices.translate(0, 0, 100);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));

        OrderedRenderCommandQueue queue =
                MinecraftClient.getInstance()
                        .gameRenderer
                        .getEntityRenderDispatcher()
                        .getQueue();

        CameraRenderState camera = ((WorldRendererAccessor) MinecraftClient.getInstance().worldRenderer).getWorldRenderState().cameraRenderState;


        renderer.render(state, matrices, queue, camera);
        matrices.pop();
    }

    @Override
    public Class<BannerGuiElementState> getElementClass() {
        return BannerGuiElementState.class;
    }

    @Override
    protected String getName() {
        return "Banner GUI Renderer";
    }
}