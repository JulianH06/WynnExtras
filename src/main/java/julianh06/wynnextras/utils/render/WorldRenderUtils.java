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
import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.WEVec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;

public class WorldRenderUtils {
    public static WorldRenderUtils INSTANCE_WAYPOINTS = new WorldRenderUtils();
    public static WorldRenderUtils INSTANCE_SHAMANTOTEM = new WorldRenderUtils();

    public static final RenderPipeline FILLED_BOX = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of(WynnExtras.MOD_ID, "pipeline/debug_filled_box"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthBias(-1.0f, -1.0f)
            .build()
    );

    public static final BufferAllocator allocator = new BufferAllocator(RenderLayer.field_64009);
    public BufferBuilder buffer;
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private MappableRingBuffer vertexBuffer;

    public static WEVec getViewerPos() {
        return exactLocation(MinecraftClient.getInstance().gameRenderer.getCamera());
    }

    public static WEVec exactLocation(Entity entity, float partialTicks) {
        if (!entity.isAlive()) return new WEVec(entity.getBlockPos().toBottomCenterPos());
        WEVec prev = new WEVec(entity.lastX, entity.lastY, entity.lastZ);

        return prev.add(new WEVec(entity.getBlockPos().toBottomCenterPos()).subtract(prev).multiply(partialTicks));
    }

    public static WEVec exactLocation(Camera camera) {
        return new WEVec(camera.getCameraPos());
    }

    public static WEVec exactPlayerEyeLocation(RenderWorldEvent event) {
        ClientPlayerEntity player = MinecraftUtils.localPlayer();
        double eyeHeight = player.getEyeHeight(player.getPose());
        return exactLocation(player, event.partialTicks).add(0, eyeHeight, 0);
    }

    public static Set<Pair<WEVec, WEVec>> calculateEdges(Box box) {
        WEVec bottomLeftFront = new WEVec(box.minX, box.minY, box.minZ);
        WEVec bottomLeftBack = new WEVec(box.minX, box.minY, box.maxZ);
        WEVec topLeftFront = new WEVec(box.minX, box.maxY, box.minZ);
        WEVec topLeftBack = new WEVec(box.minX, box.maxY, box.maxZ);
        WEVec bottomRightFront = new WEVec(box.maxX, box.minY, box.minZ);
        WEVec bottomRightBack = new WEVec(box.maxX, box.minY, box.maxZ);
        WEVec topRightFront = new WEVec(box.maxX, box.maxY, box.minZ);
        WEVec topRightBack = new WEVec(box.maxX, box.maxY, box.maxZ);

        return Set.of(
                new Pair<>(bottomLeftFront, bottomLeftBack),
                new Pair<>(bottomLeftBack, bottomRightBack),
                new Pair<>(bottomRightBack, bottomRightFront),
                new Pair<>(bottomRightFront, bottomLeftFront),

                new Pair<>(topLeftFront, topLeftBack),
                new Pair<>(topLeftBack, topRightBack),
                new Pair<>(topRightBack, topRightFront),
                new Pair<>(topRightFront, topLeftFront),

                new Pair<>(bottomLeftFront, topLeftFront),
                new Pair<>(bottomLeftBack, topLeftBack),
                new Pair<>(bottomRightBack, topRightBack),
                new Pair<>(bottomRightFront, topRightFront)
        );
    }

    // Draws
    public void drawFilledBoundingBox(RenderWorldEvent event, Box box, Color color, float alphaMultiplier) {
        MatrixStack matrices = event.matrices;
        Vec3d camera = event.camera.getCameraPos();

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        renderFilledBox(
                event.matrices.peek().getPositionMatrix(),
                this.buffer,
                (float) box.minX, (float) box.minY, (float) box.minZ,
                (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                color.getRed() / 255f * 0.9f,
                color.getGreen() / 255f * 0.9f,
                color.getBlue() / 255f * 0.9f,
                color.getAlpha() / 255f * alphaMultiplier
        );

        matrices.pop();
    }

    private static void renderFilledBox(Matrix4fc positionMatrix, BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float red, float green, float blue, float alpha) {
        // Front Face
        buffer.vertex(positionMatrix, minX, minY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, minY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, maxY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, maxY, maxZ).color(red, green, blue, alpha);

        // Back face
        buffer.vertex(positionMatrix, maxX, minY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, minY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, maxY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, maxY, minZ).color(red, green, blue, alpha);

        // Left face
        buffer.vertex(positionMatrix, minX, minY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, minY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, maxY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, maxY, minZ).color(red, green, blue, alpha);

        // Right face
        buffer.vertex(positionMatrix, maxX, minY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, minY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, maxY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, maxY, maxZ).color(red, green, blue, alpha);

        // Top face
        buffer.vertex(positionMatrix, minX, maxY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, maxY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, maxY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, maxY, minZ).color(red, green, blue, alpha);

        // Bottom face
        buffer.vertex(positionMatrix, minX, minY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, minY, minZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, maxX, minY, maxZ).color(red, green, blue, alpha);
        buffer.vertex(positionMatrix, minX, minY, maxZ).color(red, green, blue, alpha);
    }

    public static void drawEdges(RenderWorldEvent event, Box box, Color color, int lineWidth, boolean depth) {
        LineDrawer.draw3D(event, lineWidth, depth, lineDrawer -> lineDrawer.drawEdges(box, color));
    }

    public static void drawEdges(RenderWorldEvent event, WEVec location, Color color, int lineWidth, boolean depth) {
        LineDrawer.draw3D(event, lineWidth, depth, lineDrawer -> lineDrawer.drawEdges(location, color));
    }

    public static void draw3DLine(RenderWorldEvent event, WEVec p1, WEVec p2, Color color, int lineWidth, boolean depth) {
        LineDrawer.draw3D(event, lineWidth, depth, lineDrawer -> lineDrawer.draw3DLine(p1, p2, color));
    }

    public static void drawLineToEye(RenderWorldEvent event, WEVec location, Color color, int lineWidth, boolean depth) {
        WEVec rotationVec = new WEVec(MinecraftUtils.localPlayer().getRotationVec(event.partialTicks));
        draw3DLine(event, exactPlayerEyeLocation(event).add(rotationVec.multiply(2)), location, color, lineWidth, depth);
    }

    public static void draw3DCircle(RenderWorldEvent event, WEVec location, double radius, Color color, int lineWidth, boolean depth) {
        LineDrawer.draw3D(event, lineWidth, depth, lineDrawer -> {
            WEVec lastPoint = location.add(radius, 0, 0);

            for (int i = 1; i <= 360; i++) {
                double rad = Math.toRadians(i);
                WEVec newPoint = location.add(Math.cos(rad) * radius, 0, Math.sin(rad) * radius);
                lineDrawer.draw3DLine(lastPoint, newPoint, color);
                lastPoint = newPoint;
            }
        });
    }

    public static void drawText(
            RenderWorldEvent event,
            WEVec location,
            Text text,
            float scale,
            boolean depth
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        Camera camera = client.gameRenderer.getCamera();

        Matrix4f matrix = new Matrix4f();
        WEVec viewerPos = getViewerPos();
        float adjustedScale = scale * 0.05f;

        matrix.translate(
                (float) (location.x() - viewerPos.x()),
                (float) (location.y() - viewerPos.y()),
                (float) (location.z() - viewerPos.z())
        ).rotate(camera.getRotation()).scale(adjustedScale, -adjustedScale, adjustedScale);

        textRenderer.draw(
                text,
                -textRenderer.getWidth(text) / 2f,
                0,
                -1,
                false,
                matrix,
                event.vertexConsumerProvider,
                depth ? TextRenderer.TextLayerType.NORMAL : TextRenderer.TextLayerType.SEE_THROUGH,
                0,
                15728880
        );
    }


    public void drawFilledBoxes(MinecraftClient client, @SuppressWarnings("SameParameterValue") RenderPipeline pipeline) {
        BuiltBuffer builtBuffer = buffer.end();
        BuiltBuffer.DrawParameters drawParameters = builtBuffer.getDrawParameters();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = upload(drawParameters, format, builtBuffer);

        draw(client, pipeline, builtBuffer, drawParameters, vertices, format);

        vertexBuffer.rotate();
        buffer = null;
    }

    public void drawLines(MinecraftClient client, @SuppressWarnings("SameParameterValue") RenderPipeline pipeline) {
        drawFilledBoxes(client, pipeline);
    }

    private GpuBuffer upload(BuiltBuffer.DrawParameters drawParameters, VertexFormat format, BuiltBuffer builtBuffer) {
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            if (vertexBuffer != null) {
                vertexBuffer.close();
            }

            vertexBuffer = new MappableRingBuffer(() -> WynnExtras.MOD_ID + " render pipeline", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(vertexBuffer.getBlocking().slice(0, builtBuffer.getBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.getBuffer(), mappedView.data());
        }

        return vertexBuffer.getBlocking();
    }

    private static void draw(MinecraftClient client, RenderPipeline pipeline, BuiltBuffer builtBuffer, BuiltBuffer.DrawParameters drawParameters, GpuBuffer vertices, VertexFormat format) {
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
                .createRenderPass(() -> WynnExtras.MOD_ID + " example render pipeline rendering", client.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(), client.getFramebuffer().getDepthAttachmentView(), OptionalDouble.empty())) {
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