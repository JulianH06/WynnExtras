package julianh06.wynnextras.utils.render;

import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.WEVec;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class LineDrawer {
    private final List<QueuedLine> queuedLines = new ArrayList<>();

    public static class QueuedLine {
        public WEVec p1;
        public WEVec p2;
        public Color color;
        public WEVec normal;

        public QueuedLine(WEVec p1, WEVec p2, Color color) {
            this.p1 = p1;
            this.p2 = p2;
            this.color = color;

            this.normal = p2.subtract(p1).normalize();
        }
    }

    private final RenderWorldEvent event;
    private final int lineWidth;
    private final boolean depth;

    public LineDrawer(RenderWorldEvent event, int lineWidth, boolean depth) {
        this.event = event;
        this.lineWidth = lineWidth;
        this.depth = depth;
    }

    private void drawQueuedLines() {
        if (queuedLines.isEmpty()) return;

        RenderLayer.MultiPhase layer = RenderLayers.getLines(lineWidth, !depth);
        VertexConsumer buffer = event.vertexConsumerProvider.getBuffer(layer);
        MatrixStack.Entry matrix = event.matrices.peek();

        for (QueuedLine line: queuedLines) {
            buffer.vertex(matrix.getPositionMatrix(), (float) line.p1.x(), (float) line.p1.y(), (float) line.p1.z())
                    .normal(matrix, (float) line.normal.x(), (float) line.normal.y(), (float) line.normal.z())
                    .color(line.color.getRed(), line.color.getGreen(), line.color.getBlue(), line.color.getAlpha());

            buffer.vertex(matrix.getPositionMatrix(), (float) line.p2.x(), (float) line.p2.y(), (float) line.p2.z())
                    .normal(matrix, (float) line.normal.x(), (float) line.normal.y(), (float) line.normal.z())
                    .color(line.color.getRed(), line.color.getGreen(), line.color.getBlue(), line.color.getAlpha());
        }

        queuedLines.clear();
    }

    private void addQueuedLine(WEVec p1, WEVec p2, Color color) {
        QueuedLine last = queuedLines.isEmpty() ? null : queuedLines.getLast();

        if (last == null) {
            queuedLines.add(new QueuedLine(p1, p2, color));
            return;
        }

        if (!last.p2.equals(p1)) {
            drawQueuedLines();
        }

        queuedLines.add(new QueuedLine(p1, p2, color));
    }

    public void drawEdges(WEVec location, Color color) {
        for (Pair<WEVec, WEVec> edge: location.edges()) {
            draw3DLine(edge.getFirst(), edge.getSecond(), color);
        }
    }

    public void drawEdges(Box box, Color color) {
        for (Pair<WEVec, WEVec> edge: WorldRenderUtils.calculateEdges(box)) {
            draw3DLine(edge.getFirst(), edge.getSecond(), color);
        }
    }

    public void draw3DLine(WEVec p1, WEVec p2, Color color) {
        addQueuedLine(p1, p2, color);
    }

    static void draw3D(RenderWorldEvent event, int lineWidth, boolean depth, LineDrawerDraws draws) {
        event.matrices.push();

        WEVec inverseView = WorldRenderUtils.getViewerPos().negate();
        event.matrices.translate(inverseView.x(), inverseView.y(), inverseView.z());

        LineDrawer lineDrawer = new LineDrawer(event, lineWidth, depth);
        draws.draw(lineDrawer);
        lineDrawer.drawQueuedLines();

        event.matrices.pop();
    }

    @FunctionalInterface
    interface LineDrawerDraws {
        void draw(LineDrawer lineDrawer);
    }
}
