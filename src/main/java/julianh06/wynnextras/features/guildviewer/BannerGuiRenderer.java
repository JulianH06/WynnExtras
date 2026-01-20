package julianh06.wynnextras.features.guildviewer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;

public class BannerGuiRenderer extends SpecialGuiElementRenderer<BannerGuiElementState> {
    private final SpriteHolder sprite;

    public BannerGuiRenderer(VertexConsumerProvider.Immediate immediate, SpriteHolder sprite) {
        super(immediate);
        this.sprite = sprite;
    }

    @Override
    protected void render(BannerGuiElementState state, MatrixStack matrixStack) {
        MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);
        matrixStack.translate(0.0F, 0.0F, 0.0F);
        matrixStack.multiply(new Quaternionf().rotateY(-0.5f));
        RenderDispatcher renderDispatcher = MinecraftClient.getInstance().gameRenderer.getEntityRenderDispatcher();
        OrderedRenderCommandQueueImpl orderedRenderCommandQueueImpl = renderDispatcher.getQueue();

        MinecraftClient client = MinecraftClient.getInstance();

        long worldTime =
                client.world != null
                        ? client.world.getTime()
                        : 0L;

        float tickDelta =
                client.getRenderTickCounter().getTickProgress(false);

        float time = (worldTime % 100L) + tickDelta;

        BannerBlockEntityRenderer.renderCanvas(
                this.sprite,
                matrixStack,
                orderedRenderCommandQueueImpl,
                15728880,
                OverlayTexture.DEFAULT_UV,
                state.flag(),
                time / 100f,
                ModelBaker.BANNER_BASE,
                true,
                state.baseColor(),
                state.resultBannerPatterns(),
                false,
                null,
                0
        );
        renderDispatcher.render();
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