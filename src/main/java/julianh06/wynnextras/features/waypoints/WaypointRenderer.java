package julianh06.wynnextras.features.waypoints;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.neoforged.bus.api.SubscribeEvent;

import java.awt.*;

@WEModule
public class WaypointRenderer {
    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        for(WaypointPackage pkg : WaypointData.INSTANCE.packages) {
            if(!pkg.enabled) continue;
            for(Waypoint waypoint : pkg.waypoints) {
                WEVec pos = new WEVec(waypoint.x + 0.5f, waypoint.y + 1.5f, waypoint.z + 0.5f);
                if(MinecraftClient.getInstance().player != null && waypoint.showDistance) {
                    WEVec playerPos = new WEVec(MinecraftClient.getInstance().player.getPos());
                    WorldRenderUtils.drawText(event, pos, Text.of((int) pos.distanceTo(playerPos) + "m"), 0.75f, true);
                }
                WEVec namePos = new WEVec(waypoint.x + 0.5f, waypoint.y + 2f, waypoint.z + 0.5f);
                Color color = Color.cyan;
                if(waypoint.getCategory() != null) {
                    color = Color.getHSBColor(waypoint.getCategory().color.asHSB()[0], waypoint.getCategory().color.asHSB()[1], waypoint.getCategory().color.asHSB()[2]);
                }

                if(waypoint.show) {
                    float alpha = 0.5f;
                    if(waypoint.getCategory() != null) {
                        alpha = waypoint.getCategory().alpha;
                    }
                    WorldRenderUtils.drawFilledBoundingBox(event, new Box(waypoint.x, waypoint.y, waypoint.z, waypoint.x + 1, waypoint.y + 1, waypoint.z + 1), color, alpha, true);
                }
                if(!waypoint.showName) continue;
                WorldRenderUtils.drawText(event, namePos, Text.of(waypoint.name), 0.75f, true);
            }
        }
    }
}
