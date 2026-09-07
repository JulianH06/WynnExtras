package julianh06.wynnextras.features.waypoints;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.features.waypoints.data.Waypoint;
import julianh06.wynnextras.features.waypoints.data.WaypointCategory;
import julianh06.wynnextras.features.waypoints.data.WaypointData;
import julianh06.wynnextras.features.waypoints.data.WaypointPackage;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.neoforged.bus.api.SubscribeEvent;

import java.awt.*;

@WEModule
public class WaypointRenderer {
    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        BlockPos playerPos = client.player == null ? null : client.player.getBlockPos();
        double maxRangeSquared = (double) WynnExtrasConfig.INSTANCE.waypointMaxRange
                * WynnExtrasConfig.INSTANCE.waypointMaxRange;

        //Extraction phase
        for(WaypointPackage pkg : WaypointData.INSTANCE.packages) {
            if(!pkg.enabled) continue;
            for(Waypoint waypoint : pkg.waypoints) {
                if(WaypointEditMode.isEditing(waypoint)) continue;

                double centerX = waypoint.displayX() + 0.5;
                double centerZ = waypoint.displayZ() + 0.5;
                double textY = waypoint.displayY() + 1.0 + waypoint.getSize() / 2.0;
                double distanceSquared = 0;
                if (playerPos != null) {
                    double dx = centerX - (playerPos.getX() + 0.5);
                    double dy = textY - playerPos.getY();
                    double dz = centerZ - (playerPos.getZ() + 0.5);
                    distanceSquared = dx * dx + dy * dy + dz * dz;
                    if (distanceSquared > maxRangeSquared) continue;
                }

                if(isOnBarrier(waypoint)) continue;

                WaypointCategory category = waypoint.getCategory();
                boolean seeThrough = !waypoint.shouldSeeThrough();

                Box box = waypoint.getRenderBox();
                WEVec pos = new WEVec(centerX, box.maxY + 0.5, centerZ);
                if(playerPos != null && waypoint.shouldShowDistance()) {
                    WorldRenderUtils.drawText(event, pos, Text.of((int) Math.sqrt(distanceSquared) + "m"), 0.75f, seeThrough);
                }
                Color color = category != null ? category.asAwtColor() : Color.cyan;

                if(waypoint.shouldShowBlock()) {
                    float alpha = category != null ? category.alpha : 0.5f;
                    WorldRenderUtils.drawFilledBoundingBox(event, box, color, alpha);
                }
                if(!waypoint.shouldShowName()) continue;
                WEVec namePos = new WEVec(centerX, box.maxY + 1.0, centerZ);
                WorldRenderUtils.drawText(event, namePos, waypoint.getNameText(), 0.75f, seeThrough);
            }
        }
    }

    private boolean isOnBarrier(Waypoint waypoint) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if(world == null) return false;

        return world.getBlockState(new BlockPos(waypoint.x, waypoint.y, waypoint.z)).isOf(Blocks.BARRIER);
    }
}
