package julianh06.wynnextras.features.waypoints;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.utils.UI.WEScreen;

import java.util.List;

@WEModule
public class WaypointManager {
    private static final SubCommand editCommand = new SubCommand(
            "edit",
            "toggles waypoint edit mode",
            context -> {
                WaypointEditMode.toggleFromCommand();
                return 1;
            },
            null,
            null
    );

    private static final SubCommand freeMoveToggleCommand = new SubCommand(
            "freemovetoggle",
            "toggles waypoint edit free move mode",
            context -> {
                WaypointEditMode.toggleFreeMoveFromCommand();
                return 1;
            },
            null,
            null
    );

    private static final Command waypointCommand = new Command(
            "waypoints",
            "",
            context -> {
                WEScreen.open(WaypointScreen::new);
                return 1;
            },
            List.of(editCommand, freeMoveToggleCommand),
            null
    );
}
