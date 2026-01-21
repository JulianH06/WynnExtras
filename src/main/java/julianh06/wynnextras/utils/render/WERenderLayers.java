package julianh06.wynnextras.utils.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;

import java.util.concurrent.ConcurrentHashMap;

public class WERenderLayers {
    private static final ConcurrentHashMap<Integer, RenderLayer> linesCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, RenderLayer> linesThroughWallsCache = new ConcurrentHashMap<>();

    private static final VertexFormat LINE_VERTEX_FORMAT = VertexFormat.builder()
            .add("pos", VertexFormatElement.POSITION)
            .add("color", VertexFormatElement.COLOR).build();

    public static RenderPipeline pipeline = RenderPipeline.builder()
            .withLocation("wynnextras_lines")
            .withVertexShader("shaders/vertex/lines.vsh")
            .withFragmentShader("shaders/fragment/lines.fsh")
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.LINES)
            .build();


    private static RenderLayer createLineRenderLayer(int lineWidth, boolean throughWalls) {
        return RenderLayer.of(
                "wynnextras_lines_" + lineWidth + (throughWalls ? "_xray" : ""),
                RenderSetup.builder(pipeline)
                        .translucent()
                        .expectedBufferSize(256)
                        .layeringTransform(throughWalls ? LayeringTransform.NO_LAYERING : LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .build()
        );
    }

    public static RenderLayer getLines(int lineWidth, boolean throughWalls) {
        ConcurrentHashMap<Integer, RenderLayer> cache = throughWalls ? linesThroughWallsCache : linesCache;
        return cache.computeIfAbsent(lineWidth, lw -> createLineRenderLayer(lw, throughWalls));
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
