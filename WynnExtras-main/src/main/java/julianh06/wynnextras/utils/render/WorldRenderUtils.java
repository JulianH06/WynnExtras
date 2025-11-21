package julianh06.wynnextras.utils.render;

import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.WEVec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.entity.Entity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;
import java.util.Set;

public class WorldRenderUtils {
    public static WEVec getViewerPos() {
        return exactLocation(MinecraftClient.getInstance().gameRenderer.getCamera());
    }

    public static WEVec exactLocation(Entity entity, float partialTicks) {
        if (!entity.isAlive()) return new WEVec(entity.getPos());
        WEVec prev = new WEVec(entity.prevX, entity.prevY, entity.prevZ);

        return prev.add(new WEVec(entity.getPos()).subtract(prev).multiply(partialTicks));
    }

    public static WEVec exactLocation(Camera camera) {
        return new WEVec(camera.getPos());
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
    public static void drawFilledBoundingBox(RenderWorldEvent event, Box box, Color color, float alphaMultiplier, boolean depth) {
        WEVec viewerPos = getViewerPos();
        Box adjustedBox = new Box(
                box.minX - viewerPos.x(), box.minY - viewerPos.y(), box.minZ - viewerPos.z(),
                box.maxX - viewerPos.x(), box.maxY - viewerPos.y(), box.maxZ - viewerPos.z()
        );

        RenderLayer.MultiPhase layer = RenderLayers.getFilled(!depth);
        VertexConsumer buffer = event.vertexConsumerProvider.getBuffer(layer);
        event.matrices.push();

        VertexRendering.drawFilledBox(
                event.matrices,
                buffer,
                adjustedBox.minX, adjustedBox.minY, adjustedBox.minZ,
                adjustedBox.maxX, adjustedBox.maxY, adjustedBox.maxZ,
                color.getRed() / 255f * 0.9f,
                color.getGreen() / 255f * 0.9f,
                color.getBlue() / 255f * 0.9f,
                color.getAlpha() / 255f * alphaMultiplier
        );
        event.matrices.pop();
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

    public static void drawText(RenderWorldEvent event, WEVec location, Text text, float scale, boolean depth) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

        Matrix4f matrix = new Matrix4f();
        WEVec viewerPos = getViewerPos();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        float adjustedScale = scale * 0.05f;

        matrix.translate(
                (float) (location.x() - viewerPos.x()),
                (float) (location.y() - viewerPos.y()),
                (float) (location.z() - viewerPos.z())
        ).rotate(camera.getRotation()).scale(adjustedScale, -adjustedScale, adjustedScale);

        List<OrderedText> textLines = textRenderer.wrapLines(text, 1000000);

        for (int i = 0; i < textLines.size(); i++) {
            OrderedText line = textLines.get(i);
            float yOffset = i * textRenderer.fontHeight * 1.3f;
            float xOffset = -textRenderer.getWidth(line) / 2f;

            textRenderer.draw(
                    line,
                    xOffset, yOffset,
                    0xFFFFFF,
                    false,
                    matrix,
                    event.vertexConsumerProvider,
                    depth ? TextRenderer.TextLayerType.NORMAL : TextRenderer.TextLayerType.SEE_THROUGH,
                    0, LightmapTextureManager.MAX_LIGHT_COORDINATE
            );
        }
    }
}