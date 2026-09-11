package julianh06.wynnextras.features.leaderboardviewer;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.utils.UI.WEScreen;

@WEModule
public class LV {
    private static Command lvCmd = new Command(
            "lv",
            "",
            context -> {
                open();
                return 1;
            },
            null,
            null
    );

    public static void open() {
        WEScreen.open(LVScreen::new);
    }
}
