package julianh06.wynnextras.features.achievements;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.features.badges.BadgeService;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.MinecraftUtils;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

@WEModule
public class AchievementManager {
    private static final long RESET_CONFIRMATION_TIMEOUT_MS = 30_000L;
    private static long resetConfirmationExpiresAt;

    private static final SubCommand confirmResetCommand = new SubCommand(
            "confirm",
            "confirms resetting all WynnExtras achievements",
            context -> {
                if (System.currentTimeMillis() > resetConfirmationExpiresAt) {
                    MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cThe achievement reset confirmation has expired. Use §e/we achievements reset §cagain."));
                    return 0;
                }

                resetConfirmationExpiresAt = 0L;
                AchievementTracking.clearAchievements();
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aAll achievements have been reset. §7Achievements based on API data will be unlocked again automatically."));
                return 1;
            },
            null,
            null
    );

    private static final SubCommand resetCommand = new SubCommand(
            "reset",
            "resets all WynnExtras achievements",
            context -> {
                resetConfirmationExpiresAt = System.currentTimeMillis() + RESET_CONFIRMATION_TIMEOUT_MS;
                Text confirmButton = Text.literal("[Click to confirm]").setStyle(Style.EMPTY
                        .withColor(Formatting.RED)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent.RunCommand("/we achievements reset confirm"))
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("Reset all WynnExtras achievements"))));
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.literal("§cAre you sure you want to reset all achievements?\n§7Achievements based on API data will be unlocked again automatically. ")
                        .append(confirmButton)));
                return 1;
            },
            List.of(confirmResetCommand),
            null
    );

    private static final Command achievementsCommand = new Command(
            "achievements",
            "opens the WynnExtras achievements screen",
            context -> {
                WEScreen.open(AchievementScreen::new);
                return 1;
            },
            List.of(resetCommand),
            null
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
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Reloading badge data from the server.")));
                return 1;
            }
    );

    private static final Command reloadAchievementsCommand = new Command(
            "reloadAchievements",
            "checks whether new WynnExtras achievements are completed",
            context -> {
                AchievementTracking.reloadAchievementsFromApi();
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Checking achievements from the API.")));
                return 1;
            }
    );
}