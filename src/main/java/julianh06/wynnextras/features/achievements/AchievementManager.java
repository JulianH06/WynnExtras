package julianh06.wynnextras.features.achievements;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.utils.UI.WEScreen;

@WEModule
public class AchievementManager {
    private static final Command achievementsCommand = new Command(
            "achievements",
            "opens the WynnExtras achievements screen",
            context -> {
                WEScreen.open(AchievementScreen::new);
                return 1;
            }
    );
}