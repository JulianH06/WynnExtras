package julianh06.wynnextras.features.waypoints;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.event.ClickEvent;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.macosx.EnumerationMutationHandlerI;

import java.util.List;

import static julianh06.wynnextras.features.waypoints.WaypointScreen.scaleFactor;

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
                        if(MinecraftClient.getInstance().player == null || WaypointData.INSTANCE.packages.getFirst() == null) return 1;
                        int x = IntegerArgumentType.getInteger(context, "x");
                        int y = IntegerArgumentType.getInteger(context, "y");
                        int z = IntegerArgumentType.getInteger(context, "z");
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                Text.of("Waypoint added at " + x + " " + y + " " + z + " in Package " + WaypointData.INSTANCE.packages.getFirst().name)));
                        WaypointData.INSTANCE.packages.getFirst().waypoints.add(new Waypoint(x, y, z));
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
                        if(MinecraftClient.getInstance().player == null || WaypointData.INSTANCE.packages.getFirst() == null) return 1;
                        int x = (int) Math.floor(MinecraftClient.getInstance().player.getX());
                        int y = (int) Math.floor(MinecraftClient.getInstance().player.getY()) - 1;
                        int z = (int) Math.floor(MinecraftClient.getInstance().player.getZ());
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                Text.of("Waypoint added at " + x + " " + y + " " + z + " in Package " + WaypointData.INSTANCE.packages.getFirst().name)));
                        WaypointData.INSTANCE.packages.getFirst().waypoints.add(new Waypoint(x, y, z));
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
                        WaypointPackage nearestPackage = null;
                        double smallestDistance = -1;
                        for(WaypointPackage waypointPackage : WaypointData.INSTANCE.packages) {
                            for (Waypoint waypoint : waypointPackage.waypoints) {
                                if (MinecraftClient.getInstance().player == null) return 1;
                                WEVec pos = new WEVec(waypoint.x + 0.5f, waypoint.y + 1.5f, waypoint.z + 0.5f);
                                WEVec playerPos = new WEVec(MinecraftClient.getInstance().player.getBlockPos().toBottomCenterPos());
                                double distance = pos.distanceTo(playerPos);

                                if (closest == null || distance < smallestDistance) {
                                    closest = waypoint;
                                    nearestPackage = waypointPackage;
                                    smallestDistance = distance;
                                }
                            }
                        }
                        if(closest != null) {
                            nearestPackage.waypoints.remove(closest);
                            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                    Text.of("Waypoint removed at " + closest.x + " " + closest.y + " " + closest.z + " in package " + nearestPackage.name)));
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

    @SubscribeEvent
    public void onInput(KeyInputEvent event) {
        if(event.getKey() == GLFW.GLFW_KEY_UP && event.getAction() == GLFW.GLFW_PRESS) {
            if(scaleFactor == 0) return;
            WaypointScreen.scrollOffset -= 30 / scaleFactor; //Scroll up

            if(WaypointScreen.scrollOffset < 0) {
                WaypointScreen.scrollOffset = 0;
            }
        }
        if(event.getKey() == GLFW.GLFW_KEY_DOWN && event.getAction() == GLFW.GLFW_PRESS) {
            if(scaleFactor == 0) return;
            WaypointScreen.scrollOffset += 30 / scaleFactor; //Scroll down
        }
    }
}
