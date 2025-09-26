package julianh06.wynnextras.utils.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;

public class RenderLayers {
    private static final ConcurrentHashMap<Integer, RenderLayer.MultiPhase> linesCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, RenderLayer.MultiPhase> linesThroughWallsCache = new ConcurrentHashMap<>();

    private static RenderLayer.MultiPhase createLineRenderLayer(int lineWidth, boolean throughWalls) {
        return RenderLayer.of(
                "wynnextras_lines_" + lineWidth + (throughWalls ? "_xray" : ""),
                VertexFormats.LINES,
                VertexFormat.DrawMode.LINES,
                RenderLayer.DEFAULT_BUFFER_SIZE,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(RenderPhase.LINES_PROGRAM)
                        .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(lineWidth)))
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .writeMaskState(throughWalls ? RenderPhase.COLOR_MASK : RenderPhase.ALL_MASK)
                        .depthTest(throughWalls ? RenderPhase.DepthTest.ALWAYS_DEPTH_TEST : RenderPhase.DepthTest.LEQUAL_DEPTH_TEST)
                        .layering(throughWalls ? RenderPhase.NO_LAYERING : RenderPhase.VIEW_OFFSET_Z_LAYERING)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .build(false)
        );
    }

    public static RenderLayer.MultiPhase getLines(int lineWidth, boolean throughWalls) {
        ConcurrentHashMap<Integer, RenderLayer.MultiPhase> cache = throughWalls ? linesThroughWallsCache : linesCache;
        return cache.computeIfAbsent(lineWidth, lw -> createLineRenderLayer(lw, throughWalls));
    }

    public static RenderLayer.MultiPhase getFilled(boolean throughWalls) {
        return throughWalls ? FILLED_XRAY : FILLED;
    }

    // Fixed MultiPhases
    private static final RenderLayer.MultiPhase FILLED = RenderLayer.of(
            "wynnextras_filled",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.TRIANGLE_STRIP,
            RenderLayer.DEFAULT_BUFFER_SIZE,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.POSITION_COLOR_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .writeMaskState(RenderPhase.ALL_MASK)
                    .depthTest(RenderPhase.DepthTest.LEQUAL_DEPTH_TEST)
                    .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .build(false)
    );

    private static final RenderLayer.MultiPhase FILLED_XRAY = RenderLayer.of(
            "wynnextras_filled_xray",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.TRIANGLE_STRIP,
            RenderLayer.DEFAULT_BUFFER_SIZE,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.POSITION_COLOR_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .depthTest(RenderPhase.DepthTest.ALWAYS_DEPTH_TEST)
                    .layering(RenderPhase.NO_LAYERING)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .build(false)
    );
}
