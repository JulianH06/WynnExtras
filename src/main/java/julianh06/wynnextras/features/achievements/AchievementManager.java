package julianh06.wynnextras.features.achievements;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.features.badges.BadgeService;
import julianh06.wynnextras.utils.UI.WEScreen;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.text.Text;

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

    private static final Command badgeCommand = new Command(
            "badges",
            "opens the WynnExtras badges screen",
            context -> {
                AchievementScreen achievementScreen = new AchievementScreen();
                achievementScreen.setTab(AchievementScreen.Tab.BADGES);
                WEScreen.open(() -> achievementScreen);
                return 1;
            }
    );

    private static final Command reloadBadgesCommand = new Command(
            "reloadBadges",
            "reloads WynnExtras badge data from the server",
            context -> {
                BadgeService.reloadBadgeInfoFromServer();
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Reloading badge data from the server.")));
                return 1;
            }
    );

    private static final Command reloadAchievementsCommand = new Command(
            "reloadAchievements",
            "checks whether new WynnExtras achievements are completed",
            context -> {
                AchievementTracking.reloadAchievementsFromApi();
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Checking achievements from the API.")));
                return 1;
            }
    );
}