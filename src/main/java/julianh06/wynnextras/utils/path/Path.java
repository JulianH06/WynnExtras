package julianh06.wynnextras.utils.path;

import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;

import java.awt.*;
import java.util.List;

public record Path(List<Node> nodes) {
    public void draw(RenderWorldEvent event, Color color) {
        if (nodes == null || nodes.size() < 2) return;
        for (int i = 0; i < nodes.size() - 1; i++) {
            Node current = nodes.get(i);
            Node next = nodes.get(i + 1);

            // tp point stop drawing
            if (current.getDistance(next) == 0.) {
                return;
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
