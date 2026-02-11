package julianh06.wynnextras.features.waypoints;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.features.waypoints.old.WaypointData;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Path;
import java.util.List;

@WEModule
public class WaypointManager {
     private static Command waypointCommand = new Command(
         "newWaypoints",
         "",
         context -> {
             WEScreen.open(NewWaypointScreen::new);
             return 1;
         },
         null,
         null
     );

    private static Command waypointConvertTestCommand = new Command(
            "waypointConvertTest",
            "",
            context -> {
                WaypointData.loadFromFile(Path.of("E:\\modding\\WynnExtras\\run\\config\\wynnextras\\packages\\Blue Meteor Hay Bales Puzzle.json"));
                return 1;
            },
            null,
            null
    );
}
