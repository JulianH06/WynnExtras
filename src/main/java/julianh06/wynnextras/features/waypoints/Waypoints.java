package julianh06.wynnextras.features.waypoints;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.event.ClickEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.system.macosx.EnumerationMutationHandlerI;

import java.util.List;

@WEModule
public class Waypoints {
    static boolean commandsInitialized = false;

    private static SubCommand addCmd;
    private static SubCommand addCmdNoArgs;
    private static SubCommand removeCmd;
    private static Command waypointsCmd;

    public static boolean inScreen = false;


    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            if(commandsInitialized) return;

            addCmd = new SubCommand(
                    "add",
                    "",
                    context -> {
                        int x = IntegerArgumentType.getInteger(context, "x");
                        int y = IntegerArgumentType.getInteger(context, "y");
                        int z = IntegerArgumentType.getInteger(context, "z");
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                Text.of("Waypoint added at " + x + " " + y + " " + z)));
                        WaypointData.INSTANCE.waypoints.add(new Waypoint(x, y, z));
                        WaypointData.save();
                        return 1;
                    },
                    null,
                    List.of(ClientCommandManager.argument("x", IntegerArgumentType.integer()),
                            ClientCommandManager.argument("y", IntegerArgumentType.integer()),
                            ClientCommandManager.argument("z", IntegerArgumentType.integer())
                    )
            );

            addCmdNoArgs = new SubCommand(
                    "add",
                    "",
                    context -> {
                        if(MinecraftClient.getInstance().player == null) return 1;
                        int x = (int) Math.floor(MinecraftClient.getInstance().player.getX());
                        int y = (int) Math.floor(MinecraftClient.getInstance().player.getY()) - 1;
                        int z = (int) Math.floor(MinecraftClient.getInstance().player.getZ());
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                Text.of("Waypoint added at " + x + " " + y + " " + z)));
                        WaypointData.INSTANCE.waypoints.add(new Waypoint(x, y, z));
                        WaypointData.save();
                        return 1;
                    },
                    null,
                    null
            );

            removeCmd = new SubCommand(
                    "remove",
                    "removes the closest waypoint",
                    context -> {
                        Waypoint closest = null;
                        double smallestDistance = -1;
                        for(Waypoint waypoint : WaypointData.INSTANCE.waypoints) {
                            if(MinecraftClient.getInstance().player == null) return 1;
                            WEVec pos = new WEVec(waypoint.x + 0.5f, waypoint.y + 1.5f, waypoint.z + 0.5f);
                            WEVec playerPos = new WEVec(MinecraftClient.getInstance().player.getPos());
                            double distance = pos.distanceTo(playerPos);

                            if(closest == null) {
                                closest = waypoint;
                                smallestDistance = distance;
                                continue;
                            }

                            if(distance < smallestDistance) {
                                smallestDistance = distance;
                                closest = waypoint;
                            }
                        }
                        if(closest != null) {
                            WaypointData.INSTANCE.waypoints.remove(closest);
                            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                    Text.of("Waypoint removed at " + closest.x + " " + closest.y + " " + closest.z)));
                            WaypointData.save();
                        } else {
                            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                    Text.of("There are no waypoints left to remove.")));
                        }
                        return 1;
                    },
                    null,
                    null
            );

            waypointsCmd = new Command(
                    "waypoints",
                    "",
                    context -> {
                        MinecraftClient mcClient = MinecraftClient.getInstance();
                        mcClient.send(() -> mcClient.setScreen(null));
                        inScreen = true;
                        return 1;
                    },
                    List.of(addCmd, addCmdNoArgs, removeCmd),
                    null
            );
            commandsInitialized = true;
        });
    }

    @SubscribeEvent
    void onTick(TickEvent event) {
        if(inScreen) {
            MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new WaypointScreen()));
            inScreen = false;
        }
    }

    @SubscribeEvent
    void onClick(ClickEvent event) {
        WaypointScreen.onClick();
    }
}
