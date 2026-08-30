package julianh06.wynnextras.features.waypoints;

import julianh06.wynnextras.annotations.WEModule;
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
        WEVec playerPos = client.player == null ? null : new WEVec(client.player.getBlockPos().toBottomCenterPos());

        //Extraction phase
        for(WaypointPackage pkg : WaypointData.INSTANCE.packages) {
            if(!pkg.enabled) continue;
            for(Waypoint waypoint : pkg.waypoints) {
                if(WaypointEditMode.isEditing(waypoint)) continue;
                if(isOnBarrier(waypoint)) continue;

                WaypointCategory category = waypoint.getCategory();
                boolean seeThrough = !waypoint.shouldSeeThrough();

                Box box = waypoint.getRenderBox();
                double centerX = (box.minX + box.maxX) / 2;
                double centerZ = (box.minZ + box.maxZ) / 2;

                WEVec pos = new WEVec(centerX, box.maxY + 0.5, centerZ);
                if(playerPos != null && waypoint.shouldShowDistance()) {
                    WorldRenderUtils.drawText(event, pos, Text.of((int) pos.distanceTo(playerPos) + "m"), 0.75f, seeThrough);
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
