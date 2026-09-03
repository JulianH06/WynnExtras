package julianh06.wynnextras.features.waypoints;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.features.waypoints.data.Waypoint;
import julianh06.wynnextras.features.waypoints.data.WaypointData;
import julianh06.wynnextras.features.waypoints.data.WaypointPackage;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.neoforged.bus.api.SubscribeEvent;

import java.awt.*;

@WEModule
public class WaypointRenderer {
    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        WorldRenderUtils.INSTANCE_WAYPOINTS.buffer = new BufferBuilder(WorldRenderUtils.allocator, WorldRenderUtils.FILLED_BOX.getVertexFormatMode(), WorldRenderUtils.FILLED_BOX.getVertexFormat());
        boolean renderedAny = false;
        WEVec playerPos = MinecraftClient.getInstance().player == null
                ? null
                : new WEVec(MinecraftClient.getInstance().player.getBlockPos().toBottomCenterPos());

        //Extraction phase
        for(WaypointPackage pkg : WaypointData.INSTANCE.packages) {
            if(!pkg.enabled) continue;
            for(Waypoint waypoint : pkg.waypoints) {
                if(WaypointEditMode.isEditing(waypoint)) continue;
                if(isOnBarrier(waypoint)) continue;

                WEVec pos = new WEVec(waypoint.x + 0.5f, waypoint.y + 1.5f, waypoint.z + 0.5f);
                double distance = playerPos == null ? 0 : pos.distanceTo(playerPos);
                if(playerPos != null && distance > WynnExtrasConfig.INSTANCE.waypointMaxRange) continue;
                if(playerPos != null && waypoint.shouldShowDistance()) {
                    WorldRenderUtils.drawText(event, pos, Text.of((int) distance + "m"), 0.75f, !waypoint.shouldSeeThrough());
                }
                WEVec namePos = new WEVec(waypoint.x + 0.5f, waypoint.y + 2f, waypoint.z + 0.5f);
                Color color = Color.cyan;
                if(waypoint.getCategory() != null) {
                    float[] hsb = waypoint.getCategory().color.asHSB();
                    color = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
                }

                if(waypoint.shouldShowBlock()) {
                    float alpha = 0.5f;
                    if(waypoint.getCategory() != null) {
                        alpha = waypoint.getCategory().alpha;
                    }
                    WorldRenderUtils.INSTANCE_WAYPOINTS.drawFilledBoundingBox(event, new Box(waypoint.x, waypoint.y, waypoint.z, waypoint.x + 1, waypoint.y + 1, waypoint.z + 1), color, alpha);
                    renderedAny = true;
                }
                if(!waypoint.shouldShowName()) continue;
                WorldRenderUtils.drawText(event, namePos, Text.of(waypoint.name), 0.75f, !waypoint.shouldSeeThrough());
            }
        }

        //Render phase
        if(renderedAny) WorldRenderUtils.INSTANCE_WAYPOINTS.drawFilledBoxes(MinecraftClient.getInstance(), WorldRenderUtils.FILLED_BOX);
    }

    private boolean isOnBarrier(Waypoint waypoint) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if(world == null) return false;

        return world.getBlockState(new BlockPos(waypoint.x, waypoint.y, waypoint.z)).isOf(Blocks.BARRIER);
    }
}
