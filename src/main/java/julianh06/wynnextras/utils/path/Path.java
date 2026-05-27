package julianh06.wynnextras.utils.path;

import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.minecraft.util.math.BlockPos;

import java.awt.*;
import java.util.List;

public record Path(List<BlockPos> nodes) {
    public void draw(RenderWorldEvent event, Color color) {
        if (nodes == null || nodes.size() < 2) return;
        for (int i = 0; i < nodes.size() - 1; i++) {
            BlockPos current = nodes.get(i);
            BlockPos next = nodes.get(i + 1);

            WorldRenderUtils.draw3DLine(
                    event,
                    new WEVec(current.toCenterPos()),
                    new WEVec(next.toCenterPos()),
                    color,
                    2,
                    false
            );
        }
    }
}
