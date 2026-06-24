package julianh06.wynnextras.features.waypoints.old;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.event.ClickEvent;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.waypoints.WaypointEditMode;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static julianh06.wynnextras.features.waypoints.old.WaypointScreen.scaleFactor;

@WEModule
public class Waypoints {
    static boolean commandsInitialized = false;

    private static SubCommand editCmd;
    private static SubCommand freeMoveToggleCmd;
    private static Command waypointsCmd;

    public static boolean inScreen = false;


    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            if(commandsInitialized) return;

            editCmd = new SubCommand(
                    "edit",
                    "toggles waypoint edit mode",
                    context -> {
                        WaypointEditMode.toggleFromCommand();
                        return 1;
                    },
                    null,
                    null
            );

            freeMoveToggleCmd = new SubCommand(
                    "freemovetoggle",
                    "toggles waypoint edit free move mode",
                    context -> {
                        WaypointEditMode.toggleFreeMoveFromCommand();
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
                    List.of(editCmd, freeMoveToggleCmd),
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
