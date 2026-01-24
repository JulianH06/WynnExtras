package julianh06.wynnextras.utils.render;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;

public class WERenderLayers {
    // Lines rendering is no longer used - circles use FILLED quads instead
    // This is kept for backwards compatibility with any code that still uses it
    public static RenderLayer getLines(int lineWidth, boolean throughWalls) {
        // Use filled layer as fallback since vanilla LINES methods aren't available
        return throughWalls ? FILLED_XRAY : FILLED;
    }

    public static RenderLayer getFilled(boolean throughWalls) {
        return throughWalls ? FILLED_XRAY : FILLED;
    }

    private static final RenderLayer FILLED = RenderLayer.of(
            "wynnextras_filled",
            RenderSetup.builder(RenderPipelines.DEBUG_FILLED_BOX)
                    .translucent()
                    .expectedBufferSize(256)
                    .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .build()
    );

    private static final RenderLayer FILLED_XRAY = RenderLayer.of(
            "wynnextras_filled_xray",
            RenderSetup.builder(RenderPipelines.DEBUG_FILLED_BOX)
                    .translucent()
                    .expectedBufferSize(256)
                    .layeringTransform(LayeringTransform.NO_LAYERING)
                    .build()
    );

}
