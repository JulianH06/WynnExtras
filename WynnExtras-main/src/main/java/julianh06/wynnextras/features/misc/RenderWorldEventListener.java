package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.neoforged.bus.api.SubscribeEvent;

import java.awt.*;

import static julianh06.wynnextras.features.misc.ShamanTotemCircle.totemPositions;

public class RenderWorldEventListener {
    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        var matrices = event.matrices;
        var camera = event.camera;
        var partialTicks = event.partialTicks;

        System.out.println("aaaaaa " + partialTicks);

        for (int i = 0; i < 4; i++) {
            if(totemPositions.containsKey(i)) {
                WEVec endVec = new WEVec(
                        totemPositions.get(i).x() + 10,
                        totemPositions.get(i).y(),
                        totemPositions.get(i).z()
                );
                //drawCircle(context, totemPositions.get(i).getX(), totemPositions.get(i).getY() - 2, totemPositions.get(i).getZ(), 10);
                //Vec3d newPos = new Vec3d(totemPositions.get(i).getX(), totemPositions.get(i).getY() - 2, totemPositions.get(i).getZ());
                //rangeDrawer.invokeRenderCircle(context.matrixStack(), newPos, 1, CustomColor.fromHexString("FFFFFF").asInt());
                WorldRenderUtils.draw3DLine(event, totemPositions.get(i), endVec, Color.WHITE, 1, true);
            }
        }
    }
}
