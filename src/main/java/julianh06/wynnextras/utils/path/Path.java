package julianh06.wynnextras.utils.path;

import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.List;

public record Path(List<Node> nodes) {
    public void draw(RenderWorldEvent event, Color color, Vec3d playerPos) {
        if (nodes == null || nodes.isEmpty()) return;

        int startIndex = 0;

        if (playerPos != null) {
            // Find the closest node to player
            double closestDistSq = Double.MAX_VALUE;
            for (int i = 0; i < nodes.size(); i++) {
                double distSq = nodes.get(i).getCenterPos().squaredDistanceTo(playerPos);
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    startIndex = i;
                }
            }

            // If the closest node is not the first, check if we should start from previous node
            // This handles when player is between nodes
            if (startIndex <= nodes.size() - 2) {
                Node curr = nodes.get(startIndex);
                Node next = nodes.get(startIndex + 1);

                double playToNext = playerPos.squaredDistanceTo(next.getCenterPos());
                double currToNext = curr.getCenterPos().squaredDistanceTo(next.getCenterPos());

                if (playToNext < currToNext && curr.getDistance(next) != 0.) {
                    startIndex = startIndex + 1;
                }
            }

            WorldRenderUtils.drawLineToEye(
                    event,
                    new WEVec(nodes.get(startIndex).getCenterPos()),
                    color,
                    2,
                    false
            );
        }

        // Draw from startIndex to the end
        for (int i = startIndex; i < nodes.size() - 1; i++) {
            Node current = nodes.get(i);
            Node next = nodes.get(i + 1);

            // Stop drawing at teleport points
            if (current.getDistance(next) == 0.0) {
                break;
            }

            WorldRenderUtils.draw3DLine(
                    event,
                    new WEVec(current.getCenterPos()),
                    new WEVec(next.getCenterPos()),
                    color,
                    2,
                    false
            );
        }
    }
}
