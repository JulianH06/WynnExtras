package julianh06.wynnextras.utils.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.utils.WEVec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Dedicated circle renderer using manual buffer management.
 * This follows the same pattern as WorldRenderTest which is proven to work in 1.21.11.
 */
public class CircleRenderer {
    public static CircleRenderer instance;

    private static final RenderPipeline CIRCLE_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of(WynnExtras.MOD_ID, "pipeline/circle_renderer"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthBias(-1.0f, -1.0f)
            .build()
    );

    private static final BufferAllocator allocator = new BufferAllocator(RenderLayer.field_64009);
    private BufferBuilder buffer;
    private MappableRingBuffer vertexBuffer;

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private static final int SEGMENTS = 256;
    private static final float HEIGHT = 0.05f;

    // Queue of circles to render
    private final List<CircleData> circleQueue = new ArrayList<>();

    public static class CircleData {
        public final WEVec location;
        public final double radius;
        public final Color color;

        public CircleData(WEVec location, double radius, Color color) {
            this.location = location;
            this.radius = radius;
            this.color = color;
        }
    }

    /**
     * Queue a circle to be rendered this frame.
     */
    public void queueCircle(WEVec location, double radius, Color color) {
        circleQueue.add(new CircleData(location, radius, color));
    }

    /**
     * Render all queued circles and clear the queue.
     * This should be called from RenderEvents after the WEEvent is posted.
     */
    public void renderQueuedCircles(WorldRenderContext context) {
        if (circleQueue.isEmpty()) {
            return;
        }

        MatrixStack matrices = context.matrices();
        Vec3d camera = context.worldState().cameraRenderState.pos;

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (buffer == null) {
            buffer = new BufferBuilder(allocator, CIRCLE_PIPELINE.getVertexFormatMode(), CIRCLE_PIPELINE.getVertexFormat());
        }

        Matrix4fc posMatrix = matrices.peek().getPositionMatrix();

        // Add vertices for all queued circles
        for (CircleData circle : circleQueue) {
            renderCircle(posMatrix, circle.location, circle.radius, circle.color);
        }

        matrices.pop();

        // Draw all the vertices
        drawCircles(MinecraftClient.getInstance());

        // Clear the queue for next frame
        circleQueue.clear();
    }

    private void renderCircle(Matrix4fc posMatrix, WEVec location, double radius, Color color) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        float cx = (float) location.x();
        float cy = (float) location.y();
        float cz = (float) location.z();

        double angleStep = 2 * Math.PI / SEGMENTS;

        for (int i = 0; i < SEGMENTS; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;

            float x1 = cx + (float) (Math.cos(angle1) * radius);
            float z1 = cz + (float) (Math.sin(angle1) * radius);
            float x2 = cx + (float) (Math.cos(angle2) * radius);
            float z2 = cz + (float) (Math.sin(angle2) * radius);

            // Draw outer face (visible from outside)
            buffer.vertex(posMatrix, x1, cy, z1).color(r, g, b, a);
            buffer.vertex(posMatrix, x1, cy + HEIGHT, z1).color(r, g, b, a);
            buffer.vertex(posMatrix, x2, cy + HEIGHT, z2).color(r, g, b, a);
            buffer.vertex(posMatrix, x2, cy, z2).color(r, g, b, a);

            // Draw inner face (visible from inside) - reverse winding order
            buffer.vertex(posMatrix, x2, cy, z2).color(r, g, b, a);
            buffer.vertex(posMatrix, x2, cy + HEIGHT, z2).color(r, g, b, a);
            buffer.vertex(posMatrix, x1, cy + HEIGHT, z1).color(r, g, b, a);
            buffer.vertex(posMatrix, x1, cy, z1).color(r, g, b, a);
        }
    }

    private void drawCircles(MinecraftClient client) {
        BuiltBuffer builtBuffer = buffer.end();
        BuiltBuffer.DrawParameters drawParameters = builtBuffer.getDrawParameters();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = upload(drawParameters, format, builtBuffer);
        draw(client, CIRCLE_PIPELINE, builtBuffer, drawParameters, vertices, format);

        if (vertexBuffer != null) {
            vertexBuffer.rotate();
        }
        buffer = null;
    }

    private GpuBuffer upload(BuiltBuffer.DrawParameters drawParameters, VertexFormat format, BuiltBuffer builtBuffer) {
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            if (vertexBuffer != null) {
                vertexBuffer.close();
            }
            vertexBuffer = new MappableRingBuffer(() -> WynnExtras.MOD_ID + " circle renderer", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(vertexBuffer.getBlocking().slice(0, builtBuffer.getBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.getBuffer(), mappedView.data());
        }

        return vertexBuffer.getBlocking();
    }

    private void draw(MinecraftClient client, RenderPipeline pipeline, BuiltBuffer builtBuffer, BuiltBuffer.DrawParameters drawParameters, GpuBuffer vertices, VertexFormat format) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (pipeline.getVertexFormatMode() == VertexFormat.DrawMode.QUADS) {
            builtBuffer.sortQuads(allocator, RenderSystem.getProjectionType().getVertexSorter());
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.getSortedBuffer());
            indexType = builtBuffer.getDrawParameters().indexType();
        } else {
            RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = shapeIndexBuffer.getIndexBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.getIndexType();
        }

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> WynnExtras.MOD_ID + " circle renderer", client.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(), client.getFramebuffer().getDepthAttachmentView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);

            renderPass.drawIndexed(0 / format.getVertexSize(), 0, drawParameters.indexCount(), 1);
        }

        builtBuffer.close();
    }

    public void close() {
        allocator.close();
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
